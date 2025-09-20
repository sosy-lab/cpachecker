// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = JsonDeserializer.None.class)
@JsonPropertyOrder({"type", "function"})
public final class FunctionPrecisionScope extends PrecisionScope {

  public static final String FUNCTION_TYPE_IDENTIFIER = "function";

  @JsonProperty("functionName")
  private final String functionName;

  public FunctionPrecisionScope(@JsonProperty("functionName") String pFunctionName) {
    super(FUNCTION_TYPE_IDENTIFIER);
    functionName = pFunctionName;
  }

  public String getFunctionName() {
    return functionName;
  }
}
