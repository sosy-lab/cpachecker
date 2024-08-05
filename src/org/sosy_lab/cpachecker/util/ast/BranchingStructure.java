// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import java.util.Optional;

public abstract class BranchingStructure extends StatementStructure {

  /**
   * Returns the (optional) ASTElement representing the part in the round parenthesis of the
   * branching statement. This is identical to the controlling expression except for a "for" loop,
   * in which case it can also be empty since a for loop allows all 3 parts to be empty.
   */
  public abstract Optional<ASTElement> getClause();

  /**
   * Returns the (optional) ASTElement representing the controlling expression for a branching
   * structure (cf. 6.8.4-5 in the C99 standard). This is essentially the part in round parentheses,
   * except for a for loop, where it is just the part that one would call the "condition" and that
   * controls whether the body is executed or not. For the for loop, this part need not be present,
   * in which case it shall be treated as having a nonzero value (for-ever loop)
   */
  public abstract Optional<ASTElement> getControllingExpression();
}
