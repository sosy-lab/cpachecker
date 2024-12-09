// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;

public class LemmaEntry extends AbstractInformationRecord {

  @JsonProperty("value")
  protected final String value;

  public LemmaEntry(
      @JsonProperty("value") String pString,
      @JsonProperty("format") YAMLWitnessExpressionType pFormat) {
    super("lemma", pFormat);
    value = pString;
  }

  public String getValue() {
    return value;
  }
}
