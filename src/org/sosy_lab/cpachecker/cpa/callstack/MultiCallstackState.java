package org.sosy_lab.cpachecker.cpa.callstack;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;

import com.google.common.collect.ImmutableMap;

public class MultiCallstackState implements AbstractState, Partitionable {

  static final String INITIAL_THREAD_NAME = "thread0";

  private final ImmutableMap<String, CallstackState> threadContextCallstacks;

  /** It is not granted that thread is inside threadContextCallStack */
  /*TODO This attribute is nullable. This might shrink the abstract state because null doesn't say from which thread it came */
  @Nullable
  private final String thread;

  // ASSERTION ONLY
  private boolean contextLess = false;

  /**
   * changes context of state
   */
  protected MultiCallstackState(String thread, Map<String, CallstackState> stacks) {
    this.thread = thread;
    this.threadContextCallstacks = ImmutableMap.copyOf(stacks);
  }

  /**
   * for stacking
   */
  protected MultiCallstackState(@Nullable MultiCallstackState previousState, String thread, String function, CFANode callerNode) {
    assert previousState != null || thread.equals(INITIAL_THREAD_NAME); //TODO use dynamic name
    assert previousState != null || function.equals("main__0");  // creates a new context
    this.thread = thread;

    CallstackState nextState;
    HashMap<String, CallstackState> copyOfStackHeads = new HashMap<>();
    if(previousState == null) {
      nextState = new CallstackState(null, function, callerNode);
    } else {
      copyOfStackHeads.putAll(previousState.threadContextCallstacks);
      nextState = new CallstackState(previousState.getCurrentStack(), function, callerNode);
    }
    copyOfStackHeads.put(thread, nextState);

    this.threadContextCallstacks = ImmutableMap.copyOf(copyOfStackHeads);
  }

  public CallstackState getCurrentStack() {
    return threadContextCallstacks.get(thread);
  }

  /**
   * Creates new context
   */
  public MultiCallstackState setContext(@Nullable String thread) {
    return new MultiCallstackState(thread, threadContextCallstacks);
  }

  public String getThreadName() {
    return thread;
  }

  /** TODO Note! If a new context was created and a function returns after this, then new created context must be known by the state!! */
  public MultiCallstackState getPreviousState() throws NullPointerException {
    HashMap<String, CallstackState> copyOfStackHeads = new HashMap<>(this.threadContextCallstacks);
    if(getCurrentStack().getPreviousState() == null) {
      copyOfStackHeads.remove(thread);
    } else {
      copyOfStackHeads.put(thread, getCurrentStack().getPreviousState());
    }

    return new MultiCallstackState(thread, copyOfStackHeads);
  }

  @Override
  public Object getPartitionKey() {
    return threadContextCallstacks;
  }

  @Override
  public String toString() {
    StringBuilder rep = new StringBuilder();
    for (Entry<String, CallstackState> entry : threadContextCallstacks.entrySet()) {
      if (entry.getKey().equals(thread)) {
        rep.append("* ");
      }
      rep.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
    }
    return rep.toString();
  }


  // ASSERTION ONLY
  public boolean hasContext(String thread) {
    assert thread != null;

    return threadContextCallstacks.containsKey(thread);
  }

  // ASSERTION ONLY
  public boolean isContextLess() {
    return contextLess;
  }

  // ASSERTION ONLY
  public void setContextLess(boolean isContextLess) {
    this.contextLess = isContextLess;
  }


//TODO Thread shouldn't change the
  // state, because every thread has
  // it's own functions. Note except
  // of pthread_join etc. TODO Maybe
  // it affects the state because
  // function call edges will only
  // which thread will be created
  // with the right thread context

  //!isMapEqual(other.threadContextCallstacks)

  @Override
  public int hashCode() {
    return Objects.hash(thread, threadContextCallstacks);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof MultiCallstackState)) {
      return false;
    }
    MultiCallstackState other = (MultiCallstackState) obj;
    return Objects.equals(thread, other.thread)
        && Objects.equals(threadContextCallstacks, other.threadContextCallstacks);
  }
}
