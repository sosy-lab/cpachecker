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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
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
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

/** This enum stores the sizes for all the basic types that exist. */
public enum MachineModel {
  /** Machine model representing a 32bit Linux machine with alignment: */
  LINUX32(
      // numeric types
      2, // short
      4, // int
      4, // long int
      8, // long long int
      4, // float
      8, // double
      12, // long double

      // other
      1, // void
      1, // bool
      4, // pointer

      // alignof numeric types
      2, // short
      4, //int
      4, //long int
      4, // long long int
      4, //float
      4, //double
      4, //long double

      // alignof other
      1, // void
      1, //bool
      4 //pointer
  ),

  /** Machine model representing a 64bit Linux machine with alignment: */
  LINUX64(
      // numeric types
      2, // short
      4, // int
      8, // long int
      8, // long long int
      4, // float
      8, // double
      16, // long double

      // other
      1, // void
      1, // bool
      8, // pointer

      //  alignof numeric types
      2, // short
      4, // int
      8, // long int
      8, // long long int
      4, // float
      8, // double
      16, // long double

      // alignof other
      1, // void
      1, // bool
      8 // pointer
  ),

  /** Machine model representing an ARM machine with alignment: */
  ARM(
      // numeric types
      2, // short
      4, // int
      4, // long int
      8, // long long int
      4, // float
      8, // double
      8, // long double

      // other
      1, // void
      1, // bool
      4, // pointer

      //  alignof numeric types
      2, // short
      4, // int
      4, // long int
      4, // long long int
      4, // float
      4, // double
      4, // long double

      // alignof other
      1, // void
      4, // bool
      4 // pointer
  );
  // numeric types
  private final int sizeofShort;
  private final int sizeofInt;
  private final int sizeofLongInt;
  private final int sizeofLongLongInt;
  private final int sizeofFloat;
  private final int sizeofDouble;
  private final int sizeofLongDouble;

  // other
  private final int sizeofVoid;
  private final int sizeofBool;
  private final int sizeofPtr;

  // alignof numeric types
  private final int alignofShort;
  private final int alignofInt;
  private final int alignofLongInt;
  private final int alignofLongLongInt;
  private final int alignofFloat;
  private final int alignofDouble;
  private final int alignofLongDouble;

  // alignof other
  private final int alignofVoid;
  private final int alignofBool;
  private final int alignofPtr;

  // according to ANSI C, sizeof(char) is always 1
  private final int mSizeofChar = 1;
  private final int mAlignofChar = 1;

  // a char is always a byte, but a byte doesn't have to be 8 bits
  private final int mSizeofCharInBits = 8;
  private final CSimpleType ptrEquivalent;

  private MachineModel(
      int pSizeofShort,
      int pSizeofInt,
      int pSizeofLongInt,
      int pSizeofLongLongInt,
      int pSizeofFloat,
      int pSizeofDouble,
      int pSizeofLongDouble,
      int pSizeofVoid,
      int pSizeofBool,
      int pSizeOfPtr,
      int pAlignofShort,
      int pAlignofInt,
      int pAlignofLongInt,
      int pAlignofLongLongInt,
      int pAlignofFloat,
      int pAlignofDouble,
      int pAlignofLongDouble,
      int pAlignofVoid,
      int pAlignofBool,
      int pAlignofPtr) {
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

    alignofShort = pAlignofShort;
    alignofInt = pAlignofInt;
    alignofLongInt = pAlignofLongInt;
    alignofLongLongInt = pAlignofLongLongInt;
    alignofFloat = pAlignofFloat;
    alignofDouble = pAlignofDouble;
    alignofLongDouble = pAlignofLongDouble;
    alignofVoid = pAlignofVoid;
    alignofBool = pAlignofBool;
    alignofPtr = pAlignofPtr;

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
   * This method returns the signed integer type of the result of subtracting two pointers, also
   * called <code>ptrdiff_t</code>.
   *
   * <p>From ISO-C99 (6.5.6, #9):
   *
   * <p>When two pointers are subtracted, [...] The size of the result is implementation-defined,
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
   * <p>From ISO-C99 (6.2.5, #15):
   *
   * <p>The three types <code>char</code>, <code>signed char</code>, and <code>unsigned char</code>
   * are collectively called the <i>character types</i>. The implementation shall define <code>char
   * </code> to have the same range, representation, and behavior as either <code>signed char</code>
   * or <code>unsigned char</code>.
   */
  public boolean isDefaultCharSigned() {
    return true;
  }

  /**
   * Determine whether a type is signed or unsigned. Contrary to {@link CSimpleType#isSigned()} and
   * {@link CSimpleType#isUnsigned()} this method leaves no third option and should thus be
   * preferred. For floating point types it returns true, for types where signedness makes no sense
   * (bool, void) it returns false.
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
      case BOOL:
        return getSizeofBool();
      case CHAR:
        return getSizeofChar();
      case FLOAT:
        return getSizeofFloat();
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

  public int getSizeofInBits(CSimpleType type) {
    return getSizeof(type) * getSizeofCharInBits();
  }

  public int getAlignofShort() {
    return alignofShort;
  }

  public int getAlignofInt() {
    return alignofInt;
  }

  public int getAlignofLongInt() {
    return alignofLongInt;
  }

  public int getAlignofLongLongInt() {
    return alignofLongLongInt;
  }

  public int getAlignofFloat() {
    return alignofFloat;
  }

  public int getAlignofDouble() {
    return alignofDouble;
  }

  public int getAlignofLongDouble() {
    return alignofLongDouble;
  }

  public int getAlignofVoid() {
    return alignofVoid;
  }

  public int getAlignofBool() {
    return alignofBool;
  }

  public int getAlignofChar() {
    return mAlignofChar;
  }

  public int getAlignofPtr() {
    return alignofPtr;
  }

  /** returns INT, if the type is smaller than INT, else the type itself. */
  public CSimpleType getPromotedCType(CSimpleType pType) {

    /*
     * ISO-C99 (6.3.1.1 #2):
     * If an int can represent all values of the original type, the value is
     * converted to an int; otherwise, it is converted to an unsigned int.
     * These are called the integer promotions.
     */
    // TODO when do we really need unsigned_int?
    if (getSizeof(pType) < getSizeofInt()) {
      return CNumericTypes.SIGNED_INT;
    } else {
      return pType;
    }
  }

  /**
   * Get the minimal representable value for an integer type.
   *
   * @throws IllegalArgumentException If the type is not an integer type as defined by {@link
   *     CBasicType#isIntegerType()}.
   */
  public BigInteger getMinimalIntegerValue(CSimpleType pType) {
    checkArgument(pType.getType().isIntegerType());
    if (isSigned(pType)) {
      return twoToThePowerOf(getSizeofInBits(pType) - 1).negate();
    } else {
      return BigInteger.ZERO;
    }
  }

  /**
   * Get the maximal representable value for an integer type.
   *
   * @throws IllegalArgumentException If the type is not an integer type as defined by {@link
   *     CBasicType#isIntegerType()}.
   */
  public BigInteger getMaximalIntegerValue(CSimpleType pType) {
    checkArgument(pType.getType().isIntegerType());
    if (pType.getType() == CBasicType.BOOL) {
      return BigInteger.ONE;
    } else if (isSigned(pType)) {
      return twoToThePowerOf(getSizeofInBits(pType) - 1).subtract(BigInteger.ONE);
    } else {
      return twoToThePowerOf(getSizeofInBits(pType)).subtract(BigInteger.ONE);
    }
  }

  private static BigInteger twoToThePowerOf(int exp) {
    assert exp > 0 : "Exponent " + exp + " is not greater than zero.";
    BigInteger result = BigInteger.ZERO.setBit(exp);
    assert BigInteger.valueOf(2).pow(exp).equals(result);
    return result;
  }

  @SuppressFBWarnings("SE_BAD_FIELD")
  @SuppressWarnings("ImmutableEnumChecker")
  private final BaseSizeofVisitor sizeofVisitor =
      new BaseSizeofVisitor(this);

  public static class BaseSizeofVisitor implements CTypeVisitor<Integer, IllegalArgumentException> {
    private final MachineModel model;

    protected BaseSizeofVisitor(MachineModel model) {
      this.model = model;
    }

    @Override
    public Integer visit(CArrayType pArrayType) throws IllegalArgumentException {
      // TODO: Take possible padding into account

      CExpression arrayLength = pArrayType.getLength();

      if (arrayLength instanceof CIntegerLiteralExpression) {
        int length = ((CIntegerLiteralExpression) arrayLength).getValue().intValue();

        int sizeOfType = model.getSizeof(pArrayType.getType());
        return length * sizeOfType;
      }

      // Treat arrays with variable length as pointer.
      return model.getSizeofPtr();
    }

    @Override
    public Integer visit(CCompositeType pCompositeType) throws IllegalArgumentException {

      switch (pCompositeType.getKind()) {
        case STRUCT:
          return handleSizeOfStruct(pCompositeType);
        case UNION:
          return handleSizeOfUnion(pCompositeType);
        case ENUM: // There is no such kind of Composit Type.
        default:
          throw new AssertionError();
      }
    }

    private int calculateByteSize(int pBitFieldsSize) {
      if (pBitFieldsSize == 0) {
        return 0;
      }

      int result = pBitFieldsSize / model.getSizeofCharInBits();
      if (pBitFieldsSize % model.getSizeofCharInBits() > 0) {
        result++;
      }
      return result;
    }

    private Integer handleSizeOfStruct(CCompositeType pCompositeType) {
      long size = model.getFieldOffsetOrSizeOrFieldOffsetsMappedInBits(pCompositeType, null, null);

      return Math.toIntExact(size);
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

      if (pElaboratedType.getKind() == ComplexTypeKind.ENUM) {
        return model.getSizeofInt();
      }

      throw new IllegalArgumentException(
          "Cannot compute size of incomplete type " + pElaboratedType);
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

    @Override
    public Integer visit(CVoidType pVoidType) throws IllegalArgumentException {
      return model.getSizeofVoid();
    }

    @Override
    public Integer visit(CBitFieldType pCBitFieldType) throws IllegalArgumentException {
      return calculateByteSize(pCBitFieldType.getBitFieldSize());
    }
  }

  public int getSizeof(CType pType) {
    checkArgument(
        pType instanceof CVoidType || !pType.isIncomplete(),
        "Cannot compute size of incomplete type %s",
        pType);
    return getSizeof(pType, sizeofVisitor);
  }

  public int getSizeof(CType pType, BaseSizeofVisitor pSizeofVisitor) {
    checkNotNull(pSizeofVisitor);
    return pType.accept(pSizeofVisitor);
  }

  public int getSizeofPtrInBits() {
    return getSizeofPtr() * getSizeofCharInBits();
  }

  public int getSizeofInBits(CType pType) {
    return getSizeofInBits(pType, sizeofVisitor);
  }

  public int getSizeofInBits(CType pType, BaseSizeofVisitor pSizeofVisitor) {
    checkNotNull(pSizeofVisitor);
    if (pType instanceof CBitFieldType) {
      return ((CBitFieldType) pType).getBitFieldSize();
    } else {
      return getSizeof(pType, pSizeofVisitor) * getSizeofCharInBits();
    }
  }

  @SuppressFBWarnings("SE_BAD_FIELD_STORE")
  @SuppressWarnings("ImmutableEnumChecker")
  private final CTypeVisitor<Integer, IllegalArgumentException> alignofVisitor =
      new BaseAlignofVisitor(this);

  private static class BaseAlignofVisitor
      implements CTypeVisitor<Integer, IllegalArgumentException> {
    private final MachineModel model;

    private BaseAlignofVisitor(MachineModel model) {
      this.model = model;
    }

    @Override
    public Integer visit(CArrayType pArrayType) throws IllegalArgumentException {
      // the alignment of an array is the same as the alignment of an member of the array
      return pArrayType.getType().accept(this);
    }

    @Override
    public Integer visit(CCompositeType pCompositeType) throws IllegalArgumentException {

      switch (pCompositeType.getKind()) {
        case STRUCT:
        case UNION:
          int alignof = 1;
          int alignOfType = 0;
          // TODO: Take possible padding into account
          for (CCompositeTypeMemberDeclaration decl : pCompositeType.getMembers()) {
            alignOfType = decl.getType().accept(this);
            alignof = Math.max(alignof, alignOfType);
          }
          return alignof;

        case ENUM: // There is no such kind of Composite Type.
        default:
          throw new AssertionError();
      }
    }

    @Override
    public Integer visit(CElaboratedType pElaboratedType) throws IllegalArgumentException {
      CType def = pElaboratedType.getRealType();
      if (def != null) {
        return def.accept(this);
      }

      if (pElaboratedType.getKind() == ComplexTypeKind.ENUM) {
        return model.getSizeofInt();
      }

      throw new IllegalArgumentException(
          "Cannot compute alignment of incomplete type " + pElaboratedType);
    }

    @Override
    public Integer visit(CEnumType pEnumType) throws IllegalArgumentException {
      // enums are always ints
      return model.getAlignofInt();
    }

    @Override
    public Integer visit(CFunctionType pFunctionType) throws IllegalArgumentException {
      // function types have per definition the value 1 if compiled with gcc
      return 1;
    }

    @Override
    public Integer visit(CPointerType pPointerType) throws IllegalArgumentException {
      return model.getAlignofPtr();
    }

    @Override
    public Integer visit(CProblemType pProblemType) throws IllegalArgumentException {
      throw new IllegalArgumentException("Unknown C-Type: " + pProblemType.getClass().toString());
    }

    @Override
    public Integer visit(CSimpleType pSimpleType) throws IllegalArgumentException {
      switch (pSimpleType.getType()) {
        case BOOL:
          return model.getAlignofBool();
        case CHAR:
          return model.getAlignofChar();
        case FLOAT:
          return model.getAlignofFloat();
        case UNSPECIFIED: // unspecified is the same as int
        case INT:
          if (pSimpleType.isLongLong()) {
            return model.getAlignofLongLongInt();
          } else if (pSimpleType.isLong()) {
            return model.getAlignofLongInt();
          } else if (pSimpleType.isShort()) {
            return model.getAlignofShort();
          } else {
            return model.getAlignofInt();
          }
        case DOUBLE:
          if (pSimpleType.isLong()) {
            return model.getAlignofLongDouble();
          } else {
            return model.getAlignofDouble();
          }
        default:
          throw new AssertionError("Unrecognized CBasicType " + pSimpleType.getType());
      }
    }

    @Override
    public Integer visit(CTypedefType pTypedefType) throws IllegalArgumentException {
      return pTypedefType.getRealType().accept(this);
    }

    @Override
    public Integer visit(CVoidType pVoidType) throws IllegalArgumentException {
      return model.getAlignofVoid();
    }

    @Override
    public Integer visit(CBitFieldType pCBitFieldType) throws IllegalArgumentException {
      return pCBitFieldType.getType().accept(this);
    }
  }

  public int getAlignof(CType type) {
    return type.accept(alignofVisitor);
  }

  /**
   * This method creates a mapping of all fields contained by pOwnerType to their respective offsets
   * in bits and returns it to the caller.
   *
   * <p>A {@link ComplexTypeKind#UNION} will result in a {@link Map} of fields to zeroes.
   *
   * @param pOwnerType a {@link CCompositeType} to calculate its fields offsets
   * @return a mapping of typeMemberDeclarations to there corresponding offsets in pOwnerType
   */
  public Map<CCompositeTypeMemberDeclaration, Long> getAllFieldOffsetsInBits(
      CCompositeType pOwnerType) {
    ImmutableMap.Builder<CCompositeTypeMemberDeclaration, Long> outParameterMap =
        ImmutableMap.builder();

    getFieldOffsetOrSizeOrFieldOffsetsMappedInBits(pOwnerType, null, outParameterMap);

    return outParameterMap.build();
  }

  /**
   * Calculates the offset of pFieldName in pOwnerType in bits.
   *
   * @param pOwnerType a {@link CCompositeType} to calculate its field offset
   * @param pFieldName the name of the field to calculate its offset
   * @return the offset of the given field
   */
  public long getFieldOffsetInBits(CCompositeType pOwnerType, String pFieldName) {
    checkNotNull(pFieldName);
    return getFieldOffsetOrSizeOrFieldOffsetsMappedInBits(pOwnerType, pFieldName, null);
  }

  /**
   * Compute size of composite types or offsets of fields in composite types, taking alignment and
   * padding into account. Both tasks share the same complex logic, so we implement them in the same
   * private method that is exposed via various public methods for individual tasks.
   *
   * @param pOwnerType a {@link CCompositeType} to calculate its a field offset or its overall size
   * @param pFieldName the name of the field to calculate its offset; <code>null</code> for
   *     composites size
   * @param outParameterMap a {@link Map} given as both, input and output, to store the mapping of
   *     fields to offsets in; may be <code>null</code> if not required
   * @return a long that is either the offset of the given field or the size of the whole type
   */
  private long getFieldOffsetOrSizeOrFieldOffsetsMappedInBits(
      CCompositeType pOwnerType,
      @Nullable String pFieldName,
      @Nullable ImmutableMap.Builder<CCompositeTypeMemberDeclaration, Long> outParameterMap) {
    checkArgument(
        (pFieldName == null) || (outParameterMap == null),
        "Call of this method does only make sense if either pFieldName or outParameterMap "
            + "is of value null, otherwise it either stops the calculation with an incomplete "
            + "map or wastes ressources by filling a map with values that are not required.");
    final ComplexTypeKind ownerTypeKind = pOwnerType.getKind();
    List<CCompositeTypeMemberDeclaration> typeMembers = pOwnerType.getMembers();

    long bitOffset = 0L;
    long sizeOfConsecutiveBitFields = 0;

    long sizeOfByte = getSizeofCharInBits();

    if (ownerTypeKind == ComplexTypeKind.UNION) {
      if (outParameterMap == null) {
        // If the field in question is a part of the Union,
        // return an offset of 0.
        // Otherwise, to indicate a problem, the return
        // will be null.
        if (typeMembers.stream().anyMatch(m -> m.getName().equals(pFieldName))) {
          return bitOffset;
        }
      } else {
        for (CCompositeTypeMemberDeclaration typeMember : typeMembers) {
          outParameterMap.put(typeMember, 0L);
        }
      }
    } else if (ownerTypeKind == ComplexTypeKind.STRUCT) {

      for (Iterator<CCompositeTypeMemberDeclaration> iterator = typeMembers.iterator(); iterator.hasNext();) {
        CCompositeTypeMemberDeclaration typeMember = iterator.next();
        CType type = typeMember.getType();

        long fieldSizeInBits = -1;
        // If incomplete type at end of struct, just assume 0 for its size
        // and compute its offset as usual, since it isn't affected.
        //
        // If incomplete and not the end of the struct, something is wrong
        // and we return an empty Optional.
        if (type.isIncomplete()) {
          if (iterator.hasNext()) {
            throw new AssertionError(
                "unexpected incomplete type "
                    + type
                    + " for field "
                    + pFieldName
                    + " in "
                    + pOwnerType);
          } else {
            // XXX: Should there be a check for CArrayType here
            // as there was in handleSizeOfStruct or is it
            // safe to say, that this case will not occur
            // and if it does due to an error we already crash
            // in the getPadding-step below?
            fieldSizeInBits = 0;
          }
        } else {
          fieldSizeInBits = getSizeofInBits(type);
        }

        if (type instanceof CBitFieldType) {
          if (typeMember.getName().equals(pFieldName)) {
            // just escape the loop and return the current offset
            bitOffset += sizeOfConsecutiveBitFields;
            return bitOffset;
          }

          CType innerType = ((CBitFieldType) type).getType();

          if (fieldSizeInBits == 0) {
            // Bitfields with length 0 guarantee that
            // the next bitfield starts at the beginning of the
            // next address an object of the declaring
            // type could be addressed by.
            //
            // E.g., if you have a struct like this:
            //   struct s { int a : 8; char : 0; char b; };
            //
            // then the struct will be aligned to the size of int
            // (4 Bytes) and will occupy 4 Bytes of memory.
            //
            // A struct like this:
            //   struct t { int a : 8; int : 0; char b; };
            //
            // will also be aligned to the size of int, but
            // since the 'int : 0;' member adjusts the next object
            // to the next int-like addressable unit, t will
            // occupy 8 Bytes instead of 4 (the char b is placed
            // at the next 4-Byte addressable unit).
            //
            // At last, a struct like this:
            //   struct u { char a : 4; char : 0; char b : 4; };
            //
            // will be aligned to size of char and occupy 2 Bytes
            // in memory, while the same struct without the
            // 'char : 0;' member would just occupy 1 Byte.
            bitOffset =
                calculatePaddedBitsize(bitOffset, sizeOfConsecutiveBitFields, innerType, sizeOfByte);
            sizeOfConsecutiveBitFields = 0L;
          } else {
            sizeOfConsecutiveBitFields =
                calculateNecessaryBitfieldOffset(
                        sizeOfConsecutiveBitFields + bitOffset,
                        innerType,
                        sizeOfByte,
                        fieldSizeInBits)
                    - bitOffset;
            sizeOfConsecutiveBitFields += fieldSizeInBits;
          }

          // Put start offset of bitField to outParameterMap
          if (outParameterMap != null) {
            outParameterMap.put(typeMember, bitOffset + sizeOfConsecutiveBitFields - fieldSizeInBits);
          }
        } else {
          bitOffset =
              calculatePaddedBitsize(bitOffset, sizeOfConsecutiveBitFields, type, sizeOfByte);
          sizeOfConsecutiveBitFields = 0L;

          if (typeMember.getName().equals(pFieldName)) {
            // just escape the loop and return the current offset
            return bitOffset;
          }

          if (outParameterMap != null) {
            outParameterMap.put(typeMember, bitOffset);
          }
          bitOffset += fieldSizeInBits;
        }
      }
    }

    if (pFieldName != null) {
      throw new IllegalArgumentException(
          "could not find field " + pFieldName + " in " + pOwnerType);
    }

    // call with byte size of 1 to return size in bytes instead of bits
    return calculatePaddedBitsize(bitOffset, sizeOfConsecutiveBitFields, pOwnerType, 1L);
  }

  @Deprecated
  public long calculateNecessaryBitfieldOffset(
      long pBitFieldOffset, CType pType, long pSizeOfByte, long pBitFieldLength) {
    // gcc -std=c11 implements bitfields such, that it only positions a bitfield 'B'
    // directly adjacent to its preceding bitfield 'A', if 'B' fits into the
    // remainder of its own alignment unit that is already partially occupied by
    // 'A'. Otherwise 'B' is pushed into its corresponding next alignment unit.
    //
    // E.g., in 'struct s { char a: 7; int b: 25; };', 'b' is placed directly
    // preceding 'a' and a 'struct s' allocates 4 bytes.
    // On the other hand, in 'struct s { char a: 7; int b: 26; };', the 25 remaining
    // bits int the first integer alignment of 'struct s' are padded and 'b' is pushed
    // to the next integer-aligned unit, resulting in 'struct s' having 8 bytes size.
    long paddingBitSpace = getPaddingInBits(pBitFieldOffset, pType, pSizeOfByte);

    if (paddingBitSpace < pBitFieldLength) {
      pBitFieldOffset += paddingBitSpace;
    }
    return pBitFieldOffset;
  }

  @Deprecated
  public long calculatePaddedBitsize(
      long pBitOffset, long pSizeOfConsecutiveBitFields, CType pType, long pSizeOfByte) {
    pBitOffset += pSizeOfConsecutiveBitFields;
    // once pad the bits to full bytes, then pad bytes to the
    // alignment of the current type
    pBitOffset = sizeofVisitor.calculateByteSize(Math.toIntExact(pBitOffset));

    return (pBitOffset + getPadding(pBitOffset, pType)) * pSizeOfByte;
  }

  @Deprecated
  public long getPadding(long pOffset, CType pType) {
    return getPaddingInBits(pOffset, pType, 1L);
  }

  private long getPaddingInBits(long pOffset, CType pType, long pSizeOfByte) {
    long alignof = getAlignof(pType) * pSizeOfByte;
    long padding = alignof - (pOffset % alignof);
    if (padding < alignof) {
      return padding;
    }
    return 0;
  }
}
