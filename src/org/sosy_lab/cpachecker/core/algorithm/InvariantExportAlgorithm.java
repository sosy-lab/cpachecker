// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.invariants.AbstractInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.DoNothingInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantGenerator;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitness;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitnessFactory;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitnessGenerator;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.InvariantWitnessWriter;
import org.sosy_lab.cpachecker.util.predicates.invariants.ExpressionTreeInvariantSupplier;
import org.sosy_lab.cpachecker.util.predicates.invariants.FormulaInvariantsSupplier;

/**
 * Rudimentary version of InvariantExportAlgorithm.
 *
 * <p>The algorithm basically mimics BMC with auxiliary invariants, but exports them instead of
 * using them for BMC. The goal is to use the invariants for coop. verification (through the
 * invariant store).
 *
 * <p>The algorithm is rudimentary because it is only a proof of concept impl. Most missing parts
 * are marked with todo comments in the code. There is an example configuration
 * (invariantExport.prop) for trying out the new algorithm.
 */
@Options(prefix = "invariantStore")
public class InvariantExportAlgorithm implements Algorithm {

  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private final ShutdownNotifier shutdownNotifier;
  private final InvariantGenerator generator;
  private final InvariantWitnessFactory invariantWitnessFactory;
  private final InvariantWitnessWriter invariantWitnessWriter;

  private final Set<InvariantWitness> alreadyExported;

  @Option(secure = true, description = "Strategy for generating invariants")
  private InvariantGeneratorFactory invariantGenerationStrategy =
      InvariantGeneratorFactory.REACHED_SET;

  public InvariantExportAlgorithm(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownManager pShutdownManager,
      final CFA pCFA,
      final Specification pSpecification,
      final ReachedSetFactory pReachedSetFactory,
      final TargetLocationProvider pTargetLocationProvider,
      final AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, InterruptedException, CPAException {
    config = pConfig;
    config.inject(this);
    logger = pLogger;
    cfa = pCFA;
    shutdownNotifier = pShutdownManager.getNotifier();
    invariantWitnessFactory = InvariantWitnessFactory.getFactory(pLogger, pCFA);
    try {
      invariantWitnessWriter =
          InvariantWitnessWriter.getWriter(pConfig, pCFA, pSpecification, pLogger);
    } catch (IOException e) {
      throw new CPAException("could not instantiate invariant witness writer", e);
    }

    alreadyExported = new HashSet<>();

    generator =
        invariantGenerationStrategy.createInvariantGenerator(
            pConfig,
            pLogger,
            pReachedSetFactory,
            pShutdownManager,
            pCFA,
            pSpecification,
            pAggregatedReachedSets,
            pTargetLocationProvider);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    generator.start(cfa.getMainFunction());
    try {
      // TODO replace polling with a smarter strategy!
      for (int i = 0; i < Integer.MAX_VALUE; i++) {
        handleNewInvariants();

        Thread.sleep(2000);
      }
      handleNewInvariants();
    } catch (IOException e) {
      logger.logException(Level.SEVERE, e, "Cannot write inavariant witnesses");
      generator.cancel();
    }

    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  private void handleNewInvariants() throws IOException, InterruptedException {
    shutdownNotifier.shutdownIfNecessary();
    if (!generator.isStarted()) {
      return;
    }

    logger.log(Level.INFO, "Checking for invariants to export ...");
    ExpressionTreeSupplier supplier = getCurrentSupplier();
    ImmutableSet.Builder<InvariantWitness> witnesses = ImmutableSet.builder();
    for (CFANode node : cfa.nodes()) {
      ExpressionTree<Object> invariant = supplier.getInvariantFor(node);

      if (invariant.equals(ExpressionTrees.getTrue())) {
        continue;
      }

      witnesses.addAll(invariantWitnessFactory.fromNodeAndInvariant(node, invariant));
    }
    exportWitnesses(Sets.difference(witnesses.build(), alreadyExported));
  }

  private ExpressionTreeSupplier getCurrentSupplier() {
    try {
      return generator.getExpressionTreeSupplier();
    } catch (CPAException e) {
      logger.logUserException(Level.FINE, e, "Invariant generation failed.");
    } catch (InterruptedException e) {
      logger.log(Level.FINE, "Invariant generation was cancelled.");
      logger.logDebugException(e);
    }
    return ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;
  }

  private void exportWitnesses(final Set<InvariantWitness> witnesses) throws IOException {
    // TODO: Possibly run multithreaded ?
    if (witnesses.isEmpty()) {
      return;
    }
    logger.log(Level.INFO, "Found " + witnesses.size() + " new invariants and will export them.");
    for (InvariantWitness witness : witnesses) {
      alreadyExported.add(witness);
      logger.log(Level.INFO, "Exporting invariant " + witness);
      invariantWitnessWriter.exportInvariantWitness(witness);
    }
  }

  // TODO This is copy&paste from BMC Algorithm. Refactor!
  public enum InvariantGeneratorFactory {
    INDUCTION {

      @Override
      InvariantGenerator createInvariantGenerator(
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
      InvariantGenerator createInvariantGenerator(
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
      InvariantGenerator createInvariantGenerator(
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
      InvariantGenerator createInvariantGenerator(
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
          throw new CPAException("Could not instantiate from disk invariant generator", e);
        }
      }
    };

    abstract InvariantGenerator createInvariantGenerator(
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
}
