package org.briarproject.contact;

import org.briarproject.BriarTestCase;
import org.briarproject.api.contact.Contact;
import org.briarproject.api.contact.ContactId;
import org.briarproject.api.contact.ContactManager;
import org.briarproject.api.crypto.SecretKey;
import org.briarproject.api.db.DatabaseComponent;
import org.briarproject.api.db.Transaction;
import org.briarproject.api.identity.Author;
import org.briarproject.api.identity.AuthorId;
import org.briarproject.api.transport.KeyManager;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.briarproject.TestUtils.getRandomBytes;
import static org.briarproject.TestUtils.getRandomId;
import static org.briarproject.TestUtils.getSecretKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ContactManagerImplTest extends BriarTestCase {

	private final Mockery context = new Mockery();
	private final DatabaseComponent db = context.mock(DatabaseComponent.class);
	private final KeyManager keyManager = context.mock(KeyManager.class);
	private final ContactManager contactManager;
	private final ContactId contactId = new ContactId(42);
	private final Author remote =
			new Author(new AuthorId(getRandomId()), "remote",
					getRandomBytes(42));
	private final AuthorId local = new AuthorId(getRandomId());
	private final boolean verified = false, active = true;
	private final Contact contact =
			new Contact(contactId, remote, local, verified, active);

	public ContactManagerImplTest() {
		contactManager = new ContactManagerImpl(db, keyManager);
	}

	@Test
	public void testAddContact() throws Exception {
		final SecretKey master = getSecretKey();
		final long timestamp = 42;
		final boolean alice = true;
		final Transaction txn = new Transaction(null, false);

		context.checking(new Expectations() {{
			oneOf(db).startTransaction(false);
			will(returnValue(txn));
			oneOf(db).addContact(txn, remote, local, verified, active);
			will(returnValue(contactId));
			oneOf(keyManager)
					.addContact(txn, contactId, master, timestamp, alice);
			oneOf(db).getContact(txn, contactId);
			will(returnValue(contact));
			oneOf(db).endTransaction(txn);
		}});

		assertEquals(contactId, contactManager
				.addContact(remote, local, master, timestamp, alice, verified,
						active));
		context.assertIsSatisfied();
	}

	@Test
	public void testGetContact() throws Exception {
		final Transaction txn = new Transaction(null, true);
		context.checking(new Expectations() {{
			oneOf(db).startTransaction(true);
			will(returnValue(txn));
			oneOf(db).getContact(txn, contactId);
			will(returnValue(contact));
			oneOf(db).endTransaction(txn);
		}});

		assertEquals(contact, contactManager.getContact(contactId));
		context.assertIsSatisfied();
	}

	@Test
	public void testActiveContacts() throws Exception {
		Collection<Contact> activeContacts = Collections.singletonList(contact);
		final Collection<Contact> contacts = new ArrayList<>(activeContacts);
		contacts.add(new Contact(new ContactId(3), remote, local, true, false));
		final Transaction txn = new Transaction(null, true);
		context.checking(new Expectations() {{
			oneOf(db).startTransaction(true);
			will(returnValue(txn));
			oneOf(db).getContacts(txn);
			will(returnValue(contacts));
			oneOf(db).endTransaction(txn);
		}});

		assertEquals(activeContacts, contactManager.getActiveContacts());
		context.assertIsSatisfied();
	}

	@Test
	public void testRemoveContact() throws Exception {
		final Transaction txn = new Transaction(null, false);
		context.checking(new Expectations() {{
			oneOf(db).startTransaction(false);
			will(returnValue(txn));
			oneOf(db).getContact(txn, contactId);
			will(returnValue(contact));
			oneOf(db).removeContact(txn, contactId);
			oneOf(db).endTransaction(txn);
		}});

		contactManager.removeContact(contactId);
		context.assertIsSatisfied();
	}

	@Test
	public void testSetContactActive() throws Exception {
		final Transaction txn = new Transaction(null, false);
		context.checking(new Expectations() {{
			oneOf(db).setContactActive(txn, contactId, active);
		}});

		contactManager.setContactActive(txn, contactId, active);
		context.assertIsSatisfied();
	}

	@Test
	public void testContactExists() throws Exception {
		final Transaction txn = new Transaction(null, true);
		context.checking(new Expectations() {{
			oneOf(db).startTransaction(true);
			will(returnValue(txn));
			oneOf(db).containsContact(txn, remote.getId(), local);
			will(returnValue(true));
			oneOf(db).endTransaction(txn);
		}});

		assertTrue(contactManager.contactExists(remote.getId(), local));
		context.assertIsSatisfied();
	}

}
