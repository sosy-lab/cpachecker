// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

public class BaseSizeofVisitor<X extends Exception> implements CTypeVisitor<BigInteger, X> {
  private final MachineModel model;

  protected BaseSizeofVisitor(MachineModel model) {
    this.model = model;
  }

  @Override
  public BigInteger visit(CArrayType pArrayType) throws X {
    // TODO: Take possible padding into account

    CExpression arrayLength = pArrayType.getLength();

    if (arrayLength instanceof CIntegerLiteralExpression) {
      BigInteger length = ((CIntegerLiteralExpression) arrayLength).getValue();

      BigInteger sizeOfType = model.getSizeof(pArrayType.getType(), this);
      return length.multiply(sizeOfType);
    }

    // Treat arrays with variable length as pointer.
    return BigInteger.valueOf(model.getSizeofPtr());
  }

  @Override
  public BigInteger visit(CCompositeType pCompositeType) throws X {

    switch (pCompositeType.getKind()) {
      case STRUCT:
        return getFieldOffsetOrSizeOrFieldOffsetsMappedInBits(pCompositeType, null, null);
      case UNION:
        return handleSizeOfUnion(pCompositeType);
      case ENUM: // There is no such kind of Composit Type.
      default:
        throw new AssertionError();
    }
  }

  BigInteger calculateByteSize(BigInteger pBitFieldsSize) {
    if (pBitFieldsSize.compareTo(BigInteger.ZERO) == 0) {
      return BigInteger.ZERO;
    }

    BigInteger charSizeInBits = BigInteger.valueOf(model.getSizeofCharInBits());
    BigInteger result = pBitFieldsSize.divide(charSizeInBits);
    if (pBitFieldsSize.mod(charSizeInBits).compareTo(BigInteger.ZERO) > 0) {
      result = result.add(BigInteger.ONE);
    }
    return result;
  }

  private BigInteger handleSizeOfUnion(CCompositeType pCompositeType) throws X {
    BigInteger size = BigInteger.ZERO;
    BigInteger sizeOfType = BigInteger.ZERO;
    // TODO: Take possible padding into account
    for (CCompositeTypeMemberDeclaration decl : pCompositeType.getMembers()) {
      sizeOfType = decl.getType().accept(this);
      size = size.max(sizeOfType);
    }
    return size;
  }

  @Override
  public BigInteger visit(CElaboratedType pElaboratedType) throws X {
    CType def = pElaboratedType.getRealType();
    if (def != null) {
      return def.accept(this);
    }

    if (pElaboratedType.getKind() == ComplexTypeKind.ENUM) {
      return BigInteger.valueOf(model.getSizeofInt());
    }

    throw new IllegalArgumentException("Cannot compute size of incomplete type " + pElaboratedType);
  }

  @Override
  public BigInteger visit(CEnumType pEnumType) throws IllegalArgumentException {
    return BigInteger.valueOf(model.getSizeof(pEnumType.getCompatibleType()));
  }

  @Override
  public BigInteger visit(CFunctionType pFunctionType) throws X {
    // A function does not really have a size,
    // but references to functions can be used as pointers.
    return BigInteger.valueOf(model.getSizeofPtr());
  }

  @Override
  public BigInteger visit(CPointerType pPointerType) throws X {
    return BigInteger.valueOf(model.getSizeofPtr());
  }

  @Override
  public BigInteger visit(CProblemType pProblemType) throws X {
    throw new IllegalArgumentException("Unknown C-Type: " + pProblemType.getClass());
  }

  @Override
  public BigInteger visit(CSimpleType pSimpleType) throws X {
    return BigInteger.valueOf(model.getSizeof(pSimpleType));
  }

  @Override
  public BigInteger visit(CTypedefType pTypedefType) throws X {
    return pTypedefType.getRealType().accept(this);
  }

  @Override
  public BigInteger visit(CVoidType pVoidType) throws X {
    return BigInteger.valueOf(model.getSizeofVoid());
  }

  @Override
  public BigInteger visit(CBitFieldType pCBitFieldType) throws X {
    return calculateByteSize(BigInteger.valueOf(pCBitFieldType.getBitFieldSize()));
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
  @SuppressWarnings("deprecation") // these methods are intended to be called here
  BigInteger getFieldOffsetOrSizeOrFieldOffsetsMappedInBits(
      CCompositeType pOwnerType,
      @Nullable String pFieldName,
      ImmutableMap.@Nullable Builder<CCompositeTypeMemberDeclaration, BigInteger> outParameterMap)
      throws X {
    checkArgument(
        (pFieldName == null) || (outParameterMap == null),
        "Call of this method does only make sense if either pFieldName or outParameterMap "
            + "is of value null, otherwise it either stops the calculation with an incomplete "
            + "map or wastes resources by filling a map with values that are not required.");
    final ComplexTypeKind ownerTypeKind = pOwnerType.getKind();
    List<CCompositeTypeMemberDeclaration> typeMembers = pOwnerType.getMembers();

    BigInteger bitOffset = BigInteger.ZERO;
    BigInteger sizeOfConsecutiveBitFields = BigInteger.ZERO;

    long sizeOfByte = model.getSizeofCharInBits();

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
          fieldSizeInBits = model.getSizeofInBits(type, this);
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
                model.calculatePaddedBitsize(
                    bitOffset, sizeOfConsecutiveBitFields, innerType, sizeOfByte);
            sizeOfConsecutiveBitFields = BigInteger.ZERO;
          } else {
            sizeOfConsecutiveBitFields =
                model
                    .calculateNecessaryBitfieldOffset(
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
              model.calculatePaddedBitsize(bitOffset, sizeOfConsecutiveBitFields, type, sizeOfByte);
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
    return model.calculatePaddedBitsize(bitOffset, sizeOfConsecutiveBitFields, pOwnerType, 1L);
  }
}
