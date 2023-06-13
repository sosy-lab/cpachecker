// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.regions;

/**
 * An AbstractFormula is a representation of a data region in the abstract space. For instance, in
 * the case of predicate abstraction, it can be a BDD over the predicates
 */
public interface Region {

  /**
   * checks whether f represents "true"
   *
   * @return true if f represents logical truth, false otherwise
   */
  boolean isTrue();

  /**
   * checks whether f represents "false"
   *
   * @return true if f represents logical falsity, false otherwise
   */
  boolean isFalse();
}
