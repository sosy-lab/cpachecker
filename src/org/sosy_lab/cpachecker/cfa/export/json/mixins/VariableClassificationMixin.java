// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;
import java.util.Collection;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.export.json.deserialization.EdgeToPartitionsDeserializer;
import org.sosy_lab.cpachecker.cfa.export.json.deserialization.PartitionsDeserializer;
import org.sosy_lab.cpachecker.cfa.export.json.serialization.EdgeToPartitionsTableToListConverter;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/**
 * This class is a mixin for {@link VariableClassification}.
 *
 * <p>It sets the {@link PartitionsDeserializer} for all Set<Partition> fields.
 *
 * <p>It converts the edgeToPartitions field to a list of TableEntry objects during serialization
 * and back to a Table object during deserialization.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
public final class VariableClassificationMixin {

  @SuppressWarnings("unused")
  @JsonDeserialize(using = PartitionsDeserializer.class)
  private Set<Partition> partitions;

  @SuppressWarnings("unused")
  @JsonDeserialize(using = PartitionsDeserializer.class)
  private Set<Partition> intBoolPartitions;

  @SuppressWarnings("unused")
  @JsonDeserialize(using = PartitionsDeserializer.class)
  private Set<Partition> intEqualPartitions;

  @SuppressWarnings("unused")
  @JsonDeserialize(using = PartitionsDeserializer.class)
  private Set<Partition> intAddPartitions;

  @SuppressWarnings("unused")
  @JsonSerialize(converter = EdgeToPartitionsTableToListConverter.class)
  @JsonDeserialize(using = EdgeToPartitionsDeserializer.class)
  private Table<CFAEdge, Integer, Partition> edgeToPartitions;

  @SuppressWarnings("unused")
  @JsonCreator
  VariableClassificationMixin(
      @JsonProperty("hasRelevantNonIntAddVars") boolean pHasRelevantNonIntAddVars,
      @JsonProperty("intBoolVars") Set<String> pIntBoolVars,
      @JsonProperty("intEqualVars") Set<String> pIntEqualVars,
      @JsonProperty("intAddVars") Set<String> pIntAddVars,
      @JsonProperty("intOverflowVars") Set<String> pIntOverflowVars,
      @JsonProperty("relevantVariables") Set<String> pRelevantVariables,
      @JsonProperty("addressedVariables") Set<String> pAddressedVariables,
      @JsonProperty("relevantFields") Multimap<CCompositeType, String> pRelevantFields,
      @JsonProperty("addressedFields") Multimap<CCompositeType, String> pAddressedFields,
      @JsonProperty("partitions") Collection<Partition> pPartitions,
      @JsonProperty("intBoolPartitions") Set<Partition> pIntBoolPartitions,
      @JsonProperty("intEqualPartitions") Set<Partition> pIntEqualPartitions,
      @JsonProperty("intAddPartitions") Set<Partition> pIntAddPartitions,
      @JsonProperty("edgeToPartitions") Table<CFAEdge, Integer, Partition> pEdgeToPartitions,
      @JsonProperty("assumedVariables") Multiset<String> pAssumedVariables,
      @JsonProperty("assignedVariables") Multiset<String> pAssignedVariables) {}
}
