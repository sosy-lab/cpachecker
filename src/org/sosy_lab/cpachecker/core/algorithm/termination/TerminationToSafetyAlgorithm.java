// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.terminationviamemory.TerminationToReachCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;

public class TerminationToSafetyAlgorithm implements Algorithm {

  private final ConfigurableProgramAnalysis cpa;

  public TerminationToSafetyAlgorithm(Configuration pConfig, ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    cpa = checkNotNull(pCpa);

    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder.copyFrom(pConfig);
    configBuilder.clearOption("analysis.algorithm.terminationToSafety");

    TerminationToReachCPA terminationCPA =
        CPAs.retrieveCPAOrFail(
            cpa, TerminationToReachCPA.class, TerminationToSafetyAlgorithm.class);
    PredicateCPA predicateCPA =
        CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, TerminationToSafetyAlgorithm.class);

    terminationCPA.setSolver(predicateCPA.getSolver());
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }
}
