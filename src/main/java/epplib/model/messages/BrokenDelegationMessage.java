package epplib.model.messages;

import java.util.ArrayList;
import java.util.List;

public class BrokenDelegationMessage extends Message {
	private String nameserver;
	private List<String> domains;

	public BrokenDelegationMessage() {
		super();
		domains = new ArrayList<>();
	}

	public void addDomain(String domain) {
		domains.add(domain);
	}

	public List<String> getDomains() {
		return domains;
	}

	public String getNameserver() {
		return nameserver;
	}

	public void setDomains(List<String> domains) {
		this.domains = domains;
	}

	public void setNameserver(String nameserver) {
		this.nameserver = nameserver;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BrokenDelegationMessage that = (BrokenDelegationMessage) o;

        if (domains != null ? !domains.equals(that.domains) : that.domains != null) return false;
        if (nameserver != null ? !nameserver.equals(that.nameserver) : that.nameserver != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = nameserver != null ? nameserver.hashCode() : 0;
        result = 31 * result + (domains != null ? domains.hashCode() : 0);
        return result;
    }

    @Override
	public String toString() {
		return "BrokenDelegationMessage [nameserver=" + nameserver + ", domains=" + domains + ", message=" + message + "]";
	}
}
