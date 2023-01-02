// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.refinement;

import com.google.common.base.Preconditions;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg2.SMGInformation;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Immutable state that allows forgetting values of {@link MemoryLocation}s and re-adding them via
 * copy operations.
 *
 * @param <T> arbitrary type containing all information necessary for the implementation to recreate
 *     the previous state after a delete
 */
public interface ImmutableForgetfulState<T> extends ForgetfulState<T> {

  /**
   * @deprecated do not use this method.
   */
  @Deprecated
  @Override
  T forget(MemoryLocation location);

  /**
   * @deprecated do not use this method.
   */
  @Deprecated
  @Override
  void remember(MemoryLocation location, T forgottenInformation);

  /**
   * Modify the abstract state by removing all information associated with the location.
   *
   * @param location the {@link MemoryLocation} to be removed.
   * @return a tuple of the state with the information removed and the removed information.
   */
  StateAndInfo<? extends ImmutableForgetfulState<SMGInformation>, SMGInformation> copyAndForget(
      MemoryLocation location);

  /**
   * Copies and modifies the copied abstract state to undo the effect of {@link
   * #forget(MemoryLocation)} by re-inserting the removed information.
   *
   * @param location memory location associated with removed data.
   * @param forgottenInformation data which was removed.
   * @return the modified abstract state.
   */
  ImmutableForgetfulState<T> copyAndRemember(MemoryLocation location, T forgottenInformation);

  /** Return the set of all tracked memory locations. */
  @Override
  Set<MemoryLocation> getTrackedMemoryLocations();

  @Override
  int getSize();

  public static class StateAndInfo<S, T> {
    private final T info;
    private final S state;

    public StateAndInfo(S pState, T pInfo) {
      Preconditions.checkNotNull(pInfo);
      Preconditions.checkNotNull(pState);
      state = pState;
      info = pInfo;
    }

    public T getInfo() {
      return info;
    }

    public S getState() {
      return state;
    }
  }
}
