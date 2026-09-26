package com.example.idempotency.service;

public class AuthorityReservationResponse {
	 private boolean accepted;
	    private Integer available;

	    public boolean isAccepted() {
	        return accepted;
	    }

	    public void setAccepted(boolean accepted) {
	        this.accepted = accepted;
	    }

	    public Integer getAvailable() {
	        return available;
	    }

	    public void setAvailable(Integer available) {
	        this.available = available;
	    }
}
