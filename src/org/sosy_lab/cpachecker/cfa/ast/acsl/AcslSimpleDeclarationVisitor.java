// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public interface AcslSimpleDeclarationVisitor<R, X extends Exception> {

  R visit(AcslCVariableDeclaration pDecl) throws X;

  R visit(AcslVariableDeclaration pDecl) throws X;

  R visit(AcslFunctionDeclaration pDecl) throws X;

  R visit(AcslPredicateDeclaration pDecl) throws X;

  R visit(AcslTypeVariableDeclaration pDecl) throws X;

  R visit(AcslParameterDeclaration pAcslParameterDeclaration) throws X;
}
