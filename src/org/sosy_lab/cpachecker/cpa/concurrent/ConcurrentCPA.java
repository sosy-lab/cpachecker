// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

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
import org.sosy_lab.cpachecker.cpa.concurrent.PrecisionVariableManager.CompositePrecisionVariableManager;
import org.sosy_lab.cpachecker.cpa.concurrent.PrecisionVariableManager.ConfigurablePrecisionVariableManager;
import org.sosy_lab.cpachecker.cpa.concurrent.PrecisionVariableManager.PredicatePrecisionVariableManager;
import org.sosy_lab.cpachecker.cpa.concurrent.PrecisionVariableManager.ScopedRefinablePrecisionVariableManager;
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
@Options(prefix = "cpa.concurrent")
public class ConcurrentCPA extends AbstractSingleWrapperCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ConcurrentCPA.class);
  }

  @Option(
      secure = true,
      description =
          "Use an abstraction-aware POR algorithm. Abstraction-aware POR can ignorecertain"
              + " variables during dependency calculation if there is no information about themin"
              + " the precision (e.g., no predicates referring to them).")
  private boolean abstractionAware = false;

  @Option(
      secure = true,
      description =
          "Aggregate basic blocks to MultiEdges. Similar to basic block aggregation in"
              + "the CompositeCPA transfer relation, but only one global statement (i.e., a"
              + "statement that accesses a global variable or uses heap memory) is included in a"
              + "MultiEdge, so that all concurrent thread interleavings are explored.")
  private boolean aggregateBasicBlocks = false;

  @Option(
      secure = true,
      description =
          "Seed for the pseudo-random shuffling of the enabled threads in the "
              + "source-set heuristic. A single random-number generator is shared by reference "
              + "across all states of one analysis run, so successive shuffles draw fresh values "
              + "instead of every state restarting the same sequence; the seed is fixed only to "
              + "keep runs reproducible.")
  private long randomSeed = 0;

  @Option(
      secure = true,
      description = "Partial order reduction (POR) algorithm to use. Options: NOPOR, SPOR"
  )
  private String partialOrderReductionAlgorithm = "SPOR";

  private final ConcurrentTransferRelation transferRelation;
  private final ConfigurableProgramAnalysis threadSpecificCPA;
  private final PrecisionAdjustment precisionAdjustment;
  private final LogManager logger;

  @SuppressWarnings("unused")
  private ConcurrentCPA(
      ConfigurableProgramAnalysis pCpa, Configuration pConfig, LogManager pLogger, CFA pCfa)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    super(pCpa);
    pConfig.inject(this);

    logger = pLogger;

    threadSpecificCPA = new ThreadSpecificCPA(pConfig, pCfa, pLogger);
    PartialOrderReductionStrategy strategy = switch (partialOrderReductionAlgorithm) {
      case "NOPOR" -> ConcurrentState::new;
      case "SPOR" -> SPORConcurrentState::new;
      default -> throw new InvalidConfigurationException(
          "Unknown partial order reduction algorithm: " + partialOrderReductionAlgorithm);
    };
    transferRelation =
        new ConcurrentTransferRelation(
            pCpa, threadSpecificCPA, pConfig, pCfa, strategy, aggregateBasicBlocks, pLogger, new Random(randomSeed));

    final PrecisionAdjustment wrappedPrecisionAdjustment = pCpa.getPrecisionAdjustment();
    precisionAdjustment =
        (state, precision, states, stateProjection, fullState) -> {
          if (!(state instanceof ConcurrentState pConcurrentState)
              || !(precision instanceof ConcurrentPrecision pConcurrentPrecision)) {
            throw new CPAException("Expected ConcurrentState, got " + state.getClass().getSimpleName());
          }
          Optional<PrecisionAdjustmentResult> result =
              wrappedPrecisionAdjustment.prec(
                  checkNotNull(pConcurrentState.getWrappedState()),
                  pConcurrentPrecision.getWrappedPrecision(),
                  states,
                  Functions.compose(
                      s -> checkNotNull((ConcurrentState) s).getWrappedState(), stateProjection),
                  fullState);

          return result.map(
              r ->
                  new PrecisionAdjustmentResult(
                      pConcurrentState.withWrappedState(r.abstractState()),
                      pConcurrentPrecision.replaceWrappedPrecision(
                          r.precision(), Predicates.instanceOf(r.precision().getClass())),
                      r.action()));
        };
  }

  @Override
  public ConcurrentTransferRelation getTransferRelation() {
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
          Precisions.extractPrecisionByType(
              initialWrappedPrecision, ScopedRefinablePrecision.class);
      if (scopedRefinablePrecision != null) {
        variableManagers.add(new ScopedRefinablePrecisionVariableManager(logger));
      }

      PredicatePrecision predicatePrecision =
          Precisions.extractPrecisionByType(initialWrappedPrecision, PredicatePrecision.class);
      if (predicatePrecision != null) {
        var predicateCPA = CPAs.retrieveCPA(wrappedCpa, PredicateCPA.class);
        checkState(
            predicateCPA != null,
            "Abstraction-aware POR requires PredicateCPA when using PredicatePrecision, but it is"
                + " not present.");
        var fmgr = predicateCPA.getSolver().getFormulaManager();
        variableManagers.add(new PredicatePrecisionVariableManager(fmgr));
      }

      PrecisionVariableManager variableManager =
          switch (variableManagers.size()) {
            case 0 ->
                throw new IllegalStateException(
                    "Abstraction-aware POR does not support this precision: "
                        + initialWrappedPrecision);
            case 1 -> variableManagers.getFirst();
            default ->
                new CompositePrecisionVariableManager(ImmutableList.copyOf(variableManagers));
          };

      return new AbstractionAwarePORPrecision(variableManager, initialWrappedPrecision);
    }

    return new SimpleConcurrentPrecision(wrappedCpa.getInitialPrecision(pNode, pPartition));
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public MergeOperator getMergeOperator() {
    MergeOperator wrappedMergeOperator = getWrappedCpa().getMergeOperator();
    return (state1, state2, precision) -> {
      if (state1 instanceof ConcurrentState pConcurrentState1
          && state2 instanceof ConcurrentState pConcurrentState2
          && precision instanceof ConcurrentPrecision pConcurrentPrecision) {
        if (pConcurrentState1.canMerge(pConcurrentState2)) {
          AbstractState wrapped1 = pConcurrentState1.getWrappedState();
          AbstractState wrapped2 = pConcurrentState2.getWrappedState();
          Precision wrappedPrecision = pConcurrentPrecision.getWrappedPrecision();
          AbstractState mergedWrapped =
              wrappedMergeOperator.merge(wrapped1, wrapped2, wrappedPrecision);
          if (wrapped2.equals(mergedWrapped)) {
            return pConcurrentState2;
          } else {
            return pConcurrentState1.withWrappedState(mergedWrapped);
          }
        }
      }
      return state2;
    };
  }

  @Override
  public StopOperator getStopOperator() {
    final StopOperator threadSpecificStop = threadSpecificCPA.getStopOperator();
    final Precision threadSpecificPrecision;
    try {
      threadSpecificPrecision =
          threadSpecificCPA.getInitialPrecision(CFANode.newDummyCFANode(),
              StateSpacePartition.getDefaultPartition());
    } catch (InterruptedException pE) {
      throw new IllegalArgumentException("Could not get initial precision for thread-specific CPA");
    }
    return (state, reached, precision) -> {
      if (state instanceof ConcurrentState concurrentState && precision instanceof ConcurrentPrecision concurrentPrecision) {
        ImmutableList.Builder<AbstractState> builder = ImmutableList.builder();
        for (AbstractState reachedState : reached) {
          if (reachedState instanceof ConcurrentState reachedConcurrentState
              && Objects.equals(concurrentState.threads().size(), reachedConcurrentState.threads().size())
              && Objects.equals(concurrentState.livePids(), reachedConcurrentState.livePids())) {
            boolean allThreadsStop = true;
            for (var entry : concurrentState.threads.entrySet()) {
              int pid = entry.getKey();
              ThreadState reachedThreadState = reachedConcurrentState.threads.get(pid);
              if (reachedThreadState == null) {
                allThreadsStop = false;
                break;
              }
              ThreadState threadState = entry.getValue();
              if (!threadSpecificStop.stop(threadState.getWrappedState(), List.of(reachedThreadState.getWrappedState()), threadSpecificPrecision)) {
                allThreadsStop = false;
                break;
              }
            }
            if (allThreadsStop) {
              builder.add(reachedConcurrentState.getWrappedState());
            }
          }
        }
        return getWrappedCpa()
            .getStopOperator()
            .stop(concurrentState.getWrappedState(), builder.build(), concurrentPrecision.getWrappedPrecision());
      }
      return false;
    };
  }
}
