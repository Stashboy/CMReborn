package dev.cmreborn;

import org.junit.Test;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

public class TriggerFieldsTest {
    static final class Account {}
    interface PeerContract {}
    static final class CurrentPeer implements PeerContract {
        Object o = new Object();
        Account n = new Account();
    }
    static final class RenamedPeer implements PeerContract {
        Account arbitraryFutureName = new Account();
    }
    static final class AmbiguousPeer { Account first; Account second; }
    static final class StaticOnly { static Account n; Object o; }
    static final class Box { PeerContract renamed; Object unrelated; }
    static final class AmbiguousBox { PeerContract first; PeerContract second; }

    @Test public void ignoresNonNullOldFieldWhenAccountMoves() throws Exception {
        CurrentPeer peer = new CurrentPeer();
        Field field = TriggerFields.account(CurrentPeer.class, Account.class);
        assertNotNull(field);
        assertEquals("n", field.getName());
        assertSame(peer.n, field.get(peer));
        assertNotSame(peer.o, field.get(peer));
    }

    @Test public void accountLookupDoesNotDependOnAFieldLetter() {
        assertEquals("arbitraryFutureName",
                TriggerFields.account(RenamedPeer.class, Account.class).getName());
    }

    @Test public void missingStaticOrAmbiguousAccountFieldsAreRejected() {
        assertNull(TriggerFields.account(StaticOnly.class, Account.class));
        assertNull(TriggerFields.account(AmbiguousPeer.class, Account.class));
        assertNull(TriggerFields.account(Object.class, Account.class));
    }

    @Test public void peerLookupRequiresOneCompatibleInterfaceField() {
        assertEquals("renamed", TriggerFields.peer(Box.class, CurrentPeer.class).getName());
        assertNull(TriggerFields.peer(AmbiguousBox.class, CurrentPeer.class));
        assertNull(TriggerFields.peer(Object.class, CurrentPeer.class));
    }
}
