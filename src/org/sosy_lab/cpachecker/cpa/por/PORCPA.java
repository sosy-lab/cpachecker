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
import org.sosy_lab.cpachecker.cpa.por.PrecisionVariableManager.PredicatePrecisionVariableManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;

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
      description = "Use an abstraction-aware POR algorithm. Abstraction-aware POR can ignore"
          + "certain variables during dependency calculation if there is no information about them"
          + "in the precision (e.g., no predicates referring to them).")
  private boolean abstractionAware = false;

  @Option(
      secure = true,
      description = "Aggregate basic blocks to MultiEdges. Similar to basic block aggregation in"
          + "the CompositeCPA transfer relation, but only one global statement (i.e., a"
          + "statement that accesses a global variable or uses heap memory) is included in a"
          + "MultiEdge, so that all concurrent thread interleavings are explored."
  )
  private boolean aggregateBasicBlocks = false;

  private final PORTransferRelation transferRelation;
  private final PrecisionAdjustment precisionAdjustment;

  @SuppressWarnings("unused")
  private PORCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(pCpa);
    pConfig.inject(this);

    transferRelation = new PORTransferRelation(pCpa, pConfig, pCfa, aggregateBasicBlocks, pLogger);

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
          porPrecision.replaceWrappedPrecision(r.precision(),
              Predicates.instanceOf(r.precision().getClass())),
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
    if (abstractionAware) {
      Precision initialWrappedPrecision = getWrappedCpa().getInitialPrecision(pNode, pPartition);
      PrecisionVariableManager variableExtractor = null;

      PredicatePrecision predicatePrecision =
          Precisions.extractPrecisionByType(initialWrappedPrecision, PredicatePrecision.class);
      if (predicatePrecision != null) {
        var predicateCPA = CPAs.retrieveCPA(getWrappedCpa(), PredicateCPA.class);
        if (predicateCPA == null) {
          throw new IllegalStateException(
              "Abstraction-aware POR requires PredicateCPA when using PredicatePrecision, but it is not present.");
        }
        var fmgr = predicateCPA.getSolver().getFormulaManager();
        variableExtractor = new PredicatePrecisionVariableManager(fmgr);
      }

      if (variableExtractor == null) {
        throw new IllegalStateException(
            "Abstraction-aware POR does not support this precision: " + initialWrappedPrecision);
      }

      return new AbstractionAwarePORPrecision(variableExtractor, initialWrappedPrecision);
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
    MergeOperator wrappedMergeOperator = getWrappedCpa().getMergeOperator();
    return (state1, state2, precision) -> {
      if (state1 instanceof PORState porState1 && state2 instanceof PORState porState2
          && precision instanceof PORPrecision porPrecision) {
        if (porState1.canMerge(porState2)) {
          AbstractState wrapped1 = porState1.getWrappedState();
          AbstractState wrapped2 = porState2.getWrappedState();
          Precision wrappedPrecision = porPrecision.getWrappedPrecision();
          AbstractState mergedWrapped =
              wrappedMergeOperator.merge(wrapped1, wrapped2, wrappedPrecision);
          return porState1.withWrappedState(mergedWrapped);
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
