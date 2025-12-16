// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;

/**
 * A class to keep track of function return value assignments (e.g. {@code CPAchecker_TMP = retval;}
 * to the respective calling context.
 */
public record FunctionReturnValueAssignment(CExpressionAssignmentStatement statement) {}
