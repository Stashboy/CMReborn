package dev.cmreborn;

import org.junit.Test;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class ImmediateFutureFactoryTest {
    public interface NamedFuture extends Future<Object> { void addListener(Runnable r, Executor e); }
    public interface RenamedFuture extends Future<Object> { void b(Runnable r, Executor e); }
    public interface UnknownFuture extends Future<Object> { void somethingElse(); }

    @Test public void worksWithNamedAndObfuscatedListenerMethods() throws Exception {
        AtomicInteger ran = new AtomicInteger();
        NamedFuture named = (NamedFuture) ImmediateFutureFactory.create(NamedFuture.class, "named");
        RenamedFuture renamed = (RenamedFuture) ImmediateFutureFactory.create(RenamedFuture.class, "renamed");
        named.addListener(ran::incrementAndGet, Runnable::run);
        renamed.b(ran::incrementAndGet, Runnable::run);
        assertEquals(2, ran.get());
        assertEquals("named", named.get());
        assertEquals("renamed", renamed.get(0, TimeUnit.SECONDS));
        assertTrue(renamed.isDone());
        assertFalse(renamed.isCancelled());
        assertFalse(renamed.cancel(true));
        assertEquals(renamed, renamed);
        assertNotEquals(named, renamed);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownContractsBeforeTheyReachTheHost() {
        ImmediateFutureFactory.create(UnknownFuture.class, null);
    }

    @Test public void executorFailureDoesNotChangeCompletedResult() throws Exception {
        RenamedFuture future = (RenamedFuture) ImmediateFutureFactory.create(RenamedFuture.class, null);
        future.b(() -> {}, runnable -> { throw new java.util.concurrent.RejectedExecutionException(); });
        assertTrue(future.isDone());
        assertNull(future.get());
    }
}
