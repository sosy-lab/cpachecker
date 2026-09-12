// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckCoversColumnAndLine;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckEndsAtNodes;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.WitnessInvariantKind;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessVersion;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.InvariantExchangeFormatTransformer.ParsedInvariantSet;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.ParsedInvariant;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;

/**
 * Parser building a witness automaton from a correctness witness in one of the YAML witness format
 * versions 2.0, 2.1 or 2.2.
 *
 * <p>The versions differ only in which kinds of invariants they may contain. Instead of modelling
 * each version with its own subclass, this parser asks the {@link YAMLWitnessVersion} it was
 * created for whether a given kind may be used. This keeps the shared logic in a single place and
 * makes the version-specific behavior explicit at the point where it matters.
 *
 * <p>The resulting automaton contains a single state and each invariant is marked as such on a
 * transition which starts at that state and returns to it. Each transition is only passed if the
 * locations match.
 *
 * <p>More information about the witness format can be found in the <a
 * href="https://gitlab.com/sosy-lab/benchmarking/sv-witnesses/-/blob/main/user-guide/Witness-Format.md">Witnesses
 * Format</a>.
 */
class AutomatonWitnessCorrectnessV2Parser extends AutomatonWitnessV2ParserCommon {

  private static final String ENTRY_STATE_ID = "singleState";

  AutomatonWitnessCorrectnessV2Parser(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCFA,
      YAMLWitnessVersion pVersion)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pCFA, pVersion);
  }

  /**
   * Create an automaton from a correctness witness.
   *
   * @param pEntries the entries of the correctness witness
   * @return an automaton for the correctness witness
   * @throws InterruptedException if the function is interrupted
   * @throws WitnessParseException if there is some problem parsing the witness
   */
  Automaton createCorrectnessAutomatonFromEntries(List<AbstractEntry> pEntries)
      throws InterruptedException, InvalidConfigurationException {
    ParsedInvariantSet contents = transformer.parseInvariantSets(pEntries);

    if (!contents.functionContracts().isEmpty()) {
      logger.logf(
          Level.WARNING,
          "Ignoring %d function contract(s) of the correctness witness, "
              + "CPAchecker cannot validate function contracts yet. "
              + "The witness is therefore only validated partially.",
          contents.functionContracts().size());
    }

    List<AutomatonInternalState> automatonStates =
        ImmutableList.of(
            new AutomatonInternalState(
                ENTRY_STATE_ID, createTransitions(contents), false, true, true),
            AutomatonInternalState.ERROR);

    Automaton automaton;
    try {
      automaton =
          new Automaton(
              contents.uuid().orElse("No Loop Invariant Present"),
              ImmutableMap.of(),
              automatonStates,
              ENTRY_STATE_ID);
    } catch (InvalidAutomatonException e) {
      throw new WitnessParseException(
          "The witness automaton generated from the provided Witness V2 is invalid!", e);
    }

    automaton =
        getInvariantsSpecAutomaton().build(automaton, config, logger, shutdownNotifier, cfa);

    dumpAutomatonIfRequested(automaton);

    return automaton;
  }

  /**
   * Create the transitions which check the invariants of the witness.
   *
   * @param pContents the contents of the invariant sets of the witness
   * @return the transitions of the single state of the automaton
   * @throws WitnessParseException if the witness contains an invariant which cannot be handled
   */
  private ImmutableList<AutomatonTransition> createTransitions(ParsedInvariantSet pContents)
      throws WitnessParseException {
    ImmutableList.Builder<AutomatonTransition> transitions = ImmutableList.builder();

    for (ParsedInvariant invariant : pContents.invariants()) {
      if (invariant.formula().equals(ExpressionTrees.getTrue())) {
        continue;
      }

      WitnessInvariantKind kind =
          WitnessInvariantKind.of(invariant.type())
              .orElseThrow(
                  () ->
                      new WitnessParseException(
                          "The witness contains an invariant of the unsupported type "
                              + invariant.entry().getType()
                              + "!"));
      if (!version.supportedInvariantKinds().contains(kind)) {
        throw new WitnessParseException(
            "A witness in version "
                + version
                + " cannot contain invariants of the kind "
                + kind
                + "!");
      }

      // Where in the program the invariant has to hold
      Optional<AutomatonBoolExpr> location =
          switch (kind) {
            case LOOP_INVARIANT, LOOP_TRANSITION_INVARIANT -> loopHeadCheck(invariant);
            case LOCATION_INVARIANT -> statementCheck(invariant);
            case LOCATION_TRANSITION_INVARIANT ->
                throw new WitnessParseException(
                    "Transition invariants on locations are not yet supported.");
            case FUNCTION_CONTRACT ->
                throw new AssertionError("Contracts are not part of the invariants");
          };
      if (location.isEmpty()) {
        continue;
      }

      if (kind == WitnessInvariantKind.LOOP_TRANSITION_INVARIANT) {
        // The validation currently does not make use of the automaton structure, but this opens
        // the possibility of creating a validation technique based on our CPA analyses.
        transitions.add(
            new AutomatonTransition.Builder(location.orElseThrow(), ENTRY_STATE_ID)
                .withCandidateTransitionInvariants(invariant.formula())
                .build());
      } else {
        addInvariantTransitions(transitions, location.orElseThrow(), invariant);
      }
    }

    return transitions.build();
  }

  /**
   * Add the transition which makes the invariant known at its location, and, if requested, the
   * transition which reports a violation of the invariant.
   */
  private void addInvariantTransitions(
      ImmutableList.Builder<AutomatonTransition> pTransitions,
      AutomatonBoolExpr pPassTransitionWhenCheckSucceeds,
      ParsedInvariant pInvariant)
      throws WitnessParseException {
    CExpression invariantAsCExpression;
    CExpression negatedInvariantAsCExpression;
    try {
      invariantAsCExpression =
          pInvariant.formula().accept(new ToCExpressionVisitor(cfa.getMachineModel(), logger));
      negatedInvariantAsCExpression =
          new CBinaryExpressionBuilder(cfa.getMachineModel(), logger)
              .negateExpressionAndSimplify(invariantAsCExpression);
    } catch (UnrecognizedCodeException e) {
      throw new WitnessParseException(
          "The invariant could not be parsed to a C expression: "
              + Ascii.truncate(pInvariant.value(), 100, "..."),
          e);
    }

    // Add the transition for where we already know that the invariant is valid at this location
    pTransitions.add(
        new AutomatonTransition.Builder(pPassTransitionWhenCheckSucceeds, ENTRY_STATE_ID)
            .withCandidateInvariants(pInvariant.formula())
            .withAssumptions(ImmutableList.of(invariantAsCExpression))
            .build());

    if (checkInvariantsHoldForEveryPath) {
      // Add a transition which checks if the invariant holds at this location and goes to an error
      // state if it does not
      pTransitions.add(
          new AutomatonTransition.Builder(
                  pPassTransitionWhenCheckSucceeds, AutomatonInternalState.ERROR)
              .withTargetInformation(new StringExpression("invalid invariant"))
              .withAssumptions(ImmutableList.of(negatedInvariantAsCExpression))
              .build());
    }
  }

  /**
   * The check which passes when the head of the loop the invariant belongs to is reached, or an
   * empty Optional if the location of the invariant does not point at a loop of the program.
   */
  private Optional<AutomatonBoolExpr> loopHeadCheck(ParsedInvariant pInvariant) {
    Optional<AutomatonBoolExpr> check =
        cfa.getAstCfaRelation()
            .getIterationStructureFollowingColumnAtTheSameLine(
                pInvariant.column(), pInvariant.line())
            .flatMap(IterationElement::getLoopHead)
            .map(loopHead -> new CheckEndsAtNodes(ImmutableSet.of(loopHead)));
    if (check.isEmpty()) {
      logUnmatchedInvariant(pInvariant, "there is no loop at this location");
    }
    return check;
  }

  /**
   * The check which passes when the statement the invariant belongs to is reached, or an empty
   * Optional if the location of the invariant does not point at a statement of the program.
   */
  private Optional<AutomatonBoolExpr> statementCheck(ParsedInvariant pInvariant) {
    int line = pInvariant.line();
    Optional<AutomatonBoolExpr> check =
        cfa.getAstCfaRelation()
            .getTightestStatementForStarting(line, pInvariant.column())
            .map(
                statement ->
                    new CheckCoversColumnAndLine(
                        statement.location().getStartColumnInLine(), line));
    if (check.isEmpty()) {
      logUnmatchedInvariant(pInvariant, "there is no statement at this location");
    }
    return check;
  }

  /**
   * Report an invariant which cannot be matched to the program. The invariant is ignored, so the
   * witness is only validated partially and this must be visible to the user.
   */
  private void logUnmatchedInvariant(ParsedInvariant pInvariant, String pReason) {
    logger.logf(
        Level.WARNING,
        "Ignoring the %s at line %d of the correctness witness, %s. "
            + "The witness is therefore only validated partially.",
        pInvariant.entry().getType(),
        pInvariant.line(),
        pReason);
  }
}
