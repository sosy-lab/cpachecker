// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** A set of predicate definitions for memory-safety correctness witnesses in witness format 2.4. */
@JsonPropertyOrder({"entry_type", "metadata", "content"})
public class PredicateDefinitionSetEntry extends AbstractEntry {

  private static final String PREDICATE_DEFINITION_SET_IDENTIFIER = "predicate_definition_set";

  @JsonProperty("metadata")
  private final MetadataRecord metadata;

  @JsonProperty("content")
  private final List<PredicateDefinitionRecord> content;

  public PredicateDefinitionSetEntry(
      @JsonProperty("metadata") MetadataRecord pMetadata,
      @JsonProperty("content") List<PredicateDefinitionRecord> pContent) {
    super(PREDICATE_DEFINITION_SET_IDENTIFIER);
    metadata = pMetadata;
    content = pContent;
  }

  public MetadataRecord getMetadata() {
    return metadata;
  }

  public List<PredicateDefinitionRecord> getContent() {
    return content;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof PredicateDefinitionSetEntry pOtherPredicateDefinitionSetEntry
        && metadata.equals(pOtherPredicateDefinitionSetEntry.getMetadata())
        && content.equals(pOtherPredicateDefinitionSetEntry.getContent());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(metadata, content);
  }

  @Override
  public String toString() {
    return PREDICATE_DEFINITION_SET_IDENTIFIER
        + " [metadata="
        + metadata
        + ", content=["
        + String.join(
            ", ",
            content.stream()
                .map(PredicateDefinitionRecord::toString)
                .collect(ImmutableList.toImmutableList()))
        + "]"
        + "]";
  }
}
