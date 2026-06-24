// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.base.Joiner;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.por.PORState;
import org.sosy_lab.cpachecker.cpa.por.PORThreadState;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.ast.AstUtils.BoundaryNodesComputationFailed;
import org.sosy_lab.cpachecker.util.ast.IfElement;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InformationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.SegmentRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ViolationSequenceEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord.WaypointAction;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord.WaypointType;

public class CounterexampleToWitness extends AbstractYAMLWitnessExporter {

  public CounterexampleToWitness(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  /**
   * Create an Assumption Waypoint at the position of the current edge with the given assumptions
   *
   * @param assumption the assumptions at this position
   * @param edge the edge which is the location at which the assumptions are valid
   * @param pAstCfaRelation the mapping between the
   * @return a waypoint constraining the execution to the given assumptions at the location of the
   *     edge
   */
  private static WaypointRecord handleAssumptionWaypoint(
      String assumption, CFAEdge edge, AstCfaRelation pAstCfaRelation) {

    InformationRecord informationRecord =
        new InformationRecord(assumption, null, YAMLWitnessExpressionType.C);
    LocationRecord location =
        LocationRecord.createLocationRecordAfterLocation(
            edge.getFileLocation(), edge.getPredecessor().getFunctionName(), pAstCfaRelation);
    return new WaypointRecord(
        WaypointType.ASSUMPTION, WaypointAction.FOLLOW, informationRecord, location);
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
      ImmutableListMultimap<CFAEdge, String> pEdgeToAssumptions,
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
    if (!pEdge
        .getSuccessor()
        .getLeavingEdges()
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
    if (!pEdge.getSuccessor().getLeavingEdges().filter(BlankEdge.class).isEmpty()
        || pEdge instanceof BlankEdge) {
      return Optional.empty();
    }

    String assumptions = pEdgeToAssumptions.get(pEdge).get(edgeToCurrentExpressionIndex.get(pEdge));

    return Optional.of(handleAssumptionWaypoint(assumptions, pEdge, pAstCfaRelation));
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
        WaypointType.BRANCHING,
        WaypointAction.FOLLOW,
        new InformationRecord(Boolean.toString(conditionTruthValue), null, null),
        LocationRecord.createLocationRecordAtStart(
            pAstElementLocation,
            assumeEdge.getFileLocation().getFileName().toString(),
            assumeEdge.getPredecessor().getFunctionName()));
  }

  private static Optional<String> getNewThreadNameIfExists(
      ARGState pState, ARGState pPreviousState) {
    ThreadingState threadingState = extractStateByType(pState, ThreadingState.class);
    ThreadingState previousThreadingState =
        extractStateByType(pPreviousState, ThreadingState.class);
    if (threadingState != null && previousThreadingState != null) {
      for (String threadId : threadingState.getThreadIds()) {
        if (!previousThreadingState.getThreadIds().contains(threadId)) {
          return Optional.of(threadId);
        }
      }
      return Optional.empty();
    }

    // The POR analysis does not use a ThreadingCPA but tracks the active threads inside the
    // PORState. The newly created thread is the one whose PID is present in the current state but
    // not in the previous one.
    PORState porState = extractStateByType(pState, PORState.class);
    PORState previousPorState = extractStateByType(pPreviousState, PORState.class);
    if (porState != null && previousPorState != null) {
      for (Integer pid : porState.threads().keySet()) {
        if (!previousPorState.threads().containsKey(pid)) {
          return getPorThreadName(porState, pid);
        }
      }
    }

    return Optional.empty();
  }

  private static Optional<String> getCurrentThreadNameIfExists(ARGState pState, CFAEdge pEdge) {
    ThreadingState threadingState = extractStateByType(pState, ThreadingState.class);
    if (threadingState != null) {
      for (String threadId : threadingState.getThreadIds()) {
        if (threadingState
            .getThreadLocation(threadId)
            .getLocationNode()
            .equals(pEdge.getSuccessor())) {

          return Optional.of(threadId);
        }
      }
      return Optional.empty();
    }

    // The POR analysis does not use a ThreadingCPA but tracks the active threads inside the
    // PORState. Since it clones the CFA per thread, the location nodes are thread-specific and thus
    // uniquely identify the thread that just moved to the successor of the edge.
    PORState porState = extractStateByType(pState, PORState.class);
    if (porState != null) {
      for (Map.Entry<Integer, PORThreadState> entry : porState.threads().entrySet()) {
        if (entry.getValue().pLocationState().getLocationNode().equals(pEdge.getSuccessor())) {
          return getPorThreadName(porState, entry.getKey());
        }
      }
    }

    return Optional.empty();
  }

  /**
   * Returns the name used to refer to a thread of the POR analysis in the witness. The main thread
   * always has PID 0 and is named "main" to match the thread IDs used during the export. Every other
   * thread is identified by the handle (i.e. the pthread_t variable) it was created with.
   */
  private static Optional<String> getPorThreadName(PORState pState, int pPid) {
    if (pPid == 0) {
      return Optional.of("main");
    }

    for (Map.Entry<String, Integer> entry : pState.threadHandles().entrySet()) {
      if (entry.getValue() == pPid) {
        return Optional.of(entry.getKey());
      }
    }

    return Optional.empty();
  }

  private static OptionalInt getThreadIdIfExists(
      ARGState pState, CFAEdge pEdge, ImmutableMap<String, Integer> pThreadNameToId) {
    Optional<String> threadId = getCurrentThreadNameIfExists(pState, pEdge);
    if (threadId.isPresent()) {
      Integer id = pThreadNameToId.get(threadId.orElseThrow());
      if (id != null) {
        return OptionalInt.of(id);
      }
    }

    return OptionalInt.empty();
  }

  private List<WaypointRecord> buildWaypoints(
      CFAEdge pEdge,
      ImmutableListMultimap<CFAEdge, String> pEdgeToAssumptions,
      AstCfaRelation pAstCFARelation,
      Map<CFAEdge, Integer> pEdgeToCurrentExpressionIndex,
      ImmutableMap.Builder<String, Integer> pThreadNameToIdBuilder,
      ARGState pState,
      ARGState pPreviousState,
      YAMLWitnessVersion pWitnessVersion) {

    // See if the edge contains an assignment of a VerifierNondet call
    if (CFAUtils.assignsNondetFunctionCall(pEdge)) {

      Optional<WaypointRecord> assumptionWaypoint =
          handleAssumptionWhenAtPossibleLocation(
              pEdge, pEdgeToAssumptions, pEdgeToCurrentExpressionIndex, pAstCFARelation);

      if (assumptionWaypoint.isEmpty()) {
        return ImmutableList.of();
      }

      WaypointRecord assumption = assumptionWaypoint.orElseThrow();

      if (pWitnessVersion.equals(YAMLWitnessVersion.V2d2)) {
        return ImmutableList.of(
            assumption.withThreadId(
                getThreadIdIfExists(pState, pEdge, pThreadNameToIdBuilder.buildOrThrow())));
      } else {
        return ImmutableList.of(assumption);
      }
    } else if (pEdge instanceof AssumeEdge assumeEdge) {
      // Without the AST structure we cannot guarantee that we are exporting at the beginning of
      // an iteration or if statement
      // To export the branching waypoint, we first find the IfElement or IterationElement
      // containing it. Then we look for the FileLocation of the structure
      // Currently, we only export IfStructures, since there is no nice way to say how often a loop
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
        try {
          nodesBetweenConditionAndFirstBranch = ifElement.getNodesBetweenConditionAndThenBranch();
          nodesBetweenConditionAndSecondBranch = ifElement.getNodesBetweenConditionAndElseBranch();
        } catch (BoundaryNodesComputationFailed e) {
          logger.log(Level.FINEST, "Could not compute the boundary nodes for the if element", e);
          return ImmutableList.of();
        }

        astElementLocation = ifElement.getCompleteElement().location();
      } else if (optionalIterationElement.isPresent()) {
        IterationElement iterationElement = optionalIterationElement.orElseThrow();
        astElementLocation = iterationElement.getCompleteElement().location();

        if (iterationElement.getControllingExpression().isEmpty()) {
          // This can only happen for an expression of the form `for(A;;B)`, which is a loop which
          // always evaluates to true. In this case an AssumeEdge will never be used, but a blank
          // edge will be used instead
          // TODO: Handle this case correctly by exporting useful information for this type of loop
          return ImmutableList.of();
        }

        if (iterationElement.getBody().edges().isEmpty()) {
          // This happens when the loop contains no body. This can happen when the whole computation
          // is being done in the condition. In this case we cannot distinguish if the edge goes
          // into the loop or exits it, since the ASTStructure does not contain information about
          // the edges exiting the loop.
          // TODO: Handle this case correctly by exporting useful information for this type of loop
          return ImmutableList.of();
        }

        if (!iterationElement.getControllingExpression().orElseThrow().edges().contains(pEdge)) {
          // In this case we have an assume edge inside the loop which has nothing to do with its
          // controlling expression. This case should be ignored.
          return ImmutableList.of();
        }

        try {
          nodesBetweenConditionAndFirstBranch = iterationElement.getNodesBetweenConditionAndBody();
          nodesBetweenConditionAndSecondBranch = iterationElement.getNodesBetweenConditionAndExit();
        } catch (BoundaryNodesComputationFailed e) {
          logger.logDebugException(
              e, "Could not compute the boundary nodes for the iteration element");
          return ImmutableList.of();
        }
      } else {
        // TODO: Handle conditional expressions. This would need to be added at the parser level
        // and then added to the AstCfaRelation. The problem is that this occurs at the expression
        // level and we currently only consider statements. The relevant parser expression type is
        // IASTConditionalExpression.
        logger.log(Level.FINEST, "Could not find the AST structure for the edge: " + pEdge);
        return ImmutableList.of();
      }

      if (!nodesBetweenConditionAndFirstBranch.contains(successor)
          && !nodesBetweenConditionAndSecondBranch.contains(successor)) {
        return ImmutableList.of();
      }

      Verify.verifyNotNull(astElementLocation);

      WaypointRecord waypointRecord =
          handleBranchingWaypoint(
              nodesBetweenConditionAndFirstBranch.contains(successor),
              astElementLocation,
              assumeEdge);

      if (pWitnessVersion.equals(YAMLWitnessVersion.V2d2)) {
        return ImmutableList.of(
            waypointRecord.withThreadId(
                getThreadIdIfExists(pState, pEdge, pThreadNameToIdBuilder.buildOrThrow())));
      } else {
        return ImmutableList.of(waypointRecord);
      }

      // For witnesses version 2.2 we need to export also the threads being created
      // as function enter waypoints in order to match the thread being created
    } else if (pWitnessVersion.equals(YAMLWitnessVersion.V2d2)
        && pEdge instanceof CStatementEdge pStatementEdge
        && pStatementEdge.getStatement() instanceof CFunctionCallStatement pFunctionCallStatement
        && pFunctionCallStatement
            .getFunctionCallExpression()
            .getDeclaration()
            .getOrigName()
            .equals("pthread_create")) {

      FileLocation functionCallLocation = pFunctionCallStatement.getFileLocation();

      // Register the newly created thread so that later waypoints can refer to it by its ID. If the
      // analysis does not expose the new thread (e.g. it does not track threads at all), we still
      // export the waypoint, just without a thread ID.
      Optional<String> currentThreadNameIfExists = getNewThreadNameIfExists(pState, pPreviousState);
      if (currentThreadNameIfExists.isPresent()) {
        ImmutableMap<String, Integer> threadNameToId = pThreadNameToIdBuilder.buildOrThrow();
        String newThreadName = currentThreadNameIfExists.orElseThrow();
        if (!threadNameToId.containsKey(newThreadName)) {
          pThreadNameToIdBuilder.put(newThreadName, threadNameToId.size());
        }
      }

      WaypointRecord waypointRecord =
          new WaypointRecord(
              WaypointType.FUNCTION_ENTER,
              WaypointAction.FOLLOW,
              null,
              new LocationRecord(
                  functionCallLocation.getFileName().toString(),
                  functionCallLocation.getStartingLineInOrigin(),
                  functionCallLocation.getStartColumnInLine()
                      + pFunctionCallStatement.toASTString().indexOf("("),
                  pStatementEdge.getPredecessor().getFunctionName()),
              OptionalInt.empty());

      return ImmutableList.of(
          waypointRecord.withThreadId(
              getThreadIdIfExists(pState, pEdge, pThreadNameToIdBuilder.buildOrThrow())));

    } else if (exportCompleteCounterexample) {
      // Export all other edges which are not absolutely relevant for the counterexample
      Optional<WaypointRecord> assumptionWaypoint =
          handleAssumptionWhenAtPossibleLocation(
              pEdge, pEdgeToAssumptions, pEdgeToCurrentExpressionIndex, pAstCFARelation);

      if (assumptionWaypoint.isEmpty()) {
        return ImmutableList.of();
      }

      if (pWitnessVersion.equals(YAMLWitnessVersion.V2d2)) {
        return ImmutableList.of(
            assumptionWaypoint
                .orElseThrow()
                .withThreadId(
                    getThreadIdIfExists(pState, pEdge, pThreadNameToIdBuilder.buildOrThrow())));
      } else {
        return ImmutableList.of(assumptionWaypoint.orElseThrow());
      }
    }

    // Not all edges are relevant for the counterexample, so we do not export them
    return ImmutableList.of();
  }

  private static WaypointRecord defaultTargetWaypoint(CFAEdge pEdge) {
    return new WaypointRecord(
        WaypointType.TARGET,
        WaypointAction.FOLLOW,
        null,
        LocationRecord.createLocationRecordAtStart(
            pEdge.getFileLocation(), pEdge.getPredecessor().getFunctionName()));
  }

  /**
   * Create a target waypoint for a specification violation for the given edge.
   *
   * @param pEdge the edge whose execution violates the specification
   * @return a target waypoint pointing to the location in which the specification was violated. For
   *     example for the `unreach-call` specification this is the call statement and for the
   *     `no-overflow` specification this is the full expression whose execution caused the
   *     violation
   */
  private WaypointRecord targetWaypoint(CFAEdge pEdge, AstCfaRelation pAstCfaRelation) {
    Specification specification = getSpecification();
    Set<Property> properties = specification.getProperties();

    if (properties.size() != 1) {
      return defaultTargetWaypoint(pEdge);
    }

    Property property = properties.iterator().next();
    if (property instanceof CommonVerificationProperty verificationProperty) {
      if (verificationProperty == CommonVerificationProperty.OVERFLOW) {
        // The target waypoint needs to point to the full expression which caused the overflow
        //
        // If we did not find the closest full expression to the edge this is a bug and should be
        // fixed, since we need to export the target waypoint to it as defined in the standard. This
        // is well-defined, since every edge used here contains an operation whose execution causes
        // an overflow in a C program
        FileLocation fullExpressionLocation =
            CFAUtils.getClosestFullExpression((CCfaEdge) pEdge, pAstCfaRelation).orElseThrow();

        return new WaypointRecord(
            WaypointType.TARGET,
            WaypointAction.FOLLOW,
            null,
            LocationRecord.createLocationRecordAtStart(
                fullExpressionLocation, pEdge.getPredecessor().getFunctionName()));
      } else {
        // This is well-defined for the reeachability property, for all others violation witnesses
        // are not really well-defined
        return defaultTargetWaypoint(pEdge);
      }
    }

    return defaultTargetWaypoint(pEdge);
  }

  /**
   * Export the given counterexample to the path as a Witness version 2.0
   *
   * @param pCex the counterexample to be exported
   * @param pPath the path to export the witness to
   * @throws IOException if writing the witness to the path is not possible
   */
  private void exportWitness(
      CounterexampleInfo pCex, Path pPath, YAMLWitnessVersion pWitnessVersion) throws IOException {
    AstCfaRelation astCFARelation = getASTStructure();

    ImmutableListMultimap.Builder<CFAEdge, String> edgeToAssumptionsBuilder =
        new ImmutableListMultimap.Builder<>();
    Map<CFAEdge, Integer> edgeToCurrentExpressionIndex = new HashMap<>();
    if (pCex.isPreciseCounterExample()) {
      for (CFAEdgeWithAssumptions edgeWithAssumptions : pCex.getCFAPathWithAssignments()) {
        CFAEdge edge = edgeWithAssumptions.getCFAEdge();
        FluentIterable<CExpression> assumptions =
            FluentIterable.from(edgeWithAssumptions.getExpStmts())
                .transform(AExpressionStatement::getExpression)
                // Violation witnesses are currently only defined for C programs i.e. CExpressions
                // to make the following code simpler, we do the filtering as early as possible
                .filter(CExpression.class);

        // We should not export any assumptions which contains a restriction on the value where a
        // pointer points to in memory, since this may change or not even be valid. CPAchecker
        // tracks
        // this information internally, but it is meaningless to the user. This is a heuristic to
        // avoid
        // exporting this information.
        //
        // One example of such a case happens in:
        // sv-benchmarks/c/termination-recursive-malloc/rec_malloc_ex6.i
        // where the assumption `p1 == 8LL` is present, where p1 is a pointer.
        ComparesPointerWithNonPointer comparesPointerWithNonPointerVisitor =
            new ComparesPointerWithNonPointer();
        assumptions =
            assumptions.filter(stmt -> !stmt.accept(comparesPointerWithNonPointerVisitor));

        // Conjunct all assumptions for the edge into one assumption. One such case is
        // ../sv-benchmarks/c/seq-mthreaded/pals_STARTPALS_Triplicated.1.ufo.BOUNDED-10.pals.c where
        // on line 406 the assumptions are `next_state == 0`, `tmp == 0`, `tmp__0 == 0` and
        // `gate3Failed == 1`
        String statement;
        if (assumptions.isEmpty()) {
          // We need to export this waypoint in order to avoid errors caused by passing another
          // waypoint at the same location either too early or too late.
          statement = "1";
        } else {
          statement =
              assumptions
                  .transform(CExpression::toParenthesizedASTString)
                  // Remove any temporary variables created by CPAchecker
                  .filter(s -> !s.contains("__CPAchecker_TMP"))
                  .join(Joiner.on(" && "));
        }

        edgeToAssumptionsBuilder.put(edge, statement);
        edgeToCurrentExpressionIndex.put(edge, 0);
      }
    }

    ImmutableListMultimap<CFAEdge, String> edgeToAssumptions = edgeToAssumptionsBuilder.build();

    ImmutableList.Builder<SegmentRecord> segments = ImmutableList.builder();
    List<CFAEdge> edges = pCex.getTargetPath().getInnerEdges();

    // This builder keeps track of the mapping between thread IDs and the order in which they were
    // created such that we can refer to them in the witness. Main always has the thread ID 0.
    ImmutableMap.Builder<String, Integer> threadNameToIdBuilder = new ImmutableMap.Builder<>();
    threadNameToIdBuilder.put("main", 0);

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
    for (int i = 0; i < edges.size(); i++) {
      CFAEdge edge = edges.get(i);
      ARGState state = pCex.getTargetPath().getStateSet().asList().get(i + 1);
      ARGState previousState = pCex.getTargetPath().getStateSet().asList().get(i);

      List<WaypointRecord> waypoints =
          buildWaypoints(
              edge,
              edgeToAssumptions,
              astCFARelation,
              edgeToCurrentExpressionIndex,
              threadNameToIdBuilder,
              state,
              previousState,
              pWitnessVersion);

      if (!waypoints.isEmpty()) {
        segments.add(new SegmentRecord(waypoints));
      }

      edgeToCurrentExpressionIndex.compute(
          edge, (key, value) -> (value == null) ? null : value + 1);
    }

    // Add target
    // In contrast to the semantics of assumptions, targets are evaluated at the next possible
    // segment point. Therefore, instead of creating a location record the way as is for
    // assumptions,
    // this needs to be done using another function
    CFAEdge lastEdge = edges.getLast();

    WaypointRecord waypointRecord = targetWaypoint(lastEdge, astCFARelation);

    if (pWitnessVersion.equals(YAMLWitnessVersion.V2d2)) {
      if (getSpecification().getProperties().stream()
          .anyMatch(pProperty -> pProperty.equals(CommonVerificationProperty.DATA_RACE))) {
        // For data races we need to export a multi target segment which points to the last two
        // edges producing the violation
        //
        // For this we assume that the data race violation was found immediately such that
        // the data-race occured between the execution of the last and second to last thread. This
        // simplifies the witness, since we do not need to figure out which of the last ARGStates
        // actually contains the data race. However, for this we need to ignore blank edges, since
        // they are actually not part of the possible data-race.
        ImmutableList<CFAEdge> edgesWithoutBlankEdges =
            FluentIterable.from(edges)
                .filter(
                    edge ->
                        !(edge instanceof BlankEdge
                            || edge instanceof CFunctionCallEdge
                            || edge instanceof CReturnStatementEdge))
                .toList();
        CFAEdge lastEdgeOnThread = edgesWithoutBlankEdges.get(edgesWithoutBlankEdges.size() - 1);
        OptionalInt lastThreadId =
            getThreadIdIfExists(
                pCex.getTargetPath()
                    .getStateSet()
                    .asList()
                    .get(edges.size() - edges.reversed().indexOf(lastEdgeOnThread)),
                lastEdgeOnThread,
                threadNameToIdBuilder.buildOrThrow());
        Verify.verify(lastThreadId.isPresent(), "Last thread ID should be present for data races");

        OptionalInt secondToLastThreadId = OptionalInt.empty();
        CFAEdge lastEdgeOnDifferentThread = null;
        for (CFAEdge edge :
            edgesWithoutBlankEdges.reverse().subList(1, edgesWithoutBlankEdges.size())) {
          secondToLastThreadId =
              getThreadIdIfExists(
                  pCex.getTargetPath()
                      .getStateSet()
                      .asList()
                      .get(edges.size() - edges.reversed().indexOf(edge)),
                  edge,
                  threadNameToIdBuilder.buildOrThrow());

          if (secondToLastThreadId.isPresent()
              && secondToLastThreadId.orElseThrow() != lastThreadId.orElseThrow()) {
            lastEdgeOnDifferentThread = edge;
            break;
          }
        }

        Verify.verify(
            secondToLastThreadId.isPresent(),
            "Second to last thread ID should be present for data races");

        segments.add(
            new SegmentRecord(
                ImmutableList.of(
                    targetWaypoint(lastEdgeOnThread, astCFARelation).withThreadId(lastThreadId),
                    targetWaypoint(
                            Objects.requireNonNull(lastEdgeOnDifferentThread), astCFARelation)
                        .withThreadId(secondToLastThreadId))));
      } else {
        segments.add(
            SegmentRecord.ofOnlyElement(
                waypointRecord.withThreadId(
                    getThreadIdIfExists(
                        pCex.getTargetPath().getStateSet().asList().get(edges.size()),
                        lastEdge,
                        threadNameToIdBuilder.buildOrThrow()))));
      }
    } else {
      segments.add(SegmentRecord.ofOnlyElement(waypointRecord));
    }

    exportEntries(
        new ViolationSequenceEntry(getMetadata(pWitnessVersion), segments.build()), pPath);
  }

  /**
   * Export the given counterexample to a witness file. The format of the witness file is determined
   * by the witness versions given in the configuration. All versions of witnesses will be exported.
   * Currently, only Version 2 exists for Violation Witnesses.
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
      Path outputFile = pOutputFileTemplate.getPath(uniqueId, witnessVersion.toString());
      exportWitness(pCex, outputFile, witnessVersion);
    }
  }
}
