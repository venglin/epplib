package epplib.model.messages;

import epplib.model.enums.DomainMessageType;

public class DomainMessage extends Message {
	protected String name;
	private DomainMessageType type;

	public String getName() {
		return name;
	}

	public DomainMessageType getType() {
		return type;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setType(DomainMessageType type) {
		this.type = type;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DomainMessage that = (DomainMessage) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
	public String toString() {
		return "DomainMessage [name=" + name + ", type=" + type + ", message=" + message + "]";
	}
}
