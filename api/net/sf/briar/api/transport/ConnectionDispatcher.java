package net.sf.briar.api.transport;

import net.sf.briar.api.ContactId;
import net.sf.briar.api.protocol.TransportId;
import net.sf.briar.api.protocol.TransportIndex;

public interface ConnectionDispatcher {

	void dispatchReader(TransportId t, BatchTransportReader r);

	void dispatchWriter(ContactId c, TransportIndex i, BatchTransportWriter w);

	void dispatchIncomingConnection(TransportId t, StreamTransportConnection s);

	void dispatchOutgoingConnection(ContactId c, TransportIndex i,
			StreamTransportConnection s);
}
