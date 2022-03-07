// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import java.util.Collection;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public interface CoveringStateSetProvider {
  /**
   * Retrieve a set of reached states that jointly covers the given state.
   *
   * @return a subset of the reached states if the given state can be jointly covered; otherwise, an
   *     empty set
   */
  Collection<AbstractState> getCoveringStates(
      AbstractState state, Collection<AbstractState> reachedSet, Precision precision)
      throws CPAException, InterruptedException;
}
