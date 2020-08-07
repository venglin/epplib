package epplib.model;

import java.util.ArrayList;
import java.util.List;

public class PostalAddress {
	private List<String> street;
	private String city;
	private String sp;
	private String pc;
	private String cc;

	public PostalAddress() {
		street = new ArrayList<String>();
	}

	public String getCc() {
		return cc;
	}

	public String getCity() {
		return city;
	}

	public String getPc() {
		return pc;
	}

	public String getSp() {
		return sp;
	}

	public List<String> getStreet() {
		return street;
	}

	public void setCc(String cc) {
		this.cc = cc;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public void setPc(String pc) {
		this.pc = pc;
	}

	public void setSp(String sp) {
		this.sp = sp;
	}

	public void setStreet(List<String> street) {
		this.street = street;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostalAddress that = (PostalAddress) o;

        if (cc != null ? !cc.equals(that.cc) : that.cc != null) return false;
        if (city != null ? !city.equals(that.city) : that.city != null) return false;
        if (pc != null ? !pc.equals(that.pc) : that.pc != null) return false;
        if (sp != null ? !sp.equals(that.sp) : that.sp != null) return false;
        if (street != null ? !street.equals(that.street) : that.street != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = street != null ? street.hashCode() : 0;
        result = 31 * result + (city != null ? city.hashCode() : 0);
        result = 31 * result + (sp != null ? sp.hashCode() : 0);
        result = 31 * result + (pc != null ? pc.hashCode() : 0);
        result = 31 * result + (cc != null ? cc.hashCode() : 0);
        return result;
    }

    @Override
	public String toString() {
		return "PostalAddress [street=" + street + ", city=" + city + ", sp=" + sp + ", pc=" + pc + ", cc=" + cc + "]";
	}
}
