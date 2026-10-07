package qa.core;

import java.util.*;

public final class CleanupJournal implements AutoCloseable {
  private record Entry(String label, Runnable action) {}

  private final Deque<Entry> actions = new ArrayDeque<>();

  public void register(String label, Runnable r) {
    actions.push(new Entry(label, r));
  }

  public void close() {
    RuntimeException failure = new RuntimeException("Cleanup incomplete");
    while (!actions.isEmpty()) {
      Entry e = actions.pop();
      try {
        e.action.run();
      } catch (Throwable t) {
        failure.addSuppressed(new RuntimeException(e.label, t));
      }
    }
    if (failure.getSuppressed().length > 0) throw failure;
  }
}
