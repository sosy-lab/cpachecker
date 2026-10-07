// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.CounterexampleToWitness;

/**
 * A transformation of a program into another program that is analyzed in its stead. Implementations
 * carry whatever a consumer needs to map a result for the analyzed program back to the original
 * one, so this interface only provides what every transformation has.
 */
public interface ProgramTransformation {

  /** Returns the CFA of the program that this transformation was applied to. */
  CFA originalCfa();

  /**
   * Returns an instance of {@link CounterexampleToWitness}, which may be a subclass for this
   * specific {@link ProgramTransformation}. If the counterexample is not mapped back from a
   * transformed {@link CFA} to the original {@link CFA}, then {@link CounterexampleToWitness} may
   * be returned directly without any handling of the transformation.
   */
  CounterexampleToWitness createCounterexampleToWitness(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException;
}
