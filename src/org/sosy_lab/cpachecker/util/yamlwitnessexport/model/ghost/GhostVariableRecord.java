// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ghost;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GhostVariableRecord {

  @JsonProperty("name")
  private final String name;

  @JsonProperty("type")
  private final String type; // variable type

  @JsonProperty("scope")
  private final String scope = "global"; // the scope of a ghost variable is always global

  @JsonProperty("initial")
  private final InitialRecord initial;

  public GhostVariableRecord(
      @JsonProperty("name") String pName,
      @JsonProperty("type") String pType,
      @JsonProperty("initial") InitialRecord pInitial) {
    name = pName;
    type = pType;
    initial = pInitial;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getScope() {
    return scope;
  }

  public InitialRecord getInitial() {
    return initial;
  }
}
