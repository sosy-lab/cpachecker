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
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintVisitor;

/**
 * A single constraint.
 *
 * <p>A constraint describes a relation between two {@link ConstraintOperand}s that has to be true or false,
 * depending on the constraint.</p>
 *
 * <p>Possible examples would be relations like <code>'5 < 10'</code> or <code>'n == 10'</code></p>
 */
public interface Constraint {

  Type getType();

  <T> T accept(ConstraintVisitor<T> pVisitor);

  boolean isTrivial();
}
