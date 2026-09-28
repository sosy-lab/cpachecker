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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;

@Options(prefix = "witness.yamlexporter")
public class TerminationYAMLWitnessExporter extends AbstractYAMLWitnessExporter {

  @Option(
      secure = true,
      name = "terminationWitness",
      description =
          "The template from which the different "
              + "versions of the termination witnesses will be exported. "
              + "Each version replaces the string '%s' "
              + "with its version number.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate terminationWitnessOutputFileTemplate =
      PathTemplate.ofFormatString("witness-%s.yml");

  // Since the default of the 'terminationWitness' option is not null, it is not possible to
  // deactivate it in the configs, since when it is 'null' the default value is used, which is not
  // null. Due to this reason, the 'exportTerminationWitness' option is
  // added to make it possible to deactivate the export.
  @Option(
      secure = true,
      name = "exportTerminationWitness",
      description = "export termination witness in YAML format")
  private boolean exportTerminationWitness = true;

  public TerminationYAMLWitnessExporter(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
    pConfig.inject(this, TerminationYAMLWitnessExporter.class);
  }

  /** Returns whether termination witnesses should be exported according to the configuration. */
  public boolean isExportEnabled() {
    return exportTerminationWitness && terminationWitnessOutputFileTemplate != null;
  }

  /** Returns the template of the files into which the termination witnesses are exported. */
  public @Nullable PathTemplate getOutputFileTemplate() {
    return terminationWitnessOutputFileTemplate;
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
   * <p>The witness is exported into the file given by the option
   * witness.yamlexporter.terminationWitness.
   *
   * @param pTerminationArguments in the form of transition invariants and supporting invariants.
   */
  public void export(ImmutableList<AbstractInvariantEntry> pTerminationArguments)
      throws IOException {

    for (YAMLWitnessVersion witnessVersion : ImmutableSet.copyOf(witnessVersions)) {
      Path outputFile = terminationWitnessOutputFileTemplate.getPath(witnessVersion.toString());
      switch (witnessVersion) {
        case V2 ->
            logger.log(
                Level.SEVERE, "Format in version 2.0 does not support termination witnesses.");
        case V2d1, V2d2 -> constructWitness(pTerminationArguments, outputFile);
      }
    }
  }
}
