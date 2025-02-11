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
import java.util.HashSet;
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
import org.sosy_lab.java_smt.api.SolverException;

public class TPAPrecisionAdjustment extends PredicatePrecisionAdjustment {
  private final LogManager logger;
  private final BlockOperator blk;
  private final PredicateAbstractionManager formulaManager;
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
      PredicateAbstractionManager pPredAbsManager,
      PredicateCPAInvariantsManager pInvariantSupplier,
      PredicateProvider pPredicateProvider,
      PredicateStatistics pPredicateStatistics) {
    super(pLogger, pFmgr, pPfmgr, pBlk, pPredAbsManager, pInvariantSupplier, pPredicateProvider, pPredicateStatistics);

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

      if (formulaManager.getOptions().isAddTransitionPredicatesToPrecision()
      && !formulaManager.getAmgr().getVarNameToTransitionPredicates().isEmpty()) {
        List<AbstractionPredicate> satTransitionPredicates = formulaManager.getSatTransitionPredicates(pathFormula);
        additionalPredicates = removeUnsatTransitionPredicates(additionalPredicates, satTransitionPredicates);
        pathFormula = addGeneratedTransitionPredicateToPathFormula(satTransitionPredicates, pathFormula);
      } else {
        pathFormula = addTransitionPredicateToPathFormula(additionalPredicates, pathFormula);
      }


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

  private Set<AbstractionPredicate> removeUnsatTransitionPredicates(
      Set<AbstractionPredicate> pAdditionalPredicates,
      List<AbstractionPredicate> pSatTransitionPredicates) {
    Set<AbstractionPredicate> resultSet = new HashSet<>(pAdditionalPredicates);
    Set<AbstractionPredicate> transPredList =
        new HashSet<>(formulaManager.getAmgr().getVarNameToTransitionPredicates().values());

    for (AbstractionPredicate pred : pAdditionalPredicates) {
      if (!pSatTransitionPredicates.contains(pred) && transPredList.contains(pred)) {
        resultSet.remove(pred);
      }
    }
    return resultSet;
  }

  /**
   * If the precision or set of predicates contain variable with suffix "_prime". The function will add predicates with prime variables to path formula.
   * Example: (x_prime < x), new predicate (x@-1 < x@<ssaIndex>)
   * If the path formula contain predicate with variable transition (x@1 = x@0 + 1) and that variable has prime in precision. Predicate (x@0 < x@1) is added.
   *
   * @param pPredicates Predicates that are going to be used for abstraction
   * @param pPathFormula The path formula to this current location
   * @return New path formula with transition predicates
   */
  private PathFormula addTransitionPredicateToPathFormula(
      Set<AbstractionPredicate> pPredicates,
      PathFormula pPathFormula
  ) {
    PathFormula resultPathFormula = pPathFormula;
    SSAMap ssaMap = pPathFormula.getSsa();
    Map<String, Integer> varNameToMinIdx = pathFormulaManager.extractVariablesWithTransition(pPathFormula);
    final String PRIME_SUFFIX = FormulaManagerView.PRIME_SUFFIX;
    final String PRIME_DEFAULT_IDX = Integer.toString(SSAMap.PRIME_DEFAULT_IDX);

    for (AbstractionPredicate predicate : pPredicates) {
      BooleanFormula predicateTerm = predicate.getSymbolicAtom();
      List<String> varNameListInPredicate = new ArrayList<>(fmgr.extractVariableNames(predicateTerm));
      if (varNameListInPredicate.size() != 2) {
        continue;
      }
      for (int i = 0; i < 2; i++) {
        String varNameInPredicate = varNameListInPredicate.get(i);
        if (varNameInPredicate.contains(PRIME_SUFFIX) && varNameInPredicate.contains(varNameListInPredicate.get(1-i))) { // This predicate is transition predicate
          String varName = varNameInPredicate.replace(PRIME_SUFFIX, "");
          if (ssaMap.allVariables().contains(varName)) { // Path formula has the variable with prime value

            SSAMapBuilder builder = ssaMap.builder();
            builder.setIndexTPA(varName + PRIME_SUFFIX, builder.getType(varName), Integer.parseInt(PRIME_DEFAULT_IDX));
            ssaMap = builder.build();
            BooleanFormula defaultPredicate = fmgr.instantiate(predicateTerm, ssaMap);
            resultPathFormula =
                  pathFormulaManager.makeAndWithInstantiatedFormula(resultPathFormula, defaultPredicate, ssaMap);

            if (varNameToMinIdx.get(varName) != null) {
              builder = ssaMap.builder();
              builder.setIndexTPA(varName + PRIME_SUFFIX, builder.getType(varName), varNameToMinIdx.get(varName));
              ssaMap = builder.build();
              BooleanFormula constrainPredicate = fmgr.instantiate(predicateTerm, ssaMap);
              resultPathFormula =
                  pathFormulaManager.makeAndWithInstantiatedFormula(resultPathFormula,
                      constrainPredicate, ssaMap);
            }
          }
          break; // One predicate should only have one prime
        }
      }
    }
    return resultPathFormula;
  }

  private PathFormula addGeneratedTransitionPredicateToPathFormula(
      List<AbstractionPredicate> satTransitionPredicates,
      PathFormula pPathFormula
  ) {
    PathFormula resultPathFormula = pPathFormula;
    SSAMap ssaMap = pPathFormula.getSsa();
    Map<String, Integer> varNameToMinIdx = pathFormulaManager.extractVariablesWithTransition(pPathFormula);
    final String PRIME_SUFFIX = FormulaManagerView.PRIME_SUFFIX;

    for (AbstractionPredicate predicate : satTransitionPredicates) {
      BooleanFormula predicateTerm = predicate.getSymbolicAtom();
      Set<String> varNames = fmgr.extractVariableNames(predicateTerm);
      for (String varName : varNames) {
        if (!varName.contains(PRIME_SUFFIX)) {
          SSAMapBuilder builder = ssaMap.builder();
          if (varNameToMinIdx.get(varName) != null) {
            builder.setIndexTPA(varName + PRIME_SUFFIX, builder.getType(varName), varNameToMinIdx.get(varName));
            ssaMap = builder.build();
            resultPathFormula =
                pathFormulaManager.makeAndWithInstantiatedFormula(
                    resultPathFormula,
                    fmgr.instantiate(predicateTerm, ssaMap),
                    ssaMap);
          } else {
            builder.setIndexTPA(varName + PRIME_SUFFIX, builder.getType(varName), ssaMap.getIndex(varName));
            ssaMap = builder.build();
            resultPathFormula =
                pathFormulaManager.makeAndWithInstantiatedFormula(
                    resultPathFormula,
                    fmgr.instantiate(predicateTerm, ssaMap),
                    ssaMap);
          }
          break;
        }
      }
    }
    return resultPathFormula;
  }
}
