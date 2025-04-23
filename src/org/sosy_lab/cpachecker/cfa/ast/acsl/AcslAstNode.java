// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryExpression.AcslBinaryExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm.AcslBinaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermComparisonExpression.AcslBinaryTermComparisonExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryExpression.AcslUnaryExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm.AcslUnaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNodeVisitor;

public sealed interface AcslAstNode extends AAstNode
    permits AcslBinaryExpressionOperator,
        AcslBinaryTermOperator,
        AcslBinaryTermComparisonExpressionOperator,
        AcslExpression,
        AcslInitializer,
        AcslLabel,
        AcslSimpleDeclaration,
        AcslTerm,
        AcslUnaryExpressionOperator,
        AcslUnaryTermOperator {

  <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X;

  @Deprecated // Call accept() directly
  @SuppressWarnings("unchecked") // should not be necessary, but javac complains otherwise
  @Override
  default <
          R,
          R1 extends R,
          R2 extends R,
          R3 extends R,
          X1 extends Exception,
          X2 extends Exception,
          X3 extends Exception,
          V extends CAstNodeVisitor<R1, X1> & JAstNodeVisitor<R2, X2> & AcslAstNodeVisitor<R3, X3>>
      R accept_(V pV) throws X3 {
    return accept(pV);
  }
}
