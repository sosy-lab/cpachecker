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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.util.List;

import org.sosy_lab.cpachecker.core.defaults.WrappingPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

public class AdjustAutomatonPrecisionAdjustment extends WrappingPrecisionAdjustment {

  private Table<AutomatonInternalState, AutomatonPrecision, List<AutomatonTransition>> relevanTransCache = HashBasedTable.create();

  public AdjustAutomatonPrecisionAdjustment(final PrecisionAdjustment pWrappedPrecOp) {
    super(pWrappedPrecOp);
  }

  @Override
  protected Optional<PrecisionAdjustmentResult> wrappingPrec(AbstractState pState, Precision pPrecision,
      UnmodifiableReachedSet pStates, Function<AbstractState, AbstractState> pProjection, AbstractState pFullState)
          throws CPAException {

    final AutomatonState state = (AutomatonState) pState;
    final AutomatonPrecision pi = (AutomatonPrecision) pPrecision;
    final Automaton a = state.getOwningAutomaton();

    List<AutomatonTransition> relevantTransitions = null;

    relevantTransitions = relevanTransCache.get(state.getInternalState(), pi);

    if (relevantTransitions == null) {
      boolean hasIrrelevantTransitions = false;

      if (state.getInternalState().equals(AutomatonInternalState.INACTIVE)
          || state.getInternalState().equals(AutomatonInternalState.INTERMEDIATEINACTIVE)) {

        // All outgoing transitions of the state "INACTIVE" are considered to be relevant.
        relevantTransitions = state.getLeavingTransitions();

      } else {
        relevantTransitions = Lists.newArrayListWithExpectedSize(state.getLeavingTransitions().size());

        for (AutomatonTransition trans: state.getLeavingTransitions()) {

          ImmutableSet<? extends SafetyProperty> transProps = a.getIsRelevantForProperties(trans);
          final boolean transRelevantForActiveProps = !(pi.getBlacklist().containsAll(transProps));

          if (transRelevantForActiveProps) {
            relevantTransitions.add(trans);
          } else {
            hasIrrelevantTransitions = true;
          }
        }
      }

      if (hasIrrelevantTransitions) {
        relevanTransCache.put(state.getInternalState(), pi, relevantTransitions);
      } else {
        relevanTransCache.put(state.getInternalState(), pi, state.getLeavingTransitions());
      }
    }

    if (relevantTransitions.size() != state.getLeavingTransitions().size()) {
      final AutomatonState adjustedState = AutomatonState.automatonStateFactory(
          state.getVars(), state.getInternalState(), relevantTransitions,
          state.getAutomatonCPA(),
          state.getAssumptions(),
          state.getShadowCode(),
          state.getMatches(),
          state.getFailedMatches(), false,
          state.getViolatedPropertyInstances());

      return Optional.of(PrecisionAdjustmentResult.create(adjustedState, pPrecision, Action.CONTINUE));
    }

    // No change of the precision
    return Optional.of(PrecisionAdjustmentResult.create(pState, pPrecision, Action.CONTINUE));
  }


}