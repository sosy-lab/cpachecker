// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

import org.sosy_lab.cpachecker.cpa.invariants.TypeInfo;

abstract class AbstractFormula<ConstantType> implements NumeralFormula<ConstantType> {

  private final TypeInfo info;

  protected AbstractFormula(TypeInfo pInfo) {
    this.info = pInfo;
  }

  @Override
  public TypeInfo getTypeInfo() {
    return this.info;
  }
}
