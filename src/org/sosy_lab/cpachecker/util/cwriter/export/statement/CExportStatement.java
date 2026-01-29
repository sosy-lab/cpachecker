// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export.statement;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Provides a common interface for {@link CStatement} (via {@link CStatementWrapper}) and statements
 * that are exported in actual C programs like a {@link CIfStatement}.
 *
 * <p>This extra interface is added because using {@link CStatement} as the common base would
 * require adjustments to {@link CFA} handling (where a {@link CStatement} is linked to a single
 * edge whereas a {@link CIfStatement} represents multiple edges) and all visitors that handle
 * {@link CStatement}.
 *
 * <p>Note that this interface is not {@code sealed} because its implementing classes may be
 * distributed in other packages.
 */
public interface CExportStatement {

  default String toASTString() throws UnrecognizedCodeException {
    return toASTString(AAstNodeRepresentation.DEFAULT);
  }

  String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException;
}
