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

/**
 * Currently a correctness witness is composed of a list of sets of entries. Each entry contains
 * some information about the proof of the program, currently these are invariants and function
 * contracts. This class represents one set of entries in a correctness witness.
 */
@JsonPropertyOrder({"entry_type", "metadata", "content"})
public class CorrectnessWitnessSetEntry extends AbstractEntry {

  private static final String SET_ENTRY_IDENTIFIER = "entry_set";

  @JsonProperty("metadata")
  private final MetadataRecord metadata;

  @JsonProperty("content")
  private final List<CorrectnessWitnessSetElementEntry> content;

  public CorrectnessWitnessSetEntry(
      @JsonProperty("metadata") MetadataRecord pMetadata,
      @JsonProperty("content") List<CorrectnessWitnessSetElementEntry> pContent) {
    super(SET_ENTRY_IDENTIFIER);
    metadata = pMetadata;
    content = pContent;
  }
}
