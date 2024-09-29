// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.export.json.PartitionHandler;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;

/**
 * Custom JSON serializer for serializing a set of {@link Partition}s.
 *
 * <p>It serializes the partitions as an array of objects. Each object represents a partition.
 */
public final class PartitionsSerializer extends JsonSerializer<Set<Partition>> {

  @Override
  public void serialize(
      Set<Partition> pPartitions, JsonGenerator pGenerator, SerializerProvider pProvider)
      throws IOException {
    pGenerator.writeStartArray();

    for (Partition partition : pPartitions) {
      pGenerator.writeStartObject();

      /* Index */
      pGenerator.writeNumberField("index", partition.hashCode());

      /* Vars */
      pGenerator.writeArrayFieldStart("vars");
      for (String var : partition.getVars()) {
        pGenerator.writeString(var);
      }
      pGenerator.writeEndArray();

      /* Values */
      pGenerator.writeArrayFieldStart("values");
      for (BigInteger value : partition.getValues()) {
        pGenerator.writeObject(value);
      }
      pGenerator.writeEndArray();

      /* Edges (sorted) */
      List<Entry<CFAEdge, Collection<Integer>>> entries =
          new ArrayList<>(partition.getEdges().asMap().entrySet());
      Collections.sort(
          entries,
          Comparator.comparingInt(entry -> CfaEdgeIdGenerator.getIdFromEdge(entry.getKey())));

      pGenerator.writeArrayFieldStart("edges");
      for (Entry<CFAEdge, Collection<Integer>> entry : entries) {
        pGenerator.writeStartObject();
        pGenerator.writeNumberField("edge", CfaEdgeIdGenerator.getIdFromEdge(entry.getKey()));
        pGenerator.writeArrayFieldStart("indices");
        for (Integer index : entry.getValue()) {
          pGenerator.writeObject(index);
        }
        pGenerator.writeEndArray();
        pGenerator.writeEndObject();
      }
      pGenerator.writeEndArray();

      try {
        /* VarToPartition */
        /* Retrieve field via reflection. */
        Map<String, Partition> varToPartition = PartitionHandler.readVarToPartition(partition);

        /* Write field. */
        pGenerator.writeObjectFieldStart("varToPartition");
        for (Entry<String, Partition> entry : varToPartition.entrySet()) {
          pGenerator.writeObjectField(entry.getKey(), entry.getValue().hashCode());
        }
        pGenerator.writeEndObject();

        /* EdgeToPartition */
        /* Retrieve field via reflection. */
        Table<CFAEdge, Integer, Partition> edgeToPartition =
            PartitionHandler.readEdgeToPartition(partition);

        /* Write field. */
        pGenerator.writeArrayFieldStart("edgeToPartition");
        for (Cell<CFAEdge, Integer, Partition> cell : edgeToPartition.cellSet()) {
          pGenerator.writeStartObject();
          pGenerator.writeNumberField("edge", CfaEdgeIdGenerator.getIdFromEdge(cell.getRowKey()));
          pGenerator.writeNumberField("index", cell.getColumnKey());
          pGenerator.writeNumberField("partition", cell.getValue().hashCode());
          pGenerator.writeEndObject();
        }
        pGenerator.writeEndArray();

      } catch (IllegalArgumentException e) {
        throw new java.io.IOException("Error while serializing partition: " + e.getMessage(), e);
      }

      pGenerator.writeEndObject();
    }

    pGenerator.writeEndArray();
  }
}
