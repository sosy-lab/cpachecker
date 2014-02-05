/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
import static org.sosy_lab.common.Pair.zipList;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Provides methods for generating, comparing and printing the ASTs generated from String.
 */
class AutomatonASTComparator {

  /**
   * Every occurrence of the joker expression $? in the pattern is substituted by JOKER_EXPR.
   * This is necessary because the C-parser cannot parse the pattern if it contains Dollar-Symbols.
   * The JOKER_EXPR must be a valid C-Identifier. It will be used to recognize the jokers in the generated AST.
   */
  private static final String JOKER_EXPR = "CPAchecker_AutomatonAnalysis_JokerExpression";
  private static final String NUMBERED_JOKER_EXPR = "CPAchecker_AutomatonAnalysis_JokerExpression_Num";
  private static final Pattern NUMBERED_JOKER_PATTERN = Pattern.compile("\\$\\d+");

  static ASTMatcher generatePatternAST(String pPattern, CParser parser) throws InvalidAutomatonException, InvalidConfigurationException {
    // $?-Jokers, $1-Jokers and function declaration
    String tmp = addFunctionDeclaration(replaceJokersInPattern(pPattern));

    return parse(tmp, parser).accept(ASTMatcherGenerator.INSTANCE);
  }

  static CStatement generateSourceAST(String pSource, CParser parser) throws InvalidAutomatonException, InvalidConfigurationException {
    String tmp = addFunctionDeclaration(pSource);

    return parse(tmp, parser);
  }

  static List<CStatement> generateSourceASTOfBlock(String pSource, CParser parser)
      throws InvalidAutomatonException, InvalidConfigurationException, CParserException {
    String tmp = addFunctionDeclaration(pSource);

    return parseBlockOfStatements(tmp, parser);
  }

  @VisibleForTesting
  static String replaceJokersInPattern(String pPattern) {
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
   * @throws InvalidConfigurationException
   */
  private static CStatement parse(String code, CParser parser) throws InvalidAutomatonException, InvalidConfigurationException {
    try {
      CAstNode statement = parser.parseSingleStatement(code);
      if (!(statement instanceof CStatement)) {
        throw new InvalidAutomatonException("Not a valid statement: " + statement.toASTString());
      }
      return (CStatement)statement;
    } catch (ParserException e) {
      throw new InvalidAutomatonException("Error during parsing C code \""
          + code + "\": " + e.getMessage());
    }
  }

  /**
   * Parse the assumption of a automaton, which are C assignments,
   * return statements or function calls, into a list of
   * CStatements with the Eclipse CDT parser. If an error occurs,
   * an empty list will be returned, and the error will be logged.
   *
   *
   * @param code The C code to parse.
   * @return The AST.
   * @throws InvalidAutomatonException
   * @throws InvalidConfigurationException
   * @throws CParserException
   */
  private static List<CStatement> parseBlockOfStatements(String code, CParser parser) throws InvalidAutomatonException, InvalidConfigurationException, CParserException {
    List<CAstNode> statements;

    statements = parser.parseStatements(code);

    for(CAstNode statement : statements) {
      if(!(statement instanceof CStatement)) {
        throw new InvalidAutomatonException("Code in assumption: <"
      + statement.toASTString() + "> is not a valid assumption.");
      }
    }

    Function<CAstNode, CStatement> function = new Function<CAstNode, CStatement>() {
      @Override
      public CStatement apply(CAstNode statement) {
        return (CStatement) statement;
      }
    };

    return ImmutableList.copyOf(Lists.transform(statements, function));
  }


  /**
   * The interface for a pre-compiled AST pattern.
   */
  static interface ASTMatcher {

    boolean matches(CAstNode pSource, AutomatonExpressionArguments pArgs);
  }

  /**
   * The visitor that generates a pre-compiled ASTMatcher from a pattern AST.
   */
  private static enum ASTMatcherGenerator implements CRightHandSideVisitor<ASTMatcher, InvalidAutomatonException>,
                                                     CStatementVisitor<ASTMatcher, InvalidAutomatonException> {

    INSTANCE;

    @Override
    public ASTMatcher visit(CIdExpression exp) throws InvalidAutomatonException {
      String name = exp.getName();

      if (name.equals(JOKER_EXPR)) {
        return JokerMatcher.INSTANCE;

      } else if (name.startsWith(NUMBERED_JOKER_EXPR)) {
        String s = name.substring(NUMBERED_JOKER_EXPR.length());
        int i = Integer.parseInt(s);
        return new NumberedJokerMatcher(i);

      } else {
        return new CIdExpressionMatcher(exp);
      }
    }

    @Override
    public ASTMatcher visit(CArraySubscriptExpression exp) throws InvalidAutomatonException {
      return new ArraySubscriptExpressionMatcher(exp, exp.getArrayExpression().accept(this), exp.getSubscriptExpression().accept(this));
    }

    @Override
    public ASTMatcher visit(CBinaryExpression exp) throws InvalidAutomatonException {
      return new BinaryExpressionMatcher(exp, exp.getOperand1().accept(this), exp.getOperand2().accept(this));
    }

    @Override
    public ASTMatcher visit(CCastExpression exp) throws InvalidAutomatonException {
      return new CastExpressionMatcher(exp, exp.getOperand().accept(this));
    }

    @Override
    public ASTMatcher visit(CComplexCastExpression exp) throws InvalidAutomatonException {
      return new ComplexCastExpressionMatcher(exp, exp.getOperand().accept(this));
    }

    @Override
    public ASTMatcher visit(CFieldReference exp) throws InvalidAutomatonException {
      return new FieldReferenceMatcher(exp, exp.getFieldOwner().accept(this));
    }

    @Override
    public ASTMatcher visit(CCharLiteralExpression exp) throws InvalidAutomatonException {
      return new ExpressionWithFieldMatcher<CCharLiteralExpression, Character>(CCharLiteralExpression.class, exp) {

        @Override
        protected Character getFieldValueFrom(CCharLiteralExpression pSource) {
          return pSource.getCharacter();
        }
      };
    }

    @Override
    public ASTMatcher visit(CFloatLiteralExpression exp) throws InvalidAutomatonException {
      return new ExpressionWithFieldMatcher<CFloatLiteralExpression, BigDecimal>(CFloatLiteralExpression.class, exp) {

        @Override
        protected BigDecimal getFieldValueFrom(CFloatLiteralExpression pSource) {
          return pSource.getValue();
        }
      };
    }

    @Override
    public ASTMatcher visit(CImaginaryLiteralExpression exp) throws InvalidAutomatonException {
      return new ExpressionWithTwoFieldsMatcher<CImaginaryLiteralExpression, CLiteralExpression, String>(CImaginaryLiteralExpression.class, exp) {

        @Override
        protected CLiteralExpression getFieldValue1From(CImaginaryLiteralExpression pSource) {
          return pSource.getValue();
        }

        @Override
        protected String getFieldValue2From(CImaginaryLiteralExpression pSource) {
          return pSource.getImaginaryString();
        }
      };
    }

    @Override
    public ASTMatcher visit(CIntegerLiteralExpression exp) throws InvalidAutomatonException {
      return new ExpressionWithFieldMatcher<CIntegerLiteralExpression, BigInteger>(CIntegerLiteralExpression.class, exp) {

        @Override
        protected BigInteger getFieldValueFrom(CIntegerLiteralExpression pSource) {
          return pSource.getValue();
        }
      };
    }

    @Override
    public ASTMatcher visit(CStringLiteralExpression exp) throws InvalidAutomatonException {
      return new ExpressionWithFieldMatcher<CStringLiteralExpression, String>(CStringLiteralExpression.class, exp) {

        @Override
        protected String getFieldValueFrom(CStringLiteralExpression pSource) {
          return pSource.getValue();
        }
      };
    }

    @Override
    public ASTMatcher visit(CTypeIdExpression exp) {
      return new TypeIdExpressionMatcher(exp);
    }

    @Override
    public ASTMatcher visit(CTypeIdInitializerExpression exp) throws InvalidAutomatonException {
      throw new InvalidAutomatonException("Type-id initializer expressions of the form " + exp.toASTString() + " are currently not supported in automata.");
    }

    @Override
    public ASTMatcher visit(CUnaryExpression exp) throws InvalidAutomatonException {
      return new UnaryExpressionMatcher(exp, exp.getOperand().accept(this));
    }

    @Override
    public ASTMatcher visit(CPointerExpression exp) throws InvalidAutomatonException {
      return new PointerExpressionMatcher(exp, exp.getOperand().accept(this));
    }

    @Override
    public ASTMatcher visit(CFunctionCallExpression exp) throws InvalidAutomatonException {
      List<ASTMatcher> parameterPatterns = new ArrayList<>(exp.getParameterExpressions().size());
      for (CExpression parameter : exp.getParameterExpressions()) {
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
    public ASTMatcher visit(CExpressionStatement stmt) throws InvalidAutomatonException {
      return new OneOperandExpressionMatcher<CExpressionStatement, Void>(
          CExpressionStatement.class, stmt, stmt.getExpression().accept(this)) {

        @Override
        protected CExpression getOperandFrom(CExpressionStatement pSource) {
          return pSource.getExpression();
        }
      };
    }

    private ASTMatcher visit(final CAssignment stmt) throws InvalidAutomatonException {
      final ASTMatcher leftHandSide = stmt.getLeftHandSide().accept(this);
      final ASTMatcher rightHandSide = stmt.getRightHandSide().accept(this);

      if (rightHandSide == JokerMatcher.INSTANCE) {
        // we don't care about right-hand side, it may be an expression or an assignment

        return new ASTMatcher() {
          @Override
          public boolean matches(CAstNode pSource, AutomatonExpressionArguments pArgs) {
            if (pSource instanceof CAssignment) {
              CAssignment source = (CAssignment)pSource;

              return leftHandSide.matches(source.getLeftHandSide(), pArgs);
            } else {
              return false;
            }
          }

          @Override
          public String toString() {
            return stmt.toASTString();
          }
        };

      } else {
        return new ASTMatcher() {
          @Override
          public boolean matches(CAstNode pSource, AutomatonExpressionArguments pArgs) {
            if (pSource instanceof CAssignment) {
              CAssignment source = (CAssignment)pSource;

              return leftHandSide.matches(source.getLeftHandSide(), pArgs)
                  && rightHandSide.matches(source.getRightHandSide(), pArgs);
            } else {
              return false;
            }
          }

          @Override
          public String toString() {
            return stmt.toASTString();
          }
        };
      }
    }

    @Override
    public ASTMatcher visit(final CExpressionAssignmentStatement stmt) throws InvalidAutomatonException {
      return visit((CAssignment)stmt);
    }

    @Override
    public ASTMatcher visit(CFunctionCallAssignmentStatement stmt) throws InvalidAutomatonException {
      return visit((CAssignment)stmt);
    }

    @Override
    public ASTMatcher visit(CFunctionCallStatement stmt) throws InvalidAutomatonException {
      return new OneOperandExpressionMatcher<CFunctionCallStatement, Void>(
          CFunctionCallStatement.class, stmt, stmt.getFunctionCallExpression().accept(this)) {

        @Override
        protected CRightHandSide getOperandFrom(CFunctionCallStatement pSource) {
          return pSource.getFunctionCallExpression();
        }
      };
    }
  }

  // several abstract helper implementations of ASTMatcher

  private static abstract class CheckedExpressionMatcher<T extends CAstNode> implements ASTMatcher {

    private final Class<T> cls;
    private final String rawSignature;

    protected CheckedExpressionMatcher(Class<T> pCls, T pPattern) {
      assert pCls.isInstance(pPattern);
      cls = pCls;
      rawSignature = pPattern.toASTString();
    }

    @Override
    public final boolean matches(CAstNode pSource, AutomatonExpressionArguments pArgs) {
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

  private static abstract class ExpressionWithFieldMatcher<T extends CAstNode, F> extends CheckedExpressionMatcher<T> {

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

  private static abstract class ExpressionWithTwoFieldsMatcher<T extends CAstNode, F, G> extends CheckedExpressionMatcher<T> {

    private final F field1;
    private final G field2;

    protected ExpressionWithTwoFieldsMatcher(Class<T> pCls, T pPattern) {
      super(pCls, pPattern);
      field1 = getFieldValue1From(pPattern);
      field2 = getFieldValue2From(pPattern);
    }

    @Override
    protected boolean matches2(T pSource, AutomatonExpressionArguments pArg) {
      return equal(field1, getFieldValue1From(pSource)) && equal(field2, getFieldValue2From(pSource));
    }

    protected F getFieldValue1From(T pSource) {
      return null;
    }

    protected G getFieldValue2From(T pSource) {
      return null;
    }
  }

  private static abstract class OneOperandExpressionMatcher<T extends CAstNode, F> extends ExpressionWithFieldMatcher<T, F> {

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

    protected abstract CRightHandSide getOperandFrom(T pSource);
  }

  private static abstract class OneOperandExpressionWithTwoFieldsMatcher<T extends CAstNode, F, G> extends ExpressionWithTwoFieldsMatcher<T, F, G> {

    private final ASTMatcher operand;

    protected OneOperandExpressionWithTwoFieldsMatcher(Class<T> pCls, T pPattern, ASTMatcher pOperand) {
      super(pCls, pPattern);
      operand = pOperand;
    }

    @Override
    protected boolean matches2(T pSource, AutomatonExpressionArguments pArgs) {
      return super.matches2(pSource, pArgs)
          && operand.matches(getOperandFrom(pSource), pArgs);
    }

    protected abstract CRightHandSide getOperandFrom(T pSource);
  }

  private static abstract class TwoOperandExpressionMatcher<T extends CAstNode, F> extends ExpressionWithFieldMatcher<T, F> {

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

    protected abstract CRightHandSide getOperand1From(T pSource);
    protected abstract CRightHandSide getOperand2From(T pSource);
  }

  // several concrete implementations of ASTMatcher

  private static enum JokerMatcher implements ASTMatcher {
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

  private static class ArraySubscriptExpressionMatcher extends TwoOperandExpressionMatcher<CArraySubscriptExpression, Void> {

    public ArraySubscriptExpressionMatcher(CArraySubscriptExpression pPattern, ASTMatcher pOperand1, ASTMatcher pOperand2) {
      super(CArraySubscriptExpression.class, pPattern, pOperand1, pOperand2);
    }

    @Override
    protected CExpression getOperand1From(CArraySubscriptExpression pSource) {
      return pSource.getArrayExpression();
    }

    @Override
    protected CExpression getOperand2From(CArraySubscriptExpression pSource) {
      return pSource.getSubscriptExpression();
    }
  }

  private static class BinaryExpressionMatcher extends TwoOperandExpressionMatcher<CBinaryExpression, BinaryOperator> {

    public BinaryExpressionMatcher(CBinaryExpression pPattern, ASTMatcher pOperand1, ASTMatcher pOperand2) {
      super(CBinaryExpression.class, pPattern, pOperand1, pOperand2);
    }

    @Override
    protected CExpression getOperand1From(CBinaryExpression pSource) {
      return pSource.getOperand1();
    }

    @Override
    protected CExpression getOperand2From(CBinaryExpression pSource) {
      return pSource.getOperand2();
    }

    @Override
    protected BinaryOperator getFieldValueFrom(CBinaryExpression pSource) {
      return pSource.getOperator();
    }
  }

  private static class CastExpressionMatcher extends OneOperandExpressionMatcher<CCastExpression, CType> {

    public CastExpressionMatcher(CCastExpression pPattern, ASTMatcher pOperand) {
      super(CCastExpression.class, pPattern, pOperand);
    }

    @Override
    protected CExpression getOperandFrom(CCastExpression pSource) {
      return pSource.getOperand();
    }

    @Override
    protected CType getFieldValueFrom(CCastExpression pSource) {
      return pSource.getExpressionType();
    }
  }

  private static class ComplexCastExpressionMatcher extends OneOperandExpressionWithTwoFieldsMatcher<CComplexCastExpression, Boolean, CType> {

    public ComplexCastExpressionMatcher(CComplexCastExpression pPattern, ASTMatcher pOperand) {
      super(CComplexCastExpression.class, pPattern, pOperand);
    }

    @Override
    protected CExpression getOperandFrom(CComplexCastExpression pSource) {
      return pSource.getOperand();
    }

    @Override
    protected Boolean getFieldValue1From(CComplexCastExpression pSource) {
      return pSource.isRealCast();
    }

    @Override
    protected CType getFieldValue2From(CComplexCastExpression pSource) {
      return pSource.getType();
    }
  }

  private static class FieldReferenceMatcher extends OneOperandExpressionMatcher<CFieldReference, String> {

    public FieldReferenceMatcher(CFieldReference pPattern, ASTMatcher pOperand) {
      super(CFieldReference.class, pPattern, pOperand);
    }

    @Override
    protected CExpression getOperandFrom(CFieldReference pSource) {
      return pSource.getFieldOwner();
    }

    @Override
    protected String getFieldValueFrom(CFieldReference pSource) {
      return pSource.getFieldName();
    }
  }

  private static class CIdExpressionMatcher extends ExpressionWithFieldMatcher<CIdExpression, String> {

    public CIdExpressionMatcher(CIdExpression pPattern) {
      super(CIdExpression.class, pPattern);
    }

    @Override
    protected String getFieldValueFrom(CIdExpression pSource) {
      return pSource.getName();
    }
  }

  private static class UnaryExpressionMatcher extends OneOperandExpressionMatcher<CUnaryExpression, UnaryOperator> {

    public UnaryExpressionMatcher(CUnaryExpression pPattern, ASTMatcher pOperand) {
      super(CUnaryExpression.class, pPattern, pOperand);
    }

    @Override
    protected CExpression getOperandFrom(CUnaryExpression pSource) {
      return pSource.getOperand();
    }

    @Override
    protected UnaryOperator getFieldValueFrom(CUnaryExpression pSource) {
      return pSource.getOperator();
    }
  }

  private static class PointerExpressionMatcher extends OneOperandExpressionMatcher<CPointerExpression, Void> {

    public PointerExpressionMatcher(CPointerExpression pPattern, ASTMatcher pOperand) {
      super(CPointerExpression.class, pPattern, pOperand);
    }

    @Override
    protected CExpression getOperandFrom(CPointerExpression pSource) {
      return pSource.getOperand();
    }
  }

  private static class TypeIdExpressionMatcher extends ExpressionWithFieldMatcher<CTypeIdExpression, CType> {

    private final TypeIdOperator operator;

    public TypeIdExpressionMatcher(CTypeIdExpression pPattern) {
      super(CTypeIdExpression.class, pPattern);
      operator = pPattern.getOperator();
    }

    @Override
    protected boolean matches2(CTypeIdExpression pSource, AutomatonExpressionArguments pArg) {
      return equal(operator, pSource.getOperator())
          && super.matches2(pSource, pArg);
    }

    @Override
    protected CType getFieldValueFrom(CTypeIdExpression pSource) {
      return pSource.getType();
    }
  }

  private static class FunctionCallWildcardExpressionMatcher extends OneOperandExpressionMatcher<CFunctionCallExpression, Void> {

    // this matcher is for patterns like foo($?)
    // it compares only the function name and ignores any parameters

    protected FunctionCallWildcardExpressionMatcher(CFunctionCallExpression pPattern, ASTMatcher pFunctionNameExpression) {
      super(CFunctionCallExpression.class, pPattern, pFunctionNameExpression);
    }

    @Override
    protected CExpression getOperandFrom(CFunctionCallExpression pSource) {
      return pSource.getFunctionNameExpression();
    }
  }

  private static class FunctionCallExpressionMatcher extends FunctionCallWildcardExpressionMatcher {

    private final List<ASTMatcher> parameterPatterns;

    protected FunctionCallExpressionMatcher(CFunctionCallExpression pPattern, ASTMatcher pFunctionNameExpression, List<ASTMatcher> pParameterPatterns) {
      super(pPattern, pFunctionNameExpression);
      parameterPatterns = pParameterPatterns;
    }

    @Override
    protected boolean matches2(CFunctionCallExpression pSource, AutomatonExpressionArguments pArg) {
      if (!super.matches2(pSource, pArg)) {
        return false;
      }

      if (parameterPatterns.size() != pSource.getParameterExpressions().size()) {
        return false;
      }

      for (Pair<ASTMatcher, CExpression> parameters : zipList(parameterPatterns, pSource.getParameterExpressions())) {
        if (!parameters.getFirst().matches(parameters.getSecond(), pArg)) {
          return false;
        }
      }
      return true;
    }

    @Override
    protected CExpression getOperandFrom(CFunctionCallExpression pSource) {
      return pSource.getFunctionNameExpression();
    }
  }
}
