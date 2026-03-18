// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * POR (Partial Order Reduction) CPA that manages thread interleaving in concurrent programs. This
 * CPA is composed alongside other CPAs (e.g., MutexCPA, PredicateCPA) in a CompositeCPA. It uses
 * {@code strengthen} to read the MutexState from the MutexCPA for lock-based filtering during
 * source-set computation.
 */
@Options(prefix = "cpa.por")
public class PORCPA extends AbstractSingleWrapperCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PORCPA.class);
  }

  @Option(
      secure = true,
      description = "Use an abstraction-aware POR algorithm.")
  private boolean isAbstractionAware = false;

  private final PORTransferRelation transferRelation;
  private final PrecisionAdjustment precisionAdjustment;

  @SuppressWarnings("unused")
  private PORCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    super(pCpa);
    transferRelation = new PORTransferRelation(pCpa, pConfig, pCfa, pLogger, pShutdownNotifier);

    final PrecisionAdjustment wrappedPrecisionAdjustment = pCpa.getPrecisionAdjustment();
    precisionAdjustment = (state, precision, states, stateProjection, fullState) -> {
      if (!(state instanceof PORState porState)
          || !(precision instanceof PORPrecision porPrecision)) {
        throw new CPAException("Expected PORState, got " + state.getClass().getSimpleName());
      }
      Optional<PrecisionAdjustmentResult> result = wrappedPrecisionAdjustment.prec(
          checkNotNull(porState.getWrappedState()),
          porPrecision.getWrappedPrecision(),
          states,
          Functions.compose(s -> checkNotNull((PORState) s).getWrappedState(), stateProjection),
          fullState);

      return result.map(r -> new PrecisionAdjustmentResult(
          porState.withWrappedState(r.abstractState()),
          porPrecision.replaceWrappedPrecision(r.precision(), Predicates.instanceOf(r.precision().getClass())),
          r.action()
      ));

    };
  }

  @Override
  public PORTransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return transferRelation.initial(getWrappedCpa().getInitialState(node, partition));
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    if (isAbstractionAware) {
      return new AbstractionAwarePORPrecision(
          getWrappedCpa().getInitialPrecision(pNode, pPartition));
    }
    return new AbstractionUnawarePORPrecision(
        getWrappedCpa().getInitialPrecision(pNode, pPartition));
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return (state1, state2, precision) -> {
      if (state1 instanceof PORState porState1 && state2 instanceof PORState porState2) {
        if (porState1.canMerge(porState2)) {
          return porState1;
        }
      }
      return state2;
    };
  }

  @Override
  public StopOperator getStopOperator() {
    return (state, reached, precision) -> {
      if (state instanceof PORState porState) {
        for (AbstractState reachedState : reached) {
          if (reachedState instanceof PORState reachedPorState) {
            if (porState.equals(reachedPorState)) {
              return true;
            }
          }
        }
      }
      return false;
    };
  }
}
