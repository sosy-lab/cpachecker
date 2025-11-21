// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.CfaTransformationMetadata;
import org.sosy_lab.cpachecker.cfa.CfaTransformationMetadata.ProgramTransformation;
import org.sosy_lab.cpachecker.cfa.ImmutableCFA;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * The Modular Partial Order Reduction (MPOR) algorithm produces a sequentialization of a concurrent
 * C program. The algorithm contains options that allow both static and dynamic reductions in the
 * state space. Sequentializations can be given to any verifier capable of verifying sequential C
 * programs, hence modular.
 */
public class MporPreprocessingAlgorithm implements Algorithm /* TODO statistics? */ {

  private final MPOROptions options;

  private final LogManager logger;

  private final ShutdownNotifier shutdownNotifier;

  private final Configuration config;
  private final Specification specification;

  private final ImmutableCFA cfa;

  public MporPreprocessingAlgorithm(
      Configuration pConfiguration,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      ImmutableCFA pInputCfa,
      Specification pSpecification)
      throws InvalidConfigurationException {

    // the options are not null when unit testing
    options = new MPOROptions(pConfiguration);
    logger = pLogManager;
    shutdownNotifier = pShutdownNotifier;
    cfa = pInputCfa;
    config = pConfiguration;
    specification = pSpecification;
  }

  private boolean alreadySequentialized(ImmutableCFA pCFA) {
    CfaTransformationMetadata transformationMetadata =
        pCFA.getMetadata().getTransformationMetadata();
    return transformationMetadata != null
        && transformationMetadata.transformation().equals(ProgramTransformation.SEQUENTIALIZATION);
  }

  private ImmutableCFA preprocessCfaUsingSequentialization(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      ImmutableCFA pCFA)
      throws UnrecognizedCodeException,
          InterruptedException,
          ParserException,
          InvalidConfigurationException {

    pLogger.log(Level.INFO, "Starting sequentialization of the program.");

    // TODO: Statistics about the sequentialization process
    ImmutableCFA originalCfa = pCFA;
    String sequentializedCode = buildSequentializedProgram();
    pCFA =
        new CFACreator(pConfig, pLogger, pShutdownNotifier)
            .parseSourceAndCreateCFA(sequentializedCode);

    pCFA =
        pCFA.copyWithMetadata(
            pCFA.getMetadata()
                .withTransformationMetadata(
                    new CfaTransformationMetadata(
                        originalCfa, ProgramTransformation.SEQUENTIALIZATION)));

    logger.log(Level.INFO, "Finished sequentialization of the program.");

    return pCFA;
  }

  @CanIgnoreReturnValue
  @Override
  public AlgorithmStatus run(@NonNull ReachedSet pReachedSet)
      throws CPAException, InterruptedException {

    // Only sequentialize if not already done and requested.
    // We replace the CFA for its sequentialized version.
    ImmutableCFA newCfa = cfa;
    if (alreadySequentialized(cfa)) {
      logger.log(
          Level.INFO,
          "The CFA is already sequentialized. "
              + "The sequentialization will be ignored. "
              + "If this is part of a parallel algorithm, this may be expected.");
    } else {
      try {
        newCfa = preprocessCfaUsingSequentialization(config, logger, shutdownNotifier, cfa);
      } catch (UnrecognizedCodeException | ParserException | InvalidConfigurationException e) {
        throw new UnsupportedOperationException(e);
      }
    }

    final Algorithm innerAlgorithm;
    final CoreComponentsFactory coreComponents;
    final ConfigurableProgramAnalysis cpa;
    try {
      coreComponents =
          new CoreComponentsFactory(
              config, logger, shutdownNotifier, AggregatedReachedSets.empty(), newCfa);
      cpa = coreComponents.createCPA(specification);
      innerAlgorithm = coreComponents.createAlgorithm(cpa, specification);
    } catch (InvalidConfigurationException e) {
      throw new UnsupportedOperationException(e);
    }

    // Prepare new reached set
    pReachedSet.clear();
    coreComponents.initializeReachedSet(pReachedSet, newCfa.getMainFunction(), cpa);

    return innerAlgorithm.run(pReachedSet);
  }

  public String buildSequentializedProgram()
      throws UnrecognizedCodeException, InterruptedException, InvalidConfigurationException {
    // just use the first input file name for naming purposes
    return Sequentialization.tryBuildProgramString(
        options, cfa, SequentializationUtils.of(cfa, config, logger, shutdownNotifier));
  }
}
