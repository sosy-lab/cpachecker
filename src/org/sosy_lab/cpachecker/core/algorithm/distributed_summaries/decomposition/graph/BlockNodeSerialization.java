// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class BlockNodeSerialization extends StdSerializer<BlockNode> {

  private final CFAEdgeSerialization edgeSerialization;

  public BlockNodeSerialization() {
    super(BlockNode.class);
    edgeSerialization = new CFAEdgeSerialization();
  }

  @Override
  public void serialize(
      BlockNode pBlockNode, JsonGenerator pJsonGenerator, SerializerProvider pSerializerProvider)
      throws IOException {
    pJsonGenerator.writeStartObject();

    pJsonGenerator.writeStringField("id", pBlockNode.getId());
    pJsonGenerator.writeFieldName("edges");
    pJsonGenerator.writeStartArray("edges");
    for (CFAEdge edge : pBlockNode.getEdges()) {
      edgeSerialization.serialize(edge, pJsonGenerator, pSerializerProvider);
    }
    pJsonGenerator.writeEndArray();
    writeStringCollection(pBlockNode.getPredecessorIds(), "predecessors", pJsonGenerator);
    writeStringCollection(pBlockNode.getSuccessorIds(), "successors", pJsonGenerator);
    writeStringCollection(pBlockNode.getLoopPredecessorIds(), "loopPredecessors", pJsonGenerator);

    pJsonGenerator.writeFieldName("violationConditionLocation");
    edgeSerialization.serialize(
        locationToEdge(pBlockNode.getFinalLocation(), pBlockNode, false),
        pJsonGenerator,
        pSerializerProvider);

    pJsonGenerator.writeFieldName("initialLocation");
    edgeSerialization.serialize(
        locationToEdge(pBlockNode.getInitialLocation(), pBlockNode, true),
        pJsonGenerator,
        pSerializerProvider);

    pJsonGenerator.writeFieldName("finalLocation");
    edgeSerialization.serialize(
        locationToEdge(pBlockNode.getFinalLocation(), pBlockNode, false),
        pJsonGenerator,
        pSerializerProvider);

    pJsonGenerator.writeEndObject();
  }

  private CFAEdge locationToEdge(CFANode location, BlockNode node, boolean outgoing) {
    if (node.isRoot()) {
      return CFAUtils.allLeavingEdges(location).iterator().next();
    }
    Function<CFANode, Iterable<CFAEdge>> leavingOrEnteringEdges =
        outgoing ? n -> CFAUtils.allLeavingEdges(n) : n -> CFAUtils.allEnteringEdges(n);
    Set<CFAEdge> intersection =
        Sets.intersection(
            ImmutableSet.copyOf(leavingOrEnteringEdges.apply(location)), node.getEdges());
    return intersection.iterator().next();
  }

  private void writeStringCollection(
      Collection<String> items, String name, JsonGenerator pJsonGenerator) throws IOException {
    pJsonGenerator.writeFieldName(name);
    pJsonGenerator.writeStartArray(name);
    for (String item : items) {
      pJsonGenerator.writeString(item);
    }
    pJsonGenerator.writeEndArray();
  }
}
