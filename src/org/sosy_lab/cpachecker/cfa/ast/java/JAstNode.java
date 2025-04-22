// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;

/**
 * Interface for all AST Nodes of the Java AST. All classes representing Java AST Nodes have to
 * implement this interface.
 */
public sealed interface JAstNode extends AAstNode
    permits JInitializer, JReturnStatement, JRightHandSide, JSimpleDeclaration, JStatement {

  <R, X extends Exception> R accept(JAstNodeVisitor<R, X> v) throws X;

  @Deprecated // Call accept() directly
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
      R accept_(V v) throws X2 {
    return accept(v);
  }
}
