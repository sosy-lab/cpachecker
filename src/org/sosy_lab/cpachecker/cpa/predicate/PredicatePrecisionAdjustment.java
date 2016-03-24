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
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;

public class PredicatePrecisionAdjustment implements PrecisionAdjustment {

  // statistics
  final Timer totalPrecTime = new Timer();
  final Timer computingAbstractionTime = new Timer();

  int numAbstractions = 0;
  int numAbstractionsFalse = 0;
  int maxBlockSize = 0;

  private final LogManager logger;
  private final BlockOperator blk;
  private final PredicateAbstractionManager formulaManager;
  private final PathFormulaManager pathFormulaManager;
  private final FormulaManagerView fmgr;

  private @Nullable InvariantGenerator invariantGenerator;
  private InvariantSupplier invariants;
  private final PredicateProvider predicateProvider;

  public PredicatePrecisionAdjustment(
      LogManager pLogger,
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      BlockOperator pBlk,
      PredicateAbstractionManager pPredAbsManager,
      InvariantGenerator pInvariantGenerator,
      PredicateProvider pPredicateProvider) {

    logger = pLogger;
    fmgr = pFmgr;
    pathFormulaManager = pPfmgr;
    blk = pBlk;
    formulaManager = pPredAbsManager;

    invariantGenerator = checkNotNull(pInvariantGenerator);
    invariants = InvariantSupplier.TrivialInvariantSupplier.INSTANCE;

    predicateProvider = pPredicateProvider;
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

      CFANode location = AbstractStates.extractLocation(fullState);
      PathFormula pathFormula = element.getPathFormula();

      if (!element.isAbstractionState() && blk.isBlockEnd(location, pathFormula.getLength())) {
        PredicatePrecision precision = (PredicatePrecision)pPrecision;

        return computeAbstraction(element, precision, location, fullState);
      } else {
        return Optional.of(PrecisionAdjustmentResult.create(
            element, pPrecision, PrecisionAdjustmentResult.Action.CONTINUE));
      }


    } catch (SolverException e) {
      throw new CPAException("Solver Failure", e);
    } finally {
      totalPrecTime.stop();
    }
  }

  /**
   * Compute an abstraction.
   */
  private Optional<PrecisionAdjustmentResult> computeAbstraction(
      PredicateAbstractState element,
      PredicatePrecision precision,
      CFANode loc,
      AbstractState fullState)
      throws SolverException, CPAException, InterruptedException {

    AbstractionFormula abstractionFormula = element.getAbstractionFormula();
    PersistentMap<CFANode, Integer> abstractionLocations = element.getAbstractionLocationsOnPath();
    PathFormula pathFormula = element.getPathFormula();
    Integer newLocInstance = firstNonNull(abstractionLocations.get(loc), 0) + 1;

    numAbstractions++;
    logger.log(Level.FINEST, "Computing abstraction at instance", newLocInstance, "of node", loc, "in path.");

    maxBlockSize = Math.max(maxBlockSize, pathFormula.getLength());

    // get invariants and add them
    extractInvariants();
    BooleanFormula invariant =
        invariants.getInvariantFor(loc, fmgr, pathFormulaManager, pathFormula);
    if (invariant != null) {
      pathFormula = pathFormulaManager.makeAnd(pathFormula, invariant);
    }

    // get additional predicates
    Set<AbstractionPredicate> additionalPredicates = predicateProvider.getPredicates(fullState);

    AbstractionFormula newAbstractionFormula = null;

    // compute new abstraction
    computingAbstractionTime.start();
    try {
      Set<AbstractionPredicate> preds = precision.getPredicates(loc, newLocInstance);
      preds = Sets.union(preds, additionalPredicates);

      // compute a new abstraction with a precision based on `preds`
      newAbstractionFormula = formulaManager.buildAbstraction(
          loc, abstractionFormula, pathFormula, preds);
    } finally {
      computingAbstractionTime.stop();
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

  private void extractInvariants() throws CPAException, InterruptedException {
    if (invariantGenerator == null) {
      return; // already done
    }

    try {
      invariants = invariantGenerator.get();

    } catch (InterruptedException e) {
      invariantGenerator.cancel();
      throw e;

    } finally {
      invariantGenerator = null; // to allow GC'ing it and the ReachedSet
    }
  }

  void setInitialLocation(CFANode initialLocation) {
    invariantGenerator.start(initialLocation);
  }
}
