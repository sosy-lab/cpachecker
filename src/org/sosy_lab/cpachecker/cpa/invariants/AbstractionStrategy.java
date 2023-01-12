// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

interface AbstractionStrategy {

  /**
   * Gets an abstraction state with no specific data.
   *
   * @return an abstraction state with no specific data.
   */
  AbstractionState getAbstractionState();

  /**
   * Gets an abstraction state that represents the successor of the given abstraction state.
   *
   * @param pPrevious the preceding state.
   * @return an abstraction state that represents the successor of the given abstraction state.
   */
  AbstractionState getSuccessorState(AbstractionState pPrevious);

  /**
   * Gets an abstraction state that resembles the given abstraction state as close as this factory
   * allows.
   *
   * @param pOther the state to be represented.
   * @return an abstraction state that resembles the given abstraction state as close as this
   *     factory allows.
   */
  AbstractionState from(AbstractionState pOther);
}
