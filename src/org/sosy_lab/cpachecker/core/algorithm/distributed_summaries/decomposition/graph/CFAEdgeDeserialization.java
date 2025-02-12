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
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.io.IOException;
import java.io.Serial;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class CFAEdgeDeserialization extends StdDeserializer<CFAEdge> {

  @Serial private static final long serialVersionUID = 2739496247736816649L;

  private final Multimap<Integer, CFAEdge> edgeMap;

  public CFAEdgeDeserialization(CFA pCfa) {
    super(CFAEdge.class);
    edgeMap = Multimaps.index(pCfa.edges(), e -> e.getFileLocation().getStartingLineNumber());
  }

  @Override
  public CFAEdge deserialize(JsonParser pJsonParser, DeserializationContext pDeserializationContext)
      throws IOException {
    JsonNode node = pJsonParser.getCodec().readTree(pJsonParser);
    int startLine = node.get("startLine").asInt();
    int startColumn = node.get("startColumn").asInt();
    int endLine = node.get("endLine").asInt();
    int endColumn = node.get("endColumn").asInt();
    String file = node.get("file").asText();
    String function = node.get("function").asText();
    ImmutableList<@NonNull CFAEdge> candidates =
        FluentIterable.from(edgeMap.get(startLine))
            .filter(
                e ->
                    e.getFileLocation().getStartingLineNumber() == startLine
                        && e.getFileLocation().getStartColumnInLine() == startColumn
                        && e.getFileLocation().getEndingLineNumber() == endLine
                        && e.getFileLocation().getEndColumnInLine() == endColumn
                        && e.getFileLocation().getNiceFileName().equals(file)
                        && e.getPredecessor().getFunctionName().equals(function))
            .toList();
    if (candidates.isEmpty()) {
      throw new AssertionError("At least one edge has to match but found none");
    }
    if (candidates.size() == 1) {
      return Iterables.getOnlyElement(candidates);
    }
    if (!node.has("cpaId")) {
      throw new AssertionError(
          "Cannot identify unique CFAEdge in candidates while no cpaId is provided: " + candidates);
    }
    // some cfa edges cannot be mapped uniquely
    String id = node.get("cpaId").asText();
    return Iterables.getOnlyElement(
        FluentIterable.from(candidates)
            .filter(
                e ->
                    id.equals(
                        "N"
                            + e.getPredecessor().getNodeNumber()
                            + "N"
                            + e.getSuccessor().getNodeNumber())));
  }
}
