// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public sealed interface ACSLLogicExpression permits ACSLBuiltin, ACSLPredicate, ACSLTerm {

  /**
   * Returns whether the logic expression may be used in a clause of the given type.
   *
   * @param clauseType the type of the clause the logic expression should be used in
   * @return whether the logic expression may be used in a clause of the given type
   */
  boolean isAllowedIn(Class<?> clauseType);
}
