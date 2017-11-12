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
package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;

public class TestCaseData {

  private int id;

  private Map<String, String> inputs;

  private Map<String, String> outputs;

  private List<String> coveredGoals;

  private List<String> coveredLabels;

  private int errorPathLength;

  public TestCaseData() {
    id = -1;

    inputs = Maps.newLinkedHashMap();

    outputs = Maps.newLinkedHashMap();

    coveredGoals = Lists.newLinkedList();

    coveredLabels = Lists.newLinkedList();

    errorPathLength = -1;
  }

  public int getId() {
    return id;
  }

  public void setId(int pId) {
    id = pId;
  }

  public Map<String, String> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, String> pInputs) {
    inputs = pInputs;
  }

  public Map<String, String> getOutputs() {
    return outputs;
  }

  public void setOutputs(Map<String, String> pOutputs) {
    outputs = pOutputs;
  }

  public List<String> getCoveredGoals() {
    return coveredGoals;
  }

  public void setCoveredGoals(List<String> pCoveredGoals) {
    coveredGoals = pCoveredGoals;
  }

  public List<String> getCoveredLabels() {
    return coveredLabels;
  }

  public void setCoveredLabels(List<String> pCoveredLabels) {
    coveredLabels = pCoveredLabels;
  }

  public int getErrorPathLength() {
    return errorPathLength;
  }

  public void setErrorPathLength(int pErrorPathLength) {
    errorPathLength = pErrorPathLength;
  }

  @Override
  public String toString() {
    StringBuffer str = new StringBuffer();

    str.append("TestCase ").append(id).append(":\n\n");

    str.append("\tinputs and outputs {\n");

    for (String s : inputs.keySet()) {
      str.append("\t\t-> ").append(s).append(" = ").append(inputs.get(s)).append("\n");
    }
    for (String s : outputs.keySet()) {
      str.append("\t\t<- ").append(s).append(" = ").append(outputs.get(s)).append("\n");
    }

    str.append("\t}");
    str.append("\n\n");

    str.append("\tCovered goals {\n");

    for (String g : coveredGoals) {
      str.append("\t\t").append(g).append("\n");
    }

    str.append("\t}\n\n");

    str.append("\tCovered labels {\n");
    str.append("\t\t");

    for (String label : coveredLabels) {
      str.append(label).append(", ");
    }

    str.delete(str.length() - 2, str.length());
    str.append("\n");
    str.append("\t}\n");
    str.append("\n");

    if (errorPathLength != -1) {
      str.append("\tErrorpath Length: ").append(errorPathLength).append("\n");
    }

    str.append("\n\n");

    return str.toString();
  }

}
