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
  public static boolean equals(CType t1, CType t2){
    return simplifyType(t1).equals(simplifyType(t2));
  }
}
