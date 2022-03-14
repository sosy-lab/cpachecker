// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;

/**
 * A single constraint.
 *
 * <p>A constraint is a boolean relation or operation over one or more operands.
 *
 * <p>Possible examples would be relations like <code>{@code '5 < 10'}</code>, <code>'n == 10'
 * </code> or <code>'not true'</code>
 */
public interface Constraint extends SymbolicValue {

  /** Returns the expression type of the constraint */
  Type getType();

  /**
   * Returns whether this constraint is trivial. A constraint is trivial if it does not contain any
   * symbolic identifiers.
   *
   * <p>This method does not check whether a occurring symbolic identifier has a definite
   * assignment, but always returns <code>false</code>, if one exists. To consider definite
   * assignments, use {@link
   * org.sosy_lab.cpachecker.cpa.constraints.constraint.ConstraintTrivialityChecker}.
   *
   * @return <code>true</code> if the given constraint does not contain any symbolic identifiers,
   *     <code>false</code> otherwise</code>
   */
  boolean isTrivial();
}
