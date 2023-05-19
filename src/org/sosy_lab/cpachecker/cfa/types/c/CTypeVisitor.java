// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
