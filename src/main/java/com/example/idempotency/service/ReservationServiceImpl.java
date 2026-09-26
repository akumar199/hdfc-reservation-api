package com.example.idempotency.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.idempotency.dto.ReservationRequest;
import com.example.idempotency.entity.Reservation;
import com.example.idempotency.entity.ReservationQueue;
import com.example.idempotency.entity.ShadowStock;
import com.example.idempotency.exception.IdempotencyKeyReusedException;
import com.example.idempotency.repository.ReservationQueueRepository;
import com.example.idempotency.repository.ReservationRepository;
import com.example.idempotency.repository.ShadowStockRepository;

@Service
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final AuthorityClient authorityClient;
    private final ShadowStockRepository shadowStockRepository;
    private final ReservationQueueRepository reservationQueueRepository;

    private ReservationMode mode = ReservationMode.LIVE;

    private static final int STANDIN_MAX_PER_ITEM = 10;

    public ReservationServiceImpl(
            ReservationRepository reservationRepository,
            AuthorityClient authorityClient,
            ShadowStockRepository shadowStockRepository,
            ReservationQueueRepository reservationQueueRepository) {

        this.reservationRepository = reservationRepository;
        this.authorityClient = authorityClient;
        this.shadowStockRepository = shadowStockRepository;
        this.reservationQueueRepository = reservationQueueRepository;
    }

    @Override
    @Transactional
    public Reservation createReservation(
            ReservationRequest request,
            String idempotencyKey) {

        /*
         * ==========================================
         * PART A - IDEMPOTENCY
         * ==========================================
         */

        Optional<Reservation> existing =
                reservationRepository.findByUserIdAndIdempotencyKey(
                        request.getUserId(),
                        idempotencyKey
                );

        if (existing.isPresent()) {

            Reservation oldReservation = existing.get();

            boolean sameRequest =
                    oldReservation.getItemId().equals(request.getItemId())
                    && oldReservation.getQty().equals(request.getQty());

            // Same user + same key + same body
            if (sameRequest) {
                return oldReservation;
            }

            // Same user + same key + different body
            throw new IdempotencyKeyReusedException();
        }

        /*
         * ==========================================
         * CREATE NEW RESERVATION
         * ==========================================
         */

        Reservation reservation = new Reservation();

        reservation.setUserId(request.getUserId());
        reservation.setItemId(request.getItemId());
        reservation.setQty(request.getQty());
        reservation.setIdempotencyKey(idempotencyKey);

        /*
         * ==========================================
         * PART B - LIVE MODE
         * ==========================================
         */

        if (mode == ReservationMode.LIVE) {

            try {

                AuthorityReservationResponse response =
                        authorityClient.reserve(
                                reservation.getId() != null
                                        ? reservation.getId().toString()
                                        : java.util.UUID.randomUUID().toString(),
                                request
                        );

                /*
                 * Authority responded successfully.
                 */
                if (response.isAccepted()) {

                    reservation.setStatus("confirmed");

                    /*
                     * Update shadow stock using
                     * Authority's latest available count.
                     */
                    if (response.getAvailable() != null) {

                        updateShadowStock(
                                request.getItemId(),
                                response.getAvailable()
                        );
                    }

                    return saveReservation(reservation);
                }

                /*
                 * Authority rejected reservation.
                 */
                reservation.setStatus("rejected");

                return saveReservation(reservation);

            } catch (Exception e) {

                /*
                 * Authority is unavailable / timed out.
                 *
                 * Switch to stand-in mode.
                 */
                mode = ReservationMode.STANDIN;
            }
        }

        /*
         * ==========================================
         * PART B - STAND-IN MODE
         * ==========================================
         */

        return createStandInReservation(reservation, request);
    }

    /*
     * ==========================================
     * STAND-IN RESERVATION
     * ==========================================
     */

    private Reservation createStandInReservation(
            Reservation reservation,
            ReservationRequest request) {

        Optional<ShadowStock> shadowOptional =
                shadowStockRepository.findByItemId(
                        request.getItemId()
                );

        /*
         * We don't know the stock.
         * Therefore we cannot safely stand in.
         */
        if (shadowOptional.isEmpty()) {

            reservation.setStatus("rejected");

            return saveReservation(reservation);
        }

        ShadowStock shadowStock = shadowOptional.get();

        int available = shadowStock.getAvailable();

        /*
         * Check shadow stock.
         */
        if (available < request.getQty()) {

            reservation.setStatus("rejected");

            return saveReservation(reservation);
        }

        /*
         * Calculate how many units have already
         * been accepted through stand-in mode.
         */
        int alreadyAccepted =
                getStandInQuantity(request.getItemId());

        /*
         * Check stand-in limit.
         */
        if (alreadyAccepted + request.getQty()
                > STANDIN_MAX_PER_ITEM) {

            reservation.setStatus("rejected");

            return saveReservation(reservation);
        }

        /*
         * Reserve locally against shadow stock.
         */
        shadowStock.setAvailable(
                available - request.getQty()
        );

        shadowStockRepository.save(shadowStock);

        /*
         * Reservation is NOT confirmed.
         * Authority hasn't approved it yet.
         */
        reservation.setStatus("pending");

        Reservation saved =
                saveReservation(reservation);

        /*
         * Put pending reservation into durable DB queue.
         */
        ReservationQueue queue = new ReservationQueue();

        queue.setReservationId(
                saved.getId().toString()
        );

        queue.setItemId(
                request.getItemId()
        );

        queue.setUserId(
                request.getUserId()
        );

        queue.setQty(
                request.getQty()
        );

        queue.setStatus("pending");

        queue.setCreatedAt(
                LocalDateTime.now()
        );

        reservationQueueRepository.save(queue);

        return saved;
    }

    /*
     * ==========================================
     * COUNT STAND-IN QUANTITY
     * ==========================================
     */

    private int getStandInQuantity(String itemId) {


        return reservationQueueRepository
                .findByItemIdAndStatus(itemId, "pending")
                .stream()
                .mapToInt(queue -> queue.getQty())
                .sum();
    }

    /*
     * ==========================================
     * UPDATE SHADOW STOCK
     * ==========================================
     */

    private void updateShadowStock(
            String itemId,
            int available) {

        ShadowStock shadowStock =
                shadowStockRepository
                        .findByItemId(itemId)
                        .orElseGet(() -> {

                            ShadowStock newStock =
                                    new ShadowStock();

                            newStock.setItemId(itemId);

                            return newStock;
                        });

        shadowStock.setAvailable(available);

        shadowStockRepository.save(shadowStock);
    }

    /*
     * ==========================================
     * DATABASE SAVE
     * ==========================================
     */

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Reservation saveReservation(
            Reservation reservation) {

        return reservationRepository.save(reservation);
    }
}