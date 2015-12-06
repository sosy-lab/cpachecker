package org.sosy_lab.cpachecker.cpa.context;

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.AThread;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.AThreadContainer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


public class ThreadState implements AbstractState {

  private final ImmutableMap<String, Thread> threads;
  private final Thread currentThread;
  
  public ThreadState(Collection<Thread> threads, Thread currentThread) {
    assert threads.contains(currentThread);
    ImmutableMap.Builder<String, Thread> builder = ImmutableMap.builder();
    for(Thread thread : threads) {
      builder.put(thread.getThreadName(), thread);
    }
    
    this.threads = builder.build();
    this.currentThread = currentThread;
  }

  public ImmutableCollection<Thread> getThreads() {
    return threads.values();
  }
  
  @Deprecated
  public ImmutableMap<String, Thread> getThreadsMap() {
    return threads;
  }

  public Thread getThread(String threadName) {
    return threads.get(threadName);
  }
  
  public Thread getCurrentThread() {
    return currentThread;
  }

  public static AbstractState getInitialState(AThreadContainer initialThreads) {
    ImmutableSet.Builder<Thread> builder = ImmutableSet.<Thread>builder();
    Thread mainThread = null;
    for(AThread thread : initialThreads.getAllThreads()) {
      int maxProgramCounter = thread.getContextSwitchPoints().size();
      if(initialThreads.getMainThread().equals(thread)) {
        assert mainThread == null; // there can only be one main thread
        mainThread = new Thread(thread.getThreadName(), true, false, 0, maxProgramCounter);
        builder.add(mainThread);
      } else {
        builder.add(new Thread(thread.getThreadName(), false, false, 0, maxProgramCounter));
      }
    }
    
    return new ThreadState(builder.build(), mainThread);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((currentThread == null) ? 0 : currentThread.hashCode());
    result = prime * result + ((threads == null) ? 0 : threads.hashCode());
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
    ThreadState other = (ThreadState) obj;
    if (currentThread == null) {
      if (other.currentThread != null)
        return false;
    } else if (!currentThread.equals(other.currentThread))
      return false;
    if (threads == null) {
      if (other.threads != null)
        return false;
    } else if (!threads.equals(other.threads))
      return false;
    return true;
  }
  
  @Override
  public String toString() {
    String rep = "ThreadState=[";
    int i = 0;
    for(Thread thread : threads.values()) {
      if(currentThread.equals(thread)) {
        rep += "*";
      }
      rep += thread.toString();
      if(i<threads.size() - 1) {
        rep += ", ";
      }
      i++;
    }
    
    rep += "]";
      
    return rep;
  }

  
}
