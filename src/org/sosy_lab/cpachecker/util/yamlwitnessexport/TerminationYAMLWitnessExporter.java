// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;

public class TerminationYAMLWitnessExporter extends AbstractYAMLWitnessExporter {

  public TerminationYAMLWitnessExporter(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  private void constructWitness(
      ImmutableList<AbstractInvariantEntry> pTerminationArguments, Path pPath) throws IOException {
    exportEntries(
        new InvariantSetEntry(getMetadata(YAMLWitnessVersion.V2d1), pTerminationArguments), pPath);
  }

  /**
   * Export YAML witness from termination arguments in form of transition invariants and supporting
   * invariants. Termination property is supported only by witnesses of version 2.1 and higher.
   *
   * @param pTerminationArguments in the form of transition invariants and supporting invariants.
   */
  public void export(
      ImmutableList<AbstractInvariantEntry> pTerminationArguments, PathTemplate pOutputFileTemplate)
      throws IOException {

    for (YAMLWitnessVersion witnessVersion : ImmutableSet.copyOf(witnessVersions)) {
      Path outputFile = pOutputFileTemplate.getPath(witnessVersion.toString());
      switch (witnessVersion) {
        case V2 ->
            logger.log(
                Level.SEVERE, "Format in version 2.0 does not support termination witnesses.");
        case V2d1 -> constructWitness(pTerminationArguments, outputFile);
      }
    }
  }
}
