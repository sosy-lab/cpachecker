// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing;

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
}
