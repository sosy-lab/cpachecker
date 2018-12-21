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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
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
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.cpa.hybrid.HybridAnalysisState;
import org.sosy_lab.cpachecker.cpa.hybrid.exception.InvalidAssumptionException;
import org.sosy_lab.cpachecker.cpa.hybrid.util.ExpressionUtils;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.CachingPathFormulaManager;
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
public final class HybridExecutionAlgorithm implements Algorithm, ReachedSetUpdater {

  @Options(prefix = "hybridExecution")
  public static class HybridExecutionAlgorithmFactory implements AlgorithmFactory {

    @Option(secure = true, name = "useValueSets", description = "Wether to use multiple values on a state.")
    private boolean useValueSets = false;

    @Option(secure = true, name = "useBFS", description = "Whether to use BFS algorithm instead of DFS for searching the next assumption to flip.")
    private boolean useBFS = false;

    @Option(secure = true, name = "maxNumberMissedAssumption", 
      description = "The maximum number to tolerate a run of the algorithm in which no new assumption to flip could ne found")
    private int maxNumberMissedAssumption = 5;

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
            useValueSets,
            useBFS,
            maxNumberMissedAssumption);
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
  private boolean useBFS;
  private final int maxNumMissedAssumption;
  private final List<ReachedSetUpdateListener> reachedSetUpdateListeners;

  private final SearchStrategy searchStrategy;

  private HybridExecutionAlgorithm(
      Algorithm pAlgorithm,
      ARGCPA pArgCPA,
      CFA pCFA,
      LogManager pLogger,
      Configuration pConfiguration,
      ShutdownNotifier pNotifier,
      boolean pUseValueSets,
      boolean pUseBFS,
      int pMaxNumMissedAssumption)
      throws InvalidConfigurationException {

    this.algorithm = pAlgorithm;
    this.argCPA = pArgCPA;
    this.cfa = pCFA;
    this.logger = pLogger;
    this.useValueSets = pUseValueSets;
    this.useBFS = pUseBFS;
    this.maxNumMissedAssumption = pMaxNumMissedAssumption;
    this.reachedSetUpdateListeners = new ArrayList<>();

    this.solver = Solver.create(pConfiguration, pLogger, pNotifier);
    this.formulaManagerView = solver.getFormulaManager();

    this.formulaConverter = new FormulaConverter(
        formulaManagerView,
        new CProgramScope(pCFA, pLogger),
        pLogger,
        pCFA.getMachineModel(),
        pConfiguration);

    this.pathFormulaManager = new CachingPathFormulaManager(
        new PathFormulaManagerImpl(
            formulaManagerView,
            pConfiguration,
            pLogger,
            pNotifier,
            pCFA,
            AnalysisDirection.FORWARD)
    );

    // configurable search strategy
    this.searchStrategy = useBFS 
      ? (pState, pAssumptions) -> searchBFS(pState, pAssumptions)
      : (pState, pAssumptions) -> searchDFS(pState, pAssumptions);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    logger.log(Level.INFO, "Hybrid Execution algorithm started.");

    // first we need to collect all assume edges from the cfa to distinguish between already visited and new paths
    CFATraversal traversal = CFATraversal.dfs();
    EdgeCollectingCFAVisitor edgeCollectingVisitor = new EdgeCollectingCFAVisitor();
    CFANode startingNode = cfa.getMainFunction();
    traversal.traverseOnce(startingNode, edgeCollectingVisitor);
    final Set<CExpression> allAssumptions = extractAssumptions(edgeCollectingVisitor.getVisitedEdges());

    logger.log(Level.FINEST, "Assume edges from program cfa collected.");

    return runInternal(pReachedSet, allAssumptions);
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

  /**
   * Internal implementation of the algorithm
   * @param pReachedSet The current reachedSet
   * @param pAllAssumptions All assumptions occurring in the cfa
   * @return An algorithm status
   */
  private AlgorithmStatus runInternal(ReachedSet pReachedSet, Set<CExpression> pAllAssumptions) 
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException{
    
    // start with good status
    AlgorithmStatus currentStatus = AlgorithmStatus.SOUND_AND_PRECISE;

    boolean running = true;
    int assumptionMissedCounter = 0;

    while(running) {

      currentStatus = algorithm.run(pReachedSet);
      notifyListeners(pReachedSet);

      // get all bottom states (has no children and is not part of the wait list)
      final Set<ARGState> bottomStates = collectBottomStates(pReachedSet);

      // there is nothing left to do in this run
      if(bottomStates.isEmpty()){
        continue;
      }

      // gather all visited assumptions
      Set<CExpression> visitedAssumptions = bottomStates
        .stream()
        .map(argState -> ARGUtils.getOnePathTo(argState)) // TODO: check if we need to adjust this to all states (ARGUtils)
        .map(argPath -> extractAssumptions(argPath.getInnerEdges()))
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());

      // remove the visited assumptions
      pAllAssumptions.removeAll(visitedAssumptions);

      // if there are no more assumptions left, all paths have been covered
      if(pAllAssumptions.isEmpty()) {
        solver.close();
        return currentStatus;
      }

      @Nullable AssumptionContext flipAssumptionContext = null;
      Iterator<ARGState> stateIterator = bottomStates.iterator();

      // we simply continue until either a new assumption to flip was found or there are no more bottom states to work with
      while(flipAssumptionContext == null && stateIterator.hasNext()) {

        // the next state to work on
        ARGState nextBottomState = stateIterator.next();

        // search for the next state to flip
        flipAssumptionContext = searchStrategy.runStrategy(nextBottomState, pAllAssumptions);
      }


      if(flipAssumptionContext == null) {

        if(++assumptionMissedCounter > maxNumMissedAssumption) {
          logger.log(
            Level.INFO, 
            String.format("The maximum number (%d) of runs without finding a new assumption to flip was exceeded. Consider increasing.", maxNumMissedAssumption));
          solver.close();
          return currentStatus;
        }
        continue;
      }

      // 
      ARGPath pathToFoundState = flipAssumptionContext.parentToAssumptionPath;

      // build path formula
      PathFormula pathFormula = buildPathFormula(pathToFoundState.getInnerEdges());

      try {

        boolean satisfiable = !solver.isUnsat(pathFormula.getFormula());

        logger.log(Level.INFO, String.format("The boolean formula %s is not satisfiable for the solver", pathFormula.getFormula()));

        // get assignments for the new path containing the flipped assumption
        if(satisfiable) {

          try(ProverEnvironment proverEnvironment = solver.newProverEnvironment()) {

            // convert all value assignments (their respective formulas) to expressions via FormulaConverter
            Set<CBinaryExpression> assumptions = parseAssignments(proverEnvironment.getModelAssignments());

            /*
             * build a new Hybrid Analysis State:
             * 1) extract the state prior to the changes of the variables (those affecting the flipped assumption)
             * 2) merge this previous state with the new assumptions
             */
            HybridAnalysisState previousState =
                AbstractStates.extractStateByType(
                    flipAssumptionContext.parentState.getWrappedState(),
                    HybridAnalysisState.class);

            HybridAnalysisState newState = previousState.mergeWithArtificialAssignments(assumptions);

            // build an ARGState with this new hybrid analysis state
            ARGState stateToAdd = new ARGState(newState, flipAssumptionContext.parentState);

          } catch(InvalidAutomatonException iae) {
            throw new CPAException("Error occurred while parsing the value assignments into assumption expressions.", iae);
          }

        }

      } catch(SolverException sException) {
        throw new CPAException("Exception occurred in SMT-Solver.", sException);
      }

      // check for continuation
      running = checkContinue(pReachedSet);
    }

    solver.close();
    return currentStatus;
  }

  /**
   * A DFS algorithm implementation for the search strategy to find a new assumption to flip
   */
  private AssumptionContext searchDFS(ARGState pState, Set<CExpression> pAssumptions) {

   throw new NotImplementedException();
  }

  /**
   * A BFS algorithm implementation for the search strategy to find a new assumption to flip
   */
  private AssumptionContext searchBFS(ARGState pState, Set<CExpression> pAssumptions) {
    
    throw new NotImplementedException();
  }

  // builds the complete path formula for a path through the application denoted by the set of edges
  private PathFormula buildPathFormula(Collection<CFAEdge> pEdges)
      throws CPATransferException, InterruptedException {
    PathFormula formula = pathFormulaManager.makeEmptyPathFormula();

    // extract only the assume edges
    Collection<CAssumeEdge> assumeEdges = pEdges
        .stream()
        .filter(edge -> edge != null)
        .filter(edge -> edge.getEdgeType() == CFAEdgeType.AssumeEdge)
        .map(edge -> (CAssumeEdge) edge)
        .collect(Collectors.toList());

    for(CAssumeEdge edge : assumeEdges) {
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

  // helper method to extract assumptions from a given collection of cfa edges
  private Set<CExpression> extractAssumptions(Collection<CFAEdge> pEdges) {
    return pEdges
      .stream()
      .filter(edge -> edge.getEdgeType() == CFAEdgeType.AssumeEdge)
      .map(edge -> ((CAssumeEdge) edge).getExpression())
      .collect(Collectors.toSet());
  }

  /**
   * A bottom state (ARGState) is defined as follows:
   *  1) the state doesn't have children
   *  2) the state is not part of the waitlist
   * 
   * @param pReachedSet The current reached set
   * @return a set of ARGStates that fullfill the conditions mentioned above
   */
  private Set<ARGState> collectBottomStates(ReachedSet pReachedSet) {
    final Collection<AbstractState> waitList = pReachedSet.getWaitlist(); // simplification for the lambda expression
    return pReachedSet
      .asCollection()
      .stream()
      .filter(state -> !waitList.contains(state))
      .map(state -> AbstractStates.extractStateByType(state, ARGState.class))
      .filter(state -> state != null)
      .filter(argState -> argState.getChildren().isEmpty() && !argState.isDestroyed())
      .collect(Collectors.toSet());
  }

  private Set<CBinaryExpression> parseAssignments(Collection<ValueAssignment> pAssignments)
      throws InvalidAutomatonException {

    Set<CBinaryExpression> assumptios = Sets.newHashSet();
    for(ValueAssignment assignment : pAssignments) {
      Collection<CBinaryExpression> assumptionCollection = formulaConverter.convertFormulaToCBinaryExpressions(assignment.getAssignmentAsFormula());
      assumptios.addAll(assumptionCollection);
    }

    return assumptios;
  }

  @FunctionalInterface
  private interface SearchStrategy {

    /**
     * Creates an assumption context starting the search from the given state
     * @param pState The bottom state to start with
     * @param pAssumptions All remaining assumptions to choose from (
     *                     if an assumption is not contained in the set, it has been visited already)
     * @return The new assumption context
     */
    @Nullable
    AssumptionContext runStrategy(ARGState pState, Set<CExpression> pAssumptions);

  }

  /**
   * This class defines the context for an assumption to flip
   *  1) The assumption itself
   *  2) The depending variables
   *  3) The ARGState under which to insert the new state with changed variable values in compliance to the flipped assumption
   *  4) The ARGState containing the assumption
   */
  private static class AssumptionContext {

    private CBinaryExpression assumption;
    private final Set<CExpression> variables;
    private ARGState parentState;
    private ARGPath parentToAssumptionPath;

    /**
     * Constructs a new instance of the class
     * Takes the assumption containing state, extracts the assumption and negates it
     * @param pParentState The arg state previous to the last change of a variable contained within the assumption
     * @param postAssumptionState The ingoing state for the assume edge
     * @param pAssumeEdge The assume edge containing the assumption to flip
     * @throws InvalidAssumptionException The Hybrid Analysis can only work on CBinaryExpressions
     */
    AssumptionContext(CAssumeEdge pAssumeEdge) 
        throws InvalidAssumptionException {

      variables = Sets.newHashSet();

      CExpression assumeExpression = pAssumeEdge.getExpression();

      if(!(assumeExpression instanceof CBinaryExpression)) {
        throw new InvalidAssumptionException(String.format("Assumption contained in assume edge %s is not applicable for hybrid execution.", assumeExpression));
      }

      setAssumption((CBinaryExpression) assumeExpression);
    }

    /**
     * Calculates the 
     * @param pParentState
     * @param pPostAssumptionState
     */
    void calculatePath(ARGState pParentState, ARGState pPostAssumptionState) {

      /*{                           // parent state
       * int x = input();           // function call nondet
       * if(x > 5) {                // assume edge (x > 5)
       *   ...
       * } else {
       *   ...                      // reachable with the flipped assumption !(x > 5)
       * }
      }*/
      

    }

    @Nullable
    ARGState getParentState() {
      return parentState;
    }

    private void setAssumption(CBinaryExpression pAssumption) {
      assumption = ExpressionUtils.invertExpression(pAssumption);
      CExpression leftHandSide = assumption.getOperand1();
      CExpression rightHandSide = assumption.getOperand2();

      if(checkForVariableIdentifier(leftHandSide)) {
        variables.add(leftHandSide);
      }

      if(checkForVariableIdentifier(rightHandSide)) {
        variables.add(rightHandSide);
      }
    }

    private boolean checkForVariableIdentifier(CExpression pCExpression) {
      return pCExpression instanceof CIdExpression || pCExpression instanceof CArraySubscriptExpression;
    }

  }

}
