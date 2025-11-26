// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRelationalTerm;

/**
 * This interface represents a term in the SV-LIB abstract syntax tree (AST). A term can be a symbol
 * application, a constant, or an identifier. This corresponds to S-Expressions in SMT-LIB, which
 * SV-LIB builds upon. Note that this should be strictly sepparated from {@link SvLibExpression},
 * since the latter also includes some internal information of CPAchecker which we do not want to
 * arbitrarily combine inside of term applications.
 */
public sealed interface SvLibTerm
    extends SvLibAstNode, SvLibRelationalTerm, SvLibRightHandSide, SvLibExpression
    permits SvLibSymbolApplicationTerm, SvLibConstantTerm, SvLibIdTerm {}
