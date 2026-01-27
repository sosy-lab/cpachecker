// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import com.google.common.collect.ImmutableList;
import java.io.Serial;

public abstract class AIfStatement extends AbstractStatement {

  @Serial private static final long serialVersionUID = 4154628454325446837L;

  private final AExpression condition;

  private final ImmutableList<? extends AStatement> ifStatements;

  private final ImmutableList<? extends AStatement> elseStatements;

  protected AIfStatement(
      FileLocation pFileLocation,
      AExpression pCondition,
      ImmutableList<? extends AStatement> pIfStatements,
      ImmutableList<? extends AStatement> pElseStatements) {

    super(pFileLocation);
    condition = pCondition;
    ifStatements = pIfStatements;
    elseStatements = pElseStatements;
  }

  public AExpression getCondition() {
    return condition;
  }

  public ImmutableList<? extends AStatement> getIfStatements() {
    return ifStatements;
  }

  public ImmutableList<? extends AStatement> getElseStatements() {
    return elseStatements;
  }

  @Override
  public <R, X extends Exception> R accept(AStatementVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }
}
