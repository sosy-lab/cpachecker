// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LemmaSetEntry extends AbstractEntry {
  @JsonProperty("value")
  public final String value;
  private static final String LEMMA_SET_ENTRY_IDENTIFIER = "lemma_set";

  public LemmaSetEntry(
      @JsonProperty("value") String lvalue) {
    super(LEMMA_SET_ENTRY_IDENTIFIER);
    value = lvalue;
  }

}
