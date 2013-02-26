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
package org.sosy_lab.cpachecker.cfa.types;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;

/**
 * This enum stores the sizes for all the basic types that exist.
 */
public enum MachineModel {
  /**
   * Machine model representing a 32bit Linux machine
   */
  LINUX32(
      // numeric types
      2,  // short
      4,  // int
      4,  // long int
      8,  // long long int
      4,  // float
      8,  // double
      12, // long double

      // other
      1, // void
      1, // bool
      4  // pointer
      ),

  /**
   * Machine model representing a 64bit Linux machine
   */
  LINUX64(
      // numeric types
      2,  // short
      4,  // int
      8,  // long int
      8,  // long long int
      4,  // float
      8,  // double
      16, // long double

      // other
      1, // void
      1, // bool
      8  // pointer
      );

  // numeric types
  private final int     sizeofShort;
  private final int     sizeofInt;
  private final int     sizeofLongInt;
  private final int     sizeofLongLongInt;
  private final int     sizeofFloat;
  private final int     sizeofDouble;
  private final int     sizeofLongDouble;

  // other
  private final int     sizeofVoid;
  private final int     sizeofBool;
  private final int     sizeofPtr;

  // according to ANSI C, sizeof(char) is always 1
  private final int mSizeofChar = 1;

  // a char is always a byte, but a byte doesn't have to be 8 bits
  private final int mSizeofCharInBits = 8;
  private CSimpleType ptrEquivalent;

  private MachineModel(int pSizeofShort, int pSizeofInt, int pSizeofLongInt,
      int pSizeofLongLongInt, int pSizeofFloat, int pSizeofDouble,
      int pSizeofLongDouble, int pSizeofVoid, int pSizeofBool, int pSizeOfPtr) {
    sizeofShort = pSizeofShort;
    sizeofInt = pSizeofInt;
    sizeofLongInt = pSizeofLongInt;
    sizeofLongLongInt = pSizeofLongLongInt;
    sizeofFloat = pSizeofFloat;
    sizeofDouble = pSizeofDouble;
    sizeofLongDouble = pSizeofLongDouble;
    sizeofVoid = pSizeofVoid;
    sizeofBool = pSizeofBool;
    sizeofPtr = pSizeOfPtr;

    if (sizeofPtr == sizeofInt) {
      ptrEquivalent = CNumericTypes.INT;
    } else if (sizeofPtr == sizeofLongInt) {
      ptrEquivalent = CNumericTypes.LONG_INT;
    } else if (sizeofPtr == sizeofLongLongInt) {
      ptrEquivalent = CNumericTypes.LONG_LONG_INT;
    } else if (sizeofPtr == sizeofShort) {
      ptrEquivalent = CNumericTypes.SHORT_INT;
    } else {
      throw new AssertionError("No ptr-Equivalent found");
    }
  }

  public CSimpleType getPointerEquivalentSimpleType() {
    return ptrEquivalent;
  }

  public int getSizeofCharInBits() {
    return mSizeofCharInBits;
  }

  public int getSizeofShort() {
    return sizeofShort;
  }

  public int getSizeofInt() {
    return sizeofInt;
  }

  public int getSizeofLongInt() {
    return sizeofLongInt;
  }

  public int getSizeofLongLongInt() {
    return sizeofLongLongInt;
  }

  public int getSizeofFloat() {
    return sizeofFloat;
  }

  public int getSizeofDouble() {
    return sizeofDouble;
  }

  public int getSizeofLongDouble() {
    return sizeofLongDouble;
  }

  public int getSizeofVoid() {
    return sizeofVoid;
  }

  public int getSizeofBool() {
    return sizeofBool;
  }

  public int getSizeofChar() {
    return mSizeofChar;
  }

  public int getSizeofPtr() {
    return sizeofPtr;
  }

  public int getSizeof(CSimpleType type) {
    switch (type.getType()) {
    case VOID:        return getSizeofVoid();
    case BOOL:        return getSizeofBool();
    case CHAR:        return getSizeofChar();
    case FLOAT:       return getSizeofFloat();
    case UNSPECIFIED: // unspecified is the same as int
    case INT:
      if (type.isLongLong()) {
        return getSizeofLongLongInt();
      } else if (type.isLong()) {
        return getSizeofLongInt();
      } else if (type.isShort()) {
        return getSizeofShort();
      } else {
        return getSizeofInt();
      }
    case DOUBLE:
      if (type.isLong()) {
        return getSizeofLongDouble();
      } else {
        return getSizeofDouble();
      }
    default:
      throw new AssertionError("Unrecognized CBasicType " + type.getType());
    }
  }

  public CTypeVisitor<Integer, IllegalArgumentException> sizeofVisitor = new BaseSizeofVisitor(this);
  public static class BaseSizeofVisitor implements CTypeVisitor<Integer, IllegalArgumentException> {
    private MachineModel model;

    public BaseSizeofVisitor(MachineModel model) {
      this.model = model;
    }

    @Override
    public Integer visit(CArrayType pArrayType) throws IllegalArgumentException {
      // TODO: This has to be checked (Example: Char pathbuf[1 + 1];)
      // TODO: Take possible padding into account

      CExpression arrayLength = pArrayType.getLength();

      Integer length = null;

      if (arrayLength != null) {
        length = arrayLength.accept(new ArrayLengthVisitor());
      }

      if (length != null) {

        Integer sizeOfType = model.getSizeof(pArrayType.getType());

        if (sizeOfType != null) {
          return length * sizeOfType;
        }
      }


      // We do not support arrays with variable length, so treat them as pointer.
      return model.getSizeofPtr();
      //return getSizeofInt();
    }

    @Override
    public Integer visit(CCompositeType pCompositeType) throws IllegalArgumentException {

      switch(pCompositeType.getKind()) {
        case STRUCT: return handleSizeOfStruct(pCompositeType);
        case UNION:  return handleSizeOfUnion(pCompositeType);
        case ENUM: // There is no such kind of Composit Type.
        default: throw new AssertionError();
      }
    }

    private Integer handleSizeOfStruct(CCompositeType pCompositeType) {
      int size = 0;
      // TODO: Take possible padding into account
      for (CCompositeTypeMemberDeclaration decl : pCompositeType.getMembers()) {
        size += decl.getType().accept(this);
      }
      return size;
    }

    private Integer handleSizeOfUnion(CCompositeType pCompositeType) {
      int size = 0;
      int sizeOfType = 0;
      // TODO: Take possible padding into account
      for (CCompositeTypeMemberDeclaration decl : pCompositeType.getMembers()) {
        sizeOfType = decl.getType().accept(this);
        size = Math.max(size, sizeOfType);
      }
      return size;
    }

    @Override
    public Integer visit(CElaboratedType pElaboratedType) throws IllegalArgumentException {
      CType def = pElaboratedType.getRealType();
      if (def != null) {
        return def.accept(this);
      }

      switch (pElaboratedType.getKind()) {
      case ENUM:
        return model.getSizeofInt();
      case STRUCT:
        // TODO: UNDEFINED
        return model.getSizeofInt();
      case UNION:
        // TODO: UNDEFINED
        return model.getSizeofInt();
      default:
        return model.getSizeofInt();
      }
    }

    @Override
    public Integer visit(CEnumType pEnumType) throws IllegalArgumentException {
      return model.getSizeofInt();
    }

    @Override
    public Integer visit(CFunctionType pFunctionType) throws IllegalArgumentException {
      // A function does not really have a size,
      // but references to functions can be used as pointers.
      return model.getSizeofPtr();
    }

    @Override
    public Integer visit(CPointerType pPointerType) throws IllegalArgumentException {
      return model.getSizeofPtr();
    }

    @Override
    public Integer visit(CProblemType pProblemType) throws IllegalArgumentException {
      throw new IllegalArgumentException("Unknown C-Type: " + pProblemType.getClass().toString());
    }

    @Override
    public Integer visit(CSimpleType pSimpleType) throws IllegalArgumentException {
      return model.getSizeof(pSimpleType);
    }

    @Override
    public Integer visit(CTypedefType pTypedefType) throws IllegalArgumentException {
      return pTypedefType.getRealType().accept(this);
    }

    private class ArrayLengthVisitor extends DefaultCExpressionVisitor<Integer, IllegalArgumentException>
    {

      @Override
      protected Integer visitDefault(CExpression pExp) {
        return null;
      }

      @Override
      public Integer visit(CIntegerLiteralExpression exp) throws IllegalArgumentException {
        return (int) exp.asLong();
      }

      @Override
      public Integer visit(CBinaryExpression pE) throws IllegalArgumentException {
        BinaryOperator binaryOperator = pE.getOperator();
        CExpression lVarInBinaryExp = pE.getOperand1();
        CExpression rVarInBinaryExp = pE.getOperand2();

        switch (binaryOperator) {
        case PLUS:
        case MINUS:
        case DIVIDE:
        case MULTIPLY:
        case SHIFT_LEFT:
        case BINARY_AND:
        case BINARY_OR:
        case BINARY_XOR: {
          Integer lVal = lVarInBinaryExp.accept(this);
          if (lVal == null) { return null; }

          Integer rVal = rVarInBinaryExp.accept(this);
          if (rVal == null) { return null; }

          switch (binaryOperator) {
          case PLUS:
            return lVal + rVal;

          case MINUS:
            return lVal - rVal;

          case DIVIDE:
            // TODO maybe we should signal a division by zero error?
            if (rVal == 0) { return null; }

            return lVal / rVal;

          case MULTIPLY:
            return lVal * rVal;

          case SHIFT_LEFT:
            return lVal << rVal;

          case BINARY_AND:
            return lVal & rVal;

          case BINARY_OR:
            return lVal | rVal;

          case BINARY_XOR:
            return lVal ^ rVal;

          default:
            throw new AssertionError();
          }
        }

        case EQUALS:
        case NOT_EQUALS:
        case GREATER_THAN:
        case GREATER_EQUAL:
        case LESS_THAN:
        case LESS_EQUAL: {

          Integer lVal = lVarInBinaryExp.accept(this);
          if (lVal == null) { return null; }

          Integer rVal = rVarInBinaryExp.accept(this);
          if (rVal == null) { return null; }

          boolean result;
          switch (binaryOperator) {
          case EQUALS:
            result = (lVal.equals(rVal));
            break;
          case NOT_EQUALS:
            result = !(lVal.equals(rVal));
            break;
          case GREATER_THAN:
            result = (lVal > rVal);
            break;
          case GREATER_EQUAL:
            result = (lVal >= rVal);
            break;
          case LESS_THAN:
            result = (lVal < rVal);
            break;
          case LESS_EQUAL:
            result = (lVal <= rVal);
            break;

          default:
            throw new AssertionError();
          }

          // return 1 if expression holds, 0 otherwise
          return result ? 1 : 0;
        }

        case MODULO:
        case SHIFT_RIGHT:
        default:
          // TODO check which cases can be handled
          return null;
        }
      }

      @Override
      public Integer visit(CUnaryExpression unaryExpression) throws IllegalArgumentException {
        UnaryOperator unaryOperator = unaryExpression.getOperator();
        CExpression unaryOperand = unaryExpression.getOperand();

        Integer value = null;

        switch (unaryOperator) {
        case MINUS:
          value = unaryOperand.accept(this);
          return (value != null) ? -value : null;

        case NOT:
          value = unaryOperand.accept(this);

          if (value == null) {
            return null;
          } else {
            return value == 0 ? 1 : 0;
          }

        case SIZEOF:
          return model.getSizeof(unaryOperand.getExpressionType());
        case TILDE:
        default:
          return null;
        }
      }

      @Override
      public Integer visit(CTypeIdExpression typeIdExp) throws IllegalArgumentException {

        TypeIdOperator typeOperator = typeIdExp.getOperator();
        CType type = typeIdExp.getType();

        switch (typeOperator) {
        case SIZEOF:
          return model.getSizeof(type);
        default:
          return null;
          //TODO Investigate the other Operators.
        }
      }

      @Override
      public Integer visit(CCastExpression pE) throws IllegalArgumentException {
        return pE.getOperand().accept(this);
      }
    }
  }

  public int getSizeof(CType type) {
    return type.accept(sizeofVisitor);
  }
}