/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.mav;

import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.AnalysisNotifier.AnalysisListener;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.AdjustablePrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.mav.RuleSpecification.SpecificationStatus;


/**
 * This class implements Conditional Multi Aspect Verification (CMAV).
 */
public class ConditionalMAVListener implements AnalysisListener {

  private LogManager logger;
  private Configuration config;

  public ConditionalMAVListener(Configuration config, LogManager logger)
      throws InvalidConfigurationException {
    this.logger = logger;
    this.config = config;
    mav = new MultiAspectVerification(this.config);
  }

  private MultiAspectVerification mav;


  /**
   * Check internal timers:
   * Abstraction Time Limit (ATL) - set limit on a single abstraction construction;
   * in case of exhaustion specification will be disabled with verdict 'UNKNOWN' and Hard Time Limit will be started.
   * Hard Time Limit (HTL) - starts after the exhaustion of any "soft" time limit (for example, ATL or STL)
   * and sets limit on selecting new specification to check;
   * in case of exhaustion CPAchecker will be terminated with global verdict 'UNKNOWN'.
   * @throws CPAException
   */
  @Override
  public void beforeAbstractionStep(ReachedSet reachedSet) throws CPAException {
    // Get cpu time.
    Long currentCpuTime = mav.getCurrentCpuTime();

    // Check HTL.
    if (mav.getLastCheckedSpecification() == null &&
        !mav.checkHardTimeLimit(currentCpuTime))
    {
      // Stop analysis with verdict UNKNOWN.
      throw new CPAException("Hard Time Limit was reached");
    }

    // Check ATL.
    if (mav.getLastCheckedSpecification() != null &&
        !mav.checkAbstractionTimeLimit(currentCpuTime))
    {
      ControlAutomatonCPA controlAutomatonCPA = mav.getCurrentControlAutomaton();
      SpecificationKey specificationKey = mav.getLastCheckedSpecification();

      mav.updateTime(specificationKey);
      mav.changeSpecificationStatus(specificationKey, SpecificationStatus.UNKNOWN);
      mav.disableSpecification(controlAutomatonCPA, specificationKey);
      if (mav.cleanPrecision(reachedSet, specificationKey))
      {
        logger.log(Level.WARNING, "Cleaning precisions has exhausted its timeout and " +
            "was stopped");
      }

      // Reset last checked specification (start checking for HTL).
      mav.setLastCheckedSpecification(null);

      logger.log(Level.INFO, "Specification " + specificationKey +
          " has exhausted its Abstraction Time Limit " +
          "and will not be checked anymore");
      mav.printToFile();
    }

  }

  /**
   * Fix verdicts for specifications after analysis and set global result.
   * If there is any UNSAFE specification results then global result must be FALSE.
   * If analysis was finished normally, then all specification verdicts CHECKING should be
   * changed to SAFE.
   */
  @Override
  public Result updateResult(Result pResult) {
    Result result = pResult;
    // Multi-aspect verification verdicts.
    if (result == Result.TRUE || result == Result.FALSE)
    {
      for (RuleSpecification ruleSpecification : mav.getAllSpecifications()) {
        if (ruleSpecification.getStatus() == SpecificationStatus.CHECKING) {
          ruleSpecification.setStatus(SpecificationStatus.SAFE);
        }
        if (result == Result.TRUE && ruleSpecification.getStatus() == SpecificationStatus.UNSAFE)
        {
          result = Result.FALSE;
        }
      }
      try {
        // Update printed specification results.
        mav.printToFile();
      } catch (CPAException e) {
        logger.logUserException(Level.SEVERE, e, null);
      }
    }
    return result;
  }

  /**
   * Start timer and print initial specifications set.
   */
  @Override
  public void onStartAnalysis() throws CPAException {
    mav.startTimers();
    mav.printToFile();
  }

  /**
   * Process specification automatons in order to create specifications.
   */
  @Override
  public void onSpecificationAutomatonCreate(List<Automaton> automata) {
    mav.addNewSpecification(automata);
  }

  /**
   * Define violated specification.
   * It is supposed that only one specification can be violated at once.
   * In case of few violated specifications (for example, the same specification was specified twice),
   * one of them will be selected and marked as Current.
   */
  @Override
  public void beforeRefinement(ARGState lastElement) {
    // Find violated specification.
    List<AbstractState> targetList = lastElement.getTargetLeaves();
    assert targetList.size() == 1; // Only one target automaton is expected.
    assert targetList.get(0) instanceof AutomatonState;
    AutomatonState targetState = (AutomatonState)targetList.get(0);
    SpecificationKey specificationKey = mav.getViolatedSpecification(targetState);

    // Mark current violated specification.
    mav.setCurrentSpecification(specificationKey, targetState.getAutomaton());
  }

  /**
   * @throws CPAException
   * For true errors specifications will be disabled (unless option mav.analysis.stopAfterError=false
   * was specified) and marked as UNSAFE. In other case Specification Time Limit (STL)
   * will be checked. In case of exhaustion will be disabled and marked as UNKNOWN.
   */
  @Override
  public void afterRefinement(boolean isSpurious, ReachedSet pReached, ARGReachedSet reached, ARGPath path, int refinementNumber)
      throws CPAException {

    // Get Current specification.
    SpecificationKey specificationKey = mav.getCurrentSpecification();

    // Update internal MAV timers.
    mav.updateTime(specificationKey);
    Long cpuTime = mav.getCpuTime(specificationKey);

    if (!isSpurious)
    {
      // Error trace has been found (counterexample is false).
      // Current specification will have Unsafe verdict.
      if (mav.isStopAfterError())
      {
        // Multi-Aspect Verification with First Error Analysis.
        stopCheckingSpecification(SpecificationStatus.UNSAFE, pReached, reached, path, specificationKey);
        mav.setLastCheckedSpecification(specificationKey);
      }
      else
      {
        // Multi-Aspect Verification with Multiple Error Analysis.
        // Change status and continue analysis.
        mav.changeSpecificationStatus(specificationKey, SpecificationStatus.UNSAFE);

        // Check STL.
        if (!mav.checkSpecificationTimeLimit(cpuTime))
        {
          stopCheckingSpecification(SpecificationStatus.UNSAFE, pReached, reached, path, specificationKey);
          mav.setLastCheckedSpecification(null);
        }
        else
        {
          mav.setLastCheckedSpecification(specificationKey);
        }
      }
    }
    else
    {
      // Error trace has not been found (counterexample is true).
      // Analysis should continue, unless current specification
      // have exhausted its time limit.

      // Check STL.
      if (!mav.checkSpecificationTimeLimit(cpuTime))
      {
        if (mav.getLastCheckedSpecification() == null)
        {
          // Previous specification was Unknown, that is why this specification
          // should be rechecked without the previous one.
          stopCheckingSpecification(SpecificationStatus.RECHECK, pReached, reached, path, specificationKey);
        }
        else
        {
          // Mark this specification as Unknown.
          stopCheckingSpecification(SpecificationStatus.UNKNOWN, pReached, reached, path, specificationKey);
        }
        logger.log(Level.INFO, "Specification " + specificationKey +
            " has exhausted its Specification Time Limit " +
            "and will not be checked anymore");

        // Reset last checked specification (start checking HTL).
        mav.setLastCheckedSpecification(null);
      }
      else
      {
        // Continue analysis.
        // Save current specification
        mav.setLastCheckedSpecification(specificationKey);
      }
    }

    logger.log(Level.INFO, "Refinement " + refinementNumber + " finished with " +
        isSpurious + ": " + specificationKey +
        "; sum time: " + cpuTime + "ms");
    logger.log(Level.ALL, "Current results for all rule specifications:\n" + mav);
    logger.log(Level.ALL, "Last checked rule specification: " + mav.getLastCheckedSpecification());

    // Print results into file if specified.
    mav.printToFile();

  }

  /**
   * Perform required actions to stop checking this specification.
   * @param specificationStatus
   * @param pReached
   * @param reached
   * @param path
   * @param specificationKey
   * @throws CPAException
   */
  private void stopCheckingSpecification(SpecificationStatus specificationStatus,
      ReachedSet pReached,
      ARGReachedSet reached,
      ARGPath path,
      SpecificationKey specificationKey) throws CPAException {

    ControlAutomatonCPA controlAutomatonCPA = mav.getCurrentControlAutomaton();
    mav.changeSpecificationStatus(specificationKey, specificationStatus);
    mav.disableSpecification(controlAutomatonCPA, specificationKey);
    if (mav.cleanPrecision(pReached, specificationKey))
    {
      logger.log(Level.WARNING, "Cleaning precisions has exhausted its timeout and " +
          "was stopped");
    }

    // Rebuild last state.
    try {
      reached.removeSubtree(path.asStatesList().get(path.size() - 1));
    } catch (Exception e) {
      // If last state is the first state (root) then do nothing.
      logger.log(Level.WARNING, "Can't rebuild last state");
    }
  }

  /**
   * Save current adjustable precision.
   */
  @Override
  public void onPrecisionIncrementCreate(AdjustablePrecision adjustablePrecision) {
    mav.addPrecision(adjustablePrecision);
  }

}
