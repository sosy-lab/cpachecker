// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ViolationWitnessAlgorithm implements Algorithm{

  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private final ShutdownNotifier shutdownNotifier;
  private final Specification specification;

  public ViolationWitnessAlgorithm(
    Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCfa, Specification pSpecification)
    throws InvalidConfigurationException {
    config = pConfig;
    config.inject(this);
    logger = pLogger;
    cfa = pCfa;
    shutdownNotifier = pShutdownNotifier;
    specification = pSpecification;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // get cpa
    // extract specification automaton
    // create copy with inverted states and replace it in the CPA
    // extract violation automaton
    // create copy with inverted states and replace it in the CPA
    // run CPA
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }
}
