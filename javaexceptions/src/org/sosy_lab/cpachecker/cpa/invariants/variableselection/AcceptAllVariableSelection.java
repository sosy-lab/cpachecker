// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.variableselection;

import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class AcceptAllVariableSelection<ConstantType> implements VariableSelection<ConstantType> {

  @Override
  public boolean contains(MemoryLocation pMemoryLocation) {
    return true;
  }

  @Override
  public VariableSelection<ConstantType> acceptAssumption(
      BooleanFormula<ConstantType> pAssumption) {
    return this;
  }

  @Override
  public VariableSelection<ConstantType> acceptAssignment(
      MemoryLocation pMemoryLocation, NumeralFormula<ConstantType> pAssumption) {
    return this;
  }

  @Override
  public VariableSelection<ConstantType> join(VariableSelection<ConstantType> pOther) {
    return this;
  }

  @Override
  public <T> T acceptVisitor(VariableSelectionVisitor<ConstantType, T> pVisitor) {
    return pVisitor.visit(this);
  }
}
