// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.terminationviamemory.TerminationToReachCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TerminationToSafetyAlgorithm implements Algorithm {

  private final Configuration configuration;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final AggregatedReachedSets aggregatedReachedSets;
  private final Specification specification;
  private final CFA cfa;
  private final ConfigurableProgramAnalysis cpa;
  private final Algorithm algorithm;

  public TerminationToSafetyAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      AggregatedReachedSets pAggregatedReachedSets,
      Specification pSpecification,
      CFA pCfa,
      ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    logger = checkNotNull(pLogger);
    aggregatedReachedSets = pAggregatedReachedSets;
    specification = pSpecification;
    shutdownNotifier = pShutdownNotifier;
    cfa = checkNotNull(pCfa);
    cpa = checkNotNull(pCpa);
    PredicateCPA predicateCPA = null;
    TerminationToReachCPA terminationCPA = null;

    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder.copyFrom(pConfig);
    configBuilder.clearOption("analysis.algorithm.terminationToSafety");
    configuration = configBuilder.build();

    CoreComponentsFactory factory =
        new CoreComponentsFactory(
            configuration, logger, shutdownNotifier, aggregatedReachedSets, cfa);
    if (cpa instanceof ARGCPA pArgCpa) {
      CompositeCPA compositeCPA = null;
      for (ConfigurableProgramAnalysis wrappedCpa : pArgCpa.getWrappedCPAs()) {
        if (wrappedCpa instanceof CompositeCPA pCompositeCPA) {
          compositeCPA = pCompositeCPA;
        }
      }
      if (compositeCPA == null) {
        throw new InvalidConfigurationException("TerminationToSafety needs CompositeCPA!");
      }

      for (ConfigurableProgramAnalysis wrappedCpa : compositeCPA.getWrappedCPAs()) {
        if (wrappedCpa instanceof PredicateCPA pPredicateCPA) {
          predicateCPA = pPredicateCPA;
        }
        if (wrappedCpa instanceof TerminationToReachCPA pTerminationCPA) {
          terminationCPA = pTerminationCPA;
        }
      }
      if (terminationCPA == null || predicateCPA == null) {
        throw new InvalidConfigurationException(
            "TerminationToSafety needs both PredicateCPA and TerminationToReachCPA!");
      }
    } else {
      throw new InvalidConfigurationException("TerminationToSafety needs ARGCPA!");
    }

    terminationCPA.setSolver(predicateCPA.getSolver());
    algorithm = factory.createAlgorithm(cpa, specification);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    return algorithm.run(reachedSet);
  }
}
