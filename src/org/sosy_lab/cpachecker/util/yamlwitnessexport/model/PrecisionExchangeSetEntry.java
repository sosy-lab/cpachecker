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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.List;

@JsonPropertyOrder({"entry_type", "metadata", "declarations", "content"})
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "entry_type", visible = true)
public class PrecisionExchangeSetEntry extends AbstractEntry {

  private static final String PRECISION_EXCHANGE_ENTRY_IDENTIFIER = "precision_set";

  @JsonProperty("metadata")
  private final MetadataRecord metadata;

  @JsonProperty("content")
  private final List<PrecisionExchangeEntry> content;

  @JsonProperty("declarations")
  private final List<PrecisionDeclaration> declarations;

  public PrecisionExchangeSetEntry(
      @JsonProperty("metadata") MetadataRecord pMetadata,
      @JsonProperty("declarations") List<PrecisionDeclaration> pDeclarations,
      @JsonProperty("content") List<PrecisionExchangeEntry> pContent) {
    super(PRECISION_EXCHANGE_ENTRY_IDENTIFIER);
    metadata = pMetadata;
    content = pContent;
    declarations = pDeclarations;
  }

  public List<PrecisionDeclaration> getDeclarations() {
    return declarations;
  }

  public MetadataRecord getMetadata() {
    return metadata;
  }

  public List<PrecisionExchangeEntry> getContent() {
    return content;
  }
}
