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
import java.util.Comparator;
import java.util.HashMap;
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

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.MainCPAStatistics;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.StartupConfig;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.FQLSpecificationUtil;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.PredefinedCoverageCriteria;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.Edges;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.FQLSpecification;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.SingletonECPEdgeSet;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.InverseGuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.ClusteringCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.IncrementalCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.clustering.ClusteredElementaryCoveragePattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.clustering.InfeasibilityPropagation;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.clustering.InfeasibilityPropagation.Prediction;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.ARTReuse;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.PrecisionCallback;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCase;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.ThreeValuedAnswer;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.WorkerRunnable;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.WorklistEntryComparator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.Wrapper;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.WrapperUtil;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.ErrorPathShrinker;
import org.sosy_lab.cpachecker.cpa.bdd.BDDCPA;
import org.sosy_lab.cpachecker.cpa.bdd.BDDState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionRefinementStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.RefinementStrategy;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.predicates.AssignableTerm;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

import com.google.common.base.Predicates;

@Options(prefix = "tiger")
public class TigerAlgorithm implements Algorithm, PrecisionCallback<PredicatePrecision>, StatisticsProvider, Statistics {

  public static String originalMainFunction = null;

  @Option(secure = true, name = "fqlQuery", description = "Coverage criterion given as an FQL query")
  private String fqlQuery = PredefinedCoverageCriteria.BASIC_BLOCK_COVERAGE; // default is basic block coverage

  @Option(secure = true, name = "optimizeGoalAutomata", description = "Optimize the test goal automata")
  private boolean optimizeGoalAutomata = true;

  @Option(secure = true, name = "printARGperGoal", description = "Print the ARG for each test goal")
  private boolean printARGperGoal = false;

  @Option(
      secure = true,
      name = "checkCoverage",
      description = "Checks whether a test case for one goal covers another test goal")
  private boolean checkCoverage = true;

  @Option(secure = true, name = "reuseARG", description = "Reuse ARG across test goals")
  private boolean reuseARG = true;

  @Option(secure = true, name = "reusePredicates", description = "Reuse predicates across modifications of an ARG.")
  private boolean reusePredicates = true;

  @Option(secure = true, name = "testsuiteFile", description = "Filename for output of generated test suite")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path testsuiteFile = Paths.get("testsuite.txt");

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

  private LogManager logger;
  private StartupConfig startupConfig;

  private ConfigurableProgramAnalysis cpa;
  private CFA cfa;

  private CoverageSpecificationTranslator mCoverageSpecificationTranslator;
  private FQLSpecification fqlSpecification;

  private Wrapper wrapper;
  private GuardedEdgeLabel mAlphaLabel;
  private GuardedEdgeLabel mOmegaLabel;
  private InverseGuardedEdgeLabel mInverseAlphaLabel;

  private TestSuite testsuite;
  private ReachedSet reachedSet = null;
  private ReachedSet outsideReachedSet = null;

  private PredicatePrecision reusedPrecision = null;

  private int statistics_numberOfTestGoals;
  private int statistics_numberOfProcessedTestGoals = 0;

  private Prediction[] lGoalPrediction;

  private String programDenotation;
  private MainCPAStatistics stats;

  List<Pair<Goal, Region>> remainingPCs = null;
  NamedRegionManager bddCpaNamedRegionManager = null;

  public TigerAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa, ShutdownNotifier pShutdownNotifier,
      CFA pCfa, Configuration pConfig, LogManager pLogger, String programDenotation,
      @Nullable final MainCPAStatistics stats) throws InvalidConfigurationException {

    this.programDenotation = programDenotation;
    this.stats = stats;

    startupConfig = new StartupConfig(pConfig, pLogger, pShutdownNotifier);
    startupConfig.getConfig().inject(this);

    logger = pLogger;

    cpa = pCpa;
    cfa = pCfa;

    // Check if BDD is enabled for variability-aware test-suite generation
    if (useTigerAlgorithm_with_pc) {
      if (cpa instanceof WrapperCPA) {
        //TODO: This returns the *first* BDDCPA. Currently I cannot get/match which name the cpa has in the config. Might lead to problems when more than one BDDCPA is configured.
        BDDCPA bddcpa = ((WrapperCPA) cpa).retrieveWrappedCpa(BDDCPA.class);
        if (bddcpa != null) {
          bddCpaNamedRegionManager = bddcpa.getManager();
        } else {
          throw new InvalidConfigurationException("CPAtiger-variability-aware started without BDDCPA. We need BDDCPA!");
        }
      } else if (cpa instanceof BDDCPA) {
        bddCpaNamedRegionManager = ((BDDCPA) cpa).getManager();
      }
    }

    testsuite = new TestSuite(bddCpaNamedRegionManager);

    assert TigerAlgorithm.originalMainFunction != null;
    mCoverageSpecificationTranslator =
        new CoverageSpecificationTranslator(pCfa.getFunctionHead(TigerAlgorithm.originalMainFunction));


    wrapper = new Wrapper(pCfa, TigerAlgorithm.originalMainFunction);

    mAlphaLabel = new GuardedEdgeLabel(new SingletonECPEdgeSet(wrapper.getAlphaEdge()));
    mInverseAlphaLabel = new InverseGuardedEdgeLabel(mAlphaLabel);
    mOmegaLabel = new GuardedEdgeLabel(new SingletonECPEdgeSet(wrapper.getOmegaEdge()));


    // get internal representation of FQL query
    logger.logf(Level.INFO, "FQL query string: %s", fqlQuery);

    fqlSpecification = FQLSpecificationUtil.getFQLSpecification(fqlQuery);

    logger.logf(Level.INFO, "FQL query: %s", fqlSpecification.toString());

    // TODO fix this restriction
    if (fqlSpecification.hasPassingClause()) {
      logger.logf(Level.SEVERE, "No PASSING clauses supported at the moment!");

      throw new InvalidConfigurationException("No PASSING clauses supported at the moment!");
    }

    // TODO fix this restriction
    if (fqlSpecification.hasPredicate()) {
      logger.logf(Level.SEVERE, "No predicates in FQL queries supported at the moment!");

      throw new InvalidConfigurationException("No predicates in FQL queries supported at the moment!");
    }
  }

  private Region getRegionFromWrappedBDDstate(AbstractState pAbstractState) {
    //TODO: This returns the *first* BDDCPAState. Currently I cannot get/match which name the cpa has in the config. Might lead to problems when more than one BDDCPA is configured.
    BDDState wrappedBDDState = getWrappedBDDState(pAbstractState);
    if (wrappedBDDState == null) { throw new RuntimeException("Did not find a BDD state component in a state!"); }
    Region bddStateRegion = wrappedBDDState.getRegion();
    // assert wrappedBDDState.getNamedRegionManager() == bddCpaNamedRegionManager;
    return bddStateRegion;
  }

  BDDState getWrappedBDDState(AbstractState inState) {
    if (inState instanceof BDDState) {
      return (BDDState) inState;
    } else if (inState instanceof AbstractWrapperState) {
      for (AbstractState subState : ((AbstractWrapperState) inState).getWrappedStates()) {
        if (subState instanceof BDDState) {
          return (BDDState) subState;
        } else if (subState instanceof AbstractWrapperState) {
          BDDState res = getWrappedBDDState(subState);
          if (res != null) { return res; }
        }
      }
    }
    return null;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException,
      PredicatedAnalysisPropertyViolationException {

    // we empty pReachedSet to stop complaints of an incomplete analysis
    // Problem: pReachedSet does not match the internal CPA structure!
    logger.logf(Level.INFO,
        "We will not use the provided reached set since it violates the internal structure of Tiger's CPAs");
    logger.logf(Level.INFO, "We empty pReachedSet to stop complaints of an incomplete analysis");
    outsideReachedSet = pReachedSet;
    outsideReachedSet.clear();


    // Optimization: Infeasibility propagation
    Pair<Boolean, LinkedList<Edges>> lInfeasibilityPropagation;

    if (useInfeasibilityPropagation) {
      lInfeasibilityPropagation = InfeasibilityPropagation.canApplyInfeasibilityPropagation(fqlSpecification);
    }
    else {
      lInfeasibilityPropagation = Pair.of(Boolean.FALSE, null);
    }

    LinkedList<ElementaryCoveragePattern> goalPatterns;

    if (lInfeasibilityPropagation.getFirst()) {
      goalPatterns =
          extractTestGoalPatterns_InfeasibilityPropagation(fqlSpecification, lInfeasibilityPropagation.getSecond());

      lGoalPrediction = new Prediction[statistics_numberOfTestGoals];

      for (int i = 0; i < statistics_numberOfTestGoals; i++) {
        lGoalPrediction[i] = Prediction.UNKNOWN;
      }
    }
    else {
      // (ii) translate query into set of test goals
      // I didn't move this operation to the constructor since it is a potentially expensive operation.
      goalPatterns = extractTestGoalPatterns(fqlSpecification);
      // each test goal needs to be covered in all (if possible) products.
      // Therefore we add a "todo" presence-condition TRUE to each test goal
      // it is the "maximum" set of products for which we try to cover this goal (could be useful to limit this set if we have feature models?)
      lGoalPrediction = null;
    }

    LinkedList<Pair<ElementaryCoveragePattern, Region>> pTestGoalPatterns = new LinkedList<>();
    if (useTigerAlgorithm_with_pc) {
      for (int i = 0; i < goalPatterns.size(); i++) {
        pTestGoalPatterns.add(Pair.of(goalPatterns.get(i), bddCpaNamedRegionManager.makeTrue()));
      }
    } else {
      for (int i = 0; i < goalPatterns.size(); i++) {
        pTestGoalPatterns.add(Pair.of(goalPatterns.get(i), (Region) null));
      }
    }

    int goalIndex = 1;
    LinkedList<Goal> pGoalsToCover = new LinkedList<>();
    for (Pair<ElementaryCoveragePattern, Region> pair : pTestGoalPatterns) {
      Goal lGoal =
          constructGoal(goalIndex, pair.getFirst(), mAlphaLabel, mInverseAlphaLabel, mOmegaLabel, optimizeGoalAutomata,
              pair.getSecond());
      pGoalsToCover.add(lGoal);
      goalIndex++;
    }

    // (iii) do test generation for test goals ...
    boolean wasSound = true;
    if (!testGeneration(pGoalsToCover, lInfeasibilityPropagation)) {
      logger.logf(Level.WARNING, "Test generation contained unsound reachability analysis runs!");
      wasSound = false;
    }

    assert (pGoalsToCover.isEmpty());
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

  private LinkedList<ElementaryCoveragePattern> extractTestGoalPatterns_InfeasibilityPropagation(
      FQLSpecification pFQLQuery, LinkedList<Edges> pEdges) {
    logger.logf(Level.INFO, "Extracting test goals.");

    CFANode lInitialNode = this.mAlphaLabel.getEdgeSet().iterator().next().getSuccessor();
    ClusteringCoverageSpecificationTranslator lTranslator =
        new ClusteringCoverageSpecificationTranslator(mCoverageSpecificationTranslator.mPathPatternTranslator, pEdges,
            lInitialNode);

    ElementaryCoveragePattern[] lGoalPatterns = lTranslator.createElementaryCoveragePatternsAndClusters();
    statistics_numberOfTestGoals = lGoalPatterns.length;

    logger.logf(Level.INFO, "Number of test goals: %d", statistics_numberOfTestGoals);

    LinkedList<ElementaryCoveragePattern> goalPatterns = new LinkedList<>();

    for (int lGoalIndex = 0; lGoalIndex < statistics_numberOfTestGoals; lGoalIndex++) {
      goalPatterns.add(lGoalPatterns[lGoalIndex]);
    }

    return goalPatterns;
  }

  private LinkedList<ElementaryCoveragePattern> extractTestGoalPatterns(FQLSpecification pFQLQuery) {
    logger.logf(Level.INFO, "Extracting test goals.");


    // TODO check for (temporarily) unsupported features

    // TODO enable use of infeasibility propagation


    IncrementalCoverageSpecificationTranslator lTranslator =
        new IncrementalCoverageSpecificationTranslator(mCoverageSpecificationTranslator.mPathPatternTranslator);

    statistics_numberOfTestGoals = lTranslator.getNumberOfTestGoals(pFQLQuery.getCoverageSpecification());
    logger.logf(Level.INFO, "Number of test goals: %d", statistics_numberOfTestGoals);

    Iterator<ElementaryCoveragePattern> lGoalIterator = lTranslator.translate(pFQLQuery.getCoverageSpecification());
    LinkedList<ElementaryCoveragePattern> lGoalPatterns = new LinkedList<>();

    for (int lGoalIndex = 0; lGoalIndex < statistics_numberOfTestGoals; lGoalIndex++) {
      lGoalPatterns.add(lGoalIterator.next());
    }

    return lGoalPatterns;
  }

  private boolean isCovered(int goalIndex, Goal lGoal) {
    Region remainingPCforGoalCoverage = lGoal.getPresenceCondition();
    boolean isFullyCovered = false;
    for (TestCase testcase : testsuite.getTestCases()) {
      ThreeValuedAnswer isCovered = TigerAlgorithm.accepts(lGoal.getAutomaton(), testcase.getPath());
      if (isCovered.equals(ThreeValuedAnswer.UNKNOWN)) {
        logger.logf(Level.WARNING, "Coverage check for goal %d could not be performed in a precise way!", goalIndex);
        continue;
      } else if (isCovered.equals(ThreeValuedAnswer.REJECT)) {
        continue;
      }

      // test goal is already covered by an existing test case
      if (useTigerAlgorithm_with_pc) {
        boolean goalCoveredByTestCase = false;
        for (Goal goal : testsuite.getTestGoalsCoveredByTestCase(testcase)) {
          if (lGoal.getIndex() == goal.getIndex()) {
            goalCoveredByTestCase = true;
            break;
          }
        }
        if (!goalCoveredByTestCase) {
          Region coveringRegion = testcase.getPresenceCondition();

          if (!bddCpaNamedRegionManager.makeAnd(lGoal.getPresenceCondition(), coveringRegion).isFalse()) { // configurations in testGoalPCtoCover and testcase.pc have a non-empty intersection
            Goal newGoal =
                constructGoal(lGoal.getIndex(), lGoal.getPattern(), mAlphaLabel, mInverseAlphaLabel, mOmegaLabel,
                    optimizeGoalAutomata, coveringRegion);
            remainingPCforGoalCoverage =
                bddCpaNamedRegionManager.makeAnd(remainingPCforGoalCoverage,
                    bddCpaNamedRegionManager.makeNot(coveringRegion));

            testsuite.addTestCase(testcase, newGoal);

            if (remainingPCforGoalCoverage.isFalse()) {
              logger.logf(Level.INFO, "Test goal %d is already fully covered by an existing test case.", goalIndex);
              isFullyCovered = true;
              break;
            } else {
              logger.logf(Level.INFO, "Test goal %d is already partly covered by an existing test case.", goalIndex,
                  " Remaining PC: ", bddCpaNamedRegionManager.dumpRegion(remainingPCforGoalCoverage));
            }

          } else {
            // test goal is already covered by an existing test case
            logger.logf(Level.INFO, "Test goal %d is already covered by an existing test case.", goalIndex);

            testsuite.addTestCase(testcase, lGoal);

            return true;
          }
        }
      }
    }

    return isFullyCovered;
  }

  private boolean testGeneration(LinkedList<Goal> pGoalsToCover,
      Pair<Boolean, LinkedList<Edges>> pInfeasibilityPropagation) throws CPAException, InterruptedException {

    boolean wasSound = true;

    int numberOfTestGoals = pGoalsToCover.size();

    if (useTigerAlgorithm_with_pc) {
      // get all test goals and initialize their remaining presence conditions
      remainingPCs = new ArrayList<>();
      for (Goal goal : pGoalsToCover) {
        Goal newGoal =
            constructGoal(goal.getIndex(), goal.getPattern(), mAlphaLabel, mInverseAlphaLabel, mOmegaLabel,
                optimizeGoalAutomata, goal.getPresenceCondition());
        remainingPCs.add(Pair.of(newGoal, goal.getPresenceCondition()));
      }
    }

    NondeterministicFiniteAutomaton<GuardedEdgeLabel> previousAutomaton = null;

    while (!pGoalsToCover.isEmpty()) {
      statistics_numberOfProcessedTestGoals++;

      Goal goal = pGoalsToCover.poll();
      Region remainingPCforGoalCoverage = null;
      if (useTigerAlgorithm_with_pc) {
        // the condition identifying configurations that we want to cover (gets reduced in due process until only an infeasible/non-coverable condition remains)
        remainingPCforGoalCoverage = getRemainingPCByTestGoalId(goal.getIndex());
      }

      Boolean stop = false;

      if (useTigerAlgorithm_with_pc) {
        /* force that a new reachedSet is computed when first starting on a new TestGoal with initial PC TRUE.
         * This enforces that no very constrained ARG is reused when computing a new ARG for a new testgoal with broad pc (TRUE).
         * This strategy allows us to set option tiger.reuseARG=true such that ARG is reused in testgoals (pcs get only more specific).
         * Keyword: overapproximation
         */
        reachedSet = null;
      }

      while (!stop && (remainingPCforGoalCoverage != null ? !remainingPCforGoalCoverage.isFalse() : true)) {
        if (!useTigerAlgorithm_with_pc) {
          stop = true;
        }

        if (useTigerAlgorithm_with_pc) {
          remainingPCforGoalCoverage = getRemainingPCByTestGoalId(goal.getIndex());
          logger.logf(Level.INFO, "Processing test goal %d of %d for PC %s.", goal.getIndex(), numberOfTestGoals,
              bddCpaNamedRegionManager.dumpRegion(remainingPCforGoalCoverage));
        } else {
          logger.logf(Level.INFO, "Processing test goal %d of %d.", goal.getIndex(), numberOfTestGoals);
        }

        if (lGoalPrediction != null && lGoalPrediction[goal.getIndex() - 1] == Prediction.INFEASIBLE) {
          // GoalPrediction does not use the target presence condition (remainingPCforGoalCoverage)
          // I think this is OK (any infeasible goal will be even more infeasible when restricted with a certain pc)
          // TODO: remainingPCforGoalCoverage could perhaps be used to improve precision of the prediction?
          logger.logf(Level.INFO, "This goal is predicted as infeasible!");
          testsuite.addInfeasibleGoal(goal, remainingPCforGoalCoverage);
          stop = true;
          continue;
        }

        NondeterministicFiniteAutomaton<GuardedEdgeLabel> currentAutomaton = goal.getAutomaton();

        if (ARTReuse.isDegeneratedAutomaton(currentAutomaton)) {
          // current goal is for sure infeasible
          logger.logf(Level.INFO, "Test goal infeasible.");
          testsuite.addInfeasibleGoal(goal, remainingPCforGoalCoverage);

          if (lGoalPrediction != null) {
            lGoalPrediction[goal.getIndex() - 1] = Prediction.INFEASIBLE;
          }

          if (useTigerAlgorithm_with_pc) {
            // update PC coverage todo
            if (bddCpaNamedRegionManager.entails(testsuite.getInfeasibleGoals().get(goal), remainingPCforGoalCoverage)) {
              // 1st condition: this goal is infeasible for some constraint
              // 2nd condition: remainingPCforGoalCoverage is part of this constraint (implied by this constraint)
              logger.logf(Level.WARNING, "Goal %d is infeasible for remaining PC %s !", goal.getIndex(),
                  bddCpaNamedRegionManager.dumpRegion(remainingPCforGoalCoverage));
              remainingPCforGoalCoverage = bddCpaNamedRegionManager.makeFalse();
            } else {
              // now we need to cover all remaining configurations
              // the remaining configs are represented by the negation of the already covered pcs (in conjunction with the previous testGoalPCtoCover)
              remainingPCforGoalCoverage =
                  bddCpaNamedRegionManager.makeAnd(remainingPCforGoalCoverage,
                      bddCpaNamedRegionManager.makeNot(testsuite.getGoalCoverage(goal)));
              logger.logf(Level.WARNING, "Covered some PCs for Goal %d. Remaining PC %s !", goal.getIndex(),
                  bddCpaNamedRegionManager.dumpRegion(remainingPCforGoalCoverage));
            }

            stop = true;
          }

          continue; // we do not want to modify the ARG for the degenerated automaton to keep more reachability information
        }

        if (checkCoverage && isCovered(goal.getIndex(), goal)) {
          if (lGoalPrediction != null) {
            lGoalPrediction[goal.getIndex() - 1] = Prediction.FEASIBLE;
          }
          continue;
        }

        // goal is uncovered so far; run CPAchecker to cover it
        ReachabilityAnalysisResult result =
            runReachabilityAnalysis(goal.getIndex(), goal, previousAutomaton, pInfeasibilityPropagation,
                getRemainingPCByTestGoalId(goal.getIndex()));
        if (result.equals(ReachabilityAnalysisResult.UNSOUND)) {
          logger.logf(Level.WARNING, "Analysis run was unsound!");
          wasSound = false;
        }
        previousAutomaton = currentAutomaton;

        if (result.equals(ReachabilityAnalysisResult.TIMEDOUT)) {
          stop = true;
          continue;
        }

        if (useTigerAlgorithm_with_pc) {
          // update PC coverage todo
          if (testsuite.isKnownAsInfeasible(goal) &&
              bddCpaNamedRegionManager.entails(testsuite.getInfeasibleGoals().get(goal), remainingPCforGoalCoverage)) {
            // 1st condition: this goal is infeasible for some constraint
            // 2nd condition: remainingPCforGoalCoverage is part of this constraint (implied by this constraint)
            logger.logf(Level.WARNING, "Goal %d is infeasible for remaining PC %s !", goal.getIndex(),
                bddCpaNamedRegionManager.dumpRegion(remainingPCforGoalCoverage));
            remainingPCforGoalCoverage = bddCpaNamedRegionManager.makeFalse();
            stop = true;
            // remainingPCforGoalCoverage := FALSE ensures that the while loop exits and the next goal is processed.
          } else {
            // now we need to cover all remaining configurations
            // the remaining configs are represented by the negation of the already covered pcs (in conjunction with the previous testGoalPCtoCover)
            //          remainingPCforGoalCoverage = bddCpaNamedRegionManager.makeAnd(remainingPCforGoalCoverage, bddCpaNamedRegionManager.makeNot(lGoal.getPresenceCondition()));
            //          logger.logf(Level.WARNING, "Covered some PCs for Goal %d. Remaining PC %s !", goalIndex, bddCpaNamedRegionManager.dumpRegion(remainingPCforGoalCoverage));
          }
        }
      }
    }

    // reprocess timed-out goals
    if (testsuite.getTimedOutGoals().isEmpty()) {
      logger.logf(Level.INFO, "There were no timed out goals.");
    } else {
      if (!timeoutStrategy.equals(TimeoutStrategy.RETRY_AFTER_TIMEOUT)) {
        logger.logf(Level.INFO, "There were timed out goals but retry after timeout strategy is disabled.");
      } else {
        // retry timed-out goals
        // TODO move to upper loop
        Map<Goal, Integer> coverageCheckOpt = new HashMap<>();

        //int previousNumberOfTestCases = 0;
        //int previousPreviousNumberOfTestCases = testsuite.getNumberOfTestCases();

        boolean order = true;

        do {
          if (timeoutIncrement > 0) {
            long oldCPUTimeLimitPerGoal = cpuTimelimitPerGoal;
            cpuTimelimitPerGoal += timeoutIncrement;
            logger.logf(Level.INFO, "Incremented timeout from %d to %d seconds.", oldCPUTimeLimitPerGoal,
                cpuTimelimitPerGoal);
          }

          Collection<Entry<Integer, Pair<Goal, Region>>> set;

          if (useOrder) {
            if (inverseOrder) {
              order = !order;
            }

            // keep original order of goals (or inverse of it)
            if (order) {
              set = new TreeSet<>(WorklistEntryComparator.ORDER_RESPECTING_COMPARATOR);
            }
            else {
              set = new TreeSet<>(WorklistEntryComparator.ORDER_INVERTING_COMPARATOR);
            }

            set.addAll(testsuite.getTimedOutGoals().entrySet());
          }
          else {
            set = new LinkedList<>();
            set.addAll(testsuite.getTimedOutGoals().entrySet());
          }

          testsuite.getTimedOutGoals().clear();

          for (Entry<Integer, Pair<Goal, Region>> entry : set) {
            int goalIndex = entry.getKey();
            Goal lGoal = entry.getValue().getFirst();
            Region lRegion = entry.getValue().getSecond();
            logger.logf(Level.INFO, "Processing test goal %d of %d.", goalIndex, numberOfTestGoals);

            if (lGoalPrediction != null && lGoalPrediction[goalIndex - 1] == Prediction.INFEASIBLE) {
              logger.logf(Level.INFO, "This goal is predicted as infeasible!");

              testsuite.addInfeasibleGoal(lGoal, lRegion);

              continue;
            }

            // TODO optimization: do not check for coverage if no new testcases were generated.
            if (checkCoverage) {
              if (coverageCheckOpt.containsKey(lGoal)) {
                if (coverageCheckOpt.get(lGoal) < testsuite.getNumberOfTestCases()) {
                  if (isCovered(goalIndex, lGoal)) {
                    continue;
                  }
                  else {
                    // TODO optimization: only add if goal times out!
                    coverageCheckOpt.put(lGoal, testsuite.getNumberOfTestCases());
                  }
                }
              }
            }

            /*if (checkCoverage && (previousNumberOfTestCases < testsuite.getNumberOfTestCases()) && isCovered(goalIndex, lGoal)) {
              continue;
            }*/

            ReachabilityAnalysisResult result =
                runReachabilityAnalysis(goalIndex, lGoal, previousAutomaton, pInfeasibilityPropagation,
                    getRemainingPCByTestGoalId(goalIndex));
            if (result.equals(ReachabilityAnalysisResult.UNSOUND)) {
              logger.logf(Level.WARNING, "Analysis run was unsound!");
              wasSound = false;
            }

            previousAutomaton = lGoal.getAutomaton();
          }
        } while (testsuite.hasTimedoutTestGoals());
      }
    }

    //  for (Goal_with_pc g : testsuite.getTestGoals()) {
    //    if (isCovered(g.getIndex(), g)) {
    //      continue;
    //    }
    //  }

    return wasSound;
  }

  private Region getRemainingPCByTestGoalId(int id) {
    if (remainingPCs == null) { return null; }

    for (Pair<Goal, Region> pair : remainingPCs) {
      if (pair.getFirst().getIndex() == id) { return pair.getSecond(); }
    }
    return null;
  }

  enum ReachabilityAnalysisResult {
    SOUND,
    UNSOUND,
    TIMEDOUT
  }

  private ReachabilityAnalysisResult runReachabilityAnalysis(int goalIndex, Goal pGoal,
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousGoalAutomaton,
      Pair<Boolean, LinkedList<Edges>> pInfeasibilityPropagation, Region pRemainingPresenceCondition)
      throws CPAException, InterruptedException {
    GuardedEdgeAutomatonCPA lAutomatonCPA = new GuardedEdgeAutomatonCPA(pGoal.getAutomaton());

    List<ConfigurableProgramAnalysis> lAutomatonCPAs = new ArrayList<>(1);//(2);

    /*if (pPassingCPA != null) {
      lAutomatonCPAs.add(pPassingCPA);
    }*/

    lAutomatonCPAs.add(lAutomatonCPA);



    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<>();
    // TODO what is the more efficient order for the CPAs? Can we substitute a placeholder CPA? or inject an automaton in to an automaton CPA?
    //int lProductAutomatonIndex = lComponentAnalyses.size();
    int lProductAutomatonIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(ProductAutomatonCPA.create(lAutomatonCPAs, false));

    // TODO experiment
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

      lARTCPA = (ARGCPA) lARTCPAFactory.createInstance();
    } catch (InvalidConfigurationException | CPAException e) {
      throw new RuntimeException(e);
    }

    if (reuseARG && (reachedSet != null)) {
      ARTReuse.modifyReachedSet(reachedSet, cfa.getMainFunction(), lARTCPA, lProductAutomatonIndex,
          pPreviousGoalAutomaton, pGoal.getAutomaton());

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
    else {
      reachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.BFS); // TODO why does TOPSORT not exist anymore?

      AbstractState lInitialElement = lARTCPA.getInitialState(cfa.getMainFunction());
      Precision lInitialPrecision = lARTCPA.getInitialPrecision(cfa.getMainFunction());

      reachedSet.add(lInitialElement, lInitialPrecision);

      outsideReachedSet.add(lInitialElement, lInitialPrecision);

      if (reusePredicates) {
        // initialize reused predicate precision
        PredicateCPA predicateCPA = lARTCPA.retrieveWrappedCpa(PredicateCPA.class);

        if (predicateCPA != null) {
          reusedPrecision = (PredicatePrecision) predicateCPA.getInitialPrecision(cfa.getMainFunction());
        }
        else {
          logger.logf(Level.INFO, "No predicate CPA available to reuse predicates!");
        }
      }
    }

    ShutdownNotifier algNotifier = ShutdownNotifier.createWithParent(startupConfig.getShutdownNotifier());

    startupConfig.getConfig();

    Algorithm algorithm;

    try {
      Configuration internalConfiguration = Configuration.builder().loadFromFile(algorithmConfigurationFile).build();

      CoreComponentsFactory factory = new CoreComponentsFactory(internalConfiguration, logger, algNotifier);

      algorithm = factory.createAlgorithm(lARTCPA, programDenotation, cfa, stats);

      if (algorithm instanceof CEGARAlgorithm) {
        CEGARAlgorithm cegarAlg = (CEGARAlgorithm) algorithm;

        Refiner refiner = cegarAlg.getRefiner();
        if (refiner instanceof PredicateCPARefiner) {
          PredicateCPARefiner predicateRefiner = (PredicateCPARefiner) refiner;

          if (reusePredicates) {
            RefinementStrategy strategy = predicateRefiner.getRefinementStrategy();
            assert (strategy instanceof PredicateAbstractionRefinementStrategy);

            PredicateAbstractionRefinementStrategy refinementStrategy =
                (PredicateAbstractionRefinementStrategy) strategy;
            refinementStrategy.setPrecisionCallback(this);
          }
        }

        ARGStatistics lARTStatistics;
        try {
          lARTStatistics = new ARGStatistics(internalConfiguration, lARTCPA);
        } catch (InvalidConfigurationException e) {
          throw new RuntimeException(e);
        }
        Set<Statistics> lStatistics = new HashSet<>();
        lStatistics.add(lARTStatistics);
        cegarAlg.collectStatistics(lStatistics);
      }

      if (useTigerAlgorithm_with_pc) {
        // inject goal Presence Condition in BDDCPA
        BDDCPA bddcpa = null;
        if (cpa instanceof WrapperCPA) {
          // must be non-null, otherwise Exception in constructor of this class
          bddcpa = ((WrapperCPA) cpa).retrieveWrappedCpa(BDDCPA.class);
        } else if (cpa instanceof BDDCPA) {
          bddcpa = (BDDCPA) cpa;
        }
        bddcpa.getTransferRelation().setGlobalConstraint(pRemainingPresenceCondition);
      }
    } catch (IOException | InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }

    boolean analysisWasSound = false;
    boolean hasTimedOut = false;

    if (cpuTimelimitPerGoal < 0) {
      // run algorithm without time limit
      analysisWasSound = algorithm.run(reachedSet).isSound();
    }
    else {
      // run algorithm with time limit
      WorkerRunnable workerRunnable = new WorkerRunnable(algorithm, reachedSet, cpuTimelimitPerGoal, algNotifier);

      Thread workerThread = new Thread(workerRunnable);

      workerThread.start();
      workerThread.join();

      if (workerRunnable.throwableWasCaught()) {
        // TODO: handle exception
        analysisWasSound = false;
        //        throw new RuntimeException(workerRunnable.getCaughtThrowable());
      }
      else {
        analysisWasSound = workerRunnable.analysisWasSound();

        if (workerRunnable.hasTimeout()) {
          logger.logf(Level.INFO, "Test goal timed out!");

          testsuite.addTimedOutGoal(goalIndex, pGoal, pRemainingPresenceCondition);

          hasTimedOut = true;
        }
      }
    }

    if (printARGperGoal) {
      Path argFile = Paths.get("output", "ARG_goal_" + goalIndex + ".dot");

      try (Writer w = Files.openOutputFile(argFile)) {
        ARGUtils.writeARGAsDot(w, (ARGState) reachedSet.getFirstState());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
      }
    }

    if (hasTimedOut) {
      return ReachabilityAnalysisResult.TIMEDOUT;
    }
    else {
      // TODO check whether a last state might remain from an earlier run and a reuse of the ARG
      AbstractState lastState = reachedSet.getLastState();

      if (lastState != null) {
        if (AbstractStates.isTargetState(lastState)) {
          // we consider the test goal as feasible

          logger.logf(Level.INFO, "Test goal is feasible.");

          // TODO add missing soundness checks!

          if (lGoalPrediction != null) {
            lGoalPrediction[goalIndex - 1] = Prediction.FEASIBLE;
          }


          // TODO identify abstract states
          CFAEdge criticalEdge = pGoal.getCriticalEdge();


          // can we obtain a counterexample to check coverage for other test goals?
          Map<ARGState, CounterexampleInfo> counterexamples = lARTCPA.getCounterexamples();

          if (counterexamples.isEmpty()) {
            // TODO: handle empty counter example with presence conditions
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

            Region testCaseCriticalStateRegion = null;
            while (!parents.isEmpty()) {
              //assert (parents.size() == 1);
              /*if (parents.size() != 1) {
                throw new RuntimeException();
              }*/

              ARGState parent = null;

              for (ARGState tmp_parent : parents) {
                parent = tmp_parent;
                break; // we just choose some parent
              }

              CFAEdge edge = parent.getEdgeToChild(argState);
              trace.addFirst(edge);

              // TODO Alex?
              if (edge.equals(criticalEdge)) {
                if (useTigerAlgorithm_with_pc) {
                  testCaseCriticalStateRegion = getRegionFromWrappedBDDstate(argState);
                }
                logger.logf(Level.INFO, "*********************** extract abstract state ***********************");
              }

              argState = parent;
              parents = argState.getParents();
            }

            // TODO we need a different way to obtain input values
            List<BigInteger> inputValues = new ArrayList<>();

            Region testCaseFinalRegion = null;
            if (useTigerAlgorithm_with_pc) {
              testCaseFinalRegion = getRegionFromWrappedBDDstate(lastState);
              logger.logf(
                  Level.INFO,
                  " generated test case with "
                      + (testCaseCriticalStateRegion == null ? "(final)" : "(critical)")
                      + " PC "
                      + bddCpaNamedRegionManager.dumpRegion((testCaseCriticalStateRegion == null ? testCaseFinalRegion
                          : testCaseCriticalStateRegion)));
            }

            TestCase testcase =
                new TestCase(inputValues, trace, shrinkedErrorPath, (testCaseCriticalStateRegion == null
                    ? testCaseFinalRegion : testCaseCriticalStateRegion), bddCpaNamedRegionManager);
            testsuite.addTestCase(testcase, pGoal);
          }
          else {
            // test goal is feasible
            logger.logf(Level.INFO, "Counterexample is available.");

            assert counterexamples.size() == 1;

            for (Map.Entry<ARGState, CounterexampleInfo> lEntry : counterexamples.entrySet()) {
              CounterexampleInfo cex = lEntry.getValue();

              if (cex.isSpurious()) {
                logger.logf(Level.WARNING, "Counterexample is spurious!");
              }
              else {
                Model model = cex.getTargetPathModel();

                Comparator<Map.Entry<Model.AssignableTerm, Object>> comp =
                    new Comparator<Map.Entry<Model.AssignableTerm, Object>>() {

                      @Override
                      public int compare(Entry<AssignableTerm, Object> pArg0, Entry<AssignableTerm, Object> pArg1) {
                        assert pArg0.getKey().getName().equals(pArg1.getKey().getName());
                        assert pArg0.getKey() instanceof Model.Variable;
                        assert pArg1.getKey() instanceof Model.Variable;

                        Model.Variable v0 = (Model.Variable) pArg0.getKey();
                        Model.Variable v1 = (Model.Variable) pArg1.getKey();

                        return (v0.getSSAIndex() - v1.getSSAIndex());
                      }

                    };

                TreeSet<Map.Entry<Model.AssignableTerm, Object>> inputs = new TreeSet<>(comp);

                for (Map.Entry<Model.AssignableTerm, Object> e : model.entrySet()) {
                  if (e.getKey() instanceof Model.Variable) {
                    Model.Variable v = (Model.Variable) e.getKey();

                    if (v.getName().equals(WrapperUtil.CPAtiger_INPUT + "::__retval__")) {
                      inputs.add(e);
                    }
                  }
                }

                List<BigInteger> inputValues = new ArrayList<>(inputs.size());

                for (Map.Entry<Model.AssignableTerm, Object> e : inputs) {
                  //assert e.getValue() instanceof BigInteger;
                  //inputValues.add((BigInteger)e.getValue());
                  inputValues.add(new BigInteger(e.getValue().toString()));
                }

                // calcualte shrinked error path
                List<CFAEdge> shrinkedErrorPath = new ErrorPathShrinker().shrinkErrorPath(cex.getTargetPath());

                if (useTigerAlgorithm_with_pc) {
                  /* We could determine regions for coverage goals reached earlier during execution of the test case.
                     * Now we can't because cex */
                  List<Goal> newTestGoals = new ArrayList<>();
                  Region testCaseCriticalStateRegion = null;
                  PathIterator pathIterator = cex.getTargetPath().pathIterator();
                  List<Pair<Goal, Region>> toBeDeleted = new ArrayList<>();
                  List<Pair<Goal, Region>> toBeAdded = new ArrayList<>();
                  while (pathIterator.hasNext()) {
                    ARGState state = pathIterator.getAbstractState();
                    if (pathIterator.getIndex() != 0) { // get incoming edge is not allowed if index==0
                      for (Pair<Goal, Region> remaining : remainingPCs) {
                        if (pathIterator.getIncomingEdge().equals(remaining.getFirst().getCriticalEdge())) {
                          testCaseCriticalStateRegion = getRegionFromWrappedBDDstate(state);
                          if (testCaseCriticalStateRegion != null) {
                            Goal newGoal =
                                constructGoal(remaining.getFirst().getIndex(), remaining.getFirst().getPattern(),
                                    mAlphaLabel, mInverseAlphaLabel, mOmegaLabel, optimizeGoalAutomata,
                                    testCaseCriticalStateRegion);
                            newGoal.setPresenceCondition(testCaseCriticalStateRegion);
                            newTestGoals.add(newGoal);
                            toBeDeleted.add(remaining);
                            Region remainingPCforGoal =
                                bddCpaNamedRegionManager.makeAnd(remaining.getSecond(),
                                    bddCpaNamedRegionManager.makeNot(newGoal.getPresenceCondition()));
                            logger.logf(Level.WARNING, "Covered some PCs for Goal %d. Remaining PC %s !",
                                newGoal.getIndex(), bddCpaNamedRegionManager.dumpRegion(remainingPCforGoal));
                            toBeAdded.add(Pair.of(remaining.getFirst(), remainingPCforGoal));

                            if (pathIterator.getIncomingEdge().equals(criticalEdge)) {
                              pRemainingPresenceCondition =
                                  bddCpaNamedRegionManager.makeAnd(pRemainingPresenceCondition,
                                      bddCpaNamedRegionManager.makeNot(newGoal.getPresenceCondition()));
                            }
                            continue;
                          }
                        }
                      }
                    }
                    pathIterator.advance();
                  }

                  remainingPCs.removeAll(toBeDeleted);
                  remainingPCs.addAll(toBeAdded);

                  Region testCaseFinalRegion = getRegionFromWrappedBDDstate(reachedSet.getLastState());
                  logger.logf(
                      Level.INFO,
                      " generated test case with "
                          + (testCaseCriticalStateRegion == null ? "(final)" : "(critical)")
                          + " PC "
                          + bddCpaNamedRegionManager.dumpRegion((testCaseCriticalStateRegion == null
                              ? testCaseFinalRegion : testCaseCriticalStateRegion)));
                  TestCase testcase =
                      new TestCase(inputValues, cex.getTargetPath().asEdgesList(), shrinkedErrorPath,
                          (testCaseCriticalStateRegion == null ? testCaseFinalRegion : testCaseCriticalStateRegion),
                          bddCpaNamedRegionManager);
                  for (Goal newGoal : newTestGoals) {
                    testsuite.addTestCase(testcase, newGoal);
                  }
                } else {
                  TestCase testcase =
                      new TestCase(inputValues, cex.getTargetPath().asEdgesList(), shrinkedErrorPath, null, null);
                  testsuite.addTestCase(testcase, pGoal);

                  //for (Pair<ARGState, CFAEdge> stateEdgePair : cex.getTargetPath()) {
                  for (CFAEdge lCFAEdge : cex.getTargetPath().asEdgesList()) {
                    //if (stateEdgePair.getSecond().equals(criticalEdge)) {
                    if (lCFAEdge.equals(criticalEdge)) {
                      logger.logf(Level.INFO, "*********************** extract abstract state ***********************");
                    }
                  }
                }
              }
            }
          }
        }
        else {
          // TODO: handle infeasible
          // we consider the test goals is infeasible
          logger.logf(Level.INFO, "Test goal infeasible.");

          if (lGoalPrediction != null) {
            lGoalPrediction[goalIndex - 1] = Prediction.INFEASIBLE;
          }

          testsuite.addInfeasibleGoal(pGoal, pRemainingPresenceCondition);
          // TODO add missing soundness checks!
          if (pInfeasibilityPropagation.getFirst()) {
            logger.logf(Level.INFO, "Do infeasibility propagation!");
            HashSet<CFAEdge> lTargetEdges = new HashSet<>();
            ClusteredElementaryCoveragePattern lClusteredPattern =
                (ClusteredElementaryCoveragePattern) pGoal.getPattern();
            ListIterator<ClusteredElementaryCoveragePattern> lRemainingPatterns =
                lClusteredPattern.getRemainingElementsInCluster();
            int lTmpIndex = goalIndex - 1; // caution lIndex starts at 0
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
            lTmpIndex = goalIndex - 1;
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
      }
      else {
        throw new RuntimeException("We need a last state to determine the feasibility of the test goal!");
      }
    }
    if (analysisWasSound) {
      return ReachabilityAnalysisResult.SOUND;
    } else {
      return ReachabilityAnalysisResult.UNSOUND;
    }
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
          }
          else {
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

  /**
   * Constructs a test goal from the given pattern.
   * @param pGoalPattern
   * @param pAlphaLabel
   * @param pInverseAlphaLabel
   * @param pOmegaLabel
   * @param pUseAutomatonOptimization
   * @return
   */
  private Goal constructGoal(int pIndex, ElementaryCoveragePattern pGoalPattern, GuardedEdgeLabel pAlphaLabel,
      GuardedEdgeLabel pInverseAlphaLabel, GuardedLabel pOmegaLabel, boolean pUseAutomatonOptimization,
      Region pPresenceCondition) {

    NondeterministicFiniteAutomaton<GuardedEdgeLabel> automaton =
        ToGuardedAutomatonTranslator.toAutomaton(pGoalPattern, pAlphaLabel, pInverseAlphaLabel, pOmegaLabel);
    automaton = FQLSpecificationUtil.optimizeAutomaton(automaton, pUseAutomatonOptimization);

    Goal lGoal = new Goal(pIndex, pGoalPattern, automaton, pPresenceCondition);

    return lGoal;
  }

  @Override
  public PredicatePrecision getPrecision() {
    return reusedPrecision;
  }

  @Override
  public void setPrecision(PredicatePrecision pNewPrec) {
    reusedPrecision = pNewPrec;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(this);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
    // TODO Print information about feasible, infeasible, timed-out, and unprocessed test goals.

    if (testsuiteFile != null) {
      // write generated test suite and mapping to file system
      try (Writer writer =
          new BufferedWriter(new OutputStreamWriter(new FileOutputStream(testsuiteFile.toFile()), "utf-8"))) {
        writer.write(testsuite.toString());
        writer.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    int numberOfTimedoutTestGoals =
        statistics_numberOfProcessedTestGoals
            - (testsuite.getNumberOfFeasibleTestGoals() + testsuite.getNumberOfInfeasibleTestGoals());

    pOut.println("Number of test goals:                              " + statistics_numberOfTestGoals);
    pOut.println("Number of processed test goals:                    " + statistics_numberOfProcessedTestGoals);
    pOut.println("Number of feasible test goals:                     " + testsuite.getNumberOfFeasibleTestGoals());
    pOut.println("Number of infeasible test goals:                   " + testsuite.getNumberOfInfeasibleTestGoals());
    //pOut.println("Number of timedout test goals:                     " + testsuite.getNumberOfTimedoutTestGoals());
    pOut.println("Number of timedout test goals:                     " + numberOfTimedoutTestGoals);

    if (statistics_numberOfProcessedTestGoals > testsuite.getNumberOfFeasibleTestGoals()
        + testsuite.getNumberOfInfeasibleTestGoals() + testsuite.getNumberOfTimedoutTestGoals()) {
      pOut.println("Timeout occured during processing of a test goal!");
    }
  }

  @Override
  public String getName() {
    return "TigerAlgorithm";
  }

}
