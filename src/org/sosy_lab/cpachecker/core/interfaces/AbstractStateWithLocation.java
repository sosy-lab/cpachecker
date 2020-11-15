// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * This interface represents abstract states that
 * somehow store information about which CFA location the abstract state
 * belongs to.
 * The interface is intended to provide this knowledge about the location
 * to other components, such as other CPAs or algorithms.
 *
 * The method {@link AbstractStates#extractLocation(AbstractState)}
 * provides a convenient way to access this information.
 */
public interface AbstractStateWithLocation extends AbstractStateWithLocations {

  /**
   * Get the {@link CFANode} that represents the location of this state.
   * @return A node of the CFA.
   */
  CFANode getLocationNode();
}
