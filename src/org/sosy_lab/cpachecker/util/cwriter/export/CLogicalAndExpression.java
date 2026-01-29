// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * A logical AND expression in C. Example: {@code (operand1 && operand2)}
 *
 * @param operands The operands in the logical AND with at least 2 elements
 */
public record CLogicalAndExpression(ImmutableList<CExportExpression> operands)
    implements CLogicalExpression {

  public CLogicalAndExpression {
    checkArgument(
        operands.size() >= 2,
        "A CLogicalAndExpression must contain at least 2 elements (operand1 && operand2)");
  }

  public CLogicalAndExpression(CExportExpression... pOperands) {
    this(ImmutableList.copyOf(pOperands));
  }

  public CLogicalAndExpression(CExpression... pOperands) {
    this(transformedImmutableListCopy(pOperands, o -> new CExpressionWrapper(checkNotNull(o))));
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringJoiner joiner = new StringJoiner(" && ");
    for (CExportExpression operand : operands) {
      joiner.add(operand.toASTString(pAAstNodeRepresentation));
    }
    return "(" + joiner + ")";
  }
}
