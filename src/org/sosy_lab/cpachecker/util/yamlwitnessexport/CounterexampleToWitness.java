// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.base.Joiner;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.ast.IfElement;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
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

  /**
   * Create an Assumption Waypoint at the position of the current edge with the given assumptions
   *
   * @param assumptions the assumptions at this position
   * @param edge the edge which is the location at which the assumptions are valid
   * @param pAstCfaRelation the mapping between the
   * @return a waypoint constraining the execution to the given assumptions at the location of the
   *     edge
   */
  private static WaypointRecord handleAssumptionWaypoint(
      Collection<AExpressionStatement> assumptions, CFAEdge edge, AstCfaRelation pAstCfaRelation) {
    String statement;
    if (assumptions.isEmpty()) {
      // We need to export this waypoint in order to avoid errors caused by passing another
      // waypoint at the same location either too early or too late.
      statement = "1";
    } else {
      statement =
          FluentIterable.from(assumptions)
              .transform(stmt -> stmt.getExpression().toParenthesizedASTString())
              .filter(s -> !s.contains("__CPAchecker_TMP"))
              .join(Joiner.on(" && "));
    }

    InformationRecord informationRecord =
        new InformationRecord(statement, null, YAMLWitnessExpressionType.C);
    LocationRecord location =
        LocationRecord.createLocationRecordAfterLocation(
            edge.getFileLocation(), edge.getPredecessor().getFunctionName(), pAstCfaRelation);
    return new WaypointRecord(
        WaypointRecord.WaypointType.ASSUMPTION,
        WaypointRecord.WaypointAction.FOLLOW,
        informationRecord,
        location);
  }

  /**
   * Waypoints cannot be exported at every possible location. This method checks if the current edge
   * is a possible location for an assumption waypoint and if so, exports it.
   *
   * @param pEdge the edge for which to export the assumption waypoint
   * @param pEdgeToAssumptions the assumptions at each edge
   * @param edgeToCurrentExpressionIndex the index of the current assumption at each edge
   * @param pAstCfaRelation the mapping between the AST and the CFA
   * @return an assumption waypoint if the edge is a possible location for an assumption waypoint,
   *     otherwise an empty optional
   */
  private Optional<WaypointRecord> handleAssumptionWhenAtPossibleLocation(
      CFAEdge pEdge,
      ImmutableListMultimap<CFAEdge, AExpressionStatement> pEdgeToAssumptions,
      Map<CFAEdge, Integer> edgeToCurrentExpressionIndex,
      AstCfaRelation pAstCfaRelation) {

    // Do not consider edges which are added internally by CPAchecker, since this may duplicate
    // assumptions
    if (pEdge.toString().contains("__CPAchecker_TMP")) {
      return Optional.empty();
    }

    // Do not consider elements which have no assumptions
    if (!(pEdgeToAssumptions.containsKey(pEdge)
        && edgeToCurrentExpressionIndex.containsKey(pEdge))) {
      return Optional.empty();
    }

    // Currently, it is unclear what to do with assumptions where the next statement is after a
    // function return. Since the variables for the assumptions may not be in scope.
    // TODO: Add a method to export these assumptions
    if (!CFAUtils.successorsOf(pEdge.getSuccessor()).filter(FunctionExitNode.class).isEmpty()) {
      return Optional.empty();
    }

    // Currently, it is unclear what to do with assumptions where the next statement is after a
    // function call, since if the variable is a global variable, then it will be in scope, but if
    // it is a local variable, then it will not be in scope. There are methods to check this, see
    // for example outOfScopeVariables in a CFANode.
    // TODO: Add a method to export these assumptions
    if (!CFAUtils.leavingEdges(pEdge.getSuccessor())
        .filter(CDeclarationEdge.class)
        .transform(CDeclarationEdge::getDeclaration)
        .filter(CFunctionDeclaration.class)
        .isEmpty()) {
      return Optional.empty();
    }

    // Blank edges are usually a sign that we are returning to a loop head, calling a function or
    // returning from a function. Since the AST
    // location following the end of the loop is simply the next statement, we need to export
    // this assumption at the next possible edge location where the variable is in scope.
    // Since currently there is no straightforward way to do this, we simply do not export these
    // waypoints currently
    // TODO: Add a method to export these assumptions
    if (!CFAUtils.leavingEdges(pEdge.getSuccessor()).filter(BlankEdge.class).isEmpty()
        || pEdge instanceof BlankEdge) {
      return Optional.empty();
    }

    return Optional.of(
        handleAssumptionWaypoint(
            ImmutableList.of(
                pEdgeToAssumptions.get(pEdge).get(edgeToCurrentExpressionIndex.get(pEdge))),
            pEdge,
            pAstCfaRelation));
  }

  /**
   * Creates a waypoint record which describes which branch should be taken at an if statement
   *
   * @param conditionTruthValue the value the condition should evaluate to. In case of an if
   *     statement, if true the then branch should be taken, if false the else branch should be
   *     taken
   * @param pAstElementLocation the AST element where the branch taken should be constrained
   * @param assumeEdge the edge which encodes what branch should be taken
   * @return a waypoint record constraining the execution to a single branch of this if statement
   */
  private static WaypointRecord handleBranchingWaypoint(
      boolean conditionTruthValue, FileLocation pAstElementLocation, AssumeEdge assumeEdge) {

    return new WaypointRecord(
        WaypointRecord.WaypointType.BRANCHING,
        WaypointRecord.WaypointAction.FOLLOW,
        new InformationRecord(Boolean.toString(conditionTruthValue), null, null),
        LocationRecord.createLocationRecordAtStart(
            pAstElementLocation,
            assumeEdge.getFileLocation().getFileName().toString(),
            assumeEdge.getPredecessor().getFunctionName()));
  }

  private List<WaypointRecord> buildWaypoints(
      CFAEdge pEdge,
      ImmutableListMultimap<CFAEdge, AExpressionStatement> pEdgeToAssumptions,
      AstCfaRelation pAstCFARelation,
      Map<CFAEdge, Integer> pEdgeToCurrentExpressionIndex) {

    // See if the edge contains an assignment of a VerifierNondet call
    if (CFAUtils.assignsNondetFunctionCall(pEdge)) {

      Optional<WaypointRecord> assumptionWaypoint =
          handleAssumptionWhenAtPossibleLocation(
              pEdge, pEdgeToAssumptions, pEdgeToCurrentExpressionIndex, pAstCFARelation);

      if (assumptionWaypoint.isEmpty()) {
        return ImmutableList.of();
      }

      return ImmutableList.of(assumptionWaypoint.orElseThrow());
    } else if (pEdge instanceof AssumeEdge assumeEdge) {
      // Without the AST structure we cannot guarantee that we are exporting at the beginning of
      // an iteration or if statement
      // To export the branching waypoint, we first find the IfElement or IterationElement
      // containing it. Then we look for the FileLocation of the structure
      // Currently we only export IfStructures, since there is no nice way to say how often a loop
      // should be traversed and exporting this information will quickly make the witness
      // difficult to read
      Optional<IfElement> optionalIfElement =
          pAstCFARelation.getIfStructureForConditionEdge(assumeEdge);
      Optional<IterationElement> optionalIterationElement =
          pAstCFARelation.getTightestIterationStructureForNode(assumeEdge.getPredecessor());

      Set<CFANode> nodesBetweenConditionAndFirstBranch;
      Set<CFANode> nodesBetweenConditionAndSecondBranch;
      CFANode successor = assumeEdge.getSuccessor();
      FileLocation astElementLocation;
      if (optionalIfElement.isPresent()) {
        IfElement ifElement = optionalIfElement.orElseThrow();
        nodesBetweenConditionAndFirstBranch = ifElement.getNodesBetweenConditionAndThenBranch();
        nodesBetweenConditionAndSecondBranch = ifElement.getNodesBetweenConditionAndElseBranch();
        astElementLocation = ifElement.getCompleteElement().location();
      } else if (optionalIterationElement.isPresent()) {
        IterationElement iterationElement = optionalIterationElement.orElseThrow();
        astElementLocation = iterationElement.getCompleteElement().location();

        if (iterationElement.getControllingExpression().isEmpty()) {
          // This can only happen for an expression of the form `for(;;)`, which is an infinite
          // loop. In this case we directly export the assumption waypoint and continue.
          return ImmutableList.of(handleBranchingWaypoint(true, astElementLocation, assumeEdge));
        }

        nodesBetweenConditionAndFirstBranch = iterationElement.getNodesBetweenConditionAndBody();
        nodesBetweenConditionAndSecondBranch = iterationElement.getNodesBetweenConditionAndExit();
      } else {
        // TODO: Handle conditional expressions. This would need to be added at the parser level
        // and then added to the AstCfaRelation. The problem is that this occurs at the expression
        // level and we currently only consider statements. The relevant parser expression type is
        // IASTConditionalExpression.
        logger.log(Level.INFO, "Could not find the AST structure for the edge: " + pEdge);
        return ImmutableList.of();
      }

      if (!nodesBetweenConditionAndFirstBranch.contains(successor)
          && !nodesBetweenConditionAndSecondBranch.contains(successor)) {
        return ImmutableList.of();
      }

      Verify.verifyNotNull(astElementLocation);
      return ImmutableList.of(
          handleBranchingWaypoint(
              nodesBetweenConditionAndFirstBranch.contains(successor),
              astElementLocation,
              assumeEdge));

    } else if (exportCompleteCounterexample) {
      // Export all other edges which are not absolutely relevant for the counterexample
      Optional<WaypointRecord> assumptionWaypoint =
          handleAssumptionWhenAtPossibleLocation(
              pEdge, pEdgeToAssumptions, pEdgeToCurrentExpressionIndex, pAstCFARelation);

      if (assumptionWaypoint.isEmpty()) {
        return ImmutableList.of();
      }

      return ImmutableList.of(assumptionWaypoint.orElseThrow());
    }

    // Not all edges are relevant for the counterexample, so we do not export them
    return ImmutableList.of();
  }

  /**
   * Export the given counterexample to the path as a Witness version 2.0
   *
   * @param pCex the counterexample to be exported
   * @param pPath the path to export the witness to
   * @throws IOException if writing the witness to the path is not possible
   */
  private void exportWitnessVersion2(CounterexampleInfo pCex, Path pPath) throws IOException {
    AstCfaRelation astCFARelation = getASTStructure();

    ImmutableListMultimap.Builder<CFAEdge, AExpressionStatement> edgeToAssumptionsBuilder =
        new ImmutableListMultimap.Builder<>();
    Map<CFAEdge, Integer> edgeToCurrentExpressionIndex = new HashMap<>();
    if (pCex.isPreciseCounterExample()) {
      for (CFAEdgeWithAssumptions edgeWithAssumptions : pCex.getCFAPathWithAssignments()) {
        CFAEdge edge = edgeWithAssumptions.getCFAEdge();
        edgeToAssumptionsBuilder.putAll(edge, edgeWithAssumptions.getExpStmts());
        edgeToCurrentExpressionIndex.put(edge, 0);
      }
    }

    ImmutableListMultimap<CFAEdge, AExpressionStatement> edgeToAssumptions =
        edgeToAssumptionsBuilder.build();

    ImmutableList.Builder<SegmentRecord> segments = ImmutableList.builder();
    List<CFAEdge> edges = pCex.getTargetPath().getFullPath();

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
    for (CFAEdge edge : edges) {

      List<WaypointRecord> waypoints =
          buildWaypoints(edge, edgeToAssumptions, astCFARelation, edgeToCurrentExpressionIndex);

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
                LocationRecord.createLocationRecordAtStart(
                    lastEdge.getFileLocation(), lastEdge.getPredecessor().getFunctionName()))));

    exportEntries(
        new ViolationSequenceEntry(getMetadata(YAMLWitnessVersion.V2), segments.build()), pPath);
  }

  /**
   * Export the given counterexample to a witness file. The format of the witness file is determined
   * by the witness versions given in the configuration. All versions of witnesses will be exported.
   * Currently only Version 2 exists for Violation Witnesses.
   *
   * @param pCex The counterexample to export.
   * @param pOutputFileTemplate The template for the output file. The template will be used to *
   *     generate unique names for each witness version by replacing the string '%s' with the *
   *     version.
   * @throws IOException If the witness could not be written to the file.
   */
  public void export(CounterexampleInfo pCex, PathTemplate pOutputFileTemplate, int uniqueId)
      throws IOException {
    for (YAMLWitnessVersion witnessVersion : witnessVersions) {
      Path outputFile = pOutputFileTemplate.getPath(uniqueId, YAMLWitnessVersion.V2.toString());
      switch (witnessVersion) {
        case V2 -> exportWitnessVersion2(pCex, outputFile);
        case V2d1 ->
            logger.log(Level.INFO, "There is currently no version 2.1 for Violation Witnesses.");
        default -> throw new AssertionError("Unknown witness version: " + witnessVersion);
      }
    }
  }
}
