package net.sf.briar.transport;

import static net.sf.briar.api.protocol.ProtocolConstants.MAX_PACKET_LENGTH;
import static net.sf.briar.api.transport.TransportConstants.MIN_CONNECTION_LENGTH;

import java.io.ByteArrayOutputStream;
import java.util.Random;

import junit.framework.TestCase;
import net.sf.briar.TestDatabaseModule;
import net.sf.briar.api.ContactId;
import net.sf.briar.api.protocol.TransportIndex;
import net.sf.briar.api.transport.ConnectionContext;
import net.sf.briar.api.transport.ConnectionContextFactory;
import net.sf.briar.api.transport.ConnectionWriter;
import net.sf.briar.api.transport.ConnectionWriterFactory;
import net.sf.briar.crypto.CryptoModule;
import net.sf.briar.db.DatabaseModule;
import net.sf.briar.protocol.ProtocolModule;
import net.sf.briar.protocol.writers.ProtocolWritersModule;
import net.sf.briar.serial.SerialModule;
import net.sf.briar.transport.batch.TransportBatchModule;
import net.sf.briar.transport.stream.TransportStreamModule;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class ConnectionWriterTest extends TestCase {

	private final ConnectionContextFactory connectionContextFactory;
	private final ConnectionWriterFactory connectionWriterFactory;
	private final byte[] secret;
	private final ContactId contactId = new ContactId(13);
	private final TransportIndex transportIndex = new TransportIndex(13);
	private final long connection = 12345L;

	public ConnectionWriterTest() throws Exception {
		super();
		Injector i = Guice.createInjector(new CryptoModule(),
				new DatabaseModule(), new ProtocolModule(),
				new ProtocolWritersModule(), new SerialModule(),
				new TestDatabaseModule(), new TransportBatchModule(),
				new TransportModule(), new TransportStreamModule());
		connectionContextFactory =
			i.getInstance(ConnectionContextFactory.class);
		connectionWriterFactory = i.getInstance(ConnectionWriterFactory.class);
		secret = new byte[32];
		new Random().nextBytes(secret);
	}

	@Test
	public void testOverhead() throws Exception {
		ByteArrayOutputStream out =
			new ByteArrayOutputStream(MIN_CONNECTION_LENGTH);
		ConnectionContext ctx =
			connectionContextFactory.createConnectionContext(contactId,
					transportIndex, connection, secret);
		ConnectionWriter w = connectionWriterFactory.createConnectionWriter(out,
				MIN_CONNECTION_LENGTH, ctx);
		// Check that the connection writer thinks there's room for a packet
		long capacity = w.getRemainingCapacity();
		assertTrue(capacity >= MAX_PACKET_LENGTH);
		assertTrue(capacity <= MIN_CONNECTION_LENGTH);
		// Check that there really is room for a packet
		byte[] payload = new byte[MAX_PACKET_LENGTH];
		w.getOutputStream().write(payload);
		w.getOutputStream().flush();
		long used = out.size();
		assertTrue(used >= MAX_PACKET_LENGTH);
		assertTrue(used <= MIN_CONNECTION_LENGTH);
	}
}
