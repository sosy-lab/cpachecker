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

public interface VariableSelection<ConstantType> {

  boolean contains(MemoryLocation pMemoryLocation);

  VariableSelection<ConstantType> acceptAssumption(BooleanFormula<ConstantType> pAssumption);

  VariableSelection<ConstantType> acceptAssignment(
      MemoryLocation pMemoryLocation, NumeralFormula<ConstantType> pAssumption);

  VariableSelection<ConstantType> join(VariableSelection<ConstantType> pOther);

  <T> T acceptVisitor(VariableSelectionVisitor<ConstantType, T> pVisitor);
}
