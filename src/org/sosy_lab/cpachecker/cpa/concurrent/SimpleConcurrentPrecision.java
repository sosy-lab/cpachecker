// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class SimpleConcurrentPrecision extends ConcurrentPrecision {

  SimpleConcurrentPrecision(Precision pWrappedPrecision) {
    super(pWrappedPrecision);
  }

  @Override
  protected ConcurrentPrecision withWrappedPrecision(Precision newWrappedPrecision) {
    return new SimpleConcurrentPrecision(newWrappedPrecision);
  }

  @Override
  public boolean canIgnoreVariable(MemoryLocation memoryLocation) {
    return false;
  }
}
