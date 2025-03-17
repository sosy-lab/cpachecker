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

/** This class represents an ACSL function call as a CExpression. */
public final class ACSLFunctionCall extends CFunctionCallExpression implements CExpression {

  @Serial private static final long serialVersionUID = -3614875951469862750L;

  /** Class constructor. */
  public ACSLFunctionCall(
      FileLocation pFileLocation,
      CType pType,
      CExpression pFunctionName,
      List<CExpression> pParameters,
      CFunctionDeclaration pDeclaration) {
    super(pFileLocation, pType, pFunctionName, pParameters, pDeclaration);
  }

  /** Class constructor that creates an ACSLFunctionCall from a CFunctionCallExpression. */
  public ACSLFunctionCall(CFunctionCallExpression pFunctionCall) {
    this(
        pFunctionCall.getFileLocation(),
        pFunctionCall.getDeclaration().getType().getReturnType(),
        pFunctionCall.getFunctionNameExpression(),
        pFunctionCall.getParameterExpressions(),
        pFunctionCall.getDeclaration());
  }

  @Override
  public <R, X extends Exception> R accept(CExpressionVisitor<R, X> v) throws X {
    throw new UnsupportedOperationException(
        "A CExpressionVisitor should never be called on an ACSLFunctionCall.");
  }
}
