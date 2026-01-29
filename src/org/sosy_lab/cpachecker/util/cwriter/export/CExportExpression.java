// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Provides a common interface for {@link CExpression} (via {@link CExpressionWrapper}) and
 * expressions that are exported in actual C programs like a {@link CLogicalAndExpression}.
 *
 * <p>This extra interface is added because using {@link CExpression} as the common base would
 * require adjustments to {@link CFA} handling and all visitors that handle {@link CExpression}.
 */
public sealed interface CExportExpression permits CExpressionWrapper, CLogicalExpression {

  /**
   * Returns a negation of this expression without any further simplification of the expression,
   * e.g., for {@code i < N} it returns {@code !(i < N)} instead of {@code i >= N}.
   */
  default CLogicalNotExpression negate() {
    return new CLogicalNotExpression(this);
  }

  default String toASTString() throws UnrecognizedCodeException {
    return toASTString(AAstNodeRepresentation.DEFAULT);
  }

  String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException;
}
