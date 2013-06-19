/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

@Options
public class ReachedSetStrategy extends AbstractStrategy {

  protected AbstractState[] reachedSet;
  protected Multimap<CFANode, AbstractState> statesPerLocation;
  protected PropertyCheckerCPA cpa;

  public ReachedSetStrategy(Configuration pConfig, LogManager pLogger, PropertyCheckerCPA pCpa) throws InvalidConfigurationException {
    super(pConfig, pLogger);
    cpa= pCpa;
  }


  @Override
  public void constructInternalProofRepresentation(UnmodifiableReachedSet pReached) {
      reachedSet = new AbstractState[pReached.size()];
      pReached.asCollection().toArray(reachedSet);
  }

  // TODO does it work with analysis without locations?
  @Override
  public boolean checkCertificate(final ReachedSet pReachedSet) throws CPAException, InterruptedException {

    /*also restrict stop to elements of same location as analysis does*/
    StopOperator stop = cpa.getStopOperator();
    Precision initialPrec = pReachedSet.getPrecision(pReachedSet.getFirstState());

    // check if initial element covered
    AbstractState initialState = pReachedSet.popFromWaitlist();
    assert (initialState == pReachedSet.getFirstState() && pReachedSet.size() == 1);

    try {
      stats.stopTimer.start();
      if (!stop.stop(initialState, statesPerLocation.get(AbstractStates.extractLocation(initialState)), initialPrec)) {
        logger.log(Level.FINE, "Cannot check that initial element is covered by result.");
        return false;
      }
    } catch (CPAException e) {
      logger.logException(Level.FINE, e, "Stop check failed for initial element.");
      return false;
    } finally {
      stats.stopTimer.stop();
    }


    // check if elements form transitive closure
    Collection<? extends AbstractState> successors;
    for (AbstractState state : reachedSet) {

      CPAchecker.stopIfNecessary();
      stats.countIterations++;

      try {
        stats.transferTimer.start();
        successors = cpa.getTransferRelation().getAbstractSuccessors(state, initialPrec, null);
        stats.transferTimer.stop();

        for (AbstractState succ : successors) {
          try {
            stats.stopTimer.start();
            if (!stop.stop(succ, statesPerLocation.get(AbstractStates.extractLocation(succ)), initialPrec)) {
              logger.log(Level.FINE, "Cannot check that result is transitive closure.", "Successor ", succ,
                  "of element ", state, "not covered by result.");
              return false;
            }
          } finally {
            stats.stopTimer.stop();
          }
        }
      } catch (CPATransferException | InterruptedException e) {
        logger.logException(Level.FINE, e, "Computation of successors failed.");
        return false;
      } catch (CPAException e) {
        logger.logException(Level.FINE, e, "Stop check failed for successor.");
        return false;
      }
    }
    stats.propertyCheckingTimer.start();
    try {
      return cpa.getPropChecker().satisfiesProperty(Arrays.asList(reachedSet));
    } finally {
      stats.propertyCheckingTimer.stop();
    }
  }

  @Override
  protected Object getProofToWrite(UnmodifiableReachedSet pReached) {
    constructInternalProofRepresentation(pReached);
    return reachedSet;
  }


  @Override
  protected void prepareForChecking(Object pReadProof) throws InvalidConfigurationException {
    if (!(pReadProof instanceof AbstractState[])) { throw new InvalidConfigurationException(
        "Proof Type requires reached set as set of abstract states."); }
    reachedSet = (AbstractState[])pReadProof;
    orderReachedSetByLocation(reachedSet);

  }

  private void orderReachedSetByLocation(AbstractState[] pReached) {
    statesPerLocation = HashMultimap.create();
    for (AbstractState state : pReached) {
      statesPerLocation.put(AbstractStates.extractLocation(state), state);
    }
  }

}
