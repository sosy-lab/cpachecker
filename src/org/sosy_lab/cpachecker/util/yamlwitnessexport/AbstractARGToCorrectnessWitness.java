// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import java.io.IOException;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.ReportingMethodNotImplementedException;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

/**
 * An abstract base for a writer of correctness witnesses, both for original and transformed
 * programs.
 */
public abstract class AbstractARGToCorrectnessWitness extends AbstractYAMLWitnessExporter {

  AbstractARGToCorrectnessWitness(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  /**
   * Export the given ARG to a witness file in YAML format. All versions of witnesses will be
   * exported. It also prints output information to the user explaining what guarantees are provided
   * by the witness.
   *
   * @param pRootState The root state of the ARG.
   * @param pReachedSet The reached set.
   * @param pOutputFileTemplate The template for the output file. The template will be used to
   *     generate unique names for each witness version by replacing the string '%s' with the
   *     version.
   * @throws InterruptedException If the witness export was interrupted.
   * @throws IOException If the witness could not be written to the file.
   */
  public abstract void export(
      ARGState pRootState, UnmodifiableReachedSet pReachedSet, PathTemplate pOutputFileTemplate)
      throws InterruptedException, IOException, ReportingMethodNotImplementedException;
}
