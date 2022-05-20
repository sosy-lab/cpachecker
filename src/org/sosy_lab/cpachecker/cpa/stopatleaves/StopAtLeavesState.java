// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.stopatleaves;

import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

/**
 * @see org.sosy_lab.cpachecker.cpa.targetreachability.ReachabilityState
 */
public enum StopAtLeavesState implements AbstractQueryableState {
  CONTINUE(false),
  STOP(true);

  final boolean shouldStop;

  StopAtLeavesState(boolean pShouldStop) {
    shouldStop = pShouldStop;
  }

  public boolean isShouldStop() {
    return shouldStop;
  }

  @Override
  public String getCPAName() {
    return "StopAtLeavesState";
  }

  @Override
  public boolean checkProperty(String property) throws InvalidQueryException {
    if (!property.equals("at leaf")) {
      throw new InvalidQueryException("Invalid query!");
    }

    return shouldStop;
  }

  @Override
  public Object evaluateProperty(String property) throws InvalidQueryException {
    if (!property.equals("at leaf")) {
      throw new InvalidQueryException("Invalid query!");
    }

    return shouldStop;
  }

  @Override
  public void modifyProperty(String modification) throws InvalidQueryException {
    throw new InvalidQueryException("Invalid query!");
  }
}
