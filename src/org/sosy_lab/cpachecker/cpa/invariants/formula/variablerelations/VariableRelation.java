/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.invariants.formula.variablerelations;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Variable;


public interface VariableRelation<ConstantType> extends InvariantsFormula<ConstantType> {

  Variable<ConstantType> getOperand1();

  Variable<ConstantType> getOperand2();

  <T> T accept(VariableRelationVisitor<ConstantType, T> pVariableRelationVisitor);

  InvariantsFormula<ConstantType> getInvariantsFormula();

  boolean isCompatibleWith(VariableRelation<ConstantType> pOther);

  Object getCompatibilityKey();

  /**
   * Unites this relation with the given relation. If <code>null</code> is returned,
   * the union is top, meaning there is no information about the relation between the
   * two variables.
   *
   * @param pOther the relation to unite this relation with.
   * @return the union of the two relations.
   */
  @Nullable
  VariableRelation<ConstantType> union(VariableRelation<ConstantType> pOther);


  /**
   * Intersects this relation with the given relation. If <code>null</code> is returned,
   * the intersection is bottom, meaning that the relations do not intersect.

   * @param pOther the relation to intersect this relation with.
   * @return the intersection of the two relations.
   */
  @Nullable
  VariableRelation<ConstantType> intersect(VariableRelation<ConstantType> pOther);

}
