// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This interface can be used for abstract states that are not really partitionable, but where a
 * simple pseudo-key can be used to 'pseudo-partition' the states.
 */
public interface PseudoPartitionable {

  /**
   * Returns the key of the current object that indicates in which part of the partition the object
   * belongs.
   *
   * <p>The pseudo-key should divide the abstract state space into three partitions:
   *
   * <ul>
   *   <li>the <b>'lessThan' partition</b> contains states, that <b>might or might not</b> be
   *       'lessOrEqual' to the current state. There are no constraints for these states.
   *   <li>the <b>'equal' partition</b> consists of states, that might or might not be 'lessOrEqual'
   *       to the current state. For abstract states with an 'equal' key, the implementation of
   *       {@link #getPseudoHashCode} should be sufficient to further limit the possible states that
   *       are considered as 'lessOrEqual' without cutting to much.
   *   <li>the <b>'greaterThan' partition</b> consists of states, that are <b>definitely not</b>
   *       'lessOrEqual' to the current state.
   * </ul>
   *
   * Normally, a {@link Comparable} is symmetric and it's use cases should behave identically for
   * both directions of comparison. However, our use-case is very limited and thus we can use the
   * {@link Comparable} in a half-ordered lattice.
   *
   * <p>For more implementation detail of a good key, see {@link Partitionable#getPartitionKey()}.
   *
   * @return a key indicating the part of the pseudo-partition this object belongs to
   */
  @Nullable Comparable<?> getPseudoPartitionKey();

  /**
   * Return a hashable object (can be the state itself or null) that can be used to limit the set of
   * states for the current partition. We assume equal pseudo-hashCodes for states that might be
   * 'lessOrEqual', and distinct pseudo-hashCodes for states that are not 'lessOrEqual'.
   */
  @Nullable Object getPseudoHashCode();
}
