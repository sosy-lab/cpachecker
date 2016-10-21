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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.MainCPAStatistics;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmWithResult;
import org.sosy_lab.cpachecker.core.algorithm.tgar.TGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.tgar.TGARStatistics;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.PrecisionCallback;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.WorkerRunnable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTransition;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.automaton.MarkingAutomatonBuilder;
import org.sosy_lab.cpachecker.cpa.automaton.PowersetAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.bdd.BDDCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.presence.PresenceConditions;
import org.sosy_lab.cpachecker.util.presence.binary.BinaryPresenceConditionManager;
import org.sosy_lab.cpachecker.util.presence.formula.FormulaPresenceConditionManager;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceConditionManager;
import org.sosy_lab.cpachecker.util.presence.region.RegionPresenceConditionManager;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime.StatCpuTimer;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class TigerAlgorithm
    implements Algorithm, AlgorithmWithResult, PrecisionCallback<PredicatePrecision>, StatisticsProvider {

  enum ReachabilityAnalysisResult {SOUND, UNSOUND, TIMEOUT}

  private class TigerStatistics extends AbstractStatistics {

    final StatCpuTime acceptsTime = new StatCpuTime();
    final StatCpuTime updateTestsuiteByCoverageOfTime = new StatCpuTime();
    final StatCpuTime createTestcaseTime = new StatCpuTime();
    final StatCpuTime addTestToSuiteTime = new StatCpuTime();
    final StatCpuTime runAlgorithmWithLimitTime = new StatCpuTime();
    final StatCpuTime runAlgorithmTime = new StatCpuTime();
    final StatCpuTime initializeAlgorithmTime = new StatCpuTime();
    final StatCpuTime initializeReachedSetTime = new StatCpuTime();
    final StatCpuTime testGenerationTime = new StatCpuTime();
    final StatInt numOfProcessedGoals = new StatInt(StatKind.SUM, "");

    public TigerStatistics() {
      super();
    }

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
      super.printStatistics(pOut, pResult, pReached);
      pOut.append("Time for test generation " + testGenerationTime + "\n");
      pOut.append("  Number of processed test goals " + numOfProcessedGoals.getValueSum() + "\n");
      pOut.append("  Time for initializing the reached set " + initializeReachedSetTime + "\n");
      pOut.append("  Time for initializing the algorithm " + initializeAlgorithmTime + "\n");
      pOut.append("  Time for running the CPA algorithm " + runAlgorithmTime + "\n");
      pOut.append("    Time for running the CPA algorithm with limit " + runAlgorithmWithLimitTime + "\n");
      pOut.append("    Time for adding a test to the suite " + addTestToSuiteTime + "\n");
      pOut.append("      Time for creating a test case " + createTestcaseTime + "\n");
      pOut.append("      Time for updating the test coverage " + updateTestsuiteByCoverageOfTime + "\n");
      pOut.append("        Time for checking acceptance " + acceptsTime + "\n");
    }

  }

  private TigerStatistics tigerStats = new TigerStatistics();

  public static String originalMainFunction = null;

  private final Configuration config;
  private final TigerConfiguration cfg;
  private final LogManager logger;
  final private ShutdownManager mainShutdownManager;
  private final MainCPAStatistics mainStats;
  private final CFA cfa;
  private final TestGeneration tg;

  private ConfigurableProgramAnalysis cpa;
  private TGARStatistics tgarStatistics;

  private ReachedSet reachedSet = null;
  private ReachedSet outsideReachedSet = null;

  private PredicatePrecision reusedPrecision = null;

  private Map<Automaton, Automaton> markingAutomataInstances = Maps.newHashMap();

  private String programDenotation;

  private final ReachedSetFactory reachedSetFactory;

  public TigerAlgorithm(ConfigurableProgramAnalysis pCpa,
      ShutdownManager pShutdownManager, CFA pCfa, Configuration pConfig, LogManager pLogger,
      String pProgramDenotation, ReachedSetFactory pReachedSetFactory, MainCPAStatistics pMainStats)
    throws InvalidConfigurationException {

    reachedSetFactory = pReachedSetFactory;
    programDenotation = pProgramDenotation;
    mainStats = pMainStats;
    mainShutdownManager = pShutdownManager;
    logger = pLogger;

    config = pConfig;
    cpa = pCpa;
    cfa = pCfa;

    tgarStatistics = new TGARStatistics(pLogger);
    cfg = new TigerConfiguration(pConfig);
    tg = new TestGeneration(cfg, pCfa, pLogger);
  }

  @SuppressWarnings("unused")
  private PresenceConditionManager pcm() {
    return PresenceConditions.manager();
  }

  @Override
  public void setPrecision(PredicatePrecision pNewPrec) {
    reusedPrecision = pNewPrec;
  }

  @Override
  public PredicatePrecision getPrecision() {
    return reusedPrecision;
  }

  @Override
  public AlgorithmResult getResult() {
    return tg.getTestSuite();
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // we empty pReachedSet to stop complaints of an incomplete analysis
    // Problem: pReachedSet does not match the internal CPA structure!
    logger.logf(Level.INFO, "We will not use the provided reached set since it violates the internal structure of Tiger's CPAs");
    logger.logf(Level.INFO, "We empty pReachedSet to stop complaints of an incomplete analysis");

    outsideReachedSet = pReachedSet;
    outsideReachedSet.clear();

    tg.signalGenerationStart();

    // (iii) do test generation for test goals ...
    boolean wasSound = true;
    try {
      if (!testGeneration(tg.getTestSuite().getGoals())) {
        logger.logf(Level.WARNING, "Test generation contained unsound reachability analysis runs!");
        wasSound = false;
      }
    } catch (InvalidConfigurationException e1) {
      throw new CPAException("Invalid configuration!", e1);
    }

    // Write generated test suite and mapping to file system
    tg.dumpTestSuite();

    if (wasSound) {
      return AlgorithmStatus.SOUND_AND_PRECISE;
    } else {
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
    }
  }

  private boolean testGeneration(ImmutableSet<Goal> pGoalsToCover)
      throws CPAException, InterruptedException, InvalidConfigurationException {

    try (StatCpuTimer t = tigerStats.testGenerationTime.start()) {

      boolean wasSound = true;
      while (!pGoalsToCover.isEmpty()) {

        // Next partition of goals...
        Set<Goal> goalsToBeProcessed = tg.nextTestGoalSet(pGoalsToCover);
        tigerStats.numOfProcessedGoals.setNextValue(goalsToBeProcessed.size());
        tg.removeGoalsFromRemaining(goalsToBeProcessed);
        logGoals(goalsToBeProcessed);

//         if (cfg.useTigerAlgorithm_with_pc) {
//           /* force that a new reachedSet is computed when first starting on a new TestGoal with initial PC TRUE.
//            * This enforces that no very constrained ARG is reused when computing a new ARG for a new testgoal with broad pc (TRUE).
//            * This strategy allows us to set option tiger.reuseARG=true such that ARG is reused in testgoals (pcs get only more specific).
//            * Keyword: overapproximation
//            */
//           //assert false;
//           reachedSet = null;
//         }

        ReachabilityAnalysisResult result = runReachabilityAnalysis(goalsToBeProcessed);
        if (result.equals(ReachabilityAnalysisResult.UNSOUND)) {
          logger.logf(Level.WARNING, "Analysis run was unsound!");
          wasSound = false;
        }
      }

      // reprocess timed-out goals
      if (tg.getTimedOutGoals().isEmpty()) {
        logger.logf(Level.INFO, "There were no timed out goals.");
      } else {
        logger.logf(Level.INFO, "There were timed out goals but retry after timeout strategy is disabled.");
      }

      return wasSound;
    }
  }

  private void logGoals(Set<Goal> pGoalsToBeProcessed) {
    String logString = "Processing test goals ";
    for (Goal g : pGoalsToBeProcessed) {
      logString += g.getIndex() + " (" + tg.getTestSuite().getTestGoalLabel(g) + "), ";
    }
    logString = logString.substring(0, logString.length() - 2);

    logger.logf(Level.FINE, "%s of %d.", logString, tg.getTestSuite().getTotalNumberOfGoals());
  }

  private PresenceConditionManager createPresenceConditionManager(ConfigurableProgramAnalysis pCpa) {

    PredicateCPA predCpa = CPAs.retrieveCPA(pCpa, PredicateCPA.class);
    BDDCPA bddCpa = CPAs.retrieveCPA(pCpa, BDDCPA.class);

    if (predCpa != null) {
      return new FormulaPresenceConditionManager(predCpa.getPathFormulaManager(), predCpa.getSolver());
    } else if (bddCpa != null) {
      return new RegionPresenceConditionManager(bddCpa.getManager());
    } else {
      return new BinaryPresenceConditionManager();
    }
  }

  private ReachabilityAnalysisResult runReachabilityAnalysis(Set<Goal> pTestGoalsToBeProcessed)
      throws CPAException, InterruptedException, InvalidConfigurationException {

    ARGCPA cpa = composeCPA(pTestGoalsToBeProcessed, false);
    PresenceConditionManager pcm = createPresenceConditionManager(cpa);
    GlobalInfo.getInstance().setUpInfoFromCPA(cpa, pcm);

    Preconditions.checkState(cpa.getWrappedCPAs().get(0) instanceof CompositeCPA,
        "CPAcheckers automata should be used! The assumption is that the first component is the automata for the current goal!");

    initializeReachedSet(cpa);

    ShutdownManager shutdownManager = ShutdownManager.createWithParent(mainShutdownManager.getNotifier());
    Algorithm algorithm = initializeAlgorithm(cpa, shutdownManager);

    Preconditions.checkState(algorithm instanceof TGARAlgorithm);
    TGARAlgorithm tgarAlgorithm = (TGARAlgorithm) algorithm;

    return runAlgorithm(pTestGoalsToBeProcessed, shutdownManager, tgarAlgorithm);
  }

  private ReachabilityAnalysisResult runAlgorithm(final Set<Goal> pTestGoalsToBeProcessed,
      final ShutdownManager pShutdownNotifier, final TGARAlgorithm pAlgorithm)
      throws CPAException, InterruptedException {

    try (StatCpuTimer t = tigerStats.runAlgorithmTime.start()) {

      ReachabilityAnalysisResult algorithmStatus;

      do {
        algorithmStatus = runAlgorithmWithLimit(pShutdownNotifier, pAlgorithm, pTestGoalsToBeProcessed.size());

      } while ((reachedSet.hasWaitingState()
          && !tg.getTestSuite().areGoalsCovered(pTestGoalsToBeProcessed))
          && (algorithmStatus != ReachabilityAnalysisResult.TIMEOUT));

      if (algorithmStatus == ReachabilityAnalysisResult.TIMEOUT) {
        logger.logf(Level.FINE, "Test goal timed out!");
        tg.getTestSuite().setGoalsTimedout(pTestGoalsToBeProcessed);
      } else {
        // set test goals infeasible
        for (Goal goal : pTestGoalsToBeProcessed) {
          if (!tg.getTestSuite().isGoalCovered(goal)) {
            tg.handleInfeasibleTestGoal(goal);
          }
        }
      }

      return algorithmStatus;
    }
  }

  private ReachabilityAnalysisResult runAlgorithmWithLimit(final ShutdownManager algNotifier,
      final Algorithm algorithm, int numberOfGoals)
    throws CPAException, InterruptedException {

    try (StatCpuTimer t = tigerStats.runAlgorithmWithLimitTime.start()) {
      ReachabilityAnalysisResult status;
      if (cfg.cpuTimelimitPerGoal < 0) {
        // run algorithm without time limit
        if (algorithm.run(reachedSet).isSound()) {
          status = ReachabilityAnalysisResult.SOUND;
        } else {
          status = ReachabilityAnalysisResult.UNSOUND;
        }
      } else {

        long timeout = cfg.cpuTimelimitPerGoal;
        // calculate the timeout
        if (cfg.useDynamicTimeouts) {
          if (cfg.numberOfTestGoalsPerRun <= 0) {
            timeout = tg.getTestSuite().getTotalNumberOfGoals() * cfg.cpuTimelimitPerGoal;
          } else {
            timeout = numberOfGoals * cfg.cpuTimelimitPerGoal;
          }
        }

        // run algorithm with time limit
        WorkerRunnable workerRunnable = new WorkerRunnable(algorithm, reachedSet, timeout, algNotifier);

        Thread workerThread = new Thread(workerRunnable);

        workerThread.start();
        workerThread.join();

        if (workerRunnable.throwableWasCaught()) {
          // TODO: handle exception
          status = ReachabilityAnalysisResult.UNSOUND;
        } else {
          if (workerRunnable.analysisWasSound()) {
            status = ReachabilityAnalysisResult.SOUND;
          } else {
            status = ReachabilityAnalysisResult.UNSOUND;
          }

          if (workerRunnable.hasTimeout()) {
            status = ReachabilityAnalysisResult.TIMEOUT;
          }
        }
      }
      return status;
    }
  }

  private Algorithm initializeAlgorithm(ARGCPA lARTCPA, ShutdownManager algNotifier) throws CPAException {

    try (StatCpuTimer t = tigerStats.initializeAlgorithmTime.start()) {
      Algorithm algorithm;
      try {
        Configuration internalConfiguration = Configuration.builder().loadFromFile(cfg.algorithmConfigurationFile).build();

        CoreComponentsFactory coreFactory = new CoreComponentsFactory(internalConfiguration, logger, algNotifier.getNotifier());

        algorithm = coreFactory.createAlgorithm(lARTCPA, programDenotation, cfa, mainStats);

        Preconditions.checkState(algorithm instanceof TGARAlgorithm, "Only TGAR supported!");
        TGARAlgorithm tgar = (TGARAlgorithm) algorithm;
        tgar.setStats(tgarStatistics);

      } catch (IOException | InvalidConfigurationException e) {
        throw new RuntimeException(e);
      }
      return algorithm;
    }
  }

  private void initializeReachedSet(ARGCPA pArgCPA) {
    try (StatCpuTimer t = tigerStats.initializeReachedSetTime.start()) {
      // Create a new set 'reached' using the responsible factory.
      if (reachedSet != null) {
        reachedSet.clear();
      }
      reachedSet = reachedSetFactory.create();

      AbstractState initialState = pArgCPA.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
      Precision initialPrec = pArgCPA.getInitialPrecision(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());

      reachedSet.add(initialState, initialPrec);
      outsideReachedSet.add(initialState, initialPrec);
    }
  }

  private void dumpAutomaton(Automaton pA) {
    if (cfg.dumpGoalAutomataTo == null) {
      return;
    }

    try (Writer w = MoreFiles.openOutputFile(cfg.dumpGoalAutomataTo.getPath(pA.getName()), Charset.defaultCharset())) {
      pA.writeDotFile(w);
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
    }
  }

  /**
   * Check some properties of the automaton to ensure that it works as expected.
   *
   * @param pAutomaton Test goal automaton to be checked.
   */
  private static void checkAutomaton(Automaton pAutomaton) {
    for (AutomatonInternalState q: pAutomaton.getStates()) {
      if (!q.isNonDetState()) {
        Set<Pair<AutomatonBoolExpr, ImmutableList<AStatement>>> distinct = Sets.newHashSet();
        // No similar triggers!
        for (AutomatonTransition t: q.getTransitions()) {
          Pair<AutomatonBoolExpr, ImmutableList<AStatement>> key = Pair.of(t.getTrigger(), t.getAssumptions());
          if (!distinct.add(key)) {
            throw new RuntimeException("Transition not unique on MATCH-FIRST state: " + t);
          }
        }
      }
    }
  }

  private ARGCPA composeCPA(Set<Goal> pGoalsToBeProcessed, boolean addBDDToHandleFeatures)
      throws CPAException, InvalidConfigurationException {

    Preconditions.checkArgument(cpa instanceof ARGCPA,
        "Tiger: Only support for ARGCPA implemented for CPA composition!");
    ARGCPA oldArgCPA = (ARGCPA) cpa;

    List<Automaton> componentAutomata = Lists.newArrayList();
    {
      List<Automaton> goalAutomata = Lists.newArrayList();

      for (Goal goal : pGoalsToBeProcessed) {
        Automaton a = goal.createControlAutomaton();
        if (cfg.useMarkingAutomata) {
          final Automaton markingAutomata;
          if (markingAutomataInstances.containsKey(a)) {
            markingAutomata = markingAutomataInstances.get(a);
          } else {
            markingAutomata = MarkingAutomatonBuilder.build(a);
            markingAutomataInstances.put(a, markingAutomata);
          }
          a = markingAutomata;
        }

        goalAutomata.add(a);
        dumpAutomaton(a);
        checkAutomaton(a);
      }

      componentAutomata.addAll(goalAutomata);
    }

    logger.logf(Level.INFO, "Analyzing %d test goals with %d observer automata.",
        pGoalsToBeProcessed.size(),
        componentAutomata.size());

    List<ConfigurableProgramAnalysis> automataCPAs = Lists.newArrayList();

    for (Automaton componentAutomaton : componentAutomata) {

      final CPAFactory automataFactory = cfg.usePowerset
          ? PowersetAutomatonCPA.factory()
          : ControlAutomatonCPA.factory();

      automataFactory.setConfiguration(
          Configuration.copyWithNewPrefix(config, componentAutomaton.getName()));
      automataFactory.setLogger(logger.withComponentName(componentAutomaton.getName()));
      automataFactory.set(cfa, CFA.class);
      automataFactory.set(componentAutomaton, Automaton.class);

      automataCPAs.add(automataFactory.createInstance());
    }

    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<>();
    if (cfg.useComposite) {
      ConfigurationBuilder compConfigBuilder = Configuration.builder();
      compConfigBuilder.setOption("cpa.composite.separateTargetStates", "true");
      Configuration compositeConfig = compConfigBuilder.build();

      CPAFactory compositeCpaFactory = CompositeCPA.factory();
      compositeCpaFactory.setChildren(automataCPAs);
      compositeCpaFactory.setConfiguration(compositeConfig);
      compositeCpaFactory.setLogger(logger);
      compositeCpaFactory.set(cfa, CFA.class);

      ConfigurableProgramAnalysis compositeAutomatonCPA = compositeCpaFactory.createInstance();
      lComponentAnalyses.add(compositeAutomatonCPA);
    } else {
      // Add one automata CPA for each goal
      lComponentAnalyses.addAll(automataCPAs);
    }

    // Add the old composite components
    Preconditions.checkState(oldArgCPA.getWrappedCPAs().iterator().next() instanceof CompositeCPA);
    CompositeCPA argCompositeCpa = (CompositeCPA) oldArgCPA.getWrappedCPAs().iterator().next();
    lComponentAnalyses.addAll(argCompositeCpa.getWrappedCPAs());

    // create BBDCPA to handle features in a second step after test generation
    if (addBDDToHandleFeatures) {
      final CPAFactory automataFactory = BDDCPA.factory();

      automataFactory.setConfiguration(config);
      automataFactory.setLogger(logger.withComponentName(BDDCPA.class.toString()));
      automataFactory.set(cfa, CFA.class);
      automataFactory.setShutdownNotifier(mainShutdownManager.getNotifier());

      ConfigurableProgramAnalysis bddCpa = automataFactory.createInstance();
      lComponentAnalyses.add(bddCpa);
    }

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
    pStatsCollection.add(tgarStatistics);
    pStatsCollection.add(tigerStats);
    pStatsCollection.add(tg);
  }

}
