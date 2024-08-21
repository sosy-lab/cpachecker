// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces.pcc;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * Checks if an abstract state or a set of abstract states adheres to the property which should be
 * checked by the specific implementation of PropertyChecker.
 */
public interface PropertyChecker {

  /**
   * Checks if a single abstract state satisfies the represented property. If the property cannot be
   * checked for each abstract state individually, an UnsupportedOperationException should be
   * thrown.
   *
   * <p>An UnsupportedOperationException should be thrown for every abstract element of a domain D
   * or for none of the abstract elements of domain D.
   *
   * @param elemToCheck - abstract state for which property satisfaction will be checked
   * @return true if property is successfully checked on abstract state elemToCheck, false otherwise
   */
  boolean satisfiesProperty(AbstractState elemToCheck) throws UnsupportedOperationException;

  /**
   * Checks if a set of abstract states satisfies the represented property.
   *
   * @param certificate - set of abstract states for which property satisfaction will be checked
   * @return true if property holds for set of abstract states certificate, false otherwise
   */
  boolean satisfiesProperty(Collection<AbstractState> certificate);
}
