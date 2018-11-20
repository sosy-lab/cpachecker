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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import com.google.common.collect.Sets;

import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
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
import org.sosy_lab.java_smt.api.SolverException;

/**
 * This class implements a CPA algorithm based on the idea of concolic execution
 * It traversals the CFA via DFS and introduces new edges for the ARG on branching states in order to cover more branches
 */
public class HybridExecutionAlgorithm implements Algorithm, ReachedSetUpdater {

  @Options(prefix = "hybridExecution")
  public static class HybridExecutionAlgorithmFactory implements AlgorithmFactory {

    @Option(secure=true, name="unboundedDFS", description="Use dfs algorithm unbounded")
    private boolean unbounded = false;

    @Option(secure=true, name="dfsMaxDepth", description="The maximum depth for the dfs algorithm")
    private int dfsMaxDepth = 60;

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
        ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
      pConfiguration.inject(this);
      this.algorithm = pAlgorithm;
      // hybrid execution relies on arg cpa
      this.argCPA = CPAs.retrieveCPA(pArgCPA, ARGCPA.class);
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
            unbounded,
            dfsMaxDepth,
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
  private final Configuration configuration;
  private final LogManager logger;
  private final ShutdownNotifier notifier;

  private final Solver solver;
  private final FormulaManagerView formulaManagerView;
  private final FormulaConverter formulaConverter;
  private final PathFormulaManager pathFormulaManager;

  private final boolean unbounded;
  private int dfsMaxDepth;

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
      boolean pUnbounded,
      int pDfsMaxDepth,
      boolean pUseValueSets)
      throws InvalidConfigurationException {

    this.algorithm = pAlgorithm;
    this.argCPA = pArgCPA;
    this.cfa = pCFA;
    this.logger = pLogger;
    this.configuration = pConfiguration;
    this.notifier = pNotifier;
    this.unbounded = pUnbounded;
    this.dfsMaxDepth = pDfsMaxDepth;
    this.useValueSets = pUseValueSets;
    this.reachedSetUpdateListeners = new LinkedList<>();

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
    this.searchStrategy = (pState, pReachedSet) -> searchLast(pState, pReachedSet);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    // PathFormulaManager to build the BooleanFormula

    // Solver#isUnsat
    // Solver#getProverEnvironment
    // Prover#getModelAsAssignments

    // ReachedSet#add

    // ARGCPA is needed for hybrid execution

    /*
     *  algorithm idea:
     *  first we run the cpa algorihtm
     *  next up we extract an arg-state from the reached set
     *  then, we search from that state on in its children
     *  the search strategy returns an arg-state fro which we retrieve a path through the arg
     *  we build a BooleanFormula for this path
     *  then we check sat for this formula
     *  we choose a state upwards (before or at fork) and retrieve assignments for the variables
     *  decide whether to take this assignments or create new ones (e.g. if useValueSets is true)
     *
     */

    // first we need to collect all assume edges from the cfa to distinguish between already visited and new paths
    CFATraversal traversal = CFATraversal.dfs();
    EdgeCollectingCFAVisitor edgeCollectingVisitor = new EdgeCollectingCFAVisitor();
    CFANode startingNode = cfa.getMainFunction();
    traversal.traverseOnce(startingNode, edgeCollectingVisitor);
    final HashSet<CFAEdge> assumeEdges = Sets.newHashSet(edgeCollectingVisitor.getVisitedEdges());

    // start with good status
    AlgorithmStatus currentStatus = AlgorithmStatus.SOUND_AND_PRECISE;

    boolean running = true;
    while(running) {


      currentStatus = algorithm.run(pReachedSet);
      notifyListeners(pReachedSet);

      // get all bottom states (has no children and is not part of the wait list)
      final Collection<AbstractState> waitList = pReachedSet.getWaitlist(); // simplification for the lambda expression
      List<ARGState> bottomStates = pReachedSet
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

      // retrieve the next state to work on
      ARGState workingState = bottomStates.get(0);

      /*
       * We will define here a assumption context to collect all depending variables
       */

      // search for the next state to flip
      ARGState flipState = searchStrategy.runStrategy(workingState, pReachedSet);

      // bottom up path search
      ARGPath pathToFoundState = ARGUtils.getOnePathTo(flipState);

      // build path formula
      PathFormula pathFormula = buildPathFormula(pathToFoundState.getFullPath());

      boolean satisfiable = false;

      try {
        satisfiable = !solver.isUnsat(pathFormula.getFormula());
      } catch(SolverException sException) {
        throw new CPAException("Exception occurred in SMT-Solver.", sException);
      }

      // choose a state further up in the path
      // get the model assigment
      // push to waitlist

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
  private ARGState searchLast(ARGState pState, ReachedSet pReachedSet) {

    // iterate over the children and check for existence within the reached set
    Collection<ARGState> children = pState.getChildren();

    // bottom reached
    if(children.isEmpty()) {
      return pState;
    }

    for(ARGState childState : children) {

      // we need another way to determine which state to use (i.e. which path to go)
      if(!pReachedSet.contains(childState)) {

        // update reached set
        // pReachedSet.add(childState, pReachedSet.getPrecision(pState));
        // notifyListeners(pReachedSet);

        return searchLast(childState, pReachedSet);
      }
    }

    return null;
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

  @FunctionalInterface
  private interface SearchStrategy {

    ARGState runStrategy(ARGState pState, ReachedSet pReachedSet);
  }

}