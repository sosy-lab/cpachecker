/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.waitlist;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * An interface for a waitlist of AbstractStates.
 * Implementations differ in the strategy they use for pop().
 *
 * Implementations do not need to guarantee the semantics of a set
 * (i.e., preventing duplicate states).
 * This needs to be guaranteed by the caller (see
 * {@link org.sosy_lab.cpachecker.core.reachedset.ReachedSet#add(AbstractState, org.sosy_lab.cpachecker.core.interfaces.Precision)}).
 *
 * All methods of this interface should be fast (O(1) or O(log n) preferably),
 * except contains() and remove().
 *
 * The iterators provided by implementations may be unmodifiable.
 */
public interface Waitlist extends Iterable<AbstractState> {

  /**
   * Add an abstract state to the waitlist.
   */
  void add(AbstractState state);

  /**
   * Remove all abstract states from the waitlist.
   */
  void clear();

  /**
   * Checks whether an abstract state is contained in the waitlist.
   * This method uses equals().
   */
  boolean contains(AbstractState state);

  /**
   * Whether the waitlist contains no states.
   */
  boolean isEmpty();

  /**
   * Returns and removes the next abstract state that should be handled.
   * This decision is made by an implementation-specific strategy.
   * If the waitlist is empty, implementations may either trow an exception or
   * return null.
   */
  AbstractState pop();

  /**
   * Removes an abstract state, if it is contained.
   * This method uses equals() for containment checks.
   * Implementations need not to optimize their data structure for this method.
   */
  boolean remove(AbstractState state);

  /**
   * Returns the number of states in the waitlist.
   */
  int size();

  /**
   * Simple factory interface for waitlist implementations.
   */
  public static interface WaitlistFactory {

    /**
     * Create a fresh new empty instance of a waitlist.
     * The factory should keep no references to the new instance,
     * because clients of this interface may assume that nobody else will modify
     * the new waitlist except themselves.
     */
    Waitlist createWaitlistInstance();
  }

  /**
   * Enum containing standard waitlist strategies.
   * Instances of this enum can also be used as a factory for implementations
   * of the respective strategy.
   */
  public enum TraversalMethod implements WaitlistFactory {
    DFS     { @Override public Waitlist createWaitlistInstance() { return new SimpleWaitlist(this); } },
    BFS     { @Override public Waitlist createWaitlistInstance() { return new SimpleWaitlist(this); } },
    RAND    { @Override public Waitlist createWaitlistInstance() { return new RandomWaitlist();     } },
    RANDOM_PATH { @Override public Waitlist createWaitlistInstance() { return new RandomPathWaitlist(); } },
    ;
  }
}
