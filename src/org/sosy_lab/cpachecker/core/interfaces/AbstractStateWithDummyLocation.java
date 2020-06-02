// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

import java.util.Collection;

/**
 * An {@link AbstractState} that might have a dummy location.
 */
public interface AbstractStateWithDummyLocation extends AbstractState {

  /**
   * @return <code>true</code> iff this state has a location that does not exist in the original CFA
   */
  boolean isDummyLocation();

  /**
   * @return all entering {@link CFAEdge}s if {@link #isDummyLocation()} is true,
   *         an empty {@link Collection} otherwise
   */
  Collection<CFAEdge> getEnteringEdges();
}
