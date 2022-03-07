// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.exceptions.CPAException;

public interface ForcedCoveringStopOperator extends StopOperator {

  /** Check whether one state may be strengthened such that it is then covered by reachedState. */
  boolean isForcedCoveringPossible(
      AbstractState state, AbstractState reachedState, Precision precision)
      throws CPAException, InterruptedException;
}
