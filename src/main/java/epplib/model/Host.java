package epplib.model;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Host extends EPPObject {
	private String name;
	private List<InetAddress> addresses;

	public Host(String name) {
		this.name = name;
		addresses = new ArrayList<InetAddress>();
	}

	public void addAddress(String host) throws UnknownHostException {
		addresses.add(InetAddress.getByName(host));
	}

	public List<InetAddress> getAddresses() {
		return addresses;
	}

	public String getName() {
		return name;
	}

	public void setAddresses(List<InetAddress> addresses) {
		this.addresses = addresses;
	}

	public void setName(String name) {
		this.name = name;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Host host = (Host) o;

        if (addresses != null ? !addresses.equals(host.addresses) : host.addresses != null) return false;
        if (name != null ? !name.equals(host.name) : host.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (addresses != null ? addresses.hashCode() : 0);
        return result;
    }

    @Override
	public String toString() {
		return "Host [name=" + name + ", addresses=" + addresses + ", authInfo=" + authInfo + ", roid=" + roid + ", status=" + status + ", clId=" + clId
				+ ", crId=" + crId + ", upId=" + upId + ", crDate=" + crDate + ", upDate=" + upDate + ", trDate=" + trDate + "]";
	}
}
