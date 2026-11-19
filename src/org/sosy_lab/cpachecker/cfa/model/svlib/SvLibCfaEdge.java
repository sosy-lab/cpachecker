// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.svlib;

public sealed interface SvLibCfaEdge
    permits SvLibAssumeEdge,
        SvLibBlankChoiceEdge,
        SvLibDeclarationEdge,
        SvLibFunctionCallEdge,
        SvLibProcedureReturnEdge,
        SvLibProcedureSummaryEdge,
        SvLibStatementEdge {}
