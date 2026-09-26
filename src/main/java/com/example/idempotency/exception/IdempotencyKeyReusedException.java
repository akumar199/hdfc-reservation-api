package com.example.idempotency.exception;


	
	public class IdempotencyKeyReusedException extends RuntimeException {

	    public IdempotencyKeyReusedException() {
	        super("idempotency_key_reused");
	    }
	}

}
