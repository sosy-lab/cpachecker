// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
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
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckCoversColumnAndLine;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckEntersIfBranch;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonVariable.AutomatonIntVariable;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils.InvalidYAMLWitnessException;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.ast.ASTStructure;
import org.sosy_lab.cpachecker.util.ast.IfStructure;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.WaypointRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.WaypointRecord.WaypointType;

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

  private AutomatonTransition handleTarget(
      String nextStateId,
      Integer followLine,
      Integer followColumn,
      String followFilename,
      Integer pDistanceToViolation)
      throws IOException {
    // For target nodes it sometimes does not make sense to evaluate them at the last possible
    // sequence point as with assumptions. For example, a reach_error call will usually not have
    // any successors in the ARG, since the verification stops there. Therefore handling targets
    // the same way as with assumptions would not work. As an overapproximation we use the
    // covers to present the desired functionality.
    AutomatonBoolExpr expr = new CheckCoversColumnAndLine(followColumn, followLine);

    AutomatonTransition.Builder builder = new AutomatonTransition.Builder(expr, nextStateId);
    builder = distanceToViolation(builder, pDistanceToViolation);

    // When we match the target state we want to enter the error location immediately
    builder = builder.withAssertion(createViolationAssertion());
    return builder.build();
  }

  private AutomatonTransition handleAssumption(
      String nextStateId,
      Integer followLine,
      Integer followColumn,
      String followFilename,
      String function,
      Integer pDistanceToViolation,
      String constraint)
      throws IOException, InterruptedException, WitnessParseException {

    // The semantics of the witnesses V2 imply that every assumption waypoint should be
    // valid before the sequence statement it points to. Due to the semantics of the format:
    // "An assumption waypoint is evaluated at the sequence point immediately before the
    // waypoint location. The waypoint is passed if the given constraint evaluates to true."
    // Therefore, we need the Reaches Offset guard.
    AutomatonBoolExpr expr = new CheckCoversColumnAndLine(followColumn, followLine);

    AutomatonTransition.Builder builder = new AutomatonTransition.Builder(expr, nextStateId);
    builder = distanceToViolation(builder, pDistanceToViolation);

    handleConstraint(constraint, Optional.ofNullable(function), followLine, builder);

    return builder.build();
  }

  private List<AutomatonTransition> handleFollowWaypointAtIfStatement(
      ASTStructure astStructure,
      String nextStateId,
      String followFilename,
      Integer followColumn,
      Integer followLine,
      Integer pDistanceToViolation,
      Boolean followIfBranch)
      throws IOException {
    // The -1 in the column is needed since the ASTStructure element starts at the offset before
    // the if keyword, but the waypoint points to the first character of the if keyword
    IfStructure ifStructure = astStructure.getIfStructureStartingAtColumn(followColumn, followLine);
    if (ifStructure == null) {
      logger.log(
          Level.INFO, "Could not find IfStructure corresponding to the waypoint, skipping it");
      return null;
    }

    if (ifStructure
        .getNodesBetweenConditionAndElseBranch()
        .equals(ifStructure.getNodesBetweenConditionAndThenBranch())) {
      logger.log(
          Level.INFO,
          "Skipping branching waypoint at if statement since the"
              + " then and else branch are both empty,"
              + " and currently there is no way to distinguish them.");
      return null;
    }

    AutomatonBoolExpr expr = new CheckEntersIfBranch(ifStructure, followIfBranch);
    AutomatonTransition.Builder builder = new AutomatonTransition.Builder(expr, nextStateId);
    builder = distanceToViolation(builder, pDistanceToViolation);

    List<AutomatonTransition> transitions = new ArrayList<>();
    transitions.add(builder.build());

    // Add break state for the other branch, since we don't want to explore it
    builder =
        new AutomatonTransition.Builder(
            new CheckEntersIfBranch(ifStructure, !followIfBranch), AutomatonInternalState.BOTTOM);
    transitions.add(builder.build());

    return transitions;
  }

  private AutomatonTransition handleFunctionReturn(
      String nextStateId,
      Integer followLine,
      Integer followColumn,
      String followFilename,
      Integer pDistanceToViolation,
      @Nullable String constraint,
      Multimap<Integer, CFAEdge> startLineToCFAEdge)
      throws IOException, InterruptedException {

    AutomatonBoolExpr expr = new CheckCoversColumnAndLine(followColumn, followLine, true);

    AutomatonTransition.Builder builder = new AutomatonTransition.Builder(expr, nextStateId);
    builder = distanceToViolation(builder, pDistanceToViolation);

    // This is basically a special case of an assumption waypoint.
    for (AStatementEdge edge :
        FluentIterable.from(startLineToCFAEdge.get(followLine)).filter(AStatementEdge.class)) {
      // The syntax of the witness V2 describes that the return statement must point to the
      // closing bracket of the function whose return statement is being considered
      int columnEndOfEdge =
          edge.getFileLocation().getStartingLineInOrigin()
              + edge.getFileLocation().getNodeLength()
              - 1;
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
          logger.log(Level.INFO, "Could not generate automaton assumption.");
          continue;
        }
        builder.withAssumptions(expressions);
        break;
      }
    }

    return builder.build();
  }

  Automaton createViolationAutomatonFromEntriesMatchingOffsets(List<AbstractEntry> pEntries)
      throws InterruptedException, InvalidYAMLWitnessException, IOException, WitnessParseException {
    List<Pair<WaypointRecord, List<WaypointRecord>>> segments = segmentize(pEntries);
    // this needs to be called exactly WitnessAutomaton for the option
    // WitnessAutomaton.cpa.automaton.treatErrorsAsTargets to work m(
    final String automatonName = AutomatonGraphmlParser.WITNESS_AUTOMATON_NAME;

    // TODO: It may be worthwhile to refactor this into the CFA
    Multimap<Integer, CFAEdge> startLineToCFAEdge =
        FluentIterable.from(cfa.edges())
            .index(edge -> edge.getFileLocation().getStartingLineNumber());

    int counter = 0;
    final String initState = getStateName(counter++);

    final List<AutomatonInternalState> automatonStates = new ArrayList<>();
    String currentStateId = initState;
    WaypointRecord follow;

    int distance = segments.size();

    for (Pair<WaypointRecord, List<WaypointRecord>> entry : segments) {
      List<AutomatonTransition> transitions = new ArrayList<>();
      follow = entry.getFirst();
      List<WaypointRecord> avoids = entry.getSecond();
      if (avoids != null && !avoids.isEmpty()) {
        logger.log(
            Level.WARNING, "Avoid waypoints in violation witnesses V2 are currently ignored!");
      }
      String nextStateId = getStateName(counter++);
      int followLine = follow.getLocation().getLine();
      int followColumn = follow.getLocation().getColumn();
      String followFilename = follow.getLocation().getFileName();

      if (follow.getType().equals(WaypointType.TARGET)) {
        nextStateId = "X";
        transitions.add(
            handleTarget(nextStateId, followLine, followColumn, followFilename, distance));
        if (counter != segments.size()) {
          logger.log(
              Level.INFO,
              "Target waypoint is not the last waypoint, following waypoints will be ignored!");
        }
        break;
      } else if (follow.getType().equals(WaypointType.ASSUMPTION)) {
        transitions.add(
            handleAssumption(
                nextStateId,
                followLine,
                followColumn,
                followFilename,
                follow.getLocation().getFunction(),
                distance,
                follow.getConstraint().getValue()));
      } else if (follow.getType().equals(WaypointType.BRANCHING)) {
        if (cfa.getASTStructure().isEmpty()) {
          logger.log(
              Level.INFO,
              "Cannot handle branching waypoint without ASTStructure, skipping waypoint");
          continue;
        }

        ASTStructure astStructure = cfa.getASTStructure().orElseThrow();
        // The -1 in the column is needed since the ASTStructure element starts at the offset before
        // the if keyword, but the waypoint points to the first character of the if keyword

        List<AutomatonTransition> ifStatementTransitions =
            handleFollowWaypointAtIfStatement(
                astStructure,
                nextStateId,
                followFilename,
                followColumn,
                followLine,
                distance,
                Boolean.parseBoolean(follow.getConstraint().getValue()));

        // TODO: Handle branching waypoints at IterationStatements
        if (ifStatementTransitions == null) {
          logger.log(
              Level.INFO, "Could not handle branching waypoint at if statement, skipping waypoint");
          continue;
        }

        transitions.addAll(ifStatementTransitions);
      } else if (follow.getType().equals(WaypointType.FUNCTION_RETURN)) {
        transitions.add(
            handleFunctionReturn(
                nextStateId,
                followLine,
                followColumn,
                followFilename,
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
              transitions,
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

    Automaton automaton;
    Map<String, AutomatonVariable> automatonVariables = new HashMap<>();
    AutomatonIntVariable distanceVariable =
        (AutomatonIntVariable)
            AutomatonVariable.createAutomatonVariable(
                "int",
                AutomatonGraphmlParser.DISTANCE_TO_VIOLATION,
                Integer.toString(segments.size() + 1));
    automatonVariables.put(AutomatonGraphmlParser.DISTANCE_TO_VIOLATION, distanceVariable);

    // new AutomatonInternalState(entryStateId, transitions, false, false, true)
    try {
      automaton = new Automaton(automatonName, automatonVariables, automatonStates, initState);
    } catch (InvalidAutomatonException e) {
      throw new WitnessParseException(
          "The witness automaton generated from the provided Witness V2 is invalid!", e);
    }

    automaton = invariantsSpecAutomaton.build(automaton, config, logger, shutdownNotifier, cfa);

    dumpAutomatonIfRequested(automaton);

    return automaton;
  }
}
