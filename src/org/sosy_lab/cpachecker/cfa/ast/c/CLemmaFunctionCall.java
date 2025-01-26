// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class CLemmaFunctionCall extends CFunctionCallExpression implements CExpression {

  @Serial private static final long serialVersionUID = -3614875951469862750L;

  public CLemmaFunctionCall(
      FileLocation pFileLocation,
      CType pType,
      CExpression pFunctionName,
      List<CExpression> pParameters,
      CFunctionDeclaration pDeclaration) {
    super(pFileLocation, pType, pFunctionName, pParameters, pDeclaration);
  }

  @Override
  public <R, X extends Exception> R accept(CExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}
