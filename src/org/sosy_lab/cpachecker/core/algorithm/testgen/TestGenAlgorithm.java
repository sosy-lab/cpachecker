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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Files.DeleteOnCloseFile;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

@Options(prefix = "testgen")
public class TestGenAlgorithm implements Algorithm {

  private Algorithm explicitAlg;
  private LogManager logger;
  private CFA cfa;
  private ConfigurableProgramAnalysis cpa;

  private PathFormulaManager pfMgr;
  private Solver solver;
  PathChecker pathChecker;
  private ReachedSetFactory reachedSetFactory;
  private Configuration config;
  private ShutdownNotifier shutdownNotifier;


  //  ConfigurationBuilder singleConfigBuilder = Configuration.builder();
  //  singleConfigBuilder.copyFrom(globalConfig);
  //  singleConfigBuilder.clearOption("restartAlgorithm.configFiles");
  //  singleConfigBuilder.clearOption("analysis.restartAfterUnknown");


  public TestGenAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa,
      ShutdownNotifier pShutdownNotifier, CFA pCfa,
      Configuration config, LogManager pLogger) throws InvalidConfigurationException, CPAException {

    shutdownNotifier = pShutdownNotifier;
    cfa = pCfa;
    cpa = pCpa;
    this.config = config;
    config.inject(this);
    this.explicitAlg = pAlgorithm;
    this.logger = pLogger;

    FormulaManagerFactory formulaManagerFactory =
        new FormulaManagerFactory(config, pLogger, ShutdownNotifier.createWithParent(pShutdownNotifier));
    FormulaManagerView formulaManager =
        new FormulaManagerView(formulaManagerFactory.getFormulaManager(), config, logger);
    pfMgr = new PathFormulaManagerImpl(formulaManager, config, logger, cfa);
    solver = new Solver(formulaManager, formulaManagerFactory);
    pathChecker = new PathChecker(pLogger, pfMgr, solver);
    reachedSetFactory = new ReachedSetFactory(config, logger);

    /*TODO change the config file, so we can configure 'dfs'*/
    //    Configuration testCaseConfig = Configuration.copyWithNewPrefix(config, "testgen.");
    //    explicitAlg = new ExplicitTestcaseGenerator(config, logger, pShutdownNotifier, cfa, filename);
  }


  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException,
      PredicatedAnalysisPropertyViolationException {

    ReachedSet globalReached = pReachedSet;
    ReachedSet currentReached = reachedSetFactory.create();
    AbstractState initialState = globalReached.getFirstState();
    currentReached.add(initialState, globalReached.getPrecision(initialState));

    //    currentReached.add(globalReached. precision);
    while (globalReached.hasWaitingState()) {
      //explicit, DFS, PRECISION=TRACK_ALL; with automaton of new path created in previous iteration OR custom CPA
      boolean sound = explicitAlg.run(currentReached);
      //sound should normally be unsound for us.
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
      CounterexampleTraceInfo newPath = findNewFeasiblePathUsingPredicates(executedPath);
      if (newPath == null) {
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
      //TODO: Peter Implement me
      explicitAlg = createAlgorithmForNextIteration(newPath);
      initialState = globalReached.getFirstState();
      currentReached = reachedSetFactory.create();
      currentReached.add(initialState, globalReached.getPrecision(initialState));

      //ARGUtils.producePathAutomaton(sb, pRootState, pPathStates, name, pCounterExample);
      //traceInfo.getModel()
      //      globalReached.add(currentReached.getAllReached);
    }
    return false;
  }

  private Algorithm createAlgorithmForNextIteration(CounterexampleTraceInfo pNewPath) {

    // This temp file will be automatically deleted when the try block terminates.
    try (DeleteOnCloseFile automatonFile = Files.createTempFile("next_automaton", ".txt")) {

      ConfigurationBuilder builder = Configuration.builder().copyFrom(config);
      // TODO: use the right automaton (the modified one)
      builder = builder.setOption("specification", automatonFile.toPath().toAbsolutePath().toString());
      Configuration lconfig = builder.build();


      try (Writer w = Files.openOutputFile(automatonFile.toPath())) {
        ARGUtils.producePathAutomaton(w, "nextPathAutomaton", pNewPath);
      }

      if (explicitAlg instanceof CPAAlgorithm) {
        return CPAAlgorithm.create(cpa, logger, lconfig, shutdownNotifier);
      } else {
        throw new IllegalStateException("Generating a new Algorithm here only Works if the Algorithm"
            + " is a CPAAlgorithm");
      }

    } catch (IOException | InvalidConfigurationException e) {
      // TODO: use another exception?
      throw new IllegalStateException("Unable to create the Algorithm for next Iteration", e);
    }
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
      for (AutomatonState autoState : wrapped) {
        if (!autoState.getOwningAutomatonName().equals("EvalOnlyOnePathAutomaton")) {
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

  private CounterexampleTraceInfo findNewFeasiblePathUsingPredicates(ARGPath pExecutedPath)
      throws CPATransferException, InterruptedException {
    List<CFAEdge> newPath = Lists.newArrayList(pExecutedPath.asEdgesList());
    Iterator<Pair<ARGState, CFAEdge>> branchingEdges =
        Iterators.filter(Iterators.consumingIterator(pExecutedPath.descendingIterator()),
            new Predicate<Pair<ARGState, CFAEdge>>() {

              @Override
              public boolean apply(Pair<ARGState, CFAEdge> pInput) {
                CFAEdge lastEdge = pInput.getSecond();
                if (lastEdge == null) {
                return false;
                }
                CFANode decidingNode = lastEdge.getPredecessor();
                //num of leaving edges does not include a summary edge, so the check is valid.
                if (decidingNode.getNumLeavingEdges() == 2) {
                return true;
                }
                return false;
              }
            });
    while (branchingEdges.hasNext())
    {
      Pair<ARGState, CFAEdge> branchingPair = branchingEdges.next();
      CFAEdge wrongEdge = branchingPair.getSecond();
      CFANode decidingNode = wrongEdge.getPredecessor();
      CFAEdge otherEdge = null;
      for (CFAEdge cfaEdge : CFAUtils.leavingEdges(decidingNode)) {
        if (cfaEdge.equals(wrongEdge)) {
          continue;
        } else {
          otherEdge = cfaEdge;
          break;
        }
      }
      //should not happen; If it does make it visible.
      assert otherEdge != null;
      newPath = Lists.newArrayList(pExecutedPath.asEdgesList());
      newPath.add(otherEdge);
      CounterexampleTraceInfo traceInfo = pathChecker.checkPath(newPath);
      //      traceInfo.
      if (!traceInfo.isSpurious()) { return traceInfo; }

    }
    return null;
  }

  private ReachedSet createNextReachedSet(
      ConfigurableProgramAnalysis cpa,
      CFANode mainFunction,
      ReachedSetFactory pReachedSetFactory) {
    logger.log(Level.FINE, "Creating initial reached set");

    AbstractState initialState = cpa.getInitialState(mainFunction);
    Precision initialPrecision = cpa.getInitialPrecision(mainFunction);

    ReachedSet reached = pReachedSetFactory.create();
    reached.add(initialState, initialPrecision);
    return reached;
  }


  //  boolean pathToExplore = true;
  //  boolean success = true;
  //  /* run the given alg ones using the "config/specification/onepathloopautomaton.spc" and DFS */
  //  ReachedSet currentReachedSet = pReachedSet;
  //  success &= algorithm.run(currentReachedSet);
  //  do{
  ////    combinedExplPredAlg.run(currentReachedSet);
  //  }while(success);
  //
  //  do {
  ////    pReachedSet.get
  ////    AbstractStates.isTargetState(as)
  //    /**/
  //    ARGState currentRootState =(ARGState) currentReachedSet.getFirstState();
  //    ARGState lastState = (ARGState) currentReachedSet.getLastState();
  //    ARGPath path = ARGUtils.getOnePathTo(lastState);
  //
  ////    for (AbstractState s : from(reached).filter(IS_TARGET_STATE)) {
  //    currentReachedSet = explicitAlg.analysePath(currentRootState, lastState, path.getStateSet());
  ////    dummyCreator.computeOtherSuccessor(pState, pNotToChildState)
  ////    dummyCreator.computeOtherSuccessor(pState, pNotToChildState);
  //    /**/
  ////    pReachedSet.getFirstState()
  ////    ARGUtils.getPathFromBranchingInformation(root, arg, branchingInformation)
  ////    explicitAlg.analysePath(currentRootState, null, errorPathStates);
  //  }while(pathToExplore & success);
  //  return false;

}
