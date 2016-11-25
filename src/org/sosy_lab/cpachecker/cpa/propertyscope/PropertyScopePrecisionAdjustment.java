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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
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
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.holder.Holder;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Options(prefix="cpa.propertyscope")
public class PropertyScopePrecisionAdjustment implements PrecisionAdjustment {

  private final CFA cfa;
  private final LogManager logger;
  private final VariableClassification varClassification;
  private final Multimap<?,?> cfaEdgeToUsedVar;

  @Option(secure=true, description="Try to find a scope with ABS_FORMULA_IMPLICATION (many solver"
      + " queries)")
  private boolean collectImplicationScope = false;

  public PropertyScopePrecisionAdjustment(Configuration pConfig, CFA pCfa, LogManager pLogger)
      throws InvalidConfigurationException {
    cfa = pCfa;
    logger = pLogger;
    pConfig.inject(this);
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

    Holder<AbstractionFormula> hAfterGlobalInitAbsFormula =
        Holder.of(state.getAfterGlobalInitAbsFormula());

    PredicateAbstractState predState = getPredicateAbstractState(otherStates);
    FormulaManagerView fmgr = GlobalInfo.getInstance().getPredicateFormulaManagerView();
    Solver solver = GlobalInfo.getInstance().getPredicateSolver();
    BooleanFormula formula = predState.getAbstractionFormula().asFormula();
    BooleanFormula instFormula = predState.getAbstractionFormula().asInstantiatedFormula();

    PersistentList<PropertyScopeState> prevStates = state.getPrevBlockStates();
    Set<ScopeLocation> scopeLocations = new LinkedHashSet<>();

    Holder<AbstractionFormula> hVarClassScopeAbsFormula = Holder.of(state
        .getLastVarClassScopeAbsFormula());
    // TODO: String equality may not be the finest way to do this
    boolean considerVarClassScope = hVarClassScopeAbsFormula.value == null || !instFormula
        .toString().equals(hVarClassScopeAbsFormula.value.asInstantiatedFormula().toString());


    Stream.concat(Stream.of(state), prevStates.stream().limit(prevStates.size() - 1))
        .forEach(st -> {
          scopeLocations.addAll(st.getScopeLocations());

          // save the initial abstraction formula after init of globals
          if(hAfterGlobalInitAbsFormula.value == null &&
              st.getEnteringEdge().getDescription().equals("Function start dummy edge")) {
            hAfterGlobalInitAbsFormula.value = predState.getAbstractionFormula();
          }

          // simple non-true abs formula scope
          if(!predState.getAbstractionFormula().isTrue()) {
            scopeLocations.add(new ScopeLocation(st.getEnteringEdge(), st.getCallstack(),
                Reason.ABS_FORMULA));
          }

          // variables in edge occur in abs formula -> edge in scope
          if (fmgr.extractVariableNames(formula).stream()
              .anyMatch(var -> cfaEdgeToUsedVar.containsEntry(st.getEnteringEdge(), var))) {
            scopeLocations.add(new ScopeLocation(st.getEnteringEdge(), st.getCallstack(),
                Reason.ABS_FORMULA_VAR_CLASSIFICATION));
            if (considerVarClassScope) {
              scopeLocations.add(new ScopeLocation(st.getEnteringEdge(), st.getCallstack(),
                  Reason.ABS_FORMULA_VAR_CLASSIFICATION_FORMULA_CHANGE));
              hVarClassScopeAbsFormula.value = predState.getAbstractionFormula();
            }
          }
        });

    if (collectImplicationScope && predState.isAbstractionState()
        && hAfterGlobalInitAbsFormula.value != null) {
      try {
        boolean implication =
            solver.implies(hAfterGlobalInitAbsFormula.value.asInstantiatedFormula(),
                instFormula);

        if (!implication) {
          Stream.concat(Stream.of(state), prevStates.stream().limit(prevStates.size() - 1))
              .forEach(st ->
                  scopeLocations.add(new ScopeLocation(st.getEnteringEdge(), st.getCallstack(),
                      Reason.ABS_FORMULA_IMPLICATION)));
        }
      } catch (SolverException pE) {
        throw new CPAException("Solver problem!", pE);
      }
    }


    // not really helpful, look again later maybe
/*    if (absScopeInstance.isPresent()) {
      AbstractionPropertyScopeInstance absInst = (AbstractionPropertyScopeInstance)
          absScopeInstance.get();

      BooleanFormula startFormula = absInst.getStartFormula().asInstantiatedFormula();
      BooleanFormula thisFormula = predState.getAbstractionFormula().asInstantiatedFormula();

      try {
        boolean implication = solver.implies(startFormula, thisFormula);
        boolean revImplication = solver.implies(thisFormula, startFormula);

        if (implication && revImplication) {
          absScopeInstance = java.util.Optional.empty();
        }
      } catch (SolverException pE) {
        throw new CPAException("Solver problem!", pE);
      }

    }

    if (!absScopeInstance.isPresent() && hIsAbsScopeLoc.value) {
      java.util.Optional<AbstractionFormula> prevFormula = state.prevStateStream()
          .map(PropertyScopeState::getAbsFormula)
          .filter(java.util.Optional::isPresent)
          .findFirst().flatMap(identity());

      if(prevFormula.isPresent()) {
        absScopeInstance = java.util.Optional.of(PropertyScopeInstance.create(prevFormula.get()));
      }

    }*/

    return Optional.of(new PropertyScopeState(
        PersistentLinkedList.of(),
        state.getPropertyDependantMatches(),
        state.getEnteringEdge(),
        state.getCallstack(),
        scopeLocations,
        state.getPrevState().orElse(null),
        predState.getAbstractionFormula(),
        state.getAutomatonStates(),
        state.getAutomScopeInsts(),
        hAfterGlobalInitAbsFormula.value,
        hVarClassScopeAbsFormula.value));

  }
  
  private static PredicateAbstractState getPredicateAbstractState(List<AbstractState> otherStates)
      throws CPAException {
    PredicateAbstractState predState = otherStates.stream()
        .filter(PredicateAbstractState.class::isInstance)
        .map(PredicateAbstractState.class::cast)
        .findAny().orElseThrow(() -> new CPAException("Predicate state missing!"));

    return predState;
  }

}
