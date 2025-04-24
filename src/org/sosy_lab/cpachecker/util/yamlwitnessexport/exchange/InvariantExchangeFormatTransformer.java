// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils.InvalidYAMLWitnessException;
import org.sosy_lab.cpachecker.util.ACSLParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInformationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LemmaEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LemmaSetEntry;

public class InvariantExchangeFormatTransformer {

  private final CParser cparser;
  private final ParserTools parserTools;
  private final CFA cfa;
  private final LogManager logger;

  public InvariantExchangeFormatTransformer(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCFA)
      throws InvalidConfigurationException {
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
    cfa = pCFA;
    logger = pLogger;
  }

  /**
   * Create an {@link ExpressionTree} from a given string.
   *
   * @param resultFunction The function in which the expression is contained
   * @param invariantString The string to parse
   * @param pLine The line of the expression in the original code
   * @param callStack The call stack at the time of the expression
   * @param pScope The scope in which the expression is contained
   * @return The parsed expression
   * @throws InterruptedException If the parsing is interrupted
   */
  public ExpressionTree<AExpression> createExpressionTreeFromString(
      Optional<String> resultFunction,
      String invariantString,
      Integer pLine,
      Deque<String> callStack,
      Scope pScope)
      throws InterruptedException {

    return CParserUtils.parseStatementsAsExpressionTree(
        ImmutableSet.of(invariantString),
        resultFunction,
        cparser,
        AutomatonWitnessV2ParserUtils.determineScopeForLine(
            resultFunction, callStack, pLine, pScope),
        parserTools);
  }

  /**
   * Create an {@link ExpressionTree} from a given string.
   *
   * <p>This method is only intended as a temporary workaround until a proper ACSL-parser has been
   * implemented. It should not be used anywhere outside the parsing of lemmas from a YAML * witness
   * for predicate abstraction!
   *
   * @param invariantString The string to parse
   * @param pScope The scope in which the expression is contained
   * @return The parsed expression
   */
  public ExpressionTree<AExpression> createACSLExpressionTreeFromString(
      String invariantString, CProgramScope pScope) {

    try {

      return ACSLParserUtils.parseACSLStatementAsExpressionTree(
          invariantString, cparser, pScope, parserTools);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Parse the invariant string given in an {@link InvariantEntry} into an {@link ExpressionTree}.
   *
   * @param pInvariantEntry The entry whose invariant should be parsed
   * @return The parsed invariant as a {@link ExpressionTree}
   */
  public ExpressionTree<AExpression> parseInvariantEntry(
      InvariantEntry pInvariantEntry, List<CDeclaration> lemmaDeclarations)
      throws InvalidYAMLWitnessException {
    String invariantString = pInvariantEntry.getValue();

    /*
     * We need the declarations from the witness file in the scope to correctly parse any invariants that reference the lemmas.
     * This is a quick fix that needs to be reimplemented properly!*/
    CProgramScope scope = new CProgramScope(cfa, logger);
    for (CDeclaration declaration : lemmaDeclarations) {
      scope.addDeclarationToScope(declaration);
    }

    return createACSLExpressionTreeFromString(invariantString, scope);
  }

  public ExpressionTree<AExpression> parseLemmaEntry(LemmaEntry pLemmaEntry, CProgramScope pScope)
      throws Exception {
    return ACSLParserUtils.parseACSLStatementAsExpressionTree(
        pLemmaEntry.value(), cparser, pScope, parserTools);
  }

  /**
   * Transform the {@link AbstractEntry} to a set of {@link Invariant}s. These are the internal data
   * structure which should be used inside CPAchecker.
   *
   * @param pEntries The entries to transform
   * @return The set of invariants
   */
  public Set<Invariant> generateInvariantsFromEntries(List<AbstractEntry> pEntries)
      throws InvalidYAMLWitnessException {
    ImmutableSet.Builder<Invariant> invariants = new ImmutableSet.Builder<>();

    SetMultimap<Pair<Integer, Integer>, String> lineToSeenInvariants = HashMultimap.create();

    List<LemmaSetEntry> lemmaSetEntries = new ArrayList<>();
    for (AbstractEntry entry : pEntries) {
      if (entry instanceof LemmaSetEntry lemmaSetEntry) {
        lemmaSetEntries.add(lemmaSetEntry);
      }
    }

    List<String> declarationStrings =
        AutomatonWitnessV2ParserUtils.parseDeclarationsFromFile(lemmaSetEntries).asList();
    List<CDeclaration> lemmaDeclarations = ACSLParserUtils.parseDeclarations(declarationStrings);

    for (AbstractEntry entry : pEntries) {
      if (entry instanceof InvariantSetEntry invariantSetEntry) {
        for (AbstractInformationRecord entryElement : invariantSetEntry.content) {
          if (entryElement instanceof InvariantEntry invariantEntry) {
            int line = invariantEntry.getLocation().getLine();
            int column = invariantEntry.getLocation().getColumn();
            Pair<Integer, Integer> cacheLookupKey = Pair.of(line, column);
            String invariantString = invariantEntry.getValue();

            // Parsing is expensive, therefore, cache everything we can
            if (lineToSeenInvariants.get(cacheLookupKey).contains(invariantString)) {
              continue;
            }

            ExpressionTree<AExpression> invariant =
                parseInvariantEntry(invariantEntry, lemmaDeclarations);

            invariants.add(
                new Invariant(
                    invariant,
                    line,
                    column,
                    invariantEntry.getLocation().getFunction(),
                    invariantEntry
                        .getType()
                        .equals(InvariantRecordType.LOOP_INVARIANT.getKeyword())));

            lineToSeenInvariants.get(cacheLookupKey).add(invariantString);
          }
        }
      }
    }
    return invariants.build();
  }
}
