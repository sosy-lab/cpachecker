// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

/**
 * An AbstractState that evaluates Properties (String-encoded) and returns whether they are
 * satisfied in concrete states represented by the AbstractState.
 */
public interface AbstractQueryableState extends AbstractState {

  String getCPAName();

  /**
   * Checks whether this AbstractState satisfies the property. Each CPA defines which properties can
   * be evaluated.
   *
   * <p>This method is never called from outside, but it can be used as a convenience method for
   * boolean queries, if {@link #evaluateProperty(String)} delegates to this method (which it does
   * by default).
   *
   * @param property the property to be checked
   * @return if the property is satisfied
   * @throws InvalidQueryException if the property is not given in the (CPA-specific) syntax
   */
  default boolean checkProperty(String property) throws InvalidQueryException {
    throw new InvalidQueryException(getCPAName() + " does not support querying states.");
  }

  /**
   * Evaluates some property with regard to this AbstractState and returns a value. Each CPA defines
   * which properties can be evaluated.
   *
   * @param property the property to be checked
   * @return if the property is satisfied
   * @throws InvalidQueryException if the property is not given in the (CPA-specific) syntax
   */
  default Object evaluateProperty(String property) throws InvalidQueryException {
    return checkProperty(property);
  }

  /**
   * Modifies the internal state of this AbstractState. Each CPA defines a separate language for
   * definition of modifications.
   *
   * @param modification how the state should be modified
   * @throws InvalidQueryException if the modification is not given in the (CPA-specific) syntax
   */
  default void modifyProperty(String modification) throws InvalidQueryException {
    throw new InvalidQueryException(getCPAName() + " does not support modifying states.");
  }
}
