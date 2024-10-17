// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import com.google.common.base.Joiner;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressOfExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryAndExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryOrExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryXorExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.CastExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.DivisionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.EqualsExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LessThanExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LessThanOrEqualExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalAndExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalOrExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ModuloExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.MultiplicationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.NegationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.PointerExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ShiftLeftExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ShiftRightExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SubtractionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.ArrayValue;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.FunctionValue;
import org.sosy_lab.cpachecker.cpa.value.type.NullValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ConstraintToTextVisitor implements SymbolicValueVisitor<String> {

  @Override
  public String visit(final SymbolicIdentifier pValue) {
    Optional<MemoryLocation> var = pValue.getRepresentedLocation();
    if (var.isPresent()) {
      return var.orElseThrow().getExtendedQualifiedName() + "@" + pValue.getId();
    } else {
      return "@" + pValue.getId();
    }
  }

  @Override
  public String visit(final ConstantSymbolicExpression pExpression) {
    Value constVal = pExpression.getValue();
    assert !(constVal instanceof SymbolicExpression);
    if (constVal instanceof Constraint) {
      return ((Constraint) constVal).accept(this);
    }

    if (constVal instanceof SymbolicIdentifier) {
      return ((SymbolicIdentifier) constVal).accept(this);
    }

    if (constVal instanceof NumericValue) {
      Number n = ((NumericValue) constVal).getNumber();
      if (n instanceof Integer) {
        return Integer.toString(((Integer) n).intValue());
      } else if (n instanceof UnsignedInteger) {
        return ((UnsignedInteger) n).intValue() + "u";
      } else if (n instanceof Float) {
        return ((Float) n).floatValue() + "f";
      } else if (n instanceof Double) {
        return ((Double) n).doubleValue() + "d";
      } else if (n instanceof Long) {
        return ((Long) n).longValue() + "l";
      } else if (n instanceof UnsignedLong) {
        return ((UnsignedLong) n).longValue() + "ul";
      } else if (n instanceof Short) {
        return ((Short) n).shortValue() + "s";
      } else if (n instanceof Byte) {
        return ((Byte) n).byteValue() + "b";
      } else if (n instanceof Rational) {
        Rational r = (Rational) n;
        return translateBigInteger(r.getNum()) + "," + translateBigInteger(r.getDen());
      } else if (n instanceof BigInteger) {
        return translateBigInteger((BigInteger) n);
      } else if (n instanceof BigDecimal) {
        return ((BigDecimal) n).toString() + "bd";
      } else {
        // e.g., IntegerType (NativeLong, NativeSize, size_t), ScalaNumber (BigDecimal, BigInt)
        // Striped64 (DoubleAccumulator, DoubleAdder, LongAccumulator, LongAdder)
        // AtomicInteger, AtomicDouble, AtomicLong
        // NegativeNaN
        throw new UnsupportedOperationException(
            "Numeric value " + n.getClass() + " not supported by " + this.getClass() + ".");
      }
    } else if (constVal instanceof BooleanValue) {
      if (((BooleanValue) constVal).isTrue()) {
        return "true";
      } else {
        return "false";
      }
    } else if (constVal instanceof FunctionValue) {
      throw new UnsupportedOperationException(
          "FunctionValue not supported by " + this.getClass() + ".");
    } else if (constVal instanceof NullValue) {
      return "null";
    } else if (constVal instanceof ArrayValue) {
      throw new UnsupportedOperationException(
          "ArrayValue not supported by " + this.getClass() + ".");
    } else if (constVal instanceof UnknownValue) {
      return "@unknown";
    } else {
      throw new UnsupportedOperationException(
          "Unsupported constant value " + constVal + " of type " + constVal.getClass() + ".");
    }
  }

  private String translateBigInteger(final BigInteger pNumber) {
    byte[] rep = pNumber.toByteArray();
    StringBuilder sBuild = new StringBuilder();
    sBuild.append(rep.length).append(";");
    Joiner.on(":").appendTo(sBuild, Bytes.asList(rep));
    return sBuild.toString();
  }

  @Override
  public String visit(final AdditionExpression pExpression) {
    return binaryConstraintToText(pExpression.getOperand1(), pExpression.getOperand2(), "+");
  }

  @Override
  public String visit(final SubtractionExpression pExpression) {
    return binaryConstraintToText(pExpression.getOperand1(), pExpression.getOperand2(), "-");
  }

  @Override
  public String visit(final MultiplicationExpression pExpression) {
    return binaryConstraintToText(pExpression.getOperand1(), pExpression.getOperand2(), "*");
  }

  @Override
  public String visit(final DivisionExpression pExpression) {
    return binaryConstraintToText(pExpression.getOperand1(), pExpression.getOperand2(), "/");
  }

  @Override
  public String visit(final ModuloExpression pExpression) {
    return binaryConstraintToText(pExpression.getOperand1(), pExpression.getOperand2(), "%");
  }

  @Override
  public String visit(final BinaryAndExpression pExpression) {
    return binaryConstraintToText(pExpression.getOperand1(), pExpression.getOperand2(), "&");
  }

  @Override
  public String visit(final BinaryNotExpression pExpression) {
    return unaryConstraintToText(pExpression.getOperand(), "~");
  }

  @Override
  public String visit(final BinaryOrExpression pExpression) {
    return binaryConstraintToText(pExpression.getOperand1(), pExpression.getOperand2(), "|");
  }

  @Override
  public String visit(final BinaryXorExpression pExpression) {
    return binaryConstraintToText(pExpression.getOperand1(), pExpression.getOperand2(), "^");
  }

  @Override
  public String visit(final ShiftRightExpression pExpression) {
    if (pExpression.isSigned()) {
      return binaryConstraintToText(pExpression.getOperand1(), pExpression.getOperand2(), ">>");
    } else {
      return binaryConstraintToText(pExpression.getOperand1(), pExpression.getOperand2(), ">>>");
    }
  }

  @Override
  public String visit(final ShiftLeftExpression pExpression) {
    return binaryConstraintToText(pExpression.getOperand1(), pExpression.getOperand2(), "<<");
  }

  @Override
  public String visit(final LogicalNotExpression pExpression) {
    return unaryConstraintToText(pExpression.getOperand(), "!");
  }

  @Override
  public String visit(final LessThanOrEqualExpression pExpression) {
    return binaryConstraintToText(pExpression.getOperand1(), pExpression.getOperand2(), "<=");
  }

  @Override
  public String visit(final LessThanExpression pExpression) {
    return binaryConstraintToText(pExpression.getOperand1(), pExpression.getOperand2(), "<");
  }

  @Override
  public String visit(final EqualsExpression pExpression) {
    return binaryConstraintToText(pExpression.getOperand1(), pExpression.getOperand2(), "==");
  }

  @Override
  public String visit(final LogicalOrExpression pExpression) {
    return binaryConstraintToText(pExpression.getOperand1(), pExpression.getOperand2(), "||");
  }

  @Override
  public String visit(final LogicalAndExpression pExpression) {
    return binaryConstraintToText(pExpression.getOperand1(), pExpression.getOperand2(), "&&");
  }

  @Override
  public String visit(final CastExpression pExpression) {
    return unaryConstraintToText(pExpression.getOperand(), "()");
  }

  @Override
  public String visit(final PointerExpression pExpression) {
    return unaryConstraintToText(pExpression.getOperand(), "**");
  }

  @Override
  public String visit(final AddressOfExpression pExpression) {
    return unaryConstraintToText(pExpression.getOperand(), "*&");
  }

  @Override
  public String visit(final NegationExpression pExpression) {
    return unaryConstraintToText(pExpression.getOperand(), "--"); // unary minus
  }

  private String unaryConstraintToText(final SymbolicValue pOp, String pOperator) {
    return constraintWithSpace(pOp) + pOperator;
  }

  private String binaryConstraintToText(
      final SymbolicValue pOp1, final SymbolicValue pOp2, String pOperator) {
    return constraintWithSpace(pOp1) + constraintWithSpace(pOp2) + pOperator;
  }

  private String constraintWithSpace(final SymbolicValue pConstraint) {
    return pConstraint.accept(this) + " ";
  }
}
