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
package org.sosy_lab.cpachecker.pcc.strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPABackwards;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

@Options
public class PartialReachedSetStrategy extends ReachedSetStrategy {


  public PartialReachedSetStrategy(Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier, PropertyCheckerCPA pCpa) throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pCpa);
  }

  @Override
  public void constructInternalProofRepresentation(UnmodifiableReachedSet pReached) {
    reachedSet = computePartialReachedSet(pReached);
    orderReachedSetByLocation(reachedSet);
  }

  private AbstractState[] computePartialReachedSet(UnmodifiableReachedSet pReached) {
    // TODO probably change computation to add if successor of element not contained
    ArrayList<AbstractState> result = new ArrayList<>();
    CFANode node;
    for (AbstractState state : pReached.asCollection()) {
      node = AbstractStates.extractLocation(state);
      if (node == null || node.getNumEnteringEdges() > 1
          || (node.getNumLeavingEdges() > 0 && node.getLeavingEdge(0).getEdgeType() == CFAEdgeType.FunctionCallEdge)) {
        result.add(state);
      }
    }
    if (!result.contains(pReached.getFirstState())) {
      result.add(pReached.getFirstState());
    }
    AbstractState[] arrayRep = new AbstractState[result.size()];
    result.toArray(arrayRep);
    return arrayRep;
  }

  @Override
  protected void prepareForChecking(Object pReadProof) throws InvalidConfigurationException {
    if (CPAs.retrieveCPA(cpa, LocationCPABackwards.class) != null) { throw new InvalidConfigurationException(
        "Partial reached set not supported as certificate for backward analysis"); }
    super.prepareForChecking(pReadProof);
  }

  @Override
  public boolean checkCertificate(final ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // TODO for parallelization use Runtime.getRuntime().availableProcessors(); to identify maximal number of parallel threads
      List<AbstractState> certificate = new ArrayList<>(reachedSet.length);
      for (AbstractState elem : reachedSet) {
        certificate.add(elem);
      }

      /*also restrict stop to elements of same location as analysis does*/
      StopOperator stop = cpa.getStopOperator();
      Precision initialPrec = pReachedSet.getPrecision(pReachedSet.getFirstState());

      // check initial element
      AbstractState initialState = pReachedSet.popFromWaitlist();
      assert (initialState == pReachedSet.getFirstState() && pReachedSet.size() == 1);

      try {
        stats.stopTimer.start();
        if (!stop.stop(initialState, statesPerLocation.get(AbstractStates.extractLocation(initialState)), initialPrec)) {
          logger.log(Level.FINE, "Initial element not in partial reached set.", "Add to elements whose successors ",
              "must be computed.");
          addElement(initialState, certificate);
        }
      } catch (CPAException e) {
        logger.logException(Level.FINE, e, "Stop check failed for initial element.");
        return false;
      } finally {
        stats.stopTimer.stop();
      }


      // check if elements form transitive closure
      Collection<? extends AbstractState> successors;
      while (!certificate.isEmpty()) {

        shutdownNotifier.shutdownIfNecessary();
        stats.countIterations++;

        try {
          stats.transferTimer.start();
          successors =
              cpa.getTransferRelation().getAbstractSuccessors(certificate.remove(certificate.size() - 1), initialPrec,
                  null);
          stats.transferTimer.stop();

          for (AbstractState succ : successors) {
            try {
              stats.stopTimer.start();
              if (!stop.stop(succ, statesPerLocation.get(AbstractStates.extractLocation(succ)), initialPrec)) {
                logger.log(Level.FINE, "Successor ", succ, " not in partial reached set.",
                    "Add to elements whose successors ",
                    "must be computed.");
                if (AbstractStates.extractLocation(succ).getNumEnteringEdges() > 1) {
                  stop.stop(succ, statesPerLocation.get(AbstractStates.extractLocation(succ)), initialPrec);
                }
                addElement(succ, certificate);
              }
            } finally {
              stats.stopTimer.stop();
            }
          }
        } catch (CPATransferException e) {
          logger.logException(Level.FINE, e, "Computation of successors failed.");
          return false;
        } catch (CPAException e) {
          logger.logException(Level.FINE, e, "Stop check failed for successor.");
          return false;
        }
      }
      stats.propertyCheckingTimer.start();
      try {
        return cpa.getPropChecker().satisfiesProperty(statesPerLocation.values());
      } finally {
        stats.propertyCheckingTimer.stop();
      }
    }

    protected void addElement(AbstractState element, List<AbstractState> insertIn) {
      insertIn.add(insertIn.size(), element);
      CFANode node = AbstractStates.extractLocation(element);
      statesPerLocation.put(node, element);
    }
}
