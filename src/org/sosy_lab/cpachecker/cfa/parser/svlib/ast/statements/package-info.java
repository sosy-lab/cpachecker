// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package contains classes for representing statements in the SV-LIB AST. These statements
 * will not make it into CFAEdge's but are merely there to make the connection between the ANTLR and
 * CFA parser easier.
 *
 * <p>These classes are also useful in order to do program transformations on the SV-LIB AST before
 * creating the CFA, and for pretty-printing the SV-LIB code.
 */
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements;
