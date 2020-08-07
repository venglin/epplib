package epplib.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import javax.xml.datatype.XMLGregorianCalendar;

public abstract class EPPObject {
	protected String authInfo;
	protected String roid;
	protected List<String> status;
	protected String clId;
	protected String crId;
	protected String upId;
	protected Date crDate;
	protected Date upDate;
	protected Date trDate;

	public EPPObject() {
		status = new ArrayList<>();
	}

	public String getAuthInfo() {
		return authInfo;
	}

	public String getClId() {
		return clId;
	}

	public Date getCrDate() {
		return crDate;
	}

	public String getCrId() {
		return crId;
	}

	public String getRoid() {
		return roid;
	}

	public List<String> getStatus() {
		return status;
	}

	public Date getTrDate() {
		return trDate;
	}

	public Date getUpDate() {
		return upDate;
	}

	public String getUpId() {
		return upId;
	}

	public void setAuthInfo(String authInfo) {
		this.authInfo = authInfo;
	}

	public void setClId(String clId) {
		this.clId = clId;
	}

	public void setCrDate(Date crDate) {
		this.crDate = crDate;
	}

	public void setCrDate(XMLGregorianCalendar crDate) {
		if (crDate != null) {
			GregorianCalendar gc = crDate.toGregorianCalendar();
			gc.setTimeZone(TimeZone.getTimeZone("UTC"));
			this.crDate = gc.getTime();
		}
	}

	public void setCrId(String crId) {
		this.crId = crId;
	}

	public void setRoid(String roid) {
		this.roid = roid;
	}

	public void setStatus(List<String> status) {
		this.status = status;
	}

	public void setTrDate(Date trDate) {
		this.trDate = trDate;
	}

	public void setTrDate(XMLGregorianCalendar trDate) {
		if (trDate != null) {
			GregorianCalendar gc = trDate.toGregorianCalendar();
			gc.setTimeZone(TimeZone.getTimeZone("UTC"));
			this.trDate = gc.getTime();
		}
	}

	public void setUpDate(Date upDate) {
		this.upDate = upDate;
	}

	public void setUpDate(XMLGregorianCalendar upDate) {
		if (upDate != null) {
			GregorianCalendar gc = upDate.toGregorianCalendar();
			gc.setTimeZone(TimeZone.getTimeZone("UTC"));
			this.upDate = gc.getTime();
		}
	}

	public void setUpId(String upId) {
		this.upId = upId;
	}

	@Override
	public String toString() {
		return "EPPObject [authInfo=" + authInfo + ", roid=" + roid + ", status=" + status + ", clId=" + clId + ", crId=" + crId + ", upId=" + upId
				+ ", crDate=" + crDate + ", upDate=" + upDate + ", trDate=" + trDate + "]";
	}
}
