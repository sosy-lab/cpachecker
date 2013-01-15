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
package org.sosy_lab.cpachecker.cfa.types.c;

import java.util.Objects;



public class CTypeUtils {
  private static CTypeVisitor<CType, RuntimeException> simplifyType = new CTypeVisitor<CType, RuntimeException>() {
    @Override
    public CType visit(CArrayType pArrayType) {
      return pArrayType;
    }

    @Override
    public CType visit(CCompositeType pCompositeType) {
      return pCompositeType;
    }

    @Override
    public CType visit(CElaboratedType pElaboratedType) {
      return pElaboratedType;
    }

    @Override
    public CType visit(CEnumType pEnumType) {
      return pEnumType;
    }

    @Override
    public CType visit(CFunctionPointerType pFunctionPointerType) {
      return pFunctionPointerType;
    }

    @Override
    public CType visit(CFunctionType pFunctionType) {
      return pFunctionType;
    }

    @Override
    public CType visit(CPointerType pPointerType) {
      return pPointerType;
    }

    @Override
    public CType visit(CProblemType pProblemType) {
      return pProblemType;
    }

    @Override
    public CType visit(CSimpleType pSimpleType) {
      return pSimpleType;
    }

    @Override
    public CType visit(CTypedefType pTypedefType) {
      return pTypedefType.getRealType().accept(this);
    }

    @Override
    public CType visit(CNamedType pCNamedType) {
      throw new IllegalArgumentException();
    }

    @Override
    public CType visit(CDummyType pCDummyType) {
      throw new IllegalArgumentException();
    }

    @Override
    public CType visit(CComplexType pCComplexType) {
      throw new IllegalArgumentException();
    }

    @Override
    public CType visit(CDereferenceType pCDereferenceType) {
      return pCDereferenceType;
    }};

  public static CType simplifyType(CType t1) {
    return t1.accept(simplifyType);
  }

  public static class BaseCTypeEqualsVisitor implements CTypeVisitor<Boolean, Exception> {
    final Object obj;
    public BaseCTypeEqualsVisitor(Object other) {
      this.obj = other;
    }

    public BaseCTypeEqualsVisitor copyWith(Object other) {
      return new BaseCTypeEqualsVisitor(other);
    }

    @Override
    public Boolean visit(CArrayType pThis) throws Exception {
      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CArrayType other = (CArrayType) obj;
      return equalsArrayType(pThis, other);
    }

    private boolean equalsArrayType(CArrayType pThis, CArrayType other) throws Exception {
      return
          Objects.equals(pThis.getLength(), other.getLength()) &&
          pThis.getType().accept(this.copyWith(other.getType()));
    }

    @Override
    public Boolean visit(CCompositeType pThis) throws Exception {
      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CCompositeType other = (CCompositeType) obj;
      return equalsCompositeType(pThis, other);
    }

    private Boolean equalsCompositeType(CCompositeType pThis, CCompositeType other) {
      return
          Objects.equals(pThis.getName(), other.getName()) &&
          // TODO: use own implementation for CCompositeMember.equals
          Objects.equals(pThis.getMembers(), other.getMembers()) &&
          pThis.getKind() == other.getKind();
    }

    @Override
    public Boolean visit(CElaboratedType pThis) throws Exception {

      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CElaboratedType other = (CElaboratedType) obj;
      return equalsElaboratedType(pThis, other);
    }

    private Boolean equalsElaboratedType(CElaboratedType pThis, CElaboratedType other) {
      return
          Objects.equals(pThis.getName(), other.getName()) &&
          pThis.getKind() == other.getKind();
    }

    @Override
    public Boolean visit(CEnumType pThis) throws Exception {

      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CEnumType other = (CEnumType) obj;
      return equalsEnumType(pThis, other);
    }

    private Boolean equalsEnumType(CEnumType pThis, CEnumType other) {
      return
          Objects.equals(pThis.getEnumerators(), other.getEnumerators()) &&
          Objects.equals(pThis.getName(), other.getName());
    }

    @Override
    public Boolean visit(CFunctionPointerType pThis) throws Exception {

      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CFunctionPointerType other = (CFunctionPointerType) obj;
      return equalsFunctionPointerType(pThis, other);
    }

    private Boolean equalsFunctionPointerType(CFunctionPointerType pThis, CFunctionPointerType other) {
      return
          Objects.equals(pThis.getName(), other.getName()) &&
          // TODO: use current equals-impl for all parameters
          Objects.equals(pThis.getParameters(), other.getParameters()) &&
          CTypeUtils.equals(pThis.getReturnType(), other.getReturnType()) &&
          pThis.takesVarArgs() == other.takesVarArgs();
    }

    @Override
    public Boolean visit(CFunctionType pThis) throws Exception {

      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CFunctionType other = (CFunctionType) obj;
      return equalsFunctionType(pThis, other);
    }

    private Boolean equalsFunctionType(CFunctionType pThis, CFunctionType other) throws Exception {
      return
          Objects.equals(pThis.getName(), other.getName()) &&
          // TODO: use current equals-impl for all parameters
          Objects.equals(pThis.getParameters(), other.getParameters()) &&
          pThis.getReturnType().accept(this.copyWith(other.getReturnType())) &&
          pThis.takesVarArgs() == other.takesVarArgs();
    }

    @Override
    public Boolean visit(CPointerType pThis) throws Exception {

      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CPointerType other = (CPointerType) obj;
      return equalsPointerType(pThis, other);
    }

    private Boolean equalsPointerType(CPointerType pThis, CPointerType other) throws Exception {
      return
          pThis.getType().accept(this.copyWith(other.getType()));
    }

    @Override
    public Boolean visit(CProblemType pThis) throws Exception {

      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CProblemType other = (CProblemType) obj;
      return equalsProblemType(pThis, other);
    }

    private Boolean equalsProblemType(CProblemType pThis, CProblemType other) {
      return
          Objects.equals(pThis.toString(), other.toString());
    }

    @Override
    public Boolean visit(CSimpleType pThis) throws Exception {

      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CSimpleType other = (CSimpleType) obj;
      return equalsSimpleType(pThis, other);
    }

    private Boolean equalsSimpleType(CSimpleType pThis, CSimpleType other) {
      return
          pThis.isComplex() == other.isComplex() &&
          pThis.isImaginary() == other.isImaginary() &&
          pThis.isLong() == other.isLong() &&
          pThis.isLongLong() == other.isLongLong() &&
          pThis.isShort() == other.isShort() &&
          pThis.isSigned() == other.isSigned() &&
          pThis.isUnsigned() == other.isUnsigned() &&

          (pThis.getType() == other.getType() ||
            (pThis.getType() == CBasicType.INT && other.getType() == CBasicType.UNSPECIFIED) ||
            (pThis.getType() == CBasicType.UNSPECIFIED && other.getType() == CBasicType.INT));
    }

    @Override
    public Boolean visit(CTypedefType pThis) throws Exception {
      // Should not happen
      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CTypedefType other = (CTypedefType) obj;
      return equalsTypedefType(pThis, other);
    }

    private Boolean equalsTypedefType(CTypedefType pThis, CTypedefType other) throws Exception {
      return
          Objects.equals(pThis.getName(), other.getName()) &&
          pThis.getRealType().accept(this.copyWith(other.getRealType()));
    }

    @Override
    public Boolean visit(CNamedType pThis) throws Exception {

      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CNamedType other = (CNamedType) obj;
      return equalsNamedType(pThis, other);
    }

    private Boolean equalsNamedType(CNamedType pThis, CNamedType other) {
      return
          Objects.equals(pThis.getName(), other.getName());
    }

    @Override
    public Boolean visit(CDummyType pThis) throws Exception {

      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CDummyType other = (CDummyType) obj;
      return equalsDummyType(pThis, other);
    }

    private Boolean equalsDummyType(CDummyType pThis, CDummyType other) {
      return true;
    }

    @Override
    public Boolean visit(CComplexType pThis) throws Exception {

      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CComplexType other = (CComplexType) obj;
      return equalsComplexType(pThis, other);
    }

    private Boolean equalsComplexType(CComplexType pThis, CComplexType other) {
      return
          Objects.equals(pThis.getName(), other.getName());
    }

    @Override
    public Boolean visit(CDereferenceType pThis) throws Exception {

      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CDereferenceType other = (CDereferenceType) obj;
      return equalsDereferenceType(pThis, other);
    }

    private Boolean equalsDereferenceType(CDereferenceType pThis, CDereferenceType other) throws Exception {
      return
          pThis.getType().accept(this.copyWith(other.getType()));
    }

  }
  public static boolean equals(CType t1, Object other) {
    if (t1 == null || other == null) {
      return t1 == other;
    }

    if (!(other instanceof CType)) return false;
    return equals(t1, (CType)other);
  }

  public static boolean equals(CType t1, CType t2) {
    if (t1 == null || t2 == null) {
      return t1 == t2;
    }

    CType simplifyType1 = simplifyType(t1);
    CType simplifyType2 = simplifyType(t2);
    BaseCTypeEqualsVisitor visitor = new BaseCTypeEqualsVisitor(simplifyType2);
    try {
      return simplifyType1.accept(visitor);
    } catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Should not happen", e);
    }
  }
}
