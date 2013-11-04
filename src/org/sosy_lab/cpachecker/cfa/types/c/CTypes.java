/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

/**
 * Helper methods for CType instances.
 */
public final class CTypes {

  /**
   * Return a copy of a given type that has the "const" flag not set.
   * If the given type is already a non-const type, it is returned unchanged.
   * 
   * This method only eliminates the outer-most const flag, if it is present,
   * i.e., it does not change a non-const pointer to a const int.
   */
  public static <T extends CType> T withoutConst(T type) {
    if (!type.isConst()) {
      return type;
    }
    @SuppressWarnings("unchecked") // Visitor always creates instances of exact same class
    T result = (T)type.accept(WithoutConstVisitor.INSTANCE);
    return result;
  }

  private static enum WithoutConstVisitor implements CTypeVisitor<CType, RuntimeException> {
    INSTANCE;

    // Make sure to always return instances of exactly the same classes!

    @Override
    public CArrayType visit(CArrayType t) {
      return new CArrayType(false, t.isVolatile(), t.getType(), t.getLength());
    }

    @Override
    public CCompositeType visit(CCompositeType t) {
      return new CCompositeType(false, t.isVolatile(), t.getKind(), t.getMembers(), t.getName());
    }

    @Override
    public CElaboratedType visit(CElaboratedType t) {
      return new CElaboratedType(false, t.isVolatile(), t.getKind(), t.getName(), t.getRealType());
    }

    @Override
    public CEnumType visit(CEnumType t) {
      return new CEnumType(false, t.isVolatile(), t.getEnumerators(), t.getName());
    }

    @Override
    public CFunctionType visit(CFunctionType t) {
      return new CFunctionType(false, t.isVolatile(), t.getReturnType(), t.getParameters(), t.takesVarArgs());
    }

    @Override
    public CPointerType visit(CPointerType t) {
      return new CPointerType(false, t.isVolatile(), t.getType());
    }

    @Override
    public CProblemType visit(CProblemType t) {
      return t;
    }

    @Override
    public CSimpleType visit(CSimpleType t) {
      return new CSimpleType(false, t.isVolatile(), t.getType(), t.isLong(), t.isShort(), t.isSigned(), t.isUnsigned(), t.isComplex(), t.isImaginary(), t.isLongLong());
    }

    @Override
    public CTypedefType visit(CTypedefType t) {
      return new CTypedefType(false, t.isVolatile(), t.getName(), t.getRealType());
    }
  }
}
