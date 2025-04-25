// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicateExpression.AcslBinaryPredicateExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm.AcslBinaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermExpression.AcslBinaryTermExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryExpression.AcslUnaryExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm.AcslUnaryTermOperator;

public interface AcslAstNodeVisitor<R, X extends Exception>
    extends AcslExpressionVisitor<R, X>,
        AcslSimpleDeclarationVisitor<R, X>,
        AcslInitializerVisitor<R, X>,
        AcslTermVisitor<R, X>,
        AcslMemoryLocationSetVisitor<R, X>,
        AcslLogicDefinitionVisitor<R, X> {

  R visit(AcslBinaryPredicateExpressionOperator pDecl) throws X;

  R visit(AcslBinaryTermOperator pDecl) throws X;

  R visit(AcslBinaryTermExpressionOperator pDecl) throws X;

  R visit(AcslUnaryTermOperator pDecl) throws X;

  R visit(AcslUnaryExpressionOperator pDecl) throws X;

  R visit(AcslBuiltinLabel pAcslBuiltinLabel) throws X;

  R visit(AcslProgramLabel pAcslProgramLabel) throws X;
}
