// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import java.io.IOException;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.AcslToCorrectnessWitness;

/** Writes the ACSL annotations of the program as correctness witness, without any analysis. */
public class AcslToWitnessAlgorithm implements Algorithm {

  private final AcslToCorrectnessWitness exporter;
  private final LogManager logger;

  public AcslToWitnessAlgorithm(
      Configuration pConfig, LogManager pLogger, CFA pCfa, Specification pSpecification)
      throws InvalidConfigurationException {
    exporter = new AcslToCorrectnessWitness(pConfig, pCfa, pSpecification, pLogger);
    logger = pLogger;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) {
    try {
      exporter.export();
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not export the witness");
    }
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }
}
