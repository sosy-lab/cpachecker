// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.cpa.value.type;

import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;

public class SerializeValueVisitor implements ValueVisitor<String> {

  @Override
  public String visit(EnumConstantValue pValue) {
    return "";
  }

  @Override
  public String visit(SymbolicValue pValue) {
    return "";
  }

  @Override
  public String visit(UnknownValue pValue) {
    return "";
  }

  @Override
  public String visit(ArrayValue pValue) {
    return "";
  }

  @Override
  public String visit(BooleanValue pValue) {
    int asInt;
    if (pValue.isTrue()) {
      asInt = 1;
    } else {
      asInt = 0;
    }
    return "BooleanValue(" + asInt + ")";
  }

  @Override
  public String visit(FunctionValue pValue) {
    return "FunctionValue(" + pValue.getName() + ")";
  }

  @Override
  public String visit(NumericValue pValue) {
    return "NumericValue(" + pValue.getNumber().longValue() + ")";
  }

  @Override
  public String visit(NullValue pValue) {
    return "";
  }
}
