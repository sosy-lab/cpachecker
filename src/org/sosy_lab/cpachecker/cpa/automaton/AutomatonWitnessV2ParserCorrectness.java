// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckCoversColumnAndLine;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckEndsAtNodes;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInformationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;

class AutomatonWitnessV2ParserCorrectness extends AutomatonWitnessV2ParserCommon {

  AutomatonWitnessV2ParserCorrectness(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCFA)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pCFA);
  }

  /**
   * Create an automaton from a correctness witness. This automaton contains a single node and each
   * invariant is marked as such on an edge which matches starts at that single state and returns to
   * it. Each transition is only passed if the locations match.
   *
   * @param entries the entries of the correctness witness
   * @return an automaton for the correctness witness
   * @throws InterruptedException if the function is interrupted
   * @throws WitnessParseException if there is some problem parsing the witness
   */
  Automaton createCorrectnessAutomatonFromEntries(List<AbstractEntry> entries)
      throws InterruptedException, WitnessParseException {
    String automatonName = "No Loop Invariant Present";
    Map<String, AutomatonVariable> automatonVariables = new HashMap<>();
    String entryStateId = "singleState";

    AstCfaRelation astCfaRelation = cfa.getAstCfaRelation();

    ImmutableList.Builder<AutomatonTransition> transitions = new ImmutableList.Builder<>();

    SetMultimap<Pair<Integer, Integer>, Pair<String, String>> lineToSeenInvariants =
        HashMultimap.create();

    for (AbstractEntry entry : entries) {
      if (entry instanceof InvariantSetEntry invariantSetEntry) {
        for (AbstractInformationRecord entryElement : invariantSetEntry.content) {
          if (entryElement instanceof InvariantEntry invariantEntry) {
            Optional<String> resultFunction =
                Optional.ofNullable(invariantEntry.getLocation().getFunction());
            String invariantString = invariantEntry.getValue();
            Integer line = invariantEntry.getLocation().getLine();
            Integer column = invariantEntry.getLocation().getColumn();
            Pair<Integer, Integer> position = Pair.of(line, column);
            String invariantType = invariantEntry.getType();

            // Parsing is expensive for long invariants, we therefore try to reduce it
            Pair<String, String> lookupKey = Pair.of(resultFunction.orElseThrow(), invariantString);

            if (lineToSeenInvariants.get(position).contains(lookupKey)) {
              continue;
            } else {
              lineToSeenInvariants.get(position).add(lookupKey);
            }

            ExpressionTree<AExpression> invariant = transformer.parseInvariantEntry(invariantEntry);

            if (invariant.equals(ExpressionTrees.getTrue())) {
              continue;
            }

            ToCExpressionVisitor toCExpressionVisitor =
                new ToCExpressionVisitor(cfa.getMachineModel(), logger);
            CExpression invariantAsCExpression;
            try {
              invariantAsCExpression = invariant.accept(toCExpressionVisitor);
            } catch (UnrecognizedCodeException pE) {
              throw new WitnessParseException(
                  "Could not parse the invariant into a C-Expression: "
                      + invariant.toString().substring(0, 100),
                  pE);
            }

            if (invariantType.equals(InvariantRecordType.LOOP_INVARIANT.getKeyword())) {
              Optional<IterationElement> optionalIterationStructure =
                  astCfaRelation.getIterationStructureStartingAtColumn(column, line);

              IterationElement iterationElement = optionalIterationStructure.orElseThrow();
              Optional<CFANode> optionalLoopHead = iterationElement.getLoopHead();
              if (optionalLoopHead.isEmpty()) {
                // TODO: Handle correctly
                continue;
              }

              transitions.add(
                  new AutomatonTransition.Builder(
                          new CheckEndsAtNodes(ImmutableSet.of(optionalLoopHead.orElseThrow())),
                          entryStateId)
                      .withCandidateInvariants(invariant)
                      .withAnalysisAssertion(invariantAsCExpression)
                      .build());
            } else if (invariantType.equals(InvariantRecordType.LOCATION_INVARIANT.getKeyword())) {
              transitions.add(
                  new AutomatonTransition.Builder(
                          new CheckCoversColumnAndLine(column, line), entryStateId)
                      .withCandidateInvariants(invariant)
                      .withAnalysisAssertion(invariantAsCExpression)
                      .build());
            } else {
              throw new WitnessParseException(
                  "The witness contained other statements than Loop and Location Invariants!");
            }
          }
        }
        automatonName = invariantSetEntry.metadata.getUuid();
      } else {
        throw new WitnessParseException(
            "The witness contained other statements than Loop Invariants!");
      }
    }

    List<AutomatonInternalState> automatonStates =
        ImmutableList.of(
            new AutomatonInternalState(entryStateId, transitions.build(), false, false, true));

    Automaton automaton;
    try {
      automaton = new Automaton(automatonName, automatonVariables, automatonStates, entryStateId);
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
