// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.slab;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.predicate.SymbolicLocationsUtility;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class SLABPredicateTransferRelation implements TransferRelation {

  private final SymbolicLocationsUtility symbolicLocationsUtility;

  public SLABPredicateTransferRelation(SymbolicLocationsUtility pSymbolicLocationsUtility) {
    symbolicLocationsUtility = pSymbolicLocationsUtility;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    ImmutableList.Builder<AbstractState> successors = new ImmutableList.Builder<>();
    for (boolean init : new boolean[] {true, false}) {
      for (boolean error : new boolean[] {true, false}) {
        AbstractState newState = symbolicLocationsUtility.makePredicateState(init, error);
        successors.add(newState);
      }
    }
    return successors.build();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    throw new UnsupportedOperationException("SLAB does not provide successors for a certain edge");
  }
}
