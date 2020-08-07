package epplib.model;

import epplib.model.enums.ContactType;

public class DomainContact {
	private Contact contact;
	private ContactType contactType;
	
	public DomainContact(Contact contact, ContactType contactType) {
		this.contact = contact;
		this.contactType = contactType;
	}

	public Contact getContact() {
		return contact;
	}

	public ContactType getContactType() {
		return contactType;
	}

	public void setContact(Contact contact) {
		this.contact = contact;
	}

	public void setContactType(ContactType contactType) {
		this.contactType = contactType;
	}

	@Override
	public String toString() {
		return "DomainContact [contact=" + contact + ", contactType=" + contactType + "]";
	}
}
