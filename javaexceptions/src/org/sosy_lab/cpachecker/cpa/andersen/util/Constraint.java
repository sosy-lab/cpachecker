// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.andersen.util;

import java.util.Objects;

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
  protected Constraint(String subVar, String superVar) {

    this.subVar = subVar;
    this.superVar = superVar;
  }

  /**
   * Returns the String identifying the subset of this constraint.
   *
   * @return the String identifying the subset of this constraint.
   */
  public String getSubVar() {
    return subVar;
  }

  /**
   * Returns the String identifying the superset of this constraint.
   *
   * @return the String identifying the superset of this constraint.
   */
  public String getSuperVar() {
    return superVar;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Constraint)) {
      return false;
    }
    Constraint o = (Constraint) other;
    return subVar.equals(o.subVar) && superVar.equals(o.superVar);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subVar, superVar);
  }
}
