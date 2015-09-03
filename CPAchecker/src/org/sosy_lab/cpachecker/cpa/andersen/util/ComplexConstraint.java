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
package org.sosy_lab.cpachecker.cpa.andersen.util;


/**
 * This class models a ComplexConstraint in pointer analysis. This constraint has the
 * structure <code>*a \subseteq b</code>, or <code>a \subseteq *b</code>.
 */
public class ComplexConstraint extends Constraint {

  private final boolean isSubDerefed;

  /**
   * Creates a new {@link BaseConstraint} with the given variables for the sub- and superset.
   *
   * @param subVar Indentifies the only element of the subset in this Constraint.
   * @param superVar Indentifies the superset variable in this Constraint.
   * @param isSubDerefed <code>true</code> if the subset is dereferenced, <code>false</code> if the
   *      superset is dereferenced.
   */
  public ComplexConstraint(String subVar, String superVar, boolean isSubDerefed) {
    super(subVar, superVar);

    this.isSubDerefed = isSubDerefed;
  }

  /**
   * Returns if the subset in this complex constraints is dereferenced. If not, the superset is
   * dereferenced.
   *
   * @return <code>true</code> if the subset is dereferenced, <code>false</code> if the
   *      superset is dereferenced.
   */
  public boolean isSubDerefed() {
    return isSubDerefed;
  }

  @Override
  public boolean equals(Object other) {

    if (super.equals(other) && this.isSubDerefed == ((ComplexConstraint) other).isSubDerefed) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {

    return super.hashCode() * 31 + (isSubDerefed ? 1 : 0);
  }
}
