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
package org.sosy_lab.cpachecker.core.reachedset;

import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

/**
 * Interface representing a set of reached states, including storing a
 * precision for each one.
 *
 * In all its operations it preserves the order in which the state were added.
 * All the collections returned from methods of this class ensure this ordering, too.
 *
 * Classes implementing this interface may not allow null values for states and precisions.
 * All methods do not return null except when stated explicitly.
 */
public interface ReachedSet extends UnmodifiableReachedSet {

  @Override
  public Set<AbstractState> asCollection();

  /**
   * Add a state with a precision to the reached set and to the waitlist.
   * If the state is already in the reached set and the precisions are equal,
   * nothing is done.
   *
   * @param state An AbstractState.
   * @param precision The Precision for the AbstractState
   * @throws IllegalArgumentException If the state is already in the reached set, but with a different precision.
   */
  public void add(AbstractState state, Precision precision) throws IllegalArgumentException;


  public void addAll(Iterable<Pair<AbstractState, Precision>> toAdd);

  /**
   * Re-add a state to the waitlist which is already contained in the reached set.
   */
  public void reAddToWaitlist(AbstractState s);

  /**
   * Change the precision of a state that is already in the reached set.
   */
  public void updatePrecision(AbstractState s, Precision newPrecision);

  public void remove(AbstractState state);

  public void removeAll(Iterable<? extends AbstractState> toRemove);

  public void removeOnlyFromWaitlist(AbstractState state);

  public void clear();

  public AbstractState popFromWaitlist();
}
