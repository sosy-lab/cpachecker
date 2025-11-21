// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.smtlib.SvLibSmtFunctionDeclaration;

public sealed interface SvLibDeclaration extends SvLibSimpleDeclaration, ADeclaration
    permits SvLibSmtFunctionDeclaration,
        SvLibProcedureDeclaration,
        SvLibSortDeclaration,
        SvLibVariableDeclaration {}
