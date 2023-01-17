package com.thelogicmaster.clearwing;

public class TranspilerException extends RuntimeException {

	public TranspilerException (String message) {
		this(message, null);
	}

	public TranspilerException (String message, Throwable cause) {
		super(message, cause);
	}
}
