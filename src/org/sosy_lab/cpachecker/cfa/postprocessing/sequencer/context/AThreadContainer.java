package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context;

import java.util.Iterator;
import java.util.List;

import com.google.common.base.Preconditions;

public abstract class AThreadContainer implements Iterable<AThread> {
  protected List<? extends AThread> threads;
  protected int threadCount;
  protected AThread mainThread;

  /**
   * stores threads. The threads list must store at least one thread(the main thread).
   *
   * @param threads
   *          the threads which will be stored. the first thread is the main
   *          thread
   */
  public AThreadContainer(List<? extends AThread> threads) {
    Preconditions.checkNotNull(threads);
    Preconditions.checkElementIndex(0, threads.size());

    this.threads = threads;
    this.mainThread = threads.get(0);
    this.threadCount = threads.size();
  }

  public List<? extends AThread> getAllThreads() {
    return threads;
  }

  public AThread getMainThread() {
    return mainThread;
  }

  public int getThreadCount() {
    return threadCount;
  }

  @Override
  public String toString() {
    return threads.toString();
  }

  @Override
  public Iterator<AThread> iterator() {
    final Iterator<? extends AThread> it = threads.iterator();
    return new Iterator<AThread>() {

      @Override
      public boolean hasNext() {
        return it.hasNext();
      }

      @Override
      public AThread next() {
        return it.next();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("Cannot delete thread from thread container");
      }
    };
  }
}
