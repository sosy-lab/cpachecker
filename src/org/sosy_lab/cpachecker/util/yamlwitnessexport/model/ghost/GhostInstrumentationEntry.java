// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ghost;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.MetadataRecord;

@JsonPropertyOrder({"entry_type", "metadata", "content"})
public class GhostInstrumentationEntry extends AbstractEntry {

  private static final String GHOST_INSTRUMENTATION_ENTRY_IDENTIFIER = "ghost_instrumentation";

  @JsonProperty("metadata")
  public final MetadataRecord metadata;

  @JsonProperty("content")
  public final GhostInstrumentationContentRecord content;

  public GhostInstrumentationEntry(
      @JsonProperty("metadata") MetadataRecord pMetadata,
      @JsonProperty("content") GhostInstrumentationContentRecord pContent) {
    super(GHOST_INSTRUMENTATION_ENTRY_IDENTIFIER);
    metadata = pMetadata;
    content = pContent;
  }
}
