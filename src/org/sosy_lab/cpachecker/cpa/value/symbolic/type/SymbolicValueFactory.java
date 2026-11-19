// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import java.util.concurrent.atomic.AtomicLong;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SymbolicValueFactory {

  private static final SymbolicValueFactory SINGLETON = new SymbolicValueFactory();
  // atomic, because analyses that run in parallel share it
  private final AtomicLong idCounter = new AtomicLong();

  private SymbolicValueFactory() {
    // DO NOTHING
  }

  public static SymbolicValueFactory getInstance() {
    return SINGLETON;
  }

  public static void reset() {
    SINGLETON.idCounter.set(0);
  }

  public SymbolicIdentifier newIdentifier(MemoryLocation pMemoryLocation) {
    return new SymbolicIdentifier(idCounter.getAndIncrement(), pMemoryLocation);
  }
}
