// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * This interface represents abstract states that
 * somehow store information about which CFA locations the abstract state
 * belongs to.
 * The interface is intended to provide this knowledge about the locations
 * to other components, such as other CPAs or algorithms.
 *
 * The method {@link AbstractStates#extractLocation(AbstractState)}
 * provides a convenient way to access this information.
 */
public interface AbstractStateWithLocations extends AbstractState {

  /**
   * Get the {@link CFANode}s that represents the locations of this state.
   * @return A node of the CFA.
   */
  Iterable<CFANode> getLocationNodes();

  /**
   * Get the edges that are considered "outgoing" from the current locations
   * by the CPA of this abstract state.
   * Note that this not necessarily need to be the edges that are the leaving
   * edges of this location in the CFA, it could be other sets of edges as well
   * (for example the entering edges), depending on how the current CPA
   * defines CFA iteration.
   *
   * For a given abstract state the order of outgoing edges is fixed.
   * The implementors should guarantee this property,
   * because the runtime of the analysis might depend on it.
   *
   * Callers may assume that for a state s and the set E of edges
   * return by the call <code>s.getOutgoingEdges()</code>,
   * the transfer relation of this CPA will return no successor state
   * (i.e., BOTTOM) when called with any pair (s, e) where e is not in E.
   * In other words, a state s would never have any successor state
   * that is not reachable via one of the returned edges.
   *
   * @return A (possibly empty) iterable of edges without duplicates.
   */
  Iterable<CFAEdge> getOutgoingEdges();

  /**
   * same as {@link #getOutgoingEdges()}, but swap "ingoing" and "outgoing"
   */
  Iterable<CFAEdge> getIngoingEdges();

}
