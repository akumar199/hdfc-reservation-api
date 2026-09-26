package com.example.idempotency.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.idempotency.dto.ReservationRequest;
import com.example.idempotency.entity.Reservation;
import com.example.idempotency.service.ReservationService;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@RestController
@RequestMapping("/reservations")
public class ReservationController {
	
	

	    private final ReservationService reservationService;

	    public ReservationController(ReservationService reservationService) {
	        this.reservationService = reservationService;
	    }

	    @PostMapping
	    public ResponseEntity<Reservation> createReservation(
	            @RequestHeader("Idempotency-Key") String idempotencyKey,
	            @RequestBody ReservationRequest request) {

	        Reservation reservation =
	                reservationService.createReservation(request, idempotencyKey);

	        return ResponseEntity.ok(reservation);
	    }
	}

