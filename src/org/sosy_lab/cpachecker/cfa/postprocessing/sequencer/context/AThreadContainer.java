/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
