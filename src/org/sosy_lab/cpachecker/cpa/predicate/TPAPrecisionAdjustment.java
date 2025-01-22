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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
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

      pathFormula = addFormulaWithPrimeToPathFormula(additionalPredicates, pathFormula);

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

  /**
   * If the precision or set of predicates contain variable with suffix "_prime". The function will add predicates with prime variables to path formula.
   * Example: (x_prime < x), new predicate (x@-1 < x@<ssaIndex>)
   * If the path formula contain predicate with variable transition (x@1 = x@0 + 1) and that variable has prime in precision. Predicate (x@0 < x@1) is added.
   *
   * @param pPredicates
   * @param pPathFormula
   * @return
   */
  private PathFormula addFormulaWithPrimeToPathFormula(
      Set<AbstractionPredicate> pPredicates,
      PathFormula pPathFormula
      ) {
    PathFormula pathFormulaWithPrimeConstraints = pPathFormula;
    SSAMap ssaMap = pPathFormula.getSsa();
    BooleanFormula pathFormula = pPathFormula.getFormula();
    HashMap<String, Integer> variableWithTransition = pathFormulaManager.extractVariablesWithTransition(pPathFormula);
    System.out.println("Precisions: " + pPredicates.toString());

    for (AbstractionPredicate predicate : pPredicates) {
      BooleanFormula predicateTerm = predicate.getSymbolicAtom();

      for (String varNameInPrecisionPredicate : fmgr.extractVariableNames(predicateTerm)) {
        if (varNameInPrecisionPredicate.contains(FormulaManagerView.PRIME_SUFFIX)) { // This predicate from precision has prime variable

          // Check if path formula contain the constrained variable
          String primeVarName = varNameInPrecisionPredicate.replace(FormulaManagerView.PRIME_SUFFIX, "");
          if (ssaMap.allVariables().contains(primeVarName)) { // Path formula has the variable with prime value
            Map<String, Formula> pathFormulaVariables = fmgr.extractVariables(pathFormula);
            // Extract or create new corresponding variable from path formula
            Set<String> allVarNameInPathFormula = fmgr.extractVariableNames(pathFormula);
            Formula varNonPrime =  pathFormulaVariables.get(primeVarName + "@" + ssaMap.getIndex(primeVarName));
            Formula varPrime;
            Formula varWithMinIndex = pathFormulaVariables.get(primeVarName + "@" + variableWithTransition.get(primeVarName));
            if (allVarNameInPathFormula.contains(primeVarName + "@-1")) { // Pathformula has the prime variable
              varPrime = pathFormulaVariables.get(primeVarName + "@-1");
            } else { // TODO: maybe create a set of prime variables to keep tracking without creating new one multiple time
              // checkArgument(index >= 0) in makeName() is commented out for this to pass
              varPrime = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(32), primeVarName, -1);
            }


            // Find which variable comes first in the formula, prime or non-prime variable?
            List<String> variableStringList = new ArrayList<>();
            variableStringList = fmgr.extractVariableOrderToList(predicateTerm, variableStringList);
            System.out.println("Precision variable order: " + variableStringList);
            List<Formula> primeVariableAtomOrder = new ArrayList<>();
            List<Formula> variableHasTransitionAtomOrder = new ArrayList<>();
            // Add variable to list in correct order
            if (variableStringList.get(0).equals(varNameInPrecisionPredicate)) {
              primeVariableAtomOrder.add(varPrime);
              primeVariableAtomOrder.add(varNonPrime);
              if (varWithMinIndex != null) {
                variableHasTransitionAtomOrder.add(varWithMinIndex);
                variableHasTransitionAtomOrder.add(varNonPrime);
              }
            } else {
              primeVariableAtomOrder.add(varNonPrime);
              primeVariableAtomOrder.add(varPrime);
              if (varWithMinIndex != null) {
                variableHasTransitionAtomOrder.add(varNonPrime);
                variableHasTransitionAtomOrder.add(varWithMinIndex);
              }
            }

            // Add prime variable's index to ssaMap
            if (varWithMinIndex != null) {
              System.out.println("Old ssa map: " + ssaMap);
              SSAMapBuilder builder = ssaMap.builder();
              builder.setIndex(varNameInPrecisionPredicate, builder.getType(primeVarName), variableWithTransition.get(primeVarName));
              ssaMap = builder.build();
              System.out.println("New ssa map: " +  ssaMap);
            }

            BooleanFormula newPredicate1 = fmgr.replaceVariableInFormula(predicateTerm, primeVariableAtomOrder);

            if (!variableHasTransitionAtomOrder.isEmpty()) {
              BooleanFormula newPredicate2 = fmgr.replaceVariableInFormula(predicateTerm, variableHasTransitionAtomOrder);
              System.out.println("New predicate 2 to path formula: " + newPredicate2);
              pathFormulaWithPrimeConstraints =
                  pathFormulaManager.makeAndFormulaWithOutInstantiateSsaIndex(pathFormulaWithPrimeConstraints, newPredicate2, ssaMap);
            }

            if (newPredicate1 != null) {
              System.out.println("New predicate 1 to path formula: " + newPredicate1);
              pathFormulaWithPrimeConstraints =
                  pathFormulaManager.makeAndFormulaWithOutInstantiateSsaIndex(pathFormulaWithPrimeConstraints, newPredicate1, ssaMap);
            }
          }
          break;
        }
      }
    }

    System.out.println("Old path formula" + pPathFormula);
    System.out.println("Result of adding prime to path" + pathFormulaWithPrimeConstraints);
    System.out.println("=========================================================================");
    return pathFormulaWithPrimeConstraints;
  }
}
