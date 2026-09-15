// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;

public abstract class AbstractInformationRecord {

  @JsonProperty("type")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  protected final String type;

  @JsonProperty("format")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  protected final YAMLWitnessExpressionType format;

  public AbstractInformationRecord(
      @JsonProperty("type") String pType,
      @JsonProperty("format") YAMLWitnessExpressionType pFormat) {
    type = pType;
    format = pFormat;
  }

  public String getType() {
    return type;
  }

  public YAMLWitnessExpressionType getFormat() {
    return format;
  }
}
