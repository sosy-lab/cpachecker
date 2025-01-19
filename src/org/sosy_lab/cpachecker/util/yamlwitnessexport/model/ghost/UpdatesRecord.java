// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ghost;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;

public class UpdatesRecord {

  @JsonProperty("variable")
  private final String variable;

  @JsonProperty("value")
  private final int value;

  @JsonProperty("format")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final YAMLWitnessExpressionType format;

  public UpdatesRecord(
      @JsonProperty("variable") String pVariable, @JsonProperty("value") int pValue) {
    variable = pVariable;
    value = pValue;
    // the format of ghost updates is currently always c_expression
    format = YAMLWitnessExpressionType.C;
  }

  public String getVariable() {
    return variable;
  }

  public int getValue() {
    return value;
  }

  public YAMLWitnessExpressionType getFormat() {
    return format;
  }
}
