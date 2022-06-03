// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.slab;

import com.google.common.collect.Iterables;
import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class SLABStopOperator implements StopOperator {

  private final AbstractDomain domain;

  public SLABStopOperator(AbstractDomain pDomain) {
    domain = pDomain;
  }

  @Override
  public boolean stop(
      AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {
    // Check if the argElement has only one parent and remember it for later:
    ARGState parent = null;
    if (((ARGState) pState).getParents().size() == 1) {
      parent = Iterables.get(((ARGState) pState).getParents(), 0);
    }

    for (AbstractState reachedState : pReached) {
      if (pState != reachedState && domain.isLessOrEqual(pState, reachedState)) {
        if (parent != null && ((ARGState) reachedState).getParents().contains(parent)) {
          ((ARGState) pState).removeFromARG();
        }
        return true;
      }
    }
    return false;
  }
}
