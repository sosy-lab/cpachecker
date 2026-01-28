// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * Provides a common interface for {@link CStatement} (via {@link CStatementWrapper}) and all other
 * statements that do not appear in the {@link CFAEdge}s of a {@link CFA}, but may e.g. be exported
 * in actual C programs like a {@link CIfStatement}.
 */
public sealed interface CAstStatement permits CStatementWrapper, CIfStatement {

  String toASTString();

  String toASTString(AAstNodeRepresentation pAAstNodeRepresentation);
}
