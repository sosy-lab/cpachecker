// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package contains all the classes for generating CPAchecker internal AST classes from ANTLR
 * parsed strings.
 *
 * <p>Each class returns a specific part of the AST, e.g., expressions, statements, declarations,
 * etc. This is done to guarantee the return types of the ANTLR parse tree visitors and to make the
 * code more modular.
 */
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
package org.sosy_lab.cpachecker.cfa.parser.svlib.antlr;
