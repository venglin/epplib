package epplib.model.messages;

import java.util.ArrayList;
import java.util.List;

public class ExpirationMessage extends Message {
	private List<String> domains;

	public ExpirationMessage() {
		domains = new ArrayList<>();
	}

	public List<String> getDomains() {
		return domains;
	}

	public void setDomains(List<String> domains) {
		this.domains = domains;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExpirationMessage that = (ExpirationMessage) o;

        if (domains != null ? !domains.equals(that.domains) : that.domains != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return domains != null ? domains.hashCode() : 0;
    }

    @Override
	public String toString() {
		return "ExpirationMessage [domains=" + domains + ", message=" + message + "]";
	}
}
