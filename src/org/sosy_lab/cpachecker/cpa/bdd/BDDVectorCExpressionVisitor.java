// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bdd;

import java.math.BigInteger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

/**
 * This Visitor implements evaluation of simply typed expressions.
 * An expression is defined as simply typed iff it is not an
 * array type ({@link org.sosy_lab.cpachecker.cfa.types.c.CArrayType}),
 * a struct or union type ({@link org.sosy_lab.cpachecker.cfa.types.c.CComplexType}),
 * a imaginary type ({@link CImaginaryLiteralExpression}),
 * or a pointer type ({@link org.sosy_lab.cpachecker.cfa.types.c.CPointerType}).
 * The key distinction between these types and simply typed types is,
 * that a value of simply typed types can be represented as a numerical
 * value without losing information.
 */
public class BDDVectorCExpressionVisitor
    extends DefaultCExpressionVisitor<Region[], UnsupportedCodeException> {

  private final MachineModel machineModel;
  protected final PredicateManager predMgr;
  protected final VariableTrackingPrecision precision;
  protected final BitvectorManager bvmgr;
  protected final CFANode location;

  /** This Visitor returns the numeral value for an expression.
   * @param pMachineModel where to get info about types, for casting and overflows
   */
  protected BDDVectorCExpressionVisitor(final PredicateManager pPredMgr, final VariableTrackingPrecision pPrecision,
                                     final BitvectorManager pBVmgr, final MachineModel pMachineModel,
                                     final CFANode pLocation) {
    this.predMgr = pPredMgr;
    this.precision = pPrecision;
    this.bvmgr = pBVmgr;
    this.machineModel = pMachineModel;
    this.location = pLocation;
  }

  @Override
  protected Region[] visitDefault(CExpression pExp) {
    return null;
  }

  @Override
  public Region[] visit(final CBinaryExpression pE) throws UnsupportedCodeException {
    final Region[] lVal = pE.getOperand1().accept(this);
    final Region[] rVal = pE.getOperand2().accept(this);
    if (lVal == null || rVal == null) { return null; }
    return calculateBinaryOperation(
        lVal,
        rVal,
        pE.getOperand1().getExpressionType(),
        pE.getOperand2().getExpressionType(),
        bvmgr,
        pE,
        machineModel);
  }

  /**
   * This method calculates the exact result for a binary operation.
   *
   * @param lVal first operand with type lType
   * @param rVal second operand with type rType
   * @param machineModel information about types
   */
  public static Region[] calculateBinaryOperation(
      Region[] lVal,
      Region[] rVal,
      final CType lType,
      final CType rType,
      final BitvectorManager bvmgr,
      final CBinaryExpression binaryExpr,
      final MachineModel machineModel) {

    final BinaryOperator binaryOperator = binaryExpr.getOperator();
    final CType calculationType = binaryExpr.getCalculationType();

    lVal = castCValue(lVal, lType, calculationType, bvmgr, machineModel);
    if (binaryOperator != BinaryOperator.SHIFT_LEFT && binaryOperator != BinaryOperator.SHIFT_RIGHT) {
      /* For SHIFT-operations we do not cast the second operator.
       * We even do not need integer-promotion,
       * because the maximum SHIFT of 64 is lower than MAX_CHAR.
       *
       * ISO-C99 (6.5.7 #3): Bitwise shift operators
       * The integer promotions are performed on each of the operands.
       * The type of the result is that of the promoted left operand.
       * If the value of the right operand is negative or is greater than
       * or equal to the width of the promoted left operand,
       * the behavior is undefined.
       */
      rVal = castCValue(rVal, rType, calculationType, bvmgr, machineModel);
    }

    Region[] result;
    switch (binaryOperator) {
      case PLUS:
      case MINUS:
      case DIVIDE:
      case MODULO:
      case MULTIPLY:
      case SHIFT_LEFT:
      case SHIFT_RIGHT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
        result = arithmeticOperation(lVal, rVal, bvmgr, binaryOperator, calculationType);
        result =
            castCValue(
                result, calculationType, binaryExpr.getExpressionType(), bvmgr, machineModel);

        break;

      case EQUALS:
      case NOT_EQUALS:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL:
        final Region tmp = booleanOperation(lVal, rVal, bvmgr, binaryOperator, calculationType);
        // return 1 if expression holds, 0 otherwise

        int size = 32;
        if (calculationType instanceof CSimpleType) {
          size = machineModel.getSizeofInBits((CSimpleType) calculationType);
        }

        result = bvmgr.wrapLast(tmp, size);
        // we do not cast here, because 0 and 1 should be small enough for every type.

        break;

      default:
        throw new AssertionError("unhandled binary operator");
    }

    return result;
  }

  private static Region[] arithmeticOperation(
      final Region[] l,
      final Region[] r,
      final BitvectorManager bvmgr,
      final BinaryOperator op,
      final CType calculationType) {

    boolean signed = true;
    if (calculationType instanceof CSimpleType) {
      signed = !((CSimpleType) calculationType).isUnsigned();
    }

    switch (op) {
      case PLUS:
        return bvmgr.makeAdd(l, r);
      case MINUS:
        return bvmgr.makeSub(l, r);
      case DIVIDE:
        // this would be working for constant numbers (2/3, x/3),
        // however timeout for variables (a/b -> exponential bdd-size).
        return bvmgr.makeDiv(l, r, signed);
      case MODULO:
        // this would be working for constant numbers (2%3, x%3),
        // however timeout for variables (a%b -> exponential bdd-size).
        return bvmgr.makeMod(l, r, signed);
      case MULTIPLY:
        // this should be working for constant numbers (2*3, x*3),
        // however timeout for variables (a*b -> exponential bdd-size).
        return bvmgr.makeMult(l, r);
      case SHIFT_LEFT:
        return bvmgr.makeShiftLeft(l, r);
      case SHIFT_RIGHT:
        return bvmgr.makeShiftRight(l, r, signed);
      case BINARY_AND:
        return bvmgr.makeBinaryAnd(l, r);
      case BINARY_OR:
        return bvmgr.makeBinaryOr(l, r);
      case BINARY_XOR:
        return bvmgr.makeXor(l, r);

      default:
        throw new AssertionError("unknown binary operation: " + op);
    }
  }

  protected static Region booleanOperation(final Region[] l, final Region[] r, final BitvectorManager bvmgr,
                                         final BinaryOperator op, final CType calculationType) {

    boolean signed = true;
    if (calculationType instanceof CSimpleType) {
      signed = !((CSimpleType) calculationType).isUnsigned();
    }

    switch (op) {
      case EQUALS:
        return bvmgr.makeLogicalEqual(l, r);
      case NOT_EQUALS:
        return bvmgr.makeNot(bvmgr.makeLogicalEqual(l, r));
      case GREATER_THAN: // A>B <--> B<A
        return bvmgr.makeLess(r, l, signed);
      case GREATER_EQUAL: // A>=B <--> B<=A
        return bvmgr.makeLessOrEqual(r, l, signed);
      case LESS_THAN:
        return bvmgr.makeLess(l, r, signed);
      case LESS_EQUAL:
        return bvmgr.makeLessOrEqual(l, r, signed);

      default:
        throw new AssertionError("unknown binary operation: " + op);
    }
  }

  @Override
  public Region[] visit(CCastExpression pE) throws UnsupportedCodeException {
    return castCValue(
        pE.getOperand().accept(this),
        pE.getOperand().getExpressionType(),
        pE.getExpressionType(),
        bvmgr,
        machineModel);
  }

  @Override
  public Region[] visit(CCharLiteralExpression pE) {
    return bvmgr.makeNumber(pE.getCharacter(), getSize(pE.getExpressionType()));
  }

  @Override
  public Region[] visit(CFieldReference pE) {
    String name = BDDTransferRelation.getCanonicalName(pE);
    if (name == null) {
      return visitDefault(pE);
    }
    return predMgr.createPredicate(
        name, pE.getExpressionType(), location, getSize(pE.getExpressionType()), precision);
  }

  @Override
  public Region[] visit(CIntegerLiteralExpression pE) {
    return bvmgr.makeNumber(pE.asLong(), getSize(pE.getExpressionType()));
  }

  @Override
  public Region[] visit(CImaginaryLiteralExpression pE) throws UnsupportedCodeException {
    return pE.getValue().accept(this);
  }

  @Override
  public Region[] visit(final CArraySubscriptExpression expression)
      throws UnsupportedCodeException {
    throw new UnsupportedCodeException("BDD-analysis does not support arrays: " + expression, null);
  }

  @Override
  public Region[] visit(final CTypeIdExpression pE) {
    final TypeIdOperator idOperator = pE.getOperator();
    final CType innerType = pE.getType();

    switch (idOperator) {
      case SIZEOF:
        return getSizeof(innerType);

      default: // TODO support more operators
        return null;
    }
  }

  @Override
  public Region[] visit(CIdExpression idExp) {
    if (idExp.getDeclaration() instanceof CEnumerator) {
      CEnumerator enumerator = (CEnumerator) idExp.getDeclaration();
      if (enumerator.hasValue()) {
        return bvmgr.makeNumber(enumerator.getValue(), getSize(idExp.getExpressionType()));
      } else {
        return null;
      }
    }

    return predMgr.createPredicate(idExp.getDeclaration().getQualifiedName(), idExp.getExpressionType(), location, getSize(idExp.getExpressionType()), precision);
  }

  @Override
  public Region[] visit(final CUnaryExpression unaryExpression) throws UnsupportedCodeException {
    final UnaryOperator unaryOperator = unaryExpression.getOperator();
    final CExpression unaryOperand = unaryExpression.getOperand();

    if (unaryOperator == UnaryOperator.SIZEOF) {
      return getSizeof(unaryOperand.getExpressionType());
    }

    final Region[] value = unaryOperand.accept(this);

    if (value == null) {
      return null;
    }

    switch (unaryOperator) {

      case MINUS: // -X == (0-X)
        return bvmgr.makeSub(bvmgr.makeNumber(BigInteger.ZERO, value.length), value);

      case SIZEOF:
        throw new AssertionError("SIZEOF should be handled before!");

      case AMPER: // valid expression, but it's a pointer value
        // TODO Not precise enough
        return getSizeof(unaryOperand.getExpressionType());
      case TILDE: // ~X == (X===0)
        return bvmgr.makeBinaryEqual(bvmgr.makeNumber(BigInteger.ZERO, value.length), value);
      default:
        // TODO handle unimplemented operators
        return null;
    }
  }

  private Region[] getSizeof(CType pType) {
    return bvmgr.makeNumber(machineModel.getSizeof(pType), machineModel.getSizeofInt());
  }

  protected int getSize(CType pType) {
    return machineModel.getSizeofInBits(pType).intValueExact();
  }

  /**
   * This method returns the value of an expression, reduced to match the type. This method handles
   * overflows and casts. If necessary warnings for the user are printed.
   *
   * @param pExp expression to evaluate
   * @param pTargetType the type of the left side of an assignment
   * @return if evaluation successful, then value, else null
   */
  public Region[] evaluate(final CExpression pExp, final CType pTargetType)
      throws UnsupportedCodeException {
    return castCValue(
        pExp.accept(this), pExp.getExpressionType(), pTargetType, bvmgr, machineModel);
  }

  /**
   * This method returns the input-value, casted to match the type.
   * If the value matches the type, it is returned unchanged.
   * This method handles overflows and print warnings for the user.
   * Example:
   * This method is called, when an value of type 'integer'
   * is assigned to a variable of type 'char'.
   *
   * @param value will be casted. If value is null, null is returned.
   * @param targetType value will be casted to targetType.
   * @param machineModel contains information about types
   */
  public static Region[] castCValue(
      @Nullable final Region[] value,
      CType sourceType,
      CType targetType,
      final BitvectorManager bvmgr,
      final MachineModel machineModel) {
    if (value == null) {
      return null;
    }

    sourceType = sourceType.getCanonicalType();
    targetType = targetType.getCanonicalType();
    if (targetType instanceof CSimpleType && sourceType instanceof CSimpleType) {
      return bvmgr.toBitsize(
          machineModel.getSizeofInBits((CSimpleType) targetType),
          machineModel.isSigned((CSimpleType) sourceType),
          value);
    }
    // currently we do not handle floats, doubles or voids, pointers, so lets ignore this case.
    return value;
  }
}