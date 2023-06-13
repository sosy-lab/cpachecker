// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class ARGBasedPartialReachedSetConstructionAlgorithm
    extends MonotoneTransferFunctionARGBasedPartialReachedSetConstructionAlgorithm {

  private ConfigurableProgramAnalysis cpa;

  public ARGBasedPartialReachedSetConstructionAlgorithm(
      final boolean pReturnARGStatesInsteadOfWrappedStates) {
    super(pReturnARGStatesInsteadOfWrappedStates, false);
  }

  @Override
  protected NodeSelectionARGPass getARGPass(
      final Precision pRootPrecision, final ARGState pRoot, final ARGCPA pCpa)
      throws InvalidConfigurationException {

    cpa = pCpa.getWrappedCPAs().get(0); // TODO this line looks dangerous!
    return new ExtendedNodeSelectionARGPass(pRootPrecision, pRoot);
  }

  private class ExtendedNodeSelectionARGPass extends NodeSelectionARGPass {

    private final Precision precision;
    private final boolean handlePredicateStates;

    public ExtendedNodeSelectionARGPass(final Precision pRootPrecision, final ARGState pRoot) {
      super(pRoot);
      precision = pRootPrecision;
      handlePredicateStates =
          AbstractStates.extractStateByType(pRoot, PredicateAbstractState.class) != null;
    }

    @Override
    protected boolean isToAdd(final ARGState pNode) {
      boolean isToAdd = super.isToAdd(pNode);
      if (!isToAdd && !pNode.isCovered()) {
        if (handlePredicateStates) {
          CFANode loc = AbstractStates.extractLocation(pNode);
          isToAdd =
              isPredicateAbstractionState(pNode)
                  || (loc.getNumEnteringEdges() > 0
                      && !(loc instanceof FunctionEntryNode || loc instanceof FunctionExitNode));
        } else {
          for (ARGState parent : pNode.getParents()) {
            if (!isTransferSuccessor(parent, pNode)) {
              isToAdd = true;
            }
            break;
          }
        }
      }
      return isToAdd;
    }

    private boolean isTransferSuccessor(ARGState pPredecessor, ARGState pChild) {
      CFAEdge edge = pPredecessor.getEdgeToChild(pChild);
      try {
        Collection<AbstractState> successors;
        if (edge == null) {
          successors =
              new ArrayList<>(
                  cpa.getTransferRelation()
                      .getAbstractSuccessors(pPredecessor.getWrappedState(), precision));
        } else {
          successors =
              new ArrayList<>(
                  cpa.getTransferRelation()
                      .getAbstractSuccessorsForEdge(
                          pPredecessor.getWrappedState(), precision, edge));
        }
        // check if child is the successor computed by transfer relation
        if (successors.contains(pChild.getWrappedState())) {
          return true;
        }
        // check if check only failed because it is not the same object
        if (!cpa.getStopOperator().stop(pChild.getWrappedState(), successors, precision)) {
          return false;
        }
        Collection<AbstractState> childCollection = Collections.singleton(pChild.getWrappedState());
        for (AbstractState state : successors) {
          if (cpa.getStopOperator().stop(state, childCollection, precision)) {
            return true;
          }
        }
      } catch (InterruptedException | CPAException e) {
        throw new AssertionError(e);
      }
      return false;
    }

    private boolean isPredicateAbstractionState(ARGState pChild) {
      return PredicateAbstractState.getPredicateState(pChild).isAbstractionState();
    }
  }
}
