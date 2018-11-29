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
/**
 * The core algorithms of CPAchecker.
 */
package org.sosy_lab.cpachecker.core.algorithm;

import apron.NotImplementedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm.ReachedSetUpdateListener;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm.ReachedSetUpdater;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * This class implements a CPA algorithm based on the idea of concolic execution It traversals the
 * CFA via DFS and introduces new edges for the ARG on branching states in order to cover more
 * branches
 */
public class HybridExecutionAlgorithm implements Algorithm, ReachedSetUpdater {

  @Options(prefix = "hybridExecution")
  public static class HybridExecutionAlgorithmFactory implements AlgorithmFactory {

    @Option(secure=true, name="useValueSets", description="Wether to use multiple values on a state")
    private boolean useValueSets = false;

    private final Algorithm algorithm;
    private final ARGCPA argCPA;
    private final CFA cfa;
    private final LogManager logger;
    private final Configuration configuration;
    private final ShutdownNotifier notifier;

    /**
     *
     * @param pAlgorithm The composed algorithm created up to this point
     * @param pArgCPA The respective wrappig CPA (the hybrid execution algorithm depends on the ARGCPA)
     * @param pCFA The cfa for the code to be checked
     * @param pLogger The logger instance
     * @param pConfiguration The applications configuration
     * @param pShutdownNotifier A shutdown notifier
     * @throws InvalidConfigurationException Throws InvalidConfigurationException if injection of the factories options fails
     */
    public HybridExecutionAlgorithmFactory(
        Algorithm pAlgorithm,
        ConfigurableProgramAnalysis pArgCPA,
        CFA pCFA,
        LogManager pLogger,
        Configuration pConfiguration,
        ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException, CPAException {
      pConfiguration.inject(this);
      this.algorithm = pAlgorithm;
      // hybrid execution relies on arg cpa
      this.argCPA = CPAs.retrieveCPA(pArgCPA, ARGCPA.class);

      if(argCPA == null) {
        throw new CPAException("Hybrid Execution relies on the >Abstract Reachability Graph CPA<");
      }

      this.cfa = pCFA;
      this.logger = pLogger;
      this.configuration = pConfiguration;
      this.notifier = pShutdownNotifier;
    }

    @Override
    public Algorithm newInstance() {
      try {
        return new HybridExecutionAlgorithm(
            algorithm,
            argCPA,
            cfa,
            logger,
            configuration,
            notifier,
            useValueSets);
      } catch (InvalidConfigurationException e) {
        // this is a bad place to catch an exception
        logger.log(Level.SEVERE, e.getMessage());
      }
      // meh...
      return algorithm;
    }

  }

  private final Algorithm algorithm;
  private final ARGCPA argCPA;
  private final CFA cfa;
  private final LogManager logger;

  private final Solver solver;
  private final FormulaManagerView formulaManagerView;
  private final FormulaConverter formulaConverter;
  private final PathFormulaManager pathFormulaManager;

  // we could run the search with several values satisfying certain conditions to achieve a broader coverage
  // this must be experimental validated (in theory it is very easy to find several variable values for a specific state that will later on lead to different executions)
  /*
   * here we might have the precondition, that y is never less than 1 (meaning, there is no path through the code that might lead to y being smaller than 1)
   * int calc(int x, int y) {
   *  int result = 0;
   *  if(x <= 0) {
   *    return 0; // we want the calculated value to never be smaller than 0
   *  }
   *
   *  result = x * y;
   *  if(result > x) {
   *    // at this point, we know that y must have been greater than 1
   *    return result;
   *  }
   *
   *  return x * predefinedConstant; // so if y was actually 1, we multiply x with a predefined constant value (for some reasons ;) )
   *
   * }
   *
   * multiple value assignments for this example:
   * (1) x = 3, y = 1 -> this leads to 3* predefinedConstant
   * (2) x = 3, y = 2 -> this leads to 6
   * we could probably use some sort of negation heuristic for existing assignments like this
   * negateOrMinimize(assigments(1)):
   * (3) x = -3, y = 1 -> leads to 0
   * here y is not negated, because of precondition being y >= 1
   *
   * with these 3 assigments we catch every path through the method
   *
   * thus, without any further investigation (and runs of the cpa algorithm), we might be able to reach many different states
   * by simply executing the search strategy for all those assignments (maybe even in parallel ...)
   *
   */
  private boolean useValueSets;
  private final List<ReachedSetUpdateListener> reachedSetUpdateListeners;

  private final SearchStrategy searchStrategy;

  private HybridExecutionAlgorithm(
      Algorithm pAlgorithm,
      ARGCPA pArgCPA,
      CFA pCFA,
      LogManager pLogger,
      Configuration pConfiguration,
      ShutdownNotifier pNotifier,
      boolean pUseValueSets)
      throws InvalidConfigurationException {

    this.algorithm = pAlgorithm;
    this.argCPA = pArgCPA;
    this.cfa = pCFA;
    this.logger = pLogger;
    this.useValueSets = pUseValueSets;
    this.reachedSetUpdateListeners = new ArrayList<>();

    this.solver = Solver.create(pConfiguration, pLogger, pNotifier);
    this.formulaManagerView = solver.getFormulaManager();

    this.formulaConverter = new FormulaConverter(
        formulaManagerView,
        new CProgramScope(pCFA, pLogger),
        pLogger,
        pCFA.getMachineModel(),
        pConfiguration);

    this.pathFormulaManager = new PathFormulaManagerImpl(
        formulaManagerView,
        pConfiguration,
        pLogger,
        pNotifier,
        pCFA,
        AnalysisDirection.FORWARD);

    // configurable search strategy
    this.searchStrategy = (pState, pARGPath, pAssumeEdges) -> searchLast(pState, pARGPath, pAssumeEdges);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    // ReachedSet#add

    logger.log(Level.INFO, "Hybrid Execution algorithm started.");

    // first we need to collect all assume edges from the cfa to distinguish between already visited and new paths
    CFATraversal traversal = CFATraversal.dfs();
    EdgeCollectingCFAVisitor edgeCollectingVisitor = new EdgeCollectingCFAVisitor();
    CFANode startingNode = cfa.getMainFunction();
    traversal.traverseOnce(startingNode, edgeCollectingVisitor);
    final Set<AssumeEdge> assumeEdges = extractAssumeEdges(edgeCollectingVisitor.getVisitedEdges());

    logger.log(Level.FINEST, "Assume edges from program cfa collected.");

    // start with good status
    AlgorithmStatus currentStatus = AlgorithmStatus.SOUND_AND_PRECISE;

    boolean running = true;
    while(running) {

      currentStatus = algorithm.run(pReachedSet);
      notifyListeners(pReachedSet);

      // get all bottom states (has no children and is not part of the wait list)
      final Collection<AbstractState> waitList = pReachedSet.getWaitlist(); // simplification for the lambda expression
      final List<ARGState> bottomStates = pReachedSet
          .asCollection()
          .stream()
          .filter(state -> !waitList.contains(state))
          .map(state -> AbstractStates.extractStateByType(state, ARGState.class))
          .filter(argState -> argState.getChildren().isEmpty())
          .collect(Collectors.toList());

      // there is nothing left to do
      if(bottomStates.isEmpty()){
        return currentStatus;
      }

      // retrieve the next state to work on -> it doesn't matter which one, thus we simply choose the first one
      ARGState workingState = bottomStates.get(0);

      // retrieve path through the arg to the working state
      ARGPath pathToWorkingState = ARGUtils.getOnePathTo(workingState);

      Collection<AssumeEdge> assumeEdgesInPath = extractAssumeEdges(pathToWorkingState.getInnerEdges());

      // remove already visited assumptions
      assumeEdges.removeAll(assumeEdgesInPath);

      /*
       * TODO
       * We will define here a assumption context to collect all depending variables
       */

      // search for the next state to flip
      ARGState flipState = searchStrategy.runStrategy(workingState, pathToWorkingState, assumeEdges);

      // bottom up path search
      ARGPath pathToFoundState = ARGUtils.getOnePathTo(flipState);

      // build path formula
      PathFormula pathFormula = buildPathFormula(pathToFoundState.getFullPath());

      try {

        boolean satisfiable = !solver.isUnsat(pathFormula.getFormula());

        // -- infeasibility could be reported here --

        logger.log(Level.INFO, String.format("The boolean formula %s is not satisfiable for the solver", pathFormula.getFormula()));

        // get assignments for the new path containing the flipped assumption
        if(satisfiable) {
          ProverEnvironment proverEnvironment = solver.newProverEnvironment();
          Collection<ValueAssignment> assignments = proverEnvironment.getModelAssignments();
        }

      } catch(SolverException sException) {
        throw new CPAException("Exception occurred in SMT-Solver.", sException);
      }

      // check for continuation
      running = checkContinue(pReachedSet);
    }

    return currentStatus;
  }

  @Override
  public void register(ReachedSetUpdateListener pReachedSetUpdateListener) {

    if(algorithm instanceof ReachedSetUpdater) {
      ((ReachedSetUpdater) algorithm).register(pReachedSetUpdateListener);
    }

    reachedSetUpdateListeners.add(pReachedSetUpdateListener);
  }

  @Override
  public void unregister(ReachedSetUpdateListener pReachedSetUpdateListener) {

    if(algorithm instanceof ReachedSetUpdater) {
      ((ReachedSetUpdater) algorithm).unregister(pReachedSetUpdateListener);
    }

    reachedSetUpdateListeners.remove(pReachedSetUpdateListener);
  }

  // traverses from a given ARGState upwards to find the next branching state
  private ARGState searchLast(ARGState pState, ARGPath pPath, Set<AssumeEdge> assumeEdges) {

   throw new NotImplementedException();
  }

  // builds the complete path formula for a path through the application denoted by the set of edges
  private PathFormula buildPathFormula(Collection<CFAEdge> pEdges)
      throws CPATransferException, InterruptedException {
    PathFormula formula = pathFormulaManager.makeEmptyPathFormula();
    for(CFAEdge edge : pEdges) {
      formula = pathFormulaManager.makeAnd(formula, edge);
    }

    return formula;
  }

  // notify listeners on update of the reached set
  private void notifyListeners(ReachedSet pReachedSet) {
    reachedSetUpdateListeners.forEach(listener -> listener.updated(pReachedSet));
  }

  // check if the algorithm should continue
  private boolean checkContinue(ReachedSet pReachedSet) {

    boolean check = true;
    // some checks for the state
    // update check

    check = check && pReachedSet.hasWaitingState();
    return check;
  }

  // helper method to extract assume edges from a given collection of general cfa edges
  private Set<AssumeEdge> extractAssumeEdges(Collection<CFAEdge> edges) {
    return edges
      .stream()
      .filter(edge -> edge.getEdgeType() == CFAEdgeType.AssumeEdge)
      .map(edge -> (AssumeEdge)edge)
      .collect(Collectors.toSet());
  }

  @FunctionalInterface
  private interface SearchStrategy {

    ARGState runStrategy(ARGState pState, ARGPath pPath, Set<AssumeEdge> assumeEdges);
  }

}
