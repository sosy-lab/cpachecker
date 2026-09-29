// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.terminationviamemory.TerminationToReachState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.SegmentRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ViolationSequenceEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord.WaypointAction;

/**
 * Exports counterexamples to termination as non-termination witnesses. In contrast to {@link
 * CounterexampleToWitness}, the waypoints of the loop of the lasso are exported with the cycle
 * action and there is no target waypoint.
 */
@Options(prefix = "witness.yamlexporter")
public class NonterminationCounterexampleToWitness extends CounterexampleToWitness {

  @Option(
      secure = true,
      name = "nonterminationWitness",
      description =
          "The template from which the different "
              + "versions of the non-termination witnesses will be exported. "
              + "Each version replaces the string '%s' "
              + "with its version number.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate nonterminationWitnessOutputFileTemplate =
      PathTemplate.ofFormatString("witness-%s.yml");

  // Since the default of the 'nonterminationWitness' option is not null, it is not possible to
  // deactivate it in the configs, since when it is 'null' the default value is used, which is not
  // null. Due to this reason, the 'exportNonterminationWitness' option is
  // added to make it possible to deactivate the export.
  @Option(
      secure = true,
      name = "exportNonterminationWitness",
      description = "export non-termination witness in YAML format")
  private boolean exportNonterminationWitness = true;

  public NonterminationCounterexampleToWitness(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
    pConfig.inject(this, NonterminationCounterexampleToWitness.class);
  }

  /**
   * Returns whether non-termination witnesses should be exported according to the configuration.
   */
  public boolean isExportEnabled() {
    return exportNonterminationWitness && nonterminationWitnessOutputFileTemplate != null;
  }

  /** Returns the template of the files into which the non-termination witnesses are exported. */
  public @Nullable PathTemplate getOutputFileTemplate() {
    return nonterminationWitnessOutputFileTemplate;
  }

  /**
   * Export the given counterexample to a non-termination witness file, see {@link
   * #export(CounterexampleInfo, PathTemplate, int)}.
   *
   * @param pNumberOfUnrollings the number of times the loop head (location of the target state) is
   *     left in the counterexample before the cycle of the non-termination witness starts
   */
  public void exportNonTerminationWitness(
      CounterexampleInfo pCex,
      PathTemplate pOutputFileTemplate,
      int uniqueId,
      int pNumberOfUnrollings)
      throws IOException {
    for (YAMLWitnessVersion witnessVersion : witnessVersions) {
      Path outputFile = pOutputFileTemplate.getPath(uniqueId, witnessVersion.toString());
      exportWitness(pCex, outputFile, witnessVersion, OptionalInt.of(pNumberOfUnrollings));
    }
  }

  /**
   * Export the given counterexample to the path as a non-termination witness.
   *
   * @param pCex the counterexample to be exported
   * @param pPath the path to export the witness to
   * @param pNumberOfUnrollings the number of times the loop head is left before the cycle starts.
   *     If empty, it is taken from the {@link TerminationToReachState} of the target state.
   * @throws IOException if writing the witness to the path is not possible
   */
  private void exportWitness(
      CounterexampleInfo pCex,
      Path pPath,
      YAMLWitnessVersion pWitnessVersion,
      OptionalInt pNumberOfUnrollings)
      throws IOException {
    if (!isNonTerminationWitness(pWitnessVersion)) {
      super.exportWitness(pCex, pPath, pWitnessVersion);
      return;
    }

    AstCfaRelation astCFARelation = getASTStructure();

    Map<CFAEdge, Integer> edgeToCurrentExpressionIndex = new HashMap<>();
    ImmutableListMultimap<CFAEdge, String> edgeToAssumptions =
        computeEdgeToAssumptions(pCex, edgeToCurrentExpressionIndex);

    ImmutableList.Builder<SegmentRecord> segments = ImmutableList.builder();
    ImmutableList<EdgeWithStates> edges = getEdgesWithStates(pCex.getTargetPath());

    // This builder keeps track of the mapping between thread IDs and the order in which they were
    // created such that we can refer to them in the witness. Main always has the thread ID 0.
    ImmutableMap.Builder<String, Integer> threadNameToIdBuilder = new ImmutableMap.Builder<>();
    threadNameToIdBuilder.put("main", 0);

    // Initialization for counters of cycle head visits in case the property is termination.
    int cycleHeadVisits = 0;
    boolean isInCycle = false;
    int numberOfUnrollings =
        pNumberOfUnrollings.isPresent()
            ? pNumberOfUnrollings.orElseThrow()
            : Verify.verifyNotNull(
                    AbstractStates.extractStateByType(
                        pCex.getTargetState(), TerminationToReachState.class),
                    "Number of unrollings for the non-termination witness is unknown")
                .getNumberOfTargetStateVisitsBeforeInfiniteLoop();
    CFANode cycleHead = AbstractStates.extractLocation(pCex.getTargetState());

    for (EdgeWithStates edgeWithStates : edges) {
      List<WaypointRecord> waypoints =
          buildWaypoints(
              edgeWithStates.edge(),
              edgeToAssumptions,
              astCFARelation,
              edgeToCurrentExpressionIndex,
              threadNameToIdBuilder,
              edgeWithStates.nextState(),
              edgeWithStates.previousState(),
              pWitnessVersion);

      // The cycle starts when the cycle head is left after it was already left
      // numberOfUnrollings times
      if (!isInCycle && edgeWithStates.edge().getPredecessor().equals(cycleHead)) {
        if (cycleHeadVisits < numberOfUnrollings) {
          cycleHeadVisits++;
        } else {
          isInCycle = true;
        }
      }
      if (isInCycle && !waypoints.isEmpty()) {
        WaypointRecord followWaypoint = getFollowWaypoint(waypoints);

        // Remove the original follow waypoint
        waypoints =
            waypoints.stream()
                .filter(waypoint -> !waypoint.equals(followWaypoint))
                .collect(ImmutableList.toImmutableList());
        // Add the follow waypoint but now with cycle action
        waypoints =
            ImmutableList.copyOf(
                Iterables.concat(waypoints, ImmutableList.of(followWaypoint.withCycleAction())));
      }

      if (!waypoints.isEmpty()) {
        segments.add(new SegmentRecord(waypoints));
      }

      edgeToCurrentExpressionIndex.compute(
          edgeWithStates.edge(), (key, value) -> (value == null) ? null : value + 1);
    }

    // Non-termination witnesses do not have a target waypoint, the cycle describes the violation
    exportEntries(
        new ViolationSequenceEntry(getMetadata(pWitnessVersion), segments.build()), pPath);
  }

  /** Finds the follow waypoint in the list of waypoints. */
  private static WaypointRecord getFollowWaypoint(List<WaypointRecord> waypoints) {
    return Iterables.find(
        waypoints, waypoint -> waypoint.getAction().equals(WaypointAction.FOLLOW));
  }

  private boolean isNonTerminationWitness(YAMLWitnessVersion pWitnessVersion) {
    return (pWitnessVersion.equals(YAMLWitnessVersion.V2d1)
            || pWitnessVersion.equals(YAMLWitnessVersion.V2d2))
        && getSpecification().getProperties().stream()
            .anyMatch(pProperty -> pProperty.equals(CommonVerificationProperty.TERMINATION));
  }
}
