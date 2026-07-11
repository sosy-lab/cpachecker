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
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.precision.ConfigurablePrecision;
import org.sosy_lab.cpachecker.core.defaults.precision.ScopedRefinablePrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.por.PrecisionVariableManager.CompositePrecisionVariableManager;
import org.sosy_lab.cpachecker.cpa.por.PrecisionVariableManager.ConfigurablePrecisionVariableManager;
import org.sosy_lab.cpachecker.cpa.por.PrecisionVariableManager.PredicatePrecisionVariableManager;
import org.sosy_lab.cpachecker.cpa.por.PrecisionVariableManager.ScopedRefinablePrecisionVariableManager;
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

  @Option(
      secure = true,
      description = "Seed for the pseudo-random shuffling of the enabled threads in the "
          + "source-set heuristic. A single random-number generator is shared by reference "
          + "across all states of one analysis run, so successive shuffles draw fresh values "
          + "instead of every state restarting the same sequence; the seed is fixed only to "
          + "keep runs reproducible."
  )
  private long randomSeed = 0;

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

    transferRelation =
        new PORTransferRelation(
            pCpa, pConfig, pCfa, aggregateBasicBlocks, pLogger, new Random(randomSeed));

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
    ConfigurableProgramAnalysis wrappedCpa = getWrappedCpa();
    if (abstractionAware) {
      Precision initialWrappedPrecision = wrappedCpa.getInitialPrecision(pNode, pPartition);
      List<PrecisionVariableManager> variableManagers = new ArrayList<>(1);

      ConfigurablePrecision configurablePrecision =
          Precisions.extractPrecisionByType(initialWrappedPrecision, ConfigurablePrecision.class);
      if (configurablePrecision != null) {
        variableManagers.add(new ConfigurablePrecisionVariableManager());
      }

      ScopedRefinablePrecision scopedRefinablePrecision =
          Precisions.extractPrecisionByType(initialWrappedPrecision, ScopedRefinablePrecision.class);
      if (scopedRefinablePrecision != null) {
        variableManagers.add(new ScopedRefinablePrecisionVariableManager());
      }

      PredicatePrecision predicatePrecision =
          Precisions.extractPrecisionByType(initialWrappedPrecision, PredicatePrecision.class);
      if (predicatePrecision != null) {
        var predicateCPA = CPAs.retrieveCPA(wrappedCpa, PredicateCPA.class);
        if (predicateCPA == null) {
          throw new IllegalStateException(
              "Abstraction-aware POR requires PredicateCPA when using PredicatePrecision, but it is not present.");
        }
        var fmgr = predicateCPA.getSolver().getFormulaManager();
        variableManagers.add(new PredicatePrecisionVariableManager(fmgr));
      }

      PrecisionVariableManager variableManager = switch (variableManagers.size()) {
        case 0 -> throw new IllegalStateException(
            "Abstraction-aware POR does not support this precision: " + initialWrappedPrecision);
        case 1 -> variableManagers.getFirst();
        default -> new CompositePrecisionVariableManager(ImmutableList.copyOf(variableManagers));
      };

      return new AbstractionAwarePORPrecision(variableManager, initialWrappedPrecision);
    }

    return new AbstractionUnawarePORPrecision(wrappedCpa.getInitialPrecision(pNode, pPartition));
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
          if (wrapped2.equals(mergedWrapped)) {
            return porState2;
          } else {
            return porState1.withWrappedState(mergedWrapped);
          }
        }
      }
      return state2;
    };
  }

  @Override
  public StopOperator getStopOperator() {
    return (state, reached, precision) -> {
      if (state instanceof PORState porState && precision instanceof PORPrecision porPrecision) {
        ImmutableList.Builder<AbstractState> builder = ImmutableList.builder();
        for (AbstractState reachedState : reached) {
          if (reachedState instanceof PORState reachedPorState && Objects.equals(porState.threads(),
              reachedPorState.threads()) && Objects.equals(porState.livePids(),
              reachedPorState.livePids())) {
            builder.add(reachedPorState.getWrappedState());
          }
        }
        return getWrappedCpa().getStopOperator()
            .stop(porState.getWrappedState(), builder.build(), porPrecision.getWrappedPrecision());
      }
      return false;
    };
  }
}
