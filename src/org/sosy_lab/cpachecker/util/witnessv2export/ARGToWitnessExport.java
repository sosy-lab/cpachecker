// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.witnessv2export;

import java.io.IOException;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.InvariantWitnessWriter.YamlWitnessExportException;
import org.sosy_lab.cpachecker.util.witnessv2export.DataTypes.WitnessVersion;

public class ARGToWitnessExport extends DirectWitnessExporter {

  private final ARGToWitnessV2 argToWitnessV2;
  private final ARGToWitnessV3 argToWitnessV3;

  public ARGToWitnessExport(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
    argToWitnessV2 = new ARGToWitnessV2(pConfig, pCfa, pSpecification, pLogger);
    argToWitnessV3 = new ARGToWitnessV3(pConfig, pCfa, pSpecification, pLogger);
  }

  public void export(ARGState pRootState)
      throws YamlWitnessExportException, InterruptedException, IOException {
    for (WitnessVersion witnessVersion : witnessVersions) {
      switch (witnessVersion) {
        case V2:
          argToWitnessV2.exportWitnesses(pRootState);
          break;
        case V3:
          logger.log(Level.INFO, "Exporting witnesses in Version 3 is currently WIP.");
          argToWitnessV3.exportWitness(pRootState);
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
