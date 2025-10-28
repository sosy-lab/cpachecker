// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.k3;

public sealed interface K3CfaEdge
    permits K3AssumeEdge,
        K3ProcedureCallEdge,
        K3ProcedureReturnEdge,
        K3ProcedureSummaryEdge,
        K3StatementEdge {}
