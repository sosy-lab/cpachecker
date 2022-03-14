// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;

enum CompoundMathematicalIntervalManagerFactory implements CompoundIntervalManagerFactory {
  INSTANCE;

  @Override
  public CompoundIntervalManager createCompoundIntervalManager(
      MachineModel pMachineModel, Type pType) {
    return CompoundMathematicalIntervalManager.INSTANCE;
  }

  @Override
  public CompoundIntervalManager createCompoundIntervalManager(TypeInfo pBitVectorInfo) {
    return CompoundMathematicalIntervalManager.INSTANCE;
  }
}
