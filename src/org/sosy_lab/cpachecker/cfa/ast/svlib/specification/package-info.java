// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package contains classes for representing and handling specifications written in the SV-LIB
 * language. This includes traces and annotations such as `:assert` and `:invariant`, which are part
 * of SV-LIB programs.
 *
 * <p>In particular, the classes are used to export strengthened specifications, i.e., witnesses
 * that include additional information to aid in verification. Therefore, it is not possible to get
 * rid of these AST classes entirely.
 */
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
package org.sosy_lab.cpachecker.cfa.ast.svlib.specification;
