// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.timedautomata;

import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class PathLengthTransferRelation implements TransferRelation {

  private int maxPathLength = 0;

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    return nextState(pState);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    return nextState(pState);
  }

  private Collection<? extends AbstractState> nextState(AbstractState successor) {
    PathLengthState state = (PathLengthState) successor;
    if (state.getPathLength() >= maxPathLength) {
      return Collections.emptySet();
    }

    if (state.getPathLength() == maxPathLength - 1) {
      return Collections.singleton(new PathLengthState(state.getPathLength() + 1, true));
    }

    return Collections.singleton(new PathLengthState(state.getPathLength() + 1, false));
  }

  public void setMaximumPathLength(int pMaxPathLength) {
    maxPathLength = pMaxPathLength;
  }
}
