// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageCPA;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

@Options(prefix = "adjustableconditions")
public class RestartWithConditionsAlgorithm implements Algorithm {

  private final Algorithm innerAlgorithm;
  private final LogManager logger;
  private final ARGCPA cpa;

  private final List<? extends AdjustableConditionCPA> conditionCPAs;

  @Option(secure = true, description = "maximum number of condition adjustments (-1 for infinite)")
  @IntegerOption(min = -1)
  private int adjustmentLimit = -1;

  public RestartWithConditionsAlgorithm(
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCpa,
      Configuration config,
      LogManager pLogger)
      throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    innerAlgorithm = pAlgorithm;

    if (!(pCpa instanceof ARGCPA)) {
      throw new InvalidConfigurationException("ARGCPA needed for RestartWithConditionsAlgorithm");
    }
    cpa = (ARGCPA) pCpa;
    if (cpa.retrieveWrappedCpa(AssumptionStorageCPA.class) == null) {
      throw new InvalidConfigurationException(
          "AssumptionStorageCPA needed for RestartWithConditionsAlgorithm");
    }

    conditionCPAs = CPAs.asIterable(cpa).filter(AdjustableConditionCPA.class).toList();
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReached) throws CPAException, InterruptedException {
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

    int count = 0;

    do {
      // run the inner algorithm to fill the reached set
      status = status.update(innerAlgorithm.run(pReached));

      if (pReached.wasTargetReached() && status.isPrecise()) {
        return status;
      }

      count++;
      if (adjustmentLimit >= 0 && count > adjustmentLimit) {
        logger.log(Level.INFO, "Terminating because adjustment limit has been reached.");
        return status;
      }

      List<AbstractState> statesWithAssumptions = getStatesWithAssumptions(pReached);

      // if there are states that an assumption is generated for
      if (!statesWithAssumptions.isEmpty()) {
        logger.log(Level.INFO, "Adjusting heuristics thresholds.");
        // if necessary, this will re-add state to the waitlist
        adjustThresholds(statesWithAssumptions, pReached);
      }

      // adjust precision of condition CPAs
      for (AdjustableConditionCPA condCpa : conditionCPAs) {
        if (!condCpa.adjustPrecision()) {
          // this cpa said "do not continue"
          logger.log(Level.INFO, "Terminating because of", condCpa.getClass().getSimpleName());
          return status;
        }
      }

    } while (pReached.hasWaitingState());

    return status;
  }

  private List<AbstractState> getStatesWithAssumptions(ReachedSet reached) {

    List<AbstractState> retList = new ArrayList<>();

    for (AbstractState state : reached) {

      // TODO do we need target states?
      //      if (AbstractStates.isTargetState(state)) {
      //        // create assumptions for target state
      //        retList.add(state);
      //
      //      } else {

      // check if stored assumption is not "true"
      AssumptionStorageState s =
          AbstractStates.extractStateByType(state, AssumptionStorageState.class);

      if (!s.isAssumptionTrue() || !s.isStopFormulaTrue()) {

        retList.add(state);
      }
    }

    return retList;
  }

  private void adjustThresholds(List<AbstractState> pStatesWithAssumptions, ReachedSet pReached)
      throws InterruptedException {

    ARGReachedSet reached = new ARGReachedSet(pReached, cpa);
    for (AbstractState s : pStatesWithAssumptions) {
      ARGState argState = (ARGState) s;

      for (ARGState parent : ImmutableSet.copyOf(argState.getParents())) {
        reached.removeSubtree(parent);
      }
    }
  }
}
