// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
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
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils.InvalidYAMLWitnessException;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInformationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionContractEntry;
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
   * Parse one invariant of a correctness witness.
   *
   * @param pInvariantEntry The entry which should be parsed
   * @return the parsed invariant
   * @throws InterruptedException If the parsing is interrupted
   */
  private ParsedInvariant parseSingleEntry(InvariantEntry pInvariantEntry)
      throws InterruptedException {
    Integer line = pInvariantEntry.getLocation().getLine();
    Optional<String> resultFunction =
        Optional.ofNullable(pInvariantEntry.getLocation().getFunction());
    String invariantString = pInvariantEntry.getValue();
    ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> previousValueVariables = ImmutableMap.of();
    if (InvariantRecordType.fromKeyword(pInvariantEntry.getType())
        == InvariantRecordType.TRANSITION_LOOP_INVARIANT) {
      invariantString = replacePrevKeywordWithFreshVariables(pInvariantEntry);
      // This adds declarations of the fresh variables to the CFA and must happen only once
      previousValueVariables = registerThePrevVariables(pInvariantEntry);
    }

    Deque<String> callStack = new ArrayDeque<>();
    callStack.push(pInvariantEntry.getLocation().getFunction());

    Scope scope =
        switch (cfa.getLanguage()) {
          case C -> new CProgramScope(cfa, logger);
          default -> DummyScope.getInstance();
        };

    return new ParsedInvariant(
        pInvariantEntry,
        createExpressionTreeFromString(resultFunction, invariantString, line, callStack, scope),
        previousValueVariables);
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

  /**
   * The content of the invariant sets of a correctness witness.
   *
   * @param invariants the invariants, with their values already parsed
   * @param functionContracts the function contracts, which are not parsed
   * @param uuid the uuid of the last invariant set, if there is any
   */
  public record ParsedInvariantSet(
      ImmutableList<ParsedInvariant> invariants,
      ImmutableList<FunctionContractEntry> functionContracts,
      Optional<String> uuid) {}

  /**
   * Parse the invariant sets of a correctness witness.
   *
   * <p>Each distinct invariant is parsed exactly once, since parsing is expensive for long
   * invariants.
   *
   * @param pEntries The entries to parse
   * @return the content of the invariant sets
   * @throws InterruptedException If the parsing is interrupted
   * @throws InvalidYAMLWitnessException if the entries do not describe a correctness witness
   */
  public ParsedInvariantSet parseInvariantSets(List<AbstractEntry> pEntries)
      throws InterruptedException, InvalidYAMLWitnessException {
    ImmutableList.Builder<ParsedInvariant> invariants = ImmutableList.builder();
    ImmutableList.Builder<FunctionContractEntry> functionContracts = ImmutableList.builder();
    Set<InvariantEntry> alreadyParsed = new HashSet<>();
    Optional<String> uuid = Optional.empty();

    for (AbstractEntry entry : pEntries) {
      if (!(entry instanceof InvariantSetEntry invariantSetEntry)) {
        throw new InvalidYAMLWitnessException(
            "Expected only invariant sets in a correctness witness, but found: " + entry);
      }
      uuid = Optional.ofNullable(invariantSetEntry.metadata.getUuid());
      for (AbstractInformationRecord entryElement : invariantSetEntry.content) {
        switch (entryElement) {
          case FunctionContractEntry functionContractEntry ->
              functionContracts.add(functionContractEntry);
          case InvariantEntry invariantEntry -> {
            if (invariantEntry.getLocation().getFunction() == null) {
              throw new InvalidYAMLWitnessException(
                  "Invariant without a function in its location: " + invariantEntry);
            }
            if (alreadyParsed.add(invariantEntry)) {
              invariants.add(parseSingleEntry(invariantEntry));
            }
          }
          default ->
              throw new InvalidYAMLWitnessException(
                  "Unknown element in an invariant set: " + entryElement);
        }
      }
    }

    return new ParsedInvariantSet(invariants.build(), functionContracts.build(), uuid);
  }

  /**
   * Transform the {@link AbstractEntry} to a set of {@link Invariant}s. These are the internal data
   * structure which should be used inside CPAchecker.
   *
   * @param pEntries The entries to transform
   * @return The set of invariants
   */
  public Set<Invariant> generateInvariantsFromEntries(List<AbstractEntry> pEntries)
      throws InterruptedException, InvalidYAMLWitnessException {
    ImmutableSet.Builder<Invariant> invariants = new ImmutableSet.Builder<>();

    for (ParsedInvariant parsed : parseInvariantSets(pEntries).invariants()) {
      if (parsed.column().isEmpty()) {
        // The consumers of an Invariant look up a CFA node for its line and column
        logger.logf(
            Level.WARNING, "Ignoring invariant without column information: %s", parsed.entry());
        continue;
      }
      int column = parsed.column().orElseThrow();

      if (parsed.hasPreviousValueVariables()) {
        invariants.add(
            new TransitionInvariant(
                parsed.formula(),
                parsed.line(),
                column,
                parsed.function(),
                parsed.isLoopInvariant(),
                parsed.previousValueVariables()));
      } else {
        invariants.add(
            new Invariant(
                parsed.formula(),
                parsed.line(),
                column,
                parsed.function(),
                parsed.isLoopInvariant()));
      }
    }
    return invariants.build();
  }
}
