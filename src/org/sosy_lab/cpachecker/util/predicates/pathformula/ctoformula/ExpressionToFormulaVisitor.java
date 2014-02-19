/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import static org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.types.CtoFormulaTypeUtils.getRealFieldOwner;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;

public class ExpressionToFormulaVisitor extends DefaultCExpressionVisitor<Formula, UnrecognizedCCodeException> {

  final CtoFormulaConverter conv;
  final CFAEdge       edge;
  final String        function;
  final SSAMapBuilder ssa;
  final Constraints   constraints;

  public ExpressionToFormulaVisitor(CtoFormulaConverter pCtoFormulaConverter, CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
    conv = pCtoFormulaConverter;
    edge = pEdge;
    function = pFunction;
    ssa = pSsa;
    constraints = pCo;
  }

  @Override
  protected Formula visitDefault(CExpression exp)
      throws UnrecognizedCCodeException {
    return conv.makeVariableUnsafe(exp, function, ssa, false);
  }

  private Formula getPointerTargetSizeLiteral(final CPointerType pointerType, final CType implicitType) {
    final int pointerTargetSize = conv.getSizeof(pointerType.getType());
    return conv.fmgr.makeNumber(conv.getFormulaTypeFromCType(implicitType), pointerTargetSize);
  }

  @Override
  public Formula visit(final CBinaryExpression exp) throws UnrecognizedCCodeException {
    final BinaryOperator op = exp.getOperator();
    final CType returnType = exp.getExpressionType();
    final CType calculationType = exp.getCalculationType();

    // these operators expect numeric arguments
    final FormulaType<?> returnFormulaType = conv.getFormulaTypeFromCType(returnType);

    CExpression e1 = exp.getOperand1();
    CExpression e2 = exp.getOperand2();
    e1 = conv.makeCastFromArrayToPointerIfNecessary(e1, returnType);
    e2 = conv.makeCastFromArrayToPointerIfNecessary(e2, returnType);
    final CType t1 = e1.getExpressionType();
    final CType t2 = e2.getExpressionType();
    Formula f1 = e1.accept(this);
    Formula f2 = e2.accept(this);

    f1 = conv.makeCast(t1, calculationType, f1, edge);
    f2 = conv.makeCast(t2, calculationType, f2, edge);

    /* FOR SHIFTS:
     * We would not need to cast the second operand, but we do casting,
     * because Mathsat assumes 2 bitvectors of same length.
     *
     * This could be incorrect in cases of negative shifts and
     * signed/unsigned conversion, example: 5U<<(-1).
     * Instead of "undefined value", we return a possible wrong value.
     *
     * ISO-C 6.5.7 Bitwise shift operators
     * If the value of the right operand is negative or is greater than or equal
     * to the width of the promoted left operand, the behavior is undefined.
     */

    final boolean signed;
    if (calculationType instanceof CSimpleType) {
      signed = conv.machineModel.isSigned((CSimpleType)calculationType);
    } else {
      signed = false;
    }

    // to INT or bigger
    final CType promT1 = conv.getPromotedCType(t1).getCanonicalType();
    final CType promT2 = conv.getPromotedCType(t2).getCanonicalType();

    final Formula ret;

    switch (op) {
    case PLUS:
      if (!(promT1 instanceof CPointerType) && !(promT2 instanceof CPointerType)) { // Just an addition e.g. 6 + 7
        ret = conv.fmgr.makePlus(f1, f2);
      } else if (!(promT2 instanceof CPointerType)) {
        // operand1 is a pointer => we should multiply the second summand by the size of the pointer target
        ret =  conv.fmgr.makePlus(f1, conv.fmgr.makeMultiply(f2,
                                                             getPointerTargetSizeLiteral((CPointerType) promT1,
                                                             calculationType)));
      } else if (!(promT1 instanceof CPointerType)) {
        // operand2 is a pointer => we should multiply the first summand by the size of the pointer target
        ret =  conv.fmgr.makePlus(f2, conv.fmgr.makeMultiply(f1,
                                                             getPointerTargetSizeLiteral((CPointerType) promT2,
                                                             calculationType)));
      } else {
        throw new UnrecognizedCCodeException("Can't add pointers", edge, exp);
      }
      break;
    case MINUS:
      if (!(promT1 instanceof CPointerType) && !(promT2 instanceof CPointerType)) { // Just a subtraction e.g. 6 - 7
        ret =  conv.fmgr.makeMinus(f1, f2);
      } else if (!(promT2 instanceof CPointerType)) {
        // operand1 is a pointer => we should multiply the subtrahend by the size of the pointer target
        ret =  conv.fmgr.makeMinus(f1, conv.fmgr.makeMultiply(f2,
                                                              getPointerTargetSizeLiteral((CPointerType) promT1,
                                                                                            calculationType)));
      } else if (promT1 instanceof CPointerType) {
        // Pointer subtraction => (operand1 - operand2) / sizeof (*operand1)
        if (promT1.equals(promT2)) {
          ret = conv.fmgr.makeDivide(conv.fmgr.makeMinus(f1, f2),
                                     getPointerTargetSizeLiteral((CPointerType) promT1, calculationType),
                                     true);
        } else {
          throw new UnrecognizedCCodeException("Can't subtract pointers of different types", edge, exp);
        }
      } else {
        throw new UnrecognizedCCodeException("Can't subtract a pointer from a non-pointer", edge, exp);
      }
      break;
    case MULTIPLY:
      ret =  conv.fmgr.makeMultiply(f1, f2);
      break;
    case DIVIDE:
      ret =  conv.fmgr.makeDivide(f1, f2, signed);
      break;
    case MODULO:
      ret =  conv.fmgr.makeModulo(f1, f2, signed);
      break;
    case BINARY_AND:
      ret =  conv.fmgr.makeAnd(f1, f2);
      break;
    case BINARY_OR:
      ret =  conv.fmgr.makeOr(f1, f2);
      break;
    case BINARY_XOR:
      ret =  conv.fmgr.makeXor(f1, f2);
      break;
    case SHIFT_LEFT:

      // NOTE: The type of the result is that of the promoted left operand. (6.5.7 3)
      ret =  conv.fmgr.makeShiftLeft(f1, f2);
      break;
    case SHIFT_RIGHT:
      // NOTE: The type of the result is that of the promoted left operand. (6.5.7 3)
      ret =  conv.fmgr.makeShiftRight(f1, f2, signed);
      break;

    case GREATER_THAN:
    case GREATER_EQUAL:
    case LESS_THAN:
    case LESS_EQUAL:
    case EQUALS:
    case NOT_EQUALS: {
      BooleanFormula result;
      switch (op) {
        case GREATER_THAN:
          result= conv.fmgr.makeGreaterThan(f1, f2, signed);
          break;
        case GREATER_EQUAL:
          result= conv.fmgr.makeGreaterOrEqual(f1, f2, signed);
          break;
        case LESS_THAN:
          result= conv.fmgr.makeLessThan(f1, f2, signed);
          break;
        case LESS_EQUAL:
          result= conv.fmgr.makeLessOrEqual(f1, f2, signed);
          break;
        case EQUALS:
          result= conv.fmgr.makeEqual(f1, f2);
          break;
        case NOT_EQUALS:
          result= conv.bfmgr.not(conv.fmgr.makeEqual(f1, f2));
          break;
        default:
          throw new AssertionError();
      }
      ret = conv.ifTrueThenOneElseZero(returnFormulaType, result);
      break;
    }
    default:
      throw new UnrecognizedCCodeException("Unknown binary operator", edge, exp);

    }

    // The CalculationType could be different from returnType, so we cast the result.
    // If the types are equal, the cast returns the Formula unchanged.
    final Formula castedResult = conv.makeCast(calculationType, returnType, ret, edge);

    assert returnFormulaType == conv.fmgr.getFormulaType(castedResult)
         : "Returntype and Formulatype do not match in visit(CBinaryExpression): " + exp;
    return castedResult;
  }




  @Override
  public Formula visit(CCastExpression cexp) throws UnrecognizedCCodeException {
    CExpression op = cexp.getOperand();
    op = conv.makeCastFromArrayToPointerIfNecessary(op, cexp.getExpressionType());

    Formula operand = op.accept(this);

    CType after = cexp.getExpressionType();
    CType before = op.getExpressionType();
    return conv.makeCast(before, after, operand, edge);
  }

  @Override
  public Formula visit(CIdExpression idExp) throws UnrecognizedCCodeException {

    if (idExp.getDeclaration() instanceof CEnumerator) {
      CEnumerator enumerator = (CEnumerator)idExp.getDeclaration();
      CType t = idExp.getExpressionType();
      if (enumerator.hasValue()) {
        return conv.fmgr.makeNumber(conv.getFormulaTypeFromCType(t), enumerator.getValue());
      } else {
        // We don't know the value here, but we know it is constant.
        return conv.makeConstant(enumerator.getName(), t, ssa);
      }
    }

    return conv.makeVariable(conv.scopedIfNecessary(idExp, function), ssa);
  }

  @Override
  public Formula visit(CFieldReference fExp) throws UnrecognizedCCodeException {
    if (conv.options.handleFieldAccess()) {
      CExpression fieldOwner = getRealFieldOwner(fExp);
      Formula f = fieldOwner.accept(this);
      return conv.accessField(fExp, f);
    }

    CExpression fieldRef = fExp.getFieldOwner();
    if (fieldRef instanceof CIdExpression) {
      CSimpleDeclaration decl = ((CIdExpression) fieldRef).getDeclaration();
      if (decl instanceof CDeclaration && ((CDeclaration)decl).isGlobal()) {
        // this is the reference to a global field variable

        // we can omit the warning (no pointers involved),
        // and we don't need to scope the variable reference
        return conv.makeVariable(CtoFormulaConverter.exprToVarName(fExp), fExp.getExpressionType(), ssa);
      }
    }

    return super.visit(fExp);
  }


  @Override
  public Formula visit(CCharLiteralExpression cExp) throws UnrecognizedCCodeException {
    // we just take the byte value
    FormulaType<?> t = conv.getFormulaTypeFromCType(cExp.getExpressionType());
    return conv.fmgr.makeNumber(t, cExp.getCharacter());
  }

  @Override
  public Formula visit(CIntegerLiteralExpression iExp) throws UnrecognizedCCodeException {
    FormulaType<?> t = conv.getFormulaTypeFromCType(iExp.getExpressionType());
    return conv.fmgr.makeNumber(t, iExp.getValue());
  }

  @Override
  public Formula visit(CImaginaryLiteralExpression exp) throws UnrecognizedCCodeException {
    return exp.getValue().accept(this);
  }

  @Override
  public Formula visit(CFloatLiteralExpression fExp) throws UnrecognizedCCodeException {
    FormulaType<?> t = conv.getFormulaTypeFromCType(fExp.getExpressionType());
    final BigDecimal val = fExp.getValue();
    if (val.scale() <= 0) {
      // actually an integral number
      return conv.fmgr.makeNumber(t, convertBigDecimalToBigInteger(val, fExp));

    } else {
      if (t.isBitvectorType()) {
        // not representible
        return conv.makeConstant("__float_constant__" + val, fExp.getExpressionType(), ssa);
      }

      // represent x.y by xy / (10^z) where z is the number of digits in y
      // (the "scale" of a BigDecimal)

      BigDecimal n = val.movePointRight(val.scale()); // this is "xy"
      BigInteger numerator = convertBigDecimalToBigInteger(n, fExp);

      BigDecimal d = BigDecimal.ONE.scaleByPowerOfTen(val.scale()); // this is "10^z"
      BigInteger denominator = convertBigDecimalToBigInteger(d, fExp);
      assert denominator.signum() > 0;

      return conv.fmgr.makeDivide(conv.fmgr.makeNumber(t, numerator),
                                   conv.fmgr.makeNumber(t, denominator),
                                   true);
    }
  }

  private static BigInteger convertBigDecimalToBigInteger(BigDecimal d, CFloatLiteralExpression fExp)
      throws NumberFormatException {
    try {
      return d.toBigIntegerExact();
    } catch (ArithmeticException e) {
      NumberFormatException nfe = new NumberFormatException("Cannot represent floating point literal " + fExp.toASTString() + " as fraction because " + d + " cannot be represented as a long");
      nfe.initCause(e);
      throw nfe;
    }
  }

  @Override
  public Formula visit(CStringLiteralExpression lexp) throws UnrecognizedCCodeException {
    // we create a string constant representing the given
    // string literal
    return conv.makeStringLiteral(lexp.getValue());
  }

  @Override
  public Formula visit(CUnaryExpression exp) throws UnrecognizedCCodeException {
    CExpression operand = exp.getOperand();
    UnaryOperator op = exp.getOperator();
    switch (op) {
    case PLUS:
    case MINUS:
    case TILDE: {
      // Handle Integer Promotion
      CType t = operand.getExpressionType();
      CType promoted = conv.getPromotedCType(t);
      Formula operandFormula = operand.accept(this);
      operandFormula = conv.makeCast(t, promoted, operandFormula, edge);
      Formula ret;
      if (op == UnaryOperator.PLUS) {
        ret = operandFormula;
      } else if (op == UnaryOperator.MINUS) {
        ret = conv.fmgr.makeNegate(operandFormula);
      } else {
        assert op == UnaryOperator.TILDE
              : "This case should be impossible because of switch";
        ret = conv.fmgr.makeNot(operandFormula);
      }

      CType returnType = exp.getExpressionType();
      FormulaType<?> returnFormulaType = conv.getFormulaTypeFromCType(returnType);
      assert returnFormulaType == conv.fmgr.getFormulaType(ret)
            : "Returntype and Formulatype do not match in visit(CUnaryExpression)";
      return ret;
    }

    case NOT: {
      Formula f = operand.accept(this);
      BooleanFormula term = conv.toBooleanFormula(f);
      return conv.ifTrueThenOneElseZero(conv.getFormulaTypeFromCType(exp.getExpressionType()), conv.bfmgr.not(term));
    }

    case AMPER:
      return visitDefault(exp);

    case SIZEOF:
      CType lCType = exp.getOperand().getExpressionType();
      return handleSizeof(exp, lCType);

    default:
      throw new UnrecognizedCCodeException("Unknown unary operator", edge, exp);
    }
  }

  @Override
  public Formula visit(CTypeIdExpression tIdExp)
      throws UnrecognizedCCodeException {

    if (tIdExp.getOperator() == TypeIdOperator.SIZEOF) {
      CType lCType = tIdExp.getType();
      return handleSizeof(tIdExp, lCType);
    } else {
      return visitDefault(tIdExp);
    }
  }

  private Formula handleSizeof(CExpression pExp, CType pCType)
      throws UnrecognizedCCodeException {
    return conv.fmgr.makeNumber(
        conv
          .getFormulaTypeFromCType(pExp.getExpressionType()),
        conv.getSizeof(pCType));
  }
}