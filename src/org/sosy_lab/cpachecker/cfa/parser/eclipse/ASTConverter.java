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
package org.sosy_lab.cpachecker.cfa.parser.eclipse;

import static java.lang.Character.isDigit;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.IASTArrayDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTArrayModifier;
import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTConditionalExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionList;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTInitializerExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializerList;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPointer;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTProblemDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTProblemStatement;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTypeId;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CDummyType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType.ElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNamedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedef;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

@SuppressWarnings("deprecation") // several methods are deprecated in CDT 7 but still working
class ASTConverter {

  private final LogManager logger;

  private final boolean ignoreCasts;

  private Scope scope;

  private LinkedList<CAstNode> preSideAssignments = new LinkedList<CAstNode>();
  private LinkedList<CAstNode> postSideAssignments = new LinkedList<CAstNode>();
  private IASTConditionalExpression conditionalExpression = null;
  private CIdExpression conditionalTemporaryVariable = null;


  public ASTConverter(Scope pScope, boolean pIgnoreCasts, LogManager pLogger) {
    scope = pScope;
    ignoreCasts = pIgnoreCasts;
    logger = pLogger;
  }

  private static void check(boolean assertion, String msg, IASTNode astNode) throws CFAGenerationRuntimeException {
    if (!assertion) {
      throw new CFAGenerationRuntimeException(msg, astNode);
    }
  }

  public int numberOfPreSideAssignments(){
    return preSideAssignments.size();
  }


  public CAstNode getNextPreSideAssignment() {
    return preSideAssignments.removeFirst();
  }

  public int numberOfPostSideAssignments(){
    return postSideAssignments.size();
  }

  public CAstNode getNextPostSideAssignment() {
    return postSideAssignments.removeFirst();
  }

  public void resetConditionalExpression() {
    conditionalExpression = null;
  }

  public IASTConditionalExpression getConditionalExpression() {
    return conditionalExpression;
  }

  public CIdExpression getConditionalTemporaryVariable() {
    return conditionalTemporaryVariable;
  }

  private static final Set<BinaryOperator> BOOLEAN_BINARY_OPERATORS = ImmutableSet.of(
      BinaryOperator.EQUALS,
      BinaryOperator.NOT_EQUALS,
      BinaryOperator.GREATER_EQUAL,
      BinaryOperator.GREATER_THAN,
      BinaryOperator.LESS_EQUAL,
      BinaryOperator.LESS_THAN,
      BinaryOperator.LOGICAL_AND,
      BinaryOperator.LOGICAL_OR);

  private boolean isBooleanExpression(CExpression e) {
    if (e instanceof CBinaryExpression) {
      return BOOLEAN_BINARY_OPERATORS.contains(((CBinaryExpression)e).getOperator());

    } else if (e instanceof CUnaryExpression) {
      return ((CUnaryExpression) e).getOperator() == UnaryOperator.NOT;

    } else {
      return false;
    }
  }

  public CExpression convertBooleanExpression(IASTExpression e){

    CExpression exp = convertExpressionWithoutSideEffects(e);
    if (!isBooleanExpression(exp)) {

      // TODO: probably the type of the zero is not always correct
      CExpression zero = new CIntegerLiteralExpression(exp.getFileLocation(), exp.getExpressionType(), BigInteger.ZERO);
      return new CBinaryExpression(exp.getFileLocation(), exp.getExpressionType(), exp, zero, BinaryOperator.NOT_EQUALS);
    }

    return exp;
  }

  public CExpression convertExpressionWithoutSideEffects(
      IASTExpression e) {

    CAstNode node = convertExpressionWithSideEffects(e);
    if (node == null || node instanceof CExpression) {
      return (CExpression) node;

    } else if (node instanceof CFunctionCallExpression) {
      return addSideassignmentsForExpressionsWithoutSideEffects(node, e);

    } else if(e instanceof IASTUnaryExpression && (((IASTUnaryExpression)e).getOperator() == IASTUnaryExpression.op_postFixDecr
                                                   || ((IASTUnaryExpression)e).getOperator() == IASTUnaryExpression.op_postFixIncr)) {
      return addSideAssignmentsForUnaryExpressions(e, ((CAssignment)node).getLeftHandSide(), node.getFileLocation(), convert(e.getExpressionType()), ((CBinaryExpression)((CAssignment)node).getRightHandSide()).getOperator());

    } else if (node instanceof CAssignment) {
      preSideAssignments.add(node);
      return ((CAssignment) node).getLeftHandSide();

    } else {
      throw new AssertionError("unknown expression " + node);
    }
  }

  private CExpression addSideassignmentsForExpressionsWithoutSideEffects(CAstNode node,
                                                                            IASTExpression e){
    CIdExpression tmp = createTemporaryVariable(e, null);

    preSideAssignments.add(new CFunctionCallAssignmentStatement(convert(e.getFileLocation()),
                                                                tmp,
                                                                (CFunctionCallExpression) node));
    return tmp;
  }

  private CIdExpression addSideAssignmentsForUnaryExpressions(IASTExpression e,
                                                              CExpression exp,
                                                              CFileLocation fileLoc,
                                                              CType type,
                                                              BinaryOperator op) {
    CIdExpression tmp = createTemporaryVariable(e, null);
    preSideAssignments.add(new CExpressionAssignmentStatement(fileLoc, tmp, exp));


    CExpression one = new CIntegerLiteralExpression(fileLoc, type, BigInteger.ONE);
    CBinaryExpression postExp = new CBinaryExpression(fileLoc, type, exp, one, op);
    preSideAssignments.add(new CExpressionAssignmentStatement(fileLoc, exp, postExp));

    return tmp;
}

  protected CAstNode convertExpressionWithSideEffects(IASTExpression e) {
    assert !(e instanceof CExpression);

    if (e == null) {
      return null;

    } else if (e instanceof IASTArraySubscriptExpression) {
      return convert((IASTArraySubscriptExpression)e);

    } else if (e instanceof IASTBinaryExpression) {
      return convert((IASTBinaryExpression)e);

    } else if (e instanceof IASTCastExpression) {
      return convert((IASTCastExpression)e);

    } else if (e instanceof IASTFieldReference) {
      return convert((IASTFieldReference)e);

    } else if (e instanceof IASTFunctionCallExpression) {
      return convert((IASTFunctionCallExpression)e);

    } else if (e instanceof IASTIdExpression) {
      return convert((IASTIdExpression)e);

    } else if (e instanceof IASTLiteralExpression) {
      return convert((IASTLiteralExpression)e);

    } else if (e instanceof IASTUnaryExpression) {
      return convert((IASTUnaryExpression)e);

    } else if (e instanceof IASTTypeIdExpression) {
      return convert((IASTTypeIdExpression)e);

    } else if (e instanceof IASTConditionalExpression) {
      return convert((IASTConditionalExpression)e);

    } else {
      throw new CFAGenerationRuntimeException("Unknown expression type " + e.getClass().getSimpleName(), e);
    }
  }

  private CAstNode convert(IASTConditionalExpression e) {
    CIdExpression tmp = createTemporaryVariable(e, null);
    conditionalTemporaryVariable = tmp;
    conditionalExpression = e;
    return tmp;
  }

  private CArraySubscriptExpression convert(IASTArraySubscriptExpression e) {
    return new CArraySubscriptExpression(convert(e.getFileLocation()), convert(e.getExpressionType()), convertExpressionWithoutSideEffects(e.getArrayExpression()), convertExpressionWithoutSideEffects(e.getSubscriptExpression()));
  }

  /**
   * creates temporary variables with increasing numbers
   */
  private CIdExpression createTemporaryVariable(IASTExpression e, String name) {
    boolean nameWasInUse = true;
    if (name == null) {
      nameWasInUse = false;
      name = "__CPAchecker_TMP_";
      int i = 0;
      while (scope.variableNameInUse(name + i, name + i)) {
        i++;
      }
      name += i;
    }

    CVariableDeclaration decl = new CVariableDeclaration(convert(e.getFileLocation()),
                                               false,
                                               CStorageClass.AUTO,
                                               convert(e.getExpressionType()),
                                               name,
                                               name,
                                               null);

    if (!nameWasInUse) {
    scope.registerDeclaration(decl);
    preSideAssignments.add(decl);
    }
    CIdExpression tmp = new CIdExpression(convert(e.getFileLocation()),
                                                convert(e.getExpressionType()),
                                                name,
                                                decl);
    return tmp;
  }

  private CAstNode convert(IASTBinaryExpression e) {
    CFileLocation fileLoc = convert(e.getFileLocation());
    CType type = convert(e.getExpressionType());
    CExpression leftHandSide = convertExpressionWithoutSideEffects(e.getOperand1());

    Pair<BinaryOperator, Boolean> opPair = convertBinaryOperator(e);
    BinaryOperator op = opPair.getFirst();
    boolean isAssign = opPair.getSecond();

    if (isAssign) {

      if (op == null) {
        // a = b
        CAstNode rightHandSide = convertExpressionWithSideEffects(e.getOperand2()); // right-hand side may have a function call


        if (rightHandSide instanceof CExpression) {
          // a = b
          return new CExpressionAssignmentStatement(fileLoc, leftHandSide, (CExpression)rightHandSide);

        } else if (rightHandSide instanceof CFunctionCallExpression) {
          // a = f()
          return new CFunctionCallAssignmentStatement(fileLoc, leftHandSide, (CFunctionCallExpression)rightHandSide);

        } else if (rightHandSide instanceof CAssignment) {
          preSideAssignments.add(rightHandSide);
          return new CExpressionAssignmentStatement(fileLoc, leftHandSide, ((CAssignment) rightHandSide).getLeftHandSide());
        } else {
          throw new CFAGenerationRuntimeException("Expression is not free of side-effects", e);
        }

      } else {
        // a += b etc.
        CExpression rightHandSide = convertExpressionWithoutSideEffects(e.getOperand2());

        // first create expression "a + b"
        CBinaryExpression exp = new CBinaryExpression(fileLoc, type, leftHandSide, rightHandSide, op);

        // and now the assignment
        return new CExpressionAssignmentStatement(fileLoc, leftHandSide, exp);
      }

    } else {
      CExpression rightHandSide = convertExpressionWithoutSideEffects(e.getOperand2());
      return new CBinaryExpression(fileLoc, type, leftHandSide, rightHandSide, op);
    }
  }

  private Pair<BinaryOperator, Boolean> convertBinaryOperator(IASTBinaryExpression e) {
    boolean isAssign = false;
    BinaryOperator operator;

    switch (e.getOperator()) {
    case IASTBinaryExpression.op_multiply:
      operator = BinaryOperator.MULTIPLY;
      break;
    case IASTBinaryExpression.op_divide:
      operator = BinaryOperator.DIVIDE;
      break;
    case IASTBinaryExpression.op_modulo:
      operator = BinaryOperator.MODULO;
      break;
    case IASTBinaryExpression.op_plus:
      operator = BinaryOperator.PLUS;
      break;
    case IASTBinaryExpression.op_minus:
      operator = BinaryOperator.MINUS;
      break;
    case IASTBinaryExpression.op_shiftLeft:
      operator = BinaryOperator.SHIFT_LEFT;
      break;
    case IASTBinaryExpression.op_shiftRight:
      operator = BinaryOperator.SHIFT_RIGHT;
      break;
    case IASTBinaryExpression.op_lessThan:
      operator = BinaryOperator.LESS_THAN;
      break;
    case IASTBinaryExpression.op_greaterThan:
      operator = BinaryOperator.GREATER_THAN;
      break;
    case IASTBinaryExpression.op_lessEqual:
      operator = BinaryOperator.LESS_EQUAL;
      break;
    case IASTBinaryExpression.op_greaterEqual:
      operator = BinaryOperator.GREATER_EQUAL;
      break;
    case IASTBinaryExpression.op_binaryAnd:
      operator = BinaryOperator.BINARY_AND;
      break;
    case IASTBinaryExpression.op_binaryXor:
      operator = BinaryOperator.BINARY_XOR;
      break;
    case IASTBinaryExpression.op_binaryOr:
      operator = BinaryOperator.BINARY_OR;
      break;
    case IASTBinaryExpression.op_logicalAnd:
      operator = BinaryOperator.LOGICAL_AND;
      break;
    case IASTBinaryExpression.op_logicalOr:
      operator = BinaryOperator.LOGICAL_OR;
      break;
    case IASTBinaryExpression.op_assign:
      operator = null;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_multiplyAssign:
      operator = BinaryOperator.MULTIPLY;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_divideAssign:
      operator = BinaryOperator.DIVIDE;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_moduloAssign:
      operator = BinaryOperator.MODULO;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_plusAssign:
      operator = BinaryOperator.PLUS;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_minusAssign:
      operator = BinaryOperator.MINUS;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_shiftLeftAssign:
      operator = BinaryOperator.SHIFT_LEFT;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_shiftRightAssign:
      operator = BinaryOperator.SHIFT_RIGHT;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_binaryAndAssign:
      operator = BinaryOperator.BINARY_AND;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_binaryXorAssign:
      operator = BinaryOperator.BINARY_XOR;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_binaryOrAssign:
      operator = BinaryOperator.BINARY_OR;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_equals:
      operator = BinaryOperator.EQUALS;
      break;
    case IASTBinaryExpression.op_notequals:
      operator = BinaryOperator.NOT_EQUALS;
      break;
    default:
      throw new CFAGenerationRuntimeException("Unknown binary operator", e);
    }

    return Pair.of(operator, isAssign);
  }

  private CAstNode convert(IASTCastExpression e) {
    if (ignoreCasts) {
      return convertExpressionWithSideEffects(e.getOperand());
    } else {
      return new CCastExpression(convert(e.getFileLocation()), convert(e.getExpressionType()), convertExpressionWithoutSideEffects(e.getOperand()), convert(e.getTypeId()));
    }
  }

  private CFieldReference convert(IASTFieldReference e) {
    return new CFieldReference(convert(e.getFileLocation()), convert(e.getExpressionType()), convert(e.getFieldName()), convertExpressionWithoutSideEffects(e.getFieldOwner()), e.isPointerDereference());
  }

  private CFunctionCallExpression convert(IASTFunctionCallExpression e) {
    IASTExpression p = e.getParameterExpression();

    List<CExpression> params;
    if (p instanceof IASTExpressionList) {
      params = convert((IASTExpressionList)p);

    } else {
      params = new ArrayList<CExpression>();
      if (p != null) {
        params.add(convertExpressionWithoutSideEffects(p));
      }
    }

    CExpression functionName = convertExpressionWithoutSideEffects(e.getFunctionNameExpression());
    CSimpleDeclaration declaration = null;

    if (functionName instanceof CIdExpression) {
      CIdExpression idExpression = (CIdExpression)functionName;
      String name = idExpression.getName();
      declaration = scope.lookupFunction(name);

      if (idExpression.getDeclaration() != null) {
        // clone idExpression because the declaration in it is wrong
        // (it's the declaration of an equally named variable)
        // TODO this is ugly

        functionName = new CIdExpression(idExpression.getFileLocation(), idExpression.getExpressionType(), name, declaration);
      }
    }

    return new CFunctionCallExpression(convert(e.getFileLocation()), convert(e.getExpressionType()), functionName, params, declaration);
  }

  private List<CExpression> convert(IASTExpressionList es) {
    List<CExpression> result = new ArrayList<CExpression>(es.getExpressions().length);
    for (IASTExpression expression : es.getExpressions()) {
      result.add(convertExpressionWithoutSideEffects(expression));
    }
    return result;
  }

  private CIdExpression convert(IASTIdExpression e) {
    String name = convert(e.getName());
    CSimpleDeclaration declaration = scope.lookupVariable(name);
    if (declaration != null) {
      name = declaration.getName();
    }
    return new CIdExpression(convert(e.getFileLocation()), convert(e.getExpressionType()), name, declaration);
  }

  private CLiteralExpression convert(IASTLiteralExpression e) {
    CFileLocation fileLoc = convert(e.getFileLocation());
    CType type = convert(e.getExpressionType());

    String valueStr = String.valueOf(e.getValue());
    switch (e.getKind()) {
    case IASTLiteralExpression.lk_char_constant:
      return new CCharLiteralExpression(fileLoc, type, parseCharacterLiteral(valueStr, e));

    case IASTLiteralExpression.lk_integer_constant:
      return new CIntegerLiteralExpression(fileLoc, type, parseIntegerLiteral(valueStr, e));

    case IASTLiteralExpression.lk_float_constant:
      BigDecimal value;
      try {
        value = new BigDecimal(valueStr);
      } catch (NumberFormatException nfe1) {
        try {
          // this might be a hex floating point literal
          // BigDecimal doesn't support this, but Double does
          // TODO handle hex floating point literals that are too large for Double
          value = BigDecimal.valueOf(Double.parseDouble(valueStr));
        } catch (NumberFormatException nfe2) {
          throw new CFAGenerationRuntimeException("illegal floating point literal", e);
        }
      }

      return new CFloatLiteralExpression(fileLoc, type, value);

    case IASTLiteralExpression.lk_string_literal:
      return new CStringLiteralExpression(fileLoc, type, valueStr);

    default:
      throw new CFAGenerationRuntimeException("Unknown literal", e);
    }
  }

  char parseCharacterLiteral(String s, IASTNode e) {
    check(s.length() >= 3, "invalid character literal (too short)", e);
    check(s.charAt(0) == '\'' && s.charAt(s.length()-1) == '\'', "character literal without quotation marks", e);
    s = s.substring(1, s.length()-1); // remove the surrounding quotation marks ''

    char result;
    if (s.length() == 1) {
      result = s.charAt(0);
      check(result != '\\', "invalid quoting sequence", e);

    } else {
      check(s.charAt(0) == '\\', "character literal too long", e);
      // quoted character literal
      s = s.substring(1); // remove leading backslash \
      check(s.length() >= 1, "invalid quoting sequence", e);

      final char c = s.charAt(0);
      if (c == 'x' || c == 'X') {
        // something like '\xFF'
        s = s.substring(1); // remove leading x
        check(s.length() > 0 && s.length() <= 3, "character literal with illegal hex number", e);
        try {
          result = (char) Integer.parseInt(s, 16);
          check(result <= 0xFF, "hex escape sequence out of range", e);
        } catch (NumberFormatException _) {
          throw new CFAGenerationRuntimeException("character literal with illegal hex number", e);
        }

      } else if (isDigit(c)) {
        // something like '\000'
        check(s.length() <= 3, "character literal with illegal octal number", e);
        try {
          result = (char)Integer.parseInt(s, 8);
          check(result <= 0xFF, "octal escape sequence out of range", e);
        } catch (NumberFormatException _) {
          throw new CFAGenerationRuntimeException("character literal with illegal octal number", e);
        }

      } else {
        // something like '\n'
        check(s.length() == 1, "character literal too long", e);
        switch (c) {
        case 'b'  : result = '\b'; break;
        case 't'  : result = '\t'; break;
        case 'v'  : result = 11; break;
        case 'n'  : result = '\n'; break;
        case 'f'  : result = '\f'; break;
        case 'r'  : result = '\r'; break;
        case '"'  : result = '\"'; break;
        case '\'' : result = '\''; break;
        case '\\' : result = '\\'; break;
        default   : throw new CFAGenerationRuntimeException("unknown character literal", e);
        }
      }
    }
    return result;
  }

  BigInteger parseIntegerLiteral(String s, IASTNode e) {
    // this might have some modifiers attached (e.g. 0ULL), we have to get rid of them
    int last = s.length()-1;
    int bits = 32;
    boolean signed = true;

    if (s.charAt(last) == 'L' || s.charAt(last) == 'l' ) {
      last--;
      // one 'L' is equal to no 'L' (TODO this assumes a 32bit machine)
    }
    if (s.charAt(last) == 'L' || s.charAt(last) == 'l') {
      last--;
      bits = 64; // two 'L' are a long long
    }
    if (s.charAt(last) == 'U' || s.charAt(last) == 'u') {
      last--;
      signed = false;
    }

    s = s.substring(0, last+1);
    BigInteger result;
    try {
      if (s.startsWith("0x") || s.startsWith("0X")) {
        // this should be in hex format, remove "0x" from the string
        s = s.substring(2);
        result = new BigInteger(s, 16);

      } else if (s.startsWith("0")) {
        result = new BigInteger(s, 8);

      } else {
        result = new BigInteger(s, 10);
      }
    } catch (NumberFormatException _) {
      throw new CFAGenerationRuntimeException("invalid number", e);
    }
    check(result.compareTo(BigInteger.ZERO) >= 0, "invalid number", e);

    // clear the bits that don't fit in the type
    // a BigInteger with the lowest "bits" bits set to one (e. 2^32-1 or 2^64-1)
    BigInteger mask = BigInteger.ZERO.setBit(bits).subtract(BigInteger.ONE);
    result = result.and(mask);
    assert result.bitLength() <= bits;

    // compute twos complement if necessary
    if (signed && result.testBit(bits-1)) {
      // highest bit is set
      result = result.clearBit(bits-1);

      // a BigInteger for -2^(bits-1) (e.g. -2^-31 or -2^-63)
      BigInteger minValue = BigInteger.ZERO.setBit(bits-1).negate();

      result = minValue.add(result);
    }

    return result;
  }

  private CAstNode convert(IASTUnaryExpression e) {
    CExpression operand = convertExpressionWithoutSideEffects(e.getOperand());

    if (e.getOperator() == IASTUnaryExpression.op_bracketedPrimary) {
      return operand;
    }

    CFileLocation fileLoc = convert(e.getFileLocation());
    CType type = convert(e.getExpressionType());


    switch (e.getOperator()) {
    case IASTUnaryExpression.op_prefixIncr:
    case IASTUnaryExpression.op_prefixDecr:
      // instead of ++x, create "x = x+1"

      BinaryOperator preOp;
      switch (e.getOperator()) {
      case IASTUnaryExpression.op_prefixIncr:
        preOp = BinaryOperator.PLUS;
        break;
      case IASTUnaryExpression.op_prefixDecr:
        preOp = BinaryOperator.MINUS;
        break;
      default: throw new AssertionError();
      }
      CExpression one = new CIntegerLiteralExpression(fileLoc, type, BigInteger.ONE);
      CBinaryExpression preExp = new CBinaryExpression(fileLoc, type, operand, one, preOp);

      return new CExpressionAssignmentStatement(fileLoc, operand, preExp);

    case IASTUnaryExpression.op_postFixIncr:
    case IASTUnaryExpression.op_postFixDecr:
      // instead of x++ create "x = x + 1"

      BinaryOperator postOp;
      switch (e.getOperator()) {
      case IASTUnaryExpression.op_postFixIncr:
        postOp = BinaryOperator.PLUS;
        break;
      case IASTUnaryExpression.op_postFixDecr:
        postOp = BinaryOperator.MINUS;
        break;
      default: throw new AssertionError();
      }

      CExpression postOne = new CIntegerLiteralExpression(fileLoc, type, BigInteger.ONE);
      CBinaryExpression postExp = new CBinaryExpression(fileLoc, type, operand, postOne, postOp);
      return new CExpressionAssignmentStatement(fileLoc, operand, postExp);

    default:
      return new CUnaryExpression(fileLoc, type, operand, convertUnaryOperator(e));
    }
  }

  private UnaryOperator convertUnaryOperator(IASTUnaryExpression e) {
    switch (e.getOperator()) {
    case IASTUnaryExpression.op_amper:
      return UnaryOperator.AMPER;
    case IASTUnaryExpression.op_minus:
      return UnaryOperator.MINUS;
    case IASTUnaryExpression.op_not:
      return UnaryOperator.NOT;
    case IASTUnaryExpression.op_plus:
      return UnaryOperator.PLUS;
    case IASTUnaryExpression.op_sizeof:
      return UnaryOperator.SIZEOF;
    case IASTUnaryExpression.op_star:
      return UnaryOperator.STAR;
    case IASTUnaryExpression.op_tilde:
      return UnaryOperator.TILDE;
    default:
      throw new CFAGenerationRuntimeException("Unknown unary operator", e);
    }
  }

  private CTypeIdExpression convert(IASTTypeIdExpression e) {
    return new CTypeIdExpression(convert(e.getFileLocation()), convert(e.getExpressionType()), convertTypeIdOperator(e), convert(e.getTypeId()));
  }

  private TypeIdOperator convertTypeIdOperator(IASTTypeIdExpression e) {
    switch (e.getOperator()) {
    case IASTTypeIdExpression.op_alignof:
      return TypeIdOperator.ALIGNOF;
    case IASTTypeIdExpression.op_sizeof:
      return TypeIdOperator.SIZEOF;
    case IASTTypeIdExpression.op_typeid:
      return TypeIdOperator.TYPEID;
    case IASTTypeIdExpression.op_typeof:
      return TypeIdOperator.TYPEOF;
    default:
      throw new CFAGenerationRuntimeException("Unknown type id operator", e);
    }
  }

  public CAstNode convert(final IASTStatement s) {

    if (s instanceof IASTExpressionStatement) {
      return convert((IASTExpressionStatement) s);

    } else if (s instanceof IASTReturnStatement) {
      return convert((IASTReturnStatement) s);

    } else if (s instanceof IASTProblemStatement) {
      throw new CFAGenerationRuntimeException((IASTProblemStatement)s);

    } else {
      throw new CFAGenerationRuntimeException("unknown statement: " + s.getClass(), s);
    }
  }

  public CStatement convert(final IASTExpressionStatement s) {
    CAstNode node = convertExpressionWithSideEffects(s.getExpression());

    if (node instanceof CExpressionAssignmentStatement) {
      return (CExpressionAssignmentStatement)node;

    } else if (node instanceof CFunctionCallAssignmentStatement) {
      return (CFunctionCallAssignmentStatement)node;

    } else if (node instanceof CFunctionCallExpression) {
      return new CFunctionCallStatement(convert(s.getFileLocation()), (CFunctionCallExpression)node);

    } else if (node instanceof CExpression) {
      return new CExpressionStatement(convert(s.getFileLocation()), (CExpression)node);

    } else {
      throw new AssertionError();
    }
  }

  public CReturnStatement convert(final IASTReturnStatement s) {
    return new CReturnStatement(convert(s.getFileLocation()), convertExpressionWithoutSideEffects(s.getReturnValue()));
  }

  public CFunctionDeclaration convert(final IASTFunctionDefinition f) {
    Pair<CStorageClass, ? extends CType> specifier = convert(f.getDeclSpecifier());

    CStorageClass cStorageClass = specifier.getFirst();
    if (!(cStorageClass == CStorageClass.AUTO
        || cStorageClass == CStorageClass.STATIC
        || cStorageClass == CStorageClass.EXTERN)) {
      // storage class static is the same as auto, just with reduced visibility to a single compilation unit,
      // and as we only handle single compilation units, we can ignore it. A storage class extern associated
      // with a function definition, while superfluous, unless it's an inline function, is allowed, too.
      throw new CFAGenerationRuntimeException("Unsupported storage class for function definition", f);
    }

    Triple<CType, CInitializer, String> declarator = convert(f.getDeclarator(), specifier.getSecond());
    if (!(declarator.getFirst() instanceof CFunctionType)) {
      throw new CFAGenerationRuntimeException("Unsupported nested declarator for function definition", f);
    }
    if (declarator.getSecond() != null) {
      throw new CFAGenerationRuntimeException("Unsupported initializer for function definition", f);
    }
    if (declarator.getThird() == null) {
      throw new CFAGenerationRuntimeException("Missing name for function definition", f);
    }

    CFunctionType declSpec = (CFunctionType)declarator.getFirst();
    String name = declarator.getThird();

    CFileLocation fileLoc = convert(f.getFileLocation());

    return new CFunctionDeclaration(fileLoc, declSpec, name);
  }

  public List<CDeclaration> convert(final IASTSimpleDeclaration d) {
    CFileLocation fileLoc = convert(d.getFileLocation());
    Pair<CStorageClass, ? extends CType> specifier = convert(d.getDeclSpecifier());
    CStorageClass cStorageClass = specifier.getFirst();
    CType type = specifier.getSecond();

    List<CDeclaration> result;
    IASTDeclarator[] declarators = d.getDeclarators();
    if (declarators == null || declarators.length == 0) {
      // declaration without declarator, i.e. struct prototype
      CDeclaration newD = createDeclaration(fileLoc, cStorageClass, type, null);
      result = Collections.singletonList(newD);

    } else if (declarators.length == 1) {
      CDeclaration newD = createDeclaration(fileLoc, cStorageClass, type, declarators[0]);
      result = Collections.singletonList(newD);

    } else {
      result = new ArrayList<CDeclaration>(declarators.length);
      for (IASTDeclarator c : declarators) {

        result.add(createDeclaration(fileLoc, cStorageClass, type, c));
      }
    }

    return result;
  }

  private CDeclaration createDeclaration(CFileLocation fileLoc, CStorageClass cStorageClass, CType type, IASTDeclarator d) {
    boolean isGlobal = scope.isGlobalScope();

    if (d != null) {
      Triple<CType, CInitializer, String> declarator = convert(d, type);
      type = declarator.getFirst();
      CInitializer initializer = declarator.getSecond();
      String name = declarator.getThird();

      if (name == null) {
        throw new CFAGenerationRuntimeException("Declaration without name", d);
      }

      // first handle all special cases

      if (cStorageClass == CStorageClass.TYPEDEF) {
        if (initializer != null) {
          throw new CFAGenerationRuntimeException("Typedef with initializer", d);
        }
        return new CTypeDefDeclaration(fileLoc, isGlobal, type, name);
      }

      if (type instanceof CFunctionType) {
        if (initializer != null) {
          throw new CFAGenerationRuntimeException("Function definition with initializer", d);
        }
        if (!isGlobal) {
          throw new CFAGenerationRuntimeException("Non-global function definition", d);
        }
        return new CFunctionDeclaration(fileLoc, (CFunctionType)type, name);
      }

      // now it should be a regular variable declaration

      if (cStorageClass == CStorageClass.EXTERN && initializer != null) {
        throw new CFAGenerationRuntimeException("Extern declarations cannot have initializers", d);
      }

      if (initializer == null && scope.isGlobalScope() && cStorageClass != CStorageClass.EXTERN) {
        // global variables are initialized to zero by default in C
        CExpression init = CDefaults.forType(type, fileLoc);
        // may still be null, because we currently don't handle initializers for complex types
        if (init != null) {
          initializer = new CInitializerExpression(fileLoc, init);
        }
      }

      String origName = name;

      if (cStorageClass == CStorageClass.STATIC) {
        if (!isGlobal) {
          isGlobal = true;
          name = "static__" + scope.getCurrentFunctionName() + "__" + name;
        }
        cStorageClass = CStorageClass.AUTO;
      }

      if (!isGlobal && scope.variableNameInUse(name, name)) {
        String sep = "__";
        int index = 1;
        while (scope.variableNameInUse(name + sep + index, origName)) {
          ++index;
        }
        name = name + sep + index;
      }
      return new CVariableDeclaration(fileLoc, isGlobal, cStorageClass, type, name, origName, initializer);

    } else {
      if (type instanceof CCompositeType
          || type instanceof CEnumType
          || type instanceof CElaboratedType) {
        // struct prototype without variable declaration or similar type definitions
        return new CComplexTypeDeclaration(fileLoc, isGlobal, type);
      }

      throw new CFAGenerationRuntimeException("Declaration without declarator, but type is unknown: " + type.toASTString(""));
    }

  }

  private List<CCompositeTypeMemberDeclaration> convertDeclarationInCompositeType(final IASTDeclaration d) {
    if (d instanceof IASTProblemDeclaration) {
      throw new CFAGenerationRuntimeException((IASTProblemDeclaration)d);
    }

    if (!(d instanceof IASTSimpleDeclaration)) {
      throw new CFAGenerationRuntimeException("unknown declaration type " + d.getClass().getSimpleName(), d);
    }
    IASTSimpleDeclaration sd = (IASTSimpleDeclaration)d;

    CFileLocation fileLoc = convert(d.getFileLocation());
    Pair<CStorageClass, ? extends CType> specifier = convert(sd.getDeclSpecifier());
    if (specifier.getFirst() != CStorageClass.AUTO) {
      throw new CFAGenerationRuntimeException("Unsupported storage class inside composite type", d);
    }
    CType type = specifier.getSecond();

    List<CCompositeTypeMemberDeclaration> result;
    IASTDeclarator[] declarators = sd.getDeclarators();
    if (declarators == null || declarators.length == 0) {
      // declaration without declarator, anonymous struct field?
      CCompositeTypeMemberDeclaration newD = createDeclarationForCompositeType(fileLoc, type, null);
      result = Collections.singletonList(newD);

    } else if (declarators.length == 1) {
      CCompositeTypeMemberDeclaration newD = createDeclarationForCompositeType(fileLoc, type, declarators[0]);
      result = Collections.singletonList(newD);

    } else {
      result = new ArrayList<CCompositeTypeMemberDeclaration>(declarators.length);
      for (IASTDeclarator c : declarators) {

        result.add(createDeclarationForCompositeType(fileLoc, type, c));
      }
    }

    return result;
  }

  private CCompositeTypeMemberDeclaration createDeclarationForCompositeType(CFileLocation fileLoc, CType type, IASTDeclarator d) {
    String name = null;

    if (d != null) {
      Triple<CType, CInitializer, String> declarator = convert(d, type);

      if (declarator.getSecond() != null) {
        throw new CFAGenerationRuntimeException("Unsupported initializer inside composite type", d);
      }

      type = declarator.getFirst();
      name = declarator.getThird();
    }

    return new CCompositeTypeMemberDeclaration(fileLoc, type, name);
  }

  private Triple<CType, CInitializer, String> convert(IASTDeclarator d, CType specifier) {
    if (d instanceof IASTFunctionDeclarator) {
      return convert((IASTFunctionDeclarator)d, specifier);

    } else {
      // Parsing type declarations in C is complex.
      // For example, array modifiers and pointer operators are declared in the
      // "wrong" way:
      // "int (*drives[4])[6]" is "array 4 of pointer to array 6 of int"
      // (The inner most modifiers are the highest-level ones.)
      // So we don't do this recursively, but instead collect all modifiers
      // and apply them after we have reached the inner-most declarator.

      // Collection of all modifiers (outermost modifier is first).
      List<IASTNode> modifiers = Lists.newArrayListWithExpectedSize(1);

      CInitializer initializer = null;
      String name = null;

      // Descend into the nested chain of declators.
      // Find out the name and the initializer, and collect all modifiers.
      IASTDeclarator currentDecl = d;
      while (currentDecl != null) {
        // TODO handle bitfields by checking for instanceof IASTFieldDeclarator

        if (currentDecl instanceof IASTFunctionDeclarator) {
          throw new CFAGenerationRuntimeException("Unsupported declaration nested function declarations", d);
        }

        modifiers.addAll(Arrays.asList(currentDecl.getPointerOperators()));

        if (currentDecl instanceof IASTArrayDeclarator) {
          modifiers.addAll(Arrays.asList(((IASTArrayDeclarator) currentDecl).getArrayModifiers()));
        }

        if (currentDecl.getInitializer() != null) {
          if (initializer != null) {
            throw new CFAGenerationRuntimeException("Unsupported declaration with two initializers", d);
          }
          initializer = convert(currentDecl.getInitializer());
        }

        if (!currentDecl.getName().toString().isEmpty()) {
          if (name != null) {
            throw new CFAGenerationRuntimeException("Unsupported declaration with two names", d);
          }
          name = convert(currentDecl.getName());
        }

        currentDecl = currentDecl.getNestedDeclarator();
      }

      name = Strings.nullToEmpty(name); // there may be no name at all, for example in parameter declarations

      // Add the modifiers to the type.
      CType type = specifier;
      for (IASTNode modifier : modifiers) {
        if (modifier instanceof IASTArrayModifier) {
          type = convert((IASTArrayModifier)modifier, type);

        } else if (modifier instanceof IASTPointerOperator) {
          type = convert((IASTPointerOperator)modifier, type);

        } else {
          assert false;
        }
      }

      return Triple.of(type, initializer, name);
    }
  }

  private CType convertPointerOperators(IASTPointerOperator[] ps, CType type) {
    for (IASTPointerOperator p : ps) {
      type = convert(p, type);
    }
    return type;
  }

  private CPointerType convert(IASTPointerOperator po, CType type) {
    if (po instanceof IASTPointer) {
      IASTPointer p = (IASTPointer)po;
      return new CPointerType(p.isConst(), p.isVolatile(), type);

    } else {
      throw new CFAGenerationRuntimeException("Unknown pointer operator", po);
    }
  }

  private CType convert(IASTArrayModifier am, CType type) {
    if (am instanceof org.eclipse.cdt.core.dom.ast.c.ICASTArrayModifier) {
      org.eclipse.cdt.core.dom.ast.c.ICASTArrayModifier a = (org.eclipse.cdt.core.dom.ast.c.ICASTArrayModifier)am;
      return new CArrayType(a.isConst(), a.isVolatile(), type, convertExpressionWithoutSideEffects(a.getConstantExpression()));

    } else {
      throw new CFAGenerationRuntimeException("Unknown array modifier", am);
    }
  }

  private Triple<CType, CInitializer, String> convert(IASTFunctionDeclarator d, CType returnType) {
    if (!(d instanceof IASTStandardFunctionDeclarator)) {
      throw new CFAGenerationRuntimeException("Unknown non-standard function definition", d);
    }
    IASTStandardFunctionDeclarator sd = (IASTStandardFunctionDeclarator)d;

    // handle return type
    returnType = convertPointerOperators(d.getPointerOperators(), returnType);

    // handle parameters
    List<CParameterDeclaration> paramsList = convert(sd.getParameters());

    // TODO constant and volatile
    CFunctionType fType = new CFunctionType(false, false, returnType, paramsList, sd.takesVarArgs());
    CType type = fType;

    String name;
    if (d.getNestedDeclarator() != null) {
      Triple<? extends CType, CInitializer, String> nestedDeclarator = convert(d.getNestedDeclarator(), type);

      assert d.getName().getRawSignature().isEmpty() : d;
      assert nestedDeclarator.getSecond() == null;

      type = nestedDeclarator.getFirst();
      name = nestedDeclarator.getThird();

    } else {
      name = convert(d.getName());
    }

    fType.setName(name);

    return Triple.of(type, convert(d.getInitializer()), name);
  }


  private Pair<CStorageClass, ? extends CType> convert(IASTDeclSpecifier d) {
    CStorageClass sc = convertCStorageClass(d);

    if (d instanceof IASTCompositeTypeSpecifier) {
      return Pair.of(sc, convert((IASTCompositeTypeSpecifier)d));

    } else if (d instanceof IASTElaboratedTypeSpecifier) {
      return Pair.of(sc, convert((IASTElaboratedTypeSpecifier)d));

    } else if (d instanceof IASTEnumerationSpecifier) {
      return Pair.of(sc, convert((IASTEnumerationSpecifier)d));

    } else if (d instanceof IASTNamedTypeSpecifier) {
      return Pair.of(sc, convert((IASTNamedTypeSpecifier)d));

    } else if (d instanceof IASTSimpleDeclSpecifier) {
      return Pair.of(sc, convert((IASTSimpleDeclSpecifier)d));

    } else {
      throw new CFAGenerationRuntimeException("unknown declSpecifier", d);
    }
  }

  private CStorageClass convertCStorageClass(IASTDeclSpecifier d) {
    switch (d.getStorageClass()) {
    case IASTDeclSpecifier.sc_unspecified:
    case IASTDeclSpecifier.sc_auto:
    case IASTDeclSpecifier.sc_register:
      return CStorageClass.AUTO;

    case IASTDeclSpecifier.sc_static:
      return CStorageClass.STATIC;

    case IASTDeclSpecifier.sc_extern:
      return CStorageClass.EXTERN;

    case IASTDeclSpecifier.sc_typedef:
      return CStorageClass.TYPEDEF;

    default:
      throw new CFAGenerationRuntimeException("Unsupported storage class", d);
    }
  }

  private CCompositeType convert(IASTCompositeTypeSpecifier d) {
    List<CCompositeTypeMemberDeclaration> list = new ArrayList<CCompositeTypeMemberDeclaration>(d.getMembers().length);

    for (IASTDeclaration c : d.getMembers()) {
      List<CCompositeTypeMemberDeclaration> newCs = convertDeclarationInCompositeType(c);
      assert !newCs.isEmpty();
      list.addAll(newCs);
    }
    return new CCompositeType(d.isConst(), d.isVolatile(), d.getKey(), list, convert(d.getName()));
  }

  private CElaboratedType convert(IASTElaboratedTypeSpecifier d) {
    ElaboratedType type;
    switch (d.getKind()) {
    case IASTElaboratedTypeSpecifier.k_enum:
      type = ElaboratedType.ENUM;
      break;
    case IASTElaboratedTypeSpecifier.k_struct:
      type = ElaboratedType.STRUCT;
      break;
    case IASTElaboratedTypeSpecifier.k_union:
      type = ElaboratedType.UNION;
      break;
    default:
      throw new CFAGenerationRuntimeException("Unknown elaborated type", d);
    }

    return new CElaboratedType(d.isConst(), d.isVolatile(), type, convert(d.getName()));
  }

  private CEnumType convert(IASTEnumerationSpecifier d) {
    List<CEnumerator> list = new ArrayList<CEnumerator>(d.getEnumerators().length);
    Long lastValue = -1L; // initialize with -1, so the first one gets value 0
    for (IASTEnumerationSpecifier.IASTEnumerator c : d.getEnumerators()) {
      CEnumerator newC = convert(c, lastValue);
      list.add(newC);
      if (newC.hasValue()) {
        lastValue = newC.getValue();
      } else {
        lastValue = null;
      }
    }
    return new CEnumType(d.isConst(), d.isVolatile(), list, convert(d.getName()));
  }

  private CNamedType convert(IASTNamedTypeSpecifier d) {
    return new CNamedType(d.isConst(), d.isVolatile(), convert(d.getName()));
  }

  private CSimpleType convert(IASTSimpleDeclSpecifier d) {
    if (!(d instanceof org.eclipse.cdt.core.dom.ast.c.ICASTSimpleDeclSpecifier)) {
      throw new CFAGenerationRuntimeException("Unsupported type", d);
    }
    org.eclipse.cdt.core.dom.ast.c.ICASTSimpleDeclSpecifier dd = (org.eclipse.cdt.core.dom.ast.c.ICASTSimpleDeclSpecifier)d;

    CBasicType type;
    switch (dd.getType()) {
    case org.eclipse.cdt.core.dom.ast.c.ICASTSimpleDeclSpecifier.t_Bool:
      type = CBasicType.BOOL;
      break;
    case IASTSimpleDeclSpecifier.t_char:
      type = CBasicType.CHAR;
      break;
    case IASTSimpleDeclSpecifier.t_double:
      type = CBasicType.DOUBLE;
      break;
    case IASTSimpleDeclSpecifier.t_float:
      type = CBasicType.FLOAT;
      break;
    case IASTSimpleDeclSpecifier.t_int:
      type = CBasicType.INT;
      break;
    case IASTSimpleDeclSpecifier.t_unspecified:
      type = CBasicType.UNSPECIFIED;
      break;
    case IASTSimpleDeclSpecifier.t_void:
      type = CBasicType.VOID;
      break;
    default:
      throw new CFAGenerationRuntimeException("Unknown basic type " + dd.getType(), d);
    }

    if ((dd.isShort() && dd.isLong())
        || (dd.isShort() && dd.isLongLong())
        || (dd.isLong() && dd.isLongLong())
        || (dd.isSigned() && dd.isUnsigned())) {
      throw new CFAGenerationRuntimeException("Illegal combination of type identifiers", d);
    }

    return new CSimpleType(dd.isConst(), dd.isVolatile(), type,
        dd.isLong(), dd.isShort(), dd.isSigned(), d.isUnsigned(),
        dd.isComplex(), dd.isImaginary(), dd.isLongLong());
  }


  private CEnumerator convert(IASTEnumerationSpecifier.IASTEnumerator e, Long lastValue) {
    Long value = null;

    if (e.getValue() == null && lastValue != null) {
      value = lastValue + 1;
    } else {
      CExpression v = convertExpressionWithoutSideEffects(e.getValue());
      boolean negate = false;

      if (v instanceof CUnaryExpression) {
        CUnaryExpression u = (CUnaryExpression)v;
        assert u.getOperator() == UnaryOperator.MINUS : v;
        negate = true;
        v = u.getOperand();
      }

      if (v instanceof CIntegerLiteralExpression) {
        value = ((CIntegerLiteralExpression)v).asLong();
        if (negate) {
          value = -value;
        }
      } else {
        // ignoring unsupported enum value
        // TODO Warning
      }
    }

    CEnumerator result = new CEnumerator(convert(e.getFileLocation()), convert(e.getName()), value);
    scope.registerDeclaration(result);
    return result;
  }

  private CInitializer convert(IASTInitializer i) {
    if (i == null) {
      return null;

    } else if (i instanceof IASTInitializerExpression) {
      return convert((IASTInitializerExpression)i);
    } else if (i instanceof IASTInitializerList) {
      return convert((IASTInitializerList)i);
    } else if (i instanceof IASTEqualsInitializer) {
      return convert((IASTEqualsInitializer)i);
    } else if (i instanceof org.eclipse.cdt.core.dom.ast.c.ICASTDesignatedInitializer) {
      logger.log(Level.INFO, "Ignoring initializer part in line", i.getFileLocation().getStartingLineNumber() + ":", i.getRawSignature());
      return null;
    } else {
      throw new CFAGenerationRuntimeException("unknown initializer: " + i.getClass().getSimpleName(), i);
    }
  }

  private CInitializerExpression convert(IASTInitializerExpression i) {
    CAstNode initializer = convertExpressionWithSideEffects(i.getExpression());
    if (initializer != null && initializer instanceof CAssignment){
      preSideAssignments.add(initializer);
      return new CInitializerExpression(convert(i.getFileLocation()), ((CAssignment)initializer).getLeftHandSide());
    }

    if (initializer != null && !(initializer instanceof CExpression)) {
      throw new CFAGenerationRuntimeException("Initializer is not free of side-effects", i);
    }

    return new CInitializerExpression(convert(i.getFileLocation()), (CExpression)initializer);
  }

  private CInitializerList convert(IASTInitializerList iList) {
    List<CInitializer> initializerList = new ArrayList<CInitializer>(iList.getInitializers().length);
    for (IASTInitializer i : iList.getInitializers()) {
      CInitializer newI = convert(i);
      if (newI != null) {
        initializerList.add(newI);
      }
    }
    return new CInitializerList(convert(iList.getFileLocation()), initializerList);
  }

  private CInitializer convert(IASTEqualsInitializer i) {
    IASTInitializerClause ic = i.getInitializerClause();
    if (ic instanceof IASTExpression) {
      IASTExpression e = (IASTExpression)ic;

      CAstNode initializer = convertExpressionWithSideEffects(e);

      if (initializer != null && initializer instanceof CAssignment){
        preSideAssignments.add(initializer);
        return new CInitializerExpression(convert(e.getFileLocation()), ((CAssignment)initializer).getLeftHandSide());
      } else if (initializer != null && initializer instanceof CFunctionCallExpression && i.getParent() instanceof IASTDeclarator) {

        String tmpname = convert(((IASTDeclarator)i.getParent()).getName());
        postSideAssignments.add(new CFunctionCallAssignmentStatement(convert(i.getFileLocation()),
                                                                        createTemporaryVariable(e, tmpname),
                                                                        (CFunctionCallExpression) initializer));
        return null;
      }

      if (initializer != null && !(initializer instanceof CExpression)) {
        throw new CFAGenerationRuntimeException("Initializer is not free of side-effects", e);
      }

      return new CInitializerExpression(convert(ic.getFileLocation()), (CExpression)initializer);

    } else if (ic instanceof IASTInitializerList) {
      return convert((IASTInitializerList)ic);
    } else {
      throw new CFAGenerationRuntimeException("unknown initializer: " + i.getClass().getSimpleName(), i);
    }
  }

  private List<CParameterDeclaration> convert(IASTParameterDeclaration[] ps) {
    List<CParameterDeclaration> paramsList = new ArrayList<CParameterDeclaration>(ps.length);
    for (IASTParameterDeclaration c : ps) {
      if (!c.getRawSignature().equals("void")) {
        paramsList.add(convert(c));
      } else {
        // there may be a function declaration f(void), which is equal to f()
        // we don't want this dummy parameter "void"
        assert ps.length == 1;
      }
    }
    return paramsList;
  }

  private CParameterDeclaration convert(IASTParameterDeclaration p) {
    Pair<CStorageClass, ? extends CType> specifier = convert(p.getDeclSpecifier());
    if (specifier.getFirst() != CStorageClass.AUTO) {
      throw new CFAGenerationRuntimeException("Unsupported storage class for parameters", p);
    }

    Triple<CType, CInitializer, String> declarator = convert(p.getDeclarator(), specifier.getSecond());
    if (declarator.getSecond() != null) {
      throw new CFAGenerationRuntimeException("Unsupported initializer for parameters", p);
    }

    CType type = declarator.getFirst();
    if (type instanceof CFunctionType) {
      CFunctionType functionType = (CFunctionType) type;
      type = new CPointerType(false, false, functionType);
    }

    return new CParameterDeclaration(convert(p.getFileLocation()), type, declarator.getThird());
  }

  public CFileLocation convert(IASTFileLocation l) {
    if (l == null) {
      return null;
    }
    return new CFileLocation(l.getEndingLineNumber(), l.getFileName(), l.getNodeLength(), l.getNodeOffset(), l.getStartingLineNumber());
  }

  private String convert(IASTName n) {
    return n.toString(); // TODO verify toString() is the correct method
  }

  private CType convert(IASTTypeId t) {
    Pair<CStorageClass, ? extends CType> specifier = convert(t.getDeclSpecifier());
    if (specifier.getFirst() != CStorageClass.AUTO) {
      throw new CFAGenerationRuntimeException("Unsupported storage class for type ids", t);
    }

    Triple<CType, CInitializer, String> declarator = convert(t.getAbstractDeclarator(), specifier.getSecond());
    if (declarator.getSecond() != null) {
      throw new CFAGenerationRuntimeException("Unsupported initializer for type ids", t);
    }
    if (declarator.getThird() != null && !declarator.getThird().trim().isEmpty()) {
      throw new CFAGenerationRuntimeException("Unsupported name for type ids", t);
    }

    return declarator.getFirst();
  }

  private CType convert(org.eclipse.cdt.core.dom.ast.IType t) {
    if (t instanceof org.eclipse.cdt.core.dom.ast.IBasicType) {
      return convert((org.eclipse.cdt.core.dom.ast.IBasicType)t);

    } else if (t instanceof org.eclipse.cdt.core.dom.ast.IPointerType) {
      return convert((org.eclipse.cdt.core.dom.ast.IPointerType)t);

    } else if (t instanceof org.eclipse.cdt.core.dom.ast.ITypedef) {
      return convert((org.eclipse.cdt.core.dom.ast.ITypedef)t);

    } else if (t instanceof org.eclipse.cdt.core.dom.ast.IBinding) {
      return new CComplexType(((org.eclipse.cdt.core.dom.ast.IBinding) t).getName());

    } else {
      return new CDummyType(t.toString());
    }
  }

  private CSimpleType convert(final org.eclipse.cdt.core.dom.ast.IBasicType t) {
    try {

      // The IBasicType has to be an ICBasicType or
      // an IBasicType of type "void" (then it is an ICPPBasicType)
      if (t instanceof org.eclipse.cdt.core.dom.ast.c.ICBasicType) {
        final org.eclipse.cdt.core.dom.ast.c.ICBasicType c =
          (org.eclipse.cdt.core.dom.ast.c.ICBasicType) t;

        CBasicType type;
        switch (t.getType()) {
        case org.eclipse.cdt.core.dom.ast.c.ICBasicType.t_Bool:
          type = CBasicType.BOOL;
          break;
        case org.eclipse.cdt.core.dom.ast.IBasicType.t_char:
          type = CBasicType.CHAR;
          break;
        case org.eclipse.cdt.core.dom.ast.IBasicType.t_double:
          type = CBasicType.DOUBLE;
          break;
        case org.eclipse.cdt.core.dom.ast.IBasicType.t_float:
          type = CBasicType.FLOAT;
          break;
        case org.eclipse.cdt.core.dom.ast.IBasicType.t_int:
          type = CBasicType.INT;
          break;
        case org.eclipse.cdt.core.dom.ast.IBasicType.t_unspecified:
          type = CBasicType.UNSPECIFIED;
          break;
        case org.eclipse.cdt.core.dom.ast.IBasicType.t_void:
          type = CBasicType.VOID;
          break;
        default:
          throw new CFAGenerationRuntimeException("Unknown basic type " + t.getType());
        }

        if ((c.isShort() && c.isLong())
            || (c.isShort() && c.isLongLong())
            || (c.isLong() && c.isLongLong())
            || (c.isSigned() && c.isUnsigned())) {
          throw new CFAGenerationRuntimeException("Illegal combination of type identifiers");
        }

        // TODO why is there no isConst() and isVolatile() here?
        return new CSimpleType(false, false, type, c.isLong(), c.isShort(), c.isSigned(), c.isUnsigned(), c.isComplex(), c.isImaginary(), c.isLongLong());

      } else if (t.getType() == org.eclipse.cdt.core.dom.ast.IBasicType.t_void) {

        // the three values isComplex, isImaginary, isLongLong are initialized
        // with FALSE, because we do not know about them
        return new CSimpleType(false, false, CBasicType.VOID, t.isLong(), t.isShort(), t.isSigned(), t.isUnsigned(), false, false, false);

      } else {
        throw new CFAGenerationRuntimeException("Unknown type " + t.toString());
      }

    } catch (org.eclipse.cdt.core.dom.ast.DOMException e) {
      throw new CFAGenerationRuntimeException(e);
    }
  }

  private CPointerType convert(org.eclipse.cdt.core.dom.ast.IPointerType t) {
    try {
      return new CPointerType(t.isConst(), t.isVolatile(), convert(getType(t)));
    } catch (org.eclipse.cdt.core.dom.ast.DOMException e) {
      throw new CFAGenerationRuntimeException(e);
    }
  }

  private org.eclipse.cdt.core.dom.ast.IType getType(org.eclipse.cdt.core.dom.ast.IPointerType t) throws org.eclipse.cdt.core.dom.ast.DOMException {
    // This method needs to throw DOMException because t.getType() does so in Eclipse CDT 6.
    // Don't inline it, because otherwise Eclipse will complain about an unreachable catch block with Eclipse CDT 7.
    return t.getType();
  }

  private CTypedef convert(org.eclipse.cdt.core.dom.ast.ITypedef t) {
    try {
      return new CTypedef(t.getName(), convert(getType(t)));
    } catch (org.eclipse.cdt.core.dom.ast.DOMException e) {
      throw new CFAGenerationRuntimeException(e);
    }
  }

  private org.eclipse.cdt.core.dom.ast.IType getType(org.eclipse.cdt.core.dom.ast.ITypedef t) throws org.eclipse.cdt.core.dom.ast.DOMException {
    // This method needs to throw DOMException because t.getType() does so in Eclipse CDT 6.
    // Don't inline it, because otherwise Eclipse will complain about an unreachable catch block with Eclipse CDT 7.
    return t.getType();
  }
}