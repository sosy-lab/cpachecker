/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.Pair;

public class TestStep {

  protected List<Pair<String,BigInteger>> variables;

  public TestStep() {
    variables = new ArrayList<>();
  }

  public class InputStep extends TestStep {

    public void addInput(String variable, BigInteger value) {
      variables.add(Pair.of(variable, value));
    }

  }

  public class OutputStep extends TestStep {

    public void addOutput(String variable, BigInteger value) {
      variables.add(Pair.of(variable, value));
    }

  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");

    for (Pair<String, BigInteger> variable : variables) {
      builder.append(variable.getFirst());
      builder.append("=");
      builder.append(variable.getSecond());
      builder.append(",");
    }
    int index = builder.lastIndexOf(",");
    builder.replace(index, index + 1, "");

    builder.append("]");

    return builder.toString();
  }

}
