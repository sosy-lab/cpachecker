// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * POR (Partial Order Reduction) CPA that wraps a composite CPA. The POR CPA manages thread
 * interleaving and delegates per-thread analysis to the wrapped CPA. It also integrates mutex
 * awareness for lock-based filtering during source-set computation.
 */
public class PORCPA extends AbstractSingleWrapperCPA {

  private final PORTransferRelation transferRelation;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PORCPA.class);
  }

  @SuppressWarnings("unused")
  private PORCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration config,
      LogManager pLogger,
      CFA pCfa,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    super(pCpa);
    transferRelation = new PORTransferRelation(pCpa, config, pCfa, pLogger, pShutdownNotifier);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return new FlatLatticeDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return (state, precision, states, stateProjection, fullState) -> {
      if(state instanceof PORState pPORState && precision instanceof AbstractionAwarePORPrecision pPORPrecision) {
        Optional<PrecisionAdjustmentResult> prec = getWrappedCpa().getPrecisionAdjustment()
            .prec(pPORState.getWrappedState(), pPORPrecision.getWrappedPrecision(), states,
                stateProjection, fullState);
        return prec.map(pPrecisionAdjustmentResult -> pPrecisionAdjustmentResult
            .withAbstractState(pPORState.withWrappedState(pPrecisionAdjustmentResult.abstractState()))
            .withPrecision(new AbstractionAwarePORPrecision(pPrecisionAdjustmentResult.precision())));
      } else {
        throw new CPATransferException("Not PORstate or WrapperPrecision");
      }
    };
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return new AbstractionAwarePORPrecision(getWrappedCpa().getInitialPrecision(pNode, pPartition));
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return transferRelation.initial();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return (state1, state2, precision) -> {
      if (state1 instanceof PORState porState1
          && state2 instanceof PORState porState2) {
        if (porState1.canMerge(porState2)) {
          return porState1;
        }
      } else {
        throw new UnsupportedOperationException(
            "PORCPA does not support merge operators over non-PORState");
      }
      return state2;
    };
  }

  @Override
  public StopOperator getStopOperator() {
    return (state, reached, precision) -> {
      for (AbstractState reachedState : reached) {
        if (state instanceof PORState porState
            && reachedState instanceof PORState reachedPorState) {
          if (porState.equals(reachedPorState)) {
            return true;
          }
        }
      }
      return false;
    };
  }
}
