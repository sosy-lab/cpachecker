// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Contains common base classes to export C expressions and statements into actual C programs.
 *
 * <p>The existing {@code CExpression} is wrapped into {@code CExpressionWrapper} which implements
 * the common interface {@code CExportExpression}. Any expression that should be exported into C
 * code but not be considered in the {@code CFA} and Visitor logic can also implement {@code
 * CExportExpression}.
 *
 * <p>A simple example includes logical {@code &&} and {@code ||} expressions that would break a lot
 * of code that works on the {@code CFA}, but can be implemented using {@code CExportExpression}.
 *
 * <p>{@code CExportStatement} handles the export of statements similarly.
 */
package org.sosy_lab.cpachecker.cfa.ast.c.export;
