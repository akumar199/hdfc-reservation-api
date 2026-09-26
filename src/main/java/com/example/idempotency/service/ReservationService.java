package com.example.idempotency.service;

import com.example.idempotency.dto.ReservationRequest;
import com.example.idempotency.entity.Reservation;

public interface ReservationService {
	

    Reservation createReservation(
            ReservationRequest request,
            String idempotencyKey
    );

}
