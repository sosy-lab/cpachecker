// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SymbolicValueFactory {

  private static final SymbolicValueFactory SINGLETON = new SymbolicValueFactory();
  private int idCounter = 0;

  private SymbolicValueFactory() {
    // DO NOTHING
  }

  public static SymbolicValueFactory getInstance() {
    return SINGLETON;
  }

  public static void reset() {
    SINGLETON.idCounter = 0;
  }

  public SymbolicIdentifier newIdentifier(MemoryLocation pMemoryLocation) {
    return new SymbolicIdentifier(idCounter++, pMemoryLocation);
  }
}
