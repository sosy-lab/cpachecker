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
 * expressions that are exported in actual C programs like a {@link CExpressionTree}.
 *
 * <p>This extra interface is added because using {@link CExpression} as the common base would
 * require adjustments to {@link CFA} handling and all visitors that handle {@link CExpression}.
 */
public sealed interface CExportExpression
    permits CExpressionTree, CNegatedExpression, CExpressionWrapper {

  default String toASTString() throws UnrecognizedCodeException {
    return toASTString(AAstNodeRepresentation.DEFAULT);
  }

  String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException;
}
