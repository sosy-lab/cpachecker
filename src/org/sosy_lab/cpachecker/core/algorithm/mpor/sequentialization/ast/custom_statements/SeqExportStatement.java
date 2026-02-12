// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;

public sealed interface SeqExportStatement
    permits SeqThreadStatement, SeqThreadStatementBlock, SeqThreadStatementClause {

  ImmutableList<CExportStatement> toCExportStatements();
}
