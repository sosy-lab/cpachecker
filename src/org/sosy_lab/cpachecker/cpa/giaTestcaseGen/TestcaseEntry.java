// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.giaTestcaseGen;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TestcaseEntry implements Serializable {

  private static final long serialVersionUID = -7715698130885655052L;
  private final String value;
  private  final @Nullable String variable;
  private final @Nullable String type;

  public TestcaseEntry(String pValue, Optional<String> pVariable, Optional<String> pType) {
    value = pValue;
    variable = pVariable.orElse(null);
    type = pType.orElse(null);
  }

  public TestcaseEntry(String pValue, String pVariable, String pType) {
    value = pValue;
    variable = pVariable;
    type = pType;
  }

  @Override
  public String toString() {
    return "TestcaseEntry{"
        + "value='"
        + value
        + '\''
        + ", variable="
        + variable
        + ", type="
        + type
        + '}';
  }

  public TestcaseEntry copy() {
    return new TestcaseEntry(this.value, this.variable, this.type);
  }

  /**
   * Generate a string of the form <input variable="y" type="unsigned char">254</input> if variable
   * and type are set. Otherwise, they are not set
   *
   * @return the parsed string
   */
  public String toXMLTestcaseLine() {
    StringBuilder sb = new StringBuilder();
    sb.append("<input ");
    if (this.variable != null) {
      sb.append(String.format("variable=\"%s\" ", variable));
    }
    if (this.type != null) {
      sb.append(String.format("type=\"%s\"", type));
    }
    sb.append(String.format(">%s</input>", value));
    return sb.toString();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof TestcaseEntry)) {
      return false;
    }
    TestcaseEntry that = (TestcaseEntry) pO;
    return Objects.equals(value, that.value)
        && Objects.equals(variable, that.variable)
        && Objects.equals(type, that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, variable, type);
  }

  public String getValue() {
    return value;
  }

  public Optional<String> getVariable() {return Optional.ofNullable(variable);}

  public Optional<String> getType() {
    return Optional.ofNullable(type);
  }
}
