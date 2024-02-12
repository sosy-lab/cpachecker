// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.ast.ASTStructure;
import org.sosy_lab.cpachecker.util.ast.IfStructure;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InformationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.SegmentRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ViolationSequenceEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord;

public class CounterexampleToWitness extends AbstractYAMLWitnessExporter {

  public CounterexampleToWitness(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  private WaypointRecord handleAssumptionWaypoint(
      Collection<AExpressionStatement> assumptions, CFAEdge edge, ASTStructure astStructure) {
    String statement;
    if (assumptions.isEmpty()) {
      // We need to export this waypoint in order to avoid errors caused by passing another
      // waypoint at the same location either too early or too late.
      statement = "1";
    } else {
      statement =
          String.join(
              " && ",
              assumptions.stream()
                  .map(AExpressionStatement::toString)
                  .map(x -> "(" + x.replace(";", "") + ")")
                  .toList());
    }

    InformationRecord informationRecord =
        new InformationRecord(statement, null, YAMLWitnessExpressionType.C.toString());
    LocationRecord location =
        YAMLWitnessesExportUtils.createLocationRecordAfterLocation(
            edge.getFileLocation(), edge.getPredecessor().getFunctionName(), astStructure);
    return new WaypointRecord(
        WaypointRecord.WaypointType.ASSUMPTION,
        WaypointRecord.WaypointAction.FOLLOW,
        informationRecord,
        location);
  }

  private WaypointRecord handleBranchingWaypoint(IfStructure ifStructure, AssumeEdge assumeEdge) {
    String branchToFollow =
        Boolean.toString(
            ifStructure
                .getNodesBetweenConditionAndThenBranch()
                .contains(assumeEdge.getSuccessor()));
    return new WaypointRecord(
        WaypointRecord.WaypointType.BRANCHING,
        WaypointRecord.WaypointAction.FOLLOW,
        new InformationRecord(branchToFollow, null, null),
        YAMLWitnessesExportUtils.createLocationRecordAtStart(
            ifStructure.getCompleteElement().location(),
            assumeEdge.getFileLocation().getFileName().toString(),
            assumeEdge.getPredecessor().getFunctionName()));
  }

  private void exportWitnessVersion2(CounterexampleInfo pCex, Path pPath)
      throws IOException, YamlWitnessExportException {
    ASTStructure astStructure = getASTStructure();

    ListMultimap<CFAEdge, AExpressionStatement> edgeToAssumptions = ArrayListMultimap.create();
    Map<CFAEdge, Integer> edgeToCurrentExpressionIndex = new HashMap<>();
    if (pCex.isPreciseCounterExample()) {
      for (CFAEdgeWithAssumptions edgeWithAssumptions : pCex.getCFAPathWithAssignments()) {
        CFAEdge edge = edgeWithAssumptions.getCFAEdge();
        edgeToAssumptions.putAll(edge, edgeWithAssumptions.getExpStmts());
        edgeToCurrentExpressionIndex.put(edge, 0);
      }
    }

    ImmutableList.Builder<SegmentRecord> segments = ImmutableList.builder();
    // For some readon the edges can contain null elements
    List<CFAEdge> edges =
        pCex.getTargetPath().getInnerEdges().stream().filter(edge -> edge != null).toList();

    // The semantics of the YAML witnesses imply that every assumption waypoint should be
    // valid before the sequence statement it points to. Due to the semantics of the format:
    // "An assumption waypoint is evaluated at the sequence point immediately before the
    // waypoint location. The waypoint is passed if the given constraint evaluates to true."
    // To make our export compliant with the format we will point to exactly one sequence
    // point after the nondet call assignment
    // The syntax of the location of an assumption waypoint states that:
    // 'Assumption
    //  The location has to point to the beginning of a statement.'
    // Therefore an assumption waypoint needs to point to the beginning of the statement before
    // which it is valid
    ImmutableMultimap<CFAEdge, CFAEdge> cfaEdgesOccurences =
        Multimaps.index(edges.iterator(), key -> key);

    for (CFAEdge edge : edges) {
      // See if the edge contains an assignment of a VerifierNondet call
      List<WaypointRecord> waypoints = new ArrayList<>();

      if (CFAUtils.assignsNondetFunctionCall(edge)) {
        // Since waypoints are considered one after the other if an edge occurs more than once with
        // possibly different assumptions in the counterexample path, if not all are exported then
        // there may be a wrong matching
        if (cfaEdgesOccurences.get(edge).size() == 1) {
          continue;
        }

        // Do not consider elements which have no assumptions
        if (!(edgeToAssumptions.containsKey(edge)
            && edgeToCurrentExpressionIndex.containsKey(edge))) {
          continue;
        }

        // Currently it is unclear what to do with assumptions where the next statement is after a
        // function return. Since the variables for the assumptions may not be in scope.
        if (!CFAUtils.leavingEdges(edge.getSuccessor())
            .transform(CFAEdge::getSuccessor)
            .filter(FunctionExitNode.class)
            .isEmpty()) {
          continue;
        }

        // Blank edges are usually a sign that we are returning to a loop head. Since the AST
        // location following the end of the loop is simply the next statement, we need to export
        // this assumption at the next possible edge location where the variable is in scope.
        // Since currently there is no straightforward way to do this, we simply do not export these
        // waypoints currently
        // TODO: Add a method to export these assumptions
        if (!CFAUtils.leavingEdges(edge.getSuccessor()).filter(BlankEdge.class).isEmpty()) {
          continue;
        }

        waypoints.add(
            handleAssumptionWaypoint(
                ImmutableList.of(
                    edgeToAssumptions.get(edge).get(edgeToCurrentExpressionIndex.get(edge))),
                edge,
                astStructure));
      } else if (edge instanceof AssumeEdge assumeEdge) {
        // Without the AST structure we cannot guarantee that we are exporting at the beginning of
        // an iteration or if statement
        // To export the branching waypoint, we first find the IfStructure or IterationStructure
        // containing it. Then we look for the FileLocation of the structure
        // Currently we only export IfStructures, since there is no nice way to say how often a loop
        // should be traversed and exporting this information will quickly make the witness
        // difficult to read
        // TODO: Also export branches at iteration statements
        IfStructure ifStructure = astStructure.getIfStructureForConditionEdge(edge);
        if (ifStructure == null) {
          continue;
        }

        Set<CFANode> nodesBetweenConditionAndThenBranch =
            ifStructure.getNodesBetweenConditionAndThenBranch();
        Set<CFANode> nodesBetweenConditionAndElseBranch =
            ifStructure.getNodesBetweenConditionAndElseBranch();
        CFANode successor = edge.getSuccessor();

        if (!nodesBetweenConditionAndThenBranch.contains(successor)
            && !nodesBetweenConditionAndElseBranch.contains(successor)) {
          continue;
        }

        waypoints.add(handleBranchingWaypoint(ifStructure, assumeEdge));
      }

      if (!waypoints.isEmpty()) {
        segments.add(new SegmentRecord(waypoints));
      }

      edgeToCurrentExpressionIndex.compute(
          edge, (key, value) -> (value == null) ? null : value + 1);
    }

    // Add target
    // In contrast to the semantics of assumptions, targets are evaluated at the next possible
    // segment point. Therefore instead of creating a location record the way as is for assumptions,
    // this needs to be done using another function
    CFAEdge lastEdge = edges.get(edges.size() - 1);
    segments.add(
        SegmentRecord.ofOnlyElement(
            new WaypointRecord(
                WaypointRecord.WaypointType.TARGET,
                WaypointRecord.WaypointAction.FOLLOW,
                null,
                YAMLWitnessesExportUtils.createLocationRecordAtStart(
                    lastEdge.getFileLocation(), lastEdge.getPredecessor().getFunctionName()))));

    exportEntries(
        new ViolationSequenceEntry(getMetadata(YAMLWitnessVersion.V2), segments.build()), pPath);
  }

  public void export(CounterexampleInfo pCex, PathTemplate pPathTemplate)
      throws YamlWitnessExportException, IOException {
    for (YAMLWitnessVersion witnessVersion : witnessVersions) {
      Path outputPath = getOutputFile(YAMLWitnessVersion.V2, pPathTemplate);
      switch (witnessVersion) {
        case V2:
          exportWitnessVersion2(pCex, outputPath);
          break;
        case V3:
          break;
        default:
          throw new YamlWitnessExportException("Unknown witness version: " + witnessVersion);
      }
    }
  }
}
