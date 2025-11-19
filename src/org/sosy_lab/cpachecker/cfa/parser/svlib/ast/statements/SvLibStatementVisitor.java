// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements;

import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibCfaEdgeStatementVisitor;

public interface SvLibStatementVisitor<R, X extends Exception>
    extends SvLibCfaEdgeStatementVisitor<R, X>, SvLibControlFlowStatementVisitor<R, X> {}
