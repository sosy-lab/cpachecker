/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.propertyscope;

import static org.sosy_lab.cpachecker.cpa.propertyscope.PropertyScopeUtil.generateCFAEdgeToUsedVar;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.defaults.SimplePrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.propertyscope.ScopeLocation.Reason;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.VariableClassification.Partition;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Stream;

public class PropertyScopePrecisionAdjustment implements PrecisionAdjustment {

  private final CFA cfa;
  private final LogManager logger;
  private final VariableClassification varClassification;
  private final Multimap<?,?> cfaEdgeToUsedVar;

  public PropertyScopePrecisionAdjustment(CFA pCfa, LogManager pLogger) {
    cfa = pCfa;
    logger = pLogger;
    varClassification = Optionals.fromGuavaOptional(cfa.getVarClassification())
        .orElseThrow(() -> new IllegalStateException("CFA is missing a VariableClassification!"));

    cfaEdgeToUsedVar = generateCFAEdgeToUsedVar(varClassification.getPartitions());
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState state,
      Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState) throws CPAException, InterruptedException {
    return Optional.of(PrecisionAdjustmentResult.create(state, precision, Action.CONTINUE));
  }

  @Override
  public Optional<? extends AbstractState> strengthen(
      AbstractState pState, Precision pPrecision, List<AbstractState> otherStates)
      throws CPAException, InterruptedException {
    PropertyScopeState state = (PropertyScopeState) pState;

    PredicateAbstractState predState = getPredicateAbstractState(otherStates);
    FormulaManagerView fmgr = GlobalInfo.getInstance().getPredicateFormulaManagerView();
    BooleanFormula formula = predState.getAbstractionFormula().asFormula();

    PersistentList<PropertyScopeState> prevStates = state.getPrevBlockStates();
    Set<ScopeLocation> scopeLocations = new LinkedHashSet<>();

    Stream.concat(Stream.of(state), prevStates.stream().limit(prevStates.size() - 1))
        .forEach(st -> {
          scopeLocations.addAll(st.getScopeLocations());

          // variables in edge occur in abs formula -> edge in scope
          fmgr.extractAtoms(formula, false).stream()
              .filter(atom -> fmgr.extractVariableNames(atom).stream()
                  .anyMatch(var -> cfaEdgeToUsedVar.containsEntry(st.getEnteringEdge(), var)))
              .findAny().ifPresent(p -> scopeLocations.add(new ScopeLocation(
                  st.getEnteringEdge(), st.getCallstack(), Reason.ABS_FORMULA)));

        });

    return Optional.of(new PropertyScopeState(
        PersistentLinkedList.of(),
        state.getPropertyDependantMatches(),
        state.getEnteringEdge(),
        state.getCallstack(),
        scopeLocations,
        state.getPrevState()));

  }

  private static PredicateAbstractState getPredicateAbstractState(List<AbstractState> otherStates)
      throws CPAException {
    PredicateAbstractState predState = otherStates.stream()
        .filter(PredicateAbstractState.class::isInstance)
        .map(PredicateAbstractState.class::cast)
        .findAny().orElseThrow(() -> new CPAException("Predicate state missing!"));

    if (!predState.isAbstractionState()) {
      throw new CPAException("Got a non-abstraction predicate state, enable SBE!");
    }

    return predState;
  }

}
