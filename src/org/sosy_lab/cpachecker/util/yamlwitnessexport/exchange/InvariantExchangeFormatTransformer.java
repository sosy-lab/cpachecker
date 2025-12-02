// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;
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

public class InvariantExchangeFormatTransformer {

  private final CParser cparser;
  private final ParserTools parserTools;
  private final CFA cfa;
  private final LogManager logger;

  private static final Pattern AT_ANY_PREV_PATTERN =
      Pattern.compile("\\\\at\\(([^)]+),\\s*AnyPrev\\s*\\)");
  private static final int PREV_VARS_GROUP_INDEX = 1;

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
    if (pInvariantEntry
        .getType()
        .equals(InvariantRecordType.TRANSITION_LOOP_INVARIANT.getKeyword())) {
      invariantString = replacePrevKeywordWithFreshVariables(pInvariantEntry);
      registerThePrevVariables(pInvariantEntry);
    }

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
   * In case the witness is termination witness, it may contain \at(x, AnyPrev) keyword which is not
   * parsed. We have to encode this keyword into the names of the variables.
   *
   * @param pInvariantEntry transition invariant string
   * @return Invariant string with \at(x, AnyPrev) encoded as __PREV suffix
   */
  private String replacePrevKeywordWithFreshVariables(InvariantEntry pInvariantEntry) {
    String invariantString = pInvariantEntry.getValue();
    if (!isTransitionInvariant(pInvariantEntry)) {
      return invariantString;
    }
    Matcher matcher = AT_ANY_PREV_PATTERN.matcher(invariantString);
    StringBuilder result = new StringBuilder();

    while (matcher.find()) {
      String variable = matcher.group(PREV_VARS_GROUP_INDEX);
      matcher.appendReplacement(result, "__CPACHECKER_" + variable + "__PREV");
    }
    matcher.appendTail(result);
    invariantString = result.toString().replace("\\", "");

    return invariantString;
  }

  /**
   * In case the witness is termination witness, it may contain x__PREV variables. These variables
   * need to be registered in the scope. We add arbitrary edges into the head of the main with the
   * declarations of these variables in CFA.
   *
   * @param pInvariantEntry the invariant entry
   */
  public ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> registerThePrevVariables(
      InvariantEntry pInvariantEntry) {
    String invariantString = pInvariantEntry.getValue();
    Matcher matcher = AT_ANY_PREV_PATTERN.matcher(invariantString);
    ImmutableMap.Builder<CSimpleDeclaration, CSimpleDeclaration> mapPrevToCurr =
        ImmutableMap.builder();

    Scope scope = new CProgramScope(cfa, logger);
    Set<String> alreadyDeclaredVariables = new HashSet<>();

    while (matcher.find()) {
      String prevVariable = matcher.group(PREV_VARS_GROUP_INDEX);
      CSimpleDeclaration currDeclaration = scope.lookupVariable(prevVariable);
      if (currDeclaration == null) {
        continue;
      }
      prevVariable = "__CPACHECKER_" + prevVariable + "__PREV";

      // We want to declare each PREV variable only once
      if (alreadyDeclaredVariables.contains(prevVariable)) {
        continue;
      }
      alreadyDeclaredVariables.add(prevVariable);

      CDeclaration prevDeclaration =
          new CVariableDeclaration(
              cfa.getMainFunction().getFileLocation(),
              false,
              CStorageClass.AUTO,
              currDeclaration.getType(),
              prevVariable,
              prevVariable,
              // The scope is not relevant as these variables are not in the original program
              "main::" + prevVariable,
              null);
      // TODO: Add also the original variable into the scope?
      cfa.getMainFunction().addOutOfScopeVariables(Collections.singleton(prevDeclaration));
      cfa.getMainFunction()
          .addLeavingEdge(
              new CDeclarationEdge(
                  currDeclaration.getType() + " " + prevVariable + ";",
                  cfa.getMainFunction().getFileLocation(),
                  cfa.getMainFunction(),
                  CFANode.newDummyCFANode(),
                  prevDeclaration));
      mapPrevToCurr.put(prevDeclaration, currDeclaration);
    }
    return mapPrevToCurr.buildOrThrow();
  }

  private boolean isLoopInvariant(InvariantEntry pInvariantEntry) {
    if (pInvariantEntry.getType().equals(InvariantRecordType.LOOP_INVARIANT.getKeyword())
        || pInvariantEntry
            .getType()
            .equals(InvariantRecordType.TRANSITION_LOOP_INVARIANT.getKeyword())) {
      return true;
    }
    return false;
  }

  private boolean isTransitionInvariant(InvariantEntry pInvariantEntry) {
    return pInvariantEntry
        .getType()
        .equals(InvariantRecordType.TRANSITION_LOOP_INVARIANT.getKeyword());
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
    ImmutableSet.Builder<Invariant> invariants = new ImmutableSet.Builder<>();

    SetMultimap<Pair<Integer, Integer>, String> lineToSeenInvariants = HashMultimap.create();

    for (AbstractEntry entry : pEntries) {
      if (entry instanceof InvariantSetEntry invariantSetEntry) {
        for (AbstractInformationRecord entryElement : invariantSetEntry.content) {
          if (entryElement instanceof InvariantEntry invariantEntry) {
            Integer line = invariantEntry.getLocation().getLine();
            Integer column = invariantEntry.getLocation().getColumn().orElseThrow();
            Pair<Integer, Integer> cacheLookupKey = Pair.of(line, column);
            String invariantString = invariantEntry.getValue();

            // Parsing is expensive, therefore, cache everything we can
            if (lineToSeenInvariants.get(cacheLookupKey).contains(invariantString)) {
              continue;
            }

            ExpressionTree<AExpression> invariant = parseInvariantEntry(invariantEntry);

            if (isTransitionInvariant(invariantEntry)) {
              invariants.add(
                  new TransitionInvariant(
                      invariant,
                      line,
                      column,
                      invariantEntry.getLocation().getFunction(),
                      isLoopInvariant(invariantEntry),
                      registerThePrevVariables(invariantEntry)));
            } else {
              invariants.add(
                  new Invariant(
                      invariant,
                      line,
                      column,
                      invariantEntry.getLocation().getFunction(),
                      isLoopInvariant(invariantEntry)));
            }

            lineToSeenInvariants.get(cacheLookupKey).add(invariantString);
          }
        }
      }
    }
    return invariants.build();
  }
}
