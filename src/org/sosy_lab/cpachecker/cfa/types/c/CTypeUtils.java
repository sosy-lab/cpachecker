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
  private static CTypeVisitor<CType, Exception> simplifyType = new CTypeVisitor<CType, Exception>() {
    @Override
    public CType visit(CArrayType pArrayType) throws Exception {
      return pArrayType;
    }

    @Override
    public CType visit(CCompositeType pCompositeType) throws Exception {
      return pCompositeType;
    }

    @Override
    public CType visit(CElaboratedType pElaboratedType) throws Exception {
      return pElaboratedType;
    }

    @Override
    public CType visit(CEnumType pEnumType) throws Exception {
      return pEnumType;
    }

    @Override
    public CType visit(CFunctionPointerType pFunctionPointerType) throws Exception {
      return pFunctionPointerType;
    }

    @Override
    public CType visit(CFunctionType pFunctionType) throws Exception {
      return pFunctionType;
    }

    @Override
    public CType visit(CPointerType pPointerType) throws Exception {
      return pPointerType;
    }

    @Override
    public CType visit(CProblemType pProblemType) throws Exception {
      return pProblemType;
    }

    @Override
    public CType visit(CSimpleType pSimpleType) throws Exception {
      return pSimpleType;
    }

    @Override
    public CType visit(CTypedefType pTypedefType) throws Exception {
      return pTypedefType.getRealType().accept(this);
    }

    @Override
    public CType visit(CNamedType pCNamedType) throws Exception {
      throw new IllegalArgumentException();
    }

    @Override
    public CType visit(CDummyType pCDummyType) throws Exception {
      throw new IllegalArgumentException();
    }

    @Override
    public CType visit(CComplexType pCComplexType) throws Exception {
      throw new IllegalArgumentException();
    }};

  public static CType simplifyType(CType t1) {
    try {
      return t1.accept(simplifyType);
    } catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Should not happen", e);
    }
  }
  public static boolean equals(CType t1, CType t2){
    return simplifyType(t1).equals(simplifyType(t2));
  }
}
