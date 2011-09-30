package net.sf.briar.protocol;

import java.util.Map;

import net.sf.briar.api.TransportId;
import net.sf.briar.api.protocol.TransportUpdate;

class TransportFactoryImpl implements TransportFactory {

	public TransportUpdate createTransportUpdate(
			Map<TransportId, Map<String, String>> transports, long timestamp) {
		return new TransportUpdateImpl(transports, timestamp);
	}
}
