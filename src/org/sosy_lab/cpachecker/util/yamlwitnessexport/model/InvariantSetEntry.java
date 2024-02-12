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
public class InvariantSetEntry extends AbstractEntry {

  private static final String INVARIANT_SET_ENTRY_IDENTIFIER = "invariant_set";

  @JsonProperty("metadata")
  public final MetadataRecord metadata;

  @JsonProperty("content")
  public final List<InvariantRecord> content;

  public InvariantSetEntry(
      @JsonProperty("metadata") MetadataRecord pMetadata,
      @JsonProperty("content") List<InvariantRecord> pContent) {
    super(INVARIANT_SET_ENTRY_IDENTIFIER);
    metadata = pMetadata;
    content = pContent;
  }
}
