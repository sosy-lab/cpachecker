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
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;

public class InitializerTestValue extends TestValue {

  private final AInitializer value;

  private InitializerTestValue(ImmutableList<AAstNode> pAuxiliaryStatements, AInitializer pValue) {
    super(pAuxiliaryStatements, pValue);
    value = pValue;
  }

  @Override
  public AInitializer getValue() {
    return value;
  }

  public static InitializerTestValue of(AInitializer pValue) {
    return of(ImmutableList.of(), pValue);
  }

  public static InitializerTestValue of(List<AAstNode> pAuxiliaryStatments, AInitializer pValue) {
    return new InitializerTestValue(ImmutableList.copyOf(pAuxiliaryStatments), pValue);
  }
}
