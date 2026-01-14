// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.And;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckClosestFullExpressionMatchesColumnAndLine;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckCoversColumnAndLine;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckMatchesColumnAndLine;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckPassesThroughNodes;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckReachesElement;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.IsStatementEdge;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.Or;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils.InvalidYAMLWitnessException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.util.ast.ASTElement;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.ast.AstUtils.BoundaryNodesComputationFailed;
import org.sosy_lab.cpachecker.util.ast.IfElement;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ViolationSequenceEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord.WaypointType;

class AutomatonWitnessViolationV2d0Parser extends AutomatonWitnessV2ParserCommon {

  private final CParser cparser;
  private final ParserTools parserTools;

  AutomatonWitnessViolationV2d0Parser(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCFA)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pCFA);
    cparser =
        CParser.Factory.getParser(
            /*
             * FIXME: Use normal logger as soon as CParser supports parsing
             * expression trees natively, such that we can remove the workaround
             * with the undefined __CPAchecker_ACSL_return dummy function that
             * causes warnings to be logged.
             */
            LogManager.createNullLogManager(),
            CParser.Factory.getOptions(pConfig),
            pCFA.getMachineModel(),
            pShutdownNotifier);
    parserTools = ParserTools.create(ExpressionTrees.newFactory(), pCFA.getMachineModel(), pLogger);
  }

  private AutomatonTransition.Builder distanceToViolation(
      AutomatonTransition.Builder pBuilder, int pDistance) {
    return pBuilder.withActions(
        ImmutableList.of(
            new AutomatonAction.Assignment(
                AutomatonGraphmlParser.DISTANCE_TO_VIOLATION,
                new AutomatonIntExpr.Constant(pDistance))));
  }

  /**
   * Handles a target waypoint
   *
   * @param nextStateId the id of the next state in the automaton being constructed
   * @param followLine the line at which the target is
   * @param followColumn the column at which the target is
   * @param pDistanceToViolation the distance to the violation
   * @param transitions of the automaton that we extended by transition for given waypoint
   * @param automatonStates that we extended by the target state
   */
  protected void handleTarget(
      String nextStateId,
      Integer followLine,
      OptionalInt followColumn,
      Integer pDistanceToViolation,
      String currentStateId,
      ImmutableList.Builder<AutomatonTransition> transitions,
      ImmutableList.Builder<AutomatonInternalState> automatonStates) {
    // The violation points to the largest full expression which produces the error.
    //
    // TODO: Currently we only deal with statements as targets. In the future we may want to
    //  consider the full expression more closely.
    ASTElement tightestStatementForStarting =
        cfa.getAstCfaRelation()
            .getTightestStatementForStarting(followLine, followColumn)
            .orElseThrow();
    AutomatonBoolExpr expr =
        new Or(
            new CheckMatchesColumnAndLine(
                tightestStatementForStarting.location().getStartColumnInLine(), followLine),
            new CheckClosestFullExpressionMatchesColumnAndLine(
                tightestStatementForStarting.location().getStartColumnInLine(),
                followLine,
                cfa.getAstCfaRelation()));

    AutomatonTransition.Builder transitionBuilder =
        new AutomatonTransition.Builder(expr, nextStateId);
    transitionBuilder = distanceToViolation(transitionBuilder, pDistanceToViolation);

    // When we match the target state we want to enter the error location immediately
    transitionBuilder = transitionBuilder.withAssertion(createViolationAssertion());

    // We need to copy the target information such that CPAchecker returns the correct information
    // for the violated property. If this is not set it will return "WitnessAutomaton"
    AutomatonTransition transition =
        new AutomatonGraphmlParser.TargetInformationCopyingAutomatonTransition(transitionBuilder);

    transitions.add(transition);
    // Add the state directly, since we are exiting the loop afterward
    automatonStates.add(
        new AutomatonInternalState(
            currentStateId,
            transitions.build(),
            /* pIsTarget= */ false,
            /* pAllTransitions= */ false,
            /* pIsCycleStart= */ false));
  }

  /**
   * Create automaton transitions matching an assumption waypoint
   *
   * @param nextStateId the id of the next state in the automaton being constructed
   * @param followLine the line at which the target is
   * @param enterElement the element in the AST that should be entered in order to pass this
   *     waypoint
   * @param function the function in which the waypoint is valid
   * @param pDistanceToViolation the distance to the violation
   * @param constraint the constraint
   * @param transitions of the automaton that we extended by transition for given waypoint
   * @throws InterruptedException if the function call is interrupted
   * @throws WitnessParseException if the constraint cannot be parsed
   */
  protected void handleAssumption(
      String nextStateId,
      ASTElement enterElement,
      int followLine,
      String function,
      Integer pDistanceToViolation,
      String constraint,
      ImmutableList.Builder<AutomatonTransition> transitions)
      throws InterruptedException, WitnessParseException {

    // The semantics of the witnesses V2 imply that every assumption waypoint should be
    // valid before the sequence statement it points to. Due to the semantics of the format:
    // "An assumption waypoint is evaluated at the sequence point immediately before the
    // waypoint location. The waypoint is passed if the given constraint evaluates to true."
    // Therefore, we need the Reaches Offset guard.
    AutomatonBoolExpr expr = new CheckReachesElement(enterElement);

    AutomatonTransition.Builder transitionBuilder =
        new AutomatonTransition.Builder(expr, nextStateId);
    transitionBuilder = distanceToViolation(transitionBuilder, pDistanceToViolation);

    handleConstraint(constraint, Optional.ofNullable(function), followLine, transitionBuilder);

    transitions.add(transitionBuilder.build());
  }

  /**
   * Transform a branching waypoint into automata transitions
   *
   * @param pAstCfaRelation the relation between the CFA and ast to find out which if statement is
   *     being considered
   * @param nextStateId the id of the next state in the automaton being constructed
   * @param followLine the line at which the target is
   * @param followColumn the column at which the target is
   * @param pDistanceToViolation the distance to the violation
   * @param pBranchToFollow which branch to follow, if true the if branch is followed
   * @param transitions of the automaton that we extended by transition for given waypoint
   */
  protected void handleFollowWaypointAtStatement(
      AstCfaRelation pAstCfaRelation,
      String nextStateId,
      OptionalInt followColumn,
      Integer followLine,
      Integer pDistanceToViolation,
      Boolean pBranchToFollow,
      ImmutableList.Builder<AutomatonTransition> transitions)
      throws WitnessParseException {
    Verify.verifyNotNull(pAstCfaRelation);
    Optional<IfElement> optionalIfStructure =
        pAstCfaRelation.getIfStructureFollowingColumnAtTheSameLine(followLine, followColumn);
    Optional<IterationElement> optionalIterationStructure =
        pAstCfaRelation.getIterationStructureFollowingColumnAtTheSameLine(followColumn, followLine);

    // This is the case for ternary operators, which are expressions and therefore not covered by if
    // or iteration structures which only cover statements.
    Optional<ASTElement> astElement =
        pAstCfaRelation.getTightestStatementForStarting(followLine, followColumn);

    Optional<List<AutomatonTransition>> newTransitions;
    if (optionalIfStructure.isEmpty()
        && optionalIterationStructure.isEmpty()
        && astElement.isEmpty()) {
      logger.log(
          Level.INFO, "Could not find an element corresponding to the waypoint, skipping it");
      return;
    }

    Set<CFANode> nodesCondition;
    Set<CFANode> nodesThenBranch;
    Set<CFANode> nodesElseBranch;

    if (optionalIfStructure.isPresent()) {
      IfElement ifElement = optionalIfStructure.orElseThrow();
      nodesCondition = ifElement.getConditionNodes().toSet();
      try {
        nodesThenBranch = ifElement.getNodesBetweenConditionAndThenBranch();
        nodesElseBranch = ifElement.getNodesBetweenConditionAndElseBranch();
      } catch (BoundaryNodesComputationFailed e) {
        logger.logDebugException(e, "Could not compute the nodes between the condition and branch");
        return;
      }

    } else if (optionalIterationStructure.isPresent()) {
      IterationElement iterationElement = optionalIterationStructure.orElseThrow();
      nodesCondition = iterationElement.getControllingExpressionNodes().toSet();
      try {
        nodesThenBranch = iterationElement.getNodesBetweenConditionAndBody();
        nodesElseBranch = iterationElement.getNodesBetweenConditionAndExit();
      } catch (BoundaryNodesComputationFailed e) {
        logger.logDebugException(e, "Could not compute the nodes between the condition and branch");
        return;
      }
    } else if (astElement.isPresent()) {
      // Ternary operator case, we cannot currently distinguish between then and else branch.
      // Therefore, we first check if we are really in a ternary operator, by checking if we
      // have assume edges at this location, and if so, we throw an exception that we cannot
      // support this.
      //
      // Else we could not find a proper if or iteration structure, but we have a statement
      // here, to continue with validating the witness we just log this and skip the waypoint.
      // This does not conform to the witness spec, but will be complete, i.e., we will not
      // miss violations due to this.
      ImmutableList<CAssumeEdge> edges =
          FluentIterable.from(astElement.orElseThrow().edges()).filter(CAssumeEdge.class).toList();

      if (edges.isEmpty()) {
        logger.log(
            Level.INFO,
            "Could not find a "
                + "statement corresponding to the location at line "
                + followLine
                + " and column "
                + followColumn
                + " of the statement, skipping it");
        return;
      }

      throw new WitnessParseException(
          "Ternary operators as branching waypoints are currently not supported!");

    } else {
      throw new AssertionError("This should never happen");
    }

    // When the condition is empty we still want to be able to pass the waypoint
    Set<CFANode> adaptedNodesCondition =
        nodesCondition.isEmpty()
            ? FluentIterable.from(nodesThenBranch)
                .append(nodesElseBranch)
                .transformAndConcat(CFAUtils::allPredecessorsOf)
                .toSet()
            : nodesCondition;

    AutomatonBoolExpr condition =
        new CheckPassesThroughNodes(
            adaptedNodesCondition, pBranchToFollow ? nodesThenBranch : nodesElseBranch);
    AutomatonTransition followBranchTransition =
        distanceToViolation(
                new AutomatonTransition.Builder(condition, nextStateId), pDistanceToViolation)
            .build();

    // Add break state for the other branch, since we don't want to explore it
    CheckPassesThroughNodes negatedCondition =
        new CheckPassesThroughNodes(
            adaptedNodesCondition, !pBranchToFollow ? nodesThenBranch : nodesElseBranch);
    AutomatonTransition avoidBranchTransition =
        new AutomatonTransition.Builder(negatedCondition, AutomatonInternalState.BOTTOM).build();

    newTransitions = Optional.of(ImmutableList.of(followBranchTransition, avoidBranchTransition));
    if (newTransitions.orElseThrow().isEmpty()) {
      logger.log(Level.INFO, "Could not handle branching waypoint, skipping it");
      return;
    }
    transitions.addAll(newTransitions.orElseThrow());
  }

  /**
   * Transform a function return into automata transitions
   *
   * @param nextStateId the id of the next state in the automaton being constructed
   * @param followLine the line at which the target is
   * @param followColumn the column at which the target is
   * @param pDistanceToViolation the distance to the violation
   * @param constraint the constraint on the return value of the function. It can be null, which
   *     means that returning from the function is the relevant aspect
   * @param startLineToCFAEdge a mapping from the start line to the CFA edge
   * @throws InterruptedException if the function call is interrupted
   */
  protected void handleFunctionReturn(
      String nextStateId,
      Integer followLine,
      OptionalInt followColumn,
      Integer pDistanceToViolation,
      @Nullable String constraint,
      Multimap<Integer, CFAEdge> startLineToCFAEdge,
      ImmutableList.Builder<AutomatonTransition> transitions)
      throws InterruptedException {

    // TODO: Handle missing columns properly here
    AutomatonBoolExpr expr =
        new And(
            new CheckCoversColumnAndLine(followColumn.orElseThrow(), followLine),
            // Edges which correspond to blocks in the code, like function declaration edges and
            // iteration statement edges may fulfill the condition, but are not always desired.
            new IsStatementEdge());

    AutomatonTransition.Builder transitionBuilder =
        new AutomatonTransition.Builder(expr, nextStateId);
    transitionBuilder = distanceToViolation(transitionBuilder, pDistanceToViolation);

    // This is basically a special case of an assumption waypoint.
    for (AStatementEdge edge :
        FluentIterable.from(startLineToCFAEdge.get(followLine)).filter(AStatementEdge.class)) {
      // The syntax of the witness V2 describes that the return statement must point to the
      // closing bracket of the function whose return statement is being considered
      AStatement statement = edge.getStatement();
      FileLocation statementLocation = statement.getFileLocation();
      int columnOfClosingBracketInFunctionCall = statementLocation.getEndColumnInLine() - 1;
      if (columnOfClosingBracketInFunctionCall != followColumn.orElseThrow()
          || statement.getFileLocation().getEndingLineInOrigin() != followLine) {
        continue;
      }

      if (statement instanceof AFunctionCallAssignmentStatement functionCallStatement) {
        Set<String> constraints = new HashSet<>();
        if (constraint != null) {
          constraints.add(constraint);
        }

        Scope scope =
            switch (cfa.getLanguage()) {
              case C -> new CProgramScope(cfa, logger);
              default -> DummyScope.getInstance();
            };

        List<AExpression> expressions;
        try {
          expressions =
              CParserUtils.convertStatementsToAssumptions(
                  CParserUtils.parseStatements(
                      constraints,
                      Optional.ofNullable(
                          functionCallStatement
                              .getRightHandSide()
                              .getFunctionNameExpression()
                              .toString()),
                      cparser,
                      scope,
                      parserTools),
                  cfa.getMachineModel(),
                  logger);
        } catch (InvalidAutomatonException e) {
          logger.logDebugException(e, "Could not parse the constraint for the function return");
          continue;
        }
        transitionBuilder.withAssumptions(expressions);
        break;
      }
    }

    transitions.add(transitionBuilder.build());
  }

  /**
   * Separate the entries into segments and check whether the witness is valid witness v2.0
   *
   * @param pEntries the entries to segmentize
   * @return the segmentized entries
   * @throws InvalidYAMLWitnessException if the YAML witness is not valid
   */
  protected ImmutableList<PartitionedWaypoints> segmentizeAndCheckV2d0(List<AbstractEntry> pEntries)
      throws InvalidYAMLWitnessException {
    ViolationSequenceEntry violationEntry = getViolationSequence(pEntries);
    ImmutableList<PartitionedWaypoints> segmentizedEntries = segmentize(violationEntry);
    checkTarget(violationEntry);
    return segmentizedEntries;
  }

  protected ViolationSequenceEntry getViolationSequence(List<AbstractEntry> pEntries)
      throws InvalidYAMLWitnessException {
    if (pEntries.size() != 1) {
      throw new InvalidYAMLWitnessException(
          "A witness in YAML format can have only one violation sequence !");
    }
    return (ViolationSequenceEntry) pEntries.getFirst();
  }

  Automaton createViolationAutomatonFromEntries(List<AbstractEntry> pEntries)
      throws InterruptedException, InvalidYAMLWitnessException, WitnessParseException {
    List<PartitionedWaypoints> segments = segmentizeAndCheckV2d0(pEntries);
    // this needs to be called exactly WitnessAutomaton for the option
    // WitnessAutomaton.cpa.automaton.treatErrorsAsTargets to work
    final String automatonName = AutomatonGraphmlParser.WITNESS_AUTOMATON_NAME;

    int stateCounter = 0;
    final String initState = getStateName(stateCounter++);

    final ImmutableList.Builder<AutomatonInternalState> automatonStates =
        new ImmutableList.Builder<>();

    // add bottom state
    automatonStates.add(AutomatonInternalState.BOTTOM);
    String currentStateId = initState;

    int distance = segments.size() - 1;

    for (PartitionedWaypoints entry : segments) {
      ImmutableList.Builder<AutomatonTransition> transitions = new ImmutableList.Builder<>();
      WaypointRecord follow = entry.follow().orElseThrow();
      String nextStateId = getStateName(stateCounter++);

      handleWaypointsV2d0(
          entry, follow, transitions, automatonStates, distance, nextStateId, currentStateId);
      ImmutableList<AutomatonTransition> transitionsList = transitions.build();
      if (transitionsList.isEmpty()) {
        continue;
      }

      if (follow.getType().equals(WaypointType.TARGET)) {
        if (stateCounter != segments.size() + 1) {
          logger.log(
              Level.INFO,
              "Target waypoint is not the last waypoint, following waypoints will be ignored!");
        }
        currentStateId = "X";
        break;
      }

      automatonStates.add(
          new AutomatonInternalState(
              currentStateId,
              transitionsList,
              /* pIsTarget= */ false,
              /* pAllTransitions= */ false,
              /* pIsCycleStart= */ false));

      distance--;
      currentStateId = nextStateId;
    }

    // add last state and stutter in it:
    automatonStates.add(
        new AutomatonInternalState(
            currentStateId,
            ImmutableList.of(
                new AutomatonGraphmlParser.TargetInformationCopyingAutomatonTransition(
                    new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, currentStateId)
                        .withAssertion(createViolationAssertion()))),
            /* pIsTarget= */ false,
            /* pAllTransitions= */ false,
            /* pIsCycleStart= */ false));

    ImmutableMap<String, AutomatonVariable> automatonVariables =
        ImmutableMap.of(
            AutomatonGraphmlParser.DISTANCE_TO_VIOLATION,
            AutomatonVariable.createAutomatonVariable(
                /* pType= */ "int",
                AutomatonGraphmlParser.DISTANCE_TO_VIOLATION,
                Integer.toString(segments.size())));

    Automaton automaton;
    try {
      automaton =
          new Automaton(automatonName, automatonVariables, automatonStates.build(), initState);
    } catch (InvalidAutomatonException e) {
      throw new WitnessParseException(
          "The witness automaton generated from the provided Witness V2 is invalid!", e);
    }

    automaton =
        getInvariantsSpecAutomaton().build(automaton, config, logger, shutdownNotifier, cfa);

    dumpAutomatonIfRequested(automaton);

    return automaton;
  }

  protected void handleWaypointsV2d0(
      PartitionedWaypoints pEntry,
      WaypointRecord follow,
      ImmutableList.Builder<AutomatonTransition> transitions,
      ImmutableList.Builder<AutomatonInternalState> automatonStates,
      int distance,
      String nextStateId,
      String currentStateId)
      throws InterruptedException, WitnessParseException {
    // TODO: It may be worthwhile to refactor this into the CFA
    ImmutableListMultimap<Integer, @NonNull CFAEdge> startLineToCFAEdge =
        FluentIterable.from(cfa.edges())
            .index(edge -> edge.getFileLocation().getStartingLineInOrigin());

    Integer followLine = follow.getLocation().getLine();
    OptionalInt followColumn = follow.getLocation().getColumn();

    for (WaypointRecord avoid : pEntry.avoids()) {
      // Handle all avoid waypoints. They can be handled similarly to follow waypoints, but
      // instead of going to the next state, they go to the bottom state.
      switch (avoid.getType()) {
        case WaypointType.ASSUMPTION ->
            handleAssumption(
                AutomatonInternalState.BOTTOM.getName(),
                cfa.getAstCfaRelation()
                    .getTightestStatementForStarting(followLine, followColumn)
                    .orElseThrow(),
                avoid.getLocation().getLine(),
                avoid.getLocation().getFunction(),
                distance,
                avoid.getConstraint().getValue(),
                transitions);
        case WaypointType.BRANCHING ->
            handleFollowWaypointAtStatement(
                cfa.getAstCfaRelation(),
                currentStateId,
                avoid.getLocation().getColumn(),
                avoid.getLocation().getLine(),
                distance,
                // We negate to remain in the same state, the actual branch we want to avoid lands
                // in the bottom state automatically due to how we handle branching waypoints
                !Boolean.parseBoolean(avoid.getConstraint().getValue()),
                transitions);
        case WaypointType.FUNCTION_RETURN -> {
          handleFunctionReturn(
              currentStateId,
              avoid.getLocation().getLine(),
              avoid.getLocation().getColumn(),
              distance,
              "!(" + avoid.getConstraint().getValue() + ")",
              startLineToCFAEdge,
              transitions);
        }
        case FUNCTION_ENTER ->
            throw new WitnessParseException(
                "We currently do not support function enter waypoints.");
        case TARGET ->
            throw new WitnessParseException("Avoid waypoints of type target are invalid.");
      }
    }

    switch (follow.getType()) {
      case WaypointType.TARGET ->
          handleTarget(
              "X",
              followLine,
              followColumn,
              distance,
              currentStateId,
              transitions,
              automatonStates);
      case WaypointType.ASSUMPTION ->
          handleAssumption(
              nextStateId,
              cfa.getAstCfaRelation()
                  .getTightestStatementForStarting(followLine, followColumn)
                  .orElseThrow(),
              followLine,
              follow.getLocation().getFunction(),
              distance,
              follow.getConstraint().getValue(),
              transitions);
      case WaypointType.BRANCHING ->
          handleFollowWaypointAtStatement(
              cfa.getAstCfaRelation(),
              nextStateId,
              followColumn,
              followLine,
              distance,
              Boolean.parseBoolean(follow.getConstraint().getValue()),
              transitions);
      case FUNCTION_ENTER ->
          throw new WitnessParseException("We currently do not support function enter waypoints.");
      case WaypointType.FUNCTION_RETURN ->
          handleFunctionReturn(
              nextStateId,
              followLine,
              followColumn,
              distance,
              follow.getConstraint().getValue(),
              startLineToCFAEdge,
              transitions);
    }
  }
}
