// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.waitlist;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * An interface for a waitlist of AbstractStates. Implementations differ in the strategy they use
 * for pop().
 *
 * <p>Implementations do not need to guarantee the semantics of a set (i.e., preventing duplicate
 * states). This needs to be guaranteed by the caller (see {@link
 * org.sosy_lab.cpachecker.core.reachedset.ReachedSet#add(AbstractState,
 * org.sosy_lab.cpachecker.core.interfaces.Precision)}).
 *
 * <p>All methods of this interface should be fast (O(1) or O(log n) preferably), except contains()
 * and remove().
 *
 * <p>The iterators provided by implementations may be unmodifiable.
 */
public interface Waitlist extends Iterable<AbstractState> {

  /** Add an abstract state to the waitlist. */
  void add(AbstractState state);

  /** Remove all abstract states from the waitlist. */
  void clear();

  /** Checks whether an abstract state is contained in the waitlist. This method uses equals(). */
  boolean contains(AbstractState state);

  /** Whether the waitlist contains no states. */
  boolean isEmpty();

  /**
   * Returns and removes the next abstract state that should be handled. This decision is made by an
   * implementation-specific strategy. If the waitlist is empty, implementations may either trow an
   * exception or return null.
   */
  AbstractState pop();

  /**
   * Removes an abstract state, if it is contained. This method uses equals() for containment
   * checks. Implementations need not to optimize their data structure for this method.
   */
  boolean remove(AbstractState state);

  /** Returns the number of states in the waitlist. */
  int size();

  /** Simple factory interface for waitlist implementations. */
  interface WaitlistFactory {

    /**
     * Create a fresh new empty instance of a waitlist. The factory should keep no references to the
     * new instance, because clients of this interface may assume that nobody else will modify the
     * new waitlist except themselves.
     */
    Waitlist createWaitlistInstance();
  }

  /**
   * Enum containing standard waitlist strategies. Instances of this enum can also be used as a
   * factory for implementations of the respective strategy.
   */
  enum TraversalMethod implements WaitlistFactory {
    DFS {
      @Override
      public Waitlist createWaitlistInstance() {
        return new SimpleWaitlist(this);
      }
    },
    BFS {
      @Override
      public Waitlist createWaitlistInstance() {
        return new SimpleWaitlist(this);
      }
    },
    RAND {
      @Override
      public Waitlist createWaitlistInstance() {
        return new RandomWaitlist();
      }
    },
    RANDOM_PATH {
      @Override
      public Waitlist createWaitlistInstance() {
        return new RandomPathWaitlist();
      }
    },
    ROUND_ROBIN {
      @Override
      public Waitlist createWaitlistInstance() {
        return new RoundRobinWaitlist();
      }
    }
  }
}
