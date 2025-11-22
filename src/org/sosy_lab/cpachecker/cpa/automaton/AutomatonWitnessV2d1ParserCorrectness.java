// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckEndsAtNodes;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInformationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;

class AutomatonWitnessV2d1ParserCorrectness extends AutomatonWitnessV2d0ParserCorrectness {
  AutomatonWitnessV2d1ParserCorrectness(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCFA)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pCFA);
  }

  /**
   * Extends the function from parser for version 2.0 to handle transition invariants in addition to
   * the normal loop and location invariants. Currently, the validation does not make use of the
   * automaton structure but this opens possibility of creating a validation technique based on our
   * CPA analyses.
   *
   * @param entries from the witness
   * @return the transitions containing invariants and transition invariants
   * @throws InterruptedException if the function is interrupted
   * @throws WitnessParseException if there is some problem parsing the witness
   */
  @Override
  protected ImmutableList.Builder<AutomatonTransition> createTransitionsFromEntries(
      List<AbstractEntry> entries) throws InterruptedException, WitnessParseException {
    ImmutableList.Builder<AutomatonTransition> transitions =
        super.createTransitionsFromEntries(entries);
    AstCfaRelation astCfaRelation = cfa.getAstCfaRelation();
    SetMultimap<Pair<Integer, OptionalInt>, Pair<String, String>> lineToSeenInvariants =
        HashMultimap.create();

    for (AbstractEntry entry : entries) {
      if (entry instanceof InvariantSetEntry invariantSetEntry) {
        for (AbstractInformationRecord entryElement : invariantSetEntry.content) {
          if (entryElement instanceof InvariantEntry invariantEntry) {
            Optional<String> resultFunction =
                Optional.ofNullable(invariantEntry.getLocation().getFunction());
            String invariantString = invariantEntry.getValue();
            Integer line = invariantEntry.getLocation().getLine();
            OptionalInt column = invariantEntry.getLocation().getColumn();
            Pair<Integer, OptionalInt> position = Pair.of(line, column);
            String invariantType = invariantEntry.getType();

            // Parsing is expensive for long invariants, we therefore try to reduce it
            Pair<String, String> lookupKey = Pair.of(resultFunction.orElseThrow(), invariantString);

            if (lineToSeenInvariants.get(position).contains(lookupKey)) {
              continue;
            } else {
              lineToSeenInvariants.get(position).add(lookupKey);
            }

            if (invariantType.equals(
                InvariantRecordType.TRANSITION_LOCATION_INVARIANT.getKeyword())) {
              throw new WitnessParseException(
                  "Transition invariants on locations are not yet supported.");
            }

            // The parsing of normal invariants is done in the parser of 2.0 which is called in the
            // beginning of this method
            if (invariantType.equals(InvariantRecordType.TRANSITION_LOOP_INVARIANT.getKeyword())) {
              ExpressionTree<AExpression> transitionInvariant =
                  transformer.parseInvariantEntry(invariantEntry);

              Optional<IterationElement> optionalIterationStructure =
                  astCfaRelation.getIterationStructureFollowingColumnAtTheSameLine(column, line);

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
                      .withCandidateTransitionInvariants(transitionInvariant)
                      .build());
            }
          }
        }
      }
    }
    return transitions;
  }
}
