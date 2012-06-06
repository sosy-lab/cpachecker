/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageCPA;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

@Options(prefix="adjustableconditions")
public class RestartWithConditionsAlgorithm implements Algorithm {

  private final Algorithm innerAlgorithm;
  private final LogManager logger;

  private final List<? extends AdjustableConditionCPA> conditionCPAs;

  @Option(description="maximum number of condition adjustments (-1 for infinite)")
  @IntegerOption(min=-1)
  private int adjustmentLimit = -1;

  public RestartWithConditionsAlgorithm(Algorithm pAlgorithm,
        ConfigurableProgramAnalysis pCpa, Configuration config, LogManager pLogger)
        throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    innerAlgorithm = pAlgorithm;

    if (!(pCpa instanceof ARGCPA)) {
      throw new InvalidConfigurationException("ARGCPA needed for RestartWithConditionsAlgorithm");
    }
    ARGCPA cpa = (ARGCPA)pCpa;
    if (cpa.retrieveWrappedCpa(AssumptionStorageCPA.class) == null) {
      throw new InvalidConfigurationException("AssumptionStorageCPA needed for RestartWithConditionsAlgorithm");
    }

    conditionCPAs = ImmutableList.copyOf(Iterables.filter(CPAs.asIterable(cpa), AdjustableConditionCPA.class));
  }

  @Override
  public boolean run(ReachedSet pReached) throws CPAException, InterruptedException {
    boolean sound = true;

    int count = 0;

    do {
      // run the inner algorithm to fill the reached set
      sound &= innerAlgorithm.run(pReached);

      if (Iterables.any(pReached, AbstractStates.IS_TARGET_STATE)) {
        return sound;
      }

      count++;
      if (adjustmentLimit >= 0 && count > adjustmentLimit) {
        logger.log(Level.INFO, "Terminating because adjustment limit has been reached.");
        return sound;
      }

      List<AbstractState> elementsWithAssumptions = getStatesWithAssumptions(pReached);

      // if there are elements that an assumption is generated for
      if (!elementsWithAssumptions.isEmpty()) {
        logger.log(Level.INFO, "Adjusting heuristics thresholds.");
        // if necessary, this will re-add elements to the waitlist
        adjustThresholds(elementsWithAssumptions, pReached);
      }

      // adjust precision of condition CPAs
      for (AdjustableConditionCPA condCpa : conditionCPAs) {
        if (!condCpa.adjustPrecision()) {
          // this cpa said "do not continue"
          logger.log(Level.INFO, "Terminating because of", condCpa.getClass().getSimpleName());
          return sound;
        }
      }

    } while (pReached.hasWaitingState());

    return sound;
  }

  private List<AbstractState> getStatesWithAssumptions(ReachedSet reached) {

    List<AbstractState> retList = new ArrayList<AbstractState>();

    for (AbstractState element : reached) {

      // TODO do we need target states?
//      if (AbstractStates.isTargetElement(element)) {
//        // create assumptions for target state
//        retList.add(element);
//
//      } else {

        // check if stored assumption is not "true"
        AssumptionStorageState e = AbstractStates.extractStateByType(element, AssumptionStorageState.class);

        if (!e.getAssumption().isTrue()
            || !e.getStopFormula().isTrue()) {

          retList.add(element);
        }
    }

    return retList;
  }

  private void adjustThresholds(List<AbstractState> pElementsWithAssumptions, ReachedSet pReached) {

    ARGReachedSet reached = new ARGReachedSet(pReached);
    for (AbstractState e: pElementsWithAssumptions) {
      ARGState argElement = (ARGState)e;

      for (ARGState parent : ImmutableSet.copyOf(argElement.getParents())){
        reached.removeSubtree(parent);
      }
    }
  }
}