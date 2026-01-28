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

/**
 * Provides a common interface for {@link CStatement} (via {@link CWrapperStatement}) and statements
 * that are exported in actual C programs like a {@link CIfStatement}.
 *
 * <p>This an extra interface is added because using {@link CStatement} as the common base would
 * require adjustments to {@link CFA} handling (where a {@link CStatement} is linked to a single
 * edge whereas a {@link CIfStatement} represents multiple edges) and all visitors that handle
 * {@link CStatement}.
 */
public sealed interface CAstStatement
    permits CGotoStatement, CIfStatement, CLabelStatement, CWrapperStatement {

  String toASTString();

  String toASTString(AAstNodeRepresentation pAAstNodeRepresentation);
}
