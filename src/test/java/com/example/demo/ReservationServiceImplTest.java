package com.example.demo;



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.idempotency.dto.ReservationRequest;
import com.example.idempotency.entity.Reservation;
import com.example.idempotency.entity.ReservationQueue;
import com.example.idempotency.entity.ShadowStock;
import com.example.idempotency.exception.IdempotencyKeyReusedException;
import com.example.idempotency.repository.ReservationQueueRepository;
import com.example.idempotency.repository.ReservationRepository;
import com.example.idempotency.repository.ShadowStockRepository;
import com.example.idempotency.service.AuthorityClient;
import com.example.idempotency.service.ReservationServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {
	private ReservationRepository reservationRepository1;
	

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private AuthorityClient authorityClient;

    @Mock
    private ShadowStockRepository shadowStockRepository;

    @Mock
    private ReservationQueueRepository reservationQueueRepository;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private ReservationRequest request;

    @BeforeEach
    void setUp() {

        request = new ReservationRequest();

        request.setUserId("user-1");
        request.setItemId("item-1");
        request.setQty(1);
    }

    @Test
    void shouldCreateNewReservation() {

        when(reservationRepository.findByUserIdAndIdempotencyKey(
                "user-1",
                "key-123"
        )).thenReturn(Optional.empty());

        Reservation savedReservation = new Reservation();

        savedReservation.setUserId("user-1");
        savedReservation.setItemId("item-1");
        savedReservation.setQty(1);
        savedReservation.setIdempotencyKey("key-123");
        savedReservation.setStatus("confirmed");

        when(reservationRepository.save(any(Reservation.class)))
                .thenReturn(savedReservation);

        /*
         * Authority response is mocked as healthy/accepted.
         * Adapt this part if your AuthorityReservationResponse
         * constructor/methods are different.
         */

        Reservation result =
                reservationService.createReservation(
                        request,
                        "key-123"
                );

        assertNotNull(result);
    }
    
    
    
    

    @Test
    void shouldReturnExistingReservationForSameIdempotencyKey() {
    	

        Reservation existing = new Reservation();

        existing.setUserId("user-1");
        existing.setItemId("item-1");
        existing.setQty(1);
        existing.setIdempotencyKey("key-123");
        existing.setStatus("confirmed");

        when(reservationRepository.findByUserIdAndIdempotencyKey(
                "user-1",
                "key-123"
        )).thenReturn(Optional.of(existing));

        Reservation result =
                reservationService.createReservation(
                        request,
                        "key-123"
                );

        assertEquals(existing, result);

        /*
         * Database must not create another reservation.
         */
        verify(reservationRepository, never())
                .save(any(Reservation.class));
    }

    @Test
    void shouldRejectSameKeyWithDifferentBody() {

        Reservation existing = new Reservation();

        existing.setUserId("user-1");
        existing.setItemId("item-1");
        existing.setQty(1);
        existing.setIdempotencyKey("key-123");
        existing.setStatus("confirmed");

        when(reservationRepository.findByUserIdAndIdempotencyKey(
                "user-1",
                "key-123"
        )).thenReturn(Optional.of(existing));

        /*
         * Same user + same key,
         * but different item.
         */
        request.setItemId("item-2");

        assertThrows(
                IdempotencyKeyReusedException.class,
                () -> reservationService.createReservation(
                        request,
                        "key-123"
                )
        );

        verify(reservationRepository, never())
                .save(any(Reservation.class));
    }

    @Test
    void shouldRejectSameKeyWithDifferentQuantity() {

        Reservation existing = new Reservation();

        existing.setUserId("user-1");
        existing.setItemId("item-1");
        existing.setQty(1);
        existing.setIdempotencyKey("key-123");
        existing.setStatus("confirmed");

        when(reservationRepository.findByUserIdAndIdempotencyKey(
                "user-1",
                "key-123"
        )).thenReturn(Optional.of(existing));

        /*
         * Same user + same key,
         * but different quantity.
         */
        request.setQty(5);

        assertThrows(
                IdempotencyKeyReusedException.class,
                () -> reservationService.createReservation(
                        request,
                        "key-123"
                )
        );

        verify(reservationRepository, never())
                .save(any(Reservation.class));
    }

    @Test
    void shouldAllowSameIdempotencyKeyForDifferentUser() {

        ReservationRequest secondRequest =
                new ReservationRequest();

        secondRequest.setUserId("user-2");
        secondRequest.setItemId("item-1");
        secondRequest.setQty(1);

        when(reservationRepository.findByUserIdAndIdempotencyKey(
                "user-2",
                "key-123"
        )).thenReturn(Optional.empty());

        Reservation saved = new Reservation();

        saved.setUserId("user-2");
        saved.setItemId("item-1");
        saved.setQty(1);
        saved.setIdempotencyKey("key-123");
        saved.setStatus("confirmed");

        when(reservationRepository.save(any(Reservation.class)))
                .thenReturn(saved);

        Reservation result =
                reservationService.createReservation(
                        secondRequest,
                        "key-123"
                );

        assertNotNull(result);

        assertEquals(
                "user-2",
                result.getUserId()
        );
    }
}