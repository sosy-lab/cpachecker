// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
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
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

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
          "Use an abstraction-aware POR algorithm. Abstraction-aware POR can ignore certain"
              + " variables during dependency calculation if there is no information about them in"
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
      name = "merge",
      values = {"SEP", "JOIN"},
      toUppercase = true,
      description =
          "Merge operator: SEP never merges states; JOIN delegates to the wrapped merge operator"
              + " when thread states are compatible.")
  private String mergeType = "SEP";

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
      description = "Partial order reduction (POR) algorithm to use. Options: NOPOR, SPOR")
  private String partialOrderReductionAlgorithm = "SPOR";

  private final ConcurrentTransferRelation transferRelation;
  private final ConfigurableProgramAnalysis threadSpecificCPA;
  private final PrecisionAdjustment precisionAdjustment;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final LogManager logger;

  @SuppressWarnings("unused")
  private ConcurrentCPA(
      ConfigurableProgramAnalysis pCpa, Configuration pConfig, LogManager pLogger, CFA pCfa)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    super(pCpa);
    pConfig.inject(this);

    logger = pLogger;

    if (pCfa.getLanguage() != Language.C) {
      throw new UnsupportedCodeException("ConcurrentCPA supports only C programs", null);
    }

    if (abstractionAware && CPAs.retrieveCPA(pCpa, PredicateCPA.class) != null) {
      // Inside a block, the path formula keeps exact values of variables without predicates, so
      // treating accesses to them as independent would prune feasible interleavings.
      BlockOperator blk = new BlockOperator();
      pConfig.inject(blk);
      if (!blk.isSingleBlockEncoding()) {
        throw new InvalidConfigurationException(
            "Abstraction-aware POR with PredicateCPA requires cpa.predicate.blk.threshold = 1");
      }
      // PredicatePrecisionVariableManager supports only predicates that hold at all locations
      @SuppressWarnings("deprecation") // just a sanity check, the option belongs to PredicateCPA
      String predicateSharing = pConfig.getProperty("cpa.predicate.precision.sharing");
      if (!"GLOBAL".equals(predicateSharing)) {
        throw new InvalidConfigurationException(
            "Abstraction-aware POR with PredicateCPA requires cpa.predicate.precision.sharing ="
                + " GLOBAL");
      }
    }
    if (abstractionAware) {
      // fail during construction rather than when the analysis asks for the initial precision
      createVariableManager(
          pCpa.getInitialPrecision(
              pCfa.getMainFunction(), StateSpacePartition.getDefaultPartition()));
    }

    threadSpecificCPA = new ThreadSpecificCPA(pConfig, pCfa, pLogger);

    PartialOrderReductionStrategy strategy =
        switch (partialOrderReductionAlgorithm) {
          case "NOPOR" -> ConcurrentState::new;
          case "SPOR" -> SPORConcurrentState::new;
          default ->
              throw new InvalidConfigurationException(
                  "Unknown partial order reduction algorithm: " + partialOrderReductionAlgorithm);
        };
    transferRelation =
        new ConcurrentTransferRelation(
            pCpa,
            threadSpecificCPA,
            pConfig,
            new ConcurrentCfaClones(pCfa),
            strategy,
            aggregateBasicBlocks,
            pLogger,
            new Random(randomSeed));

    mergeOperator =
        mergeType.equals("SEP")
            ? MergeSepOperator.getInstance()
            : new ConcurrentMergeOperator(pCpa, threadSpecificCPA);
    stopOperator = new ConcurrentStopOperator(pCpa, threadSpecificCPA);

    final PrecisionAdjustment wrappedPrecisionAdjustment = pCpa.getPrecisionAdjustment();
    precisionAdjustment =
        (state, precision, states, stateProjection, fullState) -> {
          if (!(state instanceof ConcurrentState pConcurrentState)
              || !(precision instanceof ConcurrentPrecision pConcurrentPrecision)) {
            throw new CPAException(
                "Expected ConcurrentState, got " + state.getClass().getSimpleName());
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
      PrecisionVariableManager variableManager;
      try {
        variableManager = createVariableManager(initialWrappedPrecision);
      } catch (InvalidConfigurationException e) {
        throw new AssertionError("precision was already checked in the constructor", e);
      }
      return new AbstractionAwarePORPrecision(variableManager, initialWrappedPrecision);
    }

    return new SimpleConcurrentPrecision(wrappedCpa.getInitialPrecision(pNode, pPartition));
  }

  private PrecisionVariableManager createVariableManager(Precision initialWrappedPrecision)
      throws InvalidConfigurationException {
    ConfigurableProgramAnalysis wrappedCpa = getWrappedCpa();
    List<PrecisionVariableManager> variableManagers = new ArrayList<>(1);

    ConfigurablePrecision configurablePrecision =
        Precisions.extractPrecisionByType(initialWrappedPrecision, ConfigurablePrecision.class);
    if (configurablePrecision != null) {
      variableManagers.add(new ConfigurablePrecisionVariableManager());
    }

    ScopedRefinablePrecision scopedRefinablePrecision =
        Precisions.extractPrecisionByType(initialWrappedPrecision, ScopedRefinablePrecision.class);
    if (scopedRefinablePrecision != null) {
      variableManagers.add(new ScopedRefinablePrecisionVariableManager(logger));
    }

    PredicatePrecision predicatePrecision =
        Precisions.extractPrecisionByType(initialWrappedPrecision, PredicatePrecision.class);
    if (predicatePrecision != null) {
      var predicateCPA = CPAs.retrieveCPA(wrappedCpa, PredicateCPA.class);
      if (predicateCPA == null) {
        throw new InvalidConfigurationException(
            "Abstraction-aware POR requires PredicateCPA when using PredicatePrecision, but it is"
                + " not present.");
      }
      var fmgr = predicateCPA.getSolver().getFormulaManager();
      variableManagers.add(new PredicatePrecisionVariableManager(fmgr));
    }

    return switch (variableManagers.size()) {
      case 0 ->
          throw new InvalidConfigurationException(
              "Abstraction-aware POR does not support this precision: " + initialWrappedPrecision);
      case 1 -> variableManagers.getFirst();
      default -> new CompositePrecisionVariableManager(ImmutableList.copyOf(variableManagers));
    };
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }
}
