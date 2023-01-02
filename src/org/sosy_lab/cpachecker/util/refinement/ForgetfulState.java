// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.refinement;

import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * State that allows forgetting values of {@link MemoryLocation}s and re-adding them.
 *
 * @param <T> arbitrary type containing all information necessary for the implementation to recreate
 *     the previous state after a delete
 */
public interface ForgetfulState<T> extends AbstractState {

  /**
   * Modify the abstract state by removing all information associated with {@code location}.
   *
   * @return removed information.
   */
  T forget(MemoryLocation location);

  /**
   * Modify the abstract state to undo the effect of {@link #forget(MemoryLocation)} by re-inserting
   * the removed information.
   *
   * @param location memory location associated with removed data.
   * @param forgottenInformation data which was removed.
   */
  void remember(MemoryLocation location, T forgottenInformation);

  /** Return the set of all tracked memory locations. */
  Set<MemoryLocation> getTrackedMemoryLocations();

  int getSize();
}
