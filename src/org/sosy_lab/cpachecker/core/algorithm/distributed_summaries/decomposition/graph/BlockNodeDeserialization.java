// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class BlockNodeDeserialization extends StdDeserializer<BlockNode> {

  @Serial private static final long serialVersionUID = -258539948499994266L;

  private final ObjectMapper edgeMapper;

  public BlockNodeDeserialization(CFA pCfa) {
    super(BlockNode.class);
    edgeMapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addDeserializer(CFAEdge.class, new CFAEdgeDeserialization(pCfa));
    edgeMapper.registerModule(module);
  }

  @Override
  public BlockNode deserialize(
      JsonParser pJsonParser, DeserializationContext pDeserializationContext) throws IOException {
    JsonNode node = pJsonParser.getCodec().readTree(pJsonParser);
    String id = node.get("id").asText();
    ImmutableSet<String> predecessors = readStringCollection((ArrayNode) node.get("predecessors"));
    ImmutableSet<String> successors = readStringCollection((ArrayNode) node.get("successors"));
    ImmutableSet<String> loopPredecessors =
        readStringCollection((ArrayNode) node.get("loopPredecessors"));
    ImmutableSet.Builder<CFAEdge> edgeBuilder = ImmutableSet.builder();
    for (JsonNode edgeNode : node.get("edges")) {
      CFAEdge edge = edgeMapper.readerFor(CFAEdge.class).readValue(edgeNode);
      edgeBuilder.add(edge);
    }
    ImmutableSet<CFAEdge> edges = edgeBuilder.build();
    ImmutableSet<CFANode> nodes =
        FluentIterable.from(edges)
            .transformAndConcat(e -> ImmutableList.of(e.getPredecessor(), e.getSuccessor()))
            .toSet();
    CFAEdge violationCondition =
        edgeMapper.readerFor(CFAEdge.class).readValue(node.get("violationConditionLocation"));
    CFAEdge initialLocation =
        edgeMapper.readerFor(CFAEdge.class).readValue(node.get("initialLocation"));
    CFAEdge finalLocation =
        edgeMapper.readerFor(CFAEdge.class).readValue(node.get("finalLocation"));
    return new BlockNode(
        id,
        initialLocation.getPredecessor(),
        finalLocation.getSuccessor(),
        nodes,
        edges,
        predecessors,
        loopPredecessors,
        successors,
        violationCondition.getSuccessor());
  }

  private ImmutableSet<String> readStringCollection(ArrayNode array) {
    ImmutableSet.Builder<String> items = ImmutableSet.builder();
    for (JsonNode item : array) {
      items.add(item.asText());
    }
    return items.build();
  }
}
