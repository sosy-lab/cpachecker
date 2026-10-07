// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ProgramTransformation;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.witnesses.RelevantArgStatesCollector;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.ARGToCorrectnessWitnessV2;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.AbstractARGToCorrectnessWitness;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.CounterexampleToWitness;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.SequentializedARGToWitness;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.SequentializedCounterexampleToWitness;

/**
 * The transformation of a concurrent program into an equivalent sequential one, which is analyzed
 * in its stead.
 *
 * @param originalCfa the CFA of the concurrent input program
 * @param mapping relates the elements of the sequentialization to the input program. It is absent
 *     if building the sequentialization failed, in which case the input program itself is analyzed.
 */
public record MporSequentialization(CFA originalCfa, Optional<SequentializationMapping> mapping)
    implements ProgramTransformation {

  @Override
  public boolean isSuccessful() {
    // The mapping is empty when the sequentialization fails.
    return mapping.isPresent();
  }

  @Override
  public AbstractARGToCorrectnessWitness createARGToCorrectnessWitness(
      Configuration pConfig,
      CFA pCfa,
      Specification pSpecification,
      LogManager pLogger,
      RelevantArgStatesCollector pArgStatesCollector)
      throws InvalidConfigurationException {

    // If the transformation is unsuccessful, fall back to the default ARG to correctness witness.
    if (isSuccessful()) {
      return new SequentializedARGToWitness(pConfig, pCfa, pSpecification, pLogger);
    } else {
      return new ARGToCorrectnessWitnessV2(
          pConfig, pCfa, pSpecification, pLogger, pArgStatesCollector);
    }
  }

  @Override
  public CounterexampleToWitness createCounterexampleToWitness(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {

    // If the transformation is unsuccessful, we fall back to the default counterexample.
    if (isSuccessful()) {
      return new SequentializedCounterexampleToWitness(
          pConfig, pCfa, mapping.orElseThrow(), pSpecification, pLogger);
    } else {
      return new CounterexampleToWitness(pConfig, pCfa, pSpecification, pLogger);
    }
  }
}
