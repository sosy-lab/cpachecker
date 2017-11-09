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
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Pair;
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
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmWithResult;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.StartupConfig;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.FQLSpecificationUtil;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.FQLSpecification;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCase;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestGoalUtils;
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
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

@Options(prefix = "tiger")
public class TigerAlgorithm implements AlgorithmWithResult {




  enum TimeoutStrategy {
    SKIP_AFTER_TIMEOUT,
    RETRY_AFTER_TIMEOUT
  }
  public static String originalMainFunction = null;

  private FQLSpecification fqlSpecification;
  private final LogManager logger;
  private final CFA cfa;
  private ConfigurableProgramAnalysis cpa;
  private Wrapper wrapper;
  private final Configuration config;
  private ReachedSet outsideReachedSet = null;
  private ReachedSet reachedSet = null;
  private StartupConfig startupConfig;
  private String programDenotation;
  private Specification stats;
  private TestSuite testsuite;
  private Values values;

  private int currentTestCaseID;

  private TigerAlgorithmConfiguration tigerConfig;
  private TestGoalUtils testGoalUtils;

  enum ReachabilityAnalysisResult {
    SOUND,
    UNSOUND,
    TIMEDOUT
  }


  public TigerAlgorithm(LogManager pLogger, CFA pCfa, Configuration pConfig,
      ConfigurableProgramAnalysis pCpa, ShutdownNotifier pShutdownNotifier,
      String programDenotation, @Nullable final Specification stats)
      throws InvalidConfigurationException {
    tigerConfig = new TigerAlgorithmConfiguration(pConfig);
    cfa = pCfa;
    cpa = pCpa;
    startupConfig = new StartupConfig(pConfig, pLogger, pShutdownNotifier);
    //startupConfig.getConfig().inject(this);
    logger = pLogger;
    assert TigerAlgorithm.originalMainFunction != null;
    wrapper = new Wrapper(pCfa, TigerAlgorithm.originalMainFunction);
    testGoalUtils = new TestGoalUtils(logger, wrapper, pCfa, tigerConfig.shouldOptimizeGoalAutomata(), TigerAlgorithm.originalMainFunction);
    config = pConfig;
    config.inject(this);
    logger.logf(Level.INFO, "FQL query string: %s", tigerConfig.getFqlQuery());
    fqlSpecification = FQLSpecificationUtil.getFQLSpecification(tigerConfig.getFqlQuery());
    logger.logf(Level.INFO, "FQL query: %s", fqlSpecification.toString());
    this.programDenotation = programDenotation;
    this.stats = stats;
    values = new Values(tigerConfig.getInputInterface(), tigerConfig.getOutputInterface());
    currentTestCaseID = 0;

  }

  @Override
  public AlgorithmResult getResult() {
    return testsuite;
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

    goalPatterns = testGoalUtils.extractTestGoalPatterns(fqlSpecification);

    for (int i = 0; i < goalPatterns.size(); i++) {
      pTestGoalPatterns.add(new Pair<>(goalPatterns.get(i), (Region) null));
    }

    int goalIndex = 1;
    LinkedList<Goal> pGoalsToCover = new LinkedList<>();
    for (Pair<ElementaryCoveragePattern, Region> pair : pTestGoalPatterns) {
      Goal lGoal =
          testGoalUtils.constructGoal(goalIndex, pair.getFirst(), pair.getSecond());
      logger.log(Level.INFO, lGoal.getName());
      pGoalsToCover.add(lGoal);
      goalIndex++;
    }

    testsuite = new TestSuite(null, pGoalsToCover);
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

        if (tigerConfig.getTimeoutIncrement() > 0) {
          long oldCPUTimeLimitPerGoal = tigerConfig.getCpuTimelimitPerGoal();
          tigerConfig.increaseCpuTimelimitPerGoal(tigerConfig.getTimeoutIncrement());
          //tigerConfig.getCpuTimelimitPerGoal() += tigerConfig..getTimeoutIncrement();
          logger.logf(Level.INFO, "Incremented timeout from %d to %d seconds.",
              oldCPUTimeLimitPerGoal,
              tigerConfig.getCpuTimelimitPerGoal());

          Collection<Entry<Integer, Pair<Goal, Region>>> set;
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
          testcase.coversGoal(lGoal);
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
    Algorithm algorithm = buildAlgorithm(algNotifier, lARTCPA);
    Pair<Boolean, Boolean> analysisWasSound_hasTimedOut = runAlgorithm(algorithm, algNotifier);


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
              if (tigerConfig.shouldCheckCoverage()) {
                checkGoalCoverage(pGoalsToCover, testcase, true);
                //removeAllGoalsCoveredByTestcase(pGoalsToCover, testcase);
              }
              if (tigerConfig.useAllGoalsPerTestcase()) {
                List<Goal> allGoals = testsuite.getIncludedTestGoals();
                checkGoalCoverage(allGoals, testcase, false);
                //checkGoalCoverageForTestCase(allGoals, testcase);
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
        /*throw new RuntimeException(
            "We need a last state to determine the feasibility of the test goal!");*/
        logger.logf(Level.INFO, "Test goal infeasible.");
        testsuite.addInfeasibleGoal(pGoal, null);
      }
    }

    if (analysisWasSound_hasTimedOut.getFirst() == true) {
      return ReachabilityAnalysisResult.SOUND;
    } else {
      return ReachabilityAnalysisResult.UNSOUND;
    }
  }

  private TestCase createTestcase(final CounterexampleInfo cex, final Region pPresenceCondition) {
    Map<String, BigInteger> inputValues  = values.extractInputValues(cex);
    Map<String, BigInteger> outputValus = values.extractOutputValues(cex);
    // calcualte shrinked error path
    List<CFAEdge> shrinkedErrorPath =
        new ErrorPathShrinker().shrinkErrorPath(cex.getTargetPath());
    TestCase testcase =
        new TestCase(currentTestCaseID, inputValues, outputValus, cex.getTargetPath().asEdgesList(),
            shrinkedErrorPath,
            null);
    currentTestCaseID++;
    return testcase;
  }

  private void checkGoalCoverage(List<Goal> testGoals, TestCase testCase, boolean removeCoveredGoals) {
    for (Goal goal : testCase.getCoveredGoals(testGoals)) {
      testsuite.updateTestcaseToGoalMapping(testCase, goal);
      String log = "Goal " + goal.getName() + " is covered by testcase " + testCase.getId();
      if(removeCoveredGoals) {
        testGoals.remove(goal);
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

  private LinkedList<ConfigurableProgramAnalysis> buildComponentAnalyses( CPAFactory automataFactory) throws CPAException {
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
    return lComponentAnalyses;
  }

  private ARGCPA buildARGCPA(LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses,  Specification goalAutomatonSpecification) {
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

  private ARGCPA buildCPAs(Goal pGoal) throws CPAException {
    Automaton goalAutomaton = pGoal.createControlAutomaton();
    Specification goalAutomatonSpecification =
        Specification.fromAutomata(Lists.newArrayList(goalAutomaton));

    CPAFactory automataFactory = buildAutomataFactory(goalAutomaton);
    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = buildComponentAnalyses(automataFactory);
    return buildARGCPA(lComponentAnalyses, goalAutomatonSpecification);

  }

  private Algorithm buildAlgorithm(ShutdownManager algNotifier, ARGCPA lARTCPA) throws CPAException {
    Algorithm algorithm;
    try {
      Configuration internalConfiguration =
          Configuration.builder().loadFromFile(tigerConfig.getAlgorithmConfigurationFile()).build();

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
    return algorithm;
  }

  private Pair<Boolean, Boolean> runAlgorithm(Algorithm algorithm, ShutdownManager algNotifier) throws CPAEnabledAnalysisPropertyViolationException, CPAException, InterruptedException{
    boolean analysisWasSound = false;
    boolean hasTimedOut = false;

    if (tigerConfig.getCpuTimelimitPerGoal() < 0) {
      // run algorithm without time limit
      analysisWasSound = algorithm.run(reachedSet).isSound();
    } else {
      // run algorithm with time limit
      WorkerRunnable workerRunnable =
          new WorkerRunnable(algorithm, reachedSet, tigerConfig.getCpuTimelimitPerGoal(), algNotifier);

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
    return new Pair<>(analysisWasSound, hasTimedOut);
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

    TestCase result =
        new TestCase(currentTestCaseID, inputValues, outputValues, trace, shrinkedErrorPath,
            null);
    currentTestCaseID++;
    return result;
  }
}
