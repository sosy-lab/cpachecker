// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.testcase;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;

public class ExpressionTestValue extends TestValue {

  private final AExpression value;

  private ExpressionTestValue(ImmutableList<AuxiliaryCode> pAuxiliaryCodes, AExpression pValue) {
    super(pAuxiliaryCodes, pValue);
    value = pValue;
  }

  @Override
  public AExpression getValue() {
    return value;
  }

  public static ExpressionTestValue of(AExpression pValue) {
    return of(ImmutableList.of(), pValue);
  }

  public static ExpressionTestValue of(List<AuxiliaryCode> pAuxiliaryCode, AExpression pValue) {
    return new ExpressionTestValue(ImmutableList.copyOf(pAuxiliaryCode), pValue);
  }

  public static ExpressionTestValue of(AuxiliaryCode pAuxiliaryCode, AExpression pValue) {
    return new ExpressionTestValue(ImmutableList.of(pAuxiliaryCode), pValue);
  }
}
