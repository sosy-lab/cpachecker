// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AInitializer;

/**
 * Interface for all Initializers that may occur in declarations. E.g. array initializer {@link
 * JArrayInitializer}, initializer expressions of variable expressions {@link JVariableDeclaration}.
 */
public sealed interface JInitializer extends AInitializer, JAstNode
    permits JInitializerExpression {}
