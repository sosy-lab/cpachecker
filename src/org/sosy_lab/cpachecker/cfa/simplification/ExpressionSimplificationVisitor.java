// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.simplification;

import java.math.BigInteger;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;

/**
 * This visitor visits an expression and evaluates it. The returnvalue of the visit consists of the
 * simplified expression and - if possible - a numeral value for the expression.
 */
public class ExpressionSimplificationVisitor
    extends DefaultCExpressionVisitor<CExpression, NoException> {

  private final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;

  public ExpressionSimplificationVisitor(MachineModel mm, LogManagerWithoutDuplicates pLogger) {
    machineModel = mm;
    logger = pLogger;
  }

  /** return a simplified version of the expression. */
  protected CExpression recursive(CExpression expr) {
    return expr.accept(this);
  }

  private @Nullable NumericValue getValue(CExpression expr) {
    if (expr instanceof CIntegerLiteralExpression cIntegerLiteralExpression) {
      return new NumericValue(cIntegerLiteralExpression.getValue());
    } else if (expr instanceof CCharLiteralExpression cCharLiteralExpression) {
      return new NumericValue((int) cCharLiteralExpression.getCharacter());
    } else if (expr instanceof CFloatLiteralExpression cFloatLiteralExpression) {
      return new NumericValue(cFloatLiteralExpression.getValue());
    }
    return null;
  }

  /**
   * Takes an explicit value as returned by various ExplicitCPA functions and converts it to a
   * <code>{@code Pair<CExpression, Number>}</code> as required by this class.
   */
  private CExpression convertExplicitValueToExpression(final CExpression expr, Value value) {
    // TODO: handle cases other than numeric values
    NumericValue numericResult = value.asNumericValue();
    final CType type = expr.getExpressionType().getCanonicalType();
    if (numericResult != null && type instanceof CSimpleType cSimpleType) {
      CBasicType basicType = cSimpleType.getType();
      if (basicType.isIntegerType()) {
        return new CIntegerLiteralExpression(
            expr.getFileLocation(), type, numericResult.bigIntegerValue());
      } else if (basicType.isFloatingPointType()) {
        FloatValue.Format precision = FloatValue.Format.fromCType(machineModel, type);
        return new CFloatLiteralExpression(
            expr.getFileLocation(),
            machineModel,
            type,
            numericResult.floatingPointValue(precision));
      }
    }
    if (numericResult != null) {
      logger.logf(Level.FINE, "Can not handle result of expression %s", numericResult.toString());
    } else {
      logger.logf(Level.FINE, "Can not handle result of expression, numericResult is null.");
    }
    return expr;
  }

  @Override
  protected CExpression visitDefault(final CExpression expr) {
    return expr;
  }

  @Override
  public CExpression visit(final CBinaryExpression expr) {
    final BinaryOperator binaryOperator = expr.getOperator();

    final CExpression op1 = recursive(expr.getOperand1());
    final NumericValue value1 = getValue(op1);

    final CExpression op2 = recursive(expr.getOperand2());
    final NumericValue value2 = getValue(op2);

    if (op1.equals(op2)
        // Naively, it would seem that the above condition suffices, but it does not:
        // - Floats have NaN != NaN behavior.
        // - Pointer accesses might be unsafe and memory-safety analyses would like to check that.
        // But int variables should be safe. No need to check for type of op2 due to equals() and
        // literals will be handled by having their values determined.
        && op1 instanceof CIdExpression
        && CTypes.isIntegerType(op1.getExpressionType())) {
      switch (binaryOperator) {
        case EQUALS, GREATER_EQUAL, LESS_EQUAL -> {
          return CIntegerLiteralExpression.ONE;
        }
        case NOT_EQUALS, GREATER_THAN, LESS_THAN -> {
          return CIntegerLiteralExpression.ZERO;
        }
        default -> {}
      }
    }

    // if one side can not be evaluated, build new expression
    if (value1 == null || value2 == null) {
      final CBinaryExpression newExpr;
      if (op1 == expr.getOperand1() && op2 == expr.getOperand2()) {
        // shortcut: if nothing has changed, use the original expression
        newExpr = expr;
      } else {
        final CBinaryExpressionBuilder binExprBuilder =
            new CBinaryExpressionBuilder(machineModel, logger);
        switch (binaryOperator) {
          case BINARY_AND -> {
            if (value1 != null && value1.bigIntegerValue().equals(BigInteger.ZERO)) {
              return op1;
            }
            if (value2 != null && value2.bigIntegerValue().equals(BigInteger.ZERO)) {
              return op2;
            }
          }
          case BINARY_OR -> {
            if (value1 != null && value1.bigIntegerValue().equals(BigInteger.ZERO)) {
              return op2;
            }
            if (value2 != null && value2.bigIntegerValue().equals(BigInteger.ZERO)) {
              return op1;
            }
          }
          default -> {}
        }
        newExpr = binExprBuilder.buildBinaryExpressionUnchecked(op1, op2, binaryOperator);
      }
      return newExpr;
    }

    // TODO: handle the case that it's not a CSimpleType or that it's not a number
    Value result =
        AbstractExpressionValueVisitor.calculateBinaryOperation(
            value1, value2, expr, machineModel, logger);

    return convertExplicitValueToExpression(expr, result);
  }

  @Override
  public CExpression visit(CCastExpression expr) {
    final CExpression op = recursive(expr.getOperand());
    final NumericValue value = getValue(op);

    // if expr can not be evaluated, build new expression
    if (value == null) {
      final CCastExpression newExpr;
      if (op == expr.getOperand()) {
        // shortcut: if nothing has changed, use the original expression
        newExpr = expr;
      } else {
        newExpr = new CCastExpression(expr.getFileLocation(), expr.getExpressionType(), op);
      }
      return newExpr;
    }

    // TODO: handle the case that the result is not a numeric value
    final Value castedValue =
        AbstractExpressionValueVisitor.castCValue(
            value, expr.getExpressionType(), machineModel, logger, expr.getFileLocation());

    return convertExplicitValueToExpression(expr, castedValue);
  }

  @Override
  public CExpression visit(final CTypeIdExpression expr) {
    final TypeIdOperator idOperator = expr.getOperator();
    final CType innerType = expr.getType();

    switch (idOperator) {
      case SIZEOF -> {
        if (innerType.hasKnownConstantSize()) {
          BigInteger size = machineModel.getSizeof(innerType);
          return new CIntegerLiteralExpression(
              expr.getFileLocation(), expr.getExpressionType(), size);
        }
        return visitDefault(expr);
        // TODO simplify inner part of expr?
      }
      case ALIGNOF -> {
        int alignment = machineModel.getAlignof(innerType);
        return new CIntegerLiteralExpression(
            expr.getFileLocation(), expr.getExpressionType(), BigInteger.valueOf(alignment));
      }
      default -> {
        // TODO support more operators
        return visitDefault(expr);
      }
    }
  }

  @Override
  public CExpression visit(final CUnaryExpression expr) {
    final UnaryOperator unaryOperator = expr.getOperator();
    final FileLocation loc = expr.getFileLocation();
    final CType exprType = expr.getExpressionType();
    final CExpression operand = expr.getOperand();
    final CType operandType = operand.getExpressionType();

    if (unaryOperator == UnaryOperator.SIZEOF && operandType.hasKnownConstantSize()) {
      return new CIntegerLiteralExpression(loc, exprType, machineModel.getSizeof(operandType));
    } else if (unaryOperator == UnaryOperator.ALIGNOF) {
      return new CIntegerLiteralExpression(
          loc, exprType, BigInteger.valueOf(machineModel.getAlignof(operandType)));
    }

    final CExpression op = recursive(operand);
    assert op.getExpressionType().equals(operandType) : "simplification should not change type";
    final NumericValue value = getValue(op);

    if (value != null && operandType instanceof CSimpleType cSimpleType) {
      if (unaryOperator == UnaryOperator.MINUS) {
        // we have to cast the value, because it can overflow, for example for the unary-expression
        // "-2147483648" (=MIN_INT),
        // where the operand's value "2147483648" itself creates an overflow, and the negation
        // reverses it.
        final NumericValue negatedValue =
            (NumericValue)
                AbstractExpressionValueVisitor.castCValue(
                    value.negate(), exprType, machineModel, logger, loc);
        switch (cSimpleType.getType()) {
          case BOOL, CHAR, INT -> {
            // negation of zero is zero, other values should be irrelevant
            // better do not convert to long, but directly use the computed value,
            // i.e. "-1ULL" would be converted to long -1, which is valid,
            // but does not match its CType bounds.
            return new CIntegerLiteralExpression(loc, exprType, negatedValue.bigIntegerValue());
          }
          case FLOAT, DOUBLE, FLOAT128 -> {
            FloatValue.Format precision = FloatValue.Format.fromCType(machineModel, exprType);
            return new CFloatLiteralExpression(
                loc, machineModel, exprType, negatedValue.floatingPointValue(precision));
          }
          default -> {
            // return the original expression below
          }
        }

      } else if (unaryOperator == UnaryOperator.TILDE && cSimpleType.getType().isIntegerType()) {
        // cast the value, because the evaluation of "~" is done for long and maybe the target-type
        // is integer.
        final NumericValue complementValue =
            (NumericValue)
                AbstractExpressionValueVisitor.castCValue(
                    new NumericValue(~value.longValue()), exprType, machineModel, logger, loc);
        return new CIntegerLiteralExpression(loc, exprType, complementValue.bigIntegerValue());
      }

    } else if (unaryOperator == UnaryOperator.AMPER // handle &((struct foo*)0)->field
        && op instanceof CFieldReference fieldRef
        && fieldRef.isPointerDereference()
        && fieldRef.getFieldOwner() instanceof CCastExpression cast
        && cast.getCastType().getCanonicalType() instanceof CPointerType pointerType
        && pointerType.getType().getCanonicalType() instanceof CCompositeType structType) {
      NumericValue baseAddress = getValue(cast.getOperand());
      if (baseAddress != null) {
        Optional<BigInteger> offset =
            machineModel.getFieldOffsetInBytes(structType, fieldRef.getFieldName());
        if (offset.isPresent()) {
          return new CIntegerLiteralExpression(
              loc, exprType, baseAddress.bigIntegerValue().add(offset.orElseThrow()));
        }
      }
    }

    if (op == operand) {
      // shortcut: if nothing has changed, use the original expression
      return expr;
    } else {
      return new CUnaryExpression(loc, exprType, op, unaryOperator);
    }
  }

  @Override
  public CExpression visit(CIdExpression expr) {
    final CSimpleDeclaration decl = expr.getDeclaration();
    final CType type = expr.getExpressionType();

    // enum constant
    if (decl instanceof CEnumerator cEnumerator) {
      final BigInteger v = cEnumerator.getValue();
      return new CIntegerLiteralExpression(expr.getFileLocation(), type, v);
    }

    // const variable, inline initializer
    if (!(type instanceof CProblemType)
        && type.isConst()
        && decl instanceof CVariableDeclaration cVariableDeclaration) {

      final CInitializer init = cVariableDeclaration.getInitializer();
      if (init instanceof CInitializerExpression cInitializerExpression) {
        NumericValue v = getValue(cInitializerExpression.getExpression());

        if (v != null && decl.getType() instanceof CSimpleType) {
          switch (((CSimpleType) type).getType()) {
            case BOOL, CHAR, INT -> {
              return new CIntegerLiteralExpression(
                  expr.getFileLocation(), type, v.bigIntegerValue());
            }
            case FLOAT, DOUBLE, FLOAT128 -> {
              // Cast the literal to the target type before assigning the value
              // This is necessary to handle initializer like `float x = 1.0` where the type of the
              // literal on the right is different from the type of the variable.
              FloatValue.Format precision = FloatValue.Format.fromCType(machineModel, type);
              FloatValue litValue = v.floatingPointValue(precision);
              return new CFloatLiteralExpression(
                  expr.getFileLocation(), machineModel, type, litValue);
            }
            default -> {
              // return the original expression below
            }
          }
        }
      }
    }

    return visitDefault(expr);
  }
}
