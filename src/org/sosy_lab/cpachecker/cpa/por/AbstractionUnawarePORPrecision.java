// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class AbstractionUnawarePORPrecision implements PORPrecision {

  public static final AbstractionUnawarePORPrecision INSTANCE =
      new AbstractionUnawarePORPrecision();

  private AbstractionUnawarePORPrecision() {
  }

  @Override
  public boolean hasInformationAboutVariable(MemoryLocation memoryLocation) {
    return true;
  }
}
