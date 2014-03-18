/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.testgen;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.testgen.analysis.BasicTestGenPathAnalysisStrategy;
import org.sosy_lab.cpachecker.core.algorithm.testgen.analysis.BasicTestGenPathAnalysisStrategyExperiment1;
import org.sosy_lab.cpachecker.core.algorithm.testgen.analysis.CFATrackingPathAnalysisStrategy;
import org.sosy_lab.cpachecker.core.algorithm.testgen.analysis.TestGenPathAnalysisStrategy;
import org.sosy_lab.cpachecker.core.algorithm.testgen.iteration.IterationStrategyFactory;
import org.sosy_lab.cpachecker.core.algorithm.testgen.model.PredicatePathAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.testgen.model.TestGenIterationStrategy;
import org.sosy_lab.cpachecker.core.algorithm.testgen.model.TestGenIterationStrategy.IterationModel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;

import com.google.common.base.Joiner;

@Options(prefix = "testgen")
public class TestGenAlgorithm implements Algorithm, StatisticsProvider {

  StartupConfig startupConfig;
  StartupConfig singleRunConfig;
  private LogManager logger;

  public enum IterationStrategySelector {
    AUTOMATON_CONTROLLED,
    SAME_ALGORITHM_RESTART,
    SAME_ALGORITHM_FILTER_WAITLIST
  }

  public enum AnalysisStrategySelector {
    BASIC,
    BASIC_EXPERIMENT1,
    CFA_TRACKING
  }

  @Option(name = "iterationStrategy", description = "Selects the iteration Strategy for TestGenAlgorithm")
  private IterationStrategySelector iterationStrategySelector = IterationStrategySelector.AUTOMATON_CONTROLLED;

  @Option(name = "analysisStrategy", description = "Selects the analysis Strategy for TestGenAlgorithm")
  private AnalysisStrategySelector analysisStrategySelector = AnalysisStrategySelector.CFA_TRACKING;

  @Option(
      name = "stopOnError",
      description = "algorithm stops on first found error path. Otherwise the algorithms tries to reach 100% coverage")
  private boolean stopOnError = false;

  private CFA cfa;
  private ConfigurableProgramAnalysis cpa;


  private TestGenIterationStrategy iterationStrategy;
  private TestGenPathAnalysisStrategy analysisStrategy;
  private TestCaseSet testCaseSet;

  private TestGenStatistics stats;
  private int reachedSetCounter = 0;


  //  ConfigurationBuilder singleConfigBuilder = Configuration.builder();
  //  singleConfigBuilder.copyFrom(globalConfig);
  //  singleConfigBuilder.clearOption("restartAlgorithm.configFiles");
  //  singleConfigBuilder.clearOption("analysis.restartAfterUnknown");


  public TestGenAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa,
      ShutdownNotifier pShutdownNotifier, CFA pCfa,
      Configuration pConfig, LogManager pLogger, CPABuilder pCpaBuilder) throws InvalidConfigurationException,
      CPAException {

    startupConfig = new StartupConfig(pConfig, pLogger, pShutdownNotifier);
    startupConfig.getConfig().inject(this);
    singleRunConfig = StartupConfig.createWithParent(startupConfig);
    stats = new TestGenStatistics(iterationStrategySelector == IterationStrategySelector.AUTOMATON_CONTROLLED);

    cfa = pCfa;
    cpa = pCpa;
    this.logger = pLogger;
    testCaseSet = new TestCaseSet();
    FormulaManagerFactory formulaManagerFactory =
        new FormulaManagerFactory(startupConfig.getConfig(), pLogger,
            ShutdownNotifier.createWithParent(pShutdownNotifier));
    FormulaManagerView formulaManager =
        new FormulaManagerView(formulaManagerFactory.getFormulaManager(), startupConfig.getConfig(), logger);
    PathFormulaManager pfMgr = new PathFormulaManagerImpl(formulaManager, startupConfig.getConfig(), logger, cfa);
    Solver solver = new Solver(formulaManager, formulaManagerFactory);
    PathChecker pathChecker = new PathChecker(pLogger, pfMgr, solver);
    iterationStrategy =
        new IterationStrategyFactory(singleRunConfig, cfa, new ReachedSetFactory(startupConfig.getConfig(), logger),
            pCpaBuilder, stats).createStrategy(iterationStrategySelector, pAlgorithm);

    switch (analysisStrategySelector) {
    case BASIC:
      analysisStrategy = new BasicTestGenPathAnalysisStrategy(pathChecker, startupConfig, stats);
      break;

    case BASIC_EXPERIMENT1:
      analysisStrategy = new BasicTestGenPathAnalysisStrategyExperiment1(pathChecker, startupConfig, stats, cpa);
      break;

    case CFA_TRACKING:
      analysisStrategy = new CFATrackingPathAnalysisStrategy(pathChecker, startupConfig, stats);
      break;

    default:
      throw new IllegalStateException("Not all analysisStrategySelector cases matched");
    }
    //

  }


  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException,
      PredicatedAnalysisPropertyViolationException {
    startupConfig.getShutdownNotifier().shutdownIfNecessary();
    stats.getTotalTimer().start();
//    PredicatePathAnalysisResult lastResult = PredicatePathAnalysisResult.INVALID;
    iterationStrategy.initializeModel(pReachedSet);
    long loopCounter = 0;
    boolean initialRun = true;

    while (true /*globalReached.hasWaitingState()*/) {
      startupConfig.getShutdownNotifier().shutdownIfNecessary();
      logger.logf(Level.INFO, "TestGen iteration %d", loopCounter++);
      //explicit, DFS or DFSRAND, PRECISION=TRACK_ALL; with automaton of new path created in previous iteration OR custom CPA
      try {
        //sound should normally be unsound for us. Ignore the result
        iterationStrategy.runAlgorithm();
      } catch (InterruptedException e) {
        startupConfig.getShutdownNotifier().shutdownIfNecessary();
        // TODO handle shutdown of child but not this algorithm
      }

      if (!(iterationStrategy.getLastState() instanceof ARGState)) { throw new IllegalStateException(
          "wrong configuration of explicit cpa, because concolicAlg needs ARGState"); }
      /*
       * check if reachedSet contains a target (error) state.
       */
      ARGState pseudoTarget = (ARGState) iterationStrategy.getLastState();
      ARGPath executedPath = ARGUtils.getOnePathTo(pseudoTarget);
      if (initialRun)
      {
        initialRun = false;
      }
      CounterexampleTraceInfo traceInfo = analysisStrategy.computePredicateCheck(executedPath);
      if (traceInfo.isSpurious()) {
        logger.log(Level.INFO, "Current execution path is spurious.");
        //path is infeasible continue to find a new one
      }else{
        testCaseSet.addExecutedPath(executedPath);
        if (pseudoTarget.isTarget()) {
          logger.log(Level.INFO, "Identified error path.");
          if (stopOnError) {
            updateGlobalReached();
            stats.getTotalTimer().stop();
            return true;
          } else {
            testCaseSet.addTarget(pseudoTarget);
            //TODO add error to errorpathlist
          }
        }
      }
      /*
       * selecting new path to traverse.
       */
      //      if(!ARGUtils.checkARG(iterationStrategy.getModel().getLocalReached())) {
      //        logger.log(Level.WARNING, "Current ReachedSet is invalid");
      //      }


      logger.log(Level.INFO, "Starting predicate path check...");
      PredicatePathAnalysisResult result = analysisStrategy.findNewFeasiblePathUsingPredicates(executedPath);
      logger.log(Level.INFO, "Starting predicate path check DONE");

      dumpReachedAndARG(iterationStrategy.getModel().getLocalReached());

      if (result.isEmpty()) {
        /*
         * we reached all variations (identified by predicates) of the program path.
         * If we didn't find an error, the program is safe and sound, in the sense of a concolic test.
         * TODO: Identify the problems with soundness in context on concolic testing
         */
        updateGlobalReached();
        stats.getTotalTimer().stop();
        return true; //true = sound or false = unsound. Which case is it here??
      }
      /*
       * symbolic analysis of the path conditions returned a new feasible path (or a new model)
       * the next iteration. Creating automaton to guide next iteration.
       */
      iterationStrategy.updateIterationModelForNextIteration(result);

//      lastResult = result;
    }
  }

  private void updateGlobalReached() {
    IterationModel model = iterationStrategy.getModel();
    model.getGlobalReached().clear();
    for (AbstractState state : model.getLocalReached()) {
      model.getGlobalReached().add(state, model.getLocalReached().getPrecision(state));
      model.getGlobalReached().removeOnlyFromWaitlist(state);
    }

  }


  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  private void dumpReachedAndARG(ReachedSet pReached) {
    try {
      String outputDir = new FileTypeConverter(startupConfig.getConfig()).getOutputDirectory();

      Path reachedFile = Paths.get(outputDir, "reachedsets/reached" + reachedSetCounter + ".txt");

      try (Writer w = Files.openOutputFile(reachedFile)) {
        Joiner.on('\n').appendTo(w, pReached);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write reached set to file");
      } catch (OutOfMemoryError e) {
        logger.logUserException(Level.WARNING, e,
            "Could not write reached set to file due to memory problems");
      }

      Path argFile = Paths.get(outputDir, "output/args/arg" + reachedSetCounter + ".dot");

      try (Writer w = Files.openOutputFile(argFile)) {
        ARGUtils.writeARGAsDot(w, (ARGState) pReached.getFirstState());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
      }

      reachedSetCounter++;
    } catch (InvalidConfigurationException e1) {
      throw new IllegalStateException(e1);
    }
  }

  protected class TestCaseSet {

    public void addTarget(AbstractState target) {
      //FIXME
    }

    public void addExecutedPath(ARGPath path) {
      //FIXME
    }
  }
}
