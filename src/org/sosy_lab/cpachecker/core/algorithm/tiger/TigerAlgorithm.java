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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.counterexamplecheck.CounterexampleCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.StartupConfig;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.FQLSpecificationUtil;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.PredefinedCoverageCriteria;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.FQLSpecification;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.SingletonECPEdgeSet;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.InverseGuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.clustering.InfeasibilityPropagation.Prediction;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCase;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestGoalUtils;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestStep;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.ThreeValuedAnswer;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.WorkerRunnable;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.WorklistEntryComparator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.Wrapper;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.ErrorPathShrinker;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton.State;
import org.sosy_lab.cpachecker.util.predicates.AssignableTerm;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.java_smt.api.BooleanFormula;


@Options(prefix = "tiger")
public class TigerAlgorithm implements Algorithm {

  @Option(
      secure = true,
      name = "fqlQuery",
      description = "Coverage criterion given as an FQL query")
  private String fqlQuery = PredefinedCoverageCriteria.BASIC_BLOCK_COVERAGE; // default is basic block coverage

  @Option(
      secure = true,
      name = "optimizeGoalAutomata",
      description = "Optimize the test goal automata")
  private boolean optimizeGoalAutomata = true;

  @Option(
      secure = true,
      name = "limitsPerGoal.time.cpu",
      description = "Time limit per test goal in seconds (-1 for infinity).")
  private long cpuTimelimitPerGoal = -1;

  @Option(
      secure = true,
      name = "algorithmConfigurationFile",
      description = "Configuration file for internal cpa algorithm.")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path algorithmConfigurationFile = Paths.get("config/tiger-internal-algorithm.properties");

  @Option(secure = true, name = "reuseARG", description = "Reuse ARG across test goals")
  private boolean reuseARG = true;

  @Option(
      secure = true,
      name = "limitsPerGoal.time.cpu.increment",
      description = "Value for which timeout gets incremented if timed-out goals are re-processed.")
  private int timeoutIncrement = 0;

  @Option(
      secure = true,
      name = "testsuiteFile",
      description = "Filename for output of generated test suite")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path testsuiteFile = Paths.get("testsuite.txt");

  @Option(
      secure = true,
      name = "useOrder",
      description = "Enforce the original order each time a new round of re-processing of timed-out goals begins.")
  private boolean useOrder = true;

  @Option(
      secure = true,
      name = "inverseOrder",
      description = "Inverses the order of test goals each time a new round of re-processing of timed-out goals begins.")
  private boolean inverseOrder = true;

  @Option(
      secure = true,
      name = "numberOfTestGoalsPerRun",
      description = "The number of test goals processed per CPAchecker run (0: all test goals in one run).")
  private int numberOfTestGoalsPerRun = 1;

  @Option(
      secure = true,
      description = "File for saving processed goal automata in DOT format (%s will be replaced with automaton name)")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate dumpGoalAutomataTo = PathTemplate.ofFormatString("Automaton_%s.dot");

  @Option(
      secure = true,
      name = "reusePredicates",
      description = "Reuse predicates across modifications of an ARG.")
  private boolean reusePredicates = true;

  @Option(
      secure = true,
      name = "allCoveredGoalsPerTestCase",
      description = "Returns all test goals covered by a test case.")
  private boolean allCoveredGoalsPerTestCase = false;

  @Option(
      secure = true,
      name = "checkCoverage",
      description = "Checks whether a test case for one goal covers another test goal")
  private boolean checkCoverage = true;

  @Option(
      secure = true,
      name = "printPathFormulasPerGoal",
      description = "Writes all target state path formulas for a goal in a file.")
  private boolean printPathFormulasPerGoal = false;

  @Option(
      secure = true,
      name = "timeoutStrategy",
      description = "How to proceed with timed-out goals if some time remains after processing all other goals.")
  private TimeoutStrategy timeoutStrategy = TimeoutStrategy.SKIP_AFTER_TIMEOUT;

  private FQLSpecification fqlSpecification;
  private final LogManager logger;
  private final CFA cfa;
  private ConfigurableProgramAnalysis cpa;
  private CoverageSpecificationTranslator mCoverageSpecificationTranslator;
  public static String originalMainFunction = null;
  private int statistics_numberOfTestGoals;
  private Wrapper wrapper;
  private GuardedEdgeLabel mAlphaLabel;
  private GuardedEdgeLabel mOmegaLabel;
  private InverseGuardedEdgeLabel mInverseAlphaLabel;
  private final Configuration config;
  private ReachedSet outsideReachedSet = null;
  private ReachedSet reachedSet = null;
  private StartupConfig startupConfig;
  private String programDenotation;
  private Specification stats;
  private TestSuite testsuite;

  private TestGoalUtils testGoalUtils = null;

  private final ReachedSetFactory reachedSetFactory;

  private int statistics_numberOfProcessedTestGoals = 0;

  private PredicatePrecision reusedPrecision = null;

  final private ShutdownManager mainShutdownManager;

  private Refiner refiner;

  private int testCaseId = 0;

  private Map<Goal, List<List<BooleanFormula>>> targetStateFormulas;

  private Map<CFAEdge, List<NondeterministicFiniteAutomaton<GuardedEdgeLabel>>> edgeToTgaMapping;

  private Prediction[] lGoalPrediction;


  enum ReachabilityAnalysisResult {
    SOUND,
    UNSOUND,
    TIMEDOUT
  }

  enum TimeoutStrategy {
    SKIP_AFTER_TIMEOUT,
    RETRY_AFTER_TIMEOUT
  }

  public TigerAlgorithm(LogManager pLogger, CFA pCfa, Configuration pConfig,
      ConfigurableProgramAnalysis pCpa, ShutdownNotifier pShutdownNotifier,
      String programDenotation, @Nullable final Specification stats,
      ReachedSetFactory reachedSetFactory, ShutdownManager shutdownManager)
      throws InvalidConfigurationException {
    cfa = pCfa;
    cpa = pCpa;
    startupConfig = new StartupConfig(pConfig, pLogger, pShutdownNotifier);
    startupConfig.getConfig().inject(this);
    logger = pLogger;
    assert TigerAlgorithm.originalMainFunction != null;
    mCoverageSpecificationTranslator =
        new CoverageSpecificationTranslator(
            pCfa.getFunctionHead(TigerAlgorithm.originalMainFunction));
    wrapper = new Wrapper(pCfa, TigerAlgorithm.originalMainFunction);
    mAlphaLabel = new GuardedEdgeLabel(new SingletonECPEdgeSet(wrapper.getAlphaEdge()));
    mInverseAlphaLabel = new InverseGuardedEdgeLabel(mAlphaLabel);
    mOmegaLabel = new GuardedEdgeLabel(new SingletonECPEdgeSet(wrapper.getOmegaEdge()));
    config = pConfig;
    config.inject(this);
    logger.logf(Level.INFO, "FQL query string: %s", fqlQuery);
    fqlSpecification = FQLSpecificationUtil.getFQLSpecification(fqlQuery);
    logger.logf(Level.INFO, "FQL query: %s", fqlSpecification.toString());
    this.programDenotation = programDenotation;
    this.stats = stats;
    testsuite = new TestSuite(null, true, false);

    this.reachedSetFactory = reachedSetFactory;

    mainShutdownManager = shutdownManager;

    testGoalUtils = new TestGoalUtils(logger, mAlphaLabel,
        mInverseAlphaLabel, mOmegaLabel, optimizeGoalAutomata);

    targetStateFormulas = new HashMap<>();

    edgeToTgaMapping = new HashMap<>();
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    logger.logf(Level.INFO,
        "We will not use the provided reached set since it violates the internal structure of Tiger's CPAs");
    logger.logf(Level.INFO, "We empty pReachedSet to stop complaints of an incomplete analysis");

    outsideReachedSet = pReachedSet;
    outsideReachedSet.clear();

    Set<Goal> goalsToCover =
        testGoalUtils.extractTestGoalPatterns(fqlSpecification, mCoverageSpecificationTranslator);
    fillEdgeToTgaMapping(goalsToCover);

    statistics_numberOfTestGoals = goalsToCover.size();
    logger.logf(Level.INFO, "Number of test goals: %d", statistics_numberOfTestGoals);

    // (iii) do test generation for test goals ...
    boolean wasSound = true;
    try {
      if (!testGeneration(goalsToCover)) {
        logger.logf(Level.WARNING, "Test generation contained unsound reachability analysis runs!");
        wasSound = false;
      }
    } catch (InvalidConfigurationException e1) {
      throw new CPAException("Invalid configuration!", e1);
    }

    // TODO: change testGeneration() such that it returns timedout if there was a timeout
    //    assert (!testsuite.getTimedOutGoals().isEmpty() ? goalsToCover.isEmpty() : true);

    // Write generated test suite and mapping to file system
    dumpTestSuite();

    if (wasSound) {
      return AlgorithmStatus.SOUND_AND_PRECISE;
    } else {
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
    }
    //-------------------------------------------------------------------------------------------------------------------
    /*  LinkedList<ElementaryCoveragePattern> goalPatterns;
    LinkedList<Pair<ElementaryCoveragePattern, Region>> pTestGoalPatterns = new LinkedList<>();
    logger.logf(Level.INFO,
        "We will not use the provided reached set since it violates the internal structure of Tiger's CPAs");
    logger.logf(Level.INFO, "We empty pReachedSet to stop complaints of an incomplete analysis");
    outsideReachedSet = pReachedSet;
    outsideReachedSet.clear();

    goalPatterns = extractTestGoalPatterns(fqlSpecification);

    for (int i = 0; i < goalPatterns.size(); i++) {
      pTestGoalPatterns.add(Pair.of(goalPatterns.get(i), (Region) null));
    }

    int goalIndex = 1;
    LinkedList<Goal> pGoalsToCover = new LinkedList<>();
    for (Pair<ElementaryCoveragePattern, Region> pair : pTestGoalPatterns) {
      Goal lGoal =
          constructGoal(goalIndex, pair.getFirst(), mAlphaLabel, mInverseAlphaLabel, mOmegaLabel,
              optimizeGoalAutomata,
              pair.getSecond());
      logger.log(Level.INFO, lGoal.getName());
      pGoalsToCover.add(lGoal);
      goalIndex++;
    }

    for (Goal goal : pGoalsToCover) {
      try {
        runReachabilityAnalysis(goal, goal.getIndex());
      } catch (InvalidConfigurationException e) {
        logger.log(Level.SEVERE, "Failed to run reachability analysis!");
      }
    }

    try (Writer writer =
        new BufferedWriter(new OutputStreamWriter(
            new FileOutputStream(
                "/home/gregor/Eclipse_Workspaces/CPAIntegrationTiger_v3/CPAchecker/output/testsuite.txt"),
            "utf-8"))) {
      writer.write(testsuite.toString());
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return AlgorithmStatus.SOUND_AND_PRECISE;*/
  }

  private void fillEdgeToTgaMapping(Set<Goal> pGoalsToCover) {
    for (Goal goal : pGoalsToCover) {
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> automaton = goal.getAutomaton();
      for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge edge : automaton.getEdges()) {
        if (edge.getSource().equals(edge.getTarget())) {
          continue;
        }

        GuardedEdgeLabel label = edge.getLabel();

        for (CFAEdge e : label.getEdgeSet()) {
          List<NondeterministicFiniteAutomaton<GuardedEdgeLabel>> tgaSet = edgeToTgaMapping.get(e);

          if (tgaSet == null) {
            tgaSet = new ArrayList<>();
            edgeToTgaMapping.put(e, tgaSet);
          }

          tgaSet.add(automaton);
        }
      }
    }

  }

  private void dumpTestSuite() {
    if (testsuiteFile != null) {
      //logger.log(Level.INFO, testsuiteFile.toAbsolutePath().toString());
      try (Writer writer =
          new BufferedWriter(new OutputStreamWriter(
              new FileOutputStream(
                  "/home/gregor/Eclipse_Workspaces/CPAIntegrationTiger_newTry/CPAchecker/output/testsuite.txt"/*testsuiteFile.toAbsolutePath().toString()*/),
              "utf-8"))) {
        writer.write(testsuite.toString());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private boolean testGeneration(Set<Goal> pGoalsToCover)
      throws CPAException, InterruptedException, InvalidConfigurationException {

    boolean wasSound = true;
    int numberOfTestGoals = pGoalsToCover.size();
    testsuite.addGoals(pGoalsToCover);

    NondeterministicFiniteAutomaton<GuardedEdgeLabel> previousAutomaton = null;
    boolean retry = false;

    do {
      if (retry) {
        // retry timed-out goals
        boolean order = true;

        if (timeoutIncrement > 0) {
          long oldCPUTimeLimitPerGoal = cpuTimelimitPerGoal;
          cpuTimelimitPerGoal += timeoutIncrement;
          logger.logf(Level.INFO, "Incremented timeout from %d to %d seconds.",
              oldCPUTimeLimitPerGoal,
              cpuTimelimitPerGoal);

          Collection<Entry<Integer, Pair<Goal, Region>>> set;
          if (useOrder) {
            if (inverseOrder) {
              order = !order;
            }

            // keep original order of goals (or inverse of it)
            if (order) {
              set = new TreeSet<>(WorklistEntryComparator.ORDER_RESPECTING_COMPARATOR);
            } else {
              set = new TreeSet<>(WorklistEntryComparator.ORDER_INVERTING_COMPARATOR);
            }

            set.addAll(testsuite
                .getTimedOutGoals().entrySet());
          } else {
            set = new LinkedList<>();
            set.addAll(testsuite
                .getTimedOutGoals().entrySet());
          }

          pGoalsToCover.clear();
          for (Entry<Integer, Pair<Goal, Region>> entry : set) {
            pGoalsToCover.add(entry.getValue().getFirst());
          }
          statistics_numberOfProcessedTestGoals -= testsuite.getTimedOutGoals().size();
          testsuite.getTimedOutGoals().clear();
        }
      }

      while (!pGoalsToCover.isEmpty()) {
        Set<Goal> goalsToBeProcessed = nextTestGoalSet(pGoalsToCover);
        statistics_numberOfProcessedTestGoals += goalsToBeProcessed.size();
        pGoalsToCover.removeAll(goalsToBeProcessed);

        String logString = "Processing test goals ";
        for (Goal g : goalsToBeProcessed) {
          logString += g.getIndex() + " (" + testsuite.getTestGoalLabel(g) + "), ";
        }
        logString = logString.substring(0, logString.length() - 2);

        // goal is uncovered so far; run CPAchecker to cover it
        ReachabilityAnalysisResult result =
            runReachabilityAnalysis(pGoalsToCover, goalsToBeProcessed, previousAutomaton);
        if (result.equals(ReachabilityAnalysisResult.UNSOUND)) {
          logger.logf(Level.WARNING, "Analysis run was unsound!");
          wasSound = false;
        }
        //      previousAutomaton = currentAutomaton;

        if (result.equals(ReachabilityAnalysisResult.TIMEDOUT)) {
          break;
        }
      }
      // reprocess timed-out goals
      if (testsuite.getTimedOutGoals().isEmpty()) {
        logger.logf(Level.INFO, "There were no timed out goals.");
        retry = false;
      } else {
        if (!timeoutStrategy.equals(TimeoutStrategy.RETRY_AFTER_TIMEOUT)) {
          logger.logf(Level.INFO,
              "There were timed out goals but retry after timeout strategy is disabled.");
        } else {
          retry = true;
        }
      }

    } while (retry);
    return wasSound;
  }

  private ReachabilityAnalysisResult runReachabilityAnalysis(
      Set<Goal> pUncoveredGoals,
      Set<Goal> pTestGoalsToBeProcessed,
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousGoalAutomaton)
      throws InterruptedException {
    ReachabilityAnalysisResult algorithmStatus = null;
    try {
      ARGCPA cpa = composeCPA(pTestGoalsToBeProcessed);
      Preconditions.checkState(cpa.getWrappedCPAs().get(0) instanceof CompositeCPA,
          "CPAcheckers automata should be used! The assumption is that the first component is the automata for the current goal!");
      initializeReachedSet(cpa);
      ShutdownManager shutdownManager =
          ShutdownManager.createWithParent(startupConfig.getShutdownNotifier());
      Region presenceConditionToCover = null;
      Algorithm algorithm = initializeAlgorithm(presenceConditionToCover, cpa, shutdownManager);

      algorithmStatus =
          runAlgorithm(pUncoveredGoals, pTestGoalsToBeProcessed, cpa,
              presenceConditionToCover, shutdownManager, algorithm);
    } catch (InvalidConfigurationException | CPAException e) {
    }
    return algorithmStatus;
  }

  private ReachabilityAnalysisResult runAlgorithm(Set<Goal> pUncoveredGoals,
      Set<Goal> pTestGoalsToBeProcessed, ARGCPA pARTCPA, Region pPresenceConditionToCover,
      ShutdownManager pShutdownNotifier, Algorithm pAlgorithm)
      throws CPAEnabledAnalysisPropertyViolationException, CPAException, InterruptedException {
    ReachabilityAnalysisResult algorithmStatus;

    do {
      // The wrapped algorithm (pAlgorithm) is (typically)
      // either the CEGAR algorithm,
      // or another algorithm that wraps the CEGAR algorithm.
      Preconditions.checkState(pAlgorithm instanceof CEGARAlgorithm
          || pAlgorithm instanceof CounterexampleCheckAlgorithm);

      algorithmStatus = runAlgorithmWithLimit(pShutdownNotifier, pAlgorithm);

      // Cases where runAlgorithm terminates:
      //  A) TIMEOUT
      //  B) Feasible counterexample
      //  C) No feasible counterexample (fixpoint)

      if (algorithmStatus != ReachabilityAnalysisResult.TIMEDOUT) {

        if (reachedSet.hasWaitingState() && reachedSet.getLastState() != null) {
          Preconditions.checkState(reachedSet.getLastState() instanceof ARGState);
          ARGState lastState = (ARGState) reachedSet.getLastState();
          Preconditions.checkState(lastState.isTarget());

          CounterexampleInfo cexi = pARTCPA.getCounterexamples().get(lastState);

          if (cexi == null) {
            // No feasible counterexample!
            logger.logf(Level.WARNING,
                "Analysis returned a target state (%d) without a feasible counterexample for: "
                    + lastState.getViolatedProperties(),
                lastState.getStateId());
          } else {
            dumpArgForCex(cexi);
            Set<Goal> coveredGoals = null;
            if (allCoveredGoalsPerTestCase) {
              coveredGoals = addTestToSuite(testsuite.getGoals(), cexi);
            } else if (checkCoverage) {
              coveredGoals =
                  addTestToSuite(Sets.union(pUncoveredGoals, pTestGoalsToBeProcessed), cexi);
            } else {
              coveredGoals =
                  addTestToSuite(pTestGoalsToBeProcessed, cexi);
            }
            pUncoveredGoals.removeAll(coveredGoals);
          }

        }

      }

    } while ((reachedSet.hasWaitingState()
        && !testsuite.areGoalsCoveredOrInfeasible(pTestGoalsToBeProcessed))
        && (algorithmStatus != ReachabilityAnalysisResult.TIMEDOUT));

    if (algorithmStatus == ReachabilityAnalysisResult.TIMEDOUT) {
      logger.logf(Level.INFO, "Test goal timed out!");
      testsuite.addTimedOutGoals(pTestGoalsToBeProcessed);
    } else {
      // set test goals infeasible
      for (Goal goal : pTestGoalsToBeProcessed) {
        if (!testsuite.isGoalCovered(goal)) {
          handleInfeasibleTestGoal(goal);
        }
      }
    }
    return algorithmStatus;
  }

  private void handleInfeasibleTestGoal(Goal pGoal) {
    if (lGoalPrediction != null) {
      lGoalPrediction[pGoal.getIndex() - 1] = Prediction.INFEASIBLE;
    }
    logger.logf(Level.WARNING, "Goal %d is infeasible!", pGoal.getIndex());
    testsuite.addInfeasibleGoal(pGoal, null, lGoalPrediction);
  }

  private Set<Goal> addTestToSuite(Set<Goal> pRemainingGoals, CounterexampleInfo pCex) {
    Preconditions.checkNotNull(pRemainingGoals);
    Preconditions.checkNotNull(pCex);

    ARGState lastState = pCex.getTargetPath().getLastState();

    // TODO check whether a last state might remain from an earlier run and a reuse of the ARG

    Region testCasePresenceCondition = null;

    TestCase testcase = createTestcase(pCex, testCasePresenceCondition);
    Set<Goal> coveredGoals = updateTestsuiteByCoverageOf(testcase, pRemainingGoals);

    //        if (lGoalPrediction != null) {
    //          lGoalPrediction[pGoal.getIndex() - 1] = Prediction.FEASIBLE;
    //        }
    return coveredGoals;
  }

  private Set<Goal> updateTestsuiteByCoverageOf(TestCase pTestcase, Set<Goal> pRemainingGoals) {

    Set<Goal> checkCoverageOf = new HashSet<>();
    checkCoverageOf.addAll(pRemainingGoals);

    Set<Goal> coveredGoals = Sets.newLinkedHashSet();
    Set<Goal> goalsCoveredByLastState = Sets.newLinkedHashSet();

    ARGState lastState = pTestcase.getArgPath().getLastState();

    if (printPathFormulasPerGoal) {

      List<BooleanFormula> formulas = getPathFormula(pTestcase.getArgPath());

      Set<Property> violatedProperties = lastState.getViolatedProperties();

      for (Property property : violatedProperties) {
        Preconditions.checkState(property instanceof Goal);
        Goal g = (Goal) property;
        List<List<BooleanFormula>> f = targetStateFormulas.get(g);
        if (f == null) {
          f = new ArrayList<>();
          targetStateFormulas.put(g, f);
        }

        f.add(formulas);
      }


      return new HashSet<>();
    }

    for (Property p : lastState.getViolatedProperties()) {
      Preconditions.checkState(p instanceof Goal);
      goalsCoveredByLastState.add((Goal) p);
    }

    checkCoverageOf.removeAll(goalsCoveredByLastState);

    if (!allCoveredGoalsPerTestCase) {
      for (Goal goal : pRemainingGoals) {
        if (testsuite.isGoalCovered(goal)) {
          checkCoverageOf.remove(goal);
        }
      }
    }

    Map<NondeterministicFiniteAutomaton<GuardedEdgeLabel>, AcceptStatus> acceptStati =
        accepts(checkCoverageOf, pTestcase.getErrorPath());

    for (Goal goal : goalsCoveredByLastState) {
      AcceptStatus acceptStatus = new AcceptStatus(goal);
      acceptStatus.answer = ThreeValuedAnswer.ACCEPT;
      acceptStati.put(goal.getAutomaton(), acceptStatus);
    }

    for (NondeterministicFiniteAutomaton<GuardedEdgeLabel> automaton : acceptStati.keySet()) {
      AcceptStatus acceptStatus = acceptStati.get(automaton);
      Goal goal = acceptStatus.goal;

      if (acceptStatus.answer.equals(ThreeValuedAnswer.UNKNOWN)) {
        logger.logf(Level.WARNING,
            "Coverage check for goal %d could not be performed in a precise way!",
            goal.getIndex());
        continue;
      } else if (acceptStatus.answer.equals(ThreeValuedAnswer.REJECT)) {
        continue;
      }

      testsuite.addTestCase(pTestcase, goal, null);
      logger.logf(Level.WARNING, "Covered Goal %d (%s) by test case %d!",
          goal.getIndex(),
          testsuite.getTestGoalLabel(goal),
          pTestcase.getId());
      coveredGoals.add(goal);
    }
    return coveredGoals;
  }

  private Map<NondeterministicFiniteAutomaton<GuardedEdgeLabel>, AcceptStatus> accepts(
      Collection<Goal> pGoals, List<CFAEdge> pErrorPath) {
    Map<NondeterministicFiniteAutomaton<GuardedEdgeLabel>, AcceptStatus> map = new HashMap<>();
    Set<NondeterministicFiniteAutomaton.State> lNextStates = Sets.newLinkedHashSet();

    Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>> automataWithResult = new HashSet<>();

    for (Goal goal : pGoals) {
      AcceptStatus acceptStatus = new AcceptStatus(goal);
      map.put(goal.getAutomaton(), acceptStatus);
      if (acceptStatus.automaton.getFinalStates()
          .contains(acceptStatus.automaton.getInitialState())) {
        acceptStatus.answer = ThreeValuedAnswer.ACCEPT;
        automataWithResult.add(acceptStatus.automaton);
      }
    }

    for (CFAEdge lCFAEdge : pErrorPath) {
      List<NondeterministicFiniteAutomaton<GuardedEdgeLabel>> automata =
          edgeToTgaMapping.get(lCFAEdge);
      if (automata == null) {
        continue;
      }

      for (NondeterministicFiniteAutomaton<GuardedEdgeLabel> automaton : automata) {
        if (automataWithResult.contains(automaton)) {
          continue;
        }

        AcceptStatus acceptStatus = map.get(automaton);
        if (acceptStatus == null) {
          continue;
        }
        for (NondeterministicFiniteAutomaton.State lCurrentState : acceptStatus.currentStates) {
          for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOutgoingEdge : automaton
              .getOutgoingEdges(lCurrentState)) {
            GuardedEdgeLabel lLabel = lOutgoingEdge.getLabel();

            if (lLabel.hasGuards()) {
              acceptStatus.hasPredicates = true;
            } else {
              if (lLabel.contains(lCFAEdge)) {
                lNextStates.add(lOutgoingEdge.getTarget());
                lNextStates
                    .addAll(getSuccsessorsOfEmptyTransitions(automaton, lOutgoingEdge.getTarget()));

                for (State nextState : lNextStates) {
                  // Automaton accepts as soon as it sees a final state (implicit self-loop)
                  if (automaton.getFinalStates().contains(nextState)) {
                    acceptStatus.answer = ThreeValuedAnswer.ACCEPT;
                    automataWithResult.add(automaton);
                  }
                }
              }
            }
          }
        }

        acceptStatus.currentStates.addAll(lNextStates);
        lNextStates.clear();
      }
    }

    for (NondeterministicFiniteAutomaton<GuardedEdgeLabel> autom : map.keySet()) {
      if (automataWithResult.contains(autom)) {
        continue;
      }

      AcceptStatus accepts = map.get(autom);
      if (accepts.hasPredicates) {
        accepts.answer = ThreeValuedAnswer.UNKNOWN;
      } else {
        accepts.answer = ThreeValuedAnswer.REJECT;
      }
    }

    return map;

  }

  private Collection<? extends State> getSuccsessorsOfEmptyTransitions(
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton, State pTarget) {
    Set<State> states = new HashSet<>();
    for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge edge : pAutomaton
        .getOutgoingEdges(pTarget)) {
      GuardedEdgeLabel label = edge.getLabel();
      if (Pattern.matches("E\\d+ \\[\\]", label.toString())) {
        states.add(edge.getTarget());
      }
    }
    return states;
  }

  private class AcceptStatus {

    private Goal goal;
    private NondeterministicFiniteAutomaton<GuardedEdgeLabel> automaton;
    private Set<NondeterministicFiniteAutomaton.State> currentStates;
    boolean hasPredicates;
    private ThreeValuedAnswer answer;

    public AcceptStatus(Goal pGoal) {
      goal = pGoal;
      automaton = pGoal.getAutomaton();
      currentStates = Sets.newLinkedHashSet();
      hasPredicates = false;

      currentStates.add(automaton.getInitialState());
    }

    @Override
    public String toString() {
      return goal.getName() + ": " + answer;
    }

  }

  private List<BooleanFormula> getPathFormula(ARGPath pArgPath) {
    List<BooleanFormula> formulas = null;

    Refiner refiner = this.refiner;

    /* if (refiner instanceof PredicateCPARefiner) {
      final List<ARGState> abstractionStatesTrace = PredicateCPARefiner.transformPath(pArgPath);
      formulas = ((PredicateCPARefiner) refiner).createFormulasOnPath(pArgPath, abstractionStatesTrace);
    }*/

    return formulas;
  }

  private TestCase createTestcase(CounterexampleInfo pCex, Region pTestCasePresenceCondition) {

    final List<BigInteger> inputValues = new LinkedList<>();
    //    final Pair<TreeSet<Entry<AssignableTerm, Object>>, TreeSet<Entry<AssignableTerm, Object>>> inputsAndOutputs = calculateInputAndOutputValues(model);
    final Pair<TreeSet<Entry<AssignableTerm, Object>>, TreeSet<Entry<AssignableTerm, Object>>> inputsAndOutputs =
        null;
    //    final List<TestStep> testSteps = calculateTestSteps(model, pCex);
    final List<TestStep> testSteps = null;

    TestCase testcase = new TestCase(testCaseId++,
        testSteps,
        pCex.getTargetPath(),
        pCex.getTargetPath().getInnerEdges(),
        null,
        null,
        inputValues,
        inputsAndOutputs);


    logger.logf(Level.INFO, "Generated new test case %d.", testcase.getId());


    return testcase;
  }

  private void dumpArgForCex(CounterexampleInfo pCexi) {
    //    Path argFile = Paths.get("output", "ARG_goal_" + Integer.toString(partitionId)  + ".dot");
    //  try (Writer w = Files.openOutputFile(argFile)) {
    //    final Set<Pair<ARGState, ARGState>> allTargetPathEdges = new HashSet<>();
    //    allTargetPathEdges.addAll(cexi.getTargetPath().getStatePairs());
    //
    //    ARGToDotWriter.write(w, AbstractStates.extractStateByType(reachedSet.getFirstState(), ARGState.class),
    //        ARGUtils.CHILDREN_OF_STATE,
    //        Predicates.alwaysTrue(),
    //        Predicates.in(allTargetPathEdges));
    //  } catch (IOException e) {
    //    logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
    //  }

  }

  private ReachabilityAnalysisResult runAlgorithmWithLimit(ShutdownManager algNotifier,
      Algorithm algorithm)
      throws CPAEnabledAnalysisPropertyViolationException, CPAException, InterruptedException {
    ReachabilityAnalysisResult algorithmStatus;
    if (cpuTimelimitPerGoal < 0) {
      // run algorithm without time limit
      if (algorithm.run(reachedSet).isSound()) {
        algorithmStatus = ReachabilityAnalysisResult.SOUND;
      } else {
        algorithmStatus = ReachabilityAnalysisResult.UNSOUND;
      }
    } else {
      // run algorithm with time limit
      WorkerRunnable workerRunnable =
          new WorkerRunnable(algorithm, reachedSet, cpuTimelimitPerGoal, algNotifier);

      Thread workerThread = new Thread(workerRunnable);

      workerThread.start();
      workerThread.join();

      if (workerRunnable.throwableWasCaught()) {
        // TODO: handle exception
        algorithmStatus = ReachabilityAnalysisResult.UNSOUND;
        //        throw new RuntimeException(workerRunnable.getCaughtThrowable());
      } else {
        if (workerRunnable.analysisWasSound()) {
          algorithmStatus = ReachabilityAnalysisResult.SOUND;
        } else {
          algorithmStatus = ReachabilityAnalysisResult.UNSOUND;
        }

        if (workerRunnable.hasTimeout()) {
          algorithmStatus = ReachabilityAnalysisResult.TIMEDOUT;
        }
      }
    }
    return algorithmStatus;
  }

  private Algorithm initializeAlgorithm(Region pPresenceConditionToCover, ARGCPA lARTCPA,
      ShutdownManager pShutdownManager) {
    Algorithm algorithm;
    try {
      Configuration internalConfiguration =
          Configuration.builder().loadFromFile(algorithmConfigurationFile).build();
      Set<UnmodifiableReachedSet> unmodifiableReachedSets = new HashSet<>();

      unmodifiableReachedSets.add(reachedSet);

      AggregatedReachedSets aggregatedReachedSets =
          new AggregatedReachedSets(unmodifiableReachedSets);

      CoreComponentsFactory coreFactory = new CoreComponentsFactory(internalConfiguration, logger,
          pShutdownManager.getNotifier(), aggregatedReachedSets);

      algorithm = coreFactory.createAlgorithm(lARTCPA, programDenotation, cfa, stats);

      if (algorithm instanceof CEGARAlgorithm) {
        CEGARAlgorithm cegarAlg = (CEGARAlgorithm) algorithm;
        this.refiner = cegarAlg.getmRefiner();


        ARGStatistics lARTStatistics;
        try {
          lARTStatistics = new ARGStatistics(internalConfiguration, logger, lARTCPA,
              stats, cfa);
        } catch (InvalidConfigurationException e) {
          throw new RuntimeException(e);
        }
        Set<Statistics> lStatistics = Sets.newLinkedHashSet();
        lStatistics.add(lARTStatistics);
        cegarAlg.collectStatistics(lStatistics);
      }
    } catch (IOException | InvalidConfigurationException | CPAException e) {
      throw new RuntimeException(e);
    }
    return algorithm;
  }

  private void initializeReachedSet(ARGCPA pArgCPA) throws InterruptedException {
    if (reachedSet != null) {
      reachedSet.clear();
    }
    reachedSet = reachedSetFactory.create();

    AbstractState initialState =
        pArgCPA.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
    Precision initialPrec = pArgCPA.getInitialPrecision(cfa.getMainFunction(),
        StateSpacePartition.getDefaultPartition());

    reachedSet.add(initialState, initialPrec);
    outsideReachedSet.add(initialState, initialPrec);

    if (reusePredicates) {
      // initialize reused predicate precision
      PredicateCPA predicateCPA = pArgCPA.retrieveWrappedCpa(PredicateCPA.class);

      if (predicateCPA != null) {
        reusedPrecision =
            (PredicatePrecision) predicateCPA.getInitialPrecision(cfa.getMainFunction(),
                StateSpacePartition.getDefaultPartition());
      } else {
        logger.logf(Level.INFO, "No predicate CPA available to reuse predicates!");
      }
    }

  }

  private ARGCPA composeCPA(Set<Goal> pTestGoalsToBeProcessed)
      throws InvalidConfigurationException, CPAException {
    Preconditions.checkArgument(cpa instanceof ARGCPA,
        "Tiger: Only support for ARGCPA implemented for CPA composition!");
    ARGCPA oldArgCPA = (ARGCPA) cpa;
    Specification goalAutomatonSpecification = null;
    List<Automaton> componentAutomata = Lists.newArrayList();
    {
      List<Automaton> goalAutomata = Lists.newArrayList();

      for (Goal goal : pTestGoalsToBeProcessed) {
        final Automaton a = goal.createControlAutomaton();
        goalAutomata.add(a);
        dumpAutomaton(a);
      }
      goalAutomatonSpecification =
          Specification.fromAutomata(Lists.newArrayList(goalAutomata));
      componentAutomata.addAll(goalAutomata);
    }
    logger.logf(Level.INFO, "Analyzing %d test goals with %d observer automata.",
        pTestGoalsToBeProcessed.size(),
        componentAutomata.size());

    Collection<ConfigurableProgramAnalysis> automataCPAs = Lists.newArrayList();

    for (Automaton componentAutomaton : componentAutomata) {

      final CPAFactory automataFactory = ControlAutomatonCPA.factory();

      automataFactory
          .setConfiguration(Configuration.copyWithNewPrefix(config, componentAutomaton.getName()));
      automataFactory.setLogger(logger.withComponentName(componentAutomaton.getName()));
      automataFactory.set(cfa, CFA.class);
      automataFactory.set(componentAutomaton, Automaton.class);

      automataCPAs.add(automataFactory.createInstance());
    }

    // Add one automata CPA for each goal
    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<>();
    lComponentAnalyses.addAll(automataCPAs);

    // Add the old composite components
    Preconditions.checkState(oldArgCPA.getWrappedCPAs().iterator().next() instanceof CompositeCPA);
    CompositeCPA argCompositeCpa = (CompositeCPA) oldArgCPA.getWrappedCPAs().iterator().next();
    lComponentAnalyses.addAll(argCompositeCpa.getWrappedCPAs());

    final ARGCPA result;

    try {
      // create composite CPA
      CPAFactory compositeCpaFactory = CompositeCPA.factory();
      compositeCpaFactory.setChildren(lComponentAnalyses);
      compositeCpaFactory.setConfiguration(startupConfig.getConfig());
      compositeCpaFactory.setLogger(logger);
      compositeCpaFactory.set(cfa, CFA.class);

      ConfigurableProgramAnalysis lCPA = compositeCpaFactory.createInstance();

      // create ARG CPA
      CPAFactory lARTCPAFactory = ARGCPA.factory();
      lARTCPAFactory.set(cfa, CFA.class);
      lARTCPAFactory.setChild(lCPA);
      lARTCPAFactory.setConfiguration(startupConfig.getConfig());
      lARTCPAFactory.setLogger(logger);
      lARTCPAFactory.set(goalAutomatonSpecification, Specification.class);

      result = (ARGCPA) lARTCPAFactory.createInstance();

    } catch (InvalidConfigurationException | CPAException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  private void dumpAutomaton(Automaton pA) {
    if (dumpGoalAutomataTo == null) { return; }

    Path argFile = dumpGoalAutomataTo.getPath(pA.getName());
    try (FileWriter w = new FileWriter(argFile.toString())) {

      pA.writeDotFile(w);

    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
    }
  }

  private ImmutableSet<Goal> nextTestGoalSet(Set<Goal> pGoalsToCover) {
    final int testGoalSetSize = (numberOfTestGoalsPerRun <= 0)
        ? pGoalsToCover.size()
        : (pGoalsToCover.size() > numberOfTestGoalsPerRun) ? numberOfTestGoalsPerRun
            : pGoalsToCover.size();

    Builder<Goal> builder = ImmutableSet.<Goal> builder();

    Iterator<Goal> it = pGoalsToCover.iterator();
    for (int i = 0; i < testGoalSetSize; i++) {
      if (it.hasNext()) {
        builder.add(it.next());
      }
    }

    ImmutableSet<Goal> result = builder.build();

    return result;
  }

  /*private ReachabilityAnalysisResult runReachabilityAnalysis(Goal pGoal, int goalIndex)
      throws CPAException, InterruptedException, InvalidConfigurationException {

    //build CPAs for the goal
    ARGCPA lARTCPA = buildCPAs(pGoal);

    reachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.BFS); // TODO why does TOPSORT not exist anymore?
    AbstractState lInitialElement =
        lARTCPA.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
    Precision lInitialPrecision = lARTCPA.getInitialPrecision(cfa.getMainFunction(),
        StateSpacePartition.getDefaultPartition());

    reachedSet.add(lInitialElement, lInitialPrecision);

    outsideReachedSet.add(lInitialElement, lInitialPrecision);

    ShutdownManager algNotifier =
        ShutdownManager.createWithParent(startupConfig.getShutdownNotifier());

    startupConfig.getConfig();

    //run analysis
    Pair<Boolean, Boolean> analysisWasSound_hasTimedOut =
        buildAndRunAlgorithm(algNotifier, lARTCPA);

    //write ARG to file
    Path argFile = Paths.get("output", "ARG_goal_" + goalIndex + ".dot");
    try (FileWriter w = new FileWriter(argFile.toString())) {
      ARGUtils.writeARGAsDot(w, (ARGState) reachedSet.getFirstState());
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
    }

    if (analysisWasSound_hasTimedOut.getSecond()) {
      return ReachabilityAnalysisResult.TIMEDOUT;
    } else {

      AbstractState lastState = reachedSet.getLastState();

      if (lastState != null) {

        if (AbstractStates.isTargetState(lastState)) {

          logger.logf(Level.INFO, "Test goal is feasible.");

          CFAEdge criticalEdge = pGoal.getCriticalEdge();

          //For testing
          Optional<CounterexampleInfo> cexi = ((ARGState) lastState).getCounterexampleInformation();
          if (cexi.isPresent()) {}
          //...........

          Map<ARGState, CounterexampleInfo> counterexamples = lARTCPA.getCounterexamples();

          if (counterexamples.isEmpty()) {

            TestCase testcase = handleUnavailableCounterexample(criticalEdge, lastState);

            testsuite.addTestCase(testcase, pGoal);
          } else {
            // test goal is feasible
            logger.logf(Level.INFO, "Counterexample is available.");
            assert counterexamples.size() == 1;

            for (Map.Entry<ARGState, CounterexampleInfo> lEntry : counterexamples.entrySet()) {

              CounterexampleInfo cex = lEntry.getValue();

              if (cex.isSpurious()) {
                logger.logf(Level.WARNING, "Counterexample is spurious!");
              } else {
                List<BigInteger> inputValues = new ArrayList<>(0);
                // calcualte shrinked error path
                List<CFAEdge> shrinkedErrorPath =
                    new ErrorPathShrinker().shrinkErrorPath(cex.getTargetPath());
                TestCase testcase =
                    new TestCase(inputValues, cex.getTargetPath().asEdgesList(), shrinkedErrorPath,
                        null, null);
                testsuite.addTestCase(testcase, pGoal);

              }
            }
          }
        } else {
          logger.logf(Level.INFO, "Test goal infeasible.");
          testsuite.addInfeasibleGoal(pGoal, null);
        }
      } else {
        throw new RuntimeException(
            "We need a last state to determine the feasibility of the test goal!");
      }
    }

    return ReachabilityAnalysisResult.SOUND;
  }*/

  private ARGCPA buildCPAs(Goal pGoal) throws CPAException {
    Automaton goalAutomaton = pGoal.createControlAutomaton();
    Specification goalAutomatonSpecification =
        Specification.fromAutomata(Lists.newArrayList(goalAutomaton));

    CPAFactory automataFactory = ControlAutomatonCPA.factory();
    automataFactory
        .setConfiguration(Configuration.copyWithNewPrefix(config, goalAutomaton.getName()));
    automataFactory.setLogger(logger.withComponentName(goalAutomaton.getName()));
    automataFactory.set(cfa, CFA.class);
    automataFactory.set(goalAutomaton, Automaton.class);

    List<ConfigurableProgramAnalysis> lAutomatonCPAs = new ArrayList<>(1);//(2);
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

    ARGCPA lARTCPA;
    try {
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

  private Pair<Boolean, Boolean> buildAndRunAlgorithm(ShutdownManager algNotifier, ARGCPA lARTCPA)
      throws CPAException, InterruptedException {

    Algorithm algorithm;

    boolean analysisWasSound = false;
    boolean hasTimedOut = false;

    try {
      Configuration internalConfiguration =
          Configuration.builder().loadFromFile(algorithmConfigurationFile).build();

      Set<UnmodifiableReachedSet> unmodifiableReachedSets = new HashSet<>();

      unmodifiableReachedSets.add(reachedSet);

      AggregatedReachedSets aggregatedReachedSets =
          new AggregatedReachedSets(unmodifiableReachedSets);

      CoreComponentsFactory coreFactory = new CoreComponentsFactory(internalConfiguration, logger,
          algNotifier.getNotifier(), aggregatedReachedSets);

      algorithm = coreFactory.createAlgorithm(lARTCPA, programDenotation, cfa, stats);

      if (algorithm instanceof CEGARAlgorithm) {
        CEGARAlgorithm cegarAlg = (CEGARAlgorithm) algorithm;

        ARGStatistics lARTStatistics;
        try {
          lARTStatistics = new ARGStatistics(internalConfiguration, logger, lARTCPA,
              stats, cfa);
        } catch (InvalidConfigurationException e) {
          throw new RuntimeException(e);
        }
        Set<Statistics> lStatistics = new HashSet<>();
        lStatistics.add(lARTStatistics);
        cegarAlg.collectStatistics(lStatistics);
      }
    } catch (IOException | InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }

    if (cpuTimelimitPerGoal < 0) {
      // run algorithm without time limit
      analysisWasSound = algorithm.run(reachedSet).isSound();
    } else {
      // run algorithm with time limit
      WorkerRunnable workerRunnable =
          new WorkerRunnable(algorithm, reachedSet, cpuTimelimitPerGoal, algNotifier);

      Thread workerThread = new Thread(workerRunnable);

      workerThread.start();
      workerThread.join();

      if (workerRunnable.throwableWasCaught()) {
        // TODO: handle exception
        analysisWasSound = false;
        //        throw new RuntimeException(workerRunnable.getCaughtThrowable());
      } else {
        analysisWasSound = workerRunnable.analysisWasSound();

        if (workerRunnable.hasTimeout()) {
          logger.logf(Level.INFO, "Test goal timed out!");
          hasTimedOut = true;
        }
      }
    }
    return Pair.of(analysisWasSound, hasTimedOut);
  }

  private TestCase handleUnavailableCounterexample(CFAEdge criticalEdge, AbstractState lastState) {

    logger.logf(Level.INFO, "Counterexample is not available.");

    LinkedList<CFAEdge> trace = new LinkedList<>();

    // Try to reconstruct a trace in the ARG and shrink it
    ARGState argState = AbstractStates.extractStateByType(lastState, ARGState.class);
    ARGPath path = ARGUtils.getOnePathTo(argState);
    List<CFAEdge> shrinkedErrorPath = null;

    if (path != null) {
      shrinkedErrorPath = new ErrorPathShrinker().shrinkErrorPath(path);
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
        logger.logf(Level.INFO,
            "*********************** extract abstract state ***********************");
      }

      argState = parent;
      parents = argState.getParents();
    }

    List<BigInteger> inputValues = new ArrayList<>();

    return new TestCase(inputValues, trace, shrinkedErrorPath,
        null,
        null);
  }
}
