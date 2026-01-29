// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Contains common base classes to export C statements into actual C programs.
 *
 * <p>The existing {@code CStatement} is wrapped into {@code CStatementWrapper} which implements the
 * common interface {@code CExportStatement}. Any statement that should be exported into C code but
 * not be considered in the {@code CFA} and Visitor logic can also implement {@code
 * CExportStatement}.
 *
 * <p>A simple example includes an {@code if} statement that would break a lot of code that works on
 * the {@code CFA}, but can be implemented using {@code CExportStatement}.
 */
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
package org.sosy_lab.cpachecker.util.cwriter.export.statement;
