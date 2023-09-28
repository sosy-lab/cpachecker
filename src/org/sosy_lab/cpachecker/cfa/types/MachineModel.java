// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
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
      4, // int
      4, // long int
      4, // long long int
      4, // float
      4, // double
      4, // long double

      // alignof other
      1, // void
      1, // bool
      4, // pointer
      8, // malloc
      true, // char is signed
      ByteOrder.LITTLE_ENDIAN // endianness
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
      8, // pointer
      16, // malloc
      true, // char is signed
      ByteOrder.LITTLE_ENDIAN // endianness
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
      8, // long long int
      4, // float
      8, // double
      8, // long double

      // alignof other
      1, // void
      1, // bool
      4, // pointer
      8, // malloc
      false, // char is signed
      ByteOrder.LITTLE_ENDIAN // endianness
      ),

  /** Machine model representing an ARM64 machine with alignment: */
  ARM64(
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
      8, // pointer
      16, // malloc
      false, // char is signed
      ByteOrder.LITTLE_ENDIAN // endianness
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

  private final transient ByteOrder endianness;

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
  private final int alignofMalloc;

  // according to ANSI C, sizeof(char) is always 1
  private final int mSizeofChar = 1;
  private final int mAlignofChar = 1;
  private final boolean defaultCharSigned;

  // a char is always a byte, but a byte doesn't have to be 8 bits
  private final int mSizeofCharInBits = 8;
  private final CSimpleType intptr_t;
  private final CSimpleType uintptr_t;

  MachineModel(
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
      int pAlignofPtr,
      int pAlignofMalloc,
      boolean pDefaultCharSigned,
      ByteOrder pEndianness) {
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
    alignofMalloc = pAlignofMalloc;
    defaultCharSigned = pDefaultCharSigned;
    endianness = pEndianness;

    if (sizeofPtr == sizeofInt) {
      intptr_t = CNumericTypes.INT;
      uintptr_t = CNumericTypes.UNSIGNED_INT;
    } else if (sizeofPtr == sizeofLongInt) {
      intptr_t = CNumericTypes.LONG_INT;
      uintptr_t = CNumericTypes.UNSIGNED_LONG_INT;
    } else if (sizeofPtr == sizeofLongLongInt) {
      intptr_t = CNumericTypes.LONG_LONG_INT;
      uintptr_t = CNumericTypes.UNSIGNED_LONG_LONG_INT;
    } else if (sizeofPtr == sizeofShort) {
      intptr_t = CNumericTypes.SHORT_INT;
      uintptr_t = CNumericTypes.UNSIGNED_SHORT_INT;
    } else {
      throw new AssertionError("No ptr-Equivalent found");
    }
  }

  /**
   * This method returns an integer type with the same size as pointers.
   *
   * <p>The returned type happens to be signed, but this should not be interpreted in any way as a
   * statement that pointers are signed, and callers should not rely on it. Use {@link
   * #getPointerSizedSignedIntType()} if you do.
   *
   * <p>Note that often using one of the other methods such as {@link #getPointerAsIntType()} is
   * recommended.
   */
  public CSimpleType getPointerSizedIntType() {
    // On our platforms, intptr_t has the same size as pointers.
    return intptr_t;
  }

  /**
   * This method returns a signed integer type with the same size as pointers.
   *
   * <p>Of course, pointers are not really signed, but GCC does sign extension when casting from a
   * pointer to an int (https://gcc.gnu.org/onlinedocs/gcc/Arrays-and-pointers-implementation.html),
   * so the returned type can be conveniently used in such cast calculations.
   */
  public CSimpleType getPointerSizedSignedIntType() {
    // On our platforms, intptr_t has the same size as pointers. And it is signed.
    return intptr_t;
  }

  /**
   * This method returns the <code>intptr_t</code> type, a signed integer type capable of
   * representing all pointer values.
   *
   * <p>Note that this does not guarantee that its size is the same as the size of a pointer.
   */
  public CSimpleType getPointerAsIntType() {
    return intptr_t;
  }

  /**
   * This method returns the <code>uintptr_t</code> type, an unsigned integer type capable of
   * representing all pointer values.
   *
   * <p>Note that this does not guarantee that its size is the same as the size of a pointer.
   */
  public CSimpleType getPointerAsUnsignedIntType() {
    return uintptr_t;
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
    // On our platforms, intptr_t and ptrdiff_t are the same.
    return intptr_t;
  }

  /**
   * This method returns the <code>size_t</code> type. This is an unsigned integer type capable of
   * representing allocation sizes.
   */
  public CSimpleType getSizeType() {
    // On our platforms, uintptr_t and size_t are the same.
    return uintptr_t;
  }

  /**
   * This method returns the <code>ssize_t</code> type. This is an signed integer type capable of
   * representing allocation sizes and -1.
   */
  public CSimpleType getSignedSizeType() {
    // On our platforms, intptr_t and ssize_t are the same.
    return intptr_t;
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
    return defaultCharSigned;
  }

  /**
   * Determine whether a type is signed or unsigned. Contrary to {@link
   * CSimpleType#hasSignedSpecifier()} and {@link CSimpleType#hasUnsignedSpecifier()} this method
   * leaves no third option and should thus be preferred. For floating point types it returns true,
   * for types where signedness makes no sense (bool, void) it returns false.
   */
  public boolean isSigned(CSimpleType t) {
    // resolve UNSPECIFIED and INT to SIGNED INT etc.
    t = t.getCanonicalType();

    if (t.hasSignedSpecifier()) {
      return true;
    } else if (t.hasUnsignedSpecifier()) {
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

  public int getSizeofInt128() {
    return 128 / getSizeofCharInBits();
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

  public int getSizeofFloat128() {
    return 128 / getSizeofCharInBits();
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
        if (type.hasLongLongSpecifier()) {
          return getSizeofLongLongInt();
        } else if (type.hasLongSpecifier()) {
          return getSizeofLongInt();
        } else if (type.hasShortSpecifier()) {
          return getSizeofShort();
        } else {
          return getSizeofInt();
        }
      case INT128:
        return getSizeofInt128();
      case DOUBLE:
        if (type.hasLongSpecifier()) {
          return getSizeofLongDouble();
        } else {
          return getSizeofDouble();
        }
      case FLOAT128:
        return getSizeofFloat128();
      default:
        throw new AssertionError("Unrecognized CBasicType " + type.getType());
    }
  }

  public ByteOrder getEndianness() {
    return endianness;
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

  public int getAlignofInt128() {
    return getSizeofInt128(); // alignment is same as size for this type
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

  public int getAlignofFloat128() {
    return getSizeofFloat128(); // alignment is same as size for this type
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

  /**
   * Return the alignment that malloc needs to guarantee for the returned memory. Source:
   * https://www.gnu.org/software/libc/manual/html_node/Malloc-Examples.html
   */
  public int getAlignofMalloc() {
    return alignofMalloc;
  }

  /** returns INT, if the type is smaller than INT, else the type itself. */
  public CType applyIntegerPromotion(CType pType) {
    checkArgument(CTypes.isIntegerType(pType), "Integer promotion cannot be applied to %s", pType);
    /*
     * ISO-C99 (6.3.1.1 #2):
     * If an int can represent all values of the original type, the value is
     * converted to an int; otherwise, it is converted to an unsigned int.
     * These are called the integer promotions.
     * All smaller integer types actually fit in an int, even (n-1)-bit bitfields.
     */
    if (getSizeof(pType).compareTo(BigInteger.valueOf(getSizeofInt())) < 0) {
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
  private final BaseSizeofVisitor sizeofVisitor = new BaseSizeofVisitor(this);

  public BigInteger getSizeof(CType pType) {
    checkArgument(
        pType.getCanonicalType() instanceof CVoidType || !pType.isIncomplete(),
        "Cannot compute size of incomplete type %s",
        pType);
    return getSizeof(pType, sizeofVisitor);
  }

  public BigInteger getSizeof(CType pType, BaseSizeofVisitor pSizeofVisitor) {
    checkNotNull(pSizeofVisitor);
    return pType.accept(pSizeofVisitor);
  }

  public int getSizeofPtrInBits() {
    return getSizeofPtr() * getSizeofCharInBits();
  }

  public BigInteger getSizeofInBits(CType pType) {
    return getSizeofInBits(pType, sizeofVisitor);
  }

  public BigInteger getSizeofInBits(CType pType, BaseSizeofVisitor pSizeofVisitor) {
    checkNotNull(pSizeofVisitor);
    if (pType instanceof CBitFieldType) {
      return BigInteger.valueOf(((CBitFieldType) pType).getBitFieldSize());
    } else {
      return getSizeof(pType, pSizeofVisitor).multiply(BigInteger.valueOf(getSizeofCharInBits()));
    }
  }

  @SuppressFBWarnings("SE_BAD_FIELD_STORE")
  @SuppressWarnings("ImmutableEnumChecker")
  private final CTypeVisitor<Integer, IllegalArgumentException> alignofVisitor =
      new BaseAlignofVisitor(this);

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
  public Map<CCompositeTypeMemberDeclaration, BigInteger> getAllFieldOffsetsInBits(
      CCompositeType pOwnerType) {
    ImmutableMap.Builder<CCompositeTypeMemberDeclaration, BigInteger> outParameterMap =
        ImmutableMap.builder();

    getFieldOffsetOrSizeOrFieldOffsetsMappedInBits(pOwnerType, null, outParameterMap);

    return outParameterMap.buildOrThrow();
  }

  /**
   * Calculates the offset of pFieldName in pOwnerType in bits.
   *
   * @param pOwnerType a {@link CCompositeType} to calculate its field offset
   * @param pFieldName the name of the field to calculate its offset
   * @return the offset of the given field
   */
  public BigInteger getFieldOffsetInBits(CCompositeType pOwnerType, String pFieldName) {
    checkNotNull(pFieldName);
    return getFieldOffsetOrSizeOrFieldOffsetsMappedInBits(pOwnerType, pFieldName, null);
  }

  /**
   * Calculates the offset of pFieldName in pOwnerType in bytes. Returns an empty optional if the
   * field is a non-byte-aligned bit field.
   *
   * @param pOwnerType a {@link CCompositeType} to calculate its field offset
   * @param pFieldName the name of the field to calculate its offset
   * @return the offset of the given field
   */
  public Optional<BigInteger> getFieldOffsetInBytes(CCompositeType pOwnerType, String pFieldName) {
    BigInteger bitOffset = getFieldOffsetInBits(pOwnerType, pFieldName);
    BigInteger[] divAndRemain = bitOffset.divideAndRemainder(BigInteger.valueOf(mSizeofCharInBits));
    if (divAndRemain[1].equals(BigInteger.ZERO)) {
      return Optional.of(divAndRemain[0]);
    }
    // bit-field with non-byte-aligned offset
    return Optional.empty();
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
  BigInteger getFieldOffsetOrSizeOrFieldOffsetsMappedInBits(
      CCompositeType pOwnerType,
      @Nullable String pFieldName,
      ImmutableMap.@Nullable Builder<CCompositeTypeMemberDeclaration, BigInteger> outParameterMap) {
    checkArgument(
        (pFieldName == null) || (outParameterMap == null),
        "Call of this method does only make sense if either pFieldName or outParameterMap "
            + "is of value null, otherwise it either stops the calculation with an incomplete "
            + "map or wastes ressources by filling a map with values that are not required.");
    final ComplexTypeKind ownerTypeKind = pOwnerType.getKind();
    List<CCompositeTypeMemberDeclaration> typeMembers = pOwnerType.getMembers();

    BigInteger bitOffset = BigInteger.ZERO;
    BigInteger sizeOfConsecutiveBitFields = BigInteger.ZERO;

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
          outParameterMap.put(typeMember, BigInteger.ZERO);
        }
      }
    } else if (ownerTypeKind == ComplexTypeKind.STRUCT) {

      for (Iterator<CCompositeTypeMemberDeclaration> iterator = typeMembers.iterator();
          iterator.hasNext(); ) {
        CCompositeTypeMemberDeclaration typeMember = iterator.next();
        CType type = typeMember.getType();

        final BigInteger fieldSizeInBits;
        if (!iterator.hasNext() && typeMember.isFlexibleArrayMember()) {
          // If incomplete type at end of struct, just assume 0 for its size
          // and compute its offset as usual, since it isn't affected.
          fieldSizeInBits = BigInteger.ZERO;
        } else {
          fieldSizeInBits = getSizeofInBits(type);
        }

        if (type instanceof CBitFieldType) {
          if (typeMember.getName().equals(pFieldName)) {
            // just escape the loop and return the current offset
            bitOffset = bitOffset.add(sizeOfConsecutiveBitFields);
            return bitOffset;
          }

          CType innerType = ((CBitFieldType) type).getType();

          if (fieldSizeInBits.compareTo(BigInteger.ZERO) == 0) {
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
                calculatePaddedBitsize(
                    bitOffset, sizeOfConsecutiveBitFields, innerType, sizeOfByte);
            sizeOfConsecutiveBitFields = BigInteger.ZERO;
          } else {
            sizeOfConsecutiveBitFields =
                calculateNecessaryBitfieldOffset(
                        sizeOfConsecutiveBitFields.add(bitOffset),
                        innerType,
                        sizeOfByte,
                        fieldSizeInBits)
                    .subtract(bitOffset);
            sizeOfConsecutiveBitFields = sizeOfConsecutiveBitFields.add(fieldSizeInBits);
          }

          // Put start offset of bitField to outParameterMap
          if (outParameterMap != null) {
            outParameterMap.put(
                typeMember, bitOffset.add(sizeOfConsecutiveBitFields).subtract(fieldSizeInBits));
          }
        } else {
          bitOffset =
              calculatePaddedBitsize(bitOffset, sizeOfConsecutiveBitFields, type, sizeOfByte);
          sizeOfConsecutiveBitFields = BigInteger.ZERO;

          if (typeMember.getName().equals(pFieldName)) {
            // just escape the loop and return the current offset
            return bitOffset;
          }

          if (outParameterMap != null) {
            outParameterMap.put(typeMember, bitOffset);
          }
          bitOffset = bitOffset.add(fieldSizeInBits);
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
  public BigInteger calculateNecessaryBitfieldOffset(
      BigInteger pBitFieldOffset, CType pType, long pSizeOfByte, BigInteger pBitFieldLength) {
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
    BigInteger paddingBitSpace = getPaddingInBits(pBitFieldOffset, pType, pSizeOfByte);

    if (paddingBitSpace.compareTo(pBitFieldLength) < 0) {
      pBitFieldOffset = pBitFieldOffset.add(paddingBitSpace);
    }
    return pBitFieldOffset;
  }

  @Deprecated
  public BigInteger calculatePaddedBitsize(
      BigInteger pBitOffset,
      BigInteger pSizeOfConsecutiveBitFields,
      CType pType,
      long pSizeOfByte) {
    pBitOffset = pBitOffset.add(pSizeOfConsecutiveBitFields);
    // once pad the bits to full bytes, then pad bytes to the
    // alignment of the current type
    pBitOffset = sizeofVisitor.calculateByteSize(pBitOffset);

    return pBitOffset.add(getPadding(pBitOffset, pType)).multiply(BigInteger.valueOf(pSizeOfByte));
  }

  @Deprecated
  public BigInteger getPadding(BigInteger pOffset, CType pType) {
    return getPaddingInBits(pOffset, pType, 1L);
  }

  private BigInteger getPaddingInBits(BigInteger pOffset, CType pType, long pSizeOfByte) {
    BigInteger alignof = BigInteger.valueOf(getAlignof(pType) * pSizeOfByte);
    BigInteger padding = alignof.subtract(pOffset.mod(alignof));
    if (padding.compareTo(alignof) < 0) {
      return padding;
    }
    return BigInteger.ZERO;
  }
}
