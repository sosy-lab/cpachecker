// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.CParserUtils;

/** Provides methods for generating, comparing and printing the ASTs generated from String. */
class AutomatonASTComparator {

  /**
   * Every occurrence of the joker expression $? in the pattern is substituted by JOKER_EXPR. This
   * is necessary because the C-parser cannot parse the pattern if it contains Dollar-Symbols. The
   * JOKER_EXPR must be a valid C-Identifier. It will be used to recognize the jokers in the
   * generated AST.
   */
  static final String JOKER_EXPR = "CPAchecker_AutomatonAnalysis_JokerExpression_Wildcard";

  private static final String NUMBERED_JOKER_EXPR =
      "CPAchecker_AutomatonAnalysis_JokerExpression_Num";
  private static final Pattern JOKER_PATTERN = Pattern.compile("\\$(\\d+|\\?)");

  static ASTMatcher generatePatternAST(String pPattern, CParser parser, Scope scope)
      throws InvalidAutomatonException, NoException, InterruptedException {
    return CParserUtils.parseSingleStatement(replaceJokersInPattern(pPattern), parser, scope)
        .accept(ASTMatcherGenerator.INSTANCE);
  }

  @VisibleForTesting
  static String replaceJokersInPattern(String pPattern) {
    Matcher matcher = JOKER_PATTERN.matcher(pPattern);
    StringBuilder result = new StringBuilder();

    // Each $? joker needs a unique C identifier to avoid type problems, so we append a counter.
    int wildcardCount = 0;

    while (matcher.find()) {
      matcher.appendReplacement(result, "");
      String match = matcher.group();
      if (match.equals("$?")) {
        result.append(' ').append(JOKER_EXPR).append(wildcardCount++).append(' ');
      } else {
        try {
          int varKey = Integer.parseInt(match.substring(1));
          result.append(' ').append(NUMBERED_JOKER_EXPR).append(varKey).append(' ');
        } catch (NumberFormatException e) {
          // may happen if number was too large
          result.append(match);
        }
      }
    }
    matcher.appendTail(result);
    return result.toString();
  }

  /** The interface for a pre-compiled AST pattern. */
  interface ASTMatcher {

    boolean matches(CAstNode pSource, AutomatonExpressionArguments pArgs);
  }

  private interface CheckedASTMatcher<T extends CAstNode> {

    boolean matches(T pSource, AutomatonExpressionArguments pArg);
  }

  /** The visitor that generates a pre-compiled ASTMatcher from a pattern AST. */
  private enum ASTMatcherGenerator
      implements
          CRightHandSideVisitor<ASTMatcher, NoException>,
          CStatementVisitor<ASTMatcher, NoException> {
    INSTANCE;

    @Override
    public ASTMatcher visit(CIdExpression exp) {
      String name = exp.getName();

      if (name.startsWith(JOKER_EXPR)) {
        return JokerMatcher.INSTANCE;

      } else if (name.startsWith(NUMBERED_JOKER_EXPR)) {
        String s = name.substring(NUMBERED_JOKER_EXPR.length());
        int i = Integer.parseInt(s);
        return new NumberedJokerMatcher(i);

      } else {
        return createMatcher(
            CIdExpression.class,
            exp,
            compareField(
                exp,
                e -> e.getDeclaration() == null ? e.getName() : e.getDeclaration().getOrigName()));
      }
    }

    @Override
    public ASTMatcher visit(CArraySubscriptExpression exp) {
      return createMatcher(
          CArraySubscriptExpression.class,
          exp,
          compareOperand(exp, CArraySubscriptExpression::getArrayExpression),
          compareOperand(exp, CArraySubscriptExpression::getSubscriptExpression));
    }

    @Override
    public ASTMatcher visit(CBinaryExpression exp) {
      return createMatcher(
          CBinaryExpression.class,
          exp,
          compareField(exp, CBinaryExpression::getOperator),
          compareOperand(exp, CBinaryExpression::getOperand1),
          compareOperand(exp, CBinaryExpression::getOperand2));
    }

    @Override
    public ASTMatcher visit(CCastExpression exp) {
      return createMatcher(
          CCastExpression.class,
          exp,
          compareField(exp, CCastExpression::getExpressionType),
          compareOperand(exp, CCastExpression::getOperand));
    }

    @Override
    public ASTMatcher visit(CComplexCastExpression exp) {
      return createMatcher(
          CComplexCastExpression.class,
          exp,
          compareField(exp, CComplexCastExpression::isRealCast),
          compareField(exp, CComplexCastExpression::getType),
          compareOperand(exp, CComplexCastExpression::getOperand));
    }

    @Override
    public ASTMatcher visit(CFieldReference exp) {
      return createMatcher(
          CFieldReference.class,
          exp,
          compareField(exp, CFieldReference::getFieldName),
          compareOperand(exp, CFieldReference::getFieldOwner));
    }

    @Override
    public ASTMatcher visit(CCharLiteralExpression exp) {
      return createMatcher(
          CCharLiteralExpression.class,
          exp,
          compareField(exp, CCharLiteralExpression::getCharacter));
    }

    @Override
    public ASTMatcher visit(CFloatLiteralExpression exp) {
      return createMatcher(
          CFloatLiteralExpression.class, exp, compareField(exp, CFloatLiteralExpression::getValue));
    }

    @Override
    public ASTMatcher visit(CImaginaryLiteralExpression exp) {
      return createMatcher(
          CImaginaryLiteralExpression.class,
          exp,
          compareField(exp, CImaginaryLiteralExpression::getValue));
    }

    @Override
    public ASTMatcher visit(CIntegerLiteralExpression exp) {
      return createMatcher(
          CIntegerLiteralExpression.class,
          exp,
          compareField(exp, CIntegerLiteralExpression::getValue));
    }

    @Override
    public ASTMatcher visit(CStringLiteralExpression exp) {
      return createMatcher(
          CStringLiteralExpression.class,
          exp,
          compareField(exp, CStringLiteralExpression::getValue));
    }

    @Override
    public ASTMatcher visit(CTypeIdExpression exp) {
      return createMatcher(
          CTypeIdExpression.class,
          exp,
          compareField(exp, CTypeIdExpression::getOperator),
          compareField(exp, CTypeIdExpression::getType));
    }

    @Override
    public ASTMatcher visit(CUnaryExpression exp) {
      return createMatcher(
          CUnaryExpression.class,
          exp,
          compareField(exp, CUnaryExpression::getOperator),
          compareOperand(exp, CUnaryExpression::getOperand));
    }

    @Override
    public ASTMatcher visit(CPointerExpression exp) {
      return createMatcher(
          CPointerExpression.class, exp, compareOperand(exp, CPointerExpression::getOperand));
    }

    @Override
    public ASTMatcher visit(CAddressOfLabelExpression exp) {
      return createMatcher(
          CAddressOfLabelExpression.class,
          exp,
          compareField(exp, CAddressOfLabelExpression::getLabelName));
    }

    @Override
    public ASTMatcher visit(CFunctionCallExpression exp) {
      List<ASTMatcher> parameterPatterns = new ArrayList<>(exp.getParameterExpressions().size());
      for (CExpression parameter : exp.getParameterExpressions()) {
        parameterPatterns.add(parameter.accept(this));
      }

      if ((parameterPatterns.size() == 1) && (parameterPatterns.get(0) == JokerMatcher.INSTANCE)) {
        // pattern is something like foo($?), this should match all calls of foo(),
        // regardless of the number of parameters
        return createMatcher(
            CFunctionCallExpression.class,
            exp,
            compareOperand(exp, CFunctionCallExpression::getFunctionNameExpression));

      } else {
        return createMatcher(
            CFunctionCallExpression.class,
            exp,
            compareOperand(exp, CFunctionCallExpression::getFunctionNameExpression),
            new OperandListMatcher(parameterPatterns));
      }
    }

    @Override
    public ASTMatcher visit(CExpressionStatement stmt) {
      return createMatcher(
          CExpressionStatement.class,
          stmt,
          compareOperand(stmt, CExpressionStatement::getExpression));
    }

    private ASTMatcher visit(final CAssignment stmt) {
      final ASTMatcher rightHandSide = stmt.getRightHandSide().accept(this);

      if (rightHandSide == JokerMatcher.INSTANCE) {
        // we don't care about right-hand side, it may be an expression or an assignment
        return createMatcher(
            CAssignment.class, stmt, compareOperand(stmt, CAssignment::getLeftHandSide));

      } else {
        return createMatcher(
            CAssignment.class,
            stmt,
            compareOperand(stmt, CAssignment::getLeftHandSide),
            (pSource, pArgs) -> rightHandSide.matches(pSource.getRightHandSide(), pArgs));
      }
    }

    @Override
    public ASTMatcher visit(final CExpressionAssignmentStatement stmt) {
      return visit((CAssignment) stmt);
    }

    @Override
    public ASTMatcher visit(CFunctionCallAssignmentStatement stmt) {
      return visit((CAssignment) stmt);
    }

    @Override
    public ASTMatcher visit(CFunctionCallStatement stmt) {
      return createMatcher(
          CFunctionCallStatement.class,
          stmt,
          compareOperand(stmt, CFunctionCallStatement::getFunctionCallExpression));
    }

    @SafeVarargs
    private static <T extends CAstNode> ASTMatcher createMatcher(
        Class<T> pCls, T pPattern, CheckedASTMatcher<T>... matchers) {
      assert pCls.isInstance(pPattern);

      return new ASTMatcher() {

        @Override
        public boolean matches(CAstNode pSource, AutomatonExpressionArguments pArgs) {
          if (pCls.isInstance(pSource)) {
            T source = pCls.cast(pSource);
            for (CheckedASTMatcher<T> matcher : matchers) {
              if (!matcher.matches(source, pArgs)) {
                return false;
              }
            }
            return true;

          } else {
            return false;
          }
        }

        @Override
        public String toString() {
          return pPattern.toASTString();
        }
      };
    }

    private <T extends CAstNode> CheckedASTMatcher<T> compareField(
        T pPattern, Function<T, ?> fieldAccess) {
      Object field = fieldAccess.apply(pPattern);

      return (pSource, pArgs) -> Objects.equals(field, fieldAccess.apply(pSource));
    }

    private <T extends CAstNode> CheckedASTMatcher<T> compareOperand(
        T pPattern, Function<T, CRightHandSide> operandAccess) {
      ASTMatcher pOperand = operandAccess.apply(pPattern).accept(this);

      return (pSource, pArgs) -> pOperand.matches(operandAccess.apply(pSource), pArgs);
    }
  }

  private static final class OperandListMatcher
      implements CheckedASTMatcher<CFunctionCallExpression> {

    private final List<ASTMatcher> parameterPatterns;

    OperandListMatcher(List<ASTMatcher> pParameterPatterns) {
      parameterPatterns = ImmutableList.copyOf(pParameterPatterns);
    }

    @Override
    public boolean matches(CFunctionCallExpression pSource, AutomatonExpressionArguments pArg) {

      if (parameterPatterns.size() != pSource.getParameterExpressions().size()) {
        return false;
      }

      return Streams.zip(
              parameterPatterns.stream(),
              pSource.getParameterExpressions().stream(),
              (pattern, parameter) -> pattern.matches(parameter, pArg))
          .allMatch(match -> match);
    }
  }

  // several concrete implementations of ASTMatcher

  private enum JokerMatcher implements ASTMatcher {
    INSTANCE;

    @Override
    public boolean matches(CAstNode pSource, AutomatonExpressionArguments pArgs) {
      return true;
    }

    @Override
    public String toString() {
      return "$?";
    }
  }

  private static class NumberedJokerMatcher implements ASTMatcher {

    private final int number;

    public NumberedJokerMatcher(int pNumber) {
      number = pNumber;
    }

    @Override
    public boolean matches(CAstNode pSource, AutomatonExpressionArguments pArgs) {
      // RawSignature returns the raw code before preprocessing.
      // This does not matter in this case because only very small sniplets, generated by method
      // "addFunctionDeclaration" are tested, no preprocessing

      pArgs.putTransitionVariable(number, pSource);
      return true;
    }

    @Override
    public String toString() {
      return "$" + number;
    }
  }
}
