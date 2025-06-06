// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.exceptions.NoException;

/** Helper methods for CType instances. */
public final class CTypes {

  private CTypes() {}

  /**
   * Check whether a given type is a character type according to the C standard § 6.2.5 (15). Also
   * returns true for all qualified versions of real types.
   */
  public static boolean isCharacterType(CType type) {
    type = type.getCanonicalType();
    return (type instanceof CSimpleType) && (((CSimpleType) type).getType() == CBasicType.CHAR);
  }

  /**
   * Check whether a given type is a real type according to the C standard § 6.2.5 (17), i.e., a
   * non-complex arithmetic type. Also returns true for all qualified versions of real types.
   */
  public static boolean isRealType(CType type) {
    type = type.getCanonicalType();
    return (type instanceof CEnumType)
        // C11 § 6.7.2.1 (10) "A bit-field is interpreted as having a signed or unsigned integer
        // type"
        || (type instanceof CBitFieldType)
        || (type instanceof CSimpleType && !((CSimpleType) type).hasComplexSpecifier());
  }

  /**
   * Check whether a given type is an integer type according to the C standard § 6.2.5 (17). Also
   * returns true for all qualified versions of integer types.
   */
  public static boolean isIntegerType(CType type) {
    type = type.getCanonicalType();
    return (type instanceof CEnumType)
        // C11 § 6.7.2.1 (10) "A bit-field is interpreted as having a signed or unsigned integer
        // type"
        || (type instanceof CBitFieldType)
        || (type instanceof CSimpleType && ((CSimpleType) type).getType().isIntegerType());
  }

  /**
   * Check whether a given type is an arithmetic type according to the C standard § 6.2.5 (18). Also
   * returns true for all qualified versions of arithmetic types.
   */
  public static boolean isArithmeticType(CType type) {
    type = type.getCanonicalType();
    return (type instanceof CEnumType)
        // C11 § 6.7.2.1 (10) "A bit-field is interpreted as having a signed or unsigned integer
        // type"
        || (type instanceof CBitFieldType)
        || (type instanceof CSimpleType)
        || ((type instanceof CComplexType)
            && ((CComplexType) type).getKind() == ComplexTypeKind.ENUM);
  }

  /**
   * Check whether a given type is a scalar type according to the C standard § 6.2.5 (21). Also
   * returns true for all qualified versions of scalar types.
   */
  public static boolean isScalarType(CType type) {
    type = type.getCanonicalType();
    return (type instanceof CPointerType) || isArithmeticType(type);
  }

  /**
   * Check whether a given type is a bool type. Also returns true for all qualified versions of bool
   * types.
   */
  public static boolean isBoolType(CType type) {
    type = type.getCanonicalType();
    if (type instanceof CBitFieldType bitFieldType) {
      type = bitFieldType.getType();
    }
    return type instanceof CSimpleType simpleType && simpleType.getType() == CBasicType.BOOL;
  }

  /**
   * Check whether a given type is a scalar type according to the C standard § 6.2.5 (21). Also
   * returns true for all qualified versions of aggregate types.
   */
  public static boolean isAggregateType(CType type) {
    type = type.getCanonicalType();
    return (type instanceof CArrayType)
        || (type instanceof CCompositeType
            && (((CCompositeType) type).getKind() == ComplexTypeKind.STRUCT));
  }

  /**
   * Check whether a given type is a scalar type according to the C standard § 6.2.5, i.e., not a
   * function type. Also returns true for all qualified versions of object types.
   */
  public static boolean isObjectType(CType type) {
    type = type.getCanonicalType();
    return !(type instanceof CFunctionType);
  }

  /** Check whether the given type is a pointer to a function. */
  public static boolean isFunctionPointer(CType type) {
    type = type.getCanonicalType();
    if (type instanceof CPointerType) {
      CType innerType = ((CPointerType) type).getType();
      if (innerType instanceof CFunctionType) {
        return true;
      }
    }
    return false;
  }

  private static class CanonicalCTypeEquivalence extends Equivalence<CType> {
    private static final CanonicalCTypeEquivalence INSTANCE = new CanonicalCTypeEquivalence();

    @Override
    protected boolean doEquivalent(CType pA, CType pB) {
      return pA.getCanonicalType().equals(pB.getCanonicalType());
    }

    @Override
    protected int doHash(CType pT) {
      return pT.getCanonicalType().hashCode();
    }
  }

  /**
   * Return an {@link Equivalence} based on the canonical type, i.e., two types are defined as equal
   * if their canonical types are equal.
   */
  public static Equivalence<CType> canonicalTypeEquivalence() {
    return CanonicalCTypeEquivalence.INSTANCE;
  }

  /**
   * Return a copy of a given type that has the "const" flag not set. If the given type is already a
   * non-const type, it is returned unchanged.
   *
   * <p>This method only eliminates the outermost const flag, if it is present, i.e., it does not
   * change a non-const pointer to a const int.
   */
  public static <T extends CType> T withoutConst(T type) {
    return withConstSetTo(type, false);
  }

  /**
   * Return a copy of a given type that has the "const" flag set. If the given type is already a
   * const type, it is returned unchanged.
   *
   * <p>This method only adds the outermost const flag, if it is not present, i.e., it does not
   * change a const pointer to a non-const int.
   */
  public static <T extends CType> T withConst(T type) {
    return withConstSetTo(type, true);
  }

  /**
   * Return a copy of a given type that has the "const" flag set to the given value.
   *
   * <p>This method only changes the outermost const flag.
   *
   * <p>If you want to set the const flag to a constant, prefer {@link #withoutConst(CType)} and
   * {@link #withConst(CType)}.
   */
  public static <T extends CType> T withConstSetTo(T type, boolean newConstValue) {
    if (type instanceof CProblemType) {
      return type;
    }

    if (type.isConst() == newConstValue) {
      return type;
    }
    @SuppressWarnings("unchecked") // Visitor always creates instances of exact same class
    T result = (T) type.accept(newConstValue ? ForceConstVisitor.TRUE : ForceConstVisitor.FALSE);
    return result;
  }

  /**
   * Return a copy of a given type that has the "volatile" flag not set. If the given type is
   * already a non-volatile type, it is returned unchanged.
   *
   * <p>This method only eliminates the outermost volatile flag, if it is present, i.e., it does not
   * change a non-volatile pointer to a volatile int.
   */
  public static <T extends CType> T withoutVolatile(T type) {
    return withVolatileSetTo(type, false);
  }

  /**
   * Return a copy of a given type that has the "volatile" flag set. If the given type is already a
   * volatile type, it is returned unchanged.
   *
   * <p>This method only adds the outermost volatile flag, if it is not present, i.e., it does not
   * change a volatile pointer to a non-volatile int.
   */
  public static <T extends CType> T withVolatile(T type) {
    return withVolatileSetTo(type, true);
  }

  /**
   * Return a copy of a given type that has the "volatile" flag set to the given value.
   *
   * <p>This method only changes the outermost volatile flag.
   *
   * <p>If you want to set the volatile flag to a constant, prefer {@link #withoutVolatile(CType)}
   * and {@link #withVolatile(CType)}.
   */
  public static <T extends CType> T withVolatileSetTo(T type, boolean newVolatileValue) {
    if (type instanceof CProblemType) {
      return type;
    }

    if (type.isVolatile() == newVolatileValue) {
      return type;
    }
    @SuppressWarnings("unchecked") // Visitor always creates instances of exact same class
    T result =
        (T) type.accept(newVolatileValue ? ForceVolatileVisitor.TRUE : ForceVolatileVisitor.FALSE);
    return result;
  }

  /**
   * Implements a compatibility check for {@link CType}s according to C-Standard §6.2.7. This
   * definition is symmetric, therefore the order of the parameters doesn't matter. This definition
   * is especially stricter than assignment compatibility (cf. {@link
   * CType#canBeAssignedFrom(CType)}).
   *
   * <p>Note that two types being compatible does not necessarily imply defined behavior in every
   * context (cf. array and function types).
   *
   * <p>Also note that we don't consider every definition for compatibility, since some are
   * irrelevant after our pre-processing (e.g., there are no structures, functions, or unions with
   * different translation units left).
   *
   * @param pTypeA one {@link CType} to be checked for compatibility with another
   * @param pTypeB one {@link CType} to be checked for compatibility with another
   * @return <b><code>true</code></b> if the two types are compatible<br>
   *     <b><code>false</code></b> if the two types are not compatible
   */
  public static boolean areTypesCompatible(CType pTypeA, CType pTypeB) {
    // If those types' canonical types equal each other, they're trivially compatible.
    //
    // Due to {@link CSimpleType}'s equals method, this is already
    // exact for simple types according to C-Standard §6.2.7 (1), §6.7.2 (5),
    // and §6.7.3 (10).
    // It also holds for {@link CTypedefType}s, since their canonical type
    // is the canonical type of their real type.
    //
    // Note, that in particular the signed and the unsigned version
    // of a type are not compatible to each other, since the conversion
    // of, e.g., an unsigned int to a signed int might change the
    // expressions value from a positive to a negative one and vice-versa,
    // which is explicitly forbidden for compatible types (C-Standard §6.3 (2)).
    //
    // Note also, that even the equals methods of the other {@link CType}s
    // return <code>true</code> only if they are compatible, but that there
    // are still some cases were two types are compatible but not equal.
    if (canonicalTypeEquivalence().equivalent(pTypeA, pTypeB)) {
      return true;
    }

    if (!(pTypeA.isConst() == pTypeB.isConst() && pTypeA.isVolatile() == pTypeB.isVolatile())) {
      // Cf. C-Standard §6.7.3 (10), two qualified types have to be
      // an identically qualified version of compatible types to be compatible.
      return false;
    }

    if (pTypeA instanceof CPointerType pointerA && pTypeB instanceof CPointerType pointerB) {
      // Cf. C-Standard §6.7.6.1 (2), compatible pointers shall point to compatible types.
      return areTypesCompatible(pointerA.getType(), pointerB.getType());
    }

    CType basicSignedInt =
        CNumericTypes.INT.getCanonicalType(pTypeA.isConst(), pTypeA.isVolatile());

    // Cf. C-Standard §6.7.2.2 (4), enumerated types shall be compatible with
    // char, a signed integer type, or an unsigned integer type, dependent on
    // implementation.
    // We chose the implementation with compatibility to 'signed int', that GCC
    // seemingly uses.
    if (pTypeA instanceof CEnumType && pTypeB instanceof CSimpleType) {
      return pTypeB.getCanonicalType().equals(basicSignedInt.getCanonicalType());
    } else if (pTypeA instanceof CSimpleType && pTypeB instanceof CEnumType) {
      return pTypeA.getCanonicalType().equals(basicSignedInt.getCanonicalType());
    }

    if ((pTypeA instanceof CArrayType arrayA && pTypeB instanceof CArrayType arrayB)
        && areTypesCompatible(arrayA.getType(), arrayB.getType())) {
      // Cf. C-Standard §6.7.6.2 (6).
      OptionalInt lengthA = arrayA.getLengthAsInt();
      OptionalInt lengthB = arrayB.getLengthAsInt();

      if (lengthA.isPresent() && lengthB.isPresent()) {
        if (lengthA.orElseThrow() == lengthB.orElseThrow()) {
          return true;
        }
      } else {
        // In this case we only get defined behavior,
        // if the size specifiers of both array types
        // evaluate to the same value.
        // According to the standard they are compatible
        // nonetheless.
        return true;
      }
    }

    // Cf. C-Standard 6.7.6.3 (15)
    if (pTypeA instanceof CFunctionType functionA && pTypeB instanceof CFunctionType functionB) {
      if (!areTypesCompatible(functionA.getReturnType(), functionB.getReturnType())) {
        return false;
      }

      List<CType> paramsA = functionA.getParameters();
      List<CType> paramsB = functionB.getParameters();

      if (paramsA.size() != paramsB.size()
          || functionA.takesVarArgs() != functionB.takesVarArgs()) {
        return false;
      }

      Iterator<CType> iteratorA = paramsA.iterator();
      Iterator<CType> iteratorB = paramsB.iterator();
      CType paramOfA;
      CType paramOfB;

      while (iteratorA.hasNext() && iteratorB.hasNext()) {
        paramOfA = iteratorA.next();
        paramOfB = iteratorB.next();

        // Cf. C-Standard §6.7.6.3 (15, last sentence in parentheses):
        // "... each parameter declared with function or array type
        // is taken as having the adjusted type ..."
        //
        // ... and C-Standard §6.7.6.3 (6 & 7).
        if (paramOfA instanceof CPointerType && !(paramOfB instanceof CPointerType)) {
          paramOfB = adjustFunctionOrArrayType(paramOfB);
        } else if (paramOfB instanceof CPointerType) {
          paramOfA = adjustFunctionOrArrayType(paramOfA);
        }

        // C-Standard §6.7.6.3 (15, last sentence in parentheses):
        // "... each parameter declared with qualified type is taken
        // as having the unqualified version of its declared type."
        paramOfA = copyDequalified(paramOfA);
        paramOfB = copyDequalified(paramOfB);

        if (!areTypesCompatible(paramOfA, paramOfB)) {
          return false;
        }
      }

      return true;
    }

    // default case
    return false;
  }

  /**
   * Implements adjustment of function and array types according to C-Standard §6.7.6.3 (7 & 8).
   *
   * <p>If <code>pType</code> is not an instance of {@link CArrayType} or {@link CFunctionType}, it
   * is returned unchanged.
   *
   * @param pType the {@link CType} to be adjusted, if necessary
   * @return the adjusted version of <code>pType</code>
   */
  public static @Nullable CType adjustFunctionOrArrayType(@Nullable CType pType) {
    if (pType == null) {
      return pType;
    }

    if (pType instanceof CArrayType) {
      CType innerType = ((CArrayType) pType).getType();
      CExpression sizeExpression = ((CArrayType) pType).getLength();

      if (sizeExpression == null) {
        pType = new CPointerType(false, false, innerType);
      } else {
        // Adjusting an array type to a pointer of its stored
        // type discards the qualifiers of the array type and
        // instead qualifies the pointer with the qualifiers
        // used inside the size specifier brackets.
        CType sizeType = sizeExpression.getExpressionType();
        pType = new CPointerType(sizeType.isConst(), sizeType.isVolatile(), innerType);
      }
    } else if (pType instanceof CFunctionType) {
      pType = new CPointerType(pType.isConst(), pType.isVolatile(), pType);
    }
    return pType;
  }

  /**
   * Creates an instance of {@link CType} that is an exact copy of <code>pType</code>, but is
   * guaranteed to not be qualified as either <code>const</code> or <code>volatile</code>.
   *
   * @param pType the {@link CType} to copy without qualifiers
   * @return a copy of <code>pType</code> without qualifiers
   */
  public static <T extends CType> T copyDequalified(T pType) {
    pType = withoutConst(pType);
    pType = withoutVolatile(pType);
    return pType;
  }

  private enum ForceConstVisitor implements CTypeVisitor<CType, NoException> {
    FALSE(false),
    TRUE(true);

    private final boolean constValue;

    ForceConstVisitor(boolean pConstValue) {
      constValue = pConstValue;
    }

    // Make sure to always return instances of exactly the same classes!

    @Override
    public CArrayType visit(CArrayType t) {
      return new CArrayType(constValue, t.isVolatile(), t.getType(), t.getLength());
    }

    @Override
    public CCompositeType visit(CCompositeType t) {
      return new CCompositeType(
          constValue, t.isVolatile(), t.getKind(), t.getMembers(), t.getName(), t.getOrigName());
    }

    @Override
    public CElaboratedType visit(CElaboratedType t) {
      return new CElaboratedType(
          constValue, t.isVolatile(), t.getKind(), t.getName(), t.getOrigName(), t.getRealType());
    }

    @Override
    public CEnumType visit(CEnumType t) {
      return new CEnumType(
          constValue,
          t.isVolatile(),
          t.getCompatibleType(),
          t.getEnumerators(),
          t.getName(),
          t.getOrigName());
    }

    @Override
    public CFunctionType visit(CFunctionType t) {
      checkArgument(!constValue, "Cannot create const function type, this is undefined");
      return t;
    }

    @Override
    public CPointerType visit(CPointerType t) {
      return new CPointerType(constValue, t.isVolatile(), t.getType());
    }

    @Override
    public CProblemType visit(CProblemType t) {
      return t;
    }

    @Override
    public CSimpleType visit(CSimpleType t) {
      return new CSimpleType(
          constValue,
          t.isVolatile(),
          t.getType(),
          t.hasLongSpecifier(),
          t.hasShortSpecifier(),
          t.hasSignedSpecifier(),
          t.hasUnsignedSpecifier(),
          t.hasComplexSpecifier(),
          t.hasImaginarySpecifier(),
          t.hasLongLongSpecifier());
    }

    @Override
    public CTypedefType visit(CTypedefType t) {
      return new CTypedefType(constValue, t.isVolatile(), t.getName(), t.getRealType());
    }

    @Override
    public CType visit(CVoidType t) {
      return CVoidType.create(constValue, t.isVolatile());
    }

    @Override
    public CType visit(CBitFieldType pCBitFieldType) {
      return new CBitFieldType(
          pCBitFieldType.getType().accept(this), pCBitFieldType.getBitFieldSize());
    }
  }

  private enum ForceVolatileVisitor implements CTypeVisitor<CType, NoException> {
    FALSE(false),
    TRUE(true);

    private final boolean volatileValue;

    ForceVolatileVisitor(boolean pVolatileValue) {
      volatileValue = pVolatileValue;
    }

    // Make sure to always return instances of exactly the same classes!

    @Override
    public CArrayType visit(CArrayType t) {
      return new CArrayType(t.isConst(), volatileValue, t.getType(), t.getLength());
    }

    @Override
    public CCompositeType visit(CCompositeType t) {
      return new CCompositeType(
          t.isConst(), volatileValue, t.getKind(), t.getMembers(), t.getName(), t.getOrigName());
    }

    @Override
    public CElaboratedType visit(CElaboratedType t) {
      return new CElaboratedType(
          t.isConst(), volatileValue, t.getKind(), t.getName(), t.getOrigName(), t.getRealType());
    }

    @Override
    public CEnumType visit(CEnumType t) {
      return new CEnumType(
          t.isConst(),
          volatileValue,
          t.getCompatibleType(),
          t.getEnumerators(),
          t.getName(),
          t.getOrigName());
    }

    @Override
    public CFunctionType visit(CFunctionType t) {
      checkArgument(!volatileValue, "Cannot create const function type, this is undefined");
      return t;
    }

    @Override
    public CPointerType visit(CPointerType t) {
      return new CPointerType(t.isConst(), volatileValue, t.getType());
    }

    @Override
    public CProblemType visit(CProblemType t) {
      return t;
    }

    @Override
    public CSimpleType visit(CSimpleType t) {
      return new CSimpleType(
          t.isConst(),
          volatileValue,
          t.getType(),
          t.hasLongSpecifier(),
          t.hasShortSpecifier(),
          t.hasSignedSpecifier(),
          t.hasUnsignedSpecifier(),
          t.hasComplexSpecifier(),
          t.hasImaginarySpecifier(),
          t.hasLongLongSpecifier());
    }

    @Override
    public CTypedefType visit(CTypedefType t) {
      return new CTypedefType(t.isConst(), volatileValue, t.getName(), t.getRealType());
    }

    @Override
    public CType visit(CVoidType t) {
      return CVoidType.create(t.isConst(), volatileValue);
    }

    @Override
    public CType visit(CBitFieldType pCBitFieldType) {
      return new CBitFieldType(
          pCBitFieldType.getType().accept(this), pCBitFieldType.getBitFieldSize());
    }
  }

  /**
   * Return all expressions representing array lengths within the given type, even deeply nested and
   * hidden inside typedefs. (No other expressions can occur in types, so this also returns all
   * expressions referenced in this type.)
   */
  public static ImmutableSet<CExpression> getArrayLengthExpressions(CType type) {
    return type.accept(new CollectArrayLengthsVisitor());
  }

  private static class CollectArrayLengthsVisitor
      implements CTypeVisitor<ImmutableSet<CExpression>, NoException> {

    @LazyInit // Visitor might be used very often for trivial types, do not allocate set eagerly.
    private Set<CType> seen;

    @Override
    public ImmutableSet<CExpression> visit(CArrayType pArrayType) {
      if (pArrayType.getLength() == null) {
        return pArrayType.getType().accept(this);
      }

      return ImmutableSet.<CExpression>builder()
          .add(pArrayType.getLength())
          .addAll(pArrayType.getType().accept(this))
          .build();
    }

    @Override
    public ImmutableSet<CExpression> visit(CCompositeType pCompositeType) {
      // need to prevent infinite recursion for members that have pointer to this type as type
      if (seen == null) {
        seen = new HashSet<>();
        seen.add(pCompositeType);
      } else if (!seen.add(pCompositeType)) {
        return ImmutableSet.of();
      }

      ImmutableSet.Builder<CExpression> expressions = ImmutableSet.builder();
      for (CCompositeTypeMemberDeclaration member : pCompositeType.getMembers()) {
        expressions.addAll(member.getType().accept(this));
      }
      return expressions.build();
    }

    @Override
    public ImmutableSet<CExpression> visit(CElaboratedType pElaboratedType) {
      if (pElaboratedType.getRealType() == null) {
        return ImmutableSet.of();
      }
      return pElaboratedType.getRealType().accept(this);
    }

    @Override
    public ImmutableSet<CExpression> visit(CEnumType pEnumType) {
      return ImmutableSet.of();
    }

    @Override
    public ImmutableSet<CExpression> visit(CFunctionType pFunctionType) {
      ImmutableSet.Builder<CExpression> expressions = ImmutableSet.builder();
      expressions.addAll(pFunctionType.getReturnType().accept(this));
      for (CType parameterType : pFunctionType.getParameters()) {
        expressions.addAll(parameterType.accept(this));
      }
      return expressions.build();
    }

    @Override
    public ImmutableSet<CExpression> visit(CPointerType pPointerType) {
      return pPointerType.getType().accept(this);
    }

    @Override
    public ImmutableSet<CExpression> visit(CProblemType pProblemType) {
      return ImmutableSet.of();
    }

    @Override
    public ImmutableSet<CExpression> visit(CSimpleType pSimpleType) {
      return ImmutableSet.of();
    }

    @Override
    public ImmutableSet<CExpression> visit(CTypedefType pTypedefType) {
      return pTypedefType.getRealType().accept(this);
    }

    @Override
    public ImmutableSet<CExpression> visit(CVoidType pVoidType) {
      return ImmutableSet.of();
    }

    @Override
    public ImmutableSet<CExpression> visit(CBitFieldType pCBitFieldType) {
      return ImmutableSet.of();
    }
  }
}
