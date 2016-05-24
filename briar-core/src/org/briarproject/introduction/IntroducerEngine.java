package org.briarproject.introduction;

import org.briarproject.api.FormatException;
import org.briarproject.api.clients.ProtocolEngine;
import org.briarproject.api.contact.ContactId;
import org.briarproject.api.data.BdfDictionary;
import org.briarproject.api.event.Event;
import org.briarproject.api.event.IntroductionAbortedEvent;
import org.briarproject.api.event.IntroductionResponseReceivedEvent;
import org.briarproject.api.identity.AuthorId;
import org.briarproject.api.introduction.IntroducerAction;
import org.briarproject.api.introduction.IntroducerProtocolState;
import org.briarproject.api.introduction.IntroductionResponse;
import org.briarproject.api.clients.SessionId;
import org.briarproject.api.sync.MessageId;
import org.briarproject.introduction.IntroducerSessionState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static org.briarproject.api.introduction.IntroducerAction.LOCAL_ABORT;
import static org.briarproject.api.introduction.IntroducerAction.LOCAL_REQUEST;
import static org.briarproject.api.introduction.IntroducerAction.REMOTE_ACCEPT_1;
import static org.briarproject.api.introduction.IntroducerAction.REMOTE_ACCEPT_2;
import static org.briarproject.api.introduction.IntroducerAction.REMOTE_DECLINE_1;
import static org.briarproject.api.introduction.IntroducerAction.REMOTE_DECLINE_2;
import static org.briarproject.api.introduction.IntroducerProtocolState.AWAIT_ACKS;
import static org.briarproject.api.introduction.IntroducerProtocolState.AWAIT_ACK_1;
import static org.briarproject.api.introduction.IntroducerProtocolState.AWAIT_ACK_2;
import static org.briarproject.api.introduction.IntroducerProtocolState.AWAIT_RESPONSES;
import static org.briarproject.api.introduction.IntroducerProtocolState.AWAIT_RESPONSE_1;
import static org.briarproject.api.introduction.IntroducerProtocolState.AWAIT_RESPONSE_2;
import static org.briarproject.api.introduction.IntroducerProtocolState.ERROR;
import static org.briarproject.api.introduction.IntroducerProtocolState.FINISHED;
import static org.briarproject.api.introduction.IntroductionConstants.ACCEPT;
import static org.briarproject.api.introduction.IntroductionConstants.AUTHOR_ID_1;
import static org.briarproject.api.introduction.IntroductionConstants.AUTHOR_ID_2;
import static org.briarproject.api.introduction.IntroductionConstants.CONTACT_1;
import static org.briarproject.api.introduction.IntroductionConstants.CONTACT_2;
import static org.briarproject.api.introduction.IntroductionConstants.CONTACT_ID_1;
import static org.briarproject.api.introduction.IntroductionConstants.CONTACT_ID_2;
import static org.briarproject.api.introduction.IntroductionConstants.GROUP_ID;
import static org.briarproject.api.introduction.IntroductionConstants.GROUP_ID_1;
import static org.briarproject.api.introduction.IntroductionConstants.GROUP_ID_2;
import static org.briarproject.api.introduction.IntroductionConstants.MESSAGE_ID;
import static org.briarproject.api.introduction.IntroductionConstants.MESSAGE_TIME;
import static org.briarproject.api.introduction.IntroductionConstants.MSG;
import static org.briarproject.api.introduction.IntroductionConstants.NAME;
import static org.briarproject.api.introduction.IntroductionConstants.PUBLIC_KEY;
import static org.briarproject.api.introduction.IntroductionConstants.PUBLIC_KEY1;
import static org.briarproject.api.introduction.IntroductionConstants.PUBLIC_KEY2;
import static org.briarproject.api.introduction.IntroductionConstants.RESPONSE_1;
import static org.briarproject.api.introduction.IntroductionConstants.RESPONSE_2;
import static org.briarproject.api.introduction.IntroductionConstants.ROLE_INTRODUCER;
import static org.briarproject.api.introduction.IntroductionConstants.SESSION_ID;
import static org.briarproject.api.introduction.IntroductionConstants.STATE;
import static org.briarproject.api.introduction.IntroductionConstants.TYPE;
import static org.briarproject.api.introduction.IntroductionConstants.TYPE_ABORT;
import static org.briarproject.api.introduction.IntroductionConstants.TYPE_ACK;
import static org.briarproject.api.introduction.IntroductionConstants.TYPE_REQUEST;
import static org.briarproject.api.introduction.IntroductionConstants.TYPE_RESPONSE;

public class IntroducerEngine
		implements ProtocolEngine<BdfDictionary, IntroducerSessionState, BdfDictionary> {

	private static final Logger LOG =
			Logger.getLogger(IntroducerEngine.class.getName());


	@Override
	public StateUpdate<IntroducerSessionState, BdfDictionary> onLocalAction(
			IntroducerSessionState localState, BdfDictionary localAction) {

		try {
			IntroducerProtocolState currentState = localState.getState();
			int type = localAction.getLong(TYPE).intValue();
			IntroducerAction action = IntroducerAction.getLocal(type);
			IntroducerProtocolState nextState = currentState.next(action);

			if (action == LOCAL_ABORT && currentState != ERROR) {
				return abortSession(currentState, localState);
			}

			if (nextState == ERROR) {
				if (LOG.isLoggable(WARNING)) {
					LOG.warning("Error: Invalid action in state " +
							currentState.name());
				}
				return noUpdate(localState);
			}

			localState.setState(nextState);
			if (action == LOCAL_REQUEST) {
				// create the introduction requests for both contacts
				List<BdfDictionary> messages = new ArrayList<BdfDictionary>(2);
				BdfDictionary msg1 = new BdfDictionary();
				msg1.put(TYPE, TYPE_REQUEST);
				msg1.put(SESSION_ID, localState.getSessionId().getBytes());
				msg1.put(GROUP_ID, localState.getGroup1Id().getBytes());
				msg1.put(NAME, localState.getContact2Name());
				msg1.put(PUBLIC_KEY, localAction.getRaw(PUBLIC_KEY2));
				if (localAction.containsKey(MSG)) {
					msg1.put(MSG, localAction.getString(MSG));
				}
				msg1.put(MESSAGE_TIME, localAction.getLong(MESSAGE_TIME));
				messages.add(msg1);
				logLocalAction(currentState, localState, msg1);
				BdfDictionary msg2 = new BdfDictionary();
				msg2.put(TYPE, TYPE_REQUEST);
				msg2.put(SESSION_ID, localState.getSessionId().getBytes());
				msg2.put(GROUP_ID, localState.getGroup2Id().getBytes());
				msg2.put(NAME, localState.getContact1Name());
				msg2.put(PUBLIC_KEY, localAction.getRaw(PUBLIC_KEY1));
				if (localAction.containsKey(MSG)) {
					msg2.put(MSG, localAction.getString(MSG));
				}
				msg2.put(MESSAGE_TIME, localAction.getLong(MESSAGE_TIME));
				messages.add(msg2);
				logLocalAction(currentState, localState, msg2);

				List<Event> events = Collections.emptyList();
				return new StateUpdate<IntroducerSessionState, BdfDictionary>(
						false, false,localState, messages, events);
			} else {
				throw new IllegalArgumentException("Unknown Local Action");
			}
		} catch (FormatException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public StateUpdate<IntroducerSessionState, BdfDictionary> onMessageReceived(
			IntroducerSessionState localState, BdfDictionary msg) {

		try {
			IntroducerProtocolState currentState = localState.getState();
			int type = msg.getLong(TYPE).intValue();
			boolean one = isContact1(localState, msg);
			IntroducerAction action = IntroducerAction.getRemote(type, one);
			IntroducerProtocolState nextState = currentState.next(action);

			logMessageReceived(currentState, nextState, localState, type, msg);

			if (nextState == ERROR) {
				if (currentState != ERROR) {
					return abortSession(currentState, localState);
				} else {
					return noUpdate(localState);
				}
			}

			List<BdfDictionary> messages;
			List<Event> events;

			// we have sent our requests and just got the 1st or 2nd response
			if (currentState == AWAIT_RESPONSES ||
					currentState == AWAIT_RESPONSE_1 ||
					currentState == AWAIT_RESPONSE_2) {
				// update next state based on message content
				action = IntroducerAction
						.getRemote(type, one, msg.getBoolean(ACCEPT));
				nextState = currentState.next(action);
				localState.setState(nextState);
				if (one) localState.setResponse1(
						new MessageId(msg.getRaw(MESSAGE_ID)));
				else localState.setResponse2(
						new MessageId(msg.getRaw(MESSAGE_ID)));

				messages = forwardMessage(localState, msg);
				events = Collections.singletonList(getEvent(localState, msg));
			}
			// we have forwarded both responses and now received the 1st or 2nd ACK
			else if (currentState == AWAIT_ACKS ||
					currentState == AWAIT_ACK_1 ||
					currentState == AWAIT_ACK_2) {
				localState.setState(nextState);
				messages = forwardMessage(localState, msg);
				events = Collections.emptyList();
			}
			// we probably received a response while already being FINISHED
			else if (currentState == FINISHED) {
				// if it was a response store it to be found later
				if (action == REMOTE_ACCEPT_1 || action == REMOTE_DECLINE_1) {
					localState.setResponse1(new MessageId(msg.getRaw(MESSAGE_ID)));
					messages = Collections.emptyList();
					events = Collections.singletonList(getEvent(localState, msg));
				} else if (action == REMOTE_ACCEPT_2 ||
						action == REMOTE_DECLINE_2) {
					localState.setResponse2(new MessageId(msg.getRaw(MESSAGE_ID)));
					messages = Collections.emptyList();
					events = Collections.singletonList(getEvent(localState, msg));
				} else return noUpdate(localState);
			} else {
				throw new IllegalArgumentException("Bad state");
			}
			return new StateUpdate<IntroducerSessionState, BdfDictionary>(false, 
					false, localState, messages, events);
		} catch (FormatException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private void logLocalAction(IntroducerProtocolState state,
			IntroducerSessionState localState, BdfDictionary msg) {

		if (!LOG.isLoggable(INFO)) return;

		try {
			String to = getMessagePartner(localState, msg);
			LOG.info("Sending introduction request in state " + state.name() +
					" to " + to + " with session ID " +
					Arrays.hashCode(msg.getRaw(SESSION_ID)) + " in group " +
					Arrays.hashCode(msg.getRaw(GROUP_ID)) + ". " +
					"Moving on to state " +
					localState.getState().name()
			);
		} catch (FormatException e) {
			if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
		}
	}

	private void logMessageReceived(IntroducerProtocolState currentState,
			IntroducerProtocolState nextState,
			IntroducerSessionState localState, int type, BdfDictionary msg) {
		if (!LOG.isLoggable(INFO)) return;

		try {
			String t = "unknown";
			if (type == TYPE_REQUEST) t = "Introduction";
			else if (type == TYPE_RESPONSE) t = "Response";
			else if (type == TYPE_ACK) t = "ACK";
			else if (type == TYPE_ABORT) t = "Abort";

			String from = getMessagePartner(localState, msg);
			String to = getOtherContact(localState, msg);

			LOG.info("Received " + t + " in state " + currentState.name() + " from " +
					from + " to " + to + " with session ID " +
					Arrays.hashCode(msg.getRaw(SESSION_ID)) + " in group " +
					Arrays.hashCode(msg.getRaw(GROUP_ID)) + ". " +
					"Moving on to state " + nextState.name()
			);
		} catch (FormatException e) {
			if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
		}
	}

	private List<BdfDictionary> forwardMessage(IntroducerSessionState localState,
			BdfDictionary message) throws FormatException {

		// clone the message here, because we still need the original
		BdfDictionary msg = (BdfDictionary) message.clone();
		if (isContact1(localState, msg)) {
			msg.put(GROUP_ID, localState.getGroup2Id());
		} else {
			msg.put(GROUP_ID, localState.getGroup1Id());
		}

		if (LOG.isLoggable(INFO)) {
			LOG.info("Forwarding message to group " +
					Arrays.hashCode(msg.getRaw(GROUP_ID)));
		}

		return Collections.singletonList(msg);
	}

	@Override
	public StateUpdate<IntroducerSessionState, BdfDictionary> onMessageDelivered(
			IntroducerSessionState localState, BdfDictionary delivered) {
		try {
			return noUpdate(localState);
		}
		catch (FormatException e) {
			if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
			return null;
		}
	}

	private Event getEvent(IntroducerSessionState localState, 
			BdfDictionary msg) throws FormatException {

		ContactId contactId = localState.getContact1Id();
		AuthorId authorId = localState.getContact1AuthorId();
		if (Arrays.equals(msg.getRaw(GROUP_ID), 
					localState.getGroup2Id().getBytes())) {
			contactId = localState.getContact2Id();
			authorId = localState.getContact2AuthorId();
		}

		SessionId sessionId = localState.getSessionId();
		MessageId messageId = new MessageId(msg.getRaw(MESSAGE_ID));
		long time = msg.getLong(MESSAGE_TIME);
		String name = getOtherContact(localState, msg);
		boolean accept = msg.getBoolean(ACCEPT);

		IntroductionResponse ir =
				new IntroductionResponse(sessionId, messageId, ROLE_INTRODUCER,
						time, false, false, false, false, authorId, name,
						accept);
		return new IntroductionResponseReceivedEvent(contactId, ir);
	}

	private boolean isContact1(IntroducerSessionState localState, 
			BdfDictionary msg) throws FormatException {

		byte[] group = msg.getRaw(GROUP_ID);
		byte[] group1 = localState.getGroup1Id().getBytes();
		byte[] group2 = localState.getGroup2Id().getBytes();

		if (Arrays.equals(group, group1)) {
			return true;
		} else if (Arrays.equals(group, group2)) {
			return false;
		} else {
			throw new FormatException();
		}
	}

	private String getMessagePartner(IntroducerSessionState localState,
			BdfDictionary msg) throws FormatException {

		String from = localState.getContact1Name();
		if (Arrays.equals(msg.getRaw(GROUP_ID), 
					localState.getGroup2Id().getBytes())) {
			from = localState.getContact2Name();
		}
		return from;
	}

	private String getOtherContact(IntroducerSessionState localState, 
			BdfDictionary msg) throws FormatException {

		String to = localState.getContact2Name();
		if (Arrays.equals(msg.getRaw(GROUP_ID), 
					localState.getGroup2Id().getBytes())) {
			to = localState.getContact1Name();
		}
		return to;
	}

	private StateUpdate<IntroducerSessionState, BdfDictionary> abortSession(
			IntroducerProtocolState currentState, 
			IntroducerSessionState localState) throws FormatException {

		if (LOG.isLoggable(WARNING)) {
			LOG.warning("Aborting protocol session " +
					Arrays.hashCode(localState.getSessionId().getBytes()) +
					" in state " + currentState.name());
		}

		localState.setState(ERROR);
		List<BdfDictionary> messages = new ArrayList<BdfDictionary>(2);
		BdfDictionary msg1 = new BdfDictionary();
		msg1.put(TYPE, TYPE_ABORT);
		msg1.put(SESSION_ID, localState.getSessionId());
		msg1.put(GROUP_ID, localState.getGroup1Id());
		messages.add(msg1);
		BdfDictionary msg2 = new BdfDictionary();
		msg2.put(TYPE, TYPE_ABORT);
		msg2.put(SESSION_ID, localState.getSessionId());
		msg2.put(GROUP_ID, localState.getGroup2Id());
		messages.add(msg2);

		// send one abort event per contact
		List<Event> events = new ArrayList<Event>(2);
		SessionId sessionId = localState.getSessionId();
		ContactId contactId1 = localState.getContact1Id();
		ContactId contactId2 = localState.getContact2Id();
		Event event1 = new IntroductionAbortedEvent(contactId1, sessionId);
		events.add(event1);
		Event event2 = new IntroductionAbortedEvent(contactId2, sessionId);
		events.add(event2);

		return new StateUpdate<IntroducerSessionState, BdfDictionary>(false, 
				false, localState, messages, events);
	}

	private StateUpdate<IntroducerSessionState, BdfDictionary> noUpdate(
			IntroducerSessionState localState) throws FormatException {

		return new StateUpdate<IntroducerSessionState, BdfDictionary>(false, false,
				localState, Collections.<BdfDictionary>emptyList(),
				Collections.<Event>emptyList());
	}
}
