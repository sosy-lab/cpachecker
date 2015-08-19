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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.ComputeAbstractionState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

public class PredicatePrecisionAdjustment implements PrecisionAdjustment {

  // statistics
  final Timer totalPrecTime = new Timer();
  final Timer computingAbstractionTime = new Timer();

  int numAbstractions = 0;
  int numAbstractionsFalse = 0;
  int maxBlockSize = 0;

  private final LogManager logger;
  private final PredicateAbstractionManager formulaManager;
  private final PathFormulaManager pathFormulaManager;
  private final FormulaManagerView fmgr;

  private @Nullable InvariantGenerator invariantGenerator;
  private InvariantSupplier invariants;

  public PredicatePrecisionAdjustment(PredicateCPA pCpa,
      InvariantGenerator pInvariantGenerator) {

    logger = pCpa.getLogger();
    formulaManager = pCpa.getPredicateManager();
    pathFormulaManager = pCpa.getPathFormulaManager();
    fmgr = pCpa.getSolver().getFormulaManager();

    invariantGenerator = checkNotNull(pInvariantGenerator);
    invariants = InvariantSupplier.TrivialInvariantSupplier.INSTANCE;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement,
      Precision pPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState) throws CPAException, InterruptedException {

    totalPrecTime.start();
    try {
      PredicateAbstractState element = (PredicateAbstractState)pElement;

      if (element instanceof ComputeAbstractionState) {
        PredicatePrecision precision = (PredicatePrecision)pPrecision;

        return computeAbstraction((ComputeAbstractionState)element, precision);
      } else {
        return Optional.of(PrecisionAdjustmentResult.create(
            element, pPrecision, PrecisionAdjustmentResult.Action.CONTINUE));
      }


    } finally {
      totalPrecTime.stop();
    }
  }

  /**
   * Compute an abstraction.
   */
  private Optional<PrecisionAdjustmentResult> computeAbstraction(
      ComputeAbstractionState element,
      PredicatePrecision precision) throws CPAException, InterruptedException {

    AbstractionFormula abstractionFormula = element.getAbstractionFormula();
    PersistentMap<CFANode, Integer> abstractionLocations = element.getAbstractionLocationsOnPath();
    PathFormula pathFormula = element.getPathFormula();
    CFANode loc = element.getLocation();
    Integer newLocInstance = firstNonNull(abstractionLocations.get(loc), 0) + 1;

    numAbstractions++;
    logger.log(Level.FINEST, "Computing abstraction at instance", newLocInstance, "of node", loc, "in path.");

    maxBlockSize = Math.max(maxBlockSize, pathFormula.getLength());

    // get invariants and add them
    extractInvariants();
    BooleanFormula invariant = invariants.getInvariantFor(loc, fmgr, pathFormulaManager);
    if (invariant != null) {
      pathFormula = pathFormulaManager.makeAnd(pathFormula, invariant);
    }

    AbstractionFormula newAbstractionFormula = null;

    // compute new abstraction
    computingAbstractionTime.start();
    try {
      Set<AbstractionPredicate> preds = precision.getPredicates(loc, newLocInstance);

      // compute a new abstraction with a precision based on `preds`
      newAbstractionFormula = formulaManager.buildAbstraction(
          loc, abstractionFormula, pathFormula, preds);
    } finally {
      computingAbstractionTime.stop();
    }

    for (BooleanFormula constraint : element.getConstraints()) {
      // add constraint to the current abstraction, such that it can be used for further steps in the analysis.
      // We manually set the SSA-indices for the constraint with the indices available in the current (new) abstractionFormula.
      // TODO: check if we use identical BDD-nodes for identical atoms for different formulas
      Region abs = formulaManager.buildRegionFromFormulaWithUnknownAtoms(constraint);
      BooleanFormula symbolicAbs = constraint;
      BooleanFormula instantiatedSymbolicAbs = fmgr.instantiate(constraint, newAbstractionFormula.getBlockFormula().getSsa());
      PathFormula blockFormula = newAbstractionFormula.getBlockFormula(); // has to be equal for 'makeAnd'

      AbstractionFormula constraintAbs = new AbstractionFormula(
          fmgr, abs, symbolicAbs, instantiatedSymbolicAbs, blockFormula, ImmutableSet.<Integer>of());

      newAbstractionFormula = formulaManager.makeAnd(constraintAbs, newAbstractionFormula);
    }

    // if the abstraction is false, return bottom (represented by empty set)
    if (newAbstractionFormula.isFalse()) {
      numAbstractionsFalse++;
      logger.log(Level.FINEST, "Abstraction is false, node is not reachable");
      return Optional.absent();
    }

    // create new empty path formula
    PathFormula newPathFormula = pathFormulaManager.makeEmptyPathFormula(pathFormula);

    // initialize path formula with current invariants
    if (invariant != null) {
      newPathFormula = pathFormulaManager.makeAnd(newPathFormula, invariant);
    }

    // update abstraction locations map
    abstractionLocations = abstractionLocations.putAndCopy(loc, newLocInstance);

    PredicateAbstractState state =
        PredicateAbstractState.mkAbstractionState(newPathFormula,
            newAbstractionFormula, abstractionLocations);
    return Optional.of(PrecisionAdjustmentResult.create(
        state, precision, PrecisionAdjustmentResult.Action.CONTINUE));
  }

  private void extractInvariants() throws CPAException {
    if (invariantGenerator == null) {
      return; // already done
    }

    try {
      invariants = invariantGenerator.get();

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();

    } finally {
      invariantGenerator = null; // to allow GC'ing it and the ReachedSet
    }
  }

  void setInitialLocation(CFANode initialLocation) {
    invariantGenerator.start(initialLocation);
  }
}
