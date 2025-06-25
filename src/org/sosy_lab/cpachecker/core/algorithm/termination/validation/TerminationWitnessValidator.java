// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TerminationWitnessValidator implements Algorithm {

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdown;
  private final CFA cfa;
  private final Automaton witness;

  public TerminationWitnessValidator(
      final CFA pCfa,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final ImmutableList<Automaton> pSpecificationAutomata)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    cfa = pCfa;
    config = pConfig;
    logger = pLogger;
    shutdown = pShutdownNotifier;

    if (pSpecificationAutomata.size() < 1) {
      throw new InvalidConfigurationException("Witness file is missing in specification.");
    }
    if (pSpecificationAutomata.size() != 1) {
      throw new InvalidConfigurationException(
          "Expect that only violation witness is part of the specification.");
    }

    witness = pSpecificationAutomata.get(0);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }
}
