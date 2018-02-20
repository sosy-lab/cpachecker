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

public interface CTypeVisitor<R, X extends Exception> {
  R visit(CArrayType pArrayType) throws X;
  R visit(CCompositeType pCompositeType) throws X;
  R visit(CElaboratedType pElaboratedType) throws X;
  R visit(CEnumType pEnumType) throws X;
  R visit(CFunctionType pFunctionType) throws X;
  R visit(CPointerType pPointerType) throws X;
  R visit(CProblemType pProblemType) throws X;
  R visit(CSimpleType pSimpleType) throws X;
  R visit(CTypedefType pTypedefType) throws X;
  R visit(CVoidType pVoidType) throws X;
  R visit(CBitFieldType pCBitFieldType) throws X;
}
