package epplib.tests;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import epplib.EPPClient;
import epplib.exceptions.EPPLibException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A registry that accepts the connection and then says nothing must not stall
 * the client for ever.
 *
 * This is the shape of a real outage rather than a hypothetical one. The
 * connection is established, the TLS certificate is valid and the host
 * resolves — every check an operator would run reports healthy — but the
 * registry never sends an HTTP response, so the reading thread stays parked in
 * a native socket read. An application that serializes registry access through
 * one client instance then stops answering altogether, because every later
 * request queues behind that one thread.
 *
 * The cause is the JDK default: an {@code HttpURLConnection} with no explicit
 * timeouts waits without limit. These tests pin both limits down.
 *
 * No registrar account is needed — the test talks to a socket it opens itself.
 */
public class TimeoutTestCase {

	/** Well above the read limit under test, so only a real hang trips it. */
	private static final int HANG_DETECTION_MS = 20_000;

	private static final int READ_TIMEOUT_MS = 1_500;

	private ServerSocket server;
	private Thread accepting;

	/**
	 * Starts a server that accepts a connection, drains the request and then
	 * keeps the socket open without replying.
	 *
	 * Draining matters: without it a request large enough to fill the socket
	 * buffer would block in the write instead, which is a different failure
	 * from the one being tested.
	 */
	@Before
	public void startSilentServer() throws IOException {
		server = new ServerSocket(0);
		accepting = new Thread(() -> {
			try (Socket socket = server.accept()) {
				InputStream in = socket.getInputStream();
				byte[] buffer = new byte[4096];
				while (in.available() > 0 && in.read(buffer) > 0) {
					// Keep reading whatever arrived, then fall through and hold
					// the connection open without writing a response.
				}
				Thread.sleep(HANG_DETECTION_MS);
			} catch (IOException | InterruptedException e) {
				// Closing the server socket during teardown lands here; the
				// test has already made its assertion by then.
			}
		});
		accepting.setDaemon(true);
		accepting.start();
	}

	@After
	public void stopSilentServer() throws IOException {
		if (accepting != null)
			accepting.interrupt();
		if (server != null)
			server.close();
	}

	private EPPClient clientPointedAtSilentServer() {
		EPPClient client = new EPPClient("http://127.0.0.1:" + server.getLocalPort() + "/registry/epp");
		client.setReadTimeout(READ_TIMEOUT_MS);
		return client;
	}

	@Test(timeout = HANG_DETECTION_MS)
	public void silentRegistryFailsInsteadOfHanging() {
		EPPClient client = clientPointedAtSilentServer();

		long start = System.currentTimeMillis();
		try {
			client.eppHello();
			fail("a registry that sends no response must not look like success");
		} catch (EPPLibException e) {
			long elapsed = System.currentTimeMillis() - start;
			assertTrue(
					String.format("gave up after %dms, which is past the %dms limit", elapsed, READ_TIMEOUT_MS),
					elapsed < HANG_DETECTION_MS / 2);
		}
	}

	/**
	 * A stalled request must not poison the client.
	 *
	 * Each request opens its own connection, so a timeout leaves nothing behind
	 * and the next call fails the same bounded way rather than hanging or
	 * throwing something unrelated about session state.
	 */
	@Test(timeout = HANG_DETECTION_MS)
	public void clientStaysUsableAfterATimeout() {
		EPPClient client = clientPointedAtSilentServer();

		for (int attempt = 1; attempt <= 2; attempt++) {
			try {
				client.eppHello();
				fail("attempt " + attempt + " should have timed out");
			} catch (EPPLibException e) {
				// Expected on both attempts.
			}
		}
	}

	/** Timeouts are limits, not optional hints: a negative value is a mistake. */
	@Test
	public void negativeTimeoutsAreRejected() {
		EPPClient client = new EPPClient("http://127.0.0.1:1/registry/epp");

		try {
			client.setReadTimeout(-1);
			fail("a negative read timeout must be rejected");
		} catch (IllegalArgumentException e) {
			assertTrue("the refusal must say what was wrong", e.getMessage() != null && !e.getMessage().isEmpty());
		}

		try {
			client.setConnectTimeout(-1);
			fail("a negative connect timeout must be rejected");
		} catch (IllegalArgumentException e) {
			assertTrue("the refusal must say what was wrong", e.getMessage() != null && !e.getMessage().isEmpty());
		}
	}
}
