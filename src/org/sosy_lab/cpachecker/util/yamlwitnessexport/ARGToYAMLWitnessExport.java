// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class ARGToYAMLWitnessExport extends AbstractYAMLWitnessExporter {

  private final ARGToWitnessV2 argToWitnessV2;
  private final ARGToWitnessV2d1 argToWitnessV2d1;

  public ARGToYAMLWitnessExport(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
    argToWitnessV2 = new ARGToWitnessV2(pConfig, pCfa, pSpecification, pLogger);
    argToWitnessV2d1 = new ARGToWitnessV2d1(pConfig, pCfa, pSpecification, pLogger);
  }

  /**
   * Export the given ARG to a witness file in YAML format. All versions of witnesses will be
   * exported.
   *
   * @param pRootState The root state of the ARG.
   * @param pOutputFileTemplate The template for the output file. The template will be used to
   *     generate unique names for each witness version by replacing the string '%s' with the
   *     version.
   * @throws InterruptedException If the witness export was interrupted.
   * @throws IOException If the witness could not be written to the file.
   */
  public void export(ARGState pRootState, PathTemplate pOutputFileTemplate)
      throws InterruptedException, IOException, UnsupportedOperationException {
    for (YAMLWitnessVersion witnessVersion : witnessVersions) {
      Path outputFile = pOutputFileTemplate.getPath(witnessVersion.toString());
      switch (witnessVersion) {
        case V2 -> argToWitnessV2.exportWitnesses(pRootState, outputFile);
        case V2d1 -> {
          logger.log(Level.INFO, "Exporting witnesses in Version 2.1 is currently WIP.");
          argToWitnessV2d1.exportWitness(pRootState, outputFile);
        }
        default -> throw new AssertionError("Unknown witness version: " + witnessVersion);
      }
    }
  }
}
