// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.type;

import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;

public interface ValueVisitor<T> {

  T visit(EnumConstantValue pValue);

  T visit(SymbolicValue pValue);

  T visit(UnknownValue pValue);

  T visit(ArrayValue pValue);

  T visit(BooleanValue pValue);

  T visit(FunctionValue pValue);

  T visit(NumericValue pValue);

  T visit(NullValue pValue);
}
