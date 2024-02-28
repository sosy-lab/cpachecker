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
  private final ARGToWitnessV3 argToWitnessV3;

  public ARGToYAMLWitnessExport(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
    argToWitnessV2 = new ARGToWitnessV2(pConfig, pCfa, pSpecification, pLogger);
    argToWitnessV3 = new ARGToWitnessV3(pConfig, pCfa, pSpecification, pLogger);
  }

  /**
   * Export the given ARG to a witness file in YAML format. All versions of witnesses will be
   * exported.
   *
   * @param pRootState The root state of the ARG.
   * @param pOutputFileTemplate The template for the output file. The template will be used to
   *     generate unique names for each witness version by replacing the string '%s' with the
   *     version.
   * @throws YamlWitnessExportException If the witness could not be exported.
   * @throws InterruptedException If the witness export was interrupted.
   * @throws IOException If the witness could not be written to the file.
   */
  public void export(ARGState pRootState, PathTemplate pOutputFileTemplate)
      throws YamlWitnessExportException, InterruptedException, IOException {
    for (YAMLWitnessVersion witnessVersion : witnessVersions) {
      Path outputFile = getOutputFile(witnessVersion, pOutputFileTemplate);
      switch (witnessVersion) {
        case V2 -> argToWitnessV2.exportWitnesses(pRootState, outputFile);
        case V3 -> {
          logger.log(Level.INFO, "Exporting witnesses in Version 3 is currently WIP.");
          argToWitnessV3.exportWitness(pRootState, outputFile);
        }
      }
    }
  }
}
