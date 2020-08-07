package epplib;

import pl.dns.nask_epp.contact.*;
import pl.dns.nask_epp.contact.CreDataType;
import pl.dns.nask_epp.domain.ContactAttrType;
import pl.dns.nask_epp.domain.HostsType;
import pl.dns.nask_epp.domain.PUnitType;
import pl.dns.nask_epp.domain.PeriodType;
import pl.dns.nask_epp.epp.*;
import pl.dns.nask_epp.epp.ObjectFactory;
import pl.dns.nask_epp.eppcom.PwAuthInfoType;
import pl.dns.nask_epp.extcon.CmdAllType;
import pl.dns.nask_epp.future.AuthInfoTypeWithoutRoid;
import pl.dns.nask_epp.host.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;

import pl.dns.nask_epp.extcon.InfoType;
import pl.dns.nask_epp.extcon.RspAllType;
import epplib.exceptions.EPPLibException;
import epplib.model.Contact;
import epplib.model.Domain;
import epplib.model.DomainContact;
import epplib.model.Future;
import epplib.model.Host;
import epplib.model.PostalAddress;
import epplib.model.PostalInfo;
import epplib.model.enums.ContactType;
import epplib.model.enums.DomainMessageType;
import epplib.model.enums.DomainType;
import epplib.model.enums.ObjectType;
import epplib.model.enums.TransferStatus;
import epplib.model.messages.AuthInfoMessage;
import epplib.model.messages.BrokenDelegationMessage;
import epplib.model.messages.DomainMessage;
import epplib.model.messages.ExpirationMessage;
import epplib.model.messages.Message;
import epplib.model.messages.TransferStatusMessage;
import pl.dns.nask_epp.host.AddRemType;
import pl.dns.nask_epp.host.AddrType;
import pl.dns.nask_epp.host.CheckType;
import pl.dns.nask_epp.host.ChgType;
import pl.dns.nask_epp.host.ChkDataType;
import pl.dns.nask_epp.host.CreateType;
import pl.dns.nask_epp.host.InfDataType;
import pl.dns.nask_epp.host.StatusType;
import pl.dns.nask_epp.host.StatusValueType;
import pl.dns.nask_epp.host.UpdateType;

class AliasKeyManager implements X509KeyManager {

	private KeyStore ks;
	private String alias;
	private String password;

	public AliasKeyManager(KeyStore ks, String alias, String password) {
		this.ks = ks;
		this.alias = alias;
		this.password = password;
	}

	@Override
	public String chooseClientAlias(String[] str, Principal[] principal, Socket socket) {
		return alias;
	}

	@Override
	public String chooseServerAlias(String str, Principal[] principal, Socket socket) {
		return alias;
	}

	@Override
	public X509Certificate[] getCertificateChain(String alias) {
		java.security.cert.Certificate[] certificates = null;
		try {
			certificates = this.ks.getCertificateChain(alias);
		} catch (KeyStoreException e) {
			throw new RuntimeException(e);
		}
		if (certificates == null)
			throw new RuntimeException(new FileNotFoundException("no certificate found for alias:" + alias));
		X509Certificate[] x509Certificates = new X509Certificate[certificates.length];
		System.arraycopy(certificates, 0, x509Certificates, 0, certificates.length);
		return x509Certificates;
	}

	@Override
	public String[] getClientAliases(String str, Principal[] principal) {
		return new String[] { alias };
	}

	@Override
	public PrivateKey getPrivateKey(String alias) {
		try {
			return (PrivateKey) ks.getKey(alias, password == null ? null : password.toCharArray());
		} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String[] getServerAliases(String str, Principal[] principal) {
		return new String[] { alias };
	}
}

public class EPPClient {
	private List<HttpCookie> cookies = new ArrayList<>();
	private static final Logger LOGGER = Logger.getLogger(EPPClient.class);

	private pl.dns.nask_epp.epp.ObjectFactory eppOF = new pl.dns.nask_epp.epp.ObjectFactory();
	private pl.dns.nask_epp.contact.ObjectFactory contactOF = new pl.dns.nask_epp.contact.ObjectFactory();
	private pl.dns.nask_epp.host.ObjectFactory hostOF = new pl.dns.nask_epp.host.ObjectFactory();
	private pl.dns.nask_epp.domain.ObjectFactory domainOF = new pl.dns.nask_epp.domain.ObjectFactory();
	private pl.dns.nask_epp.extreport.ObjectFactory extrepOF = new pl.dns.nask_epp.extreport.ObjectFactory();
	private pl.dns.nask_epp.extcon.ObjectFactory extconOF = new pl.dns.nask_epp.extcon.ObjectFactory();
	private pl.dns.nask_epp.extdom.ObjectFactory extdomOF = new pl.dns.nask_epp.extdom.ObjectFactory();
	private pl.dns.nask_epp.extfut.ObjectFactory extfutOF = new pl.dns.nask_epp.extfut.ObjectFactory();
	private pl.dns.nask_epp.future.ObjectFactory futureOF = new pl.dns.nask_epp.future.ObjectFactory();

	private SSLSocketFactory factory;
	private String url;

	private boolean loggedIn = false;

	public EPPClient() {

	}

	public EPPClient(String url) {
		this.url = url;
	}

	public EPPClient(String url, String keyStoreFileName, String keyStorePassword, String trustStoreFileName, String trustStorePassword, String alias)
			throws EPPLibException {
		KeyManager[] keyManagers;
		try {
			keyManagers = createKeyManagers(keyStoreFileName, keyStorePassword, alias);
		} catch (UnrecoverableKeyException | CertificateException | KeyStoreException | NoSuchAlgorithmException | IOException e) {
			throw new EPPLibException(e);
		}
		TrustManager[] trustManagers;
		try {
			trustManagers = createTrustManagers(trustStoreFileName, trustStorePassword);
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			throw new EPPLibException(e);
		}
		try {
			factory = initTLS(keyManagers, trustManagers);
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			throw new EPPLibException(e);
		}

		this.url = url;
	}

	private int checkResponseCode(EppType response) throws EPPLibException {
		int code = response.getResponse().getResult().get(0).getCode();
		String message = response.getResponse().getResult().get(0).getMsg().getValue();
        StringBuilder reasons = null;

		LOGGER.debug(String.format("reponse code = %d, msg = '%s', reasons = '%s'", code, message, reasons != null ? reasons.toString() : ""));

		if (code >= 2000) {
            if (reasons != null)
			    throw new EPPLibException(message + ": " + reasons.toString());
            else
                throw new EPPLibException(message);
        }

		return code;
	}

	private void checkSession() {
		if (loggedIn == false)
			throw new IllegalStateException("not logged in");
	}

	private KeyManager[] createKeyManagers(String keyStoreFileName, String keyStorePassword, String alias) throws CertificateException, IOException,
			KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
		java.io.InputStream inputStream = new java.io.FileInputStream(keyStoreFileName);
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		keyStore.load(inputStream, keyStorePassword == null ? null : keyStorePassword.toCharArray());

		KeyManager[] managers;
		if (alias != null)
			managers = new KeyManager[] { new AliasKeyManager(keyStore, alias, keyStorePassword) };
		else {
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, keyStorePassword == null ? null : keyStorePassword.toCharArray());
			managers = keyManagerFactory.getKeyManagers();
		}
		return managers;
	}

	private TrustManager[] createTrustManagers(String trustStoreFileName, String trustStorePassword) throws KeyStoreException, NoSuchAlgorithmException,
			CertificateException, IOException {
		java.io.InputStream inputStream = new java.io.FileInputStream(trustStoreFileName);
		KeyStore trustStore = KeyStore.getInstance("JKS");
		trustStore.load(inputStream, trustStorePassword == null ? null : trustStorePassword.toCharArray());
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(trustStore);
		return trustManagerFactory.getTrustManagers();
	}

	@SuppressWarnings("unchecked")
	private <T> T doRequest(Object rootElement, boolean newSession) throws EPPLibException {
		final String CTX = "pl.dns.nask_epp.epp:pl.dns.nask_epp.contact:pl.dns.nask_epp.domain:pl.dns.nask_epp.eppcom:pl.dns.nask_epp.extreport:pl.dns.nask_epp.host:pl.dns.nask_epp.extcon:pl.dns.nask_epp.host:pl.dns.nask_epp.extdom:pl.dns.nask_epp.extepp:pl.dns.nask_epp.extfut:pl.dns.nask_epp.future";

		HttpURLConnection connection = null;
		OutputStream ostream = null;
		InputStream istream = null;
		Map<String, List<String>> headers = null;
		JAXBElement<T> doc = null;

		if (url == null)
			url = "https://registry.dns.pl/registry/epp";

		try {
			URL urlc = new URL(url);
			connection = (HttpURLConnection) urlc.openConnection();
			if (connection instanceof HttpsURLConnection && factory != null)
				((HttpsURLConnection) connection).setSSLSocketFactory(factory);
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-type", "text/xml; charset=UTF-8");
			if (newSession == false) {
				for (HttpCookie cookie : cookies)
					connection.setRequestProperty("Cookie", cookie.getName() + "=" + cookie.getValue());
				LOGGER.debug(String.format("using cookies: %s", cookies));
			}
			ostream = connection.getOutputStream();

			JAXBContext context = JAXBContext.newInstance(CTX);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://www.dns.pl/nask-epp-schema/epp-2.1 epp-2.1.xsd");
			m.marshal(rootElement, ostream);
			StringWriter writer = new StringWriter();
			m.marshal(rootElement, writer);
			LOGGER.debug("Sending XML message: " + writer.toString());

			ostream.flush();
			ostream.close();
			ostream = null;

			headers = connection.getHeaderFields();
			if (newSession) {
				List<String> cookiesList = null;
				cookies.clear();

				cookiesList = headers.get("Set-Cookie");

                if (cookiesList != null)
				    for (String cookie : cookiesList)
					    cookies.addAll(HttpCookie.parse(cookie));

				LOGGER.debug(String.format("setting cookies: %s", cookies));
			}

			LOGGER.debug(String.format("headers: %s", headers));

			istream = connection.getInputStream();

			context = JAXBContext.newInstance(CTX);
			Unmarshaller u = context.createUnmarshaller();
			doc = (JAXBElement<T>) u.unmarshal(istream);

			writer = new StringWriter();
			m.marshal(doc, writer);
			LOGGER.debug("Received XML message: " + writer.toString());
		} catch (IOException | JAXBException e) {
			throw new EPPLibException(e);
		} finally {
			if (ostream != null)
				try {
					ostream.close();
				} catch (IOException e) {
				}
			if (istream != null)
				try {
					istream.close();
				} catch (IOException e) {
				}
			if (connection != null)
				connection.disconnect();
		}

		if (doc == null)
			throw new EPPLibException("Null response");

		return doc.getValue();
	}

	public void eppContactAddStatus(String id, List<String> statuses) throws EPPLibException {
		eppContactChangeStatus(id, null, statuses);
	}

	public void eppContactChange(Contact contact) throws EPPLibException {
		checkSession();

		if (contact == null || contact.getId() == null)
			throw new IllegalArgumentException();

        pl.dns.nask_epp.contact.UpdateType update = new pl.dns.nask_epp.contact.UpdateType();
        pl.dns.nask_epp.contact.ChgType change = new pl.dns.nask_epp.contact.ChgType();
		update.setId(contact.getId());

		if (contact.getEmail() != null)
			change.setEmail(contact.getEmail());

		if (contact.getAuthInfo() != null) {
			pl.dns.nask_epp.contact.AuthInfoTypeWithoutRoid authInfoType = new pl.dns.nask_epp.contact.AuthInfoTypeWithoutRoid();
			authInfoType.setPw(contact.getAuthInfo());
			change.setAuthInfo(authInfoType);
		}

		if (contact.getLocPostalInfo() != null) {
			PostalInfoType postalInfoType = new PostalInfoType();
			postalInfoType.setName(contact.getLocPostalInfo().getName());
			postalInfoType.setOrg(contact.getLocPostalInfo().getOrg());
			postalInfoType.setType(PostalInfoEnumType.LOC);

			pl.dns.nask_epp.contact.AddrType addrType = new pl.dns.nask_epp.contact.AddrType();
			addrType.setCc(contact.getLocPostalInfo().getAddr().getCc());
			addrType.setCity(contact.getLocPostalInfo().getAddr().getCity());
			addrType.setPc(contact.getLocPostalInfo().getAddr().getPc());
			addrType.setSp(contact.getLocPostalInfo().getAddr().getSp());

			if (contact.getLocPostalInfo().getAddr().getStreet().size() < 1 || contact.getLocPostalInfo().getAddr().getStreet().size() > 3)
				throw new IllegalArgumentException("invalid number of street elements");

			addrType.getStreet().addAll(contact.getLocPostalInfo().getAddr().getStreet());

			postalInfoType.setAddr(addrType);
			change.getPostalInfo().add(postalInfoType);
		}

		if (contact.getIntPostalInfo() != null) {
            PostalInfoType postalInfoType = new PostalInfoType();
			postalInfoType.setName(contact.getIntPostalInfo().getName());
			postalInfoType.setOrg(contact.getIntPostalInfo().getOrg());
			postalInfoType.setType(PostalInfoEnumType.INT);

            pl.dns.nask_epp.contact.AddrType addrType = new pl.dns.nask_epp.contact.AddrType();
			addrType.setCc(contact.getIntPostalInfo().getAddr().getCc());
			addrType.setCity(contact.getIntPostalInfo().getAddr().getCity());
			addrType.setPc(contact.getIntPostalInfo().getAddr().getPc());
			addrType.setSp(contact.getIntPostalInfo().getAddr().getSp());

			if (contact.getIntPostalInfo().getAddr().getStreet().size() < 1 || contact.getIntPostalInfo().getAddr().getStreet().size() > 3)
				throw new IllegalArgumentException("invalid number of street elements");

			addrType.getStreet().addAll(contact.getIntPostalInfo().getAddr().getStreet());

			postalInfoType.setAddr(addrType);
			change.getPostalInfo().add(postalInfoType);
		}

		if (contact.getVoice() != null) {
			OptE164Type number = new OptE164Type();
			number.setValue(contact.getVoice());
			change.setVoice(number);
		}

		if (contact.getFax() != null) {
            OptE164Type number = new OptE164Type();
			number.setValue(contact.getFax());
			change.setFax(number);
		}

		CmdAllType allType = new CmdAllType();
		allType.setIndividual(contact.isIndividual());

        CmdUpdateType cmdUpdateType = new CmdUpdateType();
        cmdUpdateType.setUpdateGroupType(contactOF.createUpdate(update));

        CmdExtType cmdExtType = new CmdExtType();
        cmdExtType.getCmdExtGroupType().add(extconOF.createUpdate(allType));

		CommandType commandType = new CommandType();
		commandType.setUpdate(cmdUpdateType);
		commandType.setExtension(cmdExtType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public void eppContactChangeStatus(String id, List<String> removeStatus, List<String> addStatus) throws EPPLibException {
		checkSession();

		if (id == null || (removeStatus == null && addStatus == null))
			throw new IllegalArgumentException();

		pl.dns.nask_epp.contact.AddRemType removeType = new pl.dns.nask_epp.contact.AddRemType();
        pl.dns.nask_epp.contact.AddRemType addType = new pl.dns.nask_epp.contact.AddRemType();

		pl.dns.nask_epp.contact.UpdateType updateType = new pl.dns.nask_epp.contact.UpdateType();
		updateType.setId(id);

		if (removeStatus != null) {
			for (String remove : removeStatus) {
				pl.dns.nask_epp.contact.StatusType sType = new pl.dns.nask_epp.contact.StatusType();
				sType.setS(pl.dns.nask_epp.contact.StatusValueType.fromValue(remove));
				removeType.getStatus().add(sType);
			}

			updateType.setRem(removeType);
		}

		if (addStatus != null) {
			for (String add : addStatus) {
                pl.dns.nask_epp.contact.StatusType sType = new pl.dns.nask_epp.contact.StatusType();
                sType.setS(pl.dns.nask_epp.contact.StatusValueType.fromValue(add));
				addType.getStatus().add(sType);
			}

			updateType.setAdd(addType);
		}

        CmdUpdateType cmdUpdateType = new CmdUpdateType();
        cmdUpdateType.setUpdateGroupType(contactOF.createUpdate(updateType));

		CommandType commandType = new CommandType();
		commandType.setUpdate(cmdUpdateType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

    public boolean eppContactCheck(String id) throws EPPLibException {
        List<String> ids = new ArrayList<>();
        ids.add(id);
        Map<String, Boolean> res = eppContactCheck(ids);
        if (res == null)
            return false;

        if (res.get(id) == null)
            return false;

        return res.get(id);
    }

	public Map<String, Boolean> eppContactCheck(List<String> ids) throws EPPLibException {
		checkSession();

		if (ids == null || ids.isEmpty())
			throw new IllegalArgumentException();

		MIDType mIdType = new MIDType();

		for (String id : ids)
			mIdType.getId().add(id);

        CmdCheckType cmdCheckType = new CmdCheckType();
        cmdCheckType.setCheckGroupType(contactOF.createCheck(mIdType));

		CommandType commandType = new CommandType();
		commandType.setCheck(cmdCheckType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);

		HashMap<String, Boolean> result = new HashMap<>();
		pl.dns.nask_epp.contact.ChkDataType resData = getResult(response);

		for (pl.dns.nask_epp.contact.CheckType c : resData.getCd()) {
			String id = c.getId().getValue();
			Boolean avail = c.getId().isAvail();
			result.put(id, avail);
		}

		return result;
	}

	public void eppContactCreate(Contact contact) throws EPPLibException {
		checkSession();

		if (contact == null)
			throw new IllegalArgumentException();

		pl.dns.nask_epp.contact.CreateType create = new pl.dns.nask_epp.contact.CreateType();

		if (contact.getId() != null)
			create.setId(contact.getId());
		else
			throw new IllegalArgumentException("id is null");

		if (contact.getEmail() != null)
			create.setEmail(contact.getEmail());
		else
			throw new IllegalArgumentException("email is null");

		if (contact.getAuthInfo() != null) {
			pl.dns.nask_epp.contact.AuthInfoTypeWithoutRoid authInfoType = new pl.dns.nask_epp.contact.AuthInfoTypeWithoutRoid();
			authInfoType.setPw(contact.getAuthInfo());
			create.setAuthInfo(authInfoType);
		} else
			throw new IllegalArgumentException("authinfo is null");

		if (contact.getLocPostalInfo() == null && contact.getIntPostalInfo() == null)
			throw new IllegalArgumentException();

		if (contact.getLocPostalInfo() != null) {
			PostalInfoType postalInfoType = new PostalInfoType();
			postalInfoType.setName(contact.getLocPostalInfo().getName());
			postalInfoType.setOrg(contact.getLocPostalInfo().getOrg());
			postalInfoType.setType(PostalInfoEnumType.LOC);

			pl.dns.nask_epp.contact.AddrType addrType = new pl.dns.nask_epp.contact.AddrType();
			addrType.setCc(contact.getLocPostalInfo().getAddr().getCc());
			addrType.setCity(contact.getLocPostalInfo().getAddr().getCity());
			addrType.setPc(contact.getLocPostalInfo().getAddr().getPc());
			addrType.setSp(contact.getLocPostalInfo().getAddr().getSp());

			if (contact.getLocPostalInfo().getAddr().getStreet().size() < 1 || contact.getLocPostalInfo().getAddr().getStreet().size() > 3)
				throw new IllegalArgumentException("invalid number of street elements");

			addrType.getStreet().addAll(contact.getLocPostalInfo().getAddr().getStreet());

			postalInfoType.setAddr(addrType);
			create.getPostalInfo().add(postalInfoType);
		}

		if (contact.getIntPostalInfo() != null) {
			PostalInfoType postalInfoType = new PostalInfoType();
			postalInfoType.setName(contact.getIntPostalInfo().getName());
			postalInfoType.setOrg(contact.getIntPostalInfo().getOrg());
			postalInfoType.setType(PostalInfoEnumType.INT);

            pl.dns.nask_epp.contact.AddrType addrType = new pl.dns.nask_epp.contact.AddrType();
			addrType.setCc(contact.getIntPostalInfo().getAddr().getCc());
			addrType.setCity(contact.getIntPostalInfo().getAddr().getCity());
			addrType.setPc(contact.getIntPostalInfo().getAddr().getPc());
			addrType.setSp(contact.getIntPostalInfo().getAddr().getSp());

			if (contact.getIntPostalInfo().getAddr().getStreet().size() < 1 || contact.getIntPostalInfo().getAddr().getStreet().size() > 3)
				throw new IllegalArgumentException("invalid number of street elements");

			addrType.getStreet().addAll(contact.getIntPostalInfo().getAddr().getStreet());

			postalInfoType.setAddr(addrType);
			create.getPostalInfo().add(postalInfoType);
		}

		if (contact.getVoice() != null) {
			E164Type number = new E164Type();
			number.setValue(contact.getVoice());
			create.setVoice(number);
		}

		if (contact.getFax() != null) {
			E164Type number = new E164Type();
			number.setValue(contact.getFax());
			create.setFax(number);
		}

        CmdAllType cmdAllType = new CmdAllType();
        cmdAllType.setIndividual(contact.isIndividual());;

        CmdCreateType cmdCreateType = new CmdCreateType();
        cmdCreateType.setCreateGroupType(contactOF.createCreate(create));

        CmdExtType cmdExtType = new CmdExtType();
        cmdExtType.getCmdExtGroupType().add(extconOF.createCreate(cmdAllType));

		CommandType commandType = new CommandType();
		commandType.setCreate(cmdCreateType);
		commandType.setExtension(cmdExtType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public void eppContactDelete(String id) throws EPPLibException {
		checkSession();

		if (id == null)
			throw new IllegalArgumentException();

        DelSIDType sIdType = new DelSIDType();
		sIdType.setId(id);

        CmdDeleteType cmdDeleteType = new CmdDeleteType();
        cmdDeleteType.setDeleteGroupType(contactOF.createDelete(sIdType));

		CommandType commandType = new CommandType();
		commandType.setDelete(cmdDeleteType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public Contact eppContactInfo(String id) throws EPPLibException {
		return eppContactInfo(id, null, null);
	}

	public Contact eppContactInfo(String id, String authInfo) throws EPPLibException {
		return eppContactInfo(id, authInfo, null);
	}

	public Contact eppContactInfo(String id, String authInfo, String roid) throws EPPLibException {
		checkSession();

		if (id == null)
			throw new IllegalArgumentException();

		InfoSIDType sIdType = new InfoSIDType();
		sIdType.setId(id);

        CmdInfoType cmdInfoType = new CmdInfoType();
        cmdInfoType.setInfoGroupType(contactOF.createInfo(sIdType));

		CommandType commandType = new CommandType();
		commandType.setInfo(cmdInfoType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		if (authInfo != null) {
			InfoType infoType = new InfoType();
			pl.dns.nask_epp.extcon.AuthInfoType authInfoType = new pl.dns.nask_epp.extcon.AuthInfoType();
			PwAuthInfoType pwType = new PwAuthInfoType();
			pwType.setValue(authInfo);
			if (roid != null)
				pwType.setRoid(roid);
			authInfoType.setPw(pwType);
			infoType.setAuthInfo(authInfoType);

            CmdExtType cmdExtType = new CmdExtType();
            cmdExtType.getCmdExtGroupType().add(extconOF.createInfo(infoType));

			commandType.setExtension(cmdExtType);
		}

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		pl.dns.nask_epp.contact.InfDataType contactInfo = getResult(response);

		Contact c = new Contact(contactInfo.getId(), contactInfo.getEmail(), contactInfo.getAuthInfo().getPw().getValue());

		if (contactInfo.getFax() != null)
			c.setFax(contactInfo.getFax().getValue());
		if (contactInfo.getVoice() != null)
			c.setVoice(contactInfo.getVoice().getValue());

		for (PostalInfoType pi : contactInfo.getPostalInfo()) {
			PostalAddress ad = new PostalAddress();

			if (pi.getAddr() != null) {
				ad.setCc(pi.getAddr().getCc());
				ad.setCity(pi.getAddr().getCity());
				ad.setPc(pi.getAddr().getPc());
				ad.setSp(pi.getAddr().getSp());
				ad.setStreet(pi.getAddr().getStreet());
			}

			PostalInfo epi = new PostalInfo();
			epi.setName(pi.getName());
			epi.setOrg(pi.getOrg());
			epi.setAddr(ad);

			switch (pi.getType()) {
			case INT:
				c.setIntPostalInfo(epi);
				break;
			case LOC:
				c.setLocPostalInfo(epi);
				break;
			}
		}

		c.setClId(contactInfo.getClID());
		c.setCrId(contactInfo.getCrID());
		c.setRoid(contactInfo.getRoid());
		for (pl.dns.nask_epp.contact.StatusType t : contactInfo.getStatus())
			if (t != null && t.getS() != null)
                c.getStatus().add(t.getS().toString());
		c.setCrDate(contactInfo.getCrDate());
		c.setTrDate(contactInfo.getTrDate());
		c.setUpDate(contactInfo.getUpDate());
		c.setUpId(contactInfo.getUpID());

		RspAllType contactExtInfo = getExtension(response);
		c.setIndividual(contactExtInfo.isIndividual());

		return c;
	}

	public void eppContactRemoveStatus(String id, List<String> statuses) throws EPPLibException {
		eppContactChangeStatus(id, statuses, null);
	}

	public void eppDomainAddContacts(String name, List<DomainContact> contacts) throws EPPLibException {
		eppDomainChangeContacts(name, null, contacts);
	}

	public void eppDomainAddNameserver(String name, List<Host> hosts) throws EPPLibException {
		eppDomainChangeNameserver(name, null, hosts);
	}

	public void eppDomainAddStatus(String name, List<String> statuses) throws EPPLibException {
		eppDomainChangeStatus(name, null, statuses);
	}

	public void eppDomainChange(String name, Contact registrant) throws EPPLibException {
		eppDomainChange(name, registrant, null);
	}

	public void eppDomainChange(String name, Contact registrant, String authInfo) throws EPPLibException {
		checkSession();

		if (name == null || (registrant == null && authInfo == null))
			throw new IllegalArgumentException();

		pl.dns.nask_epp.domain.ChgType changeType = new pl.dns.nask_epp.domain.ChgType();
		pl.dns.nask_epp.domain.UpdateType updateType = new pl.dns.nask_epp.domain.UpdateType();
		updateType.setName(name);

		if (registrant != null)
			changeType.setRegistrant(registrant.getId());

		if (authInfo != null) {
			pl.dns.nask_epp.domain.AuthInfoTypeWithoutRoid authInfoType = new pl.dns.nask_epp.domain.AuthInfoTypeWithoutRoid();
			authInfoType.setPw(authInfo);
			changeType.setAuthInfo(authInfoType);
		}

		updateType.setChg(changeType);

        CmdUpdateType cmdUpdateType = new CmdUpdateType();
        cmdUpdateType.setUpdateGroupType(domainOF.createUpdate(updateType));

        CommandType commandType = new CommandType();
        commandType.setUpdate(cmdUpdateType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public void eppDomainChange(String name, String authInfo) throws EPPLibException {
		eppDomainChange(name, null, authInfo);
	}

	public void eppDomainChangeAdmin(String name, Contact oldContact, Contact newContact) throws EPPLibException {
		List<DomainContact> add = new ArrayList<>();
		List<DomainContact> remove = new ArrayList<>();
        if (oldContact != null)
		    remove.add(new DomainContact(oldContact, ContactType.ADMIN));
        if (newContact != null)
		    add.add(new DomainContact(newContact, ContactType.ADMIN));
		eppDomainChangeContacts(name, remove, add);
	}

	public void eppDomainChangeBilling(String name, Contact oldContact, Contact newContact) throws EPPLibException {
		List<DomainContact> add = new ArrayList<>();
		List<DomainContact> remove = new ArrayList<>();
        if (oldContact != null)
		    remove.add(new DomainContact(oldContact, ContactType.BILLING));
        if (newContact != null)
		    add.add(new DomainContact(newContact, ContactType.BILLING));
		eppDomainChangeContacts(name, remove, add);
	}

	public void eppDomainChangeContacts(String name, List<DomainContact> remove, List<DomainContact> add) throws EPPLibException {
		checkSession();

		if (name == null || (remove == null && add == null))
			throw new IllegalArgumentException();

		pl.dns.nask_epp.domain.AddRemType removeType = new pl.dns.nask_epp.domain.AddRemType();
		pl.dns.nask_epp.domain.AddRemType addType = new pl.dns.nask_epp.domain.AddRemType();

		pl.dns.nask_epp.domain.UpdateType updateType = new pl.dns.nask_epp.domain.UpdateType();
		updateType.setName(name);

		if (remove != null && !remove.isEmpty()) {
			for (DomainContact c : remove) {
				pl.dns.nask_epp.domain.ContactType contactType = new pl.dns.nask_epp.domain.ContactType();
				contactType.setType(ContactAttrType.TECH);
				contactType.setValue(c.getContact().getId());
				removeType.getContact().add(contactType);
			}

			updateType.setRem(removeType);
		}

		if (add != null && !add.isEmpty()) {
			for (DomainContact c : add) {
				pl.dns.nask_epp.domain.ContactType contactType = new pl.dns.nask_epp.domain.ContactType();
				contactType.setType(ContactAttrType.TECH);
				contactType.setValue(c.getContact().getId());
				addType.getContact().add(contactType);
			}

			updateType.setAdd(addType);
		}

        CmdUpdateType cmdUpdateType = new CmdUpdateType();
        cmdUpdateType.setUpdateGroupType(domainOF.createUpdate(updateType));

        CommandType commandType = new CommandType();
        commandType.setUpdate(cmdUpdateType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public void eppDomainChangeNameserver(String name, List<Host> remove, List<Host> add) throws EPPLibException {
		checkSession();

		if (name == null || (remove == null && add == null))
			throw new IllegalArgumentException();

		pl.dns.nask_epp.domain.AddRemType removeType = new pl.dns.nask_epp.domain.AddRemType();
		pl.dns.nask_epp.domain.AddRemType addType = new pl.dns.nask_epp.domain.AddRemType();

		pl.dns.nask_epp.domain.UpdateType updateType = new pl.dns.nask_epp.domain.UpdateType();
		updateType.setName(name);

		if (remove != null && !remove.isEmpty()) {
			for (Host h : remove)
				removeType.getNs().add(h.getName());

			updateType.setRem(removeType);
		}

		if (add != null && !add.isEmpty()) {
			for (Host h : add)
				addType.getNs().add(h.getName());

			updateType.setAdd(addType);
		}

        CmdUpdateType cmdUpdateType = new CmdUpdateType();
        cmdUpdateType.setUpdateGroupType(domainOF.createUpdate(updateType));

        CommandType commandType = new CommandType();
        commandType.setUpdate(cmdUpdateType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public void eppDomainChangeStatus(String name, List<String> removeStatus, List<String> addStatus) throws EPPLibException {
		checkSession();

		if (name == null || (removeStatus == null && addStatus == null))
			throw new IllegalArgumentException();

		pl.dns.nask_epp.domain.AddRemType removeType = new pl.dns.nask_epp.domain.AddRemType();
		pl.dns.nask_epp.domain.AddRemType addType = new pl.dns.nask_epp.domain.AddRemType();

		pl.dns.nask_epp.domain.UpdateType updateType = new pl.dns.nask_epp.domain.UpdateType();
		updateType.setName(name);

		if (removeStatus != null) {
			for (String remove : removeStatus) {
				pl.dns.nask_epp.domain.StatusType sType = new pl.dns.nask_epp.domain.StatusType();
				sType.setS(pl.dns.nask_epp.domain.StatusValueType.fromValue(remove));
				removeType.getStatus().add(sType);
			}

			updateType.setRem(removeType);
		}

		if (addStatus != null) {
			for (String add : addStatus) {
				pl.dns.nask_epp.domain.StatusType sType = new pl.dns.nask_epp.domain.StatusType();
				sType.setS(pl.dns.nask_epp.domain.StatusValueType.fromValue(add));
				addType.getStatus().add(sType);
			}

			updateType.setAdd(addType);
		}

        CmdUpdateType cmdUpdateType = new CmdUpdateType();
        cmdUpdateType.setUpdateGroupType(domainOF.createUpdate(updateType));

		CommandType commandType = new CommandType();
		commandType.setUpdate(cmdUpdateType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public void eppDomainChangeTech(String name, Contact oldContact, Contact newContact) throws EPPLibException {
		List<DomainContact> add = new ArrayList<>();
		List<DomainContact> remove = new ArrayList<>();
        if (oldContact != null)
		    remove.add(new DomainContact(oldContact, ContactType.TECH));
        if (newContact != null)
		    add.add(new DomainContact(newContact, ContactType.TECH));
		eppDomainChangeContacts(name, remove, add);
	}

    public boolean eppDomainCheck(String name) throws EPPLibException {
        List<String> names = new ArrayList<>();
        names.add(name);
        Map<String, Boolean> res = eppDomainCheck(names);
        if (res == null)
            return false;

        if (res.get(name) == null)
            return false;

        return res.get(name);
    }

    public Map<String, Boolean> eppDomainCheck(List<String> names) throws EPPLibException {
		checkSession();

		if (names == null || names.isEmpty())
			throw new IllegalArgumentException();

		pl.dns.nask_epp.domain.MNameType mNameType = new pl.dns.nask_epp.domain.MNameType();

		for (String name : names)
			mNameType.getName().add(name);

		CmdCheckType cmdCheckType = new CmdCheckType();
        cmdCheckType.setCheckGroupType(domainOF.createCheck(mNameType));

		CommandType commandType = new CommandType();
		commandType.setCheck(cmdCheckType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);

		HashMap<String, Boolean> result = new HashMap<>();
		pl.dns.nask_epp.domain.ChkDataType resData = getResult(response);

		for (pl.dns.nask_epp.domain.CheckType c : resData.getCd()) {
			String id = c.getName().getValue();
			Boolean avail = c.getName().isAvail();
            String reason = null;
		if (c.getReason() != null)
			reason = c.getReason().getValue();

            if (avail == false && reason != null && reason.equals("9072"))
                avail = true;

			result.put(id, avail);
		}

		return result;
	}

	public void eppDomainCreate(Domain d, Integer periodYears) throws EPPLibException {
		checkSession();

		if (d == null || d.getName() == null || d.getAuthInfo() == null)
			throw new IllegalArgumentException();

		pl.dns.nask_epp.domain.CreateType createType = new pl.dns.nask_epp.domain.CreateType();
		createType.setName(d.getName());

		pl.dns.nask_epp.domain.AuthInfoTypeWithoutRoid authInfoType = new pl.dns.nask_epp.domain.AuthInfoTypeWithoutRoid();
		authInfoType.setPw(d.getAuthInfo());
		createType.setAuthInfo(authInfoType);

		if (d.getNameservers() != null)
			for (Host h : d.getNameservers())
				createType.getNs().add(h.getName());

		if (d.getRegistrant() != null)
			createType.setRegistrant(d.getRegistrant().getId());

		pl.dns.nask_epp.extdom.CreateType extCreateType = new pl.dns.nask_epp.extdom.CreateType();
		if (d.getType() == DomainType.BOOK)
			extCreateType.setBook("");

        CmdExtType cmdExtType = new CmdExtType();
        cmdExtType.getCmdExtGroupType().add(extdomOF.createCreate(extCreateType));

        CmdCreateType cmdCreateType = new CmdCreateType();
        cmdCreateType.setCreateGroupType(domainOF.createCreate(createType));

		CommandType commandType = new CommandType();
		commandType.setCreate(cmdCreateType);
		commandType.setExtension(cmdExtType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public void eppDomainDelete(String name) throws EPPLibException {
		checkSession();

		if (name == null)
			throw new IllegalArgumentException();

		pl.dns.nask_epp.domain.SNameType sNameType = new pl.dns.nask_epp.domain.SNameType();
		sNameType.setName(name);

        CmdDeleteType cmdDeleteType = new CmdDeleteType();
        cmdDeleteType.setDeleteGroupType(domainOF.createDelete(sNameType));

		CommandType commandType = new CommandType();
		commandType.setDelete(cmdDeleteType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public Domain eppDomainInfo(String name) throws EPPLibException {
		return eppDomainInfo(name, null, null, "all");
	}

	public Domain eppDomainInfo(String name, String authInfo) throws EPPLibException {
		return eppDomainInfo(name, authInfo, null, "all");
	}

	public Domain eppDomainInfo(String name, String authInfo, String roid, String hosts) throws EPPLibException {
		checkSession();

		if (name == null)
			throw new IllegalArgumentException();

		pl.dns.nask_epp.domain.InfoNameType infoNameType = new pl.dns.nask_epp.domain.InfoNameType();
		infoNameType.setValue(name);

		if (hosts != null)
			infoNameType.setHosts(HostsType.fromValue(hosts));

		pl.dns.nask_epp.domain.InfoType infoType = new pl.dns.nask_epp.domain.InfoType();
		infoType.setName(infoNameType);

		if (authInfo != null) {
			pl.dns.nask_epp.domain.AuthInfoType authInfoType = new pl.dns.nask_epp.domain.AuthInfoType();
			PwAuthInfoType pwType = new PwAuthInfoType();
			pwType.setValue(authInfo);
			if (roid != null)
				pwType.setRoid(roid);
			authInfoType.setPw(pwType);
			infoType.setAuthInfo(authInfoType);
		}

        CmdInfoType cmdInfoType = new CmdInfoType();
        cmdInfoType.setInfoGroupType(domainOF.createInfo(infoType));

		CommandType commandType = new CommandType();
		commandType.setInfo(cmdInfoType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);

		pl.dns.nask_epp.domain.InfDataType domainInfo = getResult(response);
		Domain d = null;

		if (domainInfo.getAuthInfo() != null && domainInfo.getAuthInfo().getPw() != null)
			d = new Domain(domainInfo.getName(), domainInfo.getAuthInfo().getPw().getValue());
		else
			d = new Domain(domainInfo.getName(), null);

		for (pl.dns.nask_epp.domain.ContactType c : domainInfo.getContact()) {
			String s = c.getValue();
			if (s == null)
				continue;
			Contact cnt = new Contact(s, null, null);
			d.setTech(cnt);
		}

		for (String ns : domainInfo.getNs()) {
			Host h = new Host(ns);
			d.addNameserver(h);
		}

		d.setExDate(domainInfo.getExDate());
		if (domainInfo.getRegistrant() != null) {
			Contact c = new Contact(domainInfo.getRegistrant(), null, null);
			d.setRegistrant(c);
		}
		if (domainInfo.getAuthInfo() != null && domainInfo.getAuthInfo().getPw() != null)
			d.setRoid(domainInfo.getAuthInfo().getPw().getRoid());
		d.setClId(domainInfo.getClID());
		d.setCrId(domainInfo.getCrID());
		d.setRoid(domainInfo.getRoid());
		for (pl.dns.nask_epp.domain.StatusType t : domainInfo.getStatus())
			d.getStatus().add(t.getS().toString());
		d.setCrDate(domainInfo.getCrDate());
		d.setTrDate(domainInfo.getTrDate());
		d.setUpDate(domainInfo.getUpDate());
		d.setUpId(domainInfo.getUpID());

		return d;
	}

	public void eppDomainRemoveContacts(String name, List<DomainContact> contacts) throws EPPLibException {
		eppDomainChangeContacts(name, contacts, null);
	}

	public void eppDomainRemoveNameserver(String name, List<Host> hosts) throws EPPLibException {
		eppDomainChangeNameserver(name, hosts, null);
	}

	public void eppDomainRemoveStatus(String name, List<String> statuses) throws EPPLibException {
		eppDomainChangeStatus(name, statuses, null);
	}

	public void eppDomainRenew(String name, Date expDate, Integer period, boolean reactivate) throws EPPLibException {
		checkSession();

		if (name == null || expDate == null || period == null)
			throw new IllegalArgumentException();

		pl.dns.nask_epp.domain.RenewType renew = new pl.dns.nask_epp.domain.RenewType();
		renew.setName(name);

		pl.dns.nask_epp.domain.PeriodType periodType = new pl.dns.nask_epp.domain.PeriodType();
		periodType.setUnit(PUnitType.Y);
		periodType.setValue(period);
		renew.setPeriod(periodType);

		GregorianCalendar c = new GregorianCalendar();
		c.setTime(expDate);
		c.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			renew.setCurExpDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
		} catch (DatatypeConfigurationException e) {
			throw new EPPLibException(e);
		}

        CmdRenewType cmdRenewType = new CmdRenewType();
        cmdRenewType.setRenewGroupType(domainOF.createRenew(renew));

		CommandType commandType = new CommandType();
		commandType.setRenew(cmdRenewType);

		if (reactivate) {
			pl.dns.nask_epp.extdom.RenewType extRenewType = new pl.dns.nask_epp.extdom.RenewType();
			extRenewType.setReactivate(true);

            CmdExtType cmdExtType = new CmdExtType();
            cmdExtType.getCmdExtGroupType().add(extdomOF.createRenew(extRenewType));

			commandType.setExtension(cmdExtType);
		}

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	private void eppDomainTransfer(TransferOpType op, String name, String authInfo, String roid, Integer period, boolean resend) throws EPPLibException {
		checkSession();

		if (name == null)
			throw new IllegalArgumentException();

		pl.dns.nask_epp.domain.TransferType transfer = new pl.dns.nask_epp.domain.TransferType();
		transfer.setName(name);

		if (authInfo != null) {
			pl.dns.nask_epp.domain.AuthInfoType authInfoType = new pl.dns.nask_epp.domain.AuthInfoType();
			PwAuthInfoType pwType = new PwAuthInfoType();
			pwType.setValue(authInfo);
			if (roid != null)
				pwType.setRoid(roid);
			authInfoType.setPw(pwType);
			transfer.setAuthInfo(authInfoType);
		}

		if (op == TransferOpType.REQUEST && period != null) {
			pl.dns.nask_epp.domain.PeriodType periodType = new pl.dns.nask_epp.domain.PeriodType();
			periodType.setUnit(PUnitType.Y);
			periodType.setValue(period);
			transfer.setPeriod(periodType);
		}

        CmdTransferType cmdTransferType = new CmdTransferType();
        cmdTransferType.setOp(op);
        cmdTransferType.setTransferGroupType(domainOF.createTransfer(transfer));

		CommandType commandType = new CommandType();
		commandType.setTransfer(cmdTransferType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		if (op == TransferOpType.REQUEST && resend == true) {
            pl.dns.nask_epp.extdom.TransferType extTransfer = new pl.dns.nask_epp.extdom.TransferType();
            extTransfer.setResendConfirmationRequest("");

            CmdExtType cmdExtType = new CmdExtType();
            cmdExtType.getCmdExtGroupType().add(extdomOF.createTransfer(extTransfer));

            commandType.setExtension(cmdExtType);
		}

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public void eppDomainTransferCancel(String name, String authInfo, String roid) throws EPPLibException {
		eppDomainTransfer(TransferOpType.CANCEL, name, authInfo, roid, null, false);
	}

	public void eppDomainTransferQuery(String name, String authInfo, String roid) throws EPPLibException {
		eppDomainTransfer(TransferOpType.QUERY, name, authInfo, roid, null, false);
	}

	public void eppDomainTransferRequest(String name, String authInfo, String roid) throws EPPLibException {
		eppDomainTransfer(TransferOpType.REQUEST, name, authInfo, roid, null, false);
	}

	public void eppDomainTransferRequest(String name, String authInfo, String roid, boolean resend) throws EPPLibException {
		eppDomainTransfer(TransferOpType.REQUEST, name, authInfo, roid, null, resend);
	}

	public void eppDomainTransferRequest(String name, String authInfo, String roid, Integer period) throws EPPLibException {
		eppDomainTransfer(TransferOpType.REQUEST, name, authInfo, roid, period, false);
	}

    public void eppDomainTransferRequest(String name, String authInfo, String roid, Integer period, boolean resend) throws EPPLibException {
        eppDomainTransfer(TransferOpType.REQUEST, name, authInfo, roid, period, resend);
    }

    public void eppFutureChange(String name, Contact registrant, String authInfo) throws EPPLibException {
		checkSession();

		if (name == null || (registrant == null && authInfo == null))
			throw new IllegalArgumentException();

		pl.dns.nask_epp.future.ChgType changeType = new pl.dns.nask_epp.future.ChgType();
		pl.dns.nask_epp.future.UpdateType updateType = new pl.dns.nask_epp.future.UpdateType();
		updateType.setName(name);

		if (registrant != null)
			changeType.setRegistrant(registrant.getId());

		if (authInfo != null) {
			pl.dns.nask_epp.future.AuthInfoTypeWithoutRoid authInfoType = new AuthInfoTypeWithoutRoid();
			authInfoType.setPw(authInfo);
			changeType.setAuthInfo(authInfoType);
		}

		updateType.setChg(changeType);

        CmdUpdateType cmdUpdateType = new CmdUpdateType();
        cmdUpdateType.setUpdateGroupType(futureOF.createUpdate(updateType));

		CommandType commandType = new CommandType();
		commandType.setUpdate(cmdUpdateType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public Map<String, Boolean> eppFutureCheck(List<String> names) throws EPPLibException {
		checkSession();

		if (names == null || names.isEmpty())
			throw new IllegalArgumentException();

		pl.dns.nask_epp.future.MNameType mNameType = new pl.dns.nask_epp.future.MNameType();

		for (String name : names)
			mNameType.getName().add(name);


        CmdCheckType cmdCheckType = new CmdCheckType();
        cmdCheckType.setCheckGroupType(futureOF.createCheck(mNameType));

		CommandType commandType = new CommandType();
		commandType.setCheck(cmdCheckType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);

		HashMap<String, Boolean> result = new HashMap<>();
		pl.dns.nask_epp.future.ChkDataType resData = getResult(response);

		for (pl.dns.nask_epp.future.CheckType c : resData.getCd()) {
			String id = c.getName().getValue();
			Boolean avail = c.getName().isAvail();
			result.put(id, avail);
		}

		return result;
	}

	public void eppFutureCreate(Future f) throws EPPLibException {
		checkSession();

		if (f == null || f.getName() == null || f.getAuthInfo() == null)
			throw new IllegalArgumentException();

		pl.dns.nask_epp.future.CreateType createType = new pl.dns.nask_epp.future.CreateType();
		createType.setName(f.getName());

		pl.dns.nask_epp.future.AuthInfoTypeWithoutRoid authInfoType = new AuthInfoTypeWithoutRoid();
		authInfoType.setPw(f.getAuthInfo());
		createType.setAuthInfo(authInfoType);

		if (f.getPeriodYears() != null) {
			pl.dns.nask_epp.future.PeriodType period = new pl.dns.nask_epp.future.PeriodType();
			period.setUnit(pl.dns.nask_epp.future.PUnitType.Y);
			period.setValue(f.getPeriodYears());
			createType.setPeriod(period);
		}

		if (f.getRegistrant() != null)
			createType.setRegistrant(f.getRegistrant().getId());

        CmdCreateType cmdCreateType = new CmdCreateType();
        cmdCreateType.setCreateGroupType(futureOF.createCreate(createType));

		CommandType commandType = new CommandType();
		commandType.setCreate(cmdCreateType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public void eppFutureDelete(String name) throws EPPLibException {
		checkSession();

		if (name == null)
			throw new IllegalArgumentException();

		pl.dns.nask_epp.future.SNameType sNameType = new pl.dns.nask_epp.future.SNameType();
		sNameType.setName(name);

        CmdDeleteType cmdDeleteType = new CmdDeleteType();
        cmdDeleteType.setDeleteGroupType(futureOF.createDelete(sNameType));

		CommandType commandType = new CommandType();
		commandType.setDelete(cmdDeleteType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public Future eppFutureInfo(String name, String authInfo, String roid) throws EPPLibException {
		checkSession();

		if (name == null)
			throw new IllegalArgumentException();

		pl.dns.nask_epp.future.InfoNameType infoNameType = new pl.dns.nask_epp.future.InfoNameType();
		infoNameType.setValue(name);

		pl.dns.nask_epp.future.InfoType infoType = new pl.dns.nask_epp.future.InfoType();
		infoType.setName(infoNameType);

		if (authInfo != null) {
			pl.dns.nask_epp.future.AuthInfoType authInfoType = new pl.dns.nask_epp.future.AuthInfoType();
			PwAuthInfoType pwType = new PwAuthInfoType();
			pwType.setValue(authInfo);
			if (roid != null)
				pwType.setRoid(roid);
			authInfoType.setPw(pwType);
			infoType.setAuthInfo(authInfoType);
		}

        CmdInfoType cmdInfoType = new CmdInfoType();
        cmdInfoType.setInfoGroupType(futureOF.createInfo(infoType));

		CommandType commandType = new CommandType();
		commandType.setInfo(cmdInfoType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);

		pl.dns.nask_epp.future.InfDataType futureInfo = getResult(response);

		Future f = new Future(futureInfo.getName(), futureInfo.getAuthInfo().getPw().getValue());

		if (authInfo == null)
			if (futureInfo.getRegistrant() != null)
				f.setRegistrant(eppContactInfo(futureInfo.getRegistrant()));
		f.setRoid(futureInfo.getAuthInfo().getPw().getRoid());
		f.setExDate(futureInfo.getExDate());

		f.setClId(futureInfo.getClID());
		f.setCrId(futureInfo.getCrID());
		f.setRoid(futureInfo.getRoid());
		f.setCrDate(futureInfo.getCrDate());
		f.setTrDate(futureInfo.getTrDate());
		f.setUpDate(futureInfo.getUpDate());
		f.setUpId(futureInfo.getUpID());

		return f;
	}

	public void eppFutureRenew(String name, Date expDate, Integer period) throws EPPLibException {
		checkSession();

		if (name == null || expDate == null || period == null)
			throw new IllegalArgumentException();

		pl.dns.nask_epp.future.RenewType renew = new pl.dns.nask_epp.future.RenewType();
		renew.setName(name);

		pl.dns.nask_epp.future.PeriodType periodType = new pl.dns.nask_epp.future.PeriodType();
		periodType.setUnit(pl.dns.nask_epp.future.PUnitType.Y);
		periodType.setValue(period);
		renew.setPeriod(periodType);

		GregorianCalendar c = new GregorianCalendar();
		c.setTime(expDate);
		c.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			renew.setCurExpDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
		} catch (DatatypeConfigurationException e) {
			throw new EPPLibException(e);
		}

        CmdRenewType cmdRenewType = new CmdRenewType();
        cmdRenewType.setRenewGroupType(futureOF.createRenew(renew));

		CommandType commandType = new CommandType();
		commandType.setRenew(cmdRenewType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	private void eppFutureTransfer(TransferOpType op, String name, String authInfo, String roid, Integer period, boolean resend) throws EPPLibException {
		checkSession();

		if (name == null)
			throw new IllegalArgumentException();

		pl.dns.nask_epp.future.TransferType transfer = new pl.dns.nask_epp.future.TransferType();
		transfer.setName(name);

		if (authInfo != null) {
			pl.dns.nask_epp.future.AuthInfoType authInfoType = new pl.dns.nask_epp.future.AuthInfoType();
			PwAuthInfoType pwType = new PwAuthInfoType();
			pwType.setValue(authInfo);
			if (roid != null)
				pwType.setRoid(roid);
			authInfoType.setPw(pwType);
			transfer.setAuthInfo(authInfoType);
		}

		if (op == TransferOpType.REQUEST && period != null) {
			pl.dns.nask_epp.future.PeriodType periodType = new pl.dns.nask_epp.future.PeriodType();
			periodType.setUnit(pl.dns.nask_epp.future.PUnitType.Y);
			periodType.setValue(period);
			transfer.setPeriod(periodType);
		}

        CmdTransferType cmdTransferType = new CmdTransferType();
        cmdTransferType.setTransferGroupType(futureOF.createTransfer(transfer));
        cmdTransferType.setOp(op);

		CommandType commandType = new CommandType();
		commandType.setTransfer(cmdTransferType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		if (op == TransferOpType.REQUEST && resend == true) {
			pl.dns.nask_epp.extfut.TransferType extTransfer = new pl.dns.nask_epp.extfut.TransferType();
			extTransfer.setResendConfirmationRequest("");

            CmdExtType cmdExtType = new CmdExtType();
            cmdExtType.getCmdExtGroupType().add(extfutOF.createTransfer(extTransfer));

			commandType.setExtension(cmdExtType);
		}

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public void eppFutureTransferCancel(String name, String authInfo, String roid) throws EPPLibException {
		eppFutureTransfer(TransferOpType.CANCEL, name, authInfo, roid, null, false);
	}

	public void eppFutureTransferQuery(String name, String authInfo, String roid) throws EPPLibException {
		eppFutureTransfer(TransferOpType.QUERY, name, authInfo, roid, null, false);
	}

	public void eppFutureTransferRequest(String name, String authInfo, String roid) throws EPPLibException {
		eppFutureTransfer(TransferOpType.REQUEST, name, authInfo, roid, null, false);
	}

	public void eppFutureTransferRequest(String name, String authInfo, String roid, boolean resend) throws EPPLibException {
		eppFutureTransfer(TransferOpType.REQUEST, name, authInfo, roid, null, resend);
	}

	public void eppFutureTransferRequest(String name, String authInfo, String roid, Integer period) throws EPPLibException {
		eppFutureTransfer(TransferOpType.REQUEST, name, authInfo, roid, period, false);
	}

	public void eppAckMessage(String id) throws EPPLibException {
        CmdPollType cmdPollType = new CmdPollType();
        cmdPollType.setOp(PollOpType.ACK);
        cmdPollType.setMsgID(id);

        CommandType commandType = new CommandType();
        commandType.setPoll(cmdPollType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public Message eppGetMessage() throws EPPLibException {
		checkSession();

		Message message = null;
		String msg = null;

        CmdPollType cmdPollType = new CmdPollType();
        cmdPollType.setOp(PollOpType.REQ);

		CommandType commandType = new CommandType();
		commandType.setPoll(cmdPollType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);

		if (response.getResponse().getMsgQ() != null) {
			String id = response.getResponse().getMsgQ().getId();
            msg = response.getResponse().getMsgQ().getMsg().getValue();
			@SuppressWarnings("rawtypes")
            JAXBElement objType = (JAXBElement) response.getResponse().getResData().getRspDataGroupType().get(0);

			String prefix = objType.getName().getNamespaceURI();
			String local = objType.getName().getLocalPart();

			LOGGER.debug(String.format("received message {%s}%s in poll response", prefix, local));

			if ("http://www.dns.pl/nask-epp-schema/extdom-2.1".equals(prefix) && "dlgData".equals(local)) {
				pl.dns.nask_epp.extdom.DlgDataType dlgData = getPollResponse(response);
				BrokenDelegationMessage dmsg = new BrokenDelegationMessage();
				dmsg.getDomains().addAll(dlgData.getName());
				dmsg.setNameserver(dlgData.getNs());
				message = dmsg;
			} else if ("http://www.dns.pl/nask-epp-schema/domain-2.1".equals(prefix) && "trnData".equals(local)) {
				pl.dns.nask_epp.domain.TrnDataType trnData = getPollResponse(response);
				TransferStatusMessage tmsg = new TransferStatusMessage();
				tmsg.setName(trnData.getName());
				tmsg.setStatus(TransferStatus.valueOf(trnData.getTrStatus().value().toUpperCase()));
				tmsg.setType(ObjectType.DOMAIN);
				message = tmsg;
			} else if ("http://www.dns.pl/nask-epp-schema/future-2.1".equals(prefix) && "trnData".equals(local)) {
				pl.dns.nask_epp.future.TrnDataType trnData = getPollResponse(response);
				TransferStatusMessage tmsg = new TransferStatusMessage();
				tmsg.setName(trnData.getName());
				tmsg.setStatus(TransferStatus.valueOf(trnData.getTrStatus().value().toUpperCase()));
				tmsg.setType(ObjectType.FUTURE);
				message = tmsg;
			} else if ("http://www.dns.pl/nask-epp-schema/extdom-2.1".equals(prefix) && "expData".equals(local)) {
				pl.dns.nask_epp.extdom.ExpDataType expData = getPollResponse(response);
				ExpirationMessage emsg = new ExpirationMessage();
				emsg.getDomains().addAll(expData.getName());
				message = emsg;
			} else if ("http://www.dns.pl/nask-epp-schema/domain-2.1".equals(prefix) && "creData".equals(local)) {
				pl.dns.nask_epp.domain.CreDataType creData = getPollResponse(response);
				DomainMessage dmsg = new DomainMessage();
				dmsg.setName(creData.getName());
				dmsg.setType(DomainMessageType.FUTURE_FINALIZED);
				message = dmsg;
			} else if ("http://www.dns.pl/nask-epp-schema/extdom-2.1".equals(prefix) && "pollDomainBlocked".equals(local)) {
				pl.dns.nask_epp.extdom.PollDomBlockType blockType = getPollResponse(response);
				DomainMessage dmsg = new DomainMessage();
				dmsg.setName(blockType.getDomain().getName());
				dmsg.setType(DomainMessageType.DOMAIN_BLOCKED);
				message = dmsg;
			} else if ("http://www.dns.pl/nask-epp-schema/extdom-2.1".equals(prefix) && "pollDomainUnblocked".equals(local)) {
				pl.dns.nask_epp.extdom.PollDomBlockType blockType = getPollResponse(response);
				DomainMessage dmsg = new DomainMessage();
				dmsg.setName(blockType.getDomain().getName());
				dmsg.setType(DomainMessageType.DOMAIN_UNBLOCKED);
				message = dmsg;
			} else if ("http://www.dns.pl/nask-epp-schema/extdom-2.1".equals(prefix) && "pollFutureRemoved".equals(local)) {
				pl.dns.nask_epp.extdom.PollFutRemType remType = getPollResponse(response);
				DomainMessage dmsg = new DomainMessage();
				dmsg.setName(remType.getFuture().getName());
				dmsg.setType(DomainMessageType.FUTURE_REMOVED);
				message = dmsg;
			} else if ("http://www.dns.pl/nask-epp-schema/extdom-2.1".equals(prefix) && "pollDomainJudicialRemoved".equals(local)) {
				pl.dns.nask_epp.extdom.PollDomJudRemType remType = getPollResponse(response);
				DomainMessage dmsg = new DomainMessage();
				dmsg.setName(remType.getDomain().getName());
				dmsg.setType(DomainMessageType.DOMAIN_JUDICIAL_REMOVED);
				message = dmsg;
			} else if ("http://www.dns.pl/nask-epp-schema/extdom-2.1".equals(prefix) && "pollDomainAutoRenewed".equals(local)) {
				pl.dns.nask_epp.extdom.PollDomainAutoRenewedType renType = getPollResponse(response);
				DomainMessage dmsg = new DomainMessage();
				dmsg.setName(renType.getName());
				dmsg.setType(DomainMessageType.DOMAIN_AUTO_RENEWED);
				message = dmsg;
			} else if ("http://www.dns.pl/nask-epp-schema/extdom-2.1".equals(prefix) && "pollDomainAutoRenewFailed".equals(local)) {
				pl.dns.nask_epp.extdom.PollDomainAutoRenewFailedType renType = getPollResponse(response);
				DomainMessage dmsg = new DomainMessage();
				dmsg.setName(renType.getName());
				dmsg.setType(DomainMessageType.DOMAIN_AUTO_RENEW_FAILED);
				message = dmsg;
			} else if ("http://www.dns.pl/nask-epp-schema/extdom-2.1".equals(prefix) && "pollAuthInfo".equals(local)) {
				pl.dns.nask_epp.extdom.PollAuthInfoDataType aiType = getPollResponse(response);
				AuthInfoMessage amsg = new AuthInfoMessage();
				amsg.setName(aiType.getDomain().getName());
				amsg.setAuthInfo(aiType.getDomain().getAuthInfo().getPw().getValue());
				message = amsg;
			} else
				message = new Message();

			message.setMessage(msg);
			message.setId(id);
		}

		return message;
	}

	public BigDecimal eppGetPaymentFunds() throws EPPLibException {
		checkSession();

		pl.dns.nask_epp.extreport.PaymentFunds payment = new pl.dns.nask_epp.extreport.PaymentFunds();
        payment.setAccountType("DOMAIN");

		pl.dns.nask_epp.extreport.Prepaid prepaid = new pl.dns.nask_epp.extreport.Prepaid();
		prepaid.setPaymentFunds(payment);

		pl.dns.nask_epp.extreport.ReportType reportType = new pl.dns.nask_epp.extreport.ReportType();
		reportType.setPrepaid(prepaid);

        ExtType extType = new ExtType();
        extType.getExtGroupType().add(extrepOF.createReport(reportType));

		EppType eppType = new EppType();
		eppType.setExtension(extType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);

		pl.dns.nask_epp.extreport.ReportDataType reportDataType = getExtension(response);
        BigDecimal leftFunds = reportDataType.getPaymentFundsDataRsp().getPaymentFundsData().get(0).getCurrentBalance();

		return leftFunds;
	}

	public void eppHello() throws EPPLibException {
        HelloType helloType = new HelloType();

        EppType eppType = new EppType();
		eppType.setHello(helloType);

		doRequest(eppOF.createEpp(eppType), false);
	}

	public void eppHostAddIp(String name, List<InetAddress> adresses) throws EPPLibException {
		eppHostChangeIp(name, null, adresses);
	}

	public void eppHostAddIp(String name, String host) throws EPPLibException, UnknownHostException {
		List<InetAddress> addresses = new ArrayList<>();

		addresses.add(InetAddress.getByName(host));
		eppHostChangeIp(name, null, addresses);
	}

	public void eppHostAddStatus(String name, List<String> statuses) throws EPPLibException {
		eppHostChangeStatus(name, null, statuses);
	}

	public void eppHostChangeIp(String name, List<InetAddress> remove, List<InetAddress> add) throws EPPLibException {
		checkSession();

		if (name == null || (remove == null && add == null))
			throw new IllegalArgumentException();

		pl.dns.nask_epp.host.AddRemType removeType = new pl.dns.nask_epp.host.AddRemType();
		pl.dns.nask_epp.host.AddRemType addType = new pl.dns.nask_epp.host.AddRemType();

		pl.dns.nask_epp.host.UpdateType updateType = new pl.dns.nask_epp.host.UpdateType();
		updateType.setName(name);

		if (remove != null && !remove.isEmpty()) {
			for (InetAddress addr : remove) {
				pl.dns.nask_epp.host.AddrType addrType = new pl.dns.nask_epp.host.AddrType();
				if (addr instanceof Inet4Address)
					addrType.setIp(IpType.V_4);
				if (addr instanceof Inet6Address)
					addrType.setIp(IpType.V_6);
				addrType.setValue(addr.getHostAddress());
				removeType.getAddr().add(addrType);

			}

			updateType.setRem(removeType);
		}

		if (add != null && !add.isEmpty()) {
			for (InetAddress addr : add) {
				pl.dns.nask_epp.host.AddrType addrType = new pl.dns.nask_epp.host.AddrType();
				if (addr instanceof Inet4Address)
					addrType.setIp(IpType.V_4);
				if (addr instanceof Inet6Address)
					addrType.setIp(IpType.V_6);
				addrType.setValue(addr.getHostAddress());
				addType.getAddr().add(addrType);

			}

			updateType.setAdd(addType);
		}

        CmdUpdateType cmdUpdateType = new CmdUpdateType();
        cmdUpdateType.setUpdateGroupType(hostOF.createUpdate(updateType));

        CommandType commandType = new CommandType();
        commandType.setUpdate(cmdUpdateType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public void eppHostChangeName(String name, String newName) throws EPPLibException {
		checkSession();

		if (name == null || newName == null)
			throw new IllegalArgumentException();

		pl.dns.nask_epp.host.UpdateType updateType = new pl.dns.nask_epp.host.UpdateType();
		pl.dns.nask_epp.host.ChgType chgType = new pl.dns.nask_epp.host.ChgType();
		chgType.setName(newName);

		updateType.setName(name);
		updateType.setChg(chgType);

        CmdUpdateType cmdUpdateType = new CmdUpdateType();
        cmdUpdateType.setUpdateGroupType(hostOF.createUpdate(updateType));

        CommandType commandType = new CommandType();
        commandType.setUpdate(cmdUpdateType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public void eppHostChangeStatus(String name, List<String> removeStatus, List<String> addStatus) throws EPPLibException {
		checkSession();

		if (name == null || (removeStatus == null && addStatus == null))
			throw new IllegalArgumentException();

		pl.dns.nask_epp.host.AddRemType removeType = new pl.dns.nask_epp.host.AddRemType();
		pl.dns.nask_epp.host.AddRemType addType = new pl.dns.nask_epp.host.AddRemType();

		pl.dns.nask_epp.host.UpdateType updateType = new pl.dns.nask_epp.host.UpdateType();
		updateType.setName(name);

		if (removeStatus != null) {
			for (String remove : removeStatus) {
				pl.dns.nask_epp.host.StatusType sType = new pl.dns.nask_epp.host.StatusType();
				sType.setS(pl.dns.nask_epp.host.StatusValueType.fromValue(remove));
				removeType.getStatus().add(sType);
			}

			updateType.setRem(removeType);
		}

		if (addStatus != null) {
			for (String add : addStatus) {
				pl.dns.nask_epp.host.StatusType sType = new pl.dns.nask_epp.host.StatusType();
				sType.setS(pl.dns.nask_epp.host.StatusValueType.fromValue(add));
				addType.getStatus().add(sType);
			}

			updateType.setAdd(addType);
		}

        CmdUpdateType cmdUpdateType = new CmdUpdateType();
        cmdUpdateType.setUpdateGroupType(hostOF.createUpdate(updateType));

		CommandType commandType = new CommandType();
		commandType.setUpdate(cmdUpdateType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

    public boolean eppHostCheck(String name) throws EPPLibException {
        List<String> names = new ArrayList<>();
        names.add(name);
        Map<String, Boolean> res = eppHostCheck(names);
        if (res == null)
            return false;

        if (res.get(name) == null)
            return false;

        return res.get(name);
    }

    public Map<String, Boolean> eppHostCheck(List<String> names) throws EPPLibException {
		checkSession();

		if (names == null || names.isEmpty())
			throw new IllegalArgumentException();

		pl.dns.nask_epp.host.MNameType mNameType = new pl.dns.nask_epp.host.MNameType();

		for (String name : names)
			mNameType.getName().add(name);

        CmdCheckType cmdCheckType = new CmdCheckType();
        cmdCheckType.setCheckGroupType(hostOF.createCheck(mNameType));

		CommandType commandType = new CommandType();
		commandType.setCheck(cmdCheckType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);

		HashMap<String, Boolean> result = new HashMap<>();
		pl.dns.nask_epp.host.ChkDataType resData = getResult(response);

		for (pl.dns.nask_epp.host.CheckType c : resData.getCd()) {
			String id = c.getName().getValue();
			Boolean avail = c.getName().isAvail();
			result.put(id, avail);
		}

		return result;
	}

	public void eppHostCreate(Host h) throws EPPLibException {
		checkSession();

		if (h == null || h.getName() == null)
			throw new IllegalArgumentException();

		pl.dns.nask_epp.host.CreateType createType = new pl.dns.nask_epp.host.CreateType();
		createType.setName(h.getName());

		for (InetAddress addr : h.getAddresses()) {
			pl.dns.nask_epp.host.AddrType addrType = new pl.dns.nask_epp.host.AddrType();
			if (addr instanceof Inet4Address)
				addrType.setIp(IpType.V_4);
			if (addr instanceof Inet6Address)
				addrType.setIp(IpType.V_6);
			addrType.setValue(addr.getHostAddress());
			createType.getAddr().add(addrType);
		}

        CmdCreateType cmdCreateType = new CmdCreateType();
        cmdCreateType.setCreateGroupType(hostOF.createCreate(createType));

		CommandType commandType = new CommandType();
		commandType.setCreate(cmdCreateType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public void eppHostDelete(String name) throws EPPLibException {
		checkSession();

		if (name == null)
			throw new IllegalArgumentException();

		DelSNameType sNameType = new DelSNameType();
		sNameType.setName(name);

        CmdDeleteType cmdDeleteType = new CmdDeleteType();
        cmdDeleteType.setDeleteGroupType(hostOF.createDelete(sNameType));

		CommandType commandType = new CommandType();
		commandType.setDelete(cmdDeleteType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);
	}

	public Host eppHostInfo(String name) throws EPPLibException {
		checkSession();

		if (name == null)
			throw new IllegalArgumentException();

		InfoSNameType sNameType = new InfoSNameType();
		sNameType.setName(name);

		CmdInfoType cmdInfoType = new CmdInfoType();
        cmdInfoType.setInfoGroupType(hostOF.createInfo(sNameType));

		CommandType commandType = new CommandType();
		commandType.setInfo(cmdInfoType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		pl.dns.nask_epp.host.InfDataType hostInfo = getResult(response);

		Host h = new Host(hostInfo.getName());
		for (pl.dns.nask_epp.host.AddrType a : hostInfo.getAddr())
			try {
				h.addAddress(a.getValue());
			} catch (UnknownHostException e) {
				throw new EPPLibException(e);
			}

		h.setClId(hostInfo.getClID());
		h.setCrId(hostInfo.getCrID());
		h.setRoid(hostInfo.getRoid());
		for (pl.dns.nask_epp.host.StatusType t : hostInfo.getStatus())
			h.getStatus().add(t.getS().toString());
		h.setCrDate(hostInfo.getCrDate());
		h.setTrDate(hostInfo.getTrDate());
		h.setUpDate(hostInfo.getUpDate());
		h.setUpId(hostInfo.getUpID());

		return h;
	}

	public void eppHostRemoveIp(String name, List<InetAddress> adresses) throws EPPLibException {
		eppHostChangeIp(name, adresses, null);
	}

	public void eppHostRemoveIp(String name, String host) throws EPPLibException, UnknownHostException {
		List<InetAddress> addresses = new ArrayList<>();

		addresses.add(InetAddress.getByName(host));
		eppHostChangeIp(name, addresses, null);
	}

	public void eppHostRemoveStatus(String name, List<String> statuses) throws EPPLibException {
		eppHostChangeStatus(name, statuses, null);
	}

	public void eppLogin(String username, String password) throws EPPLibException {
		eppLogin(username, password, null);
	}

	public void eppLogin(String username, String password, String newPassword) throws EPPLibException {
		final String EPP_VERSION = "1.0";
		final String EPP_LANG = "pl";
		final String[] EPP_NAMESPACES = { "http://www.dns.pl/nask-epp-schema/contact-2.1", "http://www.dns.pl/nask-epp-schema/host-2.1", "http://www.dns.pl/nask-epp-schema/domain-2.1",
				"http://www.dns.pl/nask-epp-schema/future-2.1" };
		final String[] EPP_EXT_NAMESPACES = { "http://www.dns.pl/nask-epp-schema/extcon-2.1", "http://www.dns.pl/nask-epp-schema/extdom-2.1" };

		if (username == null || password == null)
			throw new IllegalArgumentException();

		LoginSvcType svcs = new LoginSvcType();
		ExtURIType exturi = new ExtURIType();

		for (String ns : EPP_EXT_NAMESPACES)
			exturi.getExtURI().add(ns);

		for (String ns : EPP_NAMESPACES)
			svcs.getObjURI().add(ns);

		svcs.setSvcExtension(exturi);

		CredsOptionsType options = new CredsOptionsType();
		options.setVersion(EPP_VERSION);
		options.setLang(EPP_LANG);

		CmdLoginType loginType = new CmdLoginType();
		loginType.setClID(username);
		loginType.setPw(password);
		if (newPassword != null)
			loginType.setNewPW(newPassword);
		loginType.setOptions(options);
		loginType.setSvcs(svcs);

		CommandType commandType = new CommandType();
		commandType.setLogin(loginType);

		EppType eppType = new EppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), true);
		checkResponseCode(response);

		loggedIn = true;
	}

	public void eppLogout() throws EPPLibException {
		checkSession();

        CmdLogoutType cmdLogoutType = new CmdLogoutType();

		CommandType commandType = new CommandType();
        commandType.setLogout(cmdLogoutType);

		EppType eppType = eppOF.createEppType();
		eppType.setCommand(commandType);

		EppType response = doRequest(eppOF.createEpp(eppType), false);
		checkResponseCode(response);

		loggedIn = false;
	}

	private <T> T getExtension(EppType response) throws EPPLibException {
		checkResponseCode(response);
		@SuppressWarnings("unchecked")
		JAXBElement<T> resData = (JAXBElement<T>) response.getResponse().getExtension().getRspExtGroupType().get(0);

		return resData.getValue();
	}

	private <T> T getPollResponse(EppType response) throws EPPLibException {
		checkResponseCode(response);
		@SuppressWarnings("unchecked")
        JAXBElement<T> resData = (JAXBElement<T>) response.getResponse().getResData().getRspDataGroupType().get(0);

		return resData.getValue();
	}

	private <T> T getResult(EppType response) throws EPPLibException {
		checkResponseCode(response);
		@SuppressWarnings("unchecked")
		JAXBElement<T> resData = (JAXBElement<T>) response.getResponse().getResData().getRspDataGroupType().get(0);

		return resData.getValue();
	}

	private SSLSocketFactory initTLS(KeyManager[] keyManagers, TrustManager[] trustManagers) throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(keyManagers, trustManagers, null);
		SSLSocketFactory socketFactory = context.getSocketFactory();

		return socketFactory;
	}

	public static String genAuthInfo() {
		return RandomStringUtils.randomAlphanumeric(8);
	}
}
