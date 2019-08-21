/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;

/**
 * A single constraint.
 *
 * <p>A constraint is a boolean relation or operation over one or more operands.</p>
 *
 * <p>Possible examples would be relations like <code>'5 < 10'</code>, <code>'n == 10'</code>
 * or <code>'not true'</code></p>
 */
public interface Constraint extends SymbolicValue {

  /** Returns the expression type of the constraint */
  Type getType();

  /**
   * Returns whether this constraint is trivial.
   * A constraint is trivial if it does not contain any symbolic identifiers.
   *
   * <p>This method does not check whether a occurring symbolic identifier has a definite
   * assignment, but always returns <code>false</code>, if one exists. To consider
   * definite assignments, use
   * {@link org.sosy_lab.cpachecker.cpa.constraints.constraint.ConstraintTrivialityChecker}.</p>
   *
   * @return <code>true</code> if the given constraint does not contain any symbolic identifiers,
   *    <code>false</code> otherwise</code>
   */
  boolean isTrivial();
}
