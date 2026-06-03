// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;

public enum SeqMemoryReachType {
  /** For {@link SeqMemoryLocation}s that are reachable before any context switch occurs. */
  DIRECT("d", "DIRECT"),
  /**
   * For all {@link SeqMemoryLocation}s that are reachable, relative to a given program location.
   */
  REACHABLE("r", "REACHABLE");

  public final String shortName;

  public final String longName;

  SeqMemoryReachType(String pShortName, String pLongName) {
    shortName = pShortName;
    longName = pLongName;
  }

  public static ImmutableList<SeqMemoryReachType> getPossibleReachTypes(MPOROptions pOptions) {
    if (pOptions.executeCommutingThreadsFirst() || pOptions.abortCommutingContextSwitches()) {
      return ImmutableList.of(SeqMemoryReachType.DIRECT, SeqMemoryReachType.REACHABLE);
    }
    if (pOptions.executeThreadsUntilConflict()) {
      return ImmutableList.of(SeqMemoryReachType.REACHABLE);
    }
    // sanity check, because all options that use any bit vectors should be handled above
    checkState(!pOptions.isAnyBitVectorReductionEnabled());
    return ImmutableList.of();
  }
}
