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
package org.sosy_lab.cpachecker.core.algorithm.tiger;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.annotation.Nullable;
import javax.management.JMException;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.AlgorithmResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.MainCPAStatistics;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmWithResult;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.counterexamplecheck.CounterexampleCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.PredefinedCoverageCriteria;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.Edges;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.FQLSpecification;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.SingletonECPEdgeSet;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.InverseGuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.clustering.ClusteredElementaryCoveragePattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.clustering.InfeasibilityPropagation;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.clustering.InfeasibilityPropagation.Prediction;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.ARTReuse;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.BDDUtils;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.PrecisionCallback;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCase;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestGoalUtils;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestStep;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.ThreeValuedAnswer;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.WorkerRunnable;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.WorklistEntryComparator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.Wrapper;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.WrapperUtil;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.RichModel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGPathExporter;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.cpa.arg.ARGToDotWriter;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.cpa.automaton.PowersetAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.automaton.ReducedAutomatonProduct;
import org.sosy_lab.cpachecker.cpa.bdd.BDDCPA;
import org.sosy_lab.cpachecker.cpa.bdd.BDDTransferRelation;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionRefinementStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.RefinementStrategy;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.predicates.regions.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.resources.ProcessCpuTime;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime.NoTimeMeasurement;
import org.sosy_lab.solver.AssignableTerm;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@Options(prefix = "tiger")
public class TigerAlgorithm
    implements Algorithm, AlgorithmWithResult, PrecisionCallback<PredicatePrecision>, StatisticsProvider, Statistics {

  public static String originalMainFunction = null;

  @Option(secure = true, name = "fqlQuery", description = "Coverage criterion given as an FQL query")
  private String fqlQuery = PredefinedCoverageCriteria.BASIC_BLOCK_COVERAGE; // default is basic block coverage

  @Option(secure = true, name = "optimizeGoalAutomata", description = "Optimize the test goal automata")
  private boolean optimizeGoalAutomata = true;

  @Option(secure = true, name = "printARGperGoal", description = "Print the ARG for each test goal")
  private boolean dumpARGperPartition = false;

  @Option(
      secure = true,
      name = "useAutomataCrossProduct",
      description = "Compute the cross product of the goal automata?")
  private boolean useAutomataCrossProduct = false;

  @Option(
      secure = true,
      name = "checkCoverage",
      description = "Checks whether a test case for one goal covers another test goal")
  private boolean checkCoverage = true;

  @Option(secure = true, name = "reuseARG", description = "Reuse ARG across test goals")
  private boolean reuseARG = true;

  @Option(secure = true, name = "reusePredicates", description = "Reuse predicates across modifications of an ARG.")
  private boolean reusePredicates = true;

  @Option(secure = true, name = "usePowerset", description = "Construct the powerset of automata states.")
  private boolean usePowerset = true;

  @Option(secure = true, name = "testsuiteFile", description = "Filename for output of generated test suite")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path testsuiteFile = Paths.get("testsuite.txt");

  @Option(
      secure = true,
      name = "testcaseGeneartionTimesFile",
      description = "Filename for output of geneartion times of test cases")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path testcaseGenerationTimesFile = Paths.get("generationTimes.csv");

  @Option(
      secure = true,
      description = "File for saving processed goal automata in DOT format (%s will be replaced with automaton name)")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate dumpGoalAutomataTo = PathTemplate.ofFormatString("Automaton_%s.dot");

  @Option(
      secure = true,
      name = "useInfeasibilityPropagation",
      description = "Map information on infeasibility of one test goal to other test goals.")
  private boolean useInfeasibilityPropagation = false;

  enum TimeoutStrategy {
    SKIP_AFTER_TIMEOUT,
    RETRY_AFTER_TIMEOUT
  }

  @Option(
      secure = true,
      name = "timeoutStrategy",
      description = "How to proceed with timed-out goals if some time remains after processing all other goals.")
  private TimeoutStrategy timeoutStrategy = TimeoutStrategy.SKIP_AFTER_TIMEOUT;

  @Option(
      secure = true,
      name = "limitsPerGoal.time.cpu.increment",
      description = "Value for which timeout gets incremented if timed-out goals are re-processed.")
  private int timeoutIncrement = 0;

  /*@Option(name = "globalCoverageCheckBeforeTimeout", description = "Perform a coverage check on all remaining coverage goals before the global time out happens.")
  private boolean globalCoverageCheckBeforeTimeout = false;

  @Option(name = "timeForGlobalCoverageCheck", description = "Time budget for coverage check before global time out.")
  private String timeForGlobalCoverageCheck = "0s";*/

  @Option(
      secure = true,
      name = "limitsPerGoal.time.cpu",
      description = "Time limit per test goal in seconds (-1 for infinity).")
  private long cpuTimelimitPerGoal = -1;

  @Option(
      secure = true,
      name = "inverseOrder",
      description = "Inverses the order of test goals each time a new round of re-processing of timed-out goals begins.")
  private boolean inverseOrder = true;

  @Option(
      secure = true,
      name = "useOrder",
      description = "Enforce the original order each time a new round of re-processing of timed-out goals begins.")
  private boolean useOrder = true;

  @Option(
      secure = true,
      name = "algorithmConfigurationFile",
      description = "Configuration file for internal cpa algorithm.")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path algorithmConfigurationFile = Paths.get("config/tiger-internal-algorithm.properties");

  @Option(
      secure = true,
      name = "tiger_with_presenceConditions",
      description = "Use Test Input Generator algorithm with an extension using the BDDCPA to model product line presence conditions")
  public boolean useTigerAlgorithm_with_pc = false;

  @Option(
      secure = true,
      name = "numberOfTestGoalsPerRun",
      description = "The number of test goals processed per CPAchecker run (0: all test goals in one run).")
  private int numberOfTestGoalsPerRun = 1;

  @Option(
      secure = true,
      name = "allCoveredGoalsPerTestCase",
      description = "Returns all test goals covered by a test case.")
  private boolean allCoveredGoalsPerTestCase = false;

  @Option(
      secure = true,
      name = "outputInterface",
      description = "List of output variables: v1,v2,v3...")
  private String outputInterface = "";

  @Option(
      secure = true,
      name = "printLabels",
      description = "Prints labels reached with the error path of a test case.")
  private boolean printLabels = false;

  private final Configuration config;
  private final LogManager logger;
  final private ShutdownManager mainShutdownManager;
  private final CFA cfa;

  private ConfigurableProgramAnalysis cpa;

  private CoverageSpecificationTranslator mCoverageSpecificationTranslator;
  private FQLSpecification fqlSpecification;

  private Wrapper wrapper;
  private GuardedEdgeLabel mAlphaLabel;
  private GuardedEdgeLabel mOmegaLabel;
  private InverseGuardedEdgeLabel mInverseAlphaLabel;

  private TestSuite testsuite;
  private ReachedSet reachedSet = null;
  private ReachedSet outsideReachedSet = null;
  private Set<String> outputVariables;

  private PredicatePrecision reusedPrecision = null;

  private int statistics_numberOfTestGoals;
  private int statistics_numberOfProcessedTestGoals = 0;
  private StatCpuTime statCpuTime = null;

  private Prediction[] lGoalPrediction;

  private String programDenotation;
  private MainCPAStatistics stats;
  private int testCaseId = 0;
  private int partitionId = 0;

  NamedRegionManager bddCpaNamedRegionManager = null;

  private TestGoalUtils testGoalUtils = null;

  public TigerAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa, ShutdownManager pShutdownManager,
      CFA pCfa, Configuration pConfig, LogManager pLogger, String pProgramDenotation,
      @Nullable final MainCPAStatistics pStatistics) throws InvalidConfigurationException {

    programDenotation = pProgramDenotation;
    stats = pStatistics;
    statCpuTime = new StatCpuTime();

    mainShutdownManager = pShutdownManager;
    logger = pLogger;
    config = pConfig;
    config.inject(this);

    cpa = pCpa;
    cfa = pCfa;

    // Check if BDD is enabled for variability-aware test-suite generation
    bddCpaNamedRegionManager = BDDUtils.getBddCpaNamedRegionManagerFromCpa(cpa, useTigerAlgorithm_with_pc);

    testsuite = new TestSuite(bddCpaNamedRegionManager, printLabels, useTigerAlgorithm_with_pc);
    outputVariables = new TreeSet<>();
    for (String variable : outputInterface.split(",")) {
      outputVariables.add(variable);
    }

    assert TigerAlgorithm.originalMainFunction != null;
    mCoverageSpecificationTranslator =
        new CoverageSpecificationTranslator(pCfa.getFunctionHead(TigerAlgorithm.originalMainFunction));

    wrapper = new Wrapper(pCfa, TigerAlgorithm.originalMainFunction);

    mAlphaLabel = new GuardedEdgeLabel(new SingletonECPEdgeSet(wrapper.getAlphaEdge()));
    mInverseAlphaLabel = new InverseGuardedEdgeLabel(mAlphaLabel);
    mOmegaLabel = new GuardedEdgeLabel(new SingletonECPEdgeSet(wrapper.getOmegaEdge()));

    testGoalUtils = new TestGoalUtils(logger, useTigerAlgorithm_with_pc, bddCpaNamedRegionManager, mAlphaLabel,
        mInverseAlphaLabel, mOmegaLabel);

    // get internal representation of FQL query
    fqlSpecification = testGoalUtils.parseFQLQuery(fqlQuery);
  }

  @Override
  public String getName() {
    return "TigerAlgorithm";
  }

  @Override
  public void setPrecision(PredicatePrecision pNewPrec) {
    reusedPrecision = pNewPrec;
  }

  @Override
  public PredicatePrecision getPrecision() {
    return reusedPrecision;
  }

  public long getCpuTime() {
    long cpuTime = -1;
    try {
      long currentCpuTime = (long) (ProcessCpuTime.read() / 1e6);
      long currentWallTime = System.currentTimeMillis();
      statCpuTime.onMeasurementResult(currentCpuTime - statCpuTime.getCpuTimeSumMilliSecs(),
          currentWallTime - statCpuTime.getWallTimeSumMsec());
      cpuTime = statCpuTime.getCpuTimeSumMilliSecs();
    } catch (NoTimeMeasurement | JMException e) {
      logger.logUserException(Level.WARNING, e, "Could not get CPU time for statistics.");
    }

    return cpuTime;
  }

  @Override
  public AlgorithmResult getResult() {
    return testsuite;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // we empty pReachedSet to stop complaints of an incomplete analysis
    // Problem: pReachedSet does not match the internal CPA structure!
    logger.logf(Level.INFO,
        "We will not use the provided reached set since it violates the internal structure of Tiger's CPAs");
    logger.logf(Level.INFO, "We empty pReachedSet to stop complaints of an incomplete analysis");
    outsideReachedSet = pReachedSet;
    outsideReachedSet.clear();

    statCpuTime.start();
    testsuite.setGenerationStartTime(getCpuTime());

    // Optimization: Infeasibility propagation
    Pair<Boolean, LinkedList<Edges>> lInfeasibilityPropagation = initializeInfisabilityPropagation();

    Set<Goal> goalsToCover = testGoalUtils.extractTestGoalPatterns(fqlSpecification, lGoalPrediction,
        lInfeasibilityPropagation, mCoverageSpecificationTranslator, optimizeGoalAutomata);
    statistics_numberOfTestGoals = goalsToCover.size();
    logger.logf(Level.INFO, "Number of test goals: %d", statistics_numberOfTestGoals);


    // (iii) do test generation for test goals ...
    boolean wasSound = true;
    try {
      if (!testGeneration(goalsToCover, lInfeasibilityPropagation)) {
        logger.logf(Level.WARNING, "Test generation contained unsound reachability analysis runs!");
        wasSound = false;
      }
    } catch (InvalidConfigurationException e1) {
      throw new CPAException("Invalid configuration!", e1);
    }

    assert (goalsToCover.isEmpty());

    // write generated test suite and mapping to file system
    try (Writer writer =
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(testsuiteFile.getAbsolutePath()), "utf-8"))) {
      writer.write(testsuite.toString());
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (wasSound) {
      return AlgorithmStatus.SOUND_AND_PRECISE;
    } else {
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
    }
  }

  private Pair<Boolean, LinkedList<Edges>> initializeInfisabilityPropagation() {
    Pair<Boolean, LinkedList<Edges>> lInfeasibilityPropagation;

    if (useInfeasibilityPropagation) {
      lInfeasibilityPropagation = InfeasibilityPropagation.canApplyInfeasibilityPropagation(fqlSpecification);
    } else {
      lInfeasibilityPropagation = Pair.of(Boolean.FALSE, null);
    }

    return lInfeasibilityPropagation;
  }

  private ImmutableSet<Goal> nextTestGoalSet(Set<Goal> pGoalsToCover, TestSuite pSuite) {

    final int testGoalSetSize = (numberOfTestGoalsPerRun <= 0)
        ? pGoalsToCover.size()
        : (pGoalsToCover.size() > numberOfTestGoalsPerRun) ? numberOfTestGoalsPerRun : pGoalsToCover.size();

    Builder<Goal> builder = ImmutableSet.<Goal> builder();

    Iterator<Goal> it = pGoalsToCover.iterator();
    for (int i = 0; i < testGoalSetSize; i++) {
      if (it.hasNext()) {
        builder.add(it.next());
      }
    }

    ImmutableSet<Goal> result = builder.build();
    statistics_numberOfProcessedTestGoals += result.size();

    return result;
  }

  private boolean testGeneration(Set<Goal> pGoalsToCover,
      Pair<Boolean, LinkedList<Edges>> pInfeasibilityPropagation)
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
          logger.logf(Level.INFO, "Incremented timeout from %d to %d seconds.", oldCPUTimeLimitPerGoal,
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

            set.addAll(testsuite.getTimedOutGoals().entrySet());
          } else {
            set = new LinkedList<>();
            set.addAll(testsuite.getTimedOutGoals().entrySet());
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
        Set<Goal> goalsToBeProcessed = nextTestGoalSet(pGoalsToCover, testsuite);
        pGoalsToCover.removeAll(goalsToBeProcessed);
        partitionId++;

        if (useTigerAlgorithm_with_pc) {
          /* force that a new reachedSet is computed when first starting on a new TestGoal with initial PC TRUE.
           * This enforces that no very constrained ARG is reused when computing a new ARG for a new testgoal with broad pc (TRUE).
           * This strategy allows us to set option tiger.reuseARG=true such that ARG is reused in testgoals (pcs get only more specific).
           * Keyword: overapproximation
           */
          //assert false;
          reachedSet = null;
        }

        String logString = "Processing test goals ";
        for (Goal g : goalsToBeProcessed) {
          logString += g.getIndex() + " (" + testsuite.getTestGoalLabel(g) + "), ";
        }
        logString = logString.substring(0, logString.length() - 2);

        if (useTigerAlgorithm_with_pc) {
          Region remainingPresenceCondition =
              BDDUtils.composeRemainingPresenceConditions(goalsToBeProcessed, testsuite, bddCpaNamedRegionManager);
          logger.logf(Level.INFO, "%s of %d for PC %s.", logString, numberOfTestGoals,
              bddCpaNamedRegionManager.dumpRegion(remainingPresenceCondition));
        } else {
          logger.logf(Level.INFO, "%s of %d.", logString, numberOfTestGoals);
        }

        // TODO: enable tiger techniques for multi-goal generation in one run
        //        if (lGoalPrediction != null && lGoalPrediction[goal.getIndex() - 1] == Prediction.INFEASIBLE) {
        //          // GoalPrediction does not use the target presence condition (remainingPCforGoalCoverage)
        //          // I think this is OK (any infeasible goal will be even more infeasible when restricted with a certain pc)
        //          // TODO: remainingPCforGoalCoverage could perhaps be used to improve precision of the prediction?
        //          logger.logf(Level.INFO, "This goal is predicted as infeasible!");
        //          testsuite.addInfeasibleGoal(goal, goal.getRemainingPresenceCondition(), lGoalPrediction);
        //          continue;
        //        }
        //
        //        NondeterministicFiniteAutomaton<GuardedEdgeLabel> currentAutomaton = goal.getAutomaton();
        //        if (ARTReuse.isDegeneratedAutomaton(currentAutomaton)) {
        //          // current goal is for sure infeasible
        //          logger.logf(Level.INFO, "Test goal infeasible.");
        //          if (useTigerAlgorithm_with_pc) {
        //            logger.logf(Level.WARNING, "Goal %d is infeasible for remaining PC %s !", goal.getIndex(),
        //                bddCpaNamedRegionManager.dumpRegion(goal.getInfeasiblePresenceCondition()));
        //          }
        //          testsuite.addInfeasibleGoal(goal, goal.getRemainingPresenceCondition(), lGoalPrediction);
        //          continue; // we do not want to modify the ARG for the degenerated automaton to keep more reachability information
        //        }
        //
        //          if (checkCoverage) {
        //            for (Goal goalToBeChecked : goalsToBeProcessed) {
        //              if (checkAndCoverGoal(goalToBeChecked)) {
        //                if (useTigerAlgorithm_with_pc) {
        //                  pGoalsToCover.remove(goalToBeChecked);
        //                }
        //                if (lGoalPrediction != null) {
        //                  lGoalPrediction[goalToBeChecked.getIndex() - 1] = Prediction.FEASIBLE;
        //                }
        //              }
        //            }
        //          }

        //          if (testsuite.areGoalsCoveredOrInfeasible(goalsToBeProcessed)) {
        //            continue;
        //          }

        // goal is uncovered so far; run CPAchecker to cover it
        ReachabilityAnalysisResult result =
            runReachabilityAnalysis(pGoalsToCover, goalsToBeProcessed, previousAutomaton, pInfeasibilityPropagation);
        if (result.equals(ReachabilityAnalysisResult.UNSOUND)) {
          logger.logf(Level.WARNING, "Analysis run was unsound!");
          wasSound = false;
        }
        //        previousAutomaton = currentAutomaton;

        if (result.equals(ReachabilityAnalysisResult.TIMEOUT)) {
          break;
        }
      }

      // reprocess timed-out goals
      if (testsuite.getTimedOutGoals().isEmpty()) {
        logger.logf(Level.INFO, "There were no timed out goals.");
        retry = false;
      } else {
        if (!timeoutStrategy.equals(TimeoutStrategy.RETRY_AFTER_TIMEOUT)) {
          logger.logf(Level.INFO, "There were timed out goals but retry after timeout strategy is disabled.");
        } else {
          retry = true;
        }
      }
    } while (retry);

    return wasSound;
  }

  @Nullable private ARGState findStateAfterCriticalEdge(Goal pCriticalForGoal, ARGPath pPath) {
    PathIterator it = pPath.pathIterator();

    final CFAEdge criticalEdge = pCriticalForGoal.getCriticalEdge();

    while (it.hasNext()) {
      ARGState state = it.getAbstractState();
      if (it.getIndex() != 0) { // get incoming edge is not allowed if index==0
        if (it.getIncomingEdge().equals(criticalEdge)) {
          return state;
        }
      }
      it.advance();
    }

    return null;
  }

  private Set<Goal> updateTestsuiteByCoverageOf(TestCase pTestcase, Collection<Goal> pCheckCoverageOf) {

    Set<Goal> coveredGoals = new HashSet<>();
    Set<Goal> goalsCoveredByLastState = Sets.newHashSet();

    ARGState lastState = pTestcase.getArgPath().getLastState();
    for (Property p : lastState.getViolatedProperties()) {
      Preconditions.checkState(p instanceof Goal);
      goalsCoveredByLastState.add((Goal) p);
    }

    for (Goal goal : pCheckCoverageOf) {
      if (!allCoveredGoalsPerTestCase && testsuite.isGoalCovered(goal)) {
        continue;
      }

      final ThreeValuedAnswer isCovered;
      if (goalsCoveredByLastState.contains(goal)) {
        isCovered = ThreeValuedAnswer.ACCEPT;
      } else {
        isCovered = TigerAlgorithm.accepts(goal.getAutomaton(), pTestcase.getErrorPath());
      }

      if (isCovered.equals(ThreeValuedAnswer.UNKNOWN)) {
        logger.logf(Level.WARNING, "Coverage check for goal %d could not be performed in a precise way!",
            goal.getIndex());
        continue;
      } else if (isCovered.equals(ThreeValuedAnswer.REJECT)) {
        continue;
      }

      // test goal is already covered by an existing test case
      if (useTigerAlgorithm_with_pc) {

        final ARGState criticalState = findStateAfterCriticalEdge(goal, pTestcase.getArgPath());

        if (criticalState == null) {
          Path argFile = Paths.get("output", "ARG_goal_criticalIsNull_" + Integer.toString(goal.getIndex()) + ".dot");

          final Set<Pair<ARGState, ARGState>> allTargetPathEdges = new HashSet<>();
          allTargetPathEdges.addAll(pTestcase.getArgPath().getStatePairs());

          try (Writer w = Files.openOutputFile(argFile)) {
            ARGToDotWriter.write(w, (ARGState) reachedSet.getFirstState(),
                ARGUtils.CHILDREN_OF_STATE,
                Predicates.alwaysTrue(),
                Predicates.in(allTargetPathEdges));
          } catch (IOException e) {
            logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
          }

          throw new RuntimeException("Each ARG path of a counterexample must be along a critical edge! None for edge " + goal.getCriticalEdge());
        }

        Preconditions.checkState(criticalState != null, "Each ARG path of a counterexample must be along a critical edge!");

        Region statePresenceCondition = BDDUtils.getRegionFromWrappedBDDstate(criticalState);
        Preconditions.checkState(statePresenceCondition != null, "Each critical state must be annotated with a presence condition!");

        if (allCoveredGoalsPerTestCase
            || !bddCpaNamedRegionManager.makeAnd(testsuite.getRemainingPresenceCondition(goal), statePresenceCondition).isFalse()) {

          // configurations in testGoalPCtoCover and testcase.pc have a non-empty intersection

          testsuite.addTestCase(pTestcase, goal, statePresenceCondition);

          logger.logf(Level.WARNING, "Covered some PCs for Goal %d (%s) for PC %s by test case %d!",
              goal.getIndex(), testsuite.getTestGoalLabel(goal),
              bddCpaNamedRegionManager.dumpRegion(statePresenceCondition), pTestcase.getId());
          logger.logf(Level.WARNING, "Remaining PC %s!",
              bddCpaNamedRegionManager.dumpRegion(testsuite.getRemainingPresenceCondition(goal)));

          if (testsuite.getRemainingPresenceCondition(goal).isFalse()) {
            coveredGoals.add(goal);
          }
        }

      } else {
        testsuite.addTestCase(pTestcase, goal, null);
        logger.logf(Level.WARNING, "Covered Goal %d (%s) by test case %d!",
            goal.getIndex(),
            testsuite.getTestGoalLabel(goal),
            pTestcase.getId());
        coveredGoals.add(goal);
      }
    }

    return coveredGoals;
  }


  public static ThreeValuedAnswer accepts(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton,
      List<CFAEdge> pCFAPath) {
    Set<NondeterministicFiniteAutomaton.State> lCurrentStates = new HashSet<>();
    Set<NondeterministicFiniteAutomaton.State> lNextStates = new HashSet<>();

    lCurrentStates.add(pAutomaton.getInitialState());

    boolean lHasPredicates = false;

    for (CFAEdge lCFAEdge : pCFAPath) {
      for (NondeterministicFiniteAutomaton.State lCurrentState : lCurrentStates) {
        // Automaton accepts as soon as it sees a final state (implicit self-loop)
        if (pAutomaton.getFinalStates().contains(lCurrentState)) { return ThreeValuedAnswer.ACCEPT; }

        for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOutgoingEdge : pAutomaton
            .getOutgoingEdges(lCurrentState)) {
          GuardedEdgeLabel lLabel = lOutgoingEdge.getLabel();

          if (lLabel.hasGuards()) {
            lHasPredicates = true;
          } else {
            if (lLabel.contains(lCFAEdge)) {
              lNextStates.add(lOutgoingEdge.getTarget());
            }
          }
        }
      }

      lCurrentStates.clear();

      Set<NondeterministicFiniteAutomaton.State> lTmp = lCurrentStates;
      lCurrentStates = lNextStates;
      lNextStates = lTmp;
    }

    for (NondeterministicFiniteAutomaton.State lCurrentState : lCurrentStates) {
      // Automaton accepts as soon as it sees a final state (implicit self-loop)
      if (pAutomaton.getFinalStates().contains(lCurrentState)) { return ThreeValuedAnswer.ACCEPT; }
    }

    if (lHasPredicates) {
      return ThreeValuedAnswer.UNKNOWN;
    } else {
      return ThreeValuedAnswer.REJECT;
    }
  }

  enum ReachabilityAnalysisResult {
    SOUND,
    UNSOUND,
    TIMEOUT
  }

  private ReachabilityAnalysisResult runReachabilityAnalysis(
      Set<Goal> pUncoveredGoals,
      Set<Goal> pTestGoalsToBeProcessed,
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousGoalAutomaton,
      Pair<Boolean, LinkedList<Edges>> pInfeasibilityPropagation)
          throws CPAException, InterruptedException, InvalidConfigurationException {

    ARGCPA cpa = composeCPA(pTestGoalsToBeProcessed);

    Preconditions.checkState(cpa.getWrappedCPAs().get(0) instanceof CompositeCPA,
        "CPAcheckers automata should be used! The assumption is that the first component is the automata for the current goal!");

    // TODO: enable tiger techniques for multi-goal generation in one run
    //    if (reuseARG && (reachedSet != null)) {
    //      reuseARG(pTestGoalsToBeProcessed, pPreviousGoalAutomaton, lARTCPA);
    //    } else {
    initializeReachedSet(cpa);
    //    }

    Region presenceConditionToCover = BDDUtils.composeRemainingPresenceConditions(
        pTestGoalsToBeProcessed, testsuite, bddCpaNamedRegionManager);

    ShutdownManager shutdownManager = ShutdownManager.createWithParent(mainShutdownManager.getNotifier());
    Algorithm algorithm = initializeAlgorithm(presenceConditionToCover, cpa, shutdownManager);

    ReachabilityAnalysisResult algorithmStatus =
        runAlgorithm(pUncoveredGoals, pTestGoalsToBeProcessed, cpa, pInfeasibilityPropagation,
            presenceConditionToCover, shutdownManager, algorithm);

//    if (dumpARGperPartition) {
//      Path argFile = Paths.get("output", "ARG_goal_" + Integer.toString(partitionId) + ".dot");
//
//      try (Writer w = Files.openOutputFile(argFile)) {
//        ARGUtils.writeARGAsDot(w, (ARGState) reachedSet.getFirstState());
//      } catch (IOException e) {
//        logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
//      }
//    }

    return algorithmStatus;
  }

  private ReachabilityAnalysisResult runAlgorithm(
      Set<Goal> pUncoveredGoals,
      final Set<Goal> pTestGoalsToBeProcessed,
      final ARGCPA pARTCPA, Pair<Boolean, LinkedList<Edges>> pInfeasibilityPropagation,
      final Region pRemainingPresenceCondition,
      final ShutdownManager pShutdownNotifier,
      final Algorithm pAlgorithm)
          throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

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

      if (algorithmStatus != ReachabilityAnalysisResult.TIMEOUT) {

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
            Set<Goal> coveredGoals = null;
            if (allCoveredGoalsPerTestCase) {
              coveredGoals = addTestToSuite(testsuite.getGoals(), cexi, pInfeasibilityPropagation);
            } else if (checkCoverage) {
              coveredGoals =
                  addTestToSuite(Sets.union(pUncoveredGoals, pTestGoalsToBeProcessed), cexi, pInfeasibilityPropagation);
            } else {
              coveredGoals = addTestToSuite(pTestGoalsToBeProcessed, cexi, pInfeasibilityPropagation);
            }
            pUncoveredGoals.removeAll(coveredGoals);
          }
        }

        if (reachedSet.hasWaitingState()) {

          if (useTigerAlgorithm_with_pc) {
            Region remainingPC =
                BDDUtils.composeRemainingPresenceConditions(pTestGoalsToBeProcessed, testsuite,
                    bddCpaNamedRegionManager);
            restrictBdd(remainingPC);
          }
          // Exclude covered goals from further exploration
          Set<Property> toBlacklist = Sets.newHashSet();
          for (Goal goal : pTestGoalsToBeProcessed) {
            if (testsuite.isGoalCoveredOrInfeasible(goal)) {
              toBlacklist.add(goal);
            }
          }

          Precisions.disablePropertiesForWaitlist(pARTCPA, reachedSet, toBlacklist);
        }
      }

    } while ((reachedSet.hasWaitingState()
        && !testsuite.areGoalsCoveredOrInfeasible(pTestGoalsToBeProcessed))
        && (algorithmStatus != ReachabilityAnalysisResult.TIMEOUT));

    if (algorithmStatus == ReachabilityAnalysisResult.TIMEOUT) {
      logger.logf(Level.INFO, "Test goal timed out!");
      testsuite.addTimedOutGoals(pTestGoalsToBeProcessed);
    } else {
      // set test goals infeasible
      for (Goal goal : pTestGoalsToBeProcessed) {
        if (!testsuite.isGoalCovered(goal)) {
          handleInfeasibleTestGoal(goal, pInfeasibilityPropagation);
        }
      }
    }

    return algorithmStatus;
  }

  private ReachabilityAnalysisResult runAlgorithmWithLimit(
      final ShutdownManager algNotifier,
      final Algorithm algorithm)
          throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

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
      WorkerRunnable workerRunnable = new WorkerRunnable(algorithm, reachedSet, cpuTimelimitPerGoal, algNotifier);

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
          algorithmStatus = ReachabilityAnalysisResult.TIMEOUT;
        }
      }
    }
    return algorithmStatus;
  }

  private void restrictBdd(Region pRemainingPresenceCondition) {
    // inject goal Presence Condition in BDDCPA
    BDDCPA bddcpa = null;
    if (cpa instanceof WrapperCPA) {
      // must be non-null, otherwise Exception in constructor of this class
      bddcpa = ((WrapperCPA) cpa).retrieveWrappedCpa(BDDCPA.class);
    } else if (cpa instanceof BDDCPA) {
      bddcpa = (BDDCPA) cpa;
    }
    if (bddcpa.getTransferRelation() instanceof BDDTransferRelation) {
      ((BDDTransferRelation) bddcpa.getTransferRelation()).setGlobalConstraint(pRemainingPresenceCondition);
      logger.logf(Level.INFO, "Restrict BDD to %s.",
          bddCpaNamedRegionManager.dumpRegion(pRemainingPresenceCondition));
    }
  }

  private Algorithm initializeAlgorithm(Region pRemainingPresenceCondition, ARGCPA lARTCPA,
      ShutdownManager algNotifier) throws CPAException {

    Algorithm algorithm;
    try {
      Configuration internalConfiguration = Configuration.builder().loadFromFile(algorithmConfigurationFile).build();

      CoreComponentsFactory coreFactory = new CoreComponentsFactory(internalConfiguration, logger, algNotifier.getNotifier());

      ARGPathExporter argPathExporter = new ARGPathExporter(config, logger, cfa.getMachineModel(), cfa.getLanguage());

      algorithm = coreFactory.createAlgorithm(lARTCPA, programDenotation, cfa, stats);

      if (algorithm instanceof CEGARAlgorithm) {
        CEGARAlgorithm cegarAlg = (CEGARAlgorithm) algorithm;

        Refiner refiner = cegarAlg.getRefiner();
        if (refiner instanceof PredicateCPARefiner) {
          PredicateCPARefiner predicateRefiner = (PredicateCPARefiner) refiner;

          if (reusePredicates) {
            RefinementStrategy strategy = predicateRefiner.getStrategy();
            assert (strategy instanceof PredicateAbstractionRefinementStrategy);

            PredicateAbstractionRefinementStrategy refinementStrategy =
                (PredicateAbstractionRefinementStrategy) strategy;
            refinementStrategy.setPrecisionCallback(this);
          }
        }

        ARGStatistics lARTStatistics;
        try {
          lARTStatistics =
              new ARGStatistics(internalConfiguration, logger, lARTCPA,
                  cfa.getMachineModel(), cfa.getLanguage(), null, argPathExporter);
        } catch (InvalidConfigurationException e) {
          throw new RuntimeException(e);
        }
        Set<Statistics> lStatistics = new HashSet<>();
        lStatistics.add(lARTStatistics);
        cegarAlg.collectStatistics(lStatistics);
      }

      if (useTigerAlgorithm_with_pc) {
        restrictBdd(pRemainingPresenceCondition);
      }
    } catch (IOException | InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }
    return algorithm;
  }

  private void initializeReachedSet(ARGCPA lARTCPA) {
    reachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.BFS); // TODO why does TOPSORT not exist anymore?

    AbstractState lInitialElement =
        lARTCPA.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
    Precision lInitialPrecision =
        lARTCPA.getInitialPrecision(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());

    reachedSet.add(lInitialElement, lInitialPrecision);

    outsideReachedSet.add(lInitialElement, lInitialPrecision);

    if (reusePredicates) {
      // initialize reused predicate precision
      PredicateCPA predicateCPA = lARTCPA.retrieveWrappedCpa(PredicateCPA.class);

      if (predicateCPA != null) {
        reusedPrecision = (PredicatePrecision) predicateCPA.getInitialPrecision(cfa.getMainFunction(),
            StateSpacePartition.getDefaultPartition());
      } else {
        logger.logf(Level.INFO, "No predicate CPA available to reuse predicates!");
      }
    }
  }

  private void reuseARG(Goal pGoal, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousGoalAutomaton,
      ARGCPA lARTCPA) {
    ARTReuse.modifyReachedSet(reachedSet, cfa.getMainFunction(), lARTCPA, 0, pPreviousGoalAutomaton,
        pGoal.getAutomaton());

    // reusedPrecision == null indicates that there is no PredicateCPA
    if (reusePredicates && reusedPrecision != null) {
      for (AbstractState lWaitlistElement : reachedSet.getWaitlist()) {
        Precision lOldPrecision = reachedSet.getPrecision(lWaitlistElement);
        Precision lNewPrecision =
            Precisions.replaceByType(lOldPrecision, reusedPrecision, Predicates.instanceOf(PredicatePrecision.class));

        reachedSet.updatePrecision(lWaitlistElement, lNewPrecision);
      }
    }
  }

  /**
   * Context:
   *  The analysis has identified a feasible counterexample, i.e., a test case.
   *
   * Add the test case to the test suite. This includes:
   *  * Register the test case for the goals that it reached on its last abstract state.
   *  * Add the test case for the goals that it would also cover;
   *    this gets checked by running all (uncovered) goal automata on the ARG path of the test case.
   *
   * @param pRemainingGoals
   * @param pCex
   * @param pInfeasibilityPropagation
   */
  private Set<Goal> addTestToSuite(Set<Goal> pRemainingGoals,
      CounterexampleInfo pCex, Pair<Boolean, LinkedList<Edges>> pInfeasibilityPropagation) {

    Preconditions.checkNotNull(pInfeasibilityPropagation);
    Preconditions.checkNotNull(pRemainingGoals);
    Preconditions.checkNotNull(pCex);

    ARGState lastState = pCex.getTargetPath().getLastState();

    // TODO check whether a last state might remain from an earlier run and a reuse of the ARG

    Region testCasePresenceCondition = useTigerAlgorithm_with_pc
        ? BDDUtils.getRegionFromWrappedBDDstate(lastState)
        : null;

    TestCase testcase = createTestcase(pCex, testCasePresenceCondition);
    Set<Goal> coveredGoals = updateTestsuiteByCoverageOf(testcase, pRemainingGoals);

    //        if (lGoalPrediction != null) {
    //          lGoalPrediction[pGoal.getIndex() - 1] = Prediction.FEASIBLE;
    //        }
    return coveredGoals;
  }

  private TestCase createTestcase(final CounterexampleInfo pCex, final Region pPresenceCondition) {

    final RichModel model = pCex.getTargetPathModel();
    final List<BigInteger> inputValues = calculateInputValues(model);
    final List<TestStep> testSteps = calculateTestSteps(model, pCex);

    TestCase testcase = new TestCase(testCaseId++,
        testSteps,
        pCex.getTargetPath(),
        pCex.getTargetPath().getInnerEdges(),
        pPresenceCondition,
        bddCpaNamedRegionManager,
        getCpuTime(),
        inputValues);

    if (useTigerAlgorithm_with_pc) {
      logger.logf(Level.INFO, "Generated new test case %d with PC %s in the last state.", testcase.getId(),
          bddCpaNamedRegionManager.dumpRegion(testcase.getPresenceCondition()));
    } else {
      logger.logf(Level.INFO, "Generated new test case %d.", testcase.getId());
    }

    return testcase;
  }

  private List<TestStep> calculateTestSteps(RichModel pModel, CounterexampleInfo pCex) {
    List<TestStep> testSteps = new ArrayList<>();

    Map<ARGState, CFAEdgeWithAssumptions> x = pModel.getExactVariableValues(pCex.getTargetPath());

    CFAPathWithAssumptions path = pModel.getCFAPathWithAssignments();
    for (CFAEdgeWithAssumptions cfaEdgeWithAssumptions : path) {
    }

    for (Entry<AssignableTerm, Object> e : pModel.entrySet()) {
      if (e.getKey() instanceof AssignableTerm.Variable) {
        AssignableTerm.Variable v = (AssignableTerm.Variable) e.getKey();

        if (v.getName().startsWith(WrapperUtil.CPAtiger_INPUT + "::__retval__")) {
        }
      }
    }

    //    for (Entry<AssignableTerm, Object> e : model.entrySet()) {
    //      if (e.getKey() instanceof AssignableTerm.Variable) {
    //        AssignableTerm.Variable v = (AssignableTerm.Variable) e.getKey();
    //
    //        if (v.getName().startsWith(WrapperUtil.CPAtiger_INPUT + "::__retval__")) {
    //          inputs.add(e);
    //        }
    //      }
    //    }
    //
    //    List<BigInteger> inputValues = new ArrayList<>(inputs.size());
    //
    //    for (Entry<AssignableTerm, Object> e : inputs) {
    //      //assert e.getValue() instanceof BigInteger;
    //      //inputValues.add((BigInteger)e.getValue());
    //      inputValues.add(new BigInteger(e.getValue().toString()));
    //    }

    return null;
  }

  private void handleInfeasibleTestGoal(Goal pGoal, Pair<Boolean, LinkedList<Edges>> pInfeasibilityPropagation) {
    if (lGoalPrediction != null) {
      lGoalPrediction[pGoal.getIndex() - 1] = Prediction.INFEASIBLE;
    }

    if (useTigerAlgorithm_with_pc) {
      testsuite.addInfeasibleGoal(pGoal, testsuite.getRemainingPresenceCondition(pGoal), lGoalPrediction);
      testsuite.setInfeasiblePresenceCondition(pGoal, testsuite.getRemainingPresenceCondition(pGoal));
      testsuite.setRemainingPresenceCondition(pGoal, bddCpaNamedRegionManager.makeFalse());
      logger.logf(Level.WARNING, "Goal %d is infeasible for remaining PC %s!", pGoal.getIndex(),
          bddCpaNamedRegionManager.dumpRegion(testsuite.getRemainingPresenceCondition(pGoal)));
    } else {
      logger.logf(Level.WARNING, "Goal %d is infeasible!", pGoal.getIndex());
      testsuite.addInfeasibleGoal(pGoal, null, lGoalPrediction);
    }

    // TODO add missing soundness checks!
    if (pInfeasibilityPropagation.getFirst()) {
      logger.logf(Level.INFO, "Do infeasibility propagation!");
      HashSet<CFAEdge> lTargetEdges = new HashSet<>();
      ClusteredElementaryCoveragePattern lClusteredPattern =
          (ClusteredElementaryCoveragePattern) pGoal.getPattern();
      ListIterator<ClusteredElementaryCoveragePattern> lRemainingPatterns =
          lClusteredPattern.getRemainingElementsInCluster();
      int lTmpIndex = pGoal.getIndex() - 1; // caution lIndex starts at 0
      while (lRemainingPatterns.hasNext()) {
        Prediction lPrediction = lGoalPrediction[lTmpIndex];
        ClusteredElementaryCoveragePattern lRemainingPattern = lRemainingPatterns.next();
        if (lPrediction.equals(Prediction.UNKNOWN)) {
          lTargetEdges.add(lRemainingPattern.getLastSingletonCFAEdge());
        }

        lTmpIndex++;
      }
      Collection<CFAEdge> lFoundEdges =
          InfeasibilityPropagation.dfs2(lClusteredPattern.getCFANode(),
              lClusteredPattern.getLastSingletonCFAEdge(), lTargetEdges);
      lRemainingPatterns = lClusteredPattern.getRemainingElementsInCluster();
      lTmpIndex = pGoal.getIndex() - 1;
      while (lRemainingPatterns.hasNext()) {
        Prediction lPrediction = lGoalPrediction[lTmpIndex];
        ClusteredElementaryCoveragePattern lRemainingPattern = lRemainingPatterns.next();
        if (lPrediction.equals(Prediction.UNKNOWN)) {
          if (!lFoundEdges.contains(lRemainingPattern.getLastSingletonCFAEdge())) {
            //mFeasibilityInformation.setStatus(lTmpIndex+1, FeasibilityInformation.FeasibilityStatus.INFEASIBLE);
            // TODO remove ???
            lGoalPrediction[lTmpIndex] = Prediction.INFEASIBLE;
          }
        }
        lTmpIndex++;
      }
    }
  }

  private Pair<String, Integer> extractSsaComponents(final String pIndexedName) {
    String[] comps = pIndexedName.split("@");

    Preconditions.checkArgument(comps.length == 2);

    String variableName = comps[0];
    Integer ssaIndex = Integer.parseInt(comps[1]);

    return Pair.of(variableName, ssaIndex);
  }

  private List<BigInteger> calculateInputValues(RichModel model) {
    Comparator<Map.Entry<AssignableTerm, Object>> comp =
        new Comparator<Map.Entry<AssignableTerm, Object>>() {

          @Override
          public int compare(Entry<AssignableTerm, Object> pArg0, Entry<AssignableTerm, Object> pArg1) {

            assert pArg0.getKey() instanceof AssignableTerm.Variable;
            assert pArg1.getKey() instanceof AssignableTerm.Variable;

            Pair<String, Integer> argOneSsaComps = extractSsaComponents(pArg0.getKey().getName());
            Pair<String, Integer> argTwoSsaComps = extractSsaComponents(pArg1.getKey().getName());

            assert argOneSsaComps.getFirst().equals(argTwoSsaComps.getFirst());

            return argOneSsaComps.getSecond() - argTwoSsaComps.getSecond();
          }

        };

    TreeSet<Map.Entry<AssignableTerm, Object>> inputs = new TreeSet<>(comp);

    for (Entry<AssignableTerm, Object> e : model.entrySet()) {
      if (e.getKey() instanceof AssignableTerm.Variable) {
        AssignableTerm.Variable v = (AssignableTerm.Variable) e.getKey();

        if (v.getName().startsWith(WrapperUtil.CPAtiger_INPUT + "::__retval__")) {
          inputs.add(e);
        }
      }
    }

    List<BigInteger> inputValues = new ArrayList<>(inputs.size());

    for (Entry<AssignableTerm, Object> e : inputs) {
      //assert e.getValue() instanceof BigInteger;
      //inputValues.add((BigInteger)e.getValue());
      inputValues.add(new BigInteger(e.getValue().toString()));
    }
    return inputValues;
  }

  private void dumpAutomaton(Automaton pA) {
    if (dumpGoalAutomataTo == null) {
      return;
    }

    try (Writer w = Files.openOutputFile(dumpGoalAutomataTo.getPath(pA.getName()))) {

      pA.writeDotFile(w);

    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
    }
  }

  private ARGCPA composeCPA(Set<Goal> pGoalsToBeProcessed) throws CPAException, InvalidConfigurationException {

    Preconditions.checkArgument(cpa instanceof ARGCPA,
        "Tiger: Only support for ARGCPA implemented for CPA composition!");
    ARGCPA oldArgCPA = (ARGCPA) cpa;

    List<Automaton> componentAutomata = Lists.newArrayList();
    {
      List<Automaton> goalAutomata = Lists.newArrayList();

      for (Goal goal : pGoalsToBeProcessed) {
        final Automaton a = goal.createControlAutomaton();
        goalAutomata.add(a);
        dumpAutomaton(a);
      }

      if (useAutomataCrossProduct) {
        final Automaton productAutomaton;
        try {
          logger.logf(Level.INFO, "Computing the cross product of %d automata.", pGoalsToBeProcessed.size());
          productAutomaton = ReducedAutomatonProduct.productOf(goalAutomata, "GOAL_PRODUCT");
          logger.logf(Level.INFO, "Cross product with %d states.", productAutomaton.getStates().size());
        } catch (InvalidAutomatonException e) {
          throw new CPAException("One of the automata is invalid!", e);
        }

        dumpAutomaton(productAutomaton);
        componentAutomata.add(productAutomaton);
      } else {
        componentAutomata.addAll(goalAutomata);
      }
    }

    logger.logf(Level.INFO, "Analyzing %d test goals with %d observer automata.", pGoalsToBeProcessed.size(),
        componentAutomata.size());

    Collection<ConfigurableProgramAnalysis> automataCPAs = Lists.newArrayList();

    for (Automaton componentAutomaton : componentAutomata) {

      final CPAFactory automataFactory = usePowerset
          ? PowersetAutomatonCPA.factory()
          : ControlAutomatonCPA.factory();

      automataFactory.setConfiguration(Configuration.copyWithNewPrefix(config, componentAutomaton.getName()));
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
      compositeCpaFactory.setConfiguration(config);
      compositeCpaFactory.setLogger(logger);
      compositeCpaFactory.set(cfa, CFA.class);

      ConfigurableProgramAnalysis lCPA = compositeCpaFactory.createInstance();

      // create ARG CPA
      CPAFactory lARTCPAFactory = ARGCPA.factory();
      lARTCPAFactory.set(cfa, CFA.class);
      lARTCPAFactory.setChild(lCPA);
      lARTCPAFactory.setConfiguration(config);
      lARTCPAFactory.setLogger(logger);

      result = (ARGCPA) lARTCPAFactory.createInstance();

    } catch (InvalidConfigurationException | CPAException e) {
      throw new RuntimeException(e);
    }

    return result;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(this);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {

    pOut.println("Number of test cases:                              " + testsuite.getNumberOfTestCases());
    pOut.println("Number of test goals:                              " + statistics_numberOfTestGoals);
    pOut.println("Number of processed test goals:                    " + statistics_numberOfProcessedTestGoals);

    Set<Goal> feasibleGoals = null;
    Set<Goal> partiallyFeasibleGoals = null;
    Set<Goal> infeasibleGoals = null;
    Set<Goal> partiallyInfeasibleGoals = null;
    Set<Goal> timedoutGoals = null;
    Set<Goal> partiallyTimedoutGoals = null;

    if (useTigerAlgorithm_with_pc) {
      feasibleGoals = new HashSet<>();
      partiallyFeasibleGoals = new HashSet<>();
      infeasibleGoals = new HashSet<>();
      partiallyInfeasibleGoals = new HashSet<>();
      timedoutGoals = new HashSet<>();
      partiallyTimedoutGoals = new HashSet<>();

      for (Goal goal : testsuite.getGoals()) {
        List<TestCase> testcases = testsuite.getCoveringTestCases(goal);
        if (testcases != null && !testcases.isEmpty()) {
          // goal is feasible
          boolean partiallyFeasible = false;
          if (testsuite.isGoalInfeasible(goal)) {
            // goal is partially feasible
            partiallyInfeasibleGoals.add(goal);
            partiallyFeasible = true;
          }
          if (testsuite.isGoalTimedout(goal)) {
            // goal is partially timedout
            partiallyTimedoutGoals.add(goal);
            partiallyFeasible = true;
          }
          if (partiallyFeasible) {
            // goal is partially feasible
            partiallyFeasibleGoals.add(goal);
          } else {
            // goal feasible
            feasibleGoals.add(goal);
          }
        } else if (testsuite.isGoalInfeasible(goal)) {
          // goal is infeasible
          if (testsuite.isGoalTimedout(goal)) {
            // goal is partially timed out
            partiallyInfeasibleGoals.add(goal);
            partiallyInfeasibleGoals.add(goal);
            ;
          } else {
            // goal is infeasible
            infeasibleGoals.add(goal);
          }
        } else {
          // goal is timedout
          timedoutGoals.add(goal);
        }
      }

      pOut.println("Number of feasible test goals:                     " + feasibleGoals.size());
      pOut.println("Number of partially feasible test goals:           " + partiallyFeasibleGoals.size());
      pOut.println("Number of infeasible test goals:                   " + infeasibleGoals.size());
      pOut.println("Number of partially infeasible test goals:         " + partiallyInfeasibleGoals.size());
      pOut.println("Number of timedout test goals:                     " + timedoutGoals.size());
      pOut.println("Number of partially timedout test goals:           " + partiallyTimedoutGoals.size());

      if (timedoutGoals.size() > 0 || partiallyTimedoutGoals.size() > 0) {
        pOut.println("Timeout occured during processing of a test goal!");
      }
    } else {
      pOut.println("Number of feasible test goals:                     " + testsuite.getNumberOfFeasibleTestGoals());
      pOut.println("Number of infeasible test goals:                   " + testsuite.getNumberOfInfeasibleTestGoals());
      pOut.println("Number of timedout test goals:                     " + testsuite.getNumberOfTimedoutTestGoals());

      if (testsuite.getNumberOfTimedoutTestGoals() > 0) {
        pOut.println("Timeout occured during processing of a test goal!");
      }
    }

    // write generated test suite and mapping to file system
    try (Writer writer = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(testsuiteFile.getAbsolutePath()), "utf-8"))) {

      writer.write(testsuite.toString());

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // write test case generation times to file system
    try (Writer writer = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(testcaseGenerationTimesFile.getAbsolutePath()), "utf-8"))) {

      List<TestCase> testcases = new ArrayList<>(testsuite.getTestCases());
      Collections.sort(testcases, new Comparator<TestCase>() {

        @Override
        public int compare(TestCase pTestCase1, TestCase pTestCase2) {
          if (pTestCase1.getGenerationTime() > pTestCase2.getGenerationTime()) {
            return 1;
          } else if (pTestCase1.getGenerationTime() < pTestCase2.getGenerationTime()) { return -1; }
          return 0;
        }
      });

      if (useTigerAlgorithm_with_pc) {
        Set<Goal> feasible = new HashSet<>();
        feasible.addAll(feasibleGoals);
        feasible.addAll(partiallyFeasibleGoals);
        feasible.removeAll(partiallyTimedoutGoals);
        for (Goal goal : feasible) {
          List<TestCase> tests = testsuite.getCoveringTestCases(goal);
          TestCase lastTestCase = getLastTestCase(tests);
          lastTestCase.incrementNumberOfNewlyCoveredGoals();
        }
        Set<Goal> partially = new HashSet<>();
        partially.addAll(feasibleGoals);
        partially.addAll(partiallyFeasibleGoals);
        partially.removeAll(partiallyInfeasibleGoals);
        for (Goal goal : partially) {
          List<TestCase> tests = testsuite.getCoveringTestCases(goal);
          TestCase lastTestCase = getLastTestCase(tests);
          lastTestCase.incrementNumberOfNewlyPartiallyCoveredGoals();
        }

        writer.write(
            "Test Case;Generation Time;Covered Goals After Generation;Completely Covered Goals After Generation;Partially Covered Goals After Generation\n");
        int completelyCoveredGoals = 0;
        int partiallyCoveredGoals = 0;
        for (TestCase testCase : testcases) {
          int newCoveredGoals = testCase.getNumberOfNewlyCoveredGoals();
          int newPartiallyCoveredGoals = testCase.getNumberOfNewlyPartiallyCoveredGoals();
          completelyCoveredGoals += newCoveredGoals;
          partiallyCoveredGoals += newPartiallyCoveredGoals;

          writer.write(testCase.getId() + ";" + testCase.getGenerationTime() + ";"
              + (completelyCoveredGoals + partiallyCoveredGoals) + ";" + completelyCoveredGoals + ";"
              + partiallyCoveredGoals + "\n");
        }
      } else {
        Set<Goal> coveredGoals = new HashSet<>();
        writer.write("Test Case;Generation Time;Covered Goals After Generation\n");
        for (TestCase testCase : testcases) {
          coveredGoals.addAll(testsuite.getTestGoalsCoveredByTestCase(testCase));
          writer.write(testCase.getId() + ";" + testCase.getGenerationTime() + ";" + coveredGoals.size() + "\n");
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private TestCase getLastTestCase(List<TestCase> pTests) {
    TestCase lastTestCase = null;
    for (TestCase testCase : pTests) {
      if (lastTestCase == null || testCase.getGenerationTime() < lastTestCase.getGenerationTime()) {
        lastTestCase = testCase;
      }
    }
    return lastTestCase;
  }

}
