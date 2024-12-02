// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class LemmaSetEntry extends AbstractEntry {

  private static final String LEMMA_SET_ENTRY_IDENTIFIER = "lemma_set";

  @JsonProperty("content")
  public final List<LemmaEntry> content;

  public LemmaSetEntry(
      @JsonProperty("content") List<LemmaEntry> pContent) {
    super(LEMMA_SET_ENTRY_IDENTIFIER);
    content = pContent;
  }
}
