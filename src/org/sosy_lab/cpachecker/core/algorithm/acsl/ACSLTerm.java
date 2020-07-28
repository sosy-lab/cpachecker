// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.acsl;

import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public interface ACSLTerm {

  CExpression accept(ACSLTermToCExpressionVisitor visitor) throws UnrecognizedCodeException;

  /**
   * Returns a version of the term where each identifier is augmented with the ACSL builtin
   * predicate "\old" to signal to use the value from the pre-state when evaluating.
   */
  ACSLTerm useOldValues();

  /**
   * Returns whether the term may be used in a clause of the given type.
   *
   * @param clauseType the type of the clause the term should be used in
   * @return true if the term may be used in a clause of the given type, false otherwise
   */
  boolean isAllowedIn(Class<?> clauseType);

  Set<ACSLBuiltin> getUsedBuiltins();
}
