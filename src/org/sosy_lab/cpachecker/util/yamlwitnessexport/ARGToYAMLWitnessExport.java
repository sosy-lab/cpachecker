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

  public void export(ARGState pRootState, PathTemplate pOutputFileTemplate)
      throws YamlWitnessExportException, InterruptedException, IOException {
    for (YAMLWitnessVersion witnessVersion : witnessVersions) {
      Path outputFile = pOutputFileTemplate.getPath(pRootState, witnessVersion);
      switch (witnessVersion) {
        case V2:
          argToWitnessV2.exportWitnesses(pRootState, outputFile);
          break;
        case V3:
          logger.log(Level.INFO, "Exporting witnesses in Version 3 is currently WIP.");
          argToWitnessV3.exportWitness(pRootState, outputFile);
          break;
        default:
          logger.log(
              Level.WARNING,
              "Witness could not be exported "
                  + "due to unknown witness version "
                  + witnessVersion);
      }
    }
  }
}
