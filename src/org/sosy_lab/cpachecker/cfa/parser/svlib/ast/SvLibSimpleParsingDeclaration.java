// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public sealed interface SvLibSimpleParsingDeclaration extends SvLibParsingAstNode
    permits SvLibParsingDeclaration, SvLibParsingParameterDeclaration {

  SvLibType getType();

  @Nullable String getProcedureName();

  SvLibSimpleDeclaration toSimpleDeclaration();
}
