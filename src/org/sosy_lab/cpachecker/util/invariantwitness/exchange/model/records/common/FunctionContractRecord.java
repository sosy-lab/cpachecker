// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import org.sosy_lab.cpachecker.util.invariantwitness.directexport.DataTypes.ExpressionType;

@Immutable
public class FunctionContractRecord implements ExportableRecord {

  @SuppressWarnings("unused")
  private static final String FUNCTION_CONTRACT_IDENTIFIER = "function_contract";

  @JsonProperty("location")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final LocationRecord location;

  @JsonProperty("ensures")
  private final EnsuresRecord ensures;

  @JsonProperty("requires")
  private final RequiresRecord requires;

  @JsonProperty("format")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final ExpressionType format;

  public FunctionContractRecord(
      @JsonProperty("ensures") EnsuresRecord pEnsures,
      @JsonProperty("requires") RequiresRecord pRequires,
      @JsonProperty("format") ExpressionType pFormat,
      @JsonProperty("location") LocationRecord pLocation) {
    location = pLocation;
    ensures = pEnsures;
    requires = pRequires;
    format = pFormat;
  }
}
