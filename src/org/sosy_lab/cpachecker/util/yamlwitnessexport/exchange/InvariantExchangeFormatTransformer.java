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
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
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
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;

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
   * Parse the invariant string given in an {@link InvariantEntry} into an {@link ExpressionTree}.
   *
   * @param pInvariantEntry The entry whose invariant should be parsed
   * @return The parsed invariant as a {@link ExpressionTree}
   * @throws InterruptedException If the parsing is interrupted
   */
  public ExpressionTree<AExpression> parseInvariantEntry(InvariantEntry pInvariantEntry)
      throws InterruptedException {
    Integer line = pInvariantEntry.getLocation().getLine();
    Optional<String> resultFunction =
        Optional.ofNullable(pInvariantEntry.getLocation().getFunction());
    String invariantString = pInvariantEntry.getValue();

    Deque<String> callStack = new ArrayDeque<>();
    callStack.push(pInvariantEntry.getLocation().getFunction());

    Scope scope =
        switch (cfa.getLanguage()) {
          case C -> new CProgramScope(cfa, logger);
          default -> DummyScope.getInstance();
        };

    return createExpressionTreeFromString(resultFunction, invariantString, line, callStack, scope);
  }

  /**
   * Transform the {@link AbstractEntry} to a set of {@link Invariant}s. These are the internal data
   * structure which should be used inside CPAchecker.
   *
   * @param pEntries The entries to transform
   * @return The set of invariants
   */
  public Set<Invariant> generateInvariantsFromEntries(List<AbstractEntry> pEntries)
      throws InterruptedException {
    Set<Invariant> invariants = new HashSet<>();

    SetMultimap<Integer, String> lineToSeenInvariants = HashMultimap.create();

    for (AbstractEntry entry : pEntries) {
      if (entry instanceof InvariantSetEntry invariantSetEntry) {
        for (InvariantEntry invariantEntry : invariantSetEntry.content) {
          Integer line = invariantEntry.getLocation().getLine();
          String invariantString = invariantEntry.getValue();

          // Parsing is expensive, therefore cache everything we can
          if (lineToSeenInvariants.get(line).contains(invariantString)) {
            continue;
          }

          ExpressionTree<AExpression> invariant = parseInvariantEntry(invariantEntry);

          // This is the position at which the invariant should be valid.
          FileLocation loc =
              new FileLocation(
                  Path.of(invariantEntry.getLocation().getFileName()),
                  // dummy offset is ok because not used for matching invariants to their position
                  -1,
                  // length 0 because invariants only refers to a particular point in the program
                  0,
                  line,
                  line,
                  invariantEntry.getLocation().getColumn(),
                  invariantEntry.getLocation().getColumn());
          invariants.add(
              new Invariant(
                  invariant,
                  loc,
                  invariantEntry
                      .getType()
                      .equals(InvariantRecordType.LOOP_INVARIANT.getKeyword())));

          lineToSeenInvariants.get(line).add(invariantString);
        }
      }
    }
    return invariants;
  }
}