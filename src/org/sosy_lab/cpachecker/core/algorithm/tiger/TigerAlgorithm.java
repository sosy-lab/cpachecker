/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger;

import com.google.common.collect.Lists;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.tiger.TigerAlgorithmConfiguration.CoverageCheck;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.FQLSpecificationUtil;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.FQLSpecification;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.AutomatonGoal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCase;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestGoalUtils;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.ThreeValuedAnswer;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.WorklistEntryComparator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.Wrapper;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.ErrorPathShrinker;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.timeout.TimeoutCPA;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

public class TigerAlgorithm extends TigerBaseAlgorithm<AutomatonGoal> {


  private TestGoalUtils testGoalUtils;
  private LinkedList<AutomatonGoal> goalsToCover;
  private FQLSpecification fqlSpecification;

  public TigerAlgorithm(
      LogManager pLogger,
      CFA pCfa,
      Configuration pConfig,
      ConfigurableProgramAnalysis pCpa,
      ShutdownNotifier pShutdownNotifier,
      @Nullable final Specification stats) throws InvalidConfigurationException {
    init(pLogger, pCfa, pConfig, pCpa, pShutdownNotifier, stats);

    pShutdownNotifier.register(this);
    testGoalUtils =
        new TestGoalUtils(
            logger,
            new Wrapper(
                pCfa,
                originalMainFunction,
                tigerConfig.shouldUseOmegaLabel()),
            pCfa,
            tigerConfig.shouldOptimizeGoalAutomata(),
            originalMainFunction);
    String preprocessFqlStmt = testGoalUtils.preprocessFQL(tigerConfig.getFqlQuery());
    fqlSpecification = FQLSpecificationUtil.getFQLSpecification(preprocessFqlStmt);
    logger.logf(Level.INFO, "FQL query: %s", fqlSpecification.toString());
  }

  @Override
  public AlgorithmResult getResult() {
    return testsuite;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
    logger.logf(
        Level.INFO,
        "We will not use the provided reached set since it violates the internal structure of Tiger's CPAs");
    logger.logf(Level.INFO, "We empty pReachedSet to stop complaints of an incomplete analysis");

    goalsToCover = initializeTestGoalSet();
    testsuite = new TestSuite<>(bddUtils, goalsToCover, tigerConfig);

    boolean wasSound = true;
    if (!testGeneration(goalsToCover)) {
      logger.logf(Level.WARNING, "Test generation contained unsound reachability analysis runs!");
      wasSound = false;
    }

    writeTestsuite();

    if (wasSound) {
      return AlgorithmStatus.SOUND_AND_PRECISE;
    } else {
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
    }
  }

  private LinkedList<AutomatonGoal> initializeTestGoalSet() {
    LinkedList<ElementaryCoveragePattern> goalPatterns;
    LinkedList<Pair<ElementaryCoveragePattern, Region>> pTestGoalPatterns = new LinkedList<>();

    goalPatterns = testGoalUtils.extractTestGoalPatterns(fqlSpecification);

    for (int i = 0; i < goalPatterns.size(); i++) {
      pTestGoalPatterns.add(Pair.of(goalPatterns.get(i), (Region) null));
    }

    int goalIndex = 1;
    LinkedList<AutomatonGoal> goals = new LinkedList<>();
    for (Pair<ElementaryCoveragePattern, Region> pair : pTestGoalPatterns) {
      AutomatonGoal lGoal =
          testGoalUtils.constructAutomatonGoal(
              goalIndex,
              pair.getFirst(),
              pair.getSecond(),
              tigerConfig.shouldUseOmegaLabel());
      logger.log(Level.INFO, lGoal.getName());
      goals.add(lGoal);
      goalIndex++;
    }

    return goals;
  }

  // TODO add pc to log output
  // TODO add parameter LinkedList<Edges>> pInfeasibilityPropagation
  // TODO add the parameter to runreachabilityanalysis
  @SuppressWarnings("unchecked")
  private boolean testGeneration(LinkedList<AutomatonGoal> pGoalsToCover)
      throws CPAException, InterruptedException {
    boolean wasSound = true;
    boolean retry = false;
    int numberOfTestGoals = pGoalsToCover.size();
    do {
      if (retry) {
        // retry timed-out goals
        boolean order = true;

        if (tigerConfig.getTimeoutIncrement() > 0) {
          long oldCPUTimeLimitPerGoal = tigerConfig.getCpuTimelimitPerGoal();
          tigerConfig.increaseCpuTimelimitPerGoal(tigerConfig.getTimeoutIncrement());
          // tigerConfig.getCpuTimelimitPerGoal() += tigerConfig..getTimeoutIncrement();
          logger.logf(
              Level.INFO,
              "Incremented timeout from %d to %d seconds.",
              oldCPUTimeLimitPerGoal,
              tigerConfig.getCpuTimelimitPerGoal());

          Collection<Entry<Integer, Pair<AutomatonGoal, Region>>> set;
          if (tigerConfig.useOrder()) {
            if (tigerConfig.useInverseOrder()) {
              order = !order;
            }

            // keep original order of goals (or inverse of it)
            if (order) {
              set = new TreeSet<>(WorklistEntryComparator.ORDER_RESPECTING_COMPARATOR);
            } else {
              set = new TreeSet<>(WorklistEntryComparator.ORDER_INVERTING_COMPARATOR);
            }

            set.addAll(testsuite.getTimedOutGoals().entrySet());
          } else {
            set = new LinkedList<>();
            set.addAll(testsuite.getTimedOutGoals().entrySet());
          }

          pGoalsToCover.clear();
          for (Entry<Integer, Pair<AutomatonGoal, Region>> entry : set) {
            pGoalsToCover.add(entry.getValue().getFirst());
          }
          testsuite.getTimedOutGoals().size();
          testsuite.getTimedOutGoals().clear();
        }
      }
      while (!pGoalsToCover.isEmpty()) {
        AutomatonGoal goal = pGoalsToCover.poll();
        int goalIndex = goal.getIndex();

        logger.logf(Level.INFO, "Processing test goal %d of %d.", goalIndex, numberOfTestGoals);

        ReachabilityAnalysisResult result = runReachabilityAnalysis(goal, goalIndex, pGoalsToCover);

        if (result.equals(ReachabilityAnalysisResult.UNSOUND)) {
          logger.logf(Level.WARNING, "Analysis run was unsound!");
          wasSound = false;
        }
        if (result.equals(ReachabilityAnalysisResult.TIMEDOUT)) {
          logger.log(Level.INFO, "Adding timedout Goal to testsuite!");
          testsuite.addTimedOutGoal(goalIndex, goal, null);
          // break;
        }
      }

      if (testsuite.getTimedOutGoals().isEmpty()) {
        logger.logf(Level.INFO, "There were no timed out goals.");
        retry = false;
      } else {
        if (!tigerConfig.getTimeoutStrategy().equals(TimeoutStrategy.RETRY_AFTER_TIMEOUT)) {
          logger.logf(
              Level.INFO,
              "There were timed out goals but retry after timeout strategy is disabled.");
        } else {
          retry = true;
        }
      }
    } while (retry);
    return wasSound;
  }

  @SuppressWarnings("unused")
  private boolean isCovered(int goalIndex, AutomatonGoal lGoal) {
    @SuppressWarnings("unused")
    Region remainingPCforGoalCoverage = lGoal.getPresenceCondition();
    boolean isFullyCovered = false;
    for (TestCase testcase : testsuite.getTestCases()) {
      ThreeValuedAnswer isCovered = testcase.coversGoal(lGoal);
      if (isCovered.equals(ThreeValuedAnswer.UNKNOWN)) {
        logger.logf(
            Level.WARNING,
            "Coverage check for goal %d could not be performed in a precise way!",
            goalIndex);
        continue;
      } else if (isCovered.equals(ThreeValuedAnswer.REJECT)) {
        continue;
      }
    }

    return isFullyCovered;
  }

  private ReachabilityAnalysisResult
      runReachabilityAnalysis(
          AutomatonGoal pGoal,
          int goalIndex,
          LinkedList<AutomatonGoal> pGoalsToCover)
          throws CPAException, InterruptedException {

    // build CPAs for the goal
    ARGCPA lARTCPA = buildCPAs(pGoal);

    if (reachedSet != null) {
      reachedSet.clear();
    }
    reachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.BFS); // TODO why does
                                                                             // TOPSORT not exist
                                                                             // anymore?

    AbstractState lInitialElement =
        lARTCPA.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
    Precision lInitialPrecision =
        lARTCPA
            .getInitialPrecision(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());

    reachedSet.add(lInitialElement, lInitialPrecision);

    // TODO reuse prediccates option
    // if (reusePredicates) {
    // // initialize reused predicate precision
    // PredicateCPA predicateCPA = pArgCPA.retrieveWrappedCpa(PredicateCPA.class);
    //
    // if (predicateCPA != null) {
    // reusedPrecision = (PredicatePrecision)
    // predicateCPA.getInitialPrecision(cfa.getMainFunction(),
    // StateSpacePartition.getDefaultPartition());
    // } else {
    // logger.logf(Level.INFO, "No predicate CPA available to reuse predicates!");
    // }
    // }

    ShutdownManager algNotifier =
        ShutdownManager.createWithParent(startupConfig.getShutdownNotifier());

    startupConfig.getConfig();

    // run analysis
    Region presenceConditionToCover = testsuite.getRemainingPresenceCondition(pGoal);

    Algorithm algorithm = buildAlgorithm(presenceConditionToCover, algNotifier, lARTCPA);
    Pair<Boolean, Boolean> analysisWasSound_hasTimedOut;
    do {

      analysisWasSound_hasTimedOut = runAlgorithm(algorithm, algNotifier);

      // fully explored reachedset, therefore the last "testcase" was already added to the testsuite
      // in this case we break out of the loop, since the goal does not have more feasable goals
      if (!reachedSet.hasWaitingState()) {
        break;
      }

      if (analysisWasSound_hasTimedOut.getSecond()) {
        // timeout, do not retry for other goals
        break;
        // return ReachabilityAnalysisResult.TIMEDOUT;
      }

      AbstractState lastState = reachedSet.getLastState();

      if (lastState == null || !AbstractStates.isTargetState(lastState)) {
        // goals are infeasible, do not continue
        break;
      }

      logger.logf(Level.INFO, "Test goal is feasible.");
      CFAEdge criticalEdge = pGoal.getCriticalEdge();

      // For testing
      Optional<CounterexampleInfo> cexi = ((ARGState) lastState).getCounterexampleInformation();
      if (cexi.isPresent()) {
        logger.log(Level.INFO, "cexi is Present");
      }

      Region testCasePresenceCondition = bddUtils.getRegionFromWrappedBDDstate(lastState);

      if (!cexi.isPresent()/* counterexamples.isEmpty() */) {

        TestCase testcase =
            handleUnavailableCounterexample(criticalEdge, lastState, testCasePresenceCondition);
        testsuite.addTestCase(testcase, pGoal, null);
      } else {
        // test goal is feasible
        logger.logf(Level.INFO, "Counterexample is available.");
        CounterexampleInfo cex = cexi.get();
        if (cex.isSpurious()) {
          logger.logf(Level.WARNING, "Counterexample is spurious!");
        } else {
          // HashMap<String, Boolean> features =
          // for null goal get the presencecondition without the validProduct method
          testCasePresenceCondition = getPresenceConditionFromCex(cex);

          Region simplifiedPresenceCondition = getPresenceConditionFromCexForGoal(cex, pGoal);
          TestCase testcase = createTestcase(cex, testCasePresenceCondition);
          // only add new Testcase and check for coverage if it does not already exist

          testsuite.addTestCase(testcase, pGoal, simplifiedPresenceCondition);

          if (tigerConfig.getCoverageCheck() == CoverageCheck.SINGLE
              || tigerConfig.getCoverageCheck() == CoverageCheck.ALL) {

            // remove covered goals from goalstocover if
            // we want only one featureconfiguration per goal
            // or do not want variability at all
            // otherwise we need to keep the goals, to cover them for each possible configuration
            boolean removeGoalsToCover =
                !bddUtils.isVariabilityAware() || tigerConfig.shouldUseSingleFeatureGoalCoverage();
            HashSet<AutomatonGoal> goalsToCheckCoverage = new HashSet<>(pGoalsToCover);
            if (tigerConfig.getCoverageCheck() == CoverageCheck.ALL) {
              goalsToCheckCoverage.addAll(testsuite.getTestGoals());
            }
            goalsToCheckCoverage.remove(pGoal);
            checkGoalCoverage(goalsToCheckCoverage, testcase, removeGoalsToCover, cex);
          }

        }

      }

      Region remainingPC = testsuite.getRemainingPresenceCondition(pGoal);
      bddUtils.restrictBdd(remainingPC);
    } // continue if we use features and need a testcase for each valid feature config for each goal
      // (continues till infeasability is reached)
    while ((bddUtils.isVariabilityAware() && !tigerConfig.shouldUseSingleFeatureGoalCoverage())
        && !reachedSet.getWaitlist().isEmpty());

    // write ARG to file
    Path argFile = Paths.get("output", "ARG_goal_" + goalIndex + ".dot");
    try (FileWriter w = new FileWriter(argFile.toString())) {
      ARGUtils.writeARGAsDot(w, (ARGState) reachedSet.getFirstState());
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
    }

    if (bddUtils.isVariabilityAware()) {
      testsuite.addInfeasibleGoal(pGoal, testsuite.getRemainingPresenceCondition(pGoal));
    } else {
      if (testsuite.getCoveringTestCases(pGoal) == null
          || testsuite.getCoveringTestCases(pGoal).isEmpty()) {
        testsuite.addInfeasibleGoal(pGoal, null);
      }
    }
    if (!bddUtils.isVariabilityAware()) {
      pGoalsToCover.removeAll(testsuite.getTestGoals());
    }

    if (analysisWasSound_hasTimedOut.getSecond() == true) {
      return ReachabilityAnalysisResult.TIMEDOUT;
    }

    if (analysisWasSound_hasTimedOut.getFirst() == true) {
      return ReachabilityAnalysisResult.SOUND;
    } else {
      return ReachabilityAnalysisResult.UNSOUND;
    }
  }

  public Region getPresenceConditionFromCex(CounterexampleInfo cex) {
    if (!bddUtils.isVariabilityAware()) {
      return null;
    }

    Region pc = bddUtils.makeTrue();
    List<CFAEdge> cfaPath = cex.getTargetPath().getFullPath();
    String validFunc = tigerConfig.getValidProductMethodName();

    for (CFAEdge cfaEdge : cfaPath) {
      String predFun = cfaEdge.getPredecessor().getFunctionName();
      String succFun = cfaEdge.getSuccessor().getFunctionName();
      if (predFun.contains(validFunc)
          && succFun.contains(tigerConfig.getValidProductMethodName())) {
        continue;
      }

      if (cfaEdge instanceof CAssumeEdge) {
        CAssumeEdge assumeEdge = (CAssumeEdge) cfaEdge;
        if (assumeEdge.getExpression() instanceof CBinaryExpression) {

          CBinaryExpression expression = (CBinaryExpression) assumeEdge.getExpression();
          String name = expression.getOperand1().toString() + "@0";

          if (name.contains(tigerConfig.getFeatureVariablePrefix())) {
            Region predNew = bddUtils.createPredicate(name);
            if (assumeEdge.getTruthAssumption()) {
              predNew = bddUtils.makeNot(predNew);
            }

            pc = bddUtils.makeAnd(pc, predNew);
          }
        }
      }
    }

    return pc;
  }

  public Region getPresenceConditionFromCexForGoal(CounterexampleInfo cex, AutomatonGoal pGoal) {
    if (!bddUtils.isVariabilityAware()) {
      return null;
    }

    Region pc = bddUtils.makeTrue();
    List<CFAEdge> cfaPath = cex.getTargetPath().getFullPath();
    String validFunc = tigerConfig.getValidProductMethodName();

    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton = pGoal.getAutomaton();
    Set<NondeterministicFiniteAutomaton.State> lCurrentStates = new HashSet<>();
    Set<NondeterministicFiniteAutomaton.State> lNextStates = new HashSet<>();

    lCurrentStates.add(lAutomaton.getInitialState());

    outer: for (CFAEdge cfaEdge : cfaPath) {
      String predFun = cfaEdge.getPredecessor().getFunctionName();
      String succFun = cfaEdge.getSuccessor().getFunctionName();

      if (!(predFun.contains(validFunc)
          && succFun.contains(tigerConfig.getValidProductMethodName()))) {
        if (cfaEdge instanceof CAssumeEdge) {
          CAssumeEdge assumeEdge = (CAssumeEdge) cfaEdge;
          if (assumeEdge.getExpression() instanceof CBinaryExpression) {

            CBinaryExpression expression = (CBinaryExpression) assumeEdge.getExpression();
            String name = expression.getOperand1().toString() + "@0";

            if (name.contains(tigerConfig.getFeatureVariablePrefix())) {
              Region predNew = bddUtils.createPredicate(name);
              if (assumeEdge.getTruthAssumption()) {
                predNew = bddUtils.makeNot(predNew);
              }

              pc = bddUtils.makeAnd(pc, predNew);
            }
          }
        }
      }

      for (NondeterministicFiniteAutomaton.State lCurrentState : lCurrentStates) {
        // Automaton accepts as soon as it sees a final state (implicit self-loop)
        if (lAutomaton.getFinalStates().contains(lCurrentState)) {
          break outer;
        }

        for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOutgoingEdge : lAutomaton
            .getOutgoingEdges(lCurrentState)) {
          GuardedEdgeLabel lLabel = lOutgoingEdge.getLabel();

          if (!lLabel.hasGuards() && lLabel.contains(cfaEdge)) {
            lNextStates.add(lOutgoingEdge.getTarget());
            if (lAutomaton.getFinalStates().contains(lOutgoingEdge.getTarget())) {
              break outer;
            }
          }
        }
      }

      lCurrentStates.addAll(lNextStates);
    }

    return pc;
  }

  private TestCase createTestcase(final CounterexampleInfo cex, final Region pPresenceCondition) {
    Map<String, BigInteger> inputValues = values.extractInputValues(cex);
    Map<String, BigInteger> outputValus = values.extractOutputValues(cex);
    // calcualte shrinked error path
    List<Pair<CFAEdgeWithAssumptions, Boolean>> shrinkedErrorPath =
        new ErrorPathShrinker()
            .shrinkErrorPath(cex.getTargetPath(), cex.getCFAPathWithAssignments());
    TestCase testcase =
        new TestCase(
            currentTestCaseID,
            inputValues,
            outputValus,
            cex.getTargetPath().asEdgesList(),
            shrinkedErrorPath,
            pPresenceCondition,
            bddUtils);
    currentTestCaseID++;
    return testcase;
  }

  private void checkGoalCoverage(
      Set<AutomatonGoal> pGoalsToCheckCoverage,
      TestCase testCase,
      boolean removeCoveredGoals,
      CounterexampleInfo cex) {
    for (AutomatonGoal goal : testCase.getCoveredGoals(pGoalsToCheckCoverage)) {
      // TODO add infeasiblitpropagaion to testsuite
      Region simplifiedPresenceCondition = getPresenceConditionFromCexForGoal(cex, goal);
      testsuite.updateTestcaseToGoalMapping(testCase, goal, simplifiedPresenceCondition);
      String log = "Goal " + goal.getName() + " is covered by testcase " + testCase.getId();
      if (removeCoveredGoals && !bddUtils.isVariabilityAware()) {
        pGoalsToCheckCoverage.remove(goal);
        log += "and is removed from goal list";
      }
      logger.log(Level.INFO, log);
    }
  }

  private CPAFactory buildAutomataFactory(Automaton goalAutomaton) {
    CPAFactory automataFactory = ControlAutomatonCPA.factory();
    automataFactory
        .setConfiguration(Configuration.copyWithNewPrefix(config, goalAutomaton.getName()));
    automataFactory.setLogger(logger.withComponentName(goalAutomaton.getName()));
    automataFactory.set(cfa, CFA.class);
    automataFactory.set(goalAutomaton, Automaton.class);
    return automataFactory;
  }

  private LinkedList<ConfigurableProgramAnalysis> buildComponentAnalyses(CPAFactory automataFactory)
      throws CPAException {
    List<ConfigurableProgramAnalysis> lAutomatonCPAs = new ArrayList<>(1);// (2);
    try {
      lAutomatonCPAs.add(automataFactory.createInstance());
    } catch (InvalidConfigurationException e1) {
      throw new CPAException("Invalid automata!", e1);
    }

    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<>();
    lComponentAnalyses.addAll(lAutomatonCPAs);

    if (cpa instanceof CompositeCPA) {
      CompositeCPA compositeCPA = (CompositeCPA) cpa;
      lComponentAnalyses.addAll(compositeCPA.getWrappedCPAs());
    } else if (cpa instanceof ARGCPA) {
      lComponentAnalyses.addAll(((ARGCPA) cpa).getWrappedCPAs());
    } else {
      lComponentAnalyses.add(cpa);
    }
    return lComponentAnalyses;
  }

  private ARGCPA buildARGCPA(
      LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses,
      Specification goalAutomatonSpecification) {
    ARGCPA lARTCPA;
    try {

      // create timeout CPA
      CPAFactory toFactory = TimeoutCPA.factory();
      toFactory.setConfiguration(startupConfig.getConfig());
      toFactory.setLogger(logger);
      timeoutCPA = (TimeoutCPA) toFactory.createInstance();
      timeoutCPA.setWalltime(tigerConfig.getCpuTimelimitPerGoal());
      lComponentAnalyses.add(timeoutCPA);

      // create composite CPA
      CPAFactory lCPAFactory = CompositeCPA.factory();
      lCPAFactory.setChildren(lComponentAnalyses);
      lCPAFactory.setConfiguration(startupConfig.getConfig());
      lCPAFactory.setLogger(logger);
      lCPAFactory.set(cfa, CFA.class);


      ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();

      // create ART CPA
      CPAFactory lARTCPAFactory = ARGCPA.factory();
      lARTCPAFactory.set(cfa, CFA.class);
      lARTCPAFactory.setChild(lCPA);
      lARTCPAFactory.setConfiguration(startupConfig.getConfig());
      lARTCPAFactory.setLogger(logger);
      lARTCPAFactory.set(goalAutomatonSpecification, Specification.class);

      lARTCPA = (ARGCPA) lARTCPAFactory.createInstance();
    } catch (InvalidConfigurationException | CPAException e) {
      throw new RuntimeException(e);
    }
    return lARTCPA;
  }

  private ARGCPA buildCPAs(AutomatonGoal pGoal)// LinkedList<Goal> pGoalsToCover)
      throws CPAException {
    Automaton goalAutomaton = pGoal.createControlAutomaton();
    Specification goalAutomatonSpecification =
        Specification.fromAutomata(Lists.newArrayList(goalAutomaton));

    CPAFactory automataFactory = buildAutomataFactory(goalAutomaton);
    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses =
        buildComponentAnalyses(automataFactory);
    return buildARGCPA(lComponentAnalyses, goalAutomatonSpecification);

  }

  private Algorithm buildAlgorithm(
      Region pRemainingPresenceCondition,
      ShutdownManager algNotifier,
      ARGCPA lARTCPA) throws CPAException {
    Algorithm algorithm;
    try {
      Configuration internalConfiguration =
          Configuration.builder().loadFromFile(tigerConfig.getAlgorithmConfigurationFile()).build();

      Set<UnmodifiableReachedSet> unmodifiableReachedSets = new HashSet<>();

      unmodifiableReachedSets.add(reachedSet);

      AggregatedReachedSets aggregatedReachedSets =
          new AggregatedReachedSets(unmodifiableReachedSets);

      CoreComponentsFactory coreFactory =
          new CoreComponentsFactory(
              internalConfiguration,
              logger,
              algNotifier.getNotifier(),
              aggregatedReachedSets);

      algorithm = coreFactory.createAlgorithm(lARTCPA, cfa, stats);

      if (algorithm instanceof CEGARAlgorithm) {
        CEGARAlgorithm cegarAlg = (CEGARAlgorithm) algorithm;

        ARGStatistics lARTStatistics;
        try {
          lARTStatistics = new ARGStatistics(internalConfiguration, logger, lARTCPA, stats, cfa);
        } catch (InvalidConfigurationException e) {
          throw new RuntimeException(e);
        }
        Set<Statistics> lStatistics = new HashSet<>();
        lStatistics.add(lARTStatistics);
        cegarAlg.collectStatistics(lStatistics);
      }

      bddUtils.restrictBdd(pRemainingPresenceCondition);
    } catch (IOException | InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }
    return algorithm;
  }

  private Pair<Boolean, Boolean> runAlgorithm(Algorithm algorithm, ShutdownManager algNotifier)
      throws CPAEnabledAnalysisPropertyViolationException, CPAException, InterruptedException {
    boolean analysisWasSound = false;
    boolean hasTimedOut = false;

    analysisWasSound = algorithm.run(reachedSet).isSound();
    hasTimedOut = timeoutCPA.hasTimedout();
    if (hasTimedOut) {
      logger.logf(Level.INFO, "Test goal timed out!");
    }
    return Pair.of(analysisWasSound, hasTimedOut);
  }

  private TestCase handleUnavailableCounterexample(
      CFAEdge criticalEdge,
      AbstractState lastState,
      Region pPresenceCondition) {

    logger.logf(Level.INFO, "Counterexample is not available.");

    LinkedList<CFAEdge> trace = new LinkedList<>();

    // Try to reconstruct a trace in the ARG and shrink it
    ARGState argState = AbstractStates.extractStateByType(lastState, ARGState.class);
    ARGPath path = ARGUtils.getOnePathTo(argState);
    // List<CFAEdge> shrinkedErrorPath = null;
    if (path != null) {
      // cannot shrink error path without conterexample
      // shrinkedErrorPath = new ErrorPathShrinker().shrinkErrorPath(path);
    }

    Collection<ARGState> parents;
    parents = argState.getParents();

    while (!parents.isEmpty()) {

      ARGState parent = null;

      for (ARGState tmp_parent : parents) {
        parent = tmp_parent;
        break; // we just choose some parent
      }

      CFAEdge edge = parent.getEdgeToChild(argState);
      trace.addFirst(edge);

      // TODO Alex?
      if (edge.equals(criticalEdge)) {
        logger.logf(
            Level.INFO,
            "*********************** extract abstract state ***********************");
      }

      argState = parent;
      parents = argState.getParents();
    }

    Map<String, BigInteger> inputValues = new LinkedHashMap<>();
    Map<String, BigInteger> outputValues = new LinkedHashMap<>();

    TestCase result =
        new TestCase(
            currentTestCaseID,
            inputValues,
            outputValues,
            trace,
            null,
            pPresenceCondition,
            bddUtils);
    currentTestCaseID++;
    return result;
  }

  @Override
  public void shutdownRequested(String pArg0) {
    for (AutomatonGoal goal : goalsToCover) {
      if (!(testsuite.isGoalCovered(goal)
          || testsuite.isInfeasible(goal)
          || testsuite.isGoalTimedOut(goal))) {
        testsuite.addTimedOutGoal(goal.getIndex(), goal, null);
      }
    }
    writeTestsuite();
  }
}
