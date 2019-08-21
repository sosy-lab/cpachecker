/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.util.refinement;

import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * State that allows forgetting values of {@link MemoryLocation}s and re-adding them.
 *
 * @param <T> arbitrary type containing all information necessary for the implementation
 *    to recreate the previous state after a delete
 */
public interface ForgetfulState<T> extends AbstractState {

  /**
   * Modify the abstract state by removing all information associated with {@code location}.
   *
   * @return removed information.
   */
  T forget(MemoryLocation location);

  /**
   * Modify the abstract state to undo the effect of {@link #forget(MemoryLocation)} by
   * re-inserting the removed information.
   *
   * @param location memory location associated with removed data.
   * @param forgottenInformation data which was removed.
   */
  void remember(MemoryLocation location, T forgottenInformation);

  /**
   * Return the set of all tracked memory locations.
   */
  Set<MemoryLocation> getTrackedMemoryLocations();

  int getSize();
}
