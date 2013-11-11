/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.andersen;

import java.util.Map;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.andersen.util.BaseConstraint;
import org.sosy_lab.cpachecker.cpa.andersen.util.ComplexConstraint;
import org.sosy_lab.cpachecker.cpa.andersen.util.ConstraintSystem;
import org.sosy_lab.cpachecker.cpa.andersen.util.SimpleConstraint;

public class AndersenState implements AbstractState, Cloneable {

  // ------- local constraint system -------
  private final ConstraintSystem localConstraintSystem;

  public AndersenState() {
    this(null);
  }

  public AndersenState(ConstraintSystem pLocalConstraintSystem) {
    this.localConstraintSystem = pLocalConstraintSystem == null ? new ConstraintSystem() : pLocalConstraintSystem.clone();
  }

  /**
   * Add a (new) {@link BaseConstraint} to this element.
   *
   * @param constr {@link BaseConstraint} that should be added.
   */
  AndersenState addConstraint(BaseConstraint constr) {
    AndersenState result = new AndersenState(localConstraintSystem);
    result.localConstraintSystem.addConstraint(constr);
    return result;
  }

  /**
   * Add a (new) {@link SimpleConstraint} to this element.
   *
   * @param constr {@link SimpleConstraint} that should be added.
   */
  AndersenState addConstraint(SimpleConstraint constr) {
    AndersenState result = new AndersenState(localConstraintSystem);
    result.localConstraintSystem.addConstraint(constr);
    return result;
  }

  /**
   * Add a (new) {@link ComplexConstraint} to this element.
   *
   * @param constr {@link ComplexConstraint} that should be added.
   */
  AndersenState addConstraint(ComplexConstraint constr) {
    AndersenState result = new AndersenState(localConstraintSystem);
    result.localConstraintSystem.addConstraint(constr);
    return result;
  }

  /**
   * Computes and returns the points-to sets for the local constraint system.
   *
   * @return points-to sets for the local constraint system.
   */
  public Map<String, String[]> getLocalPointsToSets() {
    return localConstraintSystem.getPointsToSets();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO instanceof AndersenState) {
      AndersenState other = (AndersenState) pO;
      return localConstraintSystem.equals(other.localConstraintSystem);
    }
    return false;
  }

  @Override
  public AndersenState clone() {
    return new AndersenState(localConstraintSystem);
  }

  @Override
  public int hashCode() {
    return this.localConstraintSystem == null ? 0 : this.localConstraintSystem.hashCode();
  }

  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder();
    sb.append('[').append('\n');

    for (BaseConstraint bc : this.localConstraintSystem.getBaseConstraints()) {
      sb.append('{').append(bc.getSubVar()).append("} \u2286 ");
      sb.append(bc.getSuperVar()).append('\n');
    }

    for (SimpleConstraint bc : this.localConstraintSystem.getSimpleConstraints()) {
      sb.append(bc.getSubVar()).append(" \u2286 ");
      sb.append(bc.getSuperVar()).append('\n');
    }

    for (ComplexConstraint bc : this.localConstraintSystem.getComplexConstraints()) {
      sb.append(bc.getSubVar()).append(" \u2286 ");
      sb.append(bc.getSuperVar()).append('\n');
    }

    int size = this.localConstraintSystem.getBaseConstraints().size()
        + this.localConstraintSystem.getSimpleConstraints().size()
        + this.localConstraintSystem.getComplexConstraints().size();

    sb.append("] size->  ").append(size);

    // points-to sets
    sb.append('\n');
    sb.append('[').append('\n');

    Map<String, String[]> ptSet = getLocalPointsToSets();
    for (String key : ptSet.keySet()) {

      sb.append(key).append(" -> {");
      String[] vals = ptSet.get(key);

      for (String val : vals) {
        sb.append(val).append(',');
      }

      if (vals.length > 0) {
        sb.setLength(sb.length() - 1);
      }

      sb.append('}').append('\n');
    }

    sb.append(']').append('\n');

    return sb.toString();
  }
}
