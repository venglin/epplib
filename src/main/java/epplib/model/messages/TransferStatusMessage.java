package epplib.model.messages;

import epplib.model.enums.ObjectType;
import epplib.model.enums.TransferStatus;

public class TransferStatusMessage extends Message {
	private String name;
	private TransferStatus status;
	private ObjectType type;

	public String getName() {
		return name;
	}

	public TransferStatus getStatus() {
		return status;
	}

	public ObjectType getType() {
		return type;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setStatus(TransferStatus status) {
		this.status = status;
	}

	public void setType(ObjectType type) {
		this.type = type;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TransferStatusMessage that = (TransferStatusMessage) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (status != that.status) return false;
        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
	public String toString() {
		return "TransferStatusMessage [name=" + name + ", status=" + status + ", type=" + type + ", message=" + message + "]";
	}
}
