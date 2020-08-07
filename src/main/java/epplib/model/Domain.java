package epplib.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import javax.xml.datatype.XMLGregorianCalendar;

import epplib.model.enums.DomainType;

public class Domain extends EPPObject {
	private String name;
	private List<Host> nameservers;
	private Contact registrant;
	private Contact tech;
	private Contact billing;
	private Contact admin;
	private String authInfo;
	private String reason;
	private DomainType type;
	private Date exDate;

	public Domain(String name, String authInfo) {
		this.name = name;
		this.authInfo = authInfo;
		nameservers = new ArrayList<Host>();
	}

	public void addNameserver(Host nameserver) {
		nameservers.add(nameserver);
	}

	public Contact getAdmin() {
		return admin;
	}

	@Override
	public String getAuthInfo() {
		return authInfo;
	}

	public Contact getBilling() {
		return billing;
	}

	public Date getExDate() {
		return exDate;
	}

	public String getName() {
		return name;
	}

	public List<Host> getNameservers() {
		return nameservers;
	}

	public String getReason() {
		return reason;
	}

	public Contact getRegistrant() {
		return registrant;
	}

	public Contact getTech() {
		return tech;
	}

	public DomainType getType() {
		return type;
	}

	public void setAdmin(Contact admin) {
		this.admin = admin;
	}

	@Override
	public void setAuthInfo(String authInfo) {
		this.authInfo = authInfo;
	}

	public void setBilling(Contact billing) {
		this.billing = billing;
	}

	public void setExDate(Date exDate) {
		this.exDate = exDate;
	}

	public void setExDate(XMLGregorianCalendar exDate) {
		if (exDate != null) {
			GregorianCalendar gc = exDate.toGregorianCalendar();
			gc.setTimeZone(TimeZone.getTimeZone("UTC"));
			this.exDate = gc.getTime();
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setNameservers(List<Host> nameservers) {
		this.nameservers = nameservers;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public void setRegistrant(Contact registrant) {
		this.registrant = registrant;
	}

	public void setTech(Contact tech) {
		this.tech = tech;
	}

	public void setType(DomainType type) {
		this.type = type;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Domain domain = (Domain) o;

        if (admin != null ? !admin.equals(domain.admin) : domain.admin != null) return false;
        if (authInfo != null ? !authInfo.equals(domain.authInfo) : domain.authInfo != null) return false;
        if (billing != null ? !billing.equals(domain.billing) : domain.billing != null) return false;
        if (exDate != null ? !exDate.equals(domain.exDate) : domain.exDate != null) return false;
        if (name != null ? !name.equals(domain.name) : domain.name != null) return false;
        if (nameservers != null ? !nameservers.equals(domain.nameservers) : domain.nameservers != null) return false;
        if (reason != null ? !reason.equals(domain.reason) : domain.reason != null) return false;
        if (registrant != null ? !registrant.equals(domain.registrant) : domain.registrant != null) return false;
        if (tech != null ? !tech.equals(domain.tech) : domain.tech != null) return false;
        if (type != domain.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (nameservers != null ? nameservers.hashCode() : 0);
        result = 31 * result + (registrant != null ? registrant.hashCode() : 0);
        result = 31 * result + (tech != null ? tech.hashCode() : 0);
        result = 31 * result + (billing != null ? billing.hashCode() : 0);
        result = 31 * result + (admin != null ? admin.hashCode() : 0);
        result = 31 * result + (authInfo != null ? authInfo.hashCode() : 0);
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (exDate != null ? exDate.hashCode() : 0);
        return result;
    }

    @Override
	public String toString() {
		return "Domain [name=" + name + ", nameservers=" + nameservers + ", registrant=" + registrant + ", tech=" + tech
				+ ", billing=" + billing + ", admin=" + admin + ", authInfo=" + authInfo + ", reason=" + reason + ", type=" + type + ", exDate=" + exDate
				+ ", roid=" + roid + ", status=" + status + ", clId=" + clId + ", crId=" + crId + ", upId=" + upId + ", crDate=" + crDate + ", upDate="
				+ upDate + ", trDate=" + trDate + "]";
	}
}
