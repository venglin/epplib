package epplib.model.messages;

public class Message {
	protected String message;
	protected String id;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Message [message=" + message + ", id=" + id + "]";
	}
}
