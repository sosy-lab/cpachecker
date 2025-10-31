// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;

public sealed interface K3Declaration extends K3SimpleDeclaration, ADeclaration
    permits K3FunctionDeclaration,
        K3ProcedureDeclaration,
        K3SortDeclaration,
        K3VariableDeclaration {}
