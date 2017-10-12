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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.StartupConfig;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.FQLSpecificationUtil;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.PredefinedCoverageCriteria;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.FQLSpecification;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.SingletonECPEdgeSet;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.InverseGuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.IncrementalCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCase;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.ThreeValuedAnswer;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.WorkerRunnable;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.WorklistEntryComparator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.Wrapper;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
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
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.ErrorPathShrinker;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

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
      name = "testsuiteFile",
      description = "Filename for output of generated test suite")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path testsuiteFile = Paths.get("testsuite.txt");

  @Option(
      secure = true,
      name = "checkCoverage",
      description = "Checks whether a test case for one goal covers another test goal")
  private boolean checkCoverage = true;

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
      name = "inputInterface",
      description = "List of input variables: v1,v2,v3...")
  String inputInterface = "";

  @Option(
      secure = true,
      name = "outputInterface",
      description = "List of output variables: v1,v2,v3...")
  String outputInterface = "";

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
  private Set<String> inputVariables;
  private Set<String> outputVariables;

  enum ReachabilityAnalysisResult {
    SOUND,
    UNSOUND,
    TIMEDOUT
  }

  public TigerAlgorithm(LogManager pLogger, CFA pCfa, Configuration pConfig,
      ConfigurableProgramAnalysis pCpa, ShutdownNotifier pShutdownNotifier,
      String programDenotation, @Nullable final Specification stats)
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
    testsuite = new TestSuite(null);
    inputVariables = new TreeSet<>();
    for (String variable : inputInterface.split(",")) {
      inputVariables.add(variable.trim());
    }
    outputVariables = new TreeSet<>();
    for (String variable : outputInterface.split(",")) {
      outputVariables.add(variable.trim());
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
    LinkedList<ElementaryCoveragePattern> goalPatterns;
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

    /* for (Goal goal : pGoalsToCover) {
      try {
        runReachabilityAnalysis(goal, goal.getIndex());
      } catch (InvalidConfigurationException e) {
        logger.log(Level.SEVERE, "Failed to run reachability analysis!");
      }
    }*/

    // (iii) do test generation for test goals ...
    boolean wasSound = true;
    if (!testGeneration(pGoalsToCover)) {
      logger.logf(Level.WARNING, "Test generation contained unsound reachability analysis runs!");
      wasSound = false;
    }

    try (Writer writer =
        new BufferedWriter(new OutputStreamWriter(
            new FileOutputStream(
                "output/testsuite.txt"),
            "utf-8"))) {
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

  @SuppressWarnings("unchecked")
  private boolean testGeneration(LinkedList<Goal> pGoalsToCover)
      throws CPAException, InterruptedException {
    boolean wasSound = true;
    boolean retry = false;
    int numberOfTestGoals = pGoalsToCover.size();
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

            set.addAll((Collection<? extends Entry<Integer, Pair<Goal, Region>>>) testsuite
                .getTimedOutGoals().entrySet());
          } else {
            set = new LinkedList<>();
            set.addAll((Collection<? extends Entry<Integer, Pair<Goal, Region>>>) testsuite
                .getTimedOutGoals().entrySet());
          }

          pGoalsToCover.clear();
          for (Entry<Integer, Pair<Goal, Region>> entry : set) {
            pGoalsToCover.add(entry.getValue().getFirst());
          }
          testsuite.getTimedOutGoals().size();
          testsuite.getTimedOutGoals().clear();
        }
      }
      while (!pGoalsToCover.isEmpty()) {
        Goal goal = pGoalsToCover.poll();
        int goalIndex = goal.getIndex();

        logger.logf(Level.INFO, "Processing test goal %d of %d.", goalIndex, numberOfTestGoals);

        ReachabilityAnalysisResult result =
            runReachabilityAnalysis(goal, goalIndex, pGoalsToCover);

        if (result.equals(ReachabilityAnalysisResult.UNSOUND)) {
          logger.logf(Level.WARNING, "Analysis run was unsound!");
          wasSound = false;
        }
        if (result.equals(ReachabilityAnalysisResult.TIMEDOUT)) {
          break;
        }
      }

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

  @SuppressWarnings("unused")
  private boolean isCovered(int goalIndex, Goal lGoal) {
    @SuppressWarnings("unused")
    Region remainingPCforGoalCoverage = lGoal.getPresenceCondition();
    boolean isFullyCovered = false;
    for (TestCase testcase : testsuite.getTestCases()) {
      ThreeValuedAnswer isCovered =
          TigerAlgorithm.accepts(lGoal, testcase);
      if (isCovered.equals(ThreeValuedAnswer.UNKNOWN)) {
        logger.logf(Level.WARNING,
            "Coverage check for goal %d could not be performed in a precise way!", goalIndex);
        continue;
      } else if (isCovered.equals(ThreeValuedAnswer.REJECT)) {
        continue;
      }

      // test goal is already covered by an existing test case
      /*if (useTigerAlgorithm_with_pc) {
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
      }*/
    }

    return isFullyCovered;
  }

  private static ThreeValuedAnswer accepts(Goal pGoal, TestCase pTestCase) {
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton = pGoal.getAutomaton();
    Set<NondeterministicFiniteAutomaton.State> lCurrentStates = new HashSet<>();
    Set<NondeterministicFiniteAutomaton.State> lNextStates = new HashSet<>();

    lCurrentStates.add(lAutomaton.getInitialState());

    boolean lHasPredicates = false;

    for (CFAEdge lCFAEdge : pTestCase.getPath()) {
      for (NondeterministicFiniteAutomaton.State lCurrentState : lCurrentStates) {
        // Automaton accepts as soon as it sees a final state (implicit self-loop)
        if (lAutomaton.getFinalStates()
            .contains(lCurrentState)) { return ThreeValuedAnswer.ACCEPT; }

        for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOutgoingEdge : lAutomaton
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
      if (lAutomaton.getFinalStates().contains(lCurrentState)) { return ThreeValuedAnswer.ACCEPT; }
    }

    if (lHasPredicates) {
      return ThreeValuedAnswer.UNKNOWN;
    } else {
      return ThreeValuedAnswer.REJECT;
    }
  }

  private Goal constructGoal(int pIndex, ElementaryCoveragePattern pGoalPattern,
      GuardedEdgeLabel pAlphaLabel, InverseGuardedEdgeLabel pInverseAlphaLabel,
      GuardedEdgeLabel pOmegaLabel, boolean pUseAutomatonOptimization, Region pPresenceCondition) {
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> automaton =
        ToGuardedAutomatonTranslator.toAutomaton(pGoalPattern, pAlphaLabel, pInverseAlphaLabel,
            pOmegaLabel);
    automaton = FQLSpecificationUtil.optimizeAutomaton(automaton, pUseAutomatonOptimization);

    Goal lGoal = new Goal(pIndex, pGoalPattern, automaton, pPresenceCondition);

    return lGoal;
  }

  private LinkedList<ElementaryCoveragePattern> extractTestGoalPatterns(
      FQLSpecification pFqlSpecification) {
    logger.logf(Level.INFO, "Extracting test goals.");


    // TODO check for (temporarily) unsupported features

    // TODO enable use of infeasibility propagation


    IncrementalCoverageSpecificationTranslator lTranslator =
        new IncrementalCoverageSpecificationTranslator(
            mCoverageSpecificationTranslator.mPathPatternTranslator);

    statistics_numberOfTestGoals =
        lTranslator.getNumberOfTestGoals(pFqlSpecification.getCoverageSpecification());
    logger.logf(Level.INFO, "Number of test goals: %d", statistics_numberOfTestGoals);

    Iterator<ElementaryCoveragePattern> lGoalIterator =
        lTranslator.translate(pFqlSpecification.getCoverageSpecification());
    LinkedList<ElementaryCoveragePattern> lGoalPatterns = new LinkedList<>();

    for (int lGoalIndex = 0; lGoalIndex < statistics_numberOfTestGoals; lGoalIndex++) {
      lGoalPatterns.add(lGoalIterator.next());
    }

    return lGoalPatterns;
  }

  private ReachabilityAnalysisResult runReachabilityAnalysis(Goal pGoal, int goalIndex,
      LinkedList<Goal> pGoalsToCover)
      throws CPAException, InterruptedException {

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
          if (cexi.isPresent()) {
            logger.log(Level.INFO, "cexi is Present");
          }
          //...........

          @SuppressWarnings("unused")
          Map<ARGState, CounterexampleInfo> counterexamples = lARTCPA.getCounterexamples();

          if (!cexi.isPresent()/*counterexamples.isEmpty()*/) {

            TestCase testcase = handleUnavailableCounterexample(criticalEdge, lastState);

            testsuite.addTestCase(testcase, pGoal);
          } else {
            // test goal is feasible
            logger.logf(Level.INFO, "Counterexample is available.");
            CounterexampleInfo cex = cexi.get();
            if (cex.isSpurious()) {
              logger.logf(Level.WARNING, "Counterexample is spurious!");
            } else {
              TestCase testcase = createTestcase(cex, null);
              testsuite.addTestCase(testcase, pGoal);
              if (checkCoverage) {
                removeAllGoalsCoveredByTestcase(pGoalsToCover, testcase);
              }
            }

          }
          /* assert counterexamples.size() == 1;

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
          }*/

        } else {
          logger.logf(Level.INFO, "Test goal infeasible.");
          testsuite.addInfeasibleGoal(pGoal, null);
        }
      } else {
        throw new RuntimeException(
            "We need a last state to determine the feasibility of the test goal!");
      }
    }

    if (analysisWasSound_hasTimedOut.getFirst() == true) {
      return ReachabilityAnalysisResult.SOUND;
    } else {
      return ReachabilityAnalysisResult.UNSOUND;
    }
  }

  private TestCase createTestcase(final CounterexampleInfo cex, final Region pPresenceCondition) {
    Map<String, BigInteger> inputValues = extractInputValues(cex);
    Map<String, BigInteger> outputValus = extractOutputValues(cex);
    // calcualte shrinked error path
    List<CFAEdge> shrinkedErrorPath =
        new ErrorPathShrinker().shrinkErrorPath(cex.getTargetPath());
    TestCase testcase =
        new TestCase(inputValues, outputValus, cex.getTargetPath().asEdgesList(), shrinkedErrorPath,
            null);
    return testcase;
  }

  private Map<String, BigInteger> extractOutputValues(CounterexampleInfo cex) {
    Map<String, BigInteger> variableToValueAssignments = new LinkedHashMap<>();
    CFAPathWithAssumptions path = cex.getCFAPathWithAssignments();
    int index = 0;
    for (CFAEdgeWithAssumptions edge : path) {
      if (edge.getCFAEdge() instanceof CFunctionCallEdge) {
        CFunctionCallEdge fEdge = (CFunctionCallEdge) edge.getCFAEdge();
        if (fEdge.getRawAST().get() instanceof CFunctionCallAssignmentStatement) {
          CFunctionCallAssignmentStatement functionCall =
              (CFunctionCallAssignmentStatement) fEdge.getRawAST().get();
          /*boolean setNewTestStep = createAndAddVariableAssignment(functionCall, index, path,
              AssignmentType.OUTPUT, outputVariables, outputAssignments, "", false, false);*/
          CLeftHandSide cLeft = functionCall.getLeftHandSide();
          CIdExpression cld = cLeft instanceof CIdExpression ? (CIdExpression) cLeft : null;

          if (cld != null && outputVariables.contains(cld.getName())) {

            outputVariables.remove(cld.getName());

            BigInteger value;

            value = getVariableValueFromFunctionCall(index, path);

            if (value != null) {
              variableToValueAssignments.put(cld.getName(), value);
            }
          }
        }
      }
      index++;
    }
    return variableToValueAssignments;
  }

  private Map<String, BigInteger> extractInputValues(CounterexampleInfo cex) {
    Map<String, BigInteger> variableToValueAssignments = new LinkedHashMap<>();
    Set<String> tempInputs = new LinkedHashSet<>(inputVariables);
    CFAPathWithAssumptions path = cex.getCFAPathWithAssignments();
    for (CFAEdgeWithAssumptions edge : path) {
      Collection<AExpressionStatement> expStmts = edge.getExpStmts();
      for (AExpressionStatement expStmt : expStmts) {
        if (expStmt.getExpression() instanceof CBinaryExpression) {
          CBinaryExpression exp = (CBinaryExpression) expStmt.getExpression();
          if (tempInputs.contains(exp.getOperand1().toString())
              && (edge.getCFAEdge().getCode().contains("__VERIFIER_nondet_"))) {
            String variableName = exp.getOperand1().toString();
            tempInputs.remove(variableName);
            variableName = "relevant: " + variableName;
            BigInteger value = new BigInteger(exp.getOperand2().toString());
            variableToValueAssignments.put(variableName, value);
          }
        }
      }
    }

    int index = 0;
    for (CFAEdgeWithAssumptions edge : path) {
      if (!edge.getCFAEdge().getCode().contains("__VERIFIER_nondet_")) {
        if (edge.getCFAEdge() instanceof CFunctionCallEdge) {
          CFunctionCallEdge fEdge = (CFunctionCallEdge) edge.getCFAEdge();
          if (fEdge.getRawAST().get() instanceof CFunctionCallAssignmentStatement) {
            CFunctionCallAssignmentStatement functionCall =
                (CFunctionCallAssignmentStatement) fEdge.getRawAST().get();
            /*boolean setNewTestStep =
                createAndAddVariableAssignment(functionCall, index, path, AssignmentType.INPUT,
                    remaining_inputVariables, inputAssignments, "relevant: ", false, true);*/
            CLeftHandSide cLeft = functionCall.getLeftHandSide();
            CIdExpression cld = cLeft instanceof CIdExpression ? (CIdExpression) cLeft : null;

            if (cld != null && tempInputs.contains(cld.getName())) {

              tempInputs.remove(cld.getName());

              BigInteger value;

              value = getVariableValueFromFunctionCall(index, path);

              if (value != null) {
                variableToValueAssignments.put("relevant: " + cld.getName(), value);
              }
            }
          }

        }
        index++;
        continue;
      }
      if (edge.getCFAEdge() instanceof CStatementEdge) {
        CStatementEdge statementEdge = (CStatementEdge) edge.getCFAEdge();
        if (statementEdge.getRawAST().get() instanceof CFunctionCallAssignmentStatement) {
          CFunctionCallAssignmentStatement functionCall =
              (CFunctionCallAssignmentStatement) statementEdge.getRawAST().get();
          /* boolean setNewTestStep =
              createAndAddVariableAssignment(functionCall, index, path, AssignmentType.INPUT,
                  remaining_inputVariables, inputAssignments, "irrelevant: ", true, false);*/
          CLeftHandSide cLeft = functionCall.getLeftHandSide();
          CIdExpression cld = cLeft instanceof CIdExpression ? (CIdExpression) cLeft : null;

          if (cld != null && tempInputs.contains(cld.getName())) {

            tempInputs.remove(cld.getName());

            BigInteger value;

            value = new BigInteger("0");
            variableToValueAssignments.put("irrelevant: " + cld.getName(), value);
          }
        }
      }
      index++;
    }
    return variableToValueAssignments;
  }

  private BigInteger getVariableValueFromFunctionCall(int index, CFAPathWithAssumptions path) {
    Set<AExpressionStatement> expStmts = new HashSet<>();
    int nesting = -1;
    for (int i = index; i < path.size(); i++) {
      CFAEdgeWithAssumptions edge = path.get(i);
      CFAEdge cfaEdge = edge.getCFAEdge();
      expStmts.addAll(edge.getExpStmts());
      if (cfaEdge instanceof CFunctionCallEdge) {
        nesting++;
      }
      if (cfaEdge instanceof CReturnStatementEdge) {
        if (nesting == 0) {
          CReturnStatementEdge returnEdge = (CReturnStatementEdge) cfaEdge;
          CReturnStatement returnStatement = returnEdge.getRawAST().get();
          CAssignment assignment = returnStatement.asAssignment().get();
          CRightHandSide rightHand = assignment.getRightHandSide();
          if (rightHand instanceof CIntegerLiteralExpression) {
            CIntegerLiteralExpression rightSide =
                (CIntegerLiteralExpression) rightHand;
            return rightSide.getValue();
          }
          if (assignment instanceof CExpressionAssignmentStatement) { return getValueFromComment(
              edge); }
        }
        nesting--;
      }
    }
    return null;
  }

  private BigInteger getValueFromComment(CFAEdgeWithAssumptions edge) {
    String comment = edge.getComment().replaceAll("\\s+", "");
    String[] resArray = comment.split("=");
    return new BigInteger(resArray[resArray.length - 1]);
  }

  private void removeAllGoalsCoveredByTestcase(LinkedList<Goal> pGoalsToCover, TestCase pTestcase) {
    LinkedList<Goal> temp = new LinkedList<>(pGoalsToCover);
    for (Goal goal : temp) {
      ThreeValuedAnswer answer = TigerAlgorithm.accepts(goal, pTestcase);
      if (answer.equals(ThreeValuedAnswer.ACCEPT)) {
        pGoalsToCover.remove(goal);
        testsuite.updateTestcaseToGoalMapping(pTestcase, goal);
        logger.log(Level.INFO, "Goal " + goal.getName() + " is allready covered by testcase "
            + pTestcase.getId() + " and is removed from goal list");
      }
    }
  }

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

    Map<String, BigInteger> inputValues = new LinkedHashMap<>();
    Map<String, BigInteger> outputValues = new LinkedHashMap<>();

    return new TestCase(inputValues, outputValues, trace, shrinkedErrorPath,
        null);
  }
}
