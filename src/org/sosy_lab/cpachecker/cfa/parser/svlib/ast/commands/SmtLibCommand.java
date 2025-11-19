// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands;

import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibAstNode;

public sealed interface SmtLibCommand extends SvLibAstNode
    permits SmtLibDefineFunCommand,
        SmtLibDefineFunRecCommand,
        SmtLibDefineFunsRecCommand,
        SvLibAssertCommand,
        SvLibDeclareConstCommand,
        SvLibDeclareFunCommand,
        SvLibDeclareSortCommand,
        SvLibSetInfoCommand,
        SvLibSetLogicCommand,
        SvLibSetOptionCommand {}
