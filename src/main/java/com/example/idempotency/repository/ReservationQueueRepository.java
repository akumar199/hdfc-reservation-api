package com.example.idempotency.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.idempotency.entity.Reservation;
import com.example.idempotency.entity.ReservationQueue;

public interface ReservationQueueRepository
extends JpaRepository<ReservationQueue, Long> {

List<ReservationQueue> findByStatusOrderByCreatedAtAsc(String status);

Optional<Reservation> findByItemIdAndStatus(String itemId, String string);
}