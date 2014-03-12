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

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.testgen.analysis.BasicTestGenPathAnalysisStrategy;
import org.sosy_lab.cpachecker.core.algorithm.testgen.analysis.TestGenPathAnalysisStrategy;
import org.sosy_lab.cpachecker.core.algorithm.testgen.model.AutomatonControlledIterationStrategy;
import org.sosy_lab.cpachecker.core.algorithm.testgen.model.PredicatePathAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.testgen.model.SameAlgorithmRestartAtDecisionIterationStrategy;
import org.sosy_lab.cpachecker.core.algorithm.testgen.model.TestGenIterationStrategy;
import org.sosy_lab.cpachecker.core.algorithm.testgen.model.TestGenIterationStrategy.IterationModel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;

import com.google.common.collect.FluentIterable;

@Options(prefix = "testgen")
public class TestGenAlgorithm implements Algorithm {

  StartupConfig startupConfig;
  private LogManager logger;

//  private Algorithm explicitAlg;

  private enum IterationStrategySelector {
    AUTOMATON_CONTROLLED,
    SAME_ALGORITHM_RESTART
  }

  @Option(name="iterationStrategy", description="Selects the iteration Strategy for TestGenAlgorithm")
  private IterationStrategySelector iterationStrategySelector = IterationStrategySelector.AUTOMATON_CONTROLLED;

  private CFA cfa;
  private ConfigurableProgramAnalysis cpa;

  private ReachedSetFactory reachedSetFactory;

  private TestGenIterationStrategy iterationStrategy;
  private TestGenPathAnalysisStrategy analysisStrategy;


  //  ConfigurationBuilder singleConfigBuilder = Configuration.builder();
  //  singleConfigBuilder.copyFrom(globalConfig);
  //  singleConfigBuilder.clearOption("restartAlgorithm.configFiles");
  //  singleConfigBuilder.clearOption("analysis.restartAfterUnknown");


  public TestGenAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa,
      ShutdownNotifier pShutdownNotifier, CFA pCfa,
      Configuration pConfig, LogManager pLogger, CPABuilder pCpaBuilder) throws InvalidConfigurationException, CPAException {
    startupConfig = new StartupConfig(pConfig, pLogger, pShutdownNotifier);
    startupConfig.getConfig().inject(this);
    cfa = pCfa;
    cpa = pCpa;
    this.logger = pLogger;

    FormulaManagerFactory formulaManagerFactory =
        new FormulaManagerFactory(startupConfig.getConfig(), pLogger, ShutdownNotifier.createWithParent(pShutdownNotifier));
    FormulaManagerView formulaManager =
        new FormulaManagerView(formulaManagerFactory.getFormulaManager(), startupConfig.getConfig(), logger);
    PathFormulaManager pfMgr = new PathFormulaManagerImpl(formulaManager, startupConfig.getConfig(), logger, cfa);
    Solver solver = new Solver(formulaManager, formulaManagerFactory);
    PathChecker pathChecker = new PathChecker(pLogger, pfMgr, solver);
    reachedSetFactory = new ReachedSetFactory(startupConfig.getConfig(), logger);

    IterationModel model = new IterationModel(pAlgorithm, null, null);

    switch (iterationStrategySelector) {
    case AUTOMATON_CONTROLLED:
      iterationStrategy = new AutomatonControlledIterationStrategy(startupConfig, pCfa, model, reachedSetFactory, pCpaBuilder);
      break;
    case SAME_ALGORITHM_RESTART:
      iterationStrategy = new SameAlgorithmRestartAtDecisionIterationStrategy(startupConfig, reachedSetFactory, model);
      break;
    default:
      throw new InvalidConfigurationException("Invald iteration strategy selected");
    }

    analysisStrategy = new BasicTestGenPathAnalysisStrategy(pathChecker);



    /*TODO change the config file, so we can configure 'dfs'*/
    //    Configuration testCaseConfig = Configuration.copyWithNewPrefix(config, "testgen.");
    //    explicitAlg = new ExplicitTestcaseGenerator(config, logger, pShutdownNotifier, cfa, filename);
  }


  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException,
      PredicatedAnalysisPropertyViolationException {
//    List<ReachedSet> reachedSetHistory = Lists.newLinkedList();
    PredicatePathAnalysisResult lastResult = PredicatePathAnalysisResult.INVALID;
    ReachedSet globalReached = pReachedSet;
    ReachedSet currentReached = reachedSetFactory.create();
    iterationStrategy.getModel().setGlobalReached(globalReached);
    iterationStrategy.getModel().setLocalReached(currentReached);

    AbstractState initialState = globalReached.getFirstState();
    currentReached.add(initialState, globalReached.getPrecision(initialState));

    while (true /*globalReached.hasWaitingState()*/) {

      //explicit, DFS, PRECISION=TRACK_ALL; with automaton of new path created in previous iteration OR custom CPA
      boolean sound = iterationStrategy.runAlgorithm();
      //sound should normally be unsound for us.

      //maybe remove marker node from currentReached. might depend on iterationStrategy and should be part of the runAlg()
      if(false && lastResult.isValid())
      {
        currentReached.remove(lastResult.getWrongState());
      }
      if (!(currentReached.getLastState() instanceof ARGState)) { throw new IllegalStateException(
          "wrong configuration of explicit cpa, because concolicAlg needs ARGState"); }
      /*
       * check if reachedSet contains a target state.
       * A target state is either an ERROR (declared by at least one Automaton) or TestGenTarget.
       * TestGenTarget signals that the current DFS-searched path has no more successors.
       * If TargetState is only a TestGenTarget, we continue with the TestGenAlgorithm.
       * If ERROR we can exit because we found a real error.
       */
      ARGState pseudoTarget = (ARGState) currentReached.getLastState();
      if (isRealTargetError(pseudoTarget)) { return true; }
      /*
       * not an error path. selecting new path to traverse.
       */

      ARGPath executedPath = ARGUtils.getOnePathTo(pseudoTarget);
      PredicatePathAnalysisResult result = analysisStrategy.findNewFeasiblePathUsingPredicates(executedPath);
      if (result.isEmpty()) {
        /*
         * we reached all variations (identified by predicates) of the program path.
         * If we didn't find an error, the program is safe and sound, in the sense of a concolic test.
         * TODO: Identify the problems with soundness in context on concolic testing
         */
        return true; //true = sound or false = unsound. Which case is it here??
      }
      /*
       * symbolic analysis of the path conditions returned a new feasible path (or a new model)
       * the next iteration. Creating automaton to guide next iteration.
       */
      iterationStrategy.updateIterationModelForNextIteration(result);
//      CounterexampleTraceInfo newPath = result.getTrace();

      lastResult = result;
    }
//    return false;
  }


  /**
   * checks if the given state is a error-target-state in the sense of a program error.
   * Since the concolic algorithm uses control-automaton target states to leave CPAAlgorithm early,
   * a target state can be signalled without a program error.
   * If another automaton signalled target state, the given state is handled as an error,
   * even if the concolic control automaton signalled an error as well.
   * @param pseudoTarget
   * @return true if 'real' program error, false if target state only from concolic automaton.
   */
  private boolean isRealTargetError(ARGState pseudoTarget) {
    if (AbstractStates.isTargetState(pseudoTarget))
    {
      FluentIterable<AbstractState> w = AbstractStates.asIterable(pseudoTarget);
      //get all wrapped automaton states.
      FluentIterable<AutomatonState> wrapped = AbstractStates.projectToType(w, AutomatonState.class);
      //check if any automaton except the TestGenAutomaton "EvalOnlyOnePathAutomaton" reached a target state.
      for (AutomatonState autoState : wrapped) {//TODO extract into method that is resistant to String-changes
        if (!autoState.getOwningAutomatonName().equals("EvalOnlyOnePathAutomaton") && !autoState.getOwningAutomatonName().equals("nextPathAutomaton")) {
          /*
           * target state means error state.
           * we found an error path and leave the analysis to the surrounding alg.
           */
          if (autoState.isTarget()) { return true; }
        }
      }
    }
    return false;
  }



}
