// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.mutex;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;

/**
 * The identity of a mutex, represented by the declaration of the variable that denotes it. Several
 * syntactic forms denote the same mutex ({@code m}, {@code &m}, {@code (pthread_mutex_t *)&m}), so
 * the declaration is extracted from the expression rather than storing the expression itself. Using
 * the declaration also distinguishes variables with the same name in different scopes.
 */
public record MutexHandle(CVariableDeclaration declaration) {
  public MutexHandle {
    checkNotNull(declaration);
  }

  @Override
  public String toString() {
    return declaration.getQualifiedName();
  }
}
