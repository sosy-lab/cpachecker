// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

@JsonPropertyOrder({"entry_type", "metadata", "content"})
public class SetEntry extends AbstractEntry {

  private static final String SET_ENTRY_IDENTIFIER = "entry_set";

  @JsonProperty("metadata")
  private final MetadataRecord metadata;

  @JsonProperty("content")
  private final List<SetElementRecord> content;

  public SetEntry(
      @JsonProperty("metadata") MetadataRecord pMetadata,
      @JsonProperty("content") List<SetElementRecord> pContent) {
    super(SET_ENTRY_IDENTIFIER);
    metadata = pMetadata;
    content = pContent;
  }
}
