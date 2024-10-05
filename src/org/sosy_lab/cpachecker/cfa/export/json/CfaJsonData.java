// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.TreeMultimap;
import java.util.NavigableMap;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.export.json.deserialization.CfaEdgeIdResolver;
import org.sosy_lab.cpachecker.cfa.export.json.deserialization.EdgeToPartitionsDeserializer;
import org.sosy_lab.cpachecker.cfa.export.json.deserialization.PartitionIdResolver;
import org.sosy_lab.cpachecker.cfa.export.json.deserialization.PartitionsDeserializer;
import org.sosy_lab.cpachecker.cfa.export.json.serialization.CfaEdgeIdGenerator;
import org.sosy_lab.cpachecker.cfa.export.json.serialization.EdgeToPartitionsTableToListConverter;
import org.sosy_lab.cpachecker.cfa.export.json.serialization.PartitionSetToSortedListConverter;
import org.sosy_lab.cpachecker.cfa.export.json.serialization.PartitionsSerializer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/**
 * This record represents the JSON data structure that a CFA is exported to and imported from.
 *
 * <p>The order of the fields is important for the deserialization process! Have a look at {@link
 * PartitionsDeserializer} for more information.
 *
 * <p>The serialization and deserialization of {@link Partition}s is only necessary if we want to
 * serialize and deserialize the {@link VariableClassification} field of {@link CfaMetadata}.
 *
 * <p>Involved in this serialization process are:
 *
 * <ul>
 *   <li>{@link PartitionsSerializer}
 *   <li>{@link CfaEdgeIdGenerator}
 *   <li>{@link EdgeToPartitionsTableToListConverter}
 *   <li>{@link PartitionSetToSortedListConverter}
 * </ul>
 *
 * <p>Involved in this deserialization process are:
 *
 * <ul>
 *   <li>{@link PartitionsDeserializer}
 *   <li>{@link CfaEdgeIdResolver}
 *   <li>{@link EdgeToPartitionsDeserializer}
 *   <li>{@link PartitionIdResolver}
 * </ul>
 *
 * <p>Involved in both serialization and deserialization are:
 *
 * <ul>
 *   <li>{@link PartitionHandler}
 *   <li>{@link EdgeToPartitionEntry}
 * </ul>
 *
 * @see CfaJsonImport
 * @see CfaJsonExport
 */
public record CfaJsonData(
    TreeMultimap<String, CFANode> nodes,
    Set<CFAEdge> edges,
    NavigableMap<String, FunctionEntryNode> functions,
    @JsonSerialize(using = PartitionsSerializer.class)
        @JsonDeserialize(using = PartitionsDeserializer.class)
        Set<Partition> partitions,
    CfaMetadata metadata) {}
