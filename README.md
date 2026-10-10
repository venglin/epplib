# epplib

Java client library for the EPP interface of the **.pl** domain registry (NASK).

It speaks the NASK dialect of EPP over HTTPS with TLS client-certificate
authentication, and covers contacts, hosts, domains, domain options (*future*),
DNSSEC, poll messages and account balance.

## Requirements

- Java 11 or newer
- Maven 3.6+
- A registrar account in the registry, plus the client certificate and
  truststore issued for it

## Building

```bash
mvn clean install
```

JAXB classes are generated from the XSD files in `src/main/resources` during
the build, so the first compilation takes noticeably longer than the next ones.

Add it to your project:

```xml
<dependency>
    <groupId>pl.netium</groupId>
    <artifactId>epplib</artifactId>
    <version>1.1</version>
</dependency>
```

## Getting started

```java
EPPClient client = new EPPClient(
        "https://registry.dns.pl/registry/epp",
        "/path/to/client-keystore.jks", "keystorePassword",
        "/path/to/client-truststore.jks", "truststorePassword",
        null);                       // key alias, or null for the only key

client.eppLogin("registrar", "password");
try {
    boolean free = client.eppDomainCheck("example.pl");
} finally {
    client.eppLogout();
}
```

Every call throws `EPPLibException` when the registry refuses it. The message
carries both the general result message and the specific reason the registry
gave in `<extValue><reason>`, which is usually the part worth reading.

A session can also be used to change the account password:

```java
client.eppLogin("registrar", "oldPassword", "newPassword");
```

`eppHello()` sends a greeting without a session — useful as a connectivity
check.

## Contacts

A domain needs a registrant contact, created beforehand:

```java
Contact contact = new Contact(null, "owner@example.com", "authInfoCode");
contact.setName("Jan Kowalski");
contact.setIndividual(true);

PostalAddress address = new PostalAddress();
address.setStreet("Przykladowa 1");
address.setCity("Warszawa");
address.setPostalCode("00-001");
address.setCountryCode("PL");
contact.setIntPostalInfo(new PostalInfo(address));
contact.setVoice("+48.221234567");

client.eppContactCreate(contact);    // the assigned id lands in contact.getId()
```

Further operations: `eppContactInfo`, `eppContactChange`, `eppContactDelete`,
`eppContactCheck`, and status handling through `eppContactAddStatus` /
`eppContactRemoveStatus`.

## Domains

```java
Domain domain = new Domain("example.pl", "domainAuthInfo");
domain.setRegistrant(registrantContact);
domain.addNameserver(new Host("ns1.example.pl"));
domain.addNameserver(new Host("ns2.example.pl"));

client.eppDomainCreate(domain, 1);   // registration period in years
```

```java
Domain info = client.eppDomainInfo("example.pl");
info.getExDate();          // expiry date
info.getStatus();          // registry statuses
info.getNameservers();
info.getDsRecords();       // DNSSEC, see below
```

Renewal takes the current expiry date, which the registry compares literally
with its own record:

```java
client.eppDomainRenew("example.pl", info.getExDate(), 1, false);
```

Transfers use `eppDomainTransferRequest`, `eppDomainTransferQuery` and
`eppDomainTransferCancel`. Nameservers, contacts and statuses are changed with
the `eppDomainAdd...` / `eppDomainRemove...` / `eppDomainChange...` families,
each of which maps onto a single `domain:update` command.

## Hosts

Nameservers are separate registry objects. A host subordinate to a domain you
manage needs glue addresses:

```java
Host host = new Host("ns1.example.pl");
host.addAddress("192.0.2.1");
client.eppHostCreate(host);
```

## DNSSEC

The registry implements only the **DS Data Interface** of RFC 5910, so you pass
a digest of the key, never the key itself. A record carries four fields and the
digest is a hexadecimal string:

```java
DsRecord ds = new DsRecord(
        12345,                                   // key tag, 1-65535
        (short) 13,                              // algorithm (IANA DNSSEC registry)
        (short) 2,                               // digest type: 1 SHA-1, 2 SHA-256, 4 SHA-384
        "E2D3C916F6DEEAC73294E8268FB5885044A833FC5459588F4A9184F0D1B7D5F3");
```

Sign a domain at registration time:

```java
domain.addDsRecord(ds);
client.eppDomainCreate(domain, 1);
```

...or change the records of an existing one:

```java
client.eppDomainAddDs("example.pl", List.of(ds));
client.eppDomainRemoveDs("example.pl", List.of(oldDs));
client.eppDomainRemoveAllDs("example.pl");           // unsign the domain

// both at once, in a single domain:update
client.eppDomainChangeDs("example.pl", List.of(oldDs), List.of(newDs), false);
```

`Domain.isSigned()` and `Domain.getDsRecords()` report the state returned by
`domain:info`.

> **Check the digest before you send it.** Neither the schema nor the registry
> verifies that the digest length matches the declared digest type — a SHA-256
> digest labelled as SHA-1 will be accepted. The result is not an error: the
> domain simply stops resolving for validating resolvers, while everything
> still looks correct in the registry. Expected lengths are 40 hexadecimal
> characters for SHA-1, 64 for SHA-256 and 96 for SHA-384.

## Poll messages

The registry queues notifications about transfers, expirations and broken
delegations. Read them one at a time and acknowledge each:

```java
Message message;
while ((message = client.eppGetMessage()) != null) {
    if (message instanceof TransferStatusMessage transfer) {
        // handle the transfer state change
    }
    client.eppAckMessage(message.getId());
}
```

Message types: `TransferStatusMessage`, `ExpirationMessage`, `AuthInfoMessage`,
`BrokenDelegationMessage`, `DomainMessage` and the base `Message`.

## Account balance

```java
Long funds = client.eppGetPaymentFunds();
```

## Registry behaviour worth knowing

These are constraints of the registry rather than of the library, and each of
them tends to cost an afternoon the first time you meet it.

- **Only the registrant contact is used.** The registry sets
  `MAX_CONTACT_TECH`, `MAX_CONTACT_ADMIN` and `MAX_CONTACT_BILLING` to zero, so
  a domain cannot carry technical, administrative or billing contacts. The
  corresponding methods exist for completeness but the registry rejects them.
- **An expired domain is renewed with the reactivation flag.** After expiry a
  domain spends 30 days in the `BLOCKED` state, during which it can still be
  renewed — but only with `reactivate = true` and a period of exactly one year.
  The registry charges a surcharge for it.
- **`authInfo` has a required length.** A value of the wrong length is refused
  with a parameter range error rather than a helpful message.
- **External nameservers must already exist.** A host that is not subordinate
  to the domain being registered has to be a known registry object first;
  creating it along the way leaves it pending and the domain creation fails.
- **DS records are limited to six per domain.**
- **Extensions are declared once per session.** The library announces the
  contact, domain and DNSSEC extensions at login. A command carrying an
  extension that was not declared is refused, and the refusal points at the
  command rather than at the missing declaration.

## Schemas and code generation

`src/main/resources` holds the registry XSD files; `bindings.xml` maps each of
them to a Java package and an XML prefix. The Maven JAXB plugin generates the
binding classes at build time, so adding support for a new extension means
dropping its schema in, adding a `bindings.xml` entry, and listing the new
package in the JAXB context inside `EPPClient`.

## Licence

See [LICENSE](LICENSE).

---

Sponsored and used by Netium.pl sp. z o.o. (http://www.netium.pl)
