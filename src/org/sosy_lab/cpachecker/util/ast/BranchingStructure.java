// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

public abstract class BranchingStructure extends StatementStructure {

  /**
   * @return the ASTElement representing the part in the round parenthesis of the branching
   *     statement. This is identical to the controlling expression except for a "for" loop.
   */
  public abstract ASTElement getClause();

  /**
   * @return the ASTElement representing the controlling expression for a branching structure (cf.
   *     6.8.4-5 in the C99 standard). This is essentially the part in round parentheses, except for
   *     a for loop, where it is just the part that one would call the "condition" and that controls
   *     whether the body is executed or not.
   */
  public abstract ASTElement getControllingExpression();
}
