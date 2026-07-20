// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class AbstractionAwarePORPrecision extends PORPrecision {

  private final PrecisionVariableManager variableManager;

  AbstractionAwarePORPrecision(
      PrecisionVariableManager pVariableManager, Precision wrappedPrecision) {
    super(wrappedPrecision);
    variableManager = pVariableManager;
    variableManager.setNewPrecision(wrappedPrecision);
  }

  @Override
  protected PORPrecision withWrappedPrecision(Precision newWrappedPrecision) {
    return new AbstractionAwarePORPrecision(variableManager, newWrappedPrecision);
  }

  @Override
  public boolean canIgnoreVariable(MemoryLocation memoryLocation) {
    return !variableManager.contains(memoryLocation);
  }
}
