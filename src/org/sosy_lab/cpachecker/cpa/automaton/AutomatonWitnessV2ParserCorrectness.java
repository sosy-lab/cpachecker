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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckCoversColumnAndLine;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckEndsAtNodes;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessVersion;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInformationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;

class AutomatonWitnessV2ParserCorrectness extends AutomatonWitnessParserCommon {
  protected static String ENTRY_STATE_ID = "singleState";

  AutomatonWitnessV2ParserCorrectness(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCFA)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pCFA);
  }

  protected String getUuidFromEntries(List<AbstractEntry> entries) throws WitnessParseException {
    String automatonName = "No Loop Invariant Present";
    for (AbstractEntry entry : entries) {
      if (entry instanceof InvariantSetEntry invariantSetEntry) {
        automatonName = invariantSetEntry.metadata.getUuid();
      } else {
        throw new WitnessParseException(
            "The witness contained other statements than Loop and Location Invariants!");
      }
    }
    return automatonName;
  }

  protected ImmutableList.Builder<AutomatonTransition> createTransitionsFromEntries(
      List<AbstractEntry> entries) throws InterruptedException, WitnessParseException {
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

            // Check for transition loop invariants and do not throw an exception as they are in the
            // future formats.
            if (invariantType.equals(InvariantRecordType.TRANSITION_LOOP_INVARIANT.getKeyword())
                && invariantSetEntry
                    .metadata
                    .getFormatVersion()
                    .equals(YAMLWitnessVersion.V2d1.toString())) {
              continue;
            }

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
                          ENTRY_STATE_ID)
                      .withCandidateInvariants(invariant)
                      .build());
            } else if (invariantType.equals(InvariantRecordType.LOCATION_INVARIANT.getKeyword())) {
              transitions.add(
                  new AutomatonTransition.Builder(
                          new CheckCoversColumnAndLine(column, line), ENTRY_STATE_ID)
                      .withCandidateInvariants(invariant)
                      .build());
            } else {
              throw new WitnessParseException(
                  "The witness contained other statements than Loop and Location Invariants!");
            }
          }
        }
      } else {
        throw new WitnessParseException(
            "The witness contained other statements than Loop Invariants!");
      }
    }
    return transitions;
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
    ImmutableList<AutomatonTransition> automatonTransitions =
        createTransitionsFromEntries(entries).build();
    List<AutomatonInternalState> automatonStates =
        ImmutableList.of(
            new AutomatonInternalState(ENTRY_STATE_ID, automatonTransitions, false, false, true));
    Map<String, AutomatonVariable> automatonVariables = new HashMap<>();

    Automaton automaton;
    try {
      automaton =
          new Automaton(
              getUuidFromEntries(entries), automatonVariables, automatonStates, ENTRY_STATE_ID);
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
