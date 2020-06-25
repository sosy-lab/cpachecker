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
package org.sosy_lab.cpachecker.core.interfaces;

/**
 * An abstract state represents a set of concrete states in a specific abstract domain.
 *
 * Abstract states are produced by the operator of a {@link ConfigurableProgramAnalysis}.
 * They are typically not interpreted by other components than the CPA they belong to,
 * but an abstract state may implement some sub-interfaces of AbstractState or other interfaces
 * to allow some kind of information to be extracted from it.
 *
 * Abstract states should be immutable!
 *
 * If there is a fast way for an abstract state to check whether it might be covered
 * by some other state, consider implementing {@link Partitionable} such that
 * two abstract states that are in different partitions are guaranteed to never cover it each other.
 * This can be used with a partitioned reached set to speed up coverage checks.
 */
public interface AbstractState {

  /**
   * Check whether two abstract states are equal.
   *
   * The equality relation defined here must be consistent with
   * {@link AbstractDomain#isLessOrEqual(AbstractState, AbstractState)}.
   * Equality checks should be reasonably fast because they can be called very often,
   * for example if an abstract state is added to a set.
   *
   * Equality checks must be sound, but may be imprecise,
   * i.e., for two states s1 and s2 that represent the same set of concrete states,
   * {@code s1.equals(s2)} may return {@code false},
   * but if s1 and s2 represents different sets of concrete states,
   * {@code s1.equals(s2)} must never return {@code true}.
   *
   * The requirements of {@link Object#equals(Object)} must also be fulfilled.
   */
  @Override
  boolean equals(Object pObj);

  /**
   * Compute a hash code for an abstract state.
   *
   * This method must be consistent with {@link #equals(Object)},
   * as documented in {@link Object#hashCode()}.
   */
  @Override
  int hashCode();

  /**
   * Return a human-readable representation of this state for debugging purposes.
   * The output should not be too long (most states should restrict themselves to one line).
   * Consider implementing {@link Graphable} to define a different representation
   * for graph visualizations.
   */
  @Override
  String toString();
}
