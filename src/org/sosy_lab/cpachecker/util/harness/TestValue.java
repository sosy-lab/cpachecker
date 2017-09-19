/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.harness;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;

abstract class TestValue {

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
    StringBuilder sb = new StringBuilder();
    try {
      new CodeAppender(sb).appendAssignment("RESULT", this);
    } catch (IOException e) {
      throw new AssertionError("StringBuilder is not supposed to throw an IOException.");
    }
    return sb.toString();
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
      return value.equals(other.value)
          && auxiliaryStatements.equals(other.auxiliaryStatements);
    }
    return false;
  }

}
