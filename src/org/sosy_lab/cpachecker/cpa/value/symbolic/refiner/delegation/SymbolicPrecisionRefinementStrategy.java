/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.delegation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.ConstraintsPrecision.Increment;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.ConstraintsPrecision.Increment.Builder;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionRefinementStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.ARGTreePrecisionUpdater;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * {@link org.sosy_lab.cpachecker.cpa.predicate.RefinementStrategy RefinementStrategy} that
 * does not refine the precision of PredicateCPA, but of
 * {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA ValueAnalysisCPA} and
 * {@link org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA ConstraintsCPA}.
 */
class SymbolicPrecisionRefinementStrategy extends PredicateAbstractionRefinementStrategy {

  private final FormulaManagerView formulaManager;

  public SymbolicPrecisionRefinementStrategy(
      final Configuration config,
      final LogManager pLogger,
      final PredicateAbstractionManager pPredAbsMgr,
      final Solver pSolver)
      throws InvalidConfigurationException {
    super(config, pLogger, pPredAbsMgr, pSolver);
    formulaManager = pSolver.getFormulaManager();
  }

  @Override
  public void performRefinement(
      final ARGReachedSet pReached,
      final List<ARGState> pAbstractionStatesTrace,
      final List<BooleanFormula> pInterpolants,
      final boolean pRepeatedCounterexample
  ) throws CPAException, InterruptedException {
    if (pRepeatedCounterexample) {
      throw new CPAException("Refinement using predicate refinement failed."
          + "Try using cpa.value.symbolic.refiner.SymbolicValueAnalysisRefiner");
    }

    super.performRefinement(pReached, pAbstractionStatesTrace, pInterpolants,
        pRepeatedCounterexample);
  }

  @Override
  protected void updateARG(
      PredicatePrecision newPrecision, ARGState pRefinementRoot, ARGReachedSet pReached)
      throws InterruptedException {
    assert newPrecision.getFunctionPredicates().isEmpty()
        : "Only local predicates allowed, but function predicate exists";
    assert newPrecision.getGlobalPredicates().isEmpty()
        : "Only local predicates allowed, but global predicate exists";

    final Map<CFANode, Collection<AbstractionPredicate>> localPrec =
        newPrecision.getLocalPredicates().asMap();

    Multimap<CFANode, MemoryLocation> valuePrecInc = HashMultimap.create();

    Builder constrPrecInc = Increment.builder();

    for (Map.Entry<CFANode, Collection<AbstractionPredicate>> entry : localPrec.entrySet()) {
      // this is actually the predecessor of a node we will use for precision adjustment
      CFANode currNode = entry.getKey();
      Collection<MemoryLocation> locations = new HashSet<>();

      for (AbstractionPredicate p : entry.getValue()) {
        for (String varName : formulaManager.extractVariableNames(p.getSymbolicAtom())) {
          locations.add(MemoryLocation.valueOf(varName));
        }
      }

      valuePrecInc.putAll(currNode, locations);
      constrPrecInc.locallyTracked(currNode, (Constraint) null); // we only need the node
    }

    final ARGTreePrecisionUpdater precUpdater = ARGTreePrecisionUpdater.getInstance();
    precUpdater.updateARGTree(pReached, pRefinementRoot, valuePrecInc, constrPrecInc.build());
  }
}
