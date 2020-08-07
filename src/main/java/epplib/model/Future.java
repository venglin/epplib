package epplib.model;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.datatype.XMLGregorianCalendar;

public class Future extends EPPObject {
	private String name;
	private Integer periodYears;
	private Contact registrant;
	private Date exDate;

	public Future(String name, String authInfo) {
		this.name = name;
		this.authInfo = authInfo;
	}

	public Date getExDate() {
		return exDate;
	}

	public String getName() {
		return name;
	}

	public Integer getPeriodYears() {
		return periodYears;
	}

	public Contact getRegistrant() {
		return registrant;
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

	public void setPeriodYears(Integer periodYears) {
		this.periodYears = periodYears;
	}

	public void setRegistrant(Contact registrant) {
		this.registrant = registrant;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Future future = (Future) o;

        if (exDate != null ? !exDate.equals(future.exDate) : future.exDate != null) return false;
        if (name != null ? !name.equals(future.name) : future.name != null) return false;
        if (periodYears != null ? !periodYears.equals(future.periodYears) : future.periodYears != null) return false;
        if (registrant != null ? !registrant.equals(future.registrant) : future.registrant != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (periodYears != null ? periodYears.hashCode() : 0);
        result = 31 * result + (registrant != null ? registrant.hashCode() : 0);
        result = 31 * result + (exDate != null ? exDate.hashCode() : 0);
        return result;
    }

    @Override
	public String toString() {
		return "Future [name=" + name + ", periodYears=" + periodYears + ", registrant=" + registrant + ", exDate=" + exDate + ", authInfo=" + authInfo
				+ ", roid=" + roid + ", status=" + status + ", clId=" + clId + ", crId=" + crId + ", upId=" + upId + ", crDate=" + crDate + ", upDate="
				+ upDate + ", trDate=" + trDate + "]";
	}
}
