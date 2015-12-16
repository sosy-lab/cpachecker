/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;


/**
 * This interface represents abstract states that somehow store information
 * about which shadow CFA locations the abstract state belongs to.
 *
 * The interface is intended to provide this knowledge about the shadow locations
 * to other components, such as other CPAs or algorithms.
 */
public interface AbstractStateWithShadowLocations extends AbstractState {

  /**
   * Get the {@link CFANode}s that represents the shadow locations of this state.
   * @return A node of the CFA.
   */
  Iterable<CFANode> getLocationNodes();

  /**
   * Get the edges that are considered "outgoing" from the current shadow locations
   * by the CPA of this abstract state.
   *
   * @return A (possibly empty) iterable of shadow edges without duplicates.
   */
  Iterable<CFAEdge> getOutgoingEdges();

}