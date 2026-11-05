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
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils.InvalidYAMLWitnessException;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.util.ast.ASTElement;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.ast.AstUtils.BoundaryNodesComputationFailed;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.TerminationArgumentsToWitnessUtils;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.WitnessInvariantType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInformationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionContractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;

public class InvariantExchangeFormatTransformer {

  private final CParser cparser;
  private final ParserTools parserTools;
  private final CFA cfa;
  private final LogManager logger;

  /** The arguments of \at(e, AnyPrev), where e is the first group. */
  private static final Pattern AT_ANY_PREV_ARGUMENTS =
      Pattern.compile("(.*),\\s*AnyPrev\\s*", Pattern.DOTALL);

  private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_]\\w*");

  private static final String EXPRESSION_VARIABLE_PREFIX = "__expression_";

  /**
   * The variables for the values of the expressions in \at(e, AnyPrev), by their functions and
   * expressions, such that an expression has the same variable in all invariants.
   */
  private final Map<String, CSimpleDeclaration> expressionVariables = new HashMap<>();

  private int expressionVariableCount = 0;

  public InvariantExchangeFormatTransformer(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCFA)
      throws InvalidConfigurationException {
    cparser =
        CParserUtils.createWitnessExpressionParser(
            pConfig, pCFA.getMachineModel(), pShutdownNotifier);
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
    PreviousValues previousValues =
        new PreviousValues(pInvariantEntry.getValue(), ImmutableMap.of(), ImmutableMap.of());
    if (WitnessInvariantType.of(InvariantRecordType.fromKeyword(pInvariantEntry.getType()))
        .map(WitnessInvariantType::isTransitionInvariant)
        .orElse(false)) {
      previousValues = encodePreviousValues(pInvariantEntry);
    }

    Deque<String> callStack = new ArrayDeque<>();
    callStack.push(pInvariantEntry.getLocation().getFunction());

    // The fresh variables are not declared in the CFA, so they are added to the scope
    Scope scope =
        switch (cfa.getLanguage()) {
          case C -> {
            CProgramScope programScope = new CProgramScope(cfa, logger);
            yield variablesInScope(pInvariantEntry)
                .map(programScope::withVariablesInScope)
                .orElse(programScope)
                .withAdditionalDeclarations(previousValues.variables().keySet());
          }
          default -> DummyScope.getInstance();
        };

    return new ParsedInvariant(
        pInvariantEntry,
        createExpressionTreeFromString(
            resultFunction, previousValues.invariant(), line, callStack, scope),
        previousValues.variables(),
        previousValues.expressions());
  }

  /**
   * The variables of the input program in scope where the invariant holds, if they are known. Their
   * names in the input program are distinct, although CPAchecker renamed some of them, e.g., to
   * {@code i} and {@code i__1}.
   */
  private Optional<ImmutableSet<CSimpleDeclaration>> variablesInScope(
      InvariantEntry pInvariantEntry) {
    return nodeOfInvariant(pInvariantEntry)
        .flatMap(cfa.getAstCfaRelation()::getVariablesAndParametersInScope)
        .map(variables -> variables.filter(CSimpleDeclaration.class).toSet());
  }

  /**
   * The node where the invariant holds, at which the variables of the invariant are in scope, if it
   * is known. It belongs to the function of the invariant location.
   */
  private Optional<CFANode> nodeOfInvariant(InvariantEntry pInvariantEntry) {
    AstCfaRelation astCfaRelation = cfa.getAstCfaRelation();
    int line = pInvariantEntry.getLocation().getLine();
    OptionalInt column = pInvariantEntry.getLocation().getColumn();
    String function = pInvariantEntry.getLocation().getFunction();
    Optional<CFANode> node;
    if (InvariantRecordType.fromKeyword(pInvariantEntry.getType())
            == InvariantRecordType.LOOP_INVARIANT
        || InvariantRecordType.fromKeyword(pInvariantEntry.getType())
            == InvariantRecordType.TRANSITION_LOOP_INVARIANT) {
      // A node at the start of the loop body has the variables of the loop in scope
      node =
          astCfaRelation
              .getIterationStructureFollowingColumnAtTheSameLine(column, line)
              .flatMap(
                  loop -> {
                    try {
                      return loop.getNodesBetweenConditionAndBody().stream()
                          // Not a node in a function called at the start of the body
                          .filter(candidate -> candidate.getFunctionName().equals(function))
                          .findFirst();
                    } catch (BoundaryNodesComputationFailed e) {
                      return Optional.empty();
                    }
                  });
    } else {
      node =
          astCfaRelation
              .getTightestStatementForStarting(line, column)
              .flatMap(statement -> astCfaRelation.getNodeBeforeStatement(statement, function));
    }
    return node;
  }

  /**
   * The encoding of the previous values \at(e, AnyPrev) in a transition invariant by fresh
   * variables, since the C parser does not support them. For a variable x, the fresh variable is
   * x__TransInv_PREV. For another expression e, e.g., *p, there is also a variable __expression_N
   * for the value of e, and the fresh variable is __expression_N__TransInv_PREV.
   *
   * @param invariant the invariant with the fresh variables
   * @param variables the fresh variables mapped to the variables whose previous values they are
   * @param expressions the variables for the values of expressions mapped to these expressions
   */
  private record PreviousValues(
      String invariant,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> variables,
      ImmutableMap<CSimpleDeclaration, CExpression> expressions) {}

  private PreviousValues encodePreviousValues(InvariantEntry pInvariantEntry)
      throws InterruptedException {
    String invariant = pInvariantEntry.getValue();
    String function = pInvariantEntry.getLocation().getFunction();
    // As in the invariant, a name refers to the variable in scope, e.g., x to x__1
    CProgramScope programScope = new CProgramScope(cfa, logger);
    CProgramScope scope =
        nodeOfInvariant(pInvariantEntry)
            .map(
                node ->
                    TerminationArgumentsToWitnessUtils.getScopeAtNode(
                        programScope, node, cfa.getAstCfaRelation()))
            .orElseGet(() -> programScope.withFunctionScope(function));

    Map<CSimpleDeclaration, CSimpleDeclaration> variables = new LinkedHashMap<>();
    Map<CSimpleDeclaration, CExpression> expressions = new LinkedHashMap<>();
    StringBuilder result = new StringBuilder();
    int encodedUntil = 0;
    int start = invariant.indexOf(TransitionInvariantUtils.AT_PREFIX);
    while (start >= 0) {
      int opening = start + TransitionInvariantUtils.AT_PREFIX.length() - 1;
      int closing = indexOfClosingParenthesis(invariant, opening);
      if (closing < 0) {
        break;
      }
      Matcher arguments = AT_ANY_PREV_ARGUMENTS.matcher(invariant.substring(opening + 1, closing));
      if (arguments.matches()) {
        // Otherwise, \at stays in the invariant, which then cannot be parsed
        Optional<CSimpleDeclaration> previous =
            declarePreviousValue(
                removeOuterParentheses(arguments.group(1)),
                function,
                scope,
                variables,
                expressions);
        if (previous.isPresent()) {
          result.append(invariant, encodedUntil, start).append(previous.orElseThrow().getName());
          encodedUntil = closing + 1;
        }
      }
      start = invariant.indexOf(TransitionInvariantUtils.AT_PREFIX, closing + 1);
    }
    result.append(invariant.substring(encodedUntil));
    return new PreviousValues(
        result.toString().replace("\\", ""),
        ImmutableMap.copyOf(variables),
        ImmutableMap.copyOf(expressions));
  }

  /**
   * Returns the fresh variable for the previous value of the given variable or expression, see
   * {@link PreviousValues}, and adds it to the given maps. It is empty if the variable is not in
   * scope or the expression cannot be parsed.
   */
  private Optional<CSimpleDeclaration> declarePreviousValue(
      String pExpression,
      String pFunction,
      Scope pScope,
      Map<CSimpleDeclaration, CSimpleDeclaration> pVariables,
      Map<CSimpleDeclaration, CExpression> pExpressions)
      throws InterruptedException {
    CSimpleDeclaration current;
    String previousName;
    if (IDENTIFIER.matcher(pExpression).matches()) {
      current = pScope.lookupVariable(pExpression);
      if (current == null) {
        return Optional.empty();
      }
      previousName = pExpression + TransitionInvariantUtils.PREV_KEYWORD;
    } else {
      CExpression expression;
      try {
        if (!(CParserUtils.parseSingleStatement(pExpression + ";", cparser, pScope)
            instanceof CExpressionStatement statement)) {
          return Optional.empty();
        }
        expression = statement.getExpression();
      } catch (InvalidAutomatonException e) {
        return Optional.empty();
      }
      String key = pFunction + "::" + expression.toQualifiedASTString();
      current = expressionVariables.get(key);
      if (current == null) {
        String name;
        do {
          name = EXPRESSION_VARIABLE_PREFIX + expressionVariableCount++;
        } while (pScope.variableNameInUse(name));
        current = declareFreshVariable(name, expression.getExpressionType(), pFunction);
        expressionVariables.put(key, current);
      }
      pExpressions.put(current, expression);
      previousName = current.getName() + TransitionInvariantUtils.PREV_KEYWORD;
    }
    for (CSimpleDeclaration previous : pVariables.keySet()) {
      if (previous.getName().equals(previousName)) {
        return Optional.of(previous);
      }
    }
    CSimpleDeclaration previous = declareFreshVariable(previousName, current.getType(), pFunction);
    pVariables.put(previous, current);
    return Optional.of(previous);
  }

  private CVariableDeclaration declareFreshVariable(String pName, CType pType, String pFunction) {
    return new CVariableDeclaration(
        cfa.getMainFunction().getFileLocation(),
        false,
        CStorageClass.AUTO,
        pType,
        pName,
        pName,
        // The scope is not relevant as these variables are not in the original program
        pFunction + "::" + pName,
        null,
        /* pHasThreadLocalStorage= */ false);
  }

  /** Returns the index of the parenthesis closing the one at the given index, or -1. */
  private static int indexOfClosingParenthesis(String pString, int pOpening) {
    int depth = 0;
    for (int i = pOpening; i < pString.length(); i++) {
      if (pString.charAt(i) == '(') {
        depth++;
      } else if (pString.charAt(i) == ')') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    return -1;
  }

  /** Removes the parentheses around the whole expression, e.g., (*p) becomes *p. */
  private static String removeOuterParentheses(String pExpression) {
    String expression = pExpression.trim();
    while (expression.startsWith("(")
        && indexOfClosingParenthesis(expression, 0) == expression.length() - 1) {
      expression = expression.substring(1, expression.length() - 1).trim();
    }
    return expression;
  }

  /**
   * The content of the invariant sets of a correctness witness.
   *
   * @param invariants the invariants, with their values already parsed
   * @param functionContracts the function contracts, which are not parsed
   */
  public record ParsedInvariantSet(
      ImmutableList<ParsedInvariant> invariants,
      ImmutableList<FunctionContractEntry> functionContracts) {}

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

    for (AbstractEntry entry : pEntries) {
      if (!(entry instanceof InvariantSetEntry invariantSetEntry)) {
        throw new InvalidYAMLWitnessException(
            "Expected only invariant sets in a correctness witness, but found: " + entry);
      }
      for (AbstractInformationRecord entryElement : invariantSetEntry.content) {
        switch (entryElement) {
          case FunctionContractEntry functionContractEntry ->
              functionContracts.add(functionContractEntry);
          case InvariantEntry entryWithLocation -> {
            InvariantEntry invariantEntry = addFunctionToLocationIfNoneExists(entryWithLocation);
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

    return new ParsedInvariantSet(invariants.build(), functionContracts.build());
  }

  /**
   * Returns the invariant with the column and the function of its location taken from the program
   * where the witness omits them, which the format allows. For a loop invariant, they are those of
   * the loop statement at the line, otherwise those of the first statement at the line.
   */
  private InvariantEntry addFunctionToLocationIfNoneExists(InvariantEntry pInvariantEntry) {
    LocationRecord location = pInvariantEntry.getLocation();
    if (location.getFunction() != null) {
      return pInvariantEntry;
    }
    AstCfaRelation astCfaRelation = cfa.getAstCfaRelation();
    InvariantRecordType type = InvariantRecordType.fromKeyword(pInvariantEntry.getType());
    Optional<ASTElement> statement =
        type == InvariantRecordType.LOOP_INVARIANT
                || type == InvariantRecordType.TRANSITION_LOOP_INVARIANT
            ? astCfaRelation
                .getIterationStructureFollowingColumnAtTheSameLine(
                    location.getColumn(), location.getLine())
                .map(IterationElement::getCompleteElement)
            : astCfaRelation.getTightestStatementForStarting(
                location.getLine(), location.getColumn());
    if (statement.isEmpty()) {
      return pInvariantEntry;
    }
    String function = location.getFunction();
    if (function == null) {
      function =
          statement.orElseThrow().edges().stream()
              .map(edge -> edge.getPredecessor().getFunctionName())
              .findFirst()
              .orElse(null);
    }
    return new InvariantEntry(
        pInvariantEntry.getValue(),
        pInvariantEntry.getType(),
        pInvariantEntry.getFormat(),
        new LocationRecord(
            location.getFileName(),
            location.getLine(),
            location.getColumn().isPresent() ? location.getColumn().orElseThrow() : null,
            function));
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
      OptionalInt column = parsed.column();

      if (parsed.isTransitionInvariant()) {
        invariants.add(
            new TransitionInvariant(
                parsed.formula(),
                parsed.line(),
                column,
                parsed.function(),
                parsed.isLoopInvariant(),
                parsed.previousValueVariables(),
                parsed.expressionVariables()));
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
