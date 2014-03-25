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
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.testgen.iteration.IterationStrategyFactory;
import org.sosy_lab.cpachecker.core.algorithm.testgen.model.PredicatePathAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.testgen.model.TestGenIterationStrategy;
import org.sosy_lab.cpachecker.core.algorithm.testgen.pathanalysis.PathSelectorFactory;
import org.sosy_lab.cpachecker.core.algorithm.testgen.pathanalysis.TestGenPathAnalysisStrategy;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.StartupConfig;
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
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

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
    LOCATION_AND_VALUE_STATE_TRACKING,
    CFA_TRACKING,
    CUTE_PATH_SELECTOR,
    CUTE_LIKE
  }

  @Option(name = "iterationStrategy", description = "Selects the iteration Strategy for TestGenAlgorithm")
  private IterationStrategySelector iterationStrategySelector = IterationStrategySelector.AUTOMATON_CONTROLLED;

  @Option(name = "analysisStrategy", description = "Selects the analysis Strategy for TestGenAlgorithm")
  private AnalysisStrategySelector analysisStrategySelector = AnalysisStrategySelector.CUTE_PATH_SELECTOR;

  @Option(
      name = "produceDebugFiles",
      description = "Set this to true to get the automaton files for exploring new Paths."
          + " You also get the ARG as dot file and the local reached set for every algoritm iteration"
          + " in subdirs under output.")
  boolean produceDebugFiles = false;

  @Option(name = "testcaseOutputFile", description = "Output file Template under which the"
      + " testcase automatons will be stored. Must include one %s somewhere.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  Path testcaseOutputFile = Paths.get("testcase%s.spc");

  @Option(
      name = "stopOnError",
      description = "algorithm stops on first found error path. Otherwise the algorithms tries to reach 100% coverage")
  private boolean stopOnError = false;

  private CFA cfa;
  private ConfigurableProgramAnalysis cpa;


  private TestGenIterationStrategy iterationStrategy;
  private TestGenPathAnalysisStrategy pathSelector;
  private TestCaseSet testCaseSet;

  private TestGenStatistics stats;
  private int reachedSetCounter = 0;
  private int testCaseCounter = 0;

  public TestGenAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa,
      ShutdownNotifier pShutdownNotifier, CFA pCfa,
      Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException,
      CPAException {

    startupConfig = new StartupConfig(pConfig, pLogger, pShutdownNotifier);
    startupConfig.getConfig().inject(this);
    singleRunConfig = StartupConfig.createWithParent(startupConfig);
    stats = new TestGenStatistics(iterationStrategySelector == IterationStrategySelector.AUTOMATON_CONTROLLED, pCfa);

    cfa = pCfa;
    cpa = pCpa;
    this.logger = pLogger;
    testCaseSet = new TestCaseSet();
    iterationStrategy =
        new IterationStrategyFactory(singleRunConfig, cfa, new ReachedSetFactory(startupConfig.getConfig(), logger),
            stats, produceDebugFiles).createStrategy(iterationStrategySelector, pAlgorithm);
    pathSelector = new PathSelectorFactory(startupConfig).createPathSelector(analysisStrategySelector, pCfa, stats);

  }


  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException,
      PredicatedAnalysisPropertyViolationException {
    startupConfig.getShutdownNotifier().shutdownIfNecessary();
    stats.getTotalTimer().start();

    iterationStrategy.initializeModel(pReachedSet);
    long loopCounter = 0;

    while (true /*globalReached.hasWaitingState()*/) {
      startupConfig.getShutdownNotifier().shutdownIfNecessary();
      logger.logf(Level.FINE, "TestGen iteration %d", loopCounter++);
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

      CounterexampleTraceInfo traceInfo = pathSelector.computePredicateCheck(executedPath);

      if (produceDebugFiles) {
        dumpReachedAndARG(iterationStrategy.getModel().getLocalReached());
      }

      if (traceInfo.isSpurious()) {
        logger.log(Level.FINE, "Current execution path is spurious.");
        //path is infeasible continue to find a new one
      }else{
        testCaseSet.addExecutedPath(executedPath);

        stats.addTestCase(executedPath);
        dumpTestCase(executedPath, traceInfo);


        if (pseudoTarget.isTarget()) {
          logger.log(Level.INFO, "Identified error path.");
          if (stopOnError) {
            // TODO remove  updateGlobalReached();
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
      logger.log(Level.FINE, "Starting predicate path check...");
      PredicatePathAnalysisResult result = pathSelector.findNewFeasiblePathUsingPredicates(executedPath, iterationStrategy.getModel().getLocalReached());
      logger.log(Level.FINE, "predicate path check DONE");

      if (result.isEmpty()) {
        /*
         * we reached all variations (identified by predicates) of the program path.
         * If we didn't find an error, the program is safe and sound, in the sense of a concolic test.
         * TODO: Identify the problems with soundness in context on concolic testing
         */
//      TODO remove  updateGlobalReached();
        stats.getTotalTimer().stop();
        return true; //true = sound or false = unsound. Which case is it here??
      }

      /*
       * symbolic analysis of the path conditions returned a new feasible path (or a new model)
       * the next iteration. Creating automaton to guide next iteration.
       */
      iterationStrategy.updateIterationModelForNextIteration(result);

    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  private void dumpTestCase(ARGPath pExecutedPath, CounterexampleTraceInfo pTraceInfo) {
    if (testcaseOutputFile == null) { return; }

    String fileName = String.format(testcaseOutputFile.toAbsolutePath().toString(), testCaseCounter);
    Path filePath = Paths.get(fileName);

    String automatonName = String.format("Testcase%s", testCaseCounter);
    ARGState rootState = pExecutedPath.getFirst().getFirst();
    CounterexampleInfo ceInfo = CounterexampleInfo.feasible(pExecutedPath, pTraceInfo.getModel());

    try (Writer w = Files.openOutputFile(filePath)) {
      ARGUtils.producePathAutomaton(w, rootState, pExecutedPath.getStateSet(), automatonName, ceInfo);
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write " + automatonName + " to file");
    }

    testCaseCounter++;
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

      Path argFile = Paths.get(outputDir, "args/arg" + reachedSetCounter + ".dot");

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
