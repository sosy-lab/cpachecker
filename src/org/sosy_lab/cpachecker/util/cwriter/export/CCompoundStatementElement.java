// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * An interface that forms the common base for {@link CVariableDeclarationWrapper} and {@link
 * CExportStatement} so that they can be used in a {@link CCompoundStatement}.
 *
 * <p>Reference: <a
 * href="https://docs.cppreference.com/w/c/language/statements.html#Compound_statements">https://docs.cppreference.com/w/c/language/statements.html#Compound_statements</a>
 */
public sealed interface CCompoundStatementElement
    permits CExportStatement, CVariableDeclarationWrapper {

  default String toASTString() throws UnrecognizedCodeException {
    return toASTString(AAstNodeRepresentation.DEFAULT);
  }

  String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException;
}
