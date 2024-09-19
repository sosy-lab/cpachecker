// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.ReportingMethodNotImplementedException;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.ARGToYAMLWitness.WitnessExportResult;

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

  /** Export some information to the user about the guarantees provided by the witness. */
  private void analyzeExportedWitnessQuality(
      ImmutableMap<YAMLWitnessVersion, WitnessExportResult> pWitnessExportResults,
      UnmodifiableReachedSet pReachedSet) {
    if (!FluentIterable.from(pWitnessExportResults.values())
        .allMatch(WitnessExportResult::translationAlwaysSuccessful)) {
      // For example occurring for: sv-benchmarks/c/nla-digbench-scaling/hard2_valuebound20.c
      logger.log(
          Level.INFO,
          "Witnesses exported in versions "
              + String.join(
                  ", ",
                  FluentIterable.from(pWitnessExportResults.entrySet())
                      .filter(entry -> !entry.getValue().translationAlwaysSuccessful())
                      .transform(entry -> entry.getKey().toString()))
              + " had problems during the translation process. "
              + "This may result in invariants being too large an over approximation.");
    }

    if (FluentIterable.from(pReachedSet)
        .filter(ARGState.class)
        // For some reason not all elements being covered are in the reached set, therefore this
        // workaround is needed
        // One example program where this happens is:
        // sv-benchmarks/c/nla-digbench-scaling/hard2_valuebound20.c
        .allMatch(argState -> argState.getCoveredByThis().isEmpty())) {
      // For example occurring for: sv-benchmarks/c/loops/n.c40.c
      logger.log(
          Level.INFO,
          "The ARG contains no cycles. "
              + "This means that the invariants are likely not inductive or not safe.");
    }
  }

  /**
   * Export the given ARG to a witness file in YAML format. All versions of witnesses will be
   * exported. It also prints output information to the user explaining what guarantees are provided
   * by the witness.
   *
   * @param pRootState The root state of the ARG.
   * @param pOutputFileTemplate The template for the output file. The template will be used to
   *     generate unique names for each witness version by replacing the string '%s' with the
   *     version.
   * @throws InterruptedException If the witness export was interrupted.
   * @throws IOException If the witness could not be written to the file.
   */
  public void export(
      ARGState pRootState, UnmodifiableReachedSet pReachedSet, PathTemplate pOutputFileTemplate)
      throws InterruptedException, IOException, ReportingMethodNotImplementedException {

    ImmutableMap.Builder<YAMLWitnessVersion, WitnessExportResult> witnessExportResults =
        ImmutableMap.builder();
    for (YAMLWitnessVersion witnessVersion : ImmutableSet.copyOf(witnessVersions)) {
      Path outputFile = pOutputFileTemplate.getPath(witnessVersion.toString());
      WitnessExportResult witnessExportResult =
          switch (witnessVersion) {
            case V2 -> argToWitnessV2.exportWitnesses(pRootState, outputFile);
            case V2d1 -> {
              logger.log(Level.INFO, "Exporting witnesses in Version 2.1 is currently WIP.");
              yield argToWitnessV2d1.exportWitness(pRootState, outputFile);
            }
          };
      witnessExportResults.put(witnessVersion, witnessExportResult);
    }

    if (analyseWitnessQuality) {
      analyzeExportedWitnessQuality(witnessExportResults.build(), pReachedSet);
    }
  }
}
