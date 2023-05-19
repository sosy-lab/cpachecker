// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import com.google.common.collect.ImmutableMap;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatValue;

/**
 * Interface representing a set of reached states, including storing a precision for each one.
 *
 * <p>In all its operations it preserves the order in which the state were added. All the
 * collections returned from methods of this class ensure this ordering, too.
 *
 * <p>Classes implementing this interface may not allow null values for states and precisions. All
 * methods do not return null except when stated explicitly.
 */
public interface ReachedSet extends UnmodifiableReachedSet {

  @Override
  Set<AbstractState> asCollection();

  /**
   * Add a state with a precision to the reached set and to the waitlist. If the state is already in
   * the reached set and the precisions are equal, nothing is done.
   *
   * @param state An AbstractState.
   * @param precision The Precision for the AbstractState
   * @throws IllegalArgumentException If the state is already in the reached set, but with a
   *     different precision.
   */
  void add(AbstractState state, Precision precision) throws IllegalArgumentException;

  /**
   * Like {@link #add(AbstractState, Precision)}, but does not add the state to the waitlist. Use
   * with caution to avoid unsound behavior.
   */
  void addNoWaitlist(AbstractState state, Precision precision) throws IllegalArgumentException;

  void addAll(Iterable<Pair<AbstractState, Precision>> toAdd);

  /** Re-add a state to the waitlist which is already contained in the reached set. */
  void reAddToWaitlist(AbstractState s);

  /** Change the precision of a state that is already in the reached set. */
  void updatePrecision(AbstractState s, Precision newPrecision);

  void remove(AbstractState state);

  void removeAll(Iterable<? extends AbstractState> toRemove);

  void removeOnlyFromWaitlist(AbstractState state);

  void clear();

  void clearWaitlist();

  AbstractState popFromWaitlist();

  default ImmutableMap<String, AbstractStatValue> getStatistics() {
    return ImmutableMap.of();
  }

  /** Return the instance of the CPA that belongs to the abstract states in this reached set. */
  ConfigurableProgramAnalysis getCPA();
}
