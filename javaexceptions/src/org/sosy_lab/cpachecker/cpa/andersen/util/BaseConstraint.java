// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.andersen.util;

/**
 * This class models a BaseConstraint in pointer analysis. This constraint has the structure <code>
 * {a} \subseteq b</code>.
 */
public class BaseConstraint extends Constraint {

  /**
   * Creates a new {@link BaseConstraint} with the given variables for the sub- and superset.
   *
   * @param subVar Indentifies the only element of the subset in this Constraint.
   * @param superVar Indentifies the superset variable in this Constraint.
   */
  public BaseConstraint(String subVar, String superVar) {
    super(subVar, superVar);
  }
}
