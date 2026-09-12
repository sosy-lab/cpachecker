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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.ast.ASTElement;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.ast.AstUtils.BoundaryNodesComputationFailed;
import org.sosy_lab.cpachecker.util.ast.IfElement;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.variableclassification.VariablesCollectingVisitor;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InformationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.SegmentRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ViolationSequenceEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord.WaypointAction;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord.WaypointType;

public class CounterexampleToWitness extends AbstractYAMLWitnessExporter {

  /** The name under which the thread running {@code main} is registered. */
  static final String MAIN_THREAD_NAME = "main";

  public CounterexampleToWitness(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  /**
   * One step of a counterexample: a {@link CFAEdge} together with the threads involved in executing
   * it. Both thread names are empty for analyses that do not track threads.
   *
   * @param edge the edge that is executed in this step
   * @param currentThread the name of the thread executing {@code edge}
   * @param createdThread the name of the thread created by {@code edge}, if it creates one
   */
  public record WitnessPathStep(
      CFAEdge edge, Optional<String> currentThread, Optional<String> createdThread) {}

  /**
   * Return all CFA edges of the given path together with the threads executing them. Consecutive
   * states of an {@link ARGPath} are not necessarily connected by a single CFA edge, since an
   * analysis may handle a whole basic block in one step (cf. option
   * cpa.composite.aggregateBasicBlocks). {@link ARGPath#fullPathIterator()} resolves such holes
   * into the edges they stand for. For such edges the states enclosing the whole hole are used.
   */
  private static ImmutableList<WitnessPathStep> getPathSteps(ARGPath pPath) {
    // an analysis that does not track threads has no thread name for any step
    boolean tracksThreads = extractStateByType(pPath.getFirstState(), ThreadingState.class) != null;
    ImmutableList.Builder<WitnessPathStep> steps = ImmutableList.builder();

    for (PathIterator it = pPath.fullPathIterator(); it.hasNext(); it.advance()) {
      CFAEdge edge = it.getOutgoingEdge();
      if (!tracksThreads) {
        steps.add(new WitnessPathStep(edge, Optional.empty(), Optional.empty()));
        continue;
      }
      ARGState previousState =
          it.isPositionWithState() ? it.getAbstractState() : it.getPreviousAbstractState();
      ARGState nextState = it.getNextAbstractState();
      steps.add(
          new WitnessPathStep(
              edge,
              getCurrentThreadNameIfExists(nextState, edge),
              getNewThreadNameIfExists(nextState, previousState)));
    }

    return steps.build();
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
            edge.getFileLocation(),
            edge.getPredecessor().getFunction().getOrigName(),
            pAstCfaRelation);
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
            assumeEdge.getPredecessor().getFunction().getOrigName()));
  }

  private static Optional<String> getNewThreadNameIfExists(
      ARGState pState, ARGState pPreviousState) {
    ThreadingState threadingState = extractStateByType(pState, ThreadingState.class);
    if (threadingState == null) {
      return Optional.empty();
    }

    ThreadingState previousThreadingState =
        extractStateByType(pPreviousState, ThreadingState.class);
    if (previousThreadingState == null) {
      return Optional.empty();
    }

    return Sets.difference(threadingState.getThreadIds(), previousThreadingState.getThreadIds())
        .stream()
        .findFirst();
  }

  private static Optional<String> getCurrentThreadNameIfExists(ARGState pState, CFAEdge pEdge) {
    ThreadingState threadingState = extractStateByType(pState, ThreadingState.class);
    if (threadingState == null) {
      return Optional.empty();
    }

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

  private static OptionalInt getThreadIdIfExists(
      Optional<String> pThreadName, ImmutableMap<String, Integer> pThreadNameToId) {
    if (pThreadName.isPresent()) {
      return OptionalInt.of(Objects.requireNonNull(pThreadNameToId.get(pThreadName.orElseThrow())));
    }

    return OptionalInt.empty();
  }

  private List<WaypointRecord> buildWaypoints(
      WitnessPathStep pStep,
      ImmutableListMultimap<CFAEdge, String> pEdgeToAssumptions,
      AstCfaRelation pAstCFARelation,
      Map<CFAEdge, Integer> pEdgeToCurrentExpressionIndex,
      ImmutableMap.Builder<String, Integer> pThreadNameToIdBuilder,
      YAMLWitnessVersion pWitnessVersion) {

    CFAEdge edge = pStep.edge();

    // See if the edge contains an assignment of a VerifierNondet call
    if (CFAUtils.assignsNondetFunctionCall(edge)) {

      Optional<WaypointRecord> assumptionWaypoint =
          handleAssumptionWhenAtPossibleLocation(
              edge, pEdgeToAssumptions, pEdgeToCurrentExpressionIndex, pAstCFARelation);

      if (assumptionWaypoint.isEmpty()) {
        return ImmutableList.of();
      }

      WaypointRecord assumption = assumptionWaypoint.orElseThrow();

      if (pWitnessVersion.equals(YAMLWitnessVersion.V2d2)) {
        return ImmutableList.of(
            assumption.withThreadId(
                getThreadIdIfExists(pStep.currentThread(), pThreadNameToIdBuilder.buildOrThrow())));
      } else {
        return ImmutableList.of(assumption);
      }
    } else if (edge instanceof AssumeEdge assumeEdge) {
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

        if (!iterationElement.getControllingExpression().orElseThrow().edges().contains(edge)) {
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
        logger.log(Level.FINEST, "Could not find the AST structure for the edge:", edge);
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
                getThreadIdIfExists(pStep.currentThread(), pThreadNameToIdBuilder.buildOrThrow())));
      } else {
        return ImmutableList.of(waypointRecord);
      }

      // For witnesses version 2.2 we need to export also the threads being created
      // as function enter waypoints in order to match the thread being created
    } else if (pWitnessVersion.equals(YAMLWitnessVersion.V2d2)
        && threadCreationCall(edge).isPresent()) {

      CFunctionCallStatement functionCall = threadCreationCall(edge).orElseThrow();
      FileLocation functionCallLocation = functionCall.getFileLocation();
      OptionalInt columnOfCall =
          pAstCFARelation.getColumnOfFunctionCallParenthesis(
              functionCall.getFunctionCallExpression());
      if (columnOfCall.isEmpty()) {
        logger.log(
            Level.FINEST, "Could not compute the column of the thread creation for the edge:", edge);
        return ImmutableList.of();
      }

      // a thread is registered once, even if the edge that creates it is reported several times
      ImmutableMap<String, Integer> threadNameToId = pThreadNameToIdBuilder.buildOrThrow();
      String createdThread = pStep.createdThread().orElseThrow();
      if (!threadNameToId.containsKey(createdThread)) {
        pThreadNameToIdBuilder.put(createdThread, threadNameToId.size());
      }

      WaypointRecord waypointRecord =
          new WaypointRecord(
              WaypointType.FUNCTION_ENTER,
              WaypointAction.FOLLOW,
              null,
              new LocationRecord(
                  functionCallLocation.getFileName().toString(),
                  functionCallLocation.getStartingLineInOrigin(),
                  columnOfCall.orElseThrow(),
                  edge.getPredecessor().getFunctionName()),
              OptionalInt.empty());

      return ImmutableList.of(
          waypointRecord.withThreadId(
              getThreadIdIfExists(pStep.currentThread(), pThreadNameToIdBuilder.buildOrThrow())));

    } else if (exportCompleteCounterexample) {
      // Export all other edges which are not absolutely relevant for the counterexample
      Optional<WaypointRecord> assumptionWaypoint =
          handleAssumptionWhenAtPossibleLocation(
              edge, pEdgeToAssumptions, pEdgeToCurrentExpressionIndex, pAstCFARelation);

      if (assumptionWaypoint.isEmpty()) {
        return ImmutableList.of();
      }

      if (pWitnessVersion.equals(YAMLWitnessVersion.V2d2)) {
        return ImmutableList.of(
            assumptionWaypoint
                .orElseThrow()
                .withThreadId(
                    getThreadIdIfExists(
                        pStep.currentThread(), pThreadNameToIdBuilder.buildOrThrow())));
      } else {
        return ImmutableList.of(assumptionWaypoint.orElseThrow());
      }
    }

    // Not all edges are relevant for the counterexample, so we do not export them
    return ImmutableList.of();
  }

  /** Returns the call that creates a thread, e.g. to {@code pthread_create}, of {@code pEdge}. */
  private static Optional<CFunctionCallStatement> threadCreationCall(CFAEdge pEdge) {
    if (pEdge instanceof CStatementEdge statementEdge
        && statementEdge.getStatement() instanceof CFunctionCallStatement functionCallStatement
        && isCallTo(functionCallStatement.getFunctionCallExpression(), ThreadingTransferRelation.THREAD_START)) {
      return Optional.of(functionCallStatement);
    }
    return Optional.empty();
  }

  /**
   * Whether a {@code function_enter} waypoint can be exported for {@code pEdge}, which is what
   * introduces the thread that the edge creates to the witness. Waypoints of a thread may only be
   * exported if such a waypoint precedes them.
   */
  static boolean introducesThread(CFAEdge pEdge, AstCfaRelation pAstCfaRelation) {
    return threadCreationCall(pEdge)
        .map(
            call ->
                pAstCfaRelation
                    .getColumnOfFunctionCallParenthesis(call.getFunctionCallExpression())
                    .isPresent())
        .orElse(false);
  }

  /** Checks whether the given expression directly calls the function with the given name. */
  private static boolean isCallTo(CFunctionCallExpression pCall, String pFunctionName) {
    return pCall.getFunctionNameExpression() instanceof CIdExpression functionName
        && functionName.getDeclaration() != null
        && functionName.getDeclaration().getOrigName().equals(pFunctionName);
  }

  private static WaypointRecord defaultTargetWaypoint(
      CFAEdge pEdge, AstCfaRelation pAstCfaRelation) {
    // We need to process the file location to avoid exporting FileLocation.Dummy contents which are
    // generated when the edge contains internal variables of CPAchecker, for example when verifying
    // `sv-benchmarks/c/pthread-atomic/read_write_lock-2b.i` against data-races.
    FileLocation location = pEdge.getFileLocation();
    if (!location.isRealLocation()) {
      if (pEdge instanceof CStatementEdge pStatementEdge) {
        // For the default target waypoint we want to point to the statement which contains this
        // file location
        FileLocation fileLocationStatement = pStatementEdge.getStatement().getFileLocation();
        Optional<ASTElement> tightestStatementForStarting =
            pAstCfaRelation.getTightestStatementForStarting(
                fileLocationStatement.getStartingLineInOrigin(),
                OptionalInt.of(fileLocationStatement.getStartColumnInLine()));
        location = tightestStatementForStarting.orElseThrow().location();
      } else {
        throw new IllegalStateException(
            "Cannot export a target waypoint for an edge with a dummy file location: " + pEdge);
      }
    }

    return new WaypointRecord(
        WaypointType.TARGET,
        WaypointAction.FOLLOW,
        null,
        LocationRecord.createLocationRecordAtStart(
            location, pEdge.getPredecessor().getFunction().getOrigName()));
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
      return defaultTargetWaypoint(pEdge, pAstCfaRelation);
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
                fullExpressionLocation, pEdge.getPredecessor().getFunction().getOrigName()));
      } else {
        // This is well-defined for the reeachability property, for all others violation witnesses
        // are not really well-defined
        return defaultTargetWaypoint(pEdge, pAstCfaRelation);
      }
    }

    return defaultTargetWaypoint(pEdge, pAstCfaRelation);
  }

  private boolean hasNoVariables(CExpression pExpression, CFANode pNode) {
    VariablesCollectingVisitor variablesCollectingVisitor = new VariablesCollectingVisitor(pNode);
    Set<String> resultOfVisit = pExpression.accept(variablesCollectingVisitor);
    return resultOfVisit == null || resultOfVisit.isEmpty();
  }

  private boolean hasNoVariables(CStatement pStatement, CFANode pNode) {
    return switch (pStatement) {
      case CAssignment pCAssignment -> false;
      case CExpressionStatement pCExpressionStatement ->
          hasNoVariables(pCExpressionStatement.getExpression(), pNode);
      case CFunctionCall pCFunctionCall ->
          FluentIterable.from(pCFunctionCall.getFunctionCallExpression().getParameterExpressions())
              .allMatch(expression -> hasNoVariables(expression, pNode));
    };
  }

  /**
   * Joins the assumptions that hold after executing the edge of one step of a counterexample into a
   * single constraint.
   */
  static String buildAssumptionConstraint(FluentIterable<CExpression> pAssumptions) {
    FluentIterable<CExpression> assumptions = FluentIterable.from(pAssumptions.toList());
    // We should not export any assumptions which contains a restriction on the value where a
    // pointer points to in memory, since this may change or not even be valid. CPAchecker tracks
    // this information internally, but it is meaningless to the user. This is a heuristic to avoid
    // exporting this information.
    //
    // One example of such a case happens in:
    // sv-benchmarks/c/termination-recursive-malloc/rec_malloc_ex6.i
    // where the assumption `p1 == 8LL` is present, where p1 is a pointer.
    ComparesPointerWithNonPointer comparesPointerWithNonPointerVisitor =
        new ComparesPointerWithNonPointer();
    assumptions = assumptions.filter(stmt -> !stmt.accept(comparesPointerWithNonPointerVisitor));

    // Conjunct all assumptions for the edge into one assumption. One such case is
    // ../sv-benchmarks/c/seq-mthreaded/pals_STARTPALS_Triplicated.1.ufo.BOUNDED-10.pals.c where
    // on line 406 the assumptions are `next_state == 0`, `tmp == 0`, `tmp__0 == 0` and
    // `gate3Failed == 1`
    if (assumptions.isEmpty()) {
      // We need to export this waypoint in order to avoid errors caused by passing another
      // waypoint at the same location either too early or too late.
      return "1";
    }
    return assumptions
        .transform(CExpression::toParenthesizedASTString)
        // Remove any temporary variables created by CPAchecker
        .filter(s -> !s.contains("__CPAchecker_TMP"))
        .join(Joiner.on(" && "));
  }

  /** Returns the assumptions of a counterexample, filtered to {@link CExpression}s. */
  static FluentIterable<CExpression> getAssumptions(CFAEdgeWithAssumptions pEdgeWithAssumptions) {
    return FluentIterable.from(pEdgeWithAssumptions.getExpStmts())
        .transform(AExpressionStatement::getExpression)
        // Violation witnesses are currently only defined for C programs i.e. CExpressions
        // to make the following code simpler, we do the filtering as early as possible
        .filter(CExpression.class);
  }

  /**
   * Builds the violation sequence of a witness from an already prepared counterexample.
   *
   * @param pSteps the steps of the counterexample, in the order in which they are executed
   * @param pTargetThread the name of the thread that is active at the end of the counterexample
   * @param pEdgeToAssumptions the constraints that hold after executing an edge, in path order
   * @param pWitnessVersion the witness version to build the sequence for
   */
  ViolationSequenceEntry buildViolationSequence(
      ImmutableList<WitnessPathStep> pSteps,
      Optional<String> pTargetThread,
      ImmutableListMultimap<CFAEdge, String> pEdgeToAssumptions,
      YAMLWitnessVersion pWitnessVersion)
      throws IOException {

    AstCfaRelation pAstCfaRelation = getASTStructure();

    Map<CFAEdge, Integer> edgeToCurrentExpressionIndex = new HashMap<>();
    for (CFAEdge edge : pEdgeToAssumptions.keySet()) {
      edgeToCurrentExpressionIndex.put(edge, 0);
    }

    ImmutableList.Builder<SegmentRecord> segments = ImmutableList.builder();

    // This builder keeps track of the mapping between thread IDs and the order in which they were
    // created such that we can refer to them in the witness. Main always has the thread ID 0.
    ImmutableMap.Builder<String, Integer> threadNameToIdBuilder = new ImmutableMap.Builder<>();
    threadNameToIdBuilder.put(MAIN_THREAD_NAME, 0);

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
    for (WitnessPathStep step : pSteps) {
      List<WaypointRecord> waypoints =
          buildWaypoints(
              step,
              pEdgeToAssumptions,
              pAstCfaRelation,
              edgeToCurrentExpressionIndex,
              threadNameToIdBuilder,
              pWitnessVersion);

      if (!waypoints.isEmpty()) {
        segments.add(new SegmentRecord(waypoints));
      }

      edgeToCurrentExpressionIndex.compute(
          step.edge(), (key, value) -> (value == null) ? null : value + 1);
    }

    // Add target
    // In contrast to the semantics of assumptions, targets are evaluated at the next possible
    // segment point. Therefore, instead of creating a location record the way as is for
    // assumptions,
    // this needs to be done using another function
    // Ignore blank egdes, since the violation could not have happened there.
    WitnessPathStep lastEdge = violatingStep(pSteps);
    WaypointRecord waypointRecord = targetWaypoint(lastEdge.edge(), pAstCfaRelation);

    // Required for data races, since sometimes the last
    // waypoint may collide with the target waypoint
    boolean removeSecondToLastSegment = false;

    if (pWitnessVersion.equals(YAMLWitnessVersion.V2d2)) {
      if (getSpecification().getProperties().stream()
          .anyMatch(pProperty -> pProperty.equals(CommonVerificationProperty.DATA_RACE))) {
        // For data races we need to export a multi target segment which points to the last two
        // pSteps producing the violation
        //
        // For this we assume that the data race violation was found immediately such that
        // the data-race occured between the execution of the last and second to last thread. This
        // simplifies the witness, since we do not need to figure out which of the last ARGStates
        // actually contains the data race.
        //
        // For data races we can further filter the edges to not consider function calls and return
        // edges since the race should not be possible there
        ImmutableList<WitnessPathStep> edgesWithoutBlankEdges =
            FluentIterable.from(pSteps)
                .filter(step -> !(step.edge() instanceof BlankEdge))
                .filter(edge -> !(edge.edge() instanceof CFunctionReturnEdge))
                .filter(
                    edge ->
                        !(edge.edge() instanceof CReturnStatementEdge pStatementReturnEdge
                                && (pStatementReturnEdge.getExpression().isEmpty()
                                    || hasNoVariables(
                                        pStatementReturnEdge.getExpression().orElseThrow(),
                                        pStatementReturnEdge.getPredecessor())))
                            && !(edge.edge() instanceof CStatementEdge pStatementEdge
                                && hasNoVariables(
                                    pStatementEdge.getStatement(),
                                    pStatementEdge.getPredecessor())))
                .toList();

        WitnessPathStep lastEdgeOnThread = edgesWithoutBlankEdges.getLast();
        OptionalInt lastThreadId =
            getThreadIdIfExists(
                lastEdgeOnThread.currentThread(), threadNameToIdBuilder.buildOrThrow());
        Verify.verify(lastThreadId.isPresent(), "Last thread ID should be present for data races");

        OptionalInt secondToLastThreadId = OptionalInt.empty();
        Optional<WitnessPathStep> lastEdgeOnDifferentThread = Optional.empty();
        ImmutableMap<String, Integer> threadNameToId = threadNameToIdBuilder.buildOrThrow();
        for (WitnessPathStep edge :
            edgesWithoutBlankEdges.reverse().subList(1, edgesWithoutBlankEdges.size())) {
          secondToLastThreadId = getThreadIdIfExists(edge.currentThread(), threadNameToId);

          if (secondToLastThreadId.isPresent()
              && secondToLastThreadId.orElseThrow() != lastThreadId.orElseThrow()) {
            lastEdgeOnDifferentThread = Optional.of(edge);
            break;
          }
        }

        Verify.verify(
            secondToLastThreadId.isPresent(),
            "Second to last thread ID should be present for data races");

        ImmutableList<WaypointRecord> targetWaypoints =
            ImmutableList.of(
                targetWaypoint(lastEdgeOnThread.edge(), pAstCfaRelation).withThreadId(lastThreadId),
                targetWaypoint(lastEdgeOnDifferentThread.orElseThrow().edge(), pAstCfaRelation)
                    .withThreadId(secondToLastThreadId));

        SegmentRecord lastSegment = segments.build().getLast();
        if (FluentIterable.from(targetWaypoints)
            .anyMatch(waypoint -> isWaypointAtTheSameLineInSegment(lastSegment, waypoint))) {
          removeSecondToLastSegment = true;
        }

        segments.add(new SegmentRecord(targetWaypoints));
      } else {
        segments.add(
            SegmentRecord.ofOnlyElement(
                waypointRecord.withThreadId(
                    getThreadIdIfExists(pTargetThread, threadNameToIdBuilder.buildOrThrow()))));
      }
    } else {
      segments.add(SegmentRecord.ofOnlyElement(waypointRecord));
    }

    ImmutableList<SegmentRecord> buildSegment = segments.build();
    if (removeSecondToLastSegment) {
      buildSegment =
          Collections3.listAndElement(
              buildSegment.subList(0, buildSegment.size() - 2), buildSegment.getLast());
    }

    return new ViolationSequenceEntry(getMetadata(pWitnessVersion), buildSegment);
  }

  /**
   * Export the given counterexample to the path as a violation witness.
   *
   * @param pCex the counterexample to be exported
   * @param pPath the path to export the witness to
   * @throws IOException if writing the witness to the path is not possible
   */
  protected void exportWitness(
      CounterexampleInfo pCex, Path pPath, YAMLWitnessVersion pWitnessVersion) throws IOException {

    ImmutableListMultimap.Builder<CFAEdge, String> edgeToAssumptionsBuilder =
        new ImmutableListMultimap.Builder<>();
    if (pCex.isPreciseCounterExample()) {
      for (CFAEdgeWithAssumptions edgeWithAssumptions : pCex.getCFAPathWithAssignments()) {
        edgeToAssumptionsBuilder.put(
            edgeWithAssumptions.getCFAEdge(),
            buildAssumptionConstraint(getAssumptions(edgeWithAssumptions)));
      }
    }

    ARGPath targetPath = pCex.getTargetPath();
    ImmutableList<WitnessPathStep> steps = getPathSteps(targetPath);
    // the target waypoint is built for the last non-blank edge, but the thread executing it is
    // taken from the very last state of the path
    CFAEdge lastEdge = violatingStep(steps).edge();

    exportEntries(
        buildViolationSequence(
            steps,
            getCurrentThreadNameIfExists(targetPath.getLastState(), lastEdge),
            edgeToAssumptionsBuilder.build(),
            pWitnessVersion),
        pPath);
  }

  /**
   * Returns the last step whose edge can carry the violation, i.e. the step that the target
   * waypoint is built for.
   */
  static WitnessPathStep violatingStep(List<WitnessPathStep> pSteps) {
    for (WitnessPathStep step : Lists.reverse(pSteps)) {
      if (!(step.edge() instanceof BlankEdge)) {
        return step;
      }
    }
    throw new IllegalArgumentException("The counterexample consists of blank edges only.");
  }

  /** Wether there exists a follow waypoint at the same line in the segment. */
  private static boolean isWaypointAtTheSameLineInSegment(
      SegmentRecord pSegment, WaypointRecord pWaypoint) {
    return FluentIterable.from(pSegment.getSegment())
        .anyMatch(
            existingWaypoint ->
                existingWaypoint.getAction().equals(WaypointAction.FOLLOW)
                    && Objects.equals(
                        existingWaypoint.getLocation().getLine(),
                        pWaypoint.getLocation().getLine()));
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
