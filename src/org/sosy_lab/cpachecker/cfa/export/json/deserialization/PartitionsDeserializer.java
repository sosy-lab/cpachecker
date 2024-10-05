// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.deserialization;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.export.json.CfaJsonData;
import org.sosy_lab.cpachecker.cfa.export.json.CfaJsonImport;
import org.sosy_lab.cpachecker.cfa.export.json.EdgeToPartitionEntry;
import org.sosy_lab.cpachecker.cfa.export.json.PartitionHandler;
import org.sosy_lab.cpachecker.cfa.export.json.mixins.VariableClassificationMixin;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;

/**
 * The PartitionsDeserializer class is responsible for deserializing JSON data into a set of {@link
 * Partition}s.
 *
 * <p>The deserialization process involves reading the JSON data, constructing PartitionHandler
 * objects, and adding variables, values, edges, and mappings to each PartitionHandler. Finally, the
 * deserialized Partitions are returned as a set.
 *
 * <p>In order for the deserialization process to work, the set of all {@link CFAEdge}s must be
 * deserialized before the set of all {@link Partition}s. This is because the deserialization of
 * Partitions involves references to {@link CFAEdge}s.
 *
 * @see CfaJsonImport
 * @see CfaJsonData
 * @see VariableClassificationMixin
 * @see PartitionIdResolver
 */
public final class PartitionsDeserializer extends JsonDeserializer<Set<Partition>> {
  private static ThreadLocal<Map<Integer, PartitionHandler>> partitionHandlers =
      ThreadLocal.withInitial(HashMap::new);

  /* Retrieves an existing PartitionHandler or creates a new one if it does not exist. */
  public static PartitionHandler getPartitionHandler(int pIndex) throws IOException {
    PartitionHandler handler;

    Map<Integer, PartitionHandler> handlers = partitionHandlers.get();

    checkNotNull(handlers, "No partitionHandlers available");

    if (handlers.containsKey(pIndex)) {
      handler = handlers.get(pIndex);

    } else {
      handler = new PartitionHandler(pIndex);
      handlers.put(pIndex, handler);
    }

    return handler;
  }

  /**
   * Deserialize a JSON representation of partitions into a set of {@link Partition} objects.
   *
   * @param pParser The JSON parser.
   * @param pContext The deserialization context.
   * @return the set of deserialized partitions.
   * @throws IOException if an I/O error occurs during deserialization.
   */
  @Override
  public Set<Partition> deserialize(JsonParser pParser, DeserializationContext pContext)
      throws IOException {

    Set<Partition> deserializedPartitions = new HashSet<>();

    /* Get root node. */
    ObjectMapper mapper = (ObjectMapper) pParser.getCodec();
    JsonNode rootNode = mapper.readTree(pParser);

    /* Iterate over the root node and construct PartitionHandlers. */
    for (JsonNode node : rootNode) {
      PartitionHandler handler;

      if (node.isObject()) {
        /* Node is an object: Full size deserialization. */

        /* Get handler. */
        Integer index = node.get("index").asInt();
        handler = getPartitionHandler(index);

        /* Vars */
        for (JsonNode var : node.get("vars")) {
          handler.addVar(var.asText());
        }

        /* Values */
        for (JsonNode value : node.get("values")) {
          handler.addValue(value.bigIntegerValue());
        }

        /* Edges */
        for (JsonNode edge : node.get("edges")) {
          CFAEdge cfaEdge = CfaEdgeIdResolver.getEdgeFromId(edge.get("edge").asInt());

          for (JsonNode edgeIndex : edge.get("indices")) {
            handler.addEdge(cfaEdge, edgeIndex.asInt());
          }
        }

        /* VarToPartition */
        Iterator<Map.Entry<String, JsonNode>> fields = node.get("varToPartition").fields();
        while (fields.hasNext()) {
          Map.Entry<String, JsonNode> field = fields.next();

          Partition partition = getPartitionHandler(field.getValue().asInt()).getReference();

          handler.addVarToPartition(field.getKey(), partition);
        }

        /* EdgeToPartition */
        for (JsonNode etp : node.get("edgeToPartition")) {
          EdgeToPartitionEntry tableEntry =
              EdgeToPartitionsDeserializer.deserializeEdgeToPartitionEntry(etp);
          handler.addEdgeToPartition(tableEntry.edge(), tableEntry.index(), tableEntry.partition());
        }

      } else {

        /* Node is an integer: Deserialization from object id. */
        handler = getPartitionHandler(node.asInt());
      }

      deserializedPartitions.add(handler.getReference());
    }

    return ImmutableSet.copyOf(deserializedPartitions);
  }
}
