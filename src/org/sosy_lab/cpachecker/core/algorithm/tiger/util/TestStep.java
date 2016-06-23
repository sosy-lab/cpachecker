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

public class TestStep {

  public enum AssignmentType {
    INPUT,
    OUTPUT
  }

  public static class VariableAssignment {

    private String variableName;
    private BigInteger value;
    private AssignmentType type;


    public VariableAssignment(String pVariableName, BigInteger pValue, AssignmentType pType) {
      setVariableName(pVariableName);
      setValue(pValue);
      setType(pType);
    }

    public String getVariableName() {
      return variableName;
    }

    private void setVariableName(String pVariableName) {
      variableName = pVariableName;
    }

    public BigInteger getValue() {
      return value;
    }

    private void setValue(BigInteger pValue) {
      value = pValue;
    }

    public AssignmentType getType() {
      return type;
    }

    private void setType(AssignmentType pType) {
      type = pType;
    }

    @Override
    public String toString() {
      String str = "";
      if (getType() == AssignmentType.INPUT) {
        str += "-> ";
      } else if (getType() == AssignmentType.OUTPUT) {
        str += "<- ";
      }
      str += getVariableName() + " = " + getValue();

      return str;
    }

    @Override
    public int hashCode() {
      return 64248 + 34 * variableName.hashCode() + 13 * value.hashCode() + 29 * type.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof VariableAssignment) {
        VariableAssignment other = (VariableAssignment) o;
        return variableName.equals(other.getVariableName()) && value.equals(other.getValue())
            && type == other.getType();
      }

      return false;
    }

  }

  protected List<VariableAssignment> assignments;

  public TestStep() {
    assignments = new ArrayList<>();
  }

  public void addAssignment(VariableAssignment assignment) {
    assignments.add(assignment);
  }

  public List<VariableAssignment> getAssignments() {
    return assignments;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");

    for (VariableAssignment assignment : assignments) {
      if (assignment.getType() == AssignmentType.INPUT) {
        builder.append("->");
      } else if (assignment.getType() == AssignmentType.OUTPUT) {
        builder.append("<-");
      }
      builder.append(assignment.getVariableName());
      builder.append(" = ");
      builder.append(assignment.getValue());
      builder.append(", ");
    }
    int index = builder.lastIndexOf(",");
    builder.replace(index, index + 1, "");

    builder.append("]");

    return builder.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof TestStep) {
      TestStep other = (TestStep) o;
      return assignments.equals(other.getAssignments());
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 64248 + 34 * assignments.hashCode();
  }

}
