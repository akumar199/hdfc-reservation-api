package com.example.idempotency.service;

import com.example.idempotency.dto.ReservationRequest;

public interface AuthorityClient {
	
	AuthorityReservationResponse reserve(
            String reservationId,
            ReservationRequest request);

    boolean isHealthy();
}
