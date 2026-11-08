// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public interface AcslAstNodeVisitor<R, X extends Exception>
    extends AcslPredicateVisitor<R, X>,
        AcslSimpleDeclarationVisitor<R, X>,
        AcslTermVisitor<R, X>,
        AcslMemoryLocationSetVisitor<R, X>,
        AcslLogicDefinitionVisitor<R, X> {

  R visit(AcslInitializerExpression pInitializerExpression) throws X;

  R visit(AcslBuiltinLabel pAcslBuiltinLabel) throws X;

  R visit(AcslProgramLabel pAcslProgramLabel) throws X;
}
