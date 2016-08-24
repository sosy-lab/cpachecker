/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
