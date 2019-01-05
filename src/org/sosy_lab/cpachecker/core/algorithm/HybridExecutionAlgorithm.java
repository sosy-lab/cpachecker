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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm.ReachedSetUpdateListener;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm.ReachedSetUpdater;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.hybrid.HybridAnalysisState;
import org.sosy_lab.cpachecker.cpa.hybrid.abstraction.AssumptionSearchStrategy;
import org.sosy_lab.cpachecker.cpa.hybrid.abstraction.AssumptionSearchStrategy.AssumptionContext;
import org.sosy_lab.cpachecker.cpa.hybrid.search.SearchStrategy;
import org.sosy_lab.cpachecker.cpa.hybrid.util.ExpressionUtils;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
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

    @Option(
      secure = true,
      name = "useValueSets",
      description = "Whether to use multiple values on a state.")
    private boolean useValueSets = false;

    private final Algorithm algorithm;
    private final CFA cfa;
    private final LogManager logger;
    private final Configuration configuration;
    private final ShutdownNotifier notifier;

    /**
     *
     * @param pAlgorithm        The composed algorithm created up to this point
     * @param pCFA              The cfa for the code to be checked
     * @param pLogger           The logger instance
     * @param pConfiguration    The applications configuration
     * @param pShutdownNotifier A shutdown notifier
     * @throws InvalidConfigurationException Throws InvalidConfigurationException if injection of
     *                                       the factories options fails
     */
    public HybridExecutionAlgorithmFactory(
        Algorithm pAlgorithm,
        CFA pCFA,
        LogManager pLogger,
        Configuration pConfiguration,
        ShutdownNotifier pShutdownNotifier)
        throws InvalidConfigurationException {
      pConfiguration.inject(this);
      this.algorithm = pAlgorithm;

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
   * negateOrMinimize(assignments(1)):
   * (3) x = -3, y = 1 -> leads to 0
   * here y is not negated, because of precondition being y >= 1
   *
   * with these 3 assigments we catch every path through the method
   *
   * thus, without any further investigation (and runs of the cpa algorithm), we might be able to reach many different states
   * by simply executing the search strategy for all those assignments (maybe even in parallel ...)
   *
   */
  @SuppressWarnings("unused")
  private boolean useValueSets;
  private final List<ReachedSetUpdateListener> reachedSetUpdateListeners;

  private HybridExecutionAlgorithm(
      Algorithm pAlgorithm,
      CFA pCFA,
      LogManager pLogger,
      Configuration pConfiguration,
      ShutdownNotifier pNotifier,
      boolean pUseValueSets)
      throws InvalidConfigurationException {

    this.algorithm = pAlgorithm;
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

    this.pathFormulaManager = new CachingPathFormulaManager(
        new PathFormulaManagerImpl(
            formulaManagerView,
            pConfiguration,
            pLogger,
            pNotifier,
            pCFA,
            AnalysisDirection.FORWARD)
    );
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException {

    logger.log(Level.INFO, "Hybrid Execution algorithm started.");

    // first we need to collect all assume edges from the cfa to distinguish between already visited and new paths
    CFATraversal traversal = CFATraversal.dfs();
    EdgeCollectingCFAVisitor edgeCollectingVisitor = new EdgeCollectingCFAVisitor();
    CFANode startingNode = cfa.getMainFunction();
    traversal.traverseOnce(startingNode, edgeCollectingVisitor);
    final List<CAssumeEdge> allAssumptions = extractAssumptions(edgeCollectingVisitor.getVisitedEdges());

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
  private AlgorithmStatus runInternal(ReachedSet pReachedSet, List<CAssumeEdge> pAllAssumptions)
      throws CPAException, InterruptedException {
    
    // start with good status
    AlgorithmStatus currentStatus = AlgorithmStatus.SOUND_AND_PRECISE;

    while(checkContinue(pReachedSet)) {

      currentStatus = algorithm.run(pReachedSet);
      notifyListeners(pReachedSet);

      final Set<ARGState> allARGStates = collectARGStates(pReachedSet);

      // get all bottom states (has no children and is not part of the wait list)
      final Set<ARGState> bottomStates = allARGStates
          .stream()
          .filter(argState -> argState.getChildren().isEmpty() && !argState.isDestroyed())
          .collect(Collectors.toSet());

      // there is nothing left to do in this run
      if(bottomStates.isEmpty()){
        continue;
      }

      // gather all visited assumptions
      Set<CAssumeEdge> visitedAssumptions = bottomStates
        .stream()
        .map(argState -> ARGUtils.getOnePathTo(argState))
        .map(argPath -> extractAssumptions(argPath.getInnerEdges()))
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());

      // remove the visited assumptions
      pAllAssumptions.removeAll(visitedAssumptions);
      allARGStates.removeAll(bottomStates); // TODO check for problems

      // if there are no more assumptions left, all paths have been covered
      if(pAllAssumptions.isEmpty()) {
        break;
      }

      CAssumeEdge nextAssumptionEdge = pAllAssumptions.get(0);
      CFANode assumptionPredecessor = nextAssumptionEdge.getPredecessor();
      ARGState priorAssumptionState = null;
      Iterator<ARGState> stateIterator = allARGStates.iterator();

      // we simply continue until either the prior state was found or there are no more arg states to work with
      while(priorAssumptionState == null && stateIterator.hasNext()) {

        // the next state to work on
        ARGState nextState = stateIterator.next();

        // check for location
        CFANode stateNode = AbstractStates.extractLocation(nextState);
        if(assumptionPredecessor.equals(stateNode)) {
          priorAssumptionState = nextState;
        }
      }

      assert priorAssumptionState != null;

      // the path from a parent state to the next flip assumption
      ARGPath pathFromAssumption = ARGUtils.getOnePathTo(priorAssumptionState);

      assert priorAssumptionState.equals(pathFromAssumption.getLastState());

      List<CFAEdge> pathEdges = Lists.newArrayList(pathFromAssumption.getInnerEdges());
      pathEdges.add(nextAssumptionEdge);
      List<ARGState> pathStates = pathFromAssumption.asStatesList();
      ARGState parentState = pathStates.get(pathStates.size() - 2);

      assert priorAssumptionState.getParents().contains(parentState);

      // build path formula
      PathFormula pathFormula = buildPathFormula(pathFromAssumption.getInnerEdges());

      try {

        boolean satisfiable = !solver.isUnsat(pathFormula.getFormula());

        // get assignments for the new path containing the flipped assumption
        if(satisfiable) {

          try(ProverEnvironment proverEnvironment = solver.newProverEnvironment()) {

            // convert all value assignments (their respective formulas) to expressions via FormulaConverter
            Set<CBinaryExpression> assumptions = parseAssignments(proverEnvironment.getModelAssignments());

            // extract states from composite state
            CompositeState compositeState = AbstractStates.extractStateByType(priorAssumptionState, CompositeState.class);
            List<AbstractState> statesToWrapp = compositeState.getWrappedStates()
                .stream()
                .filter(state -> !HybridAnalysisState.class.isInstance(state))
                .collect(Collectors.toList());

            HybridAnalysisState previousState =
                AbstractStates.extractStateByType(
                    parentState,
                    HybridAnalysisState.class);

            HybridAnalysisState newState = previousState.mergeWithArtificialAssignments(assumptions);
            statesToWrapp.add(newState);

            // build an ARGState with this new hybrid analysis state
            ARGState stateToAdd = new ARGState(new CompositeState(statesToWrapp), parentState);
            //stateToAdd.forkWithReplacements()
            pReachedSet.add(stateToAdd, SingletonPrecision.getInstance());

          } catch(InvalidAutomatonException iae) {
            throw new CPAException("Error occurred while parsing the value assignments into assumption expressions.", iae);
          }

        } else {
          logger.log(Level.WARNING, String.format("The boolean formula %s is not satisfiable for the solver", pathFormula.getFormula()));
        }

      } catch(SolverException sException) {
        throw new CPAException("Exception occurred in SMT-Solver.", sException);
      }

    }

    return currentStatus;
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

  // helper method to extract assumptions from a given collection of cfa edges
  private List<CAssumeEdge> extractAssumptions(Collection<CFAEdge> pEdges) {
    return pEdges
      .stream()
      .filter(edge -> edge != null)
      .filter(edge -> edge.getEdgeType() == CFAEdgeType.AssumeEdge)
        // we need to invert on false truth assumption, because the information of the inversion gets lost otherwise
      .map(edge -> (CAssumeEdge) edge)
      .collect(Collectors.toList());
  }

  private Set<ARGState> collectARGStates(ReachedSet pReachedSet) {
    return pReachedSet
        .asCollection()
        .stream()
        .map(state -> AbstractStates.extractStateByType(state, ARGState.class))
        .filter(state -> state != null)
        .collect(Collectors.toSet());
  }

  private Set<CBinaryExpression> parseAssignments(Collection<ValueAssignment> pAssignments)
      throws InvalidAutomatonException {

    Set<CBinaryExpression> assumptions = Sets.newHashSet();
    for(ValueAssignment assignment : pAssignments) {
      Collection<CBinaryExpression> assumptionCollection = formulaConverter.convertFormulaToCBinaryExpressions(assignment.getAssignmentAsFormula());
      assumptions.addAll(assumptionCollection);
    }

    assert assumptions.size() == pAssignments.size();

    return assumptions;
  }

}
