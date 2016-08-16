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
package org.sosy_lab.cpachecker.cfa.types.c;

import com.google.common.base.Equivalence;

/**
 * Helper methods for CType instances.
 */
public final class CTypes {

  private CTypes() { }

  /**
   * Check whether the given type is a pointer to a function.
   */
  public static boolean isFunctionPointer(CType type) {
    type = type.getCanonicalType();
    if (type instanceof CPointerType) {
      CType innerType = ((CPointerType)type).getType();
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
   * Return an {@link Equivalence} based on the canonical type,
   * i.e., two types are defined as equal if their canonical types are equal.
   */
  public static Equivalence<CType> canonicalTypeEquivalence() {
    return CanonicalCTypeEquivalence.INSTANCE;
  }

  /**
   * Return a copy of a given type that has the "const" flag not set.
   * If the given type is already a non-const type, it is returned unchanged.
   *
   * This method only eliminates the outer-most const flag, if it is present,
   * i.e., it does not change a non-const pointer to a const int.
   */
  public static <T extends CType> T withoutConst(T type) {
    if (type instanceof CProblemType) {
      return type;
    }

    if (!type.isConst()) {
      return type;
    }
    @SuppressWarnings("unchecked") // Visitor always creates instances of exact same class
    T result = (T)type.accept(ForceConstVisitor.FALSE);
    return result;
  }

  /**
   * Return a copy of a given type that has the "const" flag set.
   * If the given type is already a const type, it is returned unchanged.
   *
   * This method only adds the outer-most const flag, if it is not present,
   * i.e., it does not change a const pointer to a non-const int.
   */
  public static <T extends CType> T withConst(T type) {
    if (type instanceof CProblemType) {
      return type;
    }

    if (type.isConst()) {
      return type;
    }
    @SuppressWarnings("unchecked") // Visitor always creates instances of exact same class
    T result = (T)type.accept(ForceConstVisitor.TRUE);
    return result;
  }

  /**
   * Return a copy of a given type that has the "volatile" flag not set.
   * If the given type is already a non-volatile type, it is returned unchanged.
   *
   * This method only eliminates the outer-most volatile flag, if it is present,
   * i.e., it does not change a non-volatile pointer to a volatile int.
   */
  public static <T extends CType> T withoutVolatile(T type) {
    if (type instanceof CProblemType) {
      return type;
    }

    if (!type.isVolatile()) {
      return type;
    }
    @SuppressWarnings("unchecked") // Visitor always creates instances of exact same class
    T result = (T)type.accept(ForceVolatileVisitor.FALSE);
    return result;
  }

  /**
   * Return a copy of a given type that has the "volatile" flag set.
   * If the given type is already a volatile type, it is returned unchanged.
   *
   * This method only adds the outer-most volatile flag, if it is not present,
   * i.e., it does not change a volatile pointer to a non-volatile int.
   */
  public static <T extends CType> T withVolatile(T type) {
    if (type instanceof CProblemType) {
      return type;
    }

    if (type.isVolatile()) {
      return type;
    }
    @SuppressWarnings("unchecked") // Visitor always creates instances of exact same class
    T result = (T)type.accept(ForceVolatileVisitor.TRUE);
    return result;
  }

  private static enum ForceConstVisitor implements CTypeVisitor<CType, RuntimeException> {
    FALSE(false),
    TRUE(true);

    private final boolean constValue;

    private ForceConstVisitor(boolean pConstValue) {
      constValue = pConstValue;
    }

    // Make sure to always return instances of exactly the same classes!

    @Override
    public CArrayType visit(CArrayType t) {
      return new CArrayType(constValue, t.isVolatile(), t.getType(), t.getLength());
    }

    @Override
    public CCompositeType visit(CCompositeType t) {
      return new CCompositeType(constValue, t.isVolatile(), t.getKind(), t.getMembers(), t.getName(), t.getOrigName());
    }

    @Override
    public CElaboratedType visit(CElaboratedType t) {
      return new CElaboratedType(constValue, t.isVolatile(), t.getKind(), t.getName(), t.getOrigName(), t.getRealType());
    }

    @Override
    public CEnumType visit(CEnumType t) {
      return new CEnumType(constValue, t.isVolatile(), t.getEnumerators(), t.getName(), t.getOrigName());
    }

    @Override
    public CFunctionType visit(CFunctionType t) {
      return new CFunctionType(constValue, t.isVolatile(), t.getReturnType(), t.getParameters(), t.takesVarArgs());
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
      return new CSimpleType(constValue, t.isVolatile(), t.getType(), t.isLong(), t.isShort(), t.isSigned(), t.isUnsigned(), t.isComplex(), t.isImaginary(), t.isLongLong());
    }

    @Override
    public CTypedefType visit(CTypedefType t) {
      return new CTypedefType(constValue, t.isVolatile(), t.getName(), t.getRealType());
    }

    @Override
    public CType visit(CVoidType t) {
      return CVoidType.create(constValue, t.isVolatile());
    }
  }

  private static enum ForceVolatileVisitor implements CTypeVisitor<CType, RuntimeException> {
    FALSE(false),
    TRUE(true);

    private final boolean volatileValue;

    private ForceVolatileVisitor(boolean pVolatileValue) {
      volatileValue = pVolatileValue;
    }

    // Make sure to always return instances of exactly the same classes!

    @Override
    public CArrayType visit(CArrayType t) {
      return new CArrayType(t.isConst(), volatileValue, t.getType(), t.getLength());
    }

    @Override
    public CCompositeType visit(CCompositeType t) {
      return new CCompositeType(t.isConst(), volatileValue, t.getKind(), t.getMembers(), t.getName(), t.getOrigName());
    }

    @Override
    public CElaboratedType visit(CElaboratedType t) {
      return new CElaboratedType(t.isConst(), volatileValue, t.getKind(), t.getName(), t.getOrigName(), t.getRealType());
    }

    @Override
    public CEnumType visit(CEnumType t) {
      return new CEnumType(t.isConst(), volatileValue, t.getEnumerators(), t.getName(), t.getOrigName());
    }

    @Override
    public CFunctionType visit(CFunctionType t) {
      return new CFunctionType(t.isConst(), volatileValue, t.getReturnType(), t.getParameters(), t.takesVarArgs());
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
      return new CSimpleType(t.isConst(), volatileValue, t.getType(), t.isLong(), t.isShort(), t.isSigned(), t.isUnsigned(), t.isComplex(), t.isImaginary(), t.isLongLong());
    }

    @Override
    public CTypedefType visit(CTypedefType t) {
      return new CTypedefType(t.isConst(), volatileValue, t.getName(), t.getRealType());
    }

    @Override
    public CType visit(CVoidType t) {
      return CVoidType.create(t.isConst(), volatileValue);
    }
  }
}
