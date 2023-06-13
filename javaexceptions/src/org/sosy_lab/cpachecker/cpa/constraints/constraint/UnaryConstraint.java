// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;

/** A constraint with only one operand. Example: not(..); */
public interface UnaryConstraint extends Constraint {

  SymbolicExpression getOperand();
}
