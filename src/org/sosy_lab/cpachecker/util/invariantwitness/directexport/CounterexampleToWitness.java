// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.directexport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.ast.ASTStructure;
import org.sosy_lab.cpachecker.util.ast.IfStructure;
import org.sosy_lab.cpachecker.util.invariantwitness.directexport.DataTypes.ExpressionType;
import org.sosy_lab.cpachecker.util.invariantwitness.directexport.DataTypes.WitnessVersion;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.InvariantWitnessWriter.YamlWitnessExportException;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.ViolationSequenceEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.InformationRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.LocationRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.SegmentRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.WaypointRecord;

public class CounterexampleToWitness extends DirectWitnessExporter {

  public CounterexampleToWitness(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  private WaypointRecord handleAssumptionWaypoint(
      CFAEdgeWithAssumptions edgeWithAssumptions, ASTStructure astStructure) throws IOException {
    CFAEdge edge = edgeWithAssumptions.getCFAEdge();

    String statement;
    if (edgeWithAssumptions.getExpStmts().isEmpty()) {
      // We need to export this waypoint in order to avoid errors caused by passing another
      // waypoint at the same location either too early or too late.
      statement = "1";
    } else {
      statement =
          String.join(
              " && ",
              edgeWithAssumptions.getExpStmts().stream()
                  .map(AExpressionStatement::toString)
                  .map(x -> "(" + x.replace(";", "") + ")")
                  .toList());
    }

    InformationRecord informationRecord =
        new InformationRecord(statement, null, ExpressionType.C.toString());
    LocationRecord location =
        Utils.createLocationRecordAfterLocation(
            edge.getFileLocation(),
            getlineOffsetsByFile(),
            edge.getPredecessor().getFunctionName(),
            astStructure);
    return new WaypointRecord(
        WaypointRecord.WaypointType.ASSUMPTION,
        WaypointRecord.WaypointAction.FOLLOW,
        informationRecord,
        location);
  }

  private WaypointRecord handleBranchingWaypoint(IfStructure ifStructure, AssumeEdge assumeEdge)
      throws IOException {
    String branchToFollow =
        ifStructure.getNodesBetweenConditionAndThenBranch().contains(assumeEdge.getSuccessor())
            ? "true"
            : "false";
    return new WaypointRecord(
        WaypointRecord.WaypointType.BRANCHING,
        WaypointRecord.WaypointAction.FOLLOW,
        new InformationRecord(branchToFollow, null, null),
        Utils.createLocationRecordAtStart(
            ifStructure.getCompleteElement().location(),
            getlineOffsetsByFile(),
            assumeEdge.getFileLocation().getFileName().toString(),
            assumeEdge.getPredecessor().getFunctionName()));
  }

  public void exportWitnessVersion2(CounterexampleInfo pCex)
      throws IOException, YamlWitnessExportException {
    ASTStructure astStructure = getASTStructure();

    CFAPathWithAssumptions cexPathWithAssignments = pCex.getCFAPathWithAssignments();
    List<SegmentRecord> segments = new ArrayList<>();
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

    Map<CFAEdge, Long> cfaEdgesOccurences =
        cexPathWithAssignments.stream()
            .map(CFAEdgeWithAssumptions::getCFAEdge)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    for (CFAEdgeWithAssumptions edgeWithAssumptions : cexPathWithAssignments) {
      CFAEdge edge = edgeWithAssumptions.getCFAEdge();
      // See if the edge contains an assignment of a VerifierNondet call
      List<WaypointRecord> waypoints = new ArrayList<>();

      if (CFAUtils.assignsNondetFunctionCall(edge)) {
        // Since waypoints are considered one after the other if an edge occurs more than once with
        // possibly different assumptions in the counterexample path, if not all are exported then
        // there may be a wrong matching
        if (edgeWithAssumptions.getExpStmts().isEmpty() && cfaEdgesOccurences.get(edge) == 1) {
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

        waypoints.add(handleAssumptionWaypoint(edgeWithAssumptions, astStructure));
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
    }

    // Add target
    // In contrast to the semantics of assumptions, targets are evaluated at the next possible
    // segment point. Therefore instead of creating a location record the way as is for assumptions,
    // this needs to be done using another function
    CFAEdge lastEdge = cexPathWithAssignments.get(cexPathWithAssignments.size() - 1).getCFAEdge();
    segments.add(
        SegmentRecord.ofOnlyElement(
            new WaypointRecord(
                WaypointRecord.WaypointType.TARGET,
                WaypointRecord.WaypointAction.FOLLOW,
                null,
                Utils.createLocationRecordAtStart(
                    lastEdge.getFileLocation(),
                    getlineOffsetsByFile(),
                    lastEdge.getPredecessor().getFunctionName()))));

    exportEntries(
        new ViolationSequenceEntry(getMetadata(WitnessVersion.V2), segments),
        getOutputFile(WitnessVersion.V2));
  }

  public void export(CounterexampleInfo pCex) throws YamlWitnessExportException, IOException {
    for (WitnessVersion witnessVersion : witnessVersions) {
      switch (witnessVersion) {
        case V2:
          exportWitnessVersion2(pCex);
          break;
        case V3:
          break;
        default:
          throw new YamlWitnessExportException("Unknown witness version: " + witnessVersion);
      }
    }
  }
}
