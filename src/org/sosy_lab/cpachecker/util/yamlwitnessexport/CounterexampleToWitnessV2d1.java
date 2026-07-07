// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.terminationviamemory.TerminationToReachState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.SegmentRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ViolationSequenceEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord.WaypointAction;

public class CounterexampleToWitnessV2d1 extends CounterexampleToWitnessV2 {

  public CounterexampleToWitnessV2d1(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  /**
   * Export the given counterexample to the path as a Witness version 2.1
   *
   * @param pCex the counterexample to be exported
   * @param pPath the path to export the witness to
   * @throws IOException if writing the witness to the path is not possible
   */
  private void exportWitness(CounterexampleInfo pCex, Path pPath) throws IOException {
    exportEntries(
        new ViolationSequenceEntry(getMetadata(YAMLWitnessVersion.V2d1), buildSegments(pCex)),
        pPath);
  }

  @Override
  protected ImmutableList<SegmentRecord> buildSegments(CounterexampleInfo pCex) {
    ImmutableListMultimap<CFAEdge, String> edgeToAssumptions = mapEdgesToAssumptions(pCex);
    Map<CFAEdge, Integer> edgeToCurrentExpressionIndex = new HashMap<>();
    for (CFAEdge edge : edgeToAssumptions.keySet()) {
      edgeToCurrentExpressionIndex.put(edge, 0);
    }

    ImmutableList.Builder<SegmentRecord> segments = ImmutableList.builder();
    List<CFAEdge> edges = pCex.getTargetPath().getFullPath();
    AstCfaRelation astCFARelation = getASTStructure();

    TerminationToReachState terminationState =
        AbstractStates.extractStateByType(pCex.getTargetState(), TerminationToReachState.class);
    LocationState location =
        AbstractStates.extractStateByType(pCex.getTargetState(), LocationState.class);

    // The semantics of the YAML witnesses imply that every assumption waypoint should be
    // valid before the sequence statement it points to. Due to the semantics of the format:
    // "An assumption waypoint is evaluated at the sequence point immediately before the
    // waypoint location. The waypoint is passed if the given constraint evaluates to true."
    // To make our export compliant with the format we will point to exactly one sequence
    // point after the nondet call assignment
    // The syntax of the location of an assumption waypoint states that:
    // 'Assumption
    //  The location has to point to the beginning of a statement.'
    // Therefore, an assumption waypoint needs to point to the beginning of the statement before
    // which it is valid
    int numberOfTargetVisits = 0;
    for (CFAEdge edge : edges.subList(0, edges.size() - 1)) {

      List<WaypointRecord> waypoints =
          buildWaypoints(edge, edgeToAssumptions, astCFARelation, edgeToCurrentExpressionIndex);

      if (terminationState != null
          && numberOfTargetVisits >= terminationState.getNumberOfUnrollingsInTarget()) {
        waypoints =
            waypoints.stream().map(waypoint -> waypoint.withAction(WaypointAction.CYCLE)).toList();
      }
      if (edge.getSuccessor().equals(location.getLocationNode())) {
        numberOfTargetVisits += 1;
      }

      if (!waypoints.isEmpty()) {
        segments.add(new SegmentRecord(waypoints));
      }

      edgeToCurrentExpressionIndex.compute(
          edge, (key, value) -> (value == null) ? null : value + 1);
    }

    // Add target, if the property is not termination
    // In contrast to the semantics of assumptions, targets are evaluated at the next possible
    // segment point. Therefore, instead of creating a location record the way as is for
    // assumptions,
    // this needs to be done using another function
    if (terminationState == null) {
      CFAEdge lastEdge = edges.getLast();
      segments.add(SegmentRecord.ofOnlyElement(targetWaypoint(lastEdge, astCFARelation)));
    }
    return segments.build();
  }

  /**
   * Export the given counterexample to a witness file in Version 2.
   *
   * @param pCex The counterexample to export.
   * @param pOutputFileTemplate The template for the output file. The template will be used to *
   *     generate unique names for each witness version by replacing the string '%s' with the *
   *     version.
   * @throws IOException If the witness could not be written to the file.
   */
  @Override
  public void export(CounterexampleInfo pCex, PathTemplate pOutputFileTemplate, int uniqueId)
      throws IOException {
    exportWitness(pCex, pOutputFileTemplate.getPath(uniqueId, YAMLWitnessVersion.V2d1.toString()));
  }
}
