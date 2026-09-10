// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.ReportingMethodNotImplementedException;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.TranslationToExpressionTreeFailedException;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.SegmentRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ViolationSequenceEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord.WaypointAction;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord.WaypointType;

/**
 * Exports witnesses in version 2.0 for an analysis that executes the program instead of abstracting
 * it (cf. {@link org.sosy_lab.cpachecker.cpa.execution.ExecutionCPA}).
 *
 * <p>Such an analysis does not need an ARG, so the witnesses cannot be built from one:
 *
 * <ul>
 *   <li>The invariants of a correctness witness are the disjunction of all assignments that the
 *       execution actually had at the location, which the analysis collects while it runs.
 *   <li>A violation witness consists of nothing but the target waypoint, because the execution is
 *       fully determined by the program and a validator does not need any guidance to replay it.
 * </ul>
 */
public class ExecutionToYAMLWitness extends AbstractYAMLWitnessExporter {

  public ExecutionToYAMLWitness(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  /**
   * Export a correctness witness whose invariants describe all assignments that occurred at the
   * given locations.
   *
   * @param pLoopInvariants the states observed at loop heads
   * @param pLocationInvariants the states observed in front of function calls
   * @param pOutFile the file to write the witness to
   */
  public void exportCorrectnessWitness(
      Multimap<CFANode, ExpressionTreeReportingState> pLoopInvariants,
      Multimap<CFANode, ExpressionTreeReportingState> pLocationInvariants,
      Path pOutFile)
      throws InterruptedException, IOException {

    ImmutableList.Builder<AbstractInvariantEntry> entries = ImmutableList.builder();
    for (CFANode node : pLoopInvariants.keySet()) {
      InvariantEntry entry =
          createInvariant(
              pLoopInvariants.get(node),
              node,
              loopLocation(node),
              InvariantRecordType.LOOP_INVARIANT.getKeyword());
      if (entry != null) {
        entries.add(entry);
      }
    }
    for (CFANode node : pLocationInvariants.keySet()) {
      InvariantEntry entry =
          createInvariant(
              pLocationInvariants.get(node),
              node,
              functionCallLocation(node),
              InvariantRecordType.LOCATION_INVARIANT.getKeyword());
      if (entry != null) {
        entries.add(entry);
      }
    }

    exportEntries(
        new InvariantSetEntry(getMetadata(YAMLWitnessVersion.V2), entries.build()), pOutFile);
  }

  /**
   * Export a violation witness that only points at the location whose execution violates the
   * specification.
   *
   * @param pViolatingEdge the edge whose execution violates the specification
   * @param pOutFile the file to write the witness to
   */
  public void exportViolationWitness(CFAEdge pViolatingEdge, Path pOutFile) throws IOException {
    exportEntries(
        new ViolationSequenceEntry(
            getMetadata(YAMLWitnessVersion.V2),
            ImmutableList.of(SegmentRecord.ofOnlyElement(targetWaypoint(pViolatingEdge)))),
        pOutFile);
  }

  /**
   * The location of the loop that starts at the given node, or {@code null} if the node is not the
   * head of a loop of the input program (which happens for loops that CPAchecker creates itself,
   * e.g. for {@code goto} statements).
   */
  private @Nullable LocationRecord loopLocation(CFANode pNode) {
    Optional<IterationElement> iterationStructure =
        getASTStructure().getTightestIterationStructureForNode(pNode);
    if (iterationStructure.isEmpty()) {
      return null;
    }
    FileLocation fileLocation = iterationStructure.orElseThrow().getCompleteElement().location();
    return LocationRecord.createLocationRecordAtStart(
        fileLocation,
        pNode.getFunction().getFileLocation().getFileName().toString(),
        pNode.getFunctionName());
  }

  /** The location of the function call that leaves the given node. */
  private LocationRecord functionCallLocation(CFANode pNode) {
    return LocationRecord.createLocationRecordAtStart(
        pNode.getLeavingEdge(0).getFileLocation(), pNode.getFunctionName());
  }

  /**
   * Build an invariant that holds for every given state, i.e. the disjunction of the assignments of
   * the states. States of different analyses are conjuncted, because each of them describes a
   * different part of the program state.
   *
   * @return the invariant, or {@code null} if there is no location or no information to report
   */
  private @Nullable InvariantEntry createInvariant(
      Collection<ExpressionTreeReportingState> pStates,
      CFANode pNode,
      @Nullable LocationRecord pLocation,
      String pType)
      throws InterruptedException {

    if (pLocation == null) {
      return null;
    }
    FunctionEntryNode entryNode = cfa.getFunctionHead(pNode.getFunctionName());
    List<ExpressionTree<Object>> perAnalysis = new ArrayList<>();
    for (Class<?> stateClass :
        FluentIterable.from(pStates).transform(AbstractState::getClass).toSet()) {
      List<ExpressionTree<Object>> assignments = new ArrayList<>();
      for (ExpressionTreeReportingState state : pStates) {
        if (!stateClass.isAssignableFrom(state.getClass())) {
          continue;
        }
        try {
          assignments.add(
              state.getFormulaApproximationInputProgramInScopeVariables(
                  entryNode,
                  pNode,
                  cfa.getAstCfaRelation(),
                  /* useOldKeywordForVariables= */ false));
        } catch (ReportingMethodNotImplementedException
            | TranslationToExpressionTreeFailedException e) {
          logger.logDebugException(e, "Could not translate state to an expression tree");
          // We do not know what this state describes, so we must not claim anything about it.
          assignments.add(ExpressionTrees.getTrue());
        }
      }
      perAnalysis.add(Or.of(assignments));
    }

    ExpressionTree<Object> invariant = And.of(perAnalysis);
    if (ExpressionTrees.getTrue().equals(invariant)) {
      // An invariant of 'true' is valid but useless, so we do not clutter the witness with it.
      return null;
    }
    return new InvariantEntry(invariant.toString(), pType, YAMLWitnessExpressionType.C, pLocation);
  }

  /**
   * Create the target waypoint for the given edge. For the overflow property the waypoint has to
   * point to the full expression whose evaluation overflows, for all other properties to the
   * beginning of the violating statement.
   */
  private WaypointRecord targetWaypoint(CFAEdge pEdge) {
    FileLocation location = pEdge.getFileLocation();
    Set<Property> properties = getSpecification().getProperties();
    if (properties.size() == 1
        && properties.iterator().next() == CommonVerificationProperty.OVERFLOW
        && pEdge instanceof CCfaEdge cEdge) {
      Optional<FileLocation> fullExpression =
          CFAUtils.getClosestFullExpression(cEdge, getASTStructure());
      if (fullExpression.isPresent()) {
        location = fullExpression.orElseThrow();
      } else {
        logger.log(
            Level.INFO,
            "Could not find the full expression that overflows, the target waypoint of the witness"
                + " points to the whole statement instead.");
      }
    }
    return new WaypointRecord(
        WaypointType.TARGET,
        WaypointAction.FOLLOW,
        null,
        LocationRecord.createLocationRecordAtStart(
            location, pEdge.getPredecessor().getFunctionName()));
  }
}
