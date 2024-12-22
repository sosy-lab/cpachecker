// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormulaTPA;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.SolverException;

public class TPAPrecisionAdjustment implements PrecisionAdjustment {
  private final LogManager logger;
  private final BlockOperator blk;
  private final TPAPredicateAbstractionManager formulaManager;
  private final PathFormulaManager pathFormulaManager;
  private final FormulaManagerView fmgr;

  private final PredicateCPAInvariantsManager invariants;
  private final PredicateProvider predicateProvider;
  private final PredicateStatistics statistics;
  private final TimerWrapper totalPrecTime;
  private final TimerWrapper computingAbstractionTime;
  public TPAPrecisionAdjustment(
      LogManager pLogger,
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      BlockOperator pBlk,
      TPAPredicateAbstractionManager pPredAbsManager,
      PredicateCPAInvariantsManager pInvariantSupplier,
      PredicateProvider pPredicateProvider,
      PredicateStatistics pPredicateStatistics) {

    logger = pLogger;
    fmgr = pFmgr;
    pathFormulaManager = pPfmgr;
    blk = pBlk;
    formulaManager = pPredAbsManager;

    invariants = pInvariantSupplier;
    predicateProvider = pPredicateProvider;
    statistics = pPredicateStatistics;
    totalPrecTime = statistics.totalPrecTime.getNewTimer();
    computingAbstractionTime = statistics.computingAbstractionTime.getNewTimer();
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement,
      Precision pPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState)
      throws CPAException, InterruptedException {

    totalPrecTime.start();
    try {
      PredicateAbstractState element = (PredicateAbstractState) pElement;

      // default number of locations is 1, for concurrent programs we can have multiple locations.
      // if any location wants to abstract, we compute the abstraction
      final Collection<CFANode> locations =
          ImmutableList.copyOf(AbstractStates.extractLocations(fullState));
      for (CFANode location : locations) {
        if (shouldComputeAbstraction(fullState, location, element)) {
          PredicatePrecision precision = (PredicatePrecision) pPrecision;
          return computeAbstraction(element, precision, locations, fullState);
        }
      }

      return Optional.of(
          new PrecisionAdjustmentResult(
              element, pPrecision, PrecisionAdjustmentResult.Action.CONTINUE));

    } catch (SolverException e) {
      throw new CPAException("Solver Failure: " + e.getMessage(), e);
    } finally {
      totalPrecTime.stop();
    }
  }

  private boolean shouldComputeAbstraction(
      AbstractState fullState, CFANode location, PredicateAbstractState predicateState) {
    if (predicateState.isAbstractionState()) {
      return false;
    }
    if (blk.isBlockEnd(location, predicateState.getPathFormula().getLength())) {
      return true;
    }
    if (AbstractStates.isTargetState(fullState)) {
      statistics.numTargetAbstractions.inc();
      return true;
    }
    return false;
  }

  private Optional<PrecisionAdjustmentResult> computeAbstraction(
      PredicateAbstractState element,
      PredicatePrecision precision,
      Collection<CFANode> pLocations,
      AbstractState fullState)
      throws SolverException, CPAException, InterruptedException {

    AbstractionFormula abstractionFormula = element.getAbstractionFormula();
    PersistentMap<CFANode, Integer> abstractionLocations = element.getAbstractionLocationsOnPath();
    PathFormula pathFormula = element.getPathFormula();
    Optional<CallstackStateEqualsWrapper> callstackWrapper =
        AbstractStates.extractOptionalCallstackWraper(fullState);

    statistics.numAbstractions.inc();
    logger.log(Level.FINEST, "Computing abstraction at node", pLocations, "in path.");

    statistics.blockSize.setNextValue(pathFormula.getLength());

    // update/get invariants and add them, the need to be instantiated
    // (we do only update global invariants (computed by a parallelalgorithm) here
    // as everything else can only be computed during refinement)
    invariants.updateGlobalInvariants();

    final List<BooleanFormula> invariantFormulas = new ArrayList<>();
    for (CFANode loc : pLocations) {
      if (invariants.appendToPathFormula()) {
        BooleanFormula invariant =
            fmgr.instantiate(
                invariants.getInvariantFor(
                    loc, callstackWrapper, fmgr, pathFormulaManager, pathFormula),
                pathFormula.getSsa());
        invariantFormulas.add(invariant);
      }
    }
    final BooleanFormula invariant = fmgr.getBooleanFormulaManager().and(invariantFormulas);

    // we don't want to add trivially true invariants
    if (!fmgr.getBooleanFormulaManager().isTrue(invariant)) {
      pathFormula = pathFormulaManager.makeAnd(pathFormula, invariant);
    }

    // get additional predicates
    Set<AbstractionPredicate> additionalPredicates = predicateProvider.getPredicates(fullState);
    AbstractionFormula newAbstractionFormula = null;

    // compute new abstraction
    computingAbstractionTime.start();
    try {
      for (CFANode loc : pLocations) {
        Integer newLocInstance = abstractionLocations.getOrDefault(loc, 0) + 1;
        additionalPredicates.addAll(precision.getPredicates(loc, newLocInstance));
        // update abstraction locations map
        abstractionLocations = abstractionLocations.putAndCopy(loc, newLocInstance);
      }

      pathFormula = addPredicateWithPrimeToPathFormula(additionalPredicates, pathFormula);

      // compute a new abstraction with a precision based on `preds`
      newAbstractionFormula =
          formulaManager.buildAbstractionForTPA(
              pLocations, callstackWrapper, abstractionFormula, pathFormula, additionalPredicates);
    } finally {
      computingAbstractionTime.stop();
    }

    // if the abstraction is false, return bottom (represented by empty set)
    if (newAbstractionFormula.isFalse()) {
      statistics.numAbstractionsFalse.inc();
      logger.log(Level.FINEST, "Abstraction is false, node is not reachable");
      return Optional.empty();
    }

    // create new empty path formula
    PathFormula newPathFormula =
        pathFormulaManager.makeEmptyPathFormulaWithContextFrom(pathFormula);

    // initialize path formula with current invariants
    // we don't want to add trivially true invariants
    if (!fmgr.getBooleanFormulaManager().isTrue(invariant)) {
      newPathFormula = pathFormulaManager.makeAnd(newPathFormula, invariant);
    }

    PredicateAbstractState state =
        PredicateAbstractState.mkAbstractionState(
            newPathFormula,
            newAbstractionFormula,
            abstractionLocations,
            element.getPreviousAbstractionState());
    return Optional.of(
        new PrecisionAdjustmentResult(state, precision, PrecisionAdjustmentResult.Action.CONTINUE));
  }

  private PathFormula addPredicateWithPrimeToPathFormula(
      Set<AbstractionPredicate> pPredicates,
      PathFormula pPathFormula
      ) {
    PathFormula pathFormulaWithPrimeConstraints = pPathFormula;
    SSAMap ssaMap = pPathFormula.getSsa();

    for (AbstractionPredicate predicate : pPredicates) {
      BooleanFormula predicateSymbolicAtom = predicate.getSymbolicAtom();

      for (String varNameInPrecisionPredicate : fmgr.extractVariableNames(predicateSymbolicAtom)) {
        if (varNameInPrecisionPredicate.contains("_prime")) { // This predicate from precision has prime variable

          // Check if path formula contain the constrained variable
          String varName = varNameInPrecisionPredicate.replace("_prime", "");
          if (ssaMap.allVariables().contains(varName)) { // Path formula has the variable with prime value
            int ssaIndex = ssaMap.getIndex(varName);

            // Extract corresponding variable from path formula
            Formula var =  fmgr.extractVariables(pPathFormula.getFormula()).get(varName + "@" + ssaIndex);
            Set<String> allVarNameInPathFormula = fmgr.extractVariableNames(pPathFormula.getFormula());
            Formula varPrime;
            if (allVarNameInPathFormula.contains(varName + "@-1")) { // Pathformula has the prime variable
              // TODO: Check Pathformula has the same predicate which is adding into
              varPrime = fmgr.extractVariables(pPathFormula.getFormula()).get(varName + "@-1");
            } else { // TODO: maybe create a set of prime variables to keep tracking without creating new one multiple time
              // TODO: handle the exception thrown when index <0 in make name
              varPrime = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(32), varName, -1);
            }

            // TODO: Find which variable comes first in the formula, prime or non-prime variable?

            // TODO: Signed and Unsigned?
            BooleanFormula newConstraint;
            if (fmgr.isNotFormula(predicateSymbolicAtom)) { // If is not formula then reverse make operation between 2 variable
              BooleanFormula negatedAtom = fmgr.stripNegation2(predicateSymbolicAtom);
              FunctionDeclarationKind funcKind = fmgr.extractFunctionDeclarationKind(negatedAtom);
              newConstraint = switch (funcKind) {
                case BV_SLT -> fmgr.makeGreaterOrEqual(varPrime, var, true);
                case BV_ULT -> fmgr.makeGreaterOrEqual(varPrime, var, false);
                case BV_SGT -> fmgr.makeLessOrEqual(varPrime, var, true);
                case BV_UGT -> fmgr.makeLessOrEqual(varPrime, var, false);
                case BV_SLE -> fmgr.makeGreaterThan(varPrime, var, false);
                case BV_ULE -> fmgr.makeGreaterThan(varPrime, var, true);
                case BV_SGE -> fmgr.makeLessThan(varPrime, var, false);
                case BV_UGE -> fmgr.makeLessThan(varPrime, var, true);
                case BV_EQ -> fmgr.makeEqual(varPrime, var);
                default -> null;
              };
            } else {
              FunctionDeclarationKind funcKind = fmgr.extractFunctionDeclarationKind(predicateSymbolicAtom);
              newConstraint = switch (funcKind) {
                case BV_SLT -> fmgr.makeLessThan(varPrime, var, false);
                case BV_ULT -> fmgr.makeLessThan(varPrime, var, true);
                case BV_SGT -> fmgr.makeGreaterThan(varPrime, var, false);
                case BV_UGT -> fmgr.makeGreaterThan(varPrime, var, true);
                case BV_SLE -> fmgr.makeLessOrEqual(varPrime, var, true);
                case BV_ULE -> fmgr.makeLessOrEqual(varPrime, var, false);
                case BV_SGE -> fmgr.makeGreaterOrEqual(varPrime, var, true);
                case BV_UGE -> fmgr.makeGreaterOrEqual(varPrime, var, false);
                case BV_EQ -> fmgr.makeEqual(varPrime, var);
                default -> null;
              };
            }
            System.out.println("Var prime: " + varPrime.toString());
            if (newConstraint != null) {
              System.out.println("New predicate to path formula: " + newConstraint);
              pathFormulaWithPrimeConstraints = pathFormulaManager.makeAndFormulaWithSsaIndex(pathFormulaWithPrimeConstraints, newConstraint);
            }
          }
          break;
        }
      }
    }
    System.out.println("Old path formula" + pPathFormula);
    System.out.println("Result of adding prime to path" + pathFormulaWithPrimeConstraints);
    return pPathFormula;
  }
}
