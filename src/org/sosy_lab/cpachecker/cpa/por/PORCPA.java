// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

/**
 * POR (Partial Order Reduction) CPA that manages thread interleaving in concurrent programs. This
 * CPA is composed alongside other CPAs (e.g., MutexCPA, PredicateCPA) in a CompositeCPA. It uses
 * {@code strengthen} to read the MutexState from the MutexCPA for lock-based filtering during
 * source-set computation.
 */
public class PORCPA extends AbstractCPA {

  private final PORTransferRelation transferRelation;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PORCPA.class);
  }

  @SuppressWarnings("unused")
  private PORCPA(
      Configuration config,
      LogManager pLogger,
      CFA pCfa,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    super("sep", "sep", /* transfer set below */ null);
    transferRelation = new PORTransferRelation(config, pCfa, pLogger, pShutdownNotifier);
  }

  @Override
  public PORTransferRelation getTransferRelation() {
    return transferRelation;
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
