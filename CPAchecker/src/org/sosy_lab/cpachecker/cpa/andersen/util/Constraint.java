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
 * This class models an abstract Constraint in pointer analysis. All constraints have a similar
 * structure <code>a \subseteq b</code>.
 */
public abstract class Constraint {

  /** Indentifies the subset variable in this Constraint. */
  private final String subVar;

  /** Indentifies the superset variable in this Constraint. */
  private final String superVar;

  /**
   * Creates a new {@link Constraint} with the given variables for the sub- and superset.
   *
   * @param subVar Indentifies the subset variable in this Constraint.
   * @param superVar Indentifies the superset variable in this Constraint.
   */
  public Constraint(String subVar, String superVar) {

    this.subVar = subVar;
    this.superVar = superVar;
  }

  /**
   * Returns the String identifying the subset of this constraint.
   *
   * @return the String identifying the subset of this constraint.
   */
  public String getSubVar() {
    return this.subVar;
  }

  /**
   * Returns the String identifying the superset of this constraint.
   *
   * @return the String identifying the superset of this constraint.
   */
  public String getSuperVar() {
    return this.superVar;
  }

  @Override
  public boolean equals(Object other) {

    if (this == other) {
      return true;
    }

    if (other == null || !this.getClass().equals(other.getClass())) {
      return false;
    }

    Constraint o = (Constraint) other;

    return this.subVar.equals(o.subVar) && this.superVar.equals(o.superVar);
  }

  @Override
  public int hashCode() {

    int hash = 18;
    hash = 31 * hash + (subVar == null ? 0 : subVar.hashCode());
    hash = 31 * hash + (superVar == null ? 0 : superVar.hashCode());

    return hash;
  }
}
