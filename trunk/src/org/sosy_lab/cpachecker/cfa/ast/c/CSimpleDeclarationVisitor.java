// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;

public interface CSimpleDeclarationVisitor<R, X extends Exception> {

  R visit(CFunctionDeclaration pDecl) throws X;

  R visit(CComplexTypeDeclaration pDecl) throws X;

  R visit(CTypeDefDeclaration pDecl) throws X;

  R visit(CVariableDeclaration pDecl) throws X;

  R visit(CParameterDeclaration pDecl) throws X;

  R visit(CEnumerator pDecl) throws X;
}
