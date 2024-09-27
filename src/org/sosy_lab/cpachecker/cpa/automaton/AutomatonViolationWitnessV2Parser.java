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
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.And;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckCoversColumnAndLine;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckMatchesColumnAndLine;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckPassesThroughNodes;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckReachesElement;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.IsStatementEdge;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils.InvalidYAMLWitnessException;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.util.ast.ASTElement;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.ast.IfElement;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord.WaypointType;

class AutomatonViolationWitnessV2Parser extends AutomatonWitnessV2ParserCommon {

  private final CParser cparser;
  private final ParserTools parserTools;

  AutomatonViolationWitnessV2Parser(
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
   * @return an automaton transition encoding the reaching of the target
   */
  private AutomatonTransition handleTarget(
      String nextStateId, Integer followLine, Integer followColumn, Integer pDistanceToViolation) {
    // For target nodes it sometimes does not make sense to evaluate them at the last possible
    // sequence point as with assumptions. For example, a reach_error call will usually not have
    // any successors in the ARG, since the verification stops there. Therefore, handling targets
    // the same way as with assumptions would not work. As an overapproximation we use the
    // covers to present the desired functionality.
    AutomatonBoolExpr expr = new CheckMatchesColumnAndLine(followColumn, followLine);

    AutomatonTransition.Builder transitionBuilder =
        new AutomatonTransition.Builder(expr, nextStateId);
    transitionBuilder = distanceToViolation(transitionBuilder, pDistanceToViolation);

    // When we match the target state we want to enter the error location immediately
    transitionBuilder = transitionBuilder.withAssertion(createViolationAssertion());
    return transitionBuilder.build();
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
   * @return automata transitions matching the assumption waypoint
   * @throws InterruptedException if the function call is interrupted
   * @throws WitnessParseException if the constraint cannot be parsed
   */
  private AutomatonTransition handleAssumption(
      String nextStateId,
      ASTElement enterElement,
      int followLine,
      String function,
      Integer pDistanceToViolation,
      String constraint)
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

    return transitionBuilder.build();
  }

  /**
   * Transform a branching waypoint into automata transitions
   *
   * @param pAstCfaRelation the relation between the cfa and ast to find out which if statement is
   *     being considered
   * @param nextStateId the id of the next state in the automaton being constructed
   * @param followLine the line at which the target is
   * @param followColumn the column at which the target is
   * @param pDistanceToViolation the distance to the violation
   * @param pBranchToFollow which branch to follow, if true the if branch is followed
   * @return a list of transitions matching the branching waypoint, empty if they could not be
   *     created
   */
  private Optional<List<AutomatonTransition>> handleFollowWaypointAtStatement(
      AstCfaRelation pAstCfaRelation,
      String nextStateId,
      Integer followColumn,
      Integer followLine,
      Integer pDistanceToViolation,
      Boolean pBranchToFollow) {
    Optional<IfElement> optionalIfStructure =
        pAstCfaRelation.getIfStructureStartingAtColumn(followColumn, followLine);
    Optional<IterationElement> optionalIterationStructure =
        pAstCfaRelation.getIterationStructureStartingAtColumn(followColumn, followLine);
    if (optionalIfStructure.isEmpty() && optionalIterationStructure.isEmpty()) {
      logger.log(
          Level.FINE, "Could not find an element corresponding to the waypoint, skipping it");
      return Optional.empty();
    }

    Set<CFANode> nodesCondition;
    Set<CFANode> nodesThenBranch;
    Set<CFANode> nodesElseBranch;

    if (optionalIfStructure.isPresent()) {
      IfElement ifElement = optionalIfStructure.orElseThrow();
      nodesCondition = ifElement.getConditionNodes().toSet();
      nodesThenBranch = ifElement.getNodesBetweenConditionAndThenBranch();
      nodesElseBranch = ifElement.getNodesBetweenConditionAndElseBranch();
    } else if (optionalIterationStructure.isPresent()) {
      IterationElement iterationElement = optionalIterationStructure.orElseThrow();
      nodesCondition = iterationElement.getControllingExpressionNodes().toSet();
      nodesThenBranch = iterationElement.getNodesBetweenConditionAndBody();
      nodesElseBranch = iterationElement.getNodesBetweenConditionAndBody();
    } else {
      throw new AssertionError("This should never happen");
    }

    if (nodesThenBranch.equals(nodesElseBranch) || nodesCondition.isEmpty()) {
      logger.log(
          Level.FINE,
          "Skipping branching waypoint at if statement since the"
              + " then and else branch are both empty,"
              + " and currently there is no way to distinguish them.");
      return Optional.empty();
    }

    AutomatonBoolExpr condition =
        new CheckPassesThroughNodes(
            nodesCondition, pBranchToFollow ? nodesThenBranch : nodesElseBranch);
    AutomatonTransition followBranchTransition =
        distanceToViolation(
                new AutomatonTransition.Builder(condition, nextStateId), pDistanceToViolation)
            .build();

    // Add break state for the other branch, since we don't want to explore it
    CheckPassesThroughNodes negatedCondition =
        new CheckPassesThroughNodes(
            nodesCondition, !pBranchToFollow ? nodesThenBranch : nodesElseBranch);
    AutomatonTransition avoidBranchTransition =
        new AutomatonTransition.Builder(negatedCondition, AutomatonInternalState.BOTTOM).build();

    return Optional.of(ImmutableList.of(followBranchTransition, avoidBranchTransition));
  }

  /**
   * @param nextStateId the id of the next state in the automaton being constructed
   * @param followLine the line at which the target is
   * @param followColumn the column at which the target is
   * @param pDistanceToViolation the distance to the violation
   * @param constraint the constraint on the return value of the function. It can be null, which
   *     means that returning from the function is the relevant aspect
   * @param startLineToCFAEdge a mapping from the start line to the cfa edge
   * @return an automaton transition encoding the constraint on the returned value of a function
   * @throws InterruptedException if the function call is interrupted
   */
  private AutomatonTransition handleFunctionReturn(
      String nextStateId,
      Integer followLine,
      Integer followColumn,
      Integer pDistanceToViolation,
      @Nullable String constraint,
      Multimap<Integer, CFAEdge> startLineToCFAEdge)
      throws InterruptedException {

    AutomatonBoolExpr expr =
        new And(
            new CheckCoversColumnAndLine(followColumn, followLine),
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
      int columnEndOfEdge = edge.getFileLocation().getEndColumnInLine();
      if (columnEndOfEdge != followColumn
          || edge.getFileLocation().getEndingLineInOrigin() != followLine) {
        continue;
      }

      if (edge.getStatement() instanceof AFunctionCallAssignmentStatement statement) {
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
                          statement.getRightHandSide().getFunctionNameExpression().toString()),
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

    return transitionBuilder.build();
  }

  Automaton createViolationAutomatonFromEntries(List<AbstractEntry> pEntries)
      throws InterruptedException, InvalidYAMLWitnessException, WitnessParseException {
    List<PartitionedWaypoints> segments = segmentize(pEntries);
    // this needs to be called exactly WitnessAutomaton for the option
    // WitnessAutomaton.cpa.automaton.treatErrorsAsTargets to work m(
    final String automatonName = AutomatonGraphmlParser.WITNESS_AUTOMATON_NAME;

    // TODO: It may be worthwhile to refactor this into the CFA
    ImmutableListMultimap<Integer, @NonNull CFAEdge> startLineToCFAEdge =
        FluentIterable.from(cfa.edges())
            .index(edge -> edge.getFileLocation().getStartingLineNumber());

    int stateCounter = 0;
    final String initState = getStateName(stateCounter++);

    final ImmutableList.Builder<AutomatonInternalState> automatonStates =
        new ImmutableList.Builder<>();
    String currentStateId = initState;

    int distance = segments.size() - 1;

    for (PartitionedWaypoints entry : segments) {
      ImmutableList.Builder<AutomatonTransition> transitions = new ImmutableList.Builder<>();
      WaypointRecord follow = entry.follow();
      List<WaypointRecord> avoids = entry.avoids();
      if (!avoids.isEmpty()) {
        logger.log(
            Level.WARNING, "Avoid waypoints in violation witnesses V2 are currently ignored!");
      }
      String nextStateId = getStateName(stateCounter++);
      int followLine = follow.getLocation().getLine();
      int followColumn = follow.getLocation().getColumn();

      if (follow.getType().equals(WaypointType.TARGET)) {
        nextStateId = "X";
        transitions.add(handleTarget(nextStateId, followLine, followColumn, distance));
        if (stateCounter != segments.size() + 1) {
          logger.log(
              Level.INFO,
              "Target waypoint is not the last waypoint, following waypoints will be ignored!");
        }
        // Add the state directly, since we are exiting the loop afterwards
        automatonStates.add(
            new AutomatonInternalState(
                currentStateId,
                transitions.build(),
                /* pIsTarget= */ false,
                /* pAllTransitions= */ false,
                /* pIsCycleStart= */ false));
        currentStateId = nextStateId;
        break;
      } else if (follow.getType().equals(WaypointType.ASSUMPTION)) {
        ASTElement element =
            cfa.getAstCfaRelation().getTightestStatementForStarting(followLine, followColumn);
        transitions.add(
            handleAssumption(
                nextStateId,
                element,
                followLine,
                follow.getLocation().getFunction(),
                distance,
                follow.getConstraint().getValue()));
      } else if (follow.getType().equals(WaypointType.BRANCHING)) {
        AstCfaRelation astCFARelation = cfa.getAstCfaRelation();
        Verify.verifyNotNull(astCFARelation);

        Optional<List<AutomatonTransition>> ifStatementTransitions =
            handleFollowWaypointAtStatement(
                astCFARelation,
                nextStateId,
                followColumn,
                followLine,
                distance,
                Boolean.parseBoolean(follow.getConstraint().getValue()));

        if (ifStatementTransitions.isEmpty()) {
          logger.log(Level.INFO, "Could not handle branching waypoint, skipping it");
          continue;
        }

        transitions.addAll(ifStatementTransitions.orElseThrow());
      } else if (follow.getType().equals(WaypointType.FUNCTION_RETURN)) {
        transitions.add(
            handleFunctionReturn(
                nextStateId,
                followLine,
                followColumn,
                distance,
                follow.getConstraint().getValue(),
                startLineToCFAEdge));

      } else {
        logger.log(Level.WARNING, "Unknown waypoint type: " + follow.getType());
        continue;
      }

      automatonStates.add(
          new AutomatonInternalState(
              currentStateId,
              transitions.build(),
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
}
