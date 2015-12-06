package org.sosy_lab.cpachecker.cpa.callstack;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

import com.google.common.collect.ImmutableMap;

public class MultiCallstackState implements AbstractState, Partitionable, AbstractQueryableState, Serializable {
  
  private static final long serialVersionUID = -2816326882252001347L;
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
    this.threadContextCallstacks = ImmutableMap.<String, CallstackState>copyOf(stacks);
  }
  
  /**
   * for stacking
   */
  protected MultiCallstackState(@Nullable MultiCallstackState previousState, String thread, String function, CFANode callerNode) {
    assert previousState != null || thread.equals("thread0");
    assert previousState != null || function.equals("main");  // creates a new context
    this.thread = thread;

    CallstackState nextState;
    HashMap<String, CallstackState> copyOfStackHeads = new HashMap<String, CallstackState>();
    if(previousState == null) {
      nextState = new CallstackState(null, function, callerNode);
    } else {
      copyOfStackHeads.putAll(previousState.threadContextCallstacks);
      nextState = new CallstackState(previousState.getCurrentStack(), function, callerNode);
    }
    copyOfStackHeads.put(thread, nextState);
    
    this.threadContextCallstacks = ImmutableMap.copyOf(copyOfStackHeads);
  }
  
  public static MultiCallstackState initialState(String function, CFANode callerNode) {
    return new MultiCallstackState(null, "thread0", function, callerNode);
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
    HashMap<String, CallstackState> copyOfStackHeads = new HashMap<String, CallstackState>(this.threadContextCallstacks);
    if(getCurrentStack().getPreviousState() == null) {
      copyOfStackHeads.remove(thread);
    } else {
      copyOfStackHeads.put(thread, getCurrentStack().getPreviousState());
    }

    return new MultiCallstackState(thread, copyOfStackHeads);
  }
  
  @Override
  public String getCPAName() {
    return "MultiCallstackCPA";
  }

  @Override
  public boolean checkProperty(String property) throws InvalidQueryException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Object evaluateProperty(String property) throws InvalidQueryException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void modifyProperty(String modification) throws InvalidQueryException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Object getPartitionKey() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String toString() {
    String rep = "";
    for (Entry<String, CallstackState> entry : threadContextCallstacks
        .entrySet()) {
      
      if (entry.getKey().equals(thread)) {
        rep += "* ";
      } 
      rep += entry.getKey() + ": ";
      rep += entry.getValue().toString() + " \n";
    }

    return rep;
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
    final int prime = 31;
    int result = 1;
    result = prime * result + ((thread == null) ? 0 : thread.hashCode());
    result = prime * result
        + ((threadContextCallstacks == null) ? 0 : threadContextCallstacks.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MultiCallstackState other = (MultiCallstackState) obj;
    if (thread == null) {
      if (other.thread != null)
        return false;
    } else if (!thread.equals(other.thread))
      return false;
    if (threadContextCallstacks == null) {
      if (other.threadContextCallstacks != null)
        return false;
    } else if (!threadContextCallstacks.equals(other.threadContextCallstacks))
      return false;
    return true;
  }
  
  /**
   * checks if the values of the saved stacks are the same. Particularly if there
   * exists a key with a null state on stack the state might be the same as an
   * state without that key.
   * 
   * @param other
   * @return
   */
  private boolean isMapEqual(Map<String, CallstackState> other) {
    for (String thread: threadContextCallstacks.keySet()) {
      if(threadContextCallstacks.get(thread) != (other.get(thread))) {
        return false;
      }
    }
    for(String thread : other.keySet()) {
      if(other.get(thread) != threadContextCallstacks.get(thread)) {
        return false;
      }
    }
    
    return true;
  }
  
  

}
