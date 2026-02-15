// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.declaration;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorDataType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public record SeqBitVectorDeclaration(
    BitVectorDataType type, CExpression variable, CIntegerLiteralExpression initializer) {

  public String toASTString() throws UnrecognizedCodeException {
    return toASTString(AAstNodeRepresentation.DEFAULT);
  }

  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    return type().toASTString()
        + " "
        + variable.toASTString(pAAstNodeRepresentation)
        + " = "
        + initializer.toASTString(pAAstNodeRepresentation)
        + ";";
  }
}
