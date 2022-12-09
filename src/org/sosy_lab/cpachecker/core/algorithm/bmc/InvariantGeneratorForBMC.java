// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.assertAt;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.filterFirstIteration;

import com.google.common.collect.FluentIterable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm.ConditionAdjustmentEventSubscriber;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.FormulaInContext;
import org.sosy_lab.cpachecker.core.algorithm.invariants.AbstractInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.DoNothingInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantGenerator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitnessGenerator;
import org.sosy_lab.cpachecker.util.predicates.invariants.ExpressionTreeInvariantSupplier;
import org.sosy_lab.cpachecker.util.predicates.invariants.FormulaInvariantsSupplier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

/***
 * A wrapper class for invariant generation in BMC-based algorithm (including k-induction, IMC, and ISMC).
 */
@Options(prefix = "bmc")
public class InvariantGeneratorForBMC implements StatisticsProvider {
  public enum InvariantGeneratorFactory {
    INDUCTION {

      @Override
      public InvariantGenerator createInvariantGenerator(
          Configuration pConfig,
          LogManager pLogger,
          ReachedSetFactory pReachedSetFactory,
          ShutdownManager pShutdownManager,
          CFA pCFA,
          Specification pSpecification,
          AggregatedReachedSets pAggregatedReachedSets,
          TargetLocationProvider pTargetLocationProvider)
          throws InvalidConfigurationException, CPAException, InterruptedException {
        return KInductionInvariantGenerator.create(
            pConfig,
            pLogger,
            pShutdownManager,
            pCFA,
            pSpecification,
            pReachedSetFactory,
            pTargetLocationProvider,
            pAggregatedReachedSets);
      }
    },

    REACHED_SET {
      @Override
      public InvariantGenerator createInvariantGenerator(
          Configuration pConfig,
          LogManager pLogger,
          ReachedSetFactory pReachedSetFactory,
          ShutdownManager pShutdownManager,
          CFA pCFA,
          Specification pSpecification,
          AggregatedReachedSets pAggregatedReachedSets,
          TargetLocationProvider pTargetLocationProvider) {
        return new AbstractInvariantGenerator() {

          @Override
          protected void startImpl(CFANode pInitialLocation) {
            // do nothing
          }

          @Override
          public boolean isProgramSafe() {
            // just return false, program will be ended by parallel algorithm if the invariant
            // generator can prove safety before the current analysis
            return false;
          }

          @Override
          public void cancel() {
            // do nothing
          }

          @Override
          public InvariantSupplier getSupplier() throws CPAException, InterruptedException {
            return new FormulaInvariantsSupplier(pAggregatedReachedSets);
          }

          @Override
          public ExpressionTreeSupplier getExpressionTreeSupplier()
              throws CPAException, InterruptedException {
            return new ExpressionTreeInvariantSupplier(pAggregatedReachedSets, pCFA);
          }
        };
      }
    },

    DO_NOTHING {

      @Override
      public InvariantGenerator createInvariantGenerator(
          Configuration pConfig,
          LogManager pLogger,
          ReachedSetFactory pReachedSetFactory,
          ShutdownManager pShutdownManager,
          CFA pCFA,
          Specification pSpecification,
          AggregatedReachedSets pAggregatedReachedSets,
          TargetLocationProvider pTargetLocationProvider) {
        return new DoNothingInvariantGenerator();
      }
    },

    INVARIANT_STORE {
      @Override
      public InvariantGenerator createInvariantGenerator(
          Configuration pConfig,
          LogManager pLogger,
          ReachedSetFactory pReachedSetFactory,
          ShutdownManager pShutdownManager,
          CFA pCFA,
          Specification pSpecification,
          AggregatedReachedSets pAggregatedReachedSets,
          TargetLocationProvider pTargetLocationProvider)
          throws InvalidConfigurationException, CPAException, InterruptedException {
        try {
          return InvariantWitnessGenerator.getNewFromDiskInvariantGenerator(
              pConfig, pCFA, pLogger, pShutdownManager.getNotifier());
        } catch (IOException e) {
          throw new CPAException("Could not create from disk generator", e);
        }
      }
    };

    public abstract InvariantGenerator createInvariantGenerator(
        Configuration pConfig,
        LogManager pLogger,
        ReachedSetFactory pReachedSetFactory,
        ShutdownManager pShutdownManager,
        CFA pCFA,
        Specification pSpecification,
        AggregatedReachedSets pAggregatedReachedSets,
        TargetLocationProvider pTargetLocationProvider)
        throws InvalidConfigurationException, CPAException, InterruptedException;
  }

  private interface InvariantGeneratorHeadStart {

    void waitForInvariantGenerator() throws InterruptedException;
  }

  private enum InvariantGeneratorHeadStartFactories {
    NONE {

      @Override
      public InvariantGeneratorHeadStart createFor(
          InvariantGeneratorForBMC pInvariantGeneratorForBMC) {
        return new InvariantGeneratorHeadStart() {

          @Override
          public void waitForInvariantGenerator() throws InterruptedException {
            // Return immediately
          }
        };
      }
    },

    AWAIT_TERMINATION {

      @Override
      public InvariantGeneratorHeadStart createFor(
          InvariantGeneratorForBMC pInvariantGeneratorForBMC) {
        CountDownLatch latch = new CountDownLatch(1);
        pInvariantGeneratorForBMC.conditionAdjustmentEventSubscribers.add(
            new ConditionAdjustmentEventSubscriber() {

              @Override
              public void adjustmentSuccessful(ConfigurableProgramAnalysis pCpa) {
                // Ignore
              }

              @Override
              public void adjustmentRefused(ConfigurableProgramAnalysis pCpa) {
                latch.countDown();
              }
            });
        return new InvariantGeneratorHeadStartFactories.HeadStartWithLatch(
            pInvariantGeneratorForBMC, latch);
      }
    },

    WAIT_UNTIL_EXPENSIVE_ADJUSTMENT {

      @Override
      InvariantGeneratorHeadStart createFor(InvariantGeneratorForBMC pInvariantGeneratorForBMC) {
        CountDownLatch latch = new CountDownLatch(1);
        pInvariantGeneratorForBMC.conditionAdjustmentEventSubscribers.add(
            new ConditionAdjustmentEventSubscriber() {

              @Override
              public void adjustmentSuccessful(ConfigurableProgramAnalysis pCpa) {
                FluentIterable<InvariantsCPA> cpas =
                    CPAs.asIterable(pCpa).filter(InvariantsCPA.class);
                if (cpas.isEmpty()) {
                  latch.countDown();
                } else {
                  for (InvariantsCPA invariantCpa : cpas) {
                    if (invariantCpa.isLikelyLongRunning()) {
                      latch.countDown();
                      break;
                    }
                  }
                }
              }

              @Override
              public void adjustmentRefused(ConfigurableProgramAnalysis pCpa) {
                latch.countDown();
              }
            });
        return new InvariantGeneratorHeadStartFactories.HeadStartWithLatch(
            pInvariantGeneratorForBMC, latch);
      }
    };

    private static final class HeadStartWithLatch implements InvariantGeneratorHeadStart {

      private final CountDownLatch latch;

      @SuppressWarnings("UnnecessaryAnonymousClass") // ShutdownNotifier needs a strong reference
      private final ShutdownRequestListener shutdownListener =
          new ShutdownRequestListener() {

            @Override
            public void shutdownRequested(String pReason) {
              latch.countDown();
            }
          };

      public HeadStartWithLatch(
          InvariantGeneratorForBMC pInvariantGeneratorForBMC, CountDownLatch pLatch) {
        latch = Objects.requireNonNull(pLatch);
        pInvariantGeneratorForBMC.shutdownNotifier.registerAndCheckImmediately(shutdownListener);
      }

      @Override
      public void waitForInvariantGenerator() throws InterruptedException {
        latch.await();
      }
    }

    abstract InvariantGeneratorHeadStart createFor(
        InvariantGeneratorForBMC pInvariantGeneratorForBMC);
  }

  @Option(secure = true, description = "Strategy for generating auxiliary invariants")
  private InvariantGeneratorFactory invariantGenerationStrategy =
      InvariantGeneratorFactory.DO_NOTHING;

  @Option(
      secure = true,
      description =
          "Controls how long the invariant generator is allowed to run before the k-induction"
              + " procedure starts.")
  private InvariantGeneratorHeadStartFactories invariantGeneratorHeadStartStrategy =
      InvariantGeneratorHeadStartFactories.NONE;

  @Option(
      secure = true,
      description =
          "k-induction configuration to be used as an invariant generator for k-induction"
              + " (ki-ki(-ai)).")
  @FileOption(value = Type.OPTIONAL_INPUT_FILE)
  private @Nullable Path invariantGeneratorConfig = null;

  @Option(secure = true, description = "Propagates the interrupts of the invariant generator.")
  private boolean propagateInvGenInterrupts = false;

  private final List<ConditionAdjustmentEventSubscriber> conditionAdjustmentEventSubscribers;
  private final InvariantGenerator invariantGenerator;
  private final InvariantGeneratorHeadStart invariantGeneratorHeadStart;
  private boolean invariantGenerationRunning;
  private final ShutdownNotifier shutdownNotifier;
  private final LogManager logger;
  private BooleanFormula loopHeadInvariants;

  InvariantGeneratorForBMC(
      ShutdownManager pShutdownManager,
      boolean pAlgIsInvariantGenerator,
      Configuration pConfig,
      LogManager pLogger,
      ReachedSetFactory pReachedSetFactory,
      CFA pCFA,
      final Specification pSpecification,
      AggregatedReachedSets pAggregatedReachedSets,
      TargetLocationProvider pTargetLocationProvider,
      BooleanFormula pInitialInvariants)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    pConfig.inject(this);
    shutdownNotifier = pShutdownManager.getNotifier();
    invariantGenerationRunning = true;
    loopHeadInvariants = pInitialInvariants;
    conditionAdjustmentEventSubscribers = new CopyOnWriteArrayList<>();
    logger = pLogger;
    ShutdownManager invariantGeneratorShutdownManager = pShutdownManager;
    boolean addInvariantsByInduction =
        invariantGenerationStrategy == InvariantGeneratorFactory.INDUCTION;
    if (addInvariantsByInduction) {
      if (propagateInvGenInterrupts) {
        invariantGeneratorShutdownManager = pShutdownManager;
      } else {
        invariantGeneratorShutdownManager =
            ShutdownManager.createWithParent(pShutdownManager.getNotifier());
      }
      ShutdownRequestListener propagateSafetyInterrupt =
          new ShutdownRequestListener() {

            @Override
            public void shutdownRequested(String pReason) {
              if (invariantGenerator != null && invariantGenerator.isProgramSafe()) {
                pShutdownManager.requestShutdown(pReason);
              }
            }
          };
      invariantGeneratorShutdownManager.getNotifier().register(propagateSafetyInterrupt);
    }

    if (pAlgIsInvariantGenerator && addInvariantsByInduction) {
      invariantGenerationStrategy = InvariantGeneratorFactory.REACHED_SET;
    }
    Configuration invGenConfig = pConfig;
    if (invariantGeneratorConfig != null) {
      try {
        invGenConfig =
            Configuration.builder()
                .copyFrom(invGenConfig)
                .loadFromFile(invariantGeneratorConfig)
                .build();
      } catch (IOException e) {
        throw new InvalidConfigurationException(
            String.format("Cannot load configuration from file %s", invariantGeneratorConfig), e);
      }
    }
    invariantGenerator =
        invariantGenerationStrategy.createInvariantGenerator(
            invGenConfig,
            pLogger,
            pReachedSetFactory,
            invariantGeneratorShutdownManager,
            pCFA,
            pSpecification,
            pAggregatedReachedSets,
            pTargetLocationProvider);
    if (invariantGenerator instanceof ConditionAdjustmentEventSubscriber) {
      conditionAdjustmentEventSubscribers.add(
          (ConditionAdjustmentEventSubscriber) invariantGenerator);
    }
    invariantGeneratorHeadStart = invariantGeneratorHeadStartStrategy.createFor(this);
  }

  void start(CFANode initLocation) {
    invariantGenerator.start(initLocation);
  }

  void waitForHeadStart() throws InterruptedException {
    invariantGeneratorHeadStart.waitForInvariantGenerator();
  }

  void cancel() {
    invariantGenerator.cancel();
  }

  private FluentIterable<AbstractState> getLoopHeadStatesAtFirstIteration(
      ReachedSet reachedSet, Set<CFANode> loopHeadLocs) {
    return filterFirstIteration(
        AbstractStates.filterLocations(reachedSet, loopHeadLocs), loopHeadLocs);
  }

  BooleanFormula getInvariantsAssertion(
      ReachedSet reachedSet,
      Set<CFANode> loopHeadLocs,
      FormulaManagerView fmgr,
      PathFormulaManager pfmgr)
      throws CPATransferException, InterruptedException {
    Iterable<AbstractState> loopHeadStates =
        getLoopHeadStatesAtFirstIteration(reachedSet, loopHeadLocs);
    return assertAt(loopHeadStates, getInvariantsInContext(loopHeadStates, fmgr, pfmgr), fmgr);
  }

  BooleanFormula getInvariants(
      ReachedSet reachedSet,
      Set<CFANode> loopHeadLocs,
      FormulaManagerView fmgr,
      PathFormulaManager pfmgr)
      throws CPATransferException, InterruptedException {
    Iterable<AbstractState> loopHeadStates =
        getLoopHeadStatesAtFirstIteration(reachedSet, loopHeadLocs);
    return assertAt(
        loopHeadStates, getInvariantsInContext(loopHeadStates, fmgr, pfmgr), fmgr, true);
  }

  BooleanFormula getUninstantiatedInvariants(
      ReachedSet reachedSet,
      Set<CFANode> loopHeadLocs,
      FormulaManagerView fmgr,
      PathFormulaManager pfmgr)
      throws CPATransferException, InterruptedException {
    return fmgr.uninstantiate(getInvariants(reachedSet, loopHeadLocs, fmgr, pfmgr));
  }

  boolean isProgramSafe() {
    return invariantGenerator.isProgramSafe();
  }

  /**
   * Gets the most current invariants generated by the invariant generator.
   *
   * @return the most current invariants generated by the invariant generator.
   */
  private FormulaInContext getInvariantsInContext(
      Iterable<AbstractState> pAssertionStates, FormulaManagerView fmgr, PathFormulaManager pfmgr) {
    final BooleanFormulaManager bfmgr = fmgr.getBooleanFormulaManager();
    Set<CFANode> stopLoopHeads = AbstractStates.extractLocations(pAssertionStates).toSet();
    return pContext -> {
      shutdownNotifier.shutdownIfNecessary();
      if (!bfmgr.isFalse(loopHeadInvariants) && invariantGenerationRunning) {
        BooleanFormula lhi = bfmgr.makeFalse();
        for (CFANode loopHead : stopLoopHeads) {
          shutdownNotifier.shutdownIfNecessary();
          InvariantSupplier currentInvariantsSupplier = getInvariantSupplier();
          BooleanFormula locationInv =
              currentInvariantsSupplier.getInvariantFor(
                  loopHead, Optional.empty(), fmgr, pfmgr, pContext);
          lhi = bfmgr.or(lhi, locationInv);
          shutdownNotifier.shutdownIfNecessary();
        }
        for (CFANode loopHead : stopLoopHeads) {
          lhi = bfmgr.or(lhi, getLocationInvariants(loopHead, fmgr, pfmgr, pContext));
          shutdownNotifier.shutdownIfNecessary();
        }
        loopHeadInvariants = lhi;
      }
      return loopHeadInvariants;
    };
  }

  BooleanFormula getLocationInvariants(
      CFANode pLocation,
      FormulaManagerView pFormulaManager,
      PathFormulaManager pPathFormulaManager,
      PathFormula pContext)
      throws InterruptedException {
    shutdownNotifier.shutdownIfNecessary();
    return getInvariantSupplier()
        .getInvariantFor(
            pLocation, Optional.empty(), pFormulaManager, pPathFormulaManager, pContext);
  }

  private InvariantSupplier getInvariantSupplier() throws InterruptedException {
    if (invariantGenerationRunning) {
      try {
        return invariantGenerator.getSupplier();
      } catch (CPAException e) {
        logger.logUserException(Level.FINE, e, "Invariant generation failed.");
        invariantGenerationRunning = false;
      } catch (InterruptedException e) {
        shutdownNotifier.shutdownIfNecessary();
        logger.log(Level.FINE, "Invariant generation was cancelled.");
        logger.logDebugException(e);
        invariantGenerationRunning = false;
      }
    }
    return InvariantSupplier.TrivialInvariantSupplier.INSTANCE;
  }

  ExpressionTreeSupplier getExpressionTreeSupplier() throws InterruptedException {
    if (invariantGenerationRunning) {
      try {
        return invariantGenerator.getExpressionTreeSupplier();
      } catch (CPAException e) {
        logger.logUserException(Level.FINE, e, "Invariant generation failed.");
        invariantGenerationRunning = false;
      } catch (InterruptedException e) {
        shutdownNotifier.shutdownIfNecessary();
        logger.log(Level.FINE, "Invariant generation was cancelled.");
        logger.logDebugException(e);
        invariantGenerationRunning = false;
      }
    }
    return ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;
  }

  ExpressionTree<Object> getLocationInvariants(CFANode pLocation) throws InterruptedException {
    return getExpressionTreeSupplier().getInvariantFor(pLocation);
  }

  void adjustmentSuccessful(ConfigurableProgramAnalysis pCpa) {
    for (ConditionAdjustmentEventSubscriber caes : conditionAdjustmentEventSubscribers) {
      caes.adjustmentSuccessful(pCpa);
    }
  }

  void adjustmentRefused(ConfigurableProgramAnalysis pCpa) {
    for (ConditionAdjustmentEventSubscriber caes : conditionAdjustmentEventSubscribers) {
      caes.adjustmentRefused(pCpa);
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (invariantGenerator instanceof StatisticsProvider) {
      ((StatisticsProvider) invariantGenerator).collectStatistics(pStatsCollection);
    }
  }
}
