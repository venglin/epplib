package epplib.exceptions;

public class EPPLibException extends Exception {

	private static final long serialVersionUID = -2119620413419891858L;

	public EPPLibException() {
		super();
	}

	public EPPLibException(String message) {
		super(message);
	}

	public EPPLibException(String message, Throwable cause) {
		super(message, cause);
	}

	public EPPLibException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public EPPLibException(Throwable cause) {
		super(cause);
	}
}
