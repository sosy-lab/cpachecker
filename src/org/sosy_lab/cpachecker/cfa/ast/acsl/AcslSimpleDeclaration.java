// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;

public sealed interface AcslSimpleDeclaration extends ASimpleDeclaration, AcslAstNode
    permits AcslDeclaration, AcslParameterDeclaration {

  @Override
  AcslType getType();

  <R, X extends Exception> R accept(AcslSimpleDeclarationVisitor<R, X> v) throws X;
}
