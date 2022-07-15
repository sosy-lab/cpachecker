// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.CSourceOriginMapping;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Simplifier;

public class CParserUtils {

  private static final String CPACHECKER_TMP_PREFIX = "__CPAchecker_TMP";

  public static CStatement parseSingleStatement(String pSource, CParser parser, Scope scope)
      throws InvalidAutomatonException, InterruptedException {
    return parse(addFunctionDeclaration(pSource), parser, scope);
  }

  public static List<CStatement> parseListOfStatements(String pSource, CParser parser, Scope scope)
      throws InvalidAutomatonException, InterruptedException {
    return parseBlockOfStatements(addFunctionDeclaration(pSource), parser, scope);
  }

  public static List<AExpression> convertStatementsToAssumptions(
      Iterable<CStatement> assumptions, MachineModel machineModel, LogManager logger)
      throws InvalidAutomatonException {
    ImmutableList.Builder<AExpression> result = ImmutableList.builder();
    CBinaryExpressionBuilder expressionBuilder = new CBinaryExpressionBuilder(machineModel, logger);
    for (CStatement statement : assumptions) {

      if (statement instanceof CAssignment) {
        CAssignment assignment = (CAssignment) statement;

        if (assignment.getRightHandSide() instanceof CExpression) {

          CExpression expression = (CExpression) assignment.getRightHandSide();
          CBinaryExpression assumeExp =
              expressionBuilder.buildBinaryExpressionUnchecked(
                  assignment.getLeftHandSide(),
                  expression,
                  CBinaryExpression.BinaryOperator.EQUALS);

          result.add(assumeExp);
        } else if (assignment.getRightHandSide() instanceof CFunctionCall) {
          // TODO FunctionCalls, ExpressionStatements etc
          throw new InvalidAutomatonException(
              "Function call '"
                  + assignment.getRightHandSide().toASTString()
                  + "' is not supported in automaton assumption");
        }
      } else if (statement instanceof CExpressionStatement) {
        result.add(((CExpressionStatement) statement).getExpression());
      } else {
        throw new InvalidAutomatonException(
            "Statement '" + statement.toASTString() + "' is not supported in automaton assumption");
      }
    }
    return result.build();
  }

  /**
   * Surrounds the argument with a function declaration. This is necessary so the string can be
   * parsed by the CDT parser.
   *
   * @param pBody the body of the function
   * @return "void test() { " + body + ";}";
   */
  private static String addFunctionDeclaration(String pBody) {
    if (pBody.trim().endsWith(";")) {
      return "void test() { " + pBody + "}";
    } else {
      return "void test() { " + pBody + ";}";
    }
  }

  /**
   * Parse the content of a file into an AST with the Eclipse CDT parser. If an error occurs, the
   * program is halted.
   *
   * @param code The C code to parse.
   * @param parser The parser to use
   * @param scope the scope to use
   * @return The AST.
   */
  private static CStatement parse(String code, CParser parser, Scope scope)
      throws InvalidAutomatonException, InterruptedException {
    try {
      CAstNode statement = parser.parseSingleStatement(code, scope);
      if (!(statement instanceof CStatement)) {
        throw new InvalidAutomatonException("Not a valid statement: " + statement.toASTString());
      }
      return (CStatement) statement;
    } catch (ParserException e) {
      throw new InvalidAutomatonException(
          "Error during parsing C code \"" + code + "\": " + e.getMessage());
    }
  }

  /**
   * Parse the assumption of a automaton, which are C assignments, return statements or function
   * calls, into a list of CStatements with the Eclipse CDT parser. If an error occurs, an empty
   * list will be returned, and the error will be logged.
   *
   * @param code The C code to parse.
   * @return The AST.
   */
  private static List<CStatement> parseBlockOfStatements(String code, CParser parser, Scope scope)
      throws InvalidAutomatonException, InterruptedException {
    List<CAstNode> statements;
    try {
      statements = parser.parseStatements(code, scope);
    } catch (CParserException e) {
      throw new InvalidAutomatonException("Code cannot be parsed: <" + code + ">", e);
    }

    for (CAstNode statement : statements) {
      if (!(statement instanceof CStatement)) {
        throw new InvalidAutomatonException(
            "Code in assumption: <" + statement.toASTString() + "> is not a valid assumption.");
      }
    }

    return transformedImmutableListCopy(statements, statement -> (CStatement) statement);
  }

  /**
   * Parses the given set of strings as C statements.
   *
   * @param pStatements the code to be parsed as C statements.
   * @param pResultFunction the target function of {@literal "\result"} expressions.
   * @param pCParser the C parser to be used.
   * @param pScope the scope to interpret variables in.
   * @param pParserTools the auxiliary tools to be used for parsing.
   * @return a collection of C statements.
   * @throws InvalidAutomatonException if the input strings cannot be interpreted as C statements.
   */
  public static Collection<CStatement> parseStatements(
      Set<String> pStatements,
      Optional<String> pResultFunction,
      CParser pCParser,
      Scope pScope,
      ParserTools pParserTools)
      throws InvalidAutomatonException, InterruptedException {
    if (!pStatements.isEmpty()) {

      Set<CStatement> result = new HashSet<>();
      for (String assumeCode : pStatements) {
        Collection<CStatement> statements =
            parseAsCStatements(assumeCode, pResultFunction, pCParser, pScope, pParserTools);
        result.addAll(
            removeDuplicates(
                from(statements).transform(CParserUtils::adjustCharAssignmentSignedness)));
      }
      return result;
    }
    return ImmutableSet.of();
  }

  private static Collection<CStatement> parseAsCStatements(
      String pCode,
      Optional<String> pResultFunction,
      CParser pCParser,
      Scope pScope,
      ParserTools pParserTools)
      throws InvalidAutomatonException, InterruptedException {
    Collection<CStatement> result = new HashSet<>();
    boolean fallBack = false;
    ExpressionTree<AExpression> tree =
        parseStatement(pCode, pResultFunction, pCParser, pScope, pParserTools);
    if (!tree.equals(ExpressionTrees.getTrue())) {
      if (tree.equals(ExpressionTrees.getFalse())) {
        return ImmutableSet.of(
            new CExpressionStatement(
                FileLocation.DUMMY,
                new CIntegerLiteralExpression(
                    FileLocation.DUMMY, CNumericTypes.INT, BigInteger.ZERO)));
      }
      if (tree instanceof LeafExpression) {
        LeafExpression<AExpression> leaf = (LeafExpression<AExpression>) tree;
        AExpression expression = leaf.getExpression();
        if (expression instanceof CExpression) {
          result.add(new CExpressionStatement(FileLocation.DUMMY, (CExpression) expression));
        } else {
          fallBack = true;
        }
      } else if (ExpressionTrees.isAnd(tree)) {
        for (ExpressionTree<AExpression> child : ExpressionTrees.getChildren(tree)) {
          if (child instanceof LeafExpression) {
            AExpression expression = ((LeafExpression<AExpression>) child).getExpression();
            if (expression instanceof CExpression) {
              result.add(new CExpressionStatement(FileLocation.DUMMY, (CExpression) expression));
            } else {
              fallBack = true;
            }
          } else {
            fallBack = true;
          }
        }
      } else {
        fallBack = true;
      }
    }
    if (fallBack) {
      String code = tryFixACSL(tryFixArrayInitializers(pCode), pResultFunction, pScope);
      return CParserUtils.parseListOfStatements(code, pCParser, pScope);
    }
    return result;
  }

  /**
   * Attempt to parse each element of the given set of strings as a C statements, treats the
   * successfully parsed statements as expression statements, and conjoins their expressions,
   * creating an expression tree. This method does <em>not</em> fail if parsing of some or the
   * elements fails; instead a warning is logged using the given log manager.
   *
   * @param pStatements the set of strings to parse as C statements.
   * @param pResultFunction the target function of {@literal "\result"} expressions.
   * @param pCParser the C parser to be used.
   * @param pScope the scope to interpret variables in.
   * @param pParserTools the auxiliary tools to be used for parsing.
   * @return an expression tree conjoining the expressions of successfully parsed expression
   *     statements.
   */
  public static ExpressionTree<AExpression> parseStatementsAsExpressionTree(
      Set<String> pStatements,
      Optional<String> pResultFunction,
      CParser pCParser,
      Scope pScope,
      ParserTools pParserTools)
      throws InterruptedException {
    ExpressionTree<AExpression> result = ExpressionTrees.getTrue();
    for (String assumeCode : pStatements) {
      try {
        ExpressionTree<AExpression> expressionTree =
            parseStatement(assumeCode, pResultFunction, pCParser, pScope, pParserTools);
        result = And.of(result, expressionTree);
      } catch (InvalidAutomatonException e) {
        pParserTools.logger.log(
            Level.WARNING, "Cannot interpret code as C statement(s): <" + assumeCode + ">");
      }
    }
    return result;
  }

  private static ExpressionTree<AExpression> parseStatement(
      String pAssumeCode,
      Optional<String> pResultFunction,
      CParser pCParser,
      Scope pScope,
      ParserTools pParserTools)
      throws InvalidAutomatonException, InterruptedException {

    // Try the old method first; it works for simple expressions
    // and also supports assignment statements and multiple statements easily
    String assumeCode = tryFixArrayInitializers(pAssumeCode);
    Collection<CStatement> statements = null;
    try {
      statements = CParserUtils.parseListOfStatements(assumeCode, pCParser, pScope);
    } catch (RuntimeException e) {
      if (e.getMessage() != null && e.getMessage().contains("Syntax error:")) {
        statements =
            CParserUtils.parseListOfStatements(
                tryFixACSL(assumeCode, pResultFunction, pScope), pCParser, pScope);
      } else {
        throw e;
      }
    } catch (InvalidAutomatonException e) {
      statements =
          CParserUtils.parseListOfStatements(
              tryFixACSL(assumeCode, pResultFunction, pScope), pCParser, pScope);
    }
    statements =
        removeDuplicates(from(statements).transform(CParserUtils::adjustCharAssignmentSignedness));
    {
      CBinaryExpressionBuilder binaryExpressionBuilder =
          new CBinaryExpressionBuilder(pParserTools.machineModel, pParserTools.logger);
      Function<CStatement, ExpressionTree<AExpression>> fromStatement =
          pStatement -> LeafExpression.fromStatement(pStatement, binaryExpressionBuilder);
      // Check that no expressions were split
      if (!FluentIterable.from(statements)
          .anyMatch(statement -> statement.toString().toUpperCase().contains("__CPACHECKER_TMP"))) {
        return And.of(FluentIterable.from(statements).transform(fromStatement));
      }
    }

    // For complex expressions, assume we are dealing with expression statements
    ExpressionTree<AExpression> result = ExpressionTrees.getTrue();
    try {
      result = parseExpression(pAssumeCode, pResultFunction, pScope, pCParser, pParserTools);
    } catch (InvalidAutomatonException e) {
      // Try splitting on ';' to support legacy code:
      Splitter semicolonSplitter = Splitter.on(';').omitEmptyStrings().trimResults();
      List<String> clausesStrings = semicolonSplitter.splitToList(pAssumeCode);
      if (clausesStrings.isEmpty()) {
        throw e;
      }
      List<ExpressionTree<AExpression>> clauses = new ArrayList<>(clausesStrings.size());
      for (String statement : clausesStrings) {
        clauses.add(parseExpression(statement, pResultFunction, pScope, pCParser, pParserTools));
      }
      result = And.of(clauses);
    }
    return result;
  }

  private static String tryFixACSL(
      String pAssumeCode, Optional<String> pResultFunction, Scope pScope) {
    String assumeCode = pAssumeCode.trim();
    if (assumeCode.endsWith(";")) {
      assumeCode = assumeCode.substring(0, assumeCode.length() - 1);
    }

    assumeCode = replaceResultVar(pResultFunction, pScope, assumeCode);

    Splitter splitter = Splitter.on("==>").limit(2);
    while (assumeCode.contains("==>")) {
      Iterator<String> partIterator = splitter.split(assumeCode).iterator();
      assumeCode =
          String.format("!(%s) || (%s)", partIterator.next().trim(), partIterator.next().trim());
    }
    return assumeCode;
  }

  private static String replaceResultVar(
      Optional<String> pResultFunction, Scope pScope, String assumeCode) {
    if (pResultFunction.isPresent()) {
      String resultFunctionName = pResultFunction.orElseThrow();
      if (pScope instanceof CProgramScope) {
        CProgramScope scope = (CProgramScope) pScope;
        if (scope.hasFunctionReturnVariable(resultFunctionName)) {
          CSimpleDeclaration functionReturnVariable =
              scope.getFunctionReturnVariable(resultFunctionName);
          return assumeCode.replace("\\result", " " + functionReturnVariable.getName());
        }
      }
      return assumeCode.replace("\\result", String.format(" %s() ", resultFunctionName));
    }
    return assumeCode.replace("\\result", " __CPAchecker_ACSL_result() ");
  }

  private static ExpressionTree<AExpression> parseExpression(
      String pAssumeCode,
      Optional<String> pResultFunction,
      Scope pScope,
      CParser pCParser,
      ParserTools pParserTools)
      throws InvalidAutomatonException, InterruptedException {
    String assumeCode = pAssumeCode.trim();
    while (assumeCode.endsWith(";")) {
      assumeCode = assumeCode.substring(0, assumeCode.length() - 1).trim();
    }
    String formatString = "int test() { if (%s) { return 1; } else { return 0; } ; }";
    String testCode = String.format(formatString, assumeCode);

    ParseResult parseResult;
    try {
      parseResult =
          pCParser.parseString(Path.of("#expr#"), testCode, new CSourceOriginMapping(), pScope);
    } catch (CParserException e) {
      assumeCode = tryFixACSL(assumeCode, pResultFunction, pScope);
      testCode = String.format(formatString, assumeCode);
      try {
        parseResult =
            pCParser.parseString(Path.of("#expr#"), testCode, new CSourceOriginMapping(), pScope);
      } catch (CParserException e2) {
        throw new InvalidAutomatonException(
            "Cannot interpret code as C expression: <" + pAssumeCode + ">", e);
      }
    }
    FunctionEntryNode entryNode = parseResult.getFunctions().values().iterator().next();

    return asExpressionTree(entryNode, pParserTools);
  }

  private static ExpressionTree<AExpression> asExpressionTree(
      FunctionEntryNode pEntry, ParserTools pParserTools) {
    ExpressionTreeFactory<AExpression> factory = pParserTools.expressionTreeFactory;
    Map<CFANode, ExpressionTree<AExpression>> memo = new HashMap<>();
    memo.put(pEntry, ExpressionTrees.getTrue());
    Set<CFANode> ready = new HashSet<>();
    ready.add(pEntry);
    Queue<CFANode> waitlist = new ArrayDeque<>();
    waitlist.offer(pEntry);
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();

      // Current tree is already complete in this location
      ExpressionTree<AExpression> currentTree = memo.get(current);

      // Compute successor trees
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(current)) {

        CFANode succ = leavingEdge.getSuccessor();

        // Get the tree currently stored for the successor
        ExpressionTree<AExpression> succTree = memo.get(succ);
        if (succTree == null) {
          succTree = ExpressionTrees.getFalse();
        }

        // Now, build the disjunction of the old tree with the new path

        // Handle the return statement: Returning 0 means false, 1 means true
        if (leavingEdge instanceof AReturnStatementEdge) {
          AReturnStatementEdge returnStatementEdge = (AReturnStatementEdge) leavingEdge;
          Optional<? extends AExpression> optExpression = returnStatementEdge.getExpression();
          assert optExpression.isPresent();
          if (!optExpression.isPresent()) {
            return ExpressionTrees.getTrue();
          }
          AExpression expression = optExpression.get();
          if (!(expression instanceof AIntegerLiteralExpression)) {
            return ExpressionTrees.getTrue();
          }
          AIntegerLiteralExpression literal = (AIntegerLiteralExpression) expression;
          // If the value is zero, the current path is 'false', so we do not add it.
          // If the value is one, we add the current path
          if (!literal.getValue().equals(BigInteger.ZERO)) {
            succTree = factory.or(succTree, currentTree);
          }

          // Handle assume edges
        } else if (leavingEdge instanceof AssumeEdge) {
          AssumeEdge assumeEdge = (AssumeEdge) leavingEdge;
          AExpression expression = assumeEdge.getExpression();

          if (expression.toString().contains(CPACHECKER_TMP_PREFIX)) {
            for (CFAEdge enteringEdge : CFAUtils.enteringEdges(current)) {
              Map<AExpression, AExpression> tmpVariableValues =
                  collectCPAcheckerTMPValues(enteringEdge);
              if (!tmpVariableValues.isEmpty()) {
                expression =
                    replaceCPAcheckerTMPVariables(assumeEdge.getExpression(), tmpVariableValues);
              }
              final ExpressionTree<AExpression> newPath;
              if (!expression.toString().contains(CPACHECKER_TMP_PREFIX)) {
                newPath =
                    factory.and(
                        currentTree, factory.leaf(expression, assumeEdge.getTruthAssumption()));
              } else {
                newPath = currentTree;
              }
              succTree = factory.or(succTree, newPath);
            }
          } else {
            final ExpressionTree<AExpression> newPath;
            newPath =
                factory.and(currentTree, factory.leaf(expression, assumeEdge.getTruthAssumption()));
            succTree = factory.or(succTree, newPath);
          }
          // All other edges do not change the path
        } else {
          succTree = factory.or(succTree, currentTree);
        }

        memo.put(succ, succTree);
      }

      // Prepare successors
      for (CFANode successor : CFAUtils.successorsOf(current)) {
        if (CFAUtils.predecessorsOf(successor).allMatch(Predicates.in(ready))
            && ready.add(successor)) {
          waitlist.offer(successor);
        }
      }
    }
    return pParserTools.expressionTreeSimplifier.simplify(memo.get(pEntry.getExitNode()));
  }

  private static AExpression replaceCPAcheckerTMPVariables(
      AExpression pExpression, Map<AExpression, AExpression> pTmpValues) {
    // Short cut if there cannot be any matches
    if (pTmpValues.isEmpty()) {
      return pExpression;
    }
    AExpression directMatch = pTmpValues.get(pExpression);
    if (directMatch != null) {
      return directMatch;
    }
    if (pExpression instanceof CBinaryExpression) {
      CBinaryExpression binaryExpression = (CBinaryExpression) pExpression;
      CExpression op1 =
          (CExpression) replaceCPAcheckerTMPVariables(binaryExpression.getOperand1(), pTmpValues);
      CExpression op2 =
          (CExpression) replaceCPAcheckerTMPVariables(binaryExpression.getOperand2(), pTmpValues);
      return new CBinaryExpression(
          binaryExpression.getFileLocation(),
          binaryExpression.getExpressionType(),
          binaryExpression.getCalculationType(),
          op1,
          op2,
          binaryExpression.getOperator());
    }
    if (pExpression instanceof CUnaryExpression) {
      CUnaryExpression unaryExpression = (CUnaryExpression) pExpression;
      CExpression op =
          (CExpression) replaceCPAcheckerTMPVariables(unaryExpression.getOperand(), pTmpValues);
      return new CUnaryExpression(
          unaryExpression.getFileLocation(),
          unaryExpression.getExpressionType(),
          op,
          unaryExpression.getOperator());
    }
    return pExpression;
  }

  private static Map<AExpression, AExpression> collectCPAcheckerTMPValues(CFAEdge pEdge) {
    if (pEdge instanceof AStatementEdge) {
      AStatement statement = ((AStatementEdge) pEdge).getStatement();
      if (statement instanceof AExpressionAssignmentStatement) {
        AExpressionAssignmentStatement expAssignStmt = (AExpressionAssignmentStatement) statement;
        ALeftHandSide lhs = expAssignStmt.getLeftHandSide();
        if (lhs instanceof AIdExpression
            && ((AIdExpression) lhs).getName().contains(CPACHECKER_TMP_PREFIX)) {
          AExpression rhs = expAssignStmt.getRightHandSide();
          return ImmutableMap.of(lhs, rhs);
        }
      }
    }

    return ImmutableMap.of();
  }

  /**
   * Some tools put assumptions for multiple statements on the same edge, which may lead to
   * contradictions between the assumptions.
   *
   * <p>This is clearly a tool error, but for the competition we want to help them out and only use
   * the last assumption.
   *
   * @param pStatements the assumptions.
   * @return the duplicate-free assumptions.
   */
  private static Collection<CStatement> removeDuplicates(
      Iterable<? extends CStatement> pStatements) {
    Map<Object, CStatement> result = new HashMap<>();
    for (CStatement statement : pStatements) {
      if (statement instanceof CExpressionAssignmentStatement) {
        CExpressionAssignmentStatement assignmentStatement =
            (CExpressionAssignmentStatement) statement;
        result.put(assignmentStatement.getLeftHandSide(), assignmentStatement);
      } else {
        result.put(statement, statement);
      }
    }
    return result.values();
  }

  /**
   * Be nice to tools that assume that default char (when it is neither specified as signed nor as
   * unsigned) may be unsigned.
   *
   * @param pStatement the assignment statement.
   * @return the adjusted statement.
   */
  private static CStatement adjustCharAssignmentSignedness(CStatement pStatement) {
    if (pStatement instanceof CExpressionAssignmentStatement) {
      CExpressionAssignmentStatement statement = (CExpressionAssignmentStatement) pStatement;
      CLeftHandSide leftHandSide = statement.getLeftHandSide();
      CType canonicalType = leftHandSide.getExpressionType().getCanonicalType();
      if (canonicalType instanceof CSimpleType) {
        CSimpleType simpleType = (CSimpleType) canonicalType;
        CBasicType basicType = simpleType.getType();
        if (basicType.equals(CBasicType.CHAR)
            && !simpleType.isSigned()
            && !simpleType.isUnsigned()) {
          CExpression rightHandSide = statement.getRightHandSide();
          CExpression castedRightHandSide =
              new CCastExpression(rightHandSide.getFileLocation(), canonicalType, rightHandSide);
          return new CExpressionAssignmentStatement(
              statement.getFileLocation(), leftHandSide, castedRightHandSide);
        }
      }
    }
    return pStatement;
  }

  /**
   * Let's be nice to tools that ignore the restriction that array initializers are not allowed as
   * right-hand sides of assignment statements and try to help them. This is a hack, no good
   * solution. We would need a kind-of-but-not-really-C-parser to properly handle these
   * declarations-that-aren't-declarations.
   *
   * @param pAssumeCode the code from the witness assumption.
   * @return the code from the witness assumption if no supported array initializer is contained;
   *     otherwise the fixed code.
   */
  private static String tryFixArrayInitializers(String pAssumeCode) {
    String C_INTEGER = "([\\+\\-])?(0[xX])?[0-9a-fA-F]+";
    String assumeCode = pAssumeCode.trim();
    if (assumeCode.endsWith(";")) {
      assumeCode = assumeCode.substring(0, assumeCode.length() - 1);
    }
    /*
     * This only covers the special case of one assignment statement using one
     * array of integers.
     */
    if (assumeCode.matches(
        ".+=\\s*\\{\\s*(" + C_INTEGER + "\\s*(,\\s*" + C_INTEGER + "\\s*)*)?\\}\\s*")) {
      Iterable<String> assignmentParts = Splitter.on('=').trimResults().split(assumeCode);
      Iterator<String> assignmentPartIterator = assignmentParts.iterator();
      if (!assignmentPartIterator.hasNext()) {
        return pAssumeCode;
      }
      String leftHandSide = assignmentPartIterator.next();
      if (!assignmentPartIterator.hasNext()) {
        return pAssumeCode;
      }
      String rightHandSide = assignmentPartIterator.next().trim();
      if (assignmentPartIterator.hasNext()) {
        return pAssumeCode;
      }
      assert rightHandSide.startsWith("{") && rightHandSide.endsWith("}");
      rightHandSide = rightHandSide.substring(1, rightHandSide.length() - 1).trim();
      Iterable<String> elements = Splitter.on(',').trimResults().split(rightHandSide);
      StringBuilder resultBuilder = new StringBuilder();
      int index = 0;
      for (String element : elements) {
        resultBuilder.append(String.format("%s[%d] = %s; ", leftHandSide, index, element));
        ++index;
      }
      return resultBuilder.toString();
    }
    return pAssumeCode;
  }

  /**
   * Instances of this class are aggregates of utilities required for the parsing functions of
   * {@link CParserUtils}.
   */
  public static class ParserTools {

    private final ExpressionTreeFactory<AExpression> expressionTreeFactory;

    private final Simplifier<AExpression> expressionTreeSimplifier;

    private final MachineModel machineModel;

    private final LogManager logger;

    private ParserTools(
        ExpressionTreeFactory<AExpression> pExpressionTreeFactory,
        MachineModel pMachineModel,
        LogManager pLogger) {
      expressionTreeFactory = Objects.requireNonNull(pExpressionTreeFactory);
      expressionTreeSimplifier =
          Objects.requireNonNull(ExpressionTrees.newSimplifier(pExpressionTreeFactory));
      machineModel = Objects.requireNonNull(pMachineModel);
      logger = Objects.requireNonNull(pLogger);
    }

    public static ParserTools create(
        ExpressionTreeFactory<AExpression> pExpressionTreeFactory,
        MachineModel pMachineModel,
        LogManager pLogger) {
      return new ParserTools(pExpressionTreeFactory, pMachineModel, pLogger);
    }
  }
}
