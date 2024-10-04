// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.deserialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.io.IOException;
import org.sosy_lab.cpachecker.cfa.export.json.CfaJsonImport;
import org.sosy_lab.cpachecker.cfa.export.json.EdgeToPartitionEntry;
import org.sosy_lab.cpachecker.cfa.export.json.mixins.VariableClassificationMixin;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;

/**
 * EdgeToPartitionsDeserializer is a custom deserializer for converting JSON representations of
 * tables (lists of {@link EdgeToPartitionEntry} objects) into {@link Table} objects with keys of
 * type {@link CFAEdge} and {@link Integer}, and values of type {@link Partition}.
 *
 * <p>This deserializer provides methods to deserialize individual {@link EdgeToPartitionEntry}
 * objects as well as entire tables.
 *
 * <p>It is necessary since as of september 2024, jackson-datatype-guava does not support
 * deserializing tables directly.
 *
 * @see CfaJsonImport
 * @see VariableClassificationMixin
 * @see PartitionsDeserializer
 */
public final class EdgeToPartitionsDeserializer
    extends JsonDeserializer<Table<CFAEdge, Integer, Partition>> {

  /**
   * Deserializes a JSON node into a {@link EdgeToPartitionEntry} object.
   *
   * @param pNode The JSON node to deserialize.
   * @return a EdgeToPartitionEntry object containing the deserialized data.
   * @throws IOException if an I/O error occurs during deserialization.
   */
  public static EdgeToPartitionEntry deserializeEdgeToPartitionEntry(JsonNode pNode)
      throws IOException {

    CFAEdge edge = CfaEdgeIdResolver.getEdgeFromId(pNode.get("edge").asInt());
    Integer index = pNode.get("index").asInt();
    Partition partition =
        PartitionsDeserializer.getPartitionHandler(pNode.get("partition").asInt()).getReference();

    return new EdgeToPartitionEntry(edge, index, partition);
  }

  /**
   * Deserializes a JSON representation of a table (list of {@link EdgeToPartitionEntry} objects)
   * into a Table<CFAEdge, Integer, Partition> object.
   *
   * @param pParser The JsonParser used to parse the JSON content.
   * @param pContext The DeserializationContext.
   * @return a Table containing the deserialized table.
   * @throws IOException if an I/O error occurs during parsing.
   * @throws JsonProcessingException if a processing error occurs during parsing.
   */
  @Override
  public Table<CFAEdge, Integer, Partition> deserialize(
      JsonParser pParser, DeserializationContext pContext)
      throws IOException, JsonProcessingException {

    /* Get root node. */
    ObjectMapper mapper = (ObjectMapper) pParser.getCodec();
    JsonNode rootNode = mapper.readTree(pParser);

    Table<CFAEdge, Integer, Partition> table = HashBasedTable.create();

    /* Iterate over the root node and add EdgeToPartitionEntry objects to the table. */
    for (JsonNode node : rootNode) {
      EdgeToPartitionEntry tableEntry = deserializeEdgeToPartitionEntry(node);
      table.put(tableEntry.edge(), tableEntry.index(), tableEntry.partition());
    }

    return table;
  }
}
