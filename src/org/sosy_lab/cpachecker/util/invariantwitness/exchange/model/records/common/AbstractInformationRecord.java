// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class AbstractInformationRecord {
  @JsonAlias({"value", "string"})
  protected final String value;

  @JsonProperty("type")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  protected final String type;

  @JsonProperty("format")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  protected final String format;

  public AbstractInformationRecord(
      @JsonProperty("value") String string,
      @JsonProperty("type") String type,
      @JsonProperty("format") String format) {
    this.value = string;
    this.type = type;
    this.format = format;
  }

  public String getValue() {
    return value;
  }

  public String getType() {
    return type;
  }

  public String getFormat() {
    return format;
  }
}
