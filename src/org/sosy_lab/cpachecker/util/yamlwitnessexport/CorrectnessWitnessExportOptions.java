// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;

/**
 * Where to write the YAML correctness witness of a run, and whether to write one at all. Every
 * analysis that exports such a witness asks this class, so that one configuration decides for all
 * of them (cf. {@link org.sosy_lab.cpachecker.cpa.arg.counterexamples.CEXExportOptions} for the
 * violation witness).
 */
@Options(prefix = "cpa.arg")
public class CorrectnessWitnessExportOptions {

  @Option(
      secure = true,
      name = "yamlProofWitness",
      description =
          "The template from which the different "
              + "versions of the correctness witnesses will be exported. "
              + "Each version replaces the string '%s' "
              + "with its version number.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate yamlWitnessOutputFileTemplate =
      PathTemplate.ofFormatString("witness-%s.yml");

  // Since the default of the 'yamlProofWitness' option is not null, it is not possible to
  // deactivate it in the configs, since when it is 'null' the default value is used, which is not
  // null. Due to this reason, the 'exportYamlCorrectnessWitness' option is
  // added to make it possible to deactivate the export.
  @Option(
      secure = true,
      name = "exportYamlCorrectnessWitness",
      description = "export correctness witness in YAML format")
  private boolean exportYamlCorrectnessWitness = true;

  public CorrectnessWitnessExportOptions(Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  /** Whether a YAML correctness witness is written at all. */
  public boolean hasYamlWitnessExport() {
    return exportYamlCorrectnessWitness && yamlWitnessOutputFileTemplate != null;
  }

  /**
   * The template of the files into which the correctness witnesses are written, {@code null} if
   * none are written.
   */
  public @Nullable PathTemplate getYamlWitnessPathTemplate() {
    return exportYamlCorrectnessWitness ? yamlWitnessOutputFileTemplate : null;
  }
}
