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
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;

public abstract class TestValue {

  private final ImmutableList<AAstNode> auxiliaryStatements;

  private final AAstNode value;

  protected TestValue(ImmutableList<AAstNode> pAuxiliaryStatements, AAstNode pValue) {
    auxiliaryStatements = pAuxiliaryStatements;
    value = pValue;
  }

  public List<AAstNode> getAuxiliaryStatements() {
    return auxiliaryStatements;
  }

  public AAstNode getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "{ RESULT = " + value.toASTString() + "; }";
  }

  @Override
  public int hashCode() {
    return Objects.hash(auxiliaryStatements, value);
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof TestValue) {
      TestValue other = (TestValue) pObj;
      return value.equals(other.value) && auxiliaryStatements.equals(other.auxiliaryStatements);
    }
    return false;
  }
}
