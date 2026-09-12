// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
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
import org.sosy_lab.cpachecker.util.witnesses.RelevantArgStatesCollector;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.ARGToYAMLWitness.CollectedInvariants;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;

public class ARGToYAMLWitnessExport extends AbstractYAMLWitnessExporter {

  /**
   * A class to keep track of the result of the witness export, in particular to inform the caller
   * about some internals of the translation and export.
   *
   * @param translationAlwaysSuccessful if the translation from internal ARG states to strings was
   *     always successful
   */
  public record WitnessExportResult(boolean translationAlwaysSuccessful) {}

  private final ARGToYAMLWitness argToWitness;

  /** The kinds of information which are exported into the witness of each requested version. */
  private final ImmutableMap<YAMLWitnessVersion, ImmutableSet<WitnessInvariantKind>> kindsPerVersion;

  public ARGToYAMLWitnessExport(
      Configuration pConfig,
      CFA pCfa,
      Specification pSpecification,
      LogManager pLogger,
      RelevantArgStatesCollector pArgStatesCollector)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
    argToWitness =
        new ARGToYAMLWitness(pConfig, pCfa, pSpecification, pLogger, pArgStatesCollector);

    ImmutableMap.Builder<YAMLWitnessVersion, ImmutableSet<WitnessInvariantKind>> kinds =
        ImmutableMap.builder();
    for (YAMLWitnessVersion witnessVersion : ImmutableSet.copyOf(witnessVersions)) {
      kinds.put(witnessVersion, kindsToExportFor(witnessVersion));
    }
    kindsPerVersion = kinds.buildOrThrow();
  }

  /** The kinds of information which should be exported into a witness of the given version. */
  private ImmutableSet<WitnessInvariantKind> kindsToExportFor(YAMLWitnessVersion pVersion) {
    // TODO: Make this configurable instead of deriving it from the witness version
    return switch (pVersion) {
      case V2 ->
          ImmutableSet.of(
              WitnessInvariantKind.LOOP_INVARIANT, WitnessInvariantKind.LOCATION_INVARIANT);
      case V2d1, V2d2 ->
          ImmutableSet.of(
              WitnessInvariantKind.LOOP_INVARIANT, WitnessInvariantKind.FUNCTION_CONTRACT);
    };
  }

  /** Export some information to the user about the guarantees provided by the witness. */
  private void analyzeExportedWitnessQuality(
      ImmutableMap<YAMLWitnessVersion, WitnessExportResult> pWitnessExportResults,
      UnmodifiableReachedSet pReachedSet) {
    // The common prefix is used to be able to be able to automatically process these messages in
    // CPAchecker's toolinfo module in BenchExec
    String commonPrefix = "Witness export warning: ";

    if (!FluentIterable.from(pWitnessExportResults.values())
        .allMatch(WitnessExportResult::translationAlwaysSuccessful)) {
      // For example occurring for: sv-benchmarks/c/nla-digbench-scaling/hard2_valuebound20.c
      logger.log(
          Level.INFO,
          commonPrefix
              + "Witnesses exported in versions "
              + from(pWitnessExportResults.entrySet())
                  .filter(entry -> !entry.getValue().translationAlwaysSuccessful())
                  .transform(entry -> entry.getKey().toString())
                  .join(Joiner.on(", "))
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
          commonPrefix
              + "The ARG contains no cycles. "
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

    // The entries are created only once, even when several versions are exported
    CollectedInvariants invariants =
        argToWitness.createInvariantEntries(
            pRootState, ImmutableSet.copyOf(Iterables.concat(kindsPerVersion.values())));

    ImmutableMap.Builder<YAMLWitnessVersion, WitnessExportResult> witnessExportResults =
        ImmutableMap.builder();
    for (YAMLWitnessVersion witnessVersion : kindsPerVersion.keySet()) {
      ImmutableSet<WitnessInvariantKind> kinds = kindsPerVersion.get(witnessVersion);
      exportEntries(
          new InvariantSetEntry(getMetadata(witnessVersion), invariants.entriesFor(kinds)),
          pOutputFileTemplate.getPath(witnessVersion.toString()));
      witnessExportResults.put(
          witnessVersion,
          new WitnessExportResult(invariants.translationAlwaysSuccessfulFor(kinds)));
    }

    if (analyseWitnessQuality) {
      analyzeExportedWitnessQuality(witnessExportResults.buildOrThrow(), pReachedSet);
    }
  }
}
