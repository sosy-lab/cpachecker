// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.andersen.util;


/**
 * This class models a SimpleConstraint in pointer analysis. This constraint has the
 * structure <code>a \subseteq b</code>.
 */
public class SimpleConstraint extends Constraint {

  /**
   * Creates a new {@link SimpleConstraint} with the given variables for the sub- and superset.
   *
   * @param subVar Indentifies the subset variable in this Constraint.
   * @param superVar Indentifies the superset variable in this Constraint.
   */
  public SimpleConstraint(String subVar, String superVar) {
    super(subVar, superVar);
  }
}
