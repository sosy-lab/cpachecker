/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.common.Pair.zipList;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IType;
import org.sosy_lab.cpachecker.cfa.ast.RightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.StatementVisitor;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * Provides methods for generating, comparing and printing the ASTs generated from String.
 */
class AutomatonASTComparator {

  // TODO refactor
  static CParser parser = null;

  /**
   * Every occurrence of the joker expression $? in the pattern is substituted by JOKER_EXPR.
   * This is necessary because the C-parser cannot parse the pattern if it contains Dollar-Symbols.
   * The JOKER_EXPR must be a valid C-Identifier. It will be used to recognize the jokers in the generated AST.
   */
  private static final String JOKER_EXPR = "CPAChecker_AutomatonAnalysis_JokerExpression";
  private static final String NUMBERED_JOKER_EXPR = "CPAChecker_AutomatonAnalysis_JokerExpression_Num";
  private static final Pattern NUMBERED_JOKER_PATTERN = Pattern.compile("\\$\\d+");

  static ASTMatcher generatePatternAST(String pPattern) throws InvalidAutomatonException {
    // $?-Jokers, $1-Jokers and function declaration
    String tmp = addFunctionDeclaration(replaceJokersInPattern(pPattern));

    return parse(tmp).accept(ASTMatcherGenerator.INSTANCE);
  }

  static IASTStatement generateSourceAST(String pSource) throws InvalidAutomatonException {
    String tmp = addFunctionDeclaration(pSource);

    return parse(tmp);
  }

  private static String replaceJokersInPattern(String pPattern) {
    String tmp = pPattern.replaceAll("\\$\\?", " " + JOKER_EXPR + " ");
    Matcher matcher = NUMBERED_JOKER_PATTERN.matcher(tmp);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(result, "");
      String key = tmp.substring(matcher.start()+1, matcher.end());
      try {
        int varKey = Integer.parseInt(key);
        result.append(" " + NUMBERED_JOKER_EXPR + varKey + " ");
      } catch (NumberFormatException e) {
        // did not work, but i cant log it down here. Should not be able to happen anyway (regex captures only ints)
        result.append(matcher.group());
      }
    }
    matcher.appendTail(result);
    return result.toString();
  }

  /**
   * Surrounds the argument with a function declaration.
   * This is necessary so the string can be parsed by the CDT parser.
   * @param pBody
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
   * Parse the content of a file into an AST with the Eclipse CDT parser.
   * If an error occurs, the program is halted.
   *
   * @param code The C code to parse.
   * @return The AST.
   * @throws InvalidAutomatonException
   */
  private static IASTStatement parse(String code) throws InvalidAutomatonException {
    checkState(parser != null);
    try {
      IASTNode statement = parser.parseSingleStatement(code);
      if (!(statement instanceof IASTStatement)) {
        throw new InvalidAutomatonException("Not a valid statement: " + statement.toASTString());
      }
      return (IASTStatement)statement;
    } catch (ParserException e) {
      throw new InvalidAutomatonException("Error during parsing C code \""
          + code + "\": " + e.getMessage());
    }
  }


  /**
   * The interface for a pre-compiled AST pattern.
   */
  static interface ASTMatcher {

    boolean matches(IASTNode pSource, AutomatonExpressionArguments pArgs);
  }

  /**
   * The visitor that generates a pre-compiled ASTMatcher from a pattern AST.
   */
  private static enum ASTMatcherGenerator implements RightHandSideVisitor<ASTMatcher, RuntimeException>,
                                                     StatementVisitor<ASTMatcher, RuntimeException> {

    INSTANCE;

    @Override
    public ASTMatcher visit(IASTIdExpression exp) {
      String name = exp.getName();

      if (name.equals(JOKER_EXPR)) {
        return JokerMatcher.INSTANCE;

      } else if (name.startsWith(NUMBERED_JOKER_EXPR)) {
        String s = name.substring(NUMBERED_JOKER_EXPR.length());
        int i = Integer.parseInt(s);
        return new NumberedJokerMatcher(i);

      } else {
        return new IASTIdExpressionMatcher(exp);
      }
    }

    @Override
    public ASTMatcher visit(IASTArraySubscriptExpression exp) {
      return new ArraySubscriptExpressionMatcher(exp, exp.getArrayExpression().accept(this), exp.getSubscriptExpression().accept(this));
    }

    @Override
    public ASTMatcher visit(IASTBinaryExpression exp) {
      return new BinaryExpressionMatcher(exp, exp.getOperand1().accept(this), exp.getOperand2().accept(this));
    }

    @Override
    public ASTMatcher visit(IASTCastExpression exp) {
      return new CastExpressionMatcher(exp, exp.getOperand().accept(this));
    }

    @Override
    public ASTMatcher visit(IASTFieldReference exp) {
      return new FieldReferenceMatcher(exp, exp.getFieldOwner().accept(this));
    }

    @Override
    public ASTMatcher visit(IASTCharLiteralExpression exp) {
      return new ExpressionWithFieldMatcher<IASTCharLiteralExpression, Character>(IASTCharLiteralExpression.class, exp) {

        @Override
        protected Character getFieldValueFrom(IASTCharLiteralExpression pSource) {
          return pSource.getCharacter();
        }
      };
    }

    @Override
    public ASTMatcher visit(IASTFloatLiteralExpression exp) {
      return new ExpressionWithFieldMatcher<IASTFloatLiteralExpression, BigDecimal>(IASTFloatLiteralExpression.class, exp) {

        @Override
        protected BigDecimal getFieldValueFrom(IASTFloatLiteralExpression pSource) {
          return pSource.getValue();
        }
      };
    }

    @Override
    public ASTMatcher visit(IASTIntegerLiteralExpression exp) {
      return new ExpressionWithFieldMatcher<IASTIntegerLiteralExpression, BigInteger>(IASTIntegerLiteralExpression.class, exp) {

        @Override
        protected BigInteger getFieldValueFrom(IASTIntegerLiteralExpression pSource) {
          return pSource.getValue();
        }
      };
    }

    @Override
    public ASTMatcher visit(IASTStringLiteralExpression exp) {
      return new ExpressionWithFieldMatcher<IASTStringLiteralExpression, String>(IASTStringLiteralExpression.class, exp) {

        @Override
        protected String getFieldValueFrom(IASTStringLiteralExpression pSource) {
          return pSource.getValue();
        }
      };
    }

    @Override
    public ASTMatcher visit(IASTTypeIdExpression exp) {
      return new TypeIdExpressionMatcher(exp);
    }

    @Override
    public ASTMatcher visit(IASTUnaryExpression exp) {
      return new UnaryExpressionMatcher(exp, exp.getOperand().accept(this));
    }

    @Override
    public ASTMatcher visit(IASTFunctionCallExpression exp) {
      List<ASTMatcher> parameterPatterns = new ArrayList<ASTMatcher>(exp.getParameterExpressions().size());
      for (IASTExpression parameter : exp.getParameterExpressions()) {
        parameterPatterns.add(parameter.accept(this));
      }

      ASTMatcher functionNamePattern = exp.getFunctionNameExpression().accept(this);

      if ((parameterPatterns.size() == 1)
          && (parameterPatterns.get(0) == JokerMatcher.INSTANCE)) {
        // pattern is something like foo($?), this should match all calls of foo(),
        // regardless of the number of parameters
        return new FunctionCallWildcardExpressionMatcher(exp, functionNamePattern);
      } else {
        return new FunctionCallExpressionMatcher(exp, functionNamePattern, parameterPatterns);
      }
    }

    @Override
    public ASTMatcher visit(IASTExpressionStatement stmt) {
      return new OneOperandExpressionMatcher<IASTExpressionStatement, Void>(
          IASTExpressionStatement.class, stmt, stmt.getExpression().accept(this)) {

        @Override
        protected IASTExpression getOperandFrom(IASTExpressionStatement pSource) {
          return pSource.getExpression();
        }
      };
    }

    private ASTMatcher visit(final IASTAssignment stmt) {
      final ASTMatcher leftHandSide = stmt.getLeftHandSide().accept(this);
      final ASTMatcher rightHandSide = stmt.getRightHandSide().accept(this);

      if (rightHandSide == JokerMatcher.INSTANCE) {
        // we don't care about right-hand side, it may be an expression or an assignment

        return new ASTMatcher() {
          @Override
          public boolean matches(IASTNode pSource, AutomatonExpressionArguments pArgs) {
            if (pSource instanceof IASTAssignment) {
              IASTAssignment source = (IASTAssignment)pSource;

              return leftHandSide.matches(source.getLeftHandSide(), pArgs);
            } else {
              return false;
            }
          }

          @Override
          public String toString() {
            return stmt.asStatement().toASTString();
          }
        };

      } else {
        return new ASTMatcher() {
          @Override
          public boolean matches(IASTNode pSource, AutomatonExpressionArguments pArgs) {
            if (pSource instanceof IASTAssignment) {
              IASTAssignment source = (IASTAssignment)pSource;

              return leftHandSide.matches(source.getLeftHandSide(), pArgs)
                  && rightHandSide.matches(source.getRightHandSide(), pArgs);
            } else {
              return false;
            }
          }

          @Override
          public String toString() {
            return stmt.asStatement().toASTString();
          }
        };
      }
    }

    @Override
    public ASTMatcher visit(final IASTExpressionAssignmentStatement stmt) {
      return visit((IASTAssignment)stmt);
    }

    @Override
    public ASTMatcher visit(IASTFunctionCallAssignmentStatement stmt) {
      return visit((IASTAssignment)stmt);
    }

    @Override
    public ASTMatcher visit(IASTFunctionCallStatement stmt) {
      return new OneOperandExpressionMatcher<IASTFunctionCallStatement, Void>(
          IASTFunctionCallStatement.class, stmt, stmt.getFunctionCallExpression().accept(this)) {

        @Override
        protected IASTRightHandSide getOperandFrom(IASTFunctionCallStatement pSource) {
          return pSource.getFunctionCallExpression();
        }
      };
    }
  }

  // several abstract helper implementations of ASTMatcher

  private static abstract class CheckedExpressionMatcher<T extends IASTNode> implements ASTMatcher {

    private final Class<T> cls;
    private final String rawSignature;

    protected CheckedExpressionMatcher(Class<T> pCls, T pPattern) {
      assert pCls.isInstance(pPattern);
      cls = pCls;
      rawSignature = pPattern.toASTString();
    }

    @Override
    public final boolean matches(IASTNode pSource, AutomatonExpressionArguments pArgs) {
      if (cls.isInstance(pSource)) {
        return matches2(cls.cast(pSource), pArgs);

      } else {
        return false;
      }
    }

    protected abstract boolean matches2(T pSource, AutomatonExpressionArguments pArg);

    @Override
    public String toString() {
      return rawSignature;
    }
  }

  private static abstract class ExpressionWithFieldMatcher<T extends IASTNode, F> extends CheckedExpressionMatcher<T> {

    private final F field;

    protected ExpressionWithFieldMatcher(Class<T> pCls, T pPattern) {
      super(pCls, pPattern);
      field = getFieldValueFrom(pPattern);
    }

    @Override
    protected boolean matches2(T pSource, AutomatonExpressionArguments pArg) {
      return equal(field, getFieldValueFrom(pSource));
    }

    protected F getFieldValueFrom(T pSource) {
      return null;
    }
  }

  private static abstract class OneOperandExpressionMatcher<T extends IASTNode, F> extends ExpressionWithFieldMatcher<T, F> {

    private final ASTMatcher operand;

    protected OneOperandExpressionMatcher(Class<T> pCls, T pPattern, ASTMatcher pOperand) {
      super(pCls, pPattern);
      operand = pOperand;
    }

    @Override
    protected boolean matches2(T pSource, AutomatonExpressionArguments pArgs) {
      return super.matches2(pSource, pArgs)
          && operand.matches(getOperandFrom(pSource), pArgs);
    }

    protected abstract IASTRightHandSide getOperandFrom(T pSource);
  }

  private static abstract class TwoOperandExpressionMatcher<T extends IASTNode, F> extends ExpressionWithFieldMatcher<T, F> {

    private final ASTMatcher operand1;
    private final ASTMatcher operand2;

    protected TwoOperandExpressionMatcher(Class<T> pCls, T pPattern, ASTMatcher pOperand1, ASTMatcher pOperand2) {
      super(pCls, pPattern);
      operand1 = pOperand1;
      operand2 = pOperand2;
    }

    @Override
    public final boolean matches2(T pSource, AutomatonExpressionArguments pArgs) {
      return super.matches2(pSource, pArgs)
          && operand1.matches(getOperand1From(pSource), pArgs)
          && operand2.matches(getOperand2From(pSource), pArgs);
    }

    protected abstract IASTRightHandSide getOperand1From(T pSource);
    protected abstract IASTRightHandSide getOperand2From(T pSource);
  }

  // several concrete implementations of ASTMatcher

  private static enum JokerMatcher implements ASTMatcher {
    INSTANCE;

    @Override
    public boolean matches(IASTNode pSource, AutomatonExpressionArguments pArgs) {
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
    public boolean matches(IASTNode pSource, AutomatonExpressionArguments pArgs) {
      // RawSignature returns the raw code before preprocessing.
      // This does not matter in this case because only very small sniplets, generated by method "addFunctionDeclaration" are tested, no preprocessing

      String value = pSource.toASTString();
      pArgs.putTransitionVariable(number, value);
      return true;
    }

    @Override
    public String toString() {
      return "$" + number;
    }
  }

  private static class ArraySubscriptExpressionMatcher extends TwoOperandExpressionMatcher<IASTArraySubscriptExpression, Void> {

    public ArraySubscriptExpressionMatcher(IASTArraySubscriptExpression pPattern, ASTMatcher pOperand1, ASTMatcher pOperand2) {
      super(IASTArraySubscriptExpression.class, pPattern, pOperand1, pOperand2);
    }

    @Override
    protected IASTExpression getOperand1From(IASTArraySubscriptExpression pSource) {
      return pSource.getArrayExpression();
    }

    @Override
    protected IASTExpression getOperand2From(IASTArraySubscriptExpression pSource) {
      return pSource.getSubscriptExpression();
    }
  }

  private static class BinaryExpressionMatcher extends TwoOperandExpressionMatcher<IASTBinaryExpression, BinaryOperator> {

    public BinaryExpressionMatcher(IASTBinaryExpression pPattern, ASTMatcher pOperand1, ASTMatcher pOperand2) {
      super(IASTBinaryExpression.class, pPattern, pOperand1, pOperand2);
    }

    @Override
    protected IASTExpression getOperand1From(IASTBinaryExpression pSource) {
      return pSource.getOperand1();
    }

    @Override
    protected IASTExpression getOperand2From(IASTBinaryExpression pSource) {
      return pSource.getOperand2();
    }

    @Override
    protected BinaryOperator getFieldValueFrom(IASTBinaryExpression pSource) {
      return pSource.getOperator();
    }
  }

  private static class CastExpressionMatcher extends OneOperandExpressionMatcher<IASTCastExpression, IType> {

    public CastExpressionMatcher(IASTCastExpression pPattern, ASTMatcher pOperand) {
      super(IASTCastExpression.class, pPattern, pOperand);
    }

    @Override
    protected IASTExpression getOperandFrom(IASTCastExpression pSource) {
      return pSource.getOperand();
    }

    @Override
    protected IType getFieldValueFrom(IASTCastExpression pSource) {
      return pSource.getType();
    }
  }

  private static class FieldReferenceMatcher extends OneOperandExpressionMatcher<IASTFieldReference, String> {

    public FieldReferenceMatcher(IASTFieldReference pPattern, ASTMatcher pOperand) {
      super(IASTFieldReference.class, pPattern, pOperand);
    }

    @Override
    protected IASTExpression getOperandFrom(IASTFieldReference pSource) {
      return pSource.getFieldOwner();
    }

    @Override
    protected String getFieldValueFrom(IASTFieldReference pSource) {
      return pSource.getFieldName();
    }
  }

  private static class IASTIdExpressionMatcher extends ExpressionWithFieldMatcher<IASTIdExpression, String> {

    public IASTIdExpressionMatcher(IASTIdExpression pPattern) {
      super(IASTIdExpression.class, pPattern);
    }

    @Override
    protected String getFieldValueFrom(IASTIdExpression pSource) {
      return pSource.getName();
    }
  }

  private static class UnaryExpressionMatcher extends OneOperandExpressionMatcher<IASTUnaryExpression, UnaryOperator> {

    public UnaryExpressionMatcher(IASTUnaryExpression pPattern, ASTMatcher pOperand) {
      super(IASTUnaryExpression.class, pPattern, pOperand);
    }

    @Override
    protected IASTExpression getOperandFrom(IASTUnaryExpression pSource) {
      return pSource.getOperand();
    }

    @Override
    protected UnaryOperator getFieldValueFrom(IASTUnaryExpression pSource) {
      return pSource.getOperator();
    }
  }

  private static class TypeIdExpressionMatcher extends ExpressionWithFieldMatcher<IASTTypeIdExpression, IType> {

    private final TypeIdOperator operator;

    public TypeIdExpressionMatcher(IASTTypeIdExpression pPattern) {
      super(IASTTypeIdExpression.class, pPattern);
      operator = pPattern.getOperator();
    }

    @Override
    protected boolean matches2(IASTTypeIdExpression pSource, AutomatonExpressionArguments pArg) {
      return equal(operator, pSource.getOperator())
          && super.matches2(pSource, pArg);
    }

    @Override
    protected IType getFieldValueFrom(IASTTypeIdExpression pSource) {
      return pSource.getType();
    }
  }

  private static class FunctionCallWildcardExpressionMatcher extends OneOperandExpressionMatcher<IASTFunctionCallExpression, Void> {

    // this matcher is for patterns like foo($?)
    // it compares only the function name and ignores any parameters

    protected FunctionCallWildcardExpressionMatcher(IASTFunctionCallExpression pPattern, ASTMatcher pFunctionNameExpression) {
      super(IASTFunctionCallExpression.class, pPattern, pFunctionNameExpression);
    }

    @Override
    protected IASTExpression getOperandFrom(IASTFunctionCallExpression pSource) {
      return pSource.getFunctionNameExpression();
    }
  }

  private static class FunctionCallExpressionMatcher extends FunctionCallWildcardExpressionMatcher {

    private final List<ASTMatcher> parameterPatterns;

    protected FunctionCallExpressionMatcher(IASTFunctionCallExpression pPattern, ASTMatcher pFunctionNameExpression, List<ASTMatcher> pParameterPatterns) {
      super(pPattern, pFunctionNameExpression);
      parameterPatterns = pParameterPatterns;
    }

    @Override
    protected boolean matches2(IASTFunctionCallExpression pSource, AutomatonExpressionArguments pArg) {
      if (!super.matches2(pSource, pArg)) {
        return false;
      }

      if (parameterPatterns.size() != pSource.getParameterExpressions().size()) {
        return false;
      }

      for (Pair<ASTMatcher, IASTExpression> parameters : zipList(parameterPatterns, pSource.getParameterExpressions())) {
        if (!parameters.getFirst().matches(parameters.getSecond(), pArg)) {
          return false;
        }
      }
      return true;
    }

    @Override
    protected IASTExpression getOperandFrom(IASTFunctionCallExpression pSource) {
      return pSource.getFunctionNameExpression();
    }
  }
}
