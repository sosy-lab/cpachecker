// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import com.google.common.collect.ImmutableList;
import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

public sealed class CFunctionCallStatement extends AFunctionCallStatement
    implements CStatement, CFunctionCall
    // FIXME: this is broken and should be removed
    permits CThreadOperationStatement {
  @Serial private static final long serialVersionUID = 1103049666572120249L;

  public CFunctionCallStatement(FileLocation pFileLocation, CFunctionCallExpression pFunctionCall) {
    super(pFileLocation, pFunctionCall);
  }

  @Override
  public CFunctionCallExpression getFunctionCallExpression() {
    return (CFunctionCallExpression) super.getFunctionCallExpression();
  }

  @Override
  public <R, X extends Exception> R accept(CStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof CFunctionCallStatement && super.equals(obj);
  }

  /** Creates a {@link CFunctionCallStatement} invoking the given void function declaration. */
  public static CFunctionCallStatement createNoArgsVoidFunctionCall(
      FileLocation loc, CFunctionDeclaration declaration) {
    return new CFunctionCallStatement(
        loc,
        new CFunctionCallExpression(
            loc,
            CVoidType.VOID,
            new CIdExpression(loc, declaration),
            ImmutableList.of(),
            declaration));
  }
}
