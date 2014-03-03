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
package org.sosy_lab.cpachecker.cfa.types;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
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
  private final CSimpleType ptrEquivalent;

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

  /**
   * This method returns the signed integer type of the result
   * of subtracting two pointers, also called <code>ptrdiff_t</code>.
   *
   * <p>From ISO-C99 (6.5.6, #9):<p>
   * When two pointers are subtracted, [...] The size of the result is implementation-defined,
   * and its type (a signed integer type) is <code>ptrdiff_t</code> defined in the stddef.h-header.
   */
  public CSimpleType getPointerDiffType() {
    // ptrEquivalent should not be unsigned, so canonical type is always signed
    assert !ptrEquivalent.isUnsigned();
    return ptrEquivalent.getCanonicalType();
  }

  /**
   * This method decides, if a plain <code>char</code> is signed or unsigned.
   *
   * <p>From ISO-C99 (6.2.5, #15):<p>
   * The three types <code>char</code>, <code>signed char</code>, and
   * <code>unsigned char</code> are collectively called the <i>character types</i>.
   * The implementation shall define <code>char</code> to have the same range, representation, and behavior
   * as either <code>signed char</code> or <code>unsigned char</code>.
   */
  public boolean isDefaultCharSigned() {
    return true;
  }

  /**
   * Determine whether a type is signed or unsigned.
   * Contrary to {@link CSimpleType#isSigned()} and {@link CSimpleType#isUnsigned()}
   * this method leaves no third option and should thus be preferred.
   * For floating point types it returns true,
   * for types where signedness makes no sense (bool, void) it returns false.
   */
  public boolean isSigned(CSimpleType t) {
    // resolve UNSPECIFIED and INT to SIGNED INT etc.
    t = t.getCanonicalType();

    if (t.isSigned()) {
      return true;
    } else if (t.isUnsigned()) {
      return false;
    }

    switch (t.getType()) {
    case CHAR:
      return isDefaultCharSigned();
    case FLOAT:
    case DOUBLE:
      return true;
    case INT:
      throw new AssertionError("Canonical type of INT should always have sign modifier");
    case UNSPECIFIED:
      throw new AssertionError("Canonical type should never be UNSPECIFIED");
    default:
      // bool, void
      return false;
    }
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

  private final CTypeVisitor<Integer, IllegalArgumentException> sizeofVisitor = new BaseSizeofVisitor(this);

  public static class BaseSizeofVisitor implements CTypeVisitor<Integer, IllegalArgumentException> {
    private final MachineModel model;

    public BaseSizeofVisitor(MachineModel model) {
      this.model = model;
    }

    @Override
    public Integer visit(CArrayType pArrayType) throws IllegalArgumentException {
      // TODO: Take possible padding into account

      CExpression arrayLength = pArrayType.getLength();

      if (arrayLength instanceof CIntegerLiteralExpression) {
        int length = ((CIntegerLiteralExpression)arrayLength).getValue().intValue();

        Integer sizeOfType = model.getSizeof(pArrayType.getType());

        if (sizeOfType != null) {
          return length * sizeOfType;
        }
      }

      // Treat arrays with variable length as pointer.
      return model.getSizeofPtr();
    }

    @Override
    public Integer visit(CCompositeType pCompositeType) throws IllegalArgumentException {

      switch (pCompositeType.getKind()) {
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
  }

  public int getSizeof(CType type) {
    return type.accept(sizeofVisitor);
  }
}
