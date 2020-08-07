package epplib.model;

public class Contact extends EPPObject {
	private String id;
	private PostalInfo locPostalInfo;
	private PostalInfo intPostalInfo;
	private String voice;
	private String fax;
	private String email;
	private Boolean individual;
	private Boolean consentForPublishing;

	public Contact(String id, String email, String authInfo) {
		this.id = id;
		this.email = email;
		this.authInfo = authInfo;
	}

	public String getEmail() {
		return email;
	}

    public String getFax() {

		return fax;
	}

	public String getId() {
		return id;
	}

	public PostalInfo getIntPostalInfo() {
		return intPostalInfo;
	}

	public PostalInfo getLocPostalInfo() {
		return locPostalInfo;
	}

	public String getVoice() {
		return voice;
	}

	public Boolean isConsentForPublishing() {
		return consentForPublishing;
	}

	public Boolean isIndividual() {
		return individual;
	}

	public void setConsentForPublishing(Boolean consentForPublishing) {
		this.consentForPublishing = consentForPublishing;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setFax(String fax) {
		this.fax = fax;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setIndividual(Boolean individual) {
		this.individual = individual;
	}

	public void setIntPostalInfo(PostalInfo intPostalInfo) {
		this.intPostalInfo = intPostalInfo;
	}

	public void setLocPostalInfo(PostalInfo locPostalInfo) {
		this.locPostalInfo = locPostalInfo;
	}

	public void setVoice(String voice) {
		this.voice = voice;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Contact contact = (Contact) o;

        if (consentForPublishing != null ? !consentForPublishing.equals(contact.consentForPublishing) : contact.consentForPublishing != null)
            return false;
        if (email != null ? !email.equals(contact.email) : contact.email != null) return false;
        if (fax != null ? !fax.equals(contact.fax) : contact.fax != null) return false;
        if (id != null ? !id.equals(contact.id) : contact.id != null) return false;
        if (individual != null ? !individual.equals(contact.individual) : contact.individual != null) return false;
        if (intPostalInfo != null ? !intPostalInfo.equals(contact.intPostalInfo) : contact.intPostalInfo != null)
            return false;
        if (locPostalInfo != null ? !locPostalInfo.equals(contact.locPostalInfo) : contact.locPostalInfo != null)
            return false;
        if (voice != null ? !voice.equals(contact.voice) : contact.voice != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (locPostalInfo != null ? locPostalInfo.hashCode() : 0);
        result = 31 * result + (intPostalInfo != null ? intPostalInfo.hashCode() : 0);
        result = 31 * result + (voice != null ? voice.hashCode() : 0);
        result = 31 * result + (fax != null ? fax.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (individual != null ? individual.hashCode() : 0);
        result = 31 * result + (consentForPublishing != null ? consentForPublishing.hashCode() : 0);
        return result;
    }

	@Override
	public String toString() {
		return "Contact [id=" + id + ", locPostalInfo=" + locPostalInfo + ", intPostalInfo=" + intPostalInfo + ", voice=" + voice + ", fax=" + fax + ", email="
				+ email + ", individual=" + individual + ", consentForPublishing=" + consentForPublishing + ", authInfo=" + authInfo + ", roid=" + roid
				+ ", status=" + status + ", clId=" + clId + ", crId=" + crId + ", upId=" + upId + ", crDate=" + crDate + ", upDate=" + upDate + ", trDate="
				+ trDate + "]";
	}
}
