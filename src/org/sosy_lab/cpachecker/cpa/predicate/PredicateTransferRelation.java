// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.mkInfeasibleDummyState;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.InfeasibleDummyState;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Transfer relation for symbolic predicate abstraction. First it computes the strongest post for
 * the given CFA edge. Afterwards it optionally computes an abstraction.
 */
public final class PredicateTransferRelation extends SingleEdgeTransferRelation {

  private final LogManager logger;
  private final PredicateAbstractionManager formulaManager;
  private final PathFormulaManager pathFormulaManager;

  private final BlockOperator blk;
  private final FormulaManagerView fmgr;

  private final AnalysisDirection direction;
  private final PredicateStatistics statistics;
  private final PredicateCpaOptions options;

  private final TimerWrapper postTimer;
  private final TimerWrapper satCheckTimer;
  private final TimerWrapper pathFormulaTimer;
  private final TimerWrapper strengthenTimer;
  private final TimerWrapper strengthenCheckTimer;
  private final TimerWrapper abstractionCheckTimer;

  public PredicateTransferRelation(
      LogManager pLogger,
      AnalysisDirection pDirection,
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      BlockOperator pBlk,
      PredicateAbstractionManager pPredAbsManager,
      PredicateStatistics pStatistics,
      PredicateCpaOptions pOptions) {
    logger = pLogger;
    formulaManager = pPredAbsManager;
    pathFormulaManager = pPfmgr;
    fmgr = pFmgr;
    blk = pBlk;
    direction = pDirection;
    statistics = pStatistics;
    options = pOptions;

    postTimer = statistics.postTimer.getNewTimer();
    satCheckTimer = statistics.satCheckTimer.getNewTimer();
    pathFormulaTimer = statistics.pathFormulaTimer.getNewTimer();
    strengthenTimer = statistics.strengthenTimer.getNewTimer();
    strengthenCheckTimer = statistics.strengthenCheckTimer.getNewTimer();
    abstractionCheckTimer = statistics.abstractionCheckTimer.getNewTimer();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge edge)
      throws CPATransferException, InterruptedException {

    postTimer.start();
    try {
      PredicateAbstractState element = (PredicateAbstractState) pElement;

      // Check whether abstraction is false.
      // Such elements might get created when precision adjustment computes an abstraction.
      if (element.getAbstractionFormula().isFalse()) {
        return ImmutableSet.of();
      }

      // calculate strongest post
      PathFormula pathFormula = convertEdgeToPathFormula(element.getPathFormula(), edge);
      logger.log(Level.ALL, "New path formula is", pathFormula);

      // Check whether we should do a SAT check.s
      boolean satCheck = shouldDoSatCheck(edge, pathFormula);
      logger.log(
          Level.FINEST,
          "Handling non-abstraction location",
          (satCheck ? "with satisfiability check" : ""));

      try {
        if (satCheck && unsatCheck(element.getAbstractionFormula(), pathFormula)) {
          return ImmutableSet.of();
        }
      } catch (SolverException e) {
        throw new CPATransferException("Solver failed during successor generation", e);
      }

      PredicateAbstractState successor;
      if (element.isAbstractionState()) {
        successor = mkNonAbstractionStateWithNewPathFormula(pathFormula, element, element);
      } else {
        successor =
            mkNonAbstractionStateWithNewPathFormula(
                pathFormula, element, element.getPreviousAbstractionState());
      }
      return Collections.singleton(successor);

    } finally {
      postTimer.stop();
    }
  }

  private boolean shouldDoSatCheck(CFAEdge edge, PathFormula pathFormula) {
    if ((options.getSatCheckBlockSize() > 0)
        && (pathFormula.getLength() >= options.getSatCheckBlockSize())) {
      return true;
    }
    if (options.satCheckAtAbstraction()) {
      CFANode loc = getAnalysisSuccessor(edge);
      if (blk.isBlockEnd(loc, pathFormula.getLength())) {
        return true;
      }
    }
    return false;
  }

  private CFANode getAnalysisSuccessor(CFAEdge pEdge) {
    if (direction == AnalysisDirection.BACKWARD) {
      return pEdge.getPredecessor();
    } else {
      return pEdge.getSuccessor();
    }
  }

  /**
   * Checks if lastAbstraction & pathFromLastAbstraction is unsat. Collects sat check information
   * for statistics
   */
  private boolean unsatCheck(
      final AbstractionFormula lastAbstraction, final PathFormula pathFormulaFromLastAbstraction)
      throws SolverException, InterruptedException {
    satCheckTimer.start();

    boolean unsat = formulaManager.unsat(lastAbstraction, pathFormulaFromLastAbstraction);

    satCheckTimer.stop();

    if (unsat) {
      statistics.numSatChecksFalse.inc();
      logger.log(Level.FINEST, "Abstraction & PathFormula is unsatisfiable.");
    }

    return unsat;
  }

  /**
   * Converts an edge into a formula and creates a conjunction of it with the previous pathFormula.
   *
   * <p>This method implements the strongest post operator.
   *
   * @param pathFormula The previous pathFormula.
   * @param edge The edge to analyze.
   * @return The new pathFormula.
   */
  private PathFormula convertEdgeToPathFormula(PathFormula pathFormula, CFAEdge edge)
      throws CPATransferException, InterruptedException {
    pathFormulaTimer.start();
    try {
      // compute new pathFormula with the operation on the edge
      return pathFormulaManager.makeAnd(pathFormula, edge);
    } finally {
      pathFormulaTimer.stop();
    }
  }

  /*
  * Here is some code that checks memory safety properties with predicate analysis.
  * It used two configuration flags to enable these checks,
  * and relied on PredicateAbstractState to implement Targetable.
  * This is both not desired (especially the former),
  * since specifications should not be hard-coded in analysis,
  * but instead given as automata.
  * Furthermore, these checks were too expensive to be usable.
  * Thus this code is disabled now.
  * If it is one day desired to re-add these checks,
  * the checks should get executed on request of the AutomatonCPA,
  * possibly via the AbstractQueryableState interface or strengthen.

     Pair<PathFormula, ErrorConditions> edgeResult;
     pathFormulaTimer.start();
     try {
       edgeResult = pathFormulaManager.makeAndWithErrorConditions(element.getPathFormula(), edge);
     } finally {
       pathFormulaTimer.stop();
     }

     PathFormula pathFormula = edgeResult.getFirst();
     ErrorConditions conditions = edgeResult.getSecond();

     // check whether to do abstraction
     boolean doAbstraction = blk.isBlockEnd(edge, pathFormula);

     BooleanFormula invalidDerefCondition = conditions.getInvalidDerefCondition();
     BooleanFormula invalidFreeCondition = conditions.getInvalidFreeCondition();

     if (bfmgr.isTrue(invalidDerefCondition)) {
       return createState(element, pathFormula, loc, doAbstraction, ViolatedProperty.VALID_DEREF);
     }
     if (bfmgr.isTrue(invalidFreeCondition)) {
       return createState(element, pathFormula, loc, doAbstraction, ViolatedProperty.VALID_FREE);
     }

     List<PredicateAbstractState> newStates = new ArrayList<>(2);

     if (checkValidDeref && !bfmgr.isFalse(invalidDerefCondition)) {
       logger.log(Level.ALL, "Adding invalid-deref condition", invalidDerefCondition);
       PathFormula targetPathFormula = pathFormulaManager.makeAnd(edgeResult.getFirst(), invalidDerefCondition);
       newStates.addAll(createState(element, targetPathFormula, loc, doAbstraction,
           ViolatedProperty.VALID_DEREF));

       pathFormula = pathFormulaManager.makeAnd(pathFormula,
           bfmgr.not(invalidDerefCondition));
     }

     if (checkValidFree && !bfmgr.isFalse(invalidFreeCondition)) {
       logger.log(Level.ALL, "Adding invalid-free condition", invalidFreeCondition);
       PathFormula targetPathFormula = pathFormulaManager.makeAnd(edgeResult.getFirst(), invalidFreeCondition);
       newStates.addAll(createState(element, targetPathFormula, loc, doAbstraction,
           ViolatedProperty.VALID_FREE));

       pathFormula = pathFormulaManager.makeAnd(pathFormula,
           bfmgr.not(invalidFreeCondition));
     }
  */

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement,
      Iterable<AbstractState> otherElements,
      CFAEdge edge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {

    strengthenTimer.start();
    try {

      PredicateAbstractState element = (PredicateAbstractState) pElement;
      if (element.isAbstractionState()) {
        // can't do anything with this object because the path formula of
        // abstraction elements has to stay "true"
        return Collections.singleton(element);
      }

      // TODO: replace with
      // Iterables.getOnlyElement(AbstractStates.extractLocations(otherElements));
      // when the special case for PredicateCPA in CompositeTransferRelation#callStrengthen
      // is removed.
      final CFANode currentLocation;
      if (edge == null) {
        currentLocation = null;
      } else {
        currentLocation = getAnalysisSuccessor(edge);
      }

      boolean errorFound = false;
      for (AbstractState lElement : otherElements) {
        if (lElement instanceof AssumptionStorageState) {
          element = strengthen(element, (AssumptionStorageState) lElement);
        }

        if (lElement instanceof ThreadingState) {
          element = strengthen(element, (ThreadingState) lElement);
        }

        /*
         * Add additional assumptions from an automaton state.
         */
        if (!options.ignoreStateAssumptions() && lElement instanceof AbstractStateWithAssumptions) {
          element = strengthen(element, (AbstractStateWithAssumptions) lElement, edge);
          if (element instanceof InfeasibleDummyState) {
            return ImmutableSet.of(element);
          }
        }

        if (options.strengthenWithFormulaReportingStates()
            && lElement instanceof FormulaReportingState) {
          element = strengthen(element, (FormulaReportingState) lElement);
        }

        if (AbstractStates.isTargetState(lElement)) {
          errorFound = true;
        }
      }

      // check satisfiability in case of error
      // (not necessary for abstraction elements)
      if (errorFound && options.targetStateSatCheck()) {
        element = strengthenSatCheck(element, currentLocation);
        if (element == null) {
          // successor not reachable
          return ImmutableSet.of();
        }
      }

      return Collections.singleton(element);
    } catch (SolverException e) {
      throw new CPATransferException("Solver failed during strengthen sat check", e);

    } finally {
      strengthenTimer.stop();
    }
  }

  private PredicateAbstractState strengthen(
      PredicateAbstractState pElement, AbstractStateWithAssumptions pAssumeElement, CFAEdge pEdge)
      throws CPATransferException, InterruptedException, SolverException {

    PathFormula pf = pElement.getPathFormula();

    if (options.assumptionStrengtheningSatCheck()) {
      PathFormula f = pathFormulaManager.makeFormulaForPath(Collections.singletonList(pEdge));
      for (CExpression assumption :
          from(pAssumeElement.getAssumptions()).filter(CExpression.class)) {
        f = pathFormulaManager.makeAnd(f, assumption);
      }
      AbstractionFormula dummy = formulaManager.makeTrueAbstractionFormula(f);
      if (formulaManager.unsat(dummy, f)) {
        // if automaton has conflict with edge, return a dummy-successor that can be used to further
        // elaborate on it at a later stage
        return mkInfeasibleDummyState(f, dummy, pElement.getAbstractionLocationsOnPath());
      }
    }

    for (CExpression assumption : from(pAssumeElement.getAssumptions()).filter(CExpression.class)) {
      // assumptions do not contain complete type nor scope information
      // hence, not all types can be resolved, so ignore these
      // TODO: the witness automaton is complete in that regard, so use that in future
      if (CFAUtils.getIdExpressionsOfExpression(assumption)
              .anyMatch(var -> var.getExpressionType() instanceof CProblemType)
          || assumption.getExpressionType() instanceof CProblemType) {
        logger.log(Level.INFO, "Ignoring assumption", assumption, "because of CProblemType");
        continue;
      }
      pathFormulaTimer.start();
      try {
        // compute new pathFormula with the operation on the edge
        pf = pathFormulaManager.makeAnd(pf, assumption);
      } finally {
        pathFormulaTimer.stop();
      }
    }

    if (pf != pElement.getPathFormula()) {
      return replacePathFormula(pElement, pf);
    } else {
      return pElement;
    }
  }

  private PredicateAbstractState strengthen(
      PredicateAbstractState pElement, AssumptionStorageState pElement2) {

    if (pElement2.isAssumptionTrue() || pElement2.isAssumptionFalse()) {
      // we don't add the assumption false in order to not forget the content of the path formula
      // (we need it for post-processing)
      return pElement;
    }

    String asmpt = pElement2.getAssumptionAsString().toString();

    PathFormula pf = pathFormulaManager.makeAnd(pElement.getPathFormula(), fmgr.parse(asmpt));

    return replacePathFormula(pElement, pf);
  }

  private PredicateAbstractState strengthen(
      PredicateAbstractState pState, ThreadingState pThreadingState)
      throws CPATransferException, InterruptedException {
    FunctionCallEdge function = pThreadingState.getEntryFunction();
    if (function == null) {
      return pState;
    }
    PathFormula pathFormula = convertEdgeToPathFormula(pState.getPathFormula(), function);
    return replacePathFormula(pState, pathFormula);
  }

  private PredicateAbstractState strengthen(
      PredicateAbstractState pElement, FormulaReportingState pFormulaReportingState) {

    BooleanFormula formula = pFormulaReportingState.getFormulaApproximation(fmgr);

    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    if (bfmgr.isTrue(formula) || bfmgr.isFalse(formula)) {
      return pElement;
    }

    PathFormula previousPathFormula = pElement.getPathFormula();
    PathFormula newPathFormula = pathFormulaManager.makeAnd(previousPathFormula, formula);

    return replacePathFormula(pElement, newPathFormula);
  }

  /** Returns a new state with a given pathFormula. All other fields stay equal. */
  private PredicateAbstractState replacePathFormula(
      PredicateAbstractState oldElement, PathFormula newPathFormula) {
    assert !oldElement.isAbstractionState();
    return mkNonAbstractionStateWithNewPathFormula(newPathFormula, oldElement);
  }

  private PredicateAbstractState strengthenSatCheck(PredicateAbstractState pElement, CFANode loc)
      throws SolverException, InterruptedException {
    logger.log(Level.FINEST, "Checking for feasibility of path because error has been found");

    strengthenCheckTimer.start();
    PathFormula pathFormula = pElement.getPathFormula();
    boolean unsat = formulaManager.unsat(pElement.getAbstractionFormula(), pathFormula);
    strengthenCheckTimer.stop();

    if (unsat) {
      statistics.numStrengthenChecksFalse.inc();
      logger.log(Level.FINEST, "Path is infeasible.");
      return null;
    } else {
      // although this is not an abstraction location, we fake an abstraction
      // because refinement code expects it to be like this
      logger.log(Level.FINEST, "Last part of the path is not infeasible.");

      // set abstraction to true (we don't know better)
      AbstractionFormula abs = formulaManager.makeTrueAbstractionFormula(pathFormula);

      PathFormula newPathFormula =
          pathFormulaManager.makeEmptyPathFormulaWithContextFrom(pathFormula);

      // update abstraction locations map
      PersistentMap<CFANode, Integer> abstractionLocations =
          pElement.getAbstractionLocationsOnPath();
      Integer newLocInstance = abstractionLocations.getOrDefault(loc, 0) + 1;
      abstractionLocations = abstractionLocations.putAndCopy(loc, newLocInstance);

      return PredicateAbstractState.mkAbstractionState(newPathFormula, abs, abstractionLocations);
    }
  }

  boolean areAbstractSuccessors(
      AbstractState pElement,
      CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors,
      Map<PredicateAbstractState, PathFormula> computedPathFormulae)
      throws SolverException, CPATransferException, InterruptedException {
    PredicateAbstractState predicateElement = (PredicateAbstractState) pElement;
    PathFormula pathFormula = computedPathFormulae.get(predicateElement);
    if (pathFormula == null) {
      pathFormula =
          pathFormulaManager.makeEmptyPathFormulaWithContextFrom(predicateElement.getPathFormula());
    }
    boolean result = true;

    if (pSuccessors.isEmpty()) {
      // if pSuccessors is empty than successor formula needs to be unsat
      PathFormula pFormula = convertEdgeToPathFormula(pathFormula, pCfaEdge);
      if (!unsatCheck(predicateElement.getAbstractionFormula(), pFormula)) {
        result = false;
      }
      return result;
    }

    for (AbstractState e : pSuccessors) {
      PredicateAbstractState successor = (PredicateAbstractState) e;

      if (successor.isAbstractionState()) {
        pathFormula = convertEdgeToPathFormula(pathFormula, pCfaEdge);
        // check abstraction
        abstractionCheckTimer.start();
        if (!formulaManager.checkCoverage(
            predicateElement.getAbstractionFormula(),
            pathFormula,
            successor.getAbstractionFormula())) {
          result = false;
        }
        abstractionCheckTimer.stop();
      } else {
        // check abstraction
        abstractionCheckTimer.start();
        if (!successor.getAbstractionFormula().equals(predicateElement.getAbstractionFormula())) {
          result = false;
        }
        abstractionCheckTimer.stop();

        // compute path formula
        PathFormula computedPathFormula = convertEdgeToPathFormula(pathFormula, pCfaEdge);
        PathFormula mergeWithPathFormula = computedPathFormulae.get(successor);
        if (mergeWithPathFormula != null) {
          computedPathFormulae.put(
              successor, pathFormulaManager.makeOr(mergeWithPathFormula, computedPathFormula));
        } else {
          computedPathFormulae.put(successor, computedPathFormula);
        }
      }
    }

    return result;
  }
}
