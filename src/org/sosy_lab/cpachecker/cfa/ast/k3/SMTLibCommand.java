// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

public sealed interface SMTLibCommand
    permits K3AssertCommand,
        K3DeclareConstCommand,
        K3DeclareFunCommand,
        K3DeclareSortCommand,
        K3SetLogicCommand,
        K3SetOptionCommand {}
