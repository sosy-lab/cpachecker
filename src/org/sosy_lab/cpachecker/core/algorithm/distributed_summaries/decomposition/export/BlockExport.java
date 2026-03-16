// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.export;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.time.Instant;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;

public class BlockExport {

  public String writeBlockDefinition(BlockGraph blockGraph) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    BlockDefinition def = export(blockGraph);
    return mapper.writeValueAsString(def);
  }

  public BlockDefinition export(BlockGraph pBlockGraph) {
    return new BlockDefinition(
        new DssMetadata(
            "1.0",
            Instant.now().toString(),
            new Producer("CPAchecker", "2.2", "DSS"),
            new Task(ImmutableList.of(), ImmutableMap.of(), "unreach", "C")),
        transformedImmutableListCopy(pBlockGraph.getNodes(), this::export));
  }

  private BlockSegment createBlockSegmentFromInitialLocation(BlockNode pNode) {
    CFANode initialLocation = pNode.getInitialLocation();
    FluentIterable<CFAEdge> leavingEdges = initialLocation.getAllLeavingEdges();
    int numLeavingEdges = leavingEdges.size();
    int containedLeavingEdges = 0;
    Optional<Boolean> follow = Optional.empty();
    FileLocation earliestFileLocation = null;
    for (CFAEdge leavingEdge : leavingEdges) {
      if (pNode.getEdges().contains(leavingEdge)) {
        if (earliestFileLocation == null) {
          earliestFileLocation = leavingEdge.getFileLocation();
        } else {
          boolean isEarlier =
              leavingEdge.getFileLocation().getStartingLineInOrigin()
                  < earliestFileLocation.getStartingLineInOrigin();
          boolean startsOnSameLine =
              leavingEdge.getFileLocation().getStartingLineInOrigin()
                  == earliestFileLocation.getStartingLineInOrigin();
          boolean isEarlierOnLine =
              leavingEdge.getFileLocation().getStartColumnInLine()
                  < earliestFileLocation.getStartColumnInLine();
          if (isEarlier || (startsOnSameLine && isEarlierOnLine)) {
            earliestFileLocation = leavingEdge.getFileLocation();
          }
        }
        containedLeavingEdges++;
        if (leavingEdge instanceof AssumeEdge pAssumeEdge) {
          follow = Optional.of(pAssumeEdge.getTruthAssumption());
        }
      }
    }
    Preconditions.checkNotNull(
        earliestFileLocation, "No leaving edges found for block node: %s", pNode);
    if (containedLeavingEdges == numLeavingEdges) {
      follow = Optional.empty();
    }
    return new BlockSegment(
        ImmutableList.of(
            new BlockSummaryWaypoint(
                true,
                export(earliestFileLocation),
                follow.map(b -> Boolean.toString(b)),
                ImmutableList.sortedCopyOf(pNode.getPredecessorIds()))));
  }

  private BlockSegment createBlockSegmentFromFinalLocation(BlockNode pNode) {
    CFANode finalLocation = pNode.getFinalLocation();
    FileLocation latestFileLocation = null;
    for (CFAEdge enteringEdge : finalLocation.getAllEnteringEdges()) {
      if (pNode.getEdges().contains(enteringEdge)) {
        if (latestFileLocation == null) {
          latestFileLocation = enteringEdge.getFileLocation();
        } else {
          if (enteringEdge.getFileLocation().getStartingLineInOrigin()
                  > latestFileLocation.getStartingLineInOrigin()
              || (enteringEdge.getFileLocation().getStartingLineInOrigin()
                      == latestFileLocation.getStartingLineInOrigin()
                  && enteringEdge.getFileLocation().getStartColumnInLine()
                      > latestFileLocation.getStartColumnInLine())) {
            latestFileLocation = enteringEdge.getFileLocation();
          }
        }
      }
    }
    Preconditions.checkNotNull(
        latestFileLocation, "No entering edges found for block node: %s", pNode);
    return new BlockSegment(
        ImmutableList.of(
            new BlockSummaryWaypoint(
                false,
                export(latestFileLocation),
                Optional.empty(),
                ImmutableList.sortedCopyOf(pNode.getSuccessorIds()))));
  }

  public BlockMetadata export(BlockNode pNode) {
    ImmutableList.Builder<BlockSegment> segments =
        ImmutableList.builderWithExpectedSize(pNode.getEdges().size() + 2);

    BlockSegment firstSegment = createBlockSegmentFromInitialLocation(pNode);
    BlockLocation initialLocation =
        Iterables.getOnlyElement(firstSegment.getWaypoints()).getLocation();
    segments.add(firstSegment);

    // export all segments for remaining edges
    CFANode finalLocation = pNode.getFinalLocation();
    for (CFAEdge edge : pNode.getEdges()) {
      String type;
      String action;
      if (edge instanceof AssumeEdge) {
        type = "branching";
        action = "follow-true";
      } else if (edge.getSuccessor().equals(finalLocation)) {
        type = "target";
        action = "follow";
      } else {
        type = "follow";
        action = "follow";
      }
      segments.add(export(edge, type, action));
    }

    segments.add(createBlockSegmentFromFinalLocation(pNode));
    return new BlockMetadata(
        pNode.getId(),
        ImmutableList.sortedCopyOf(pNode.getPredecessorIds()),
        ImmutableList.sortedCopyOf(pNode.getSuccessorIds()),
        new BlockDescription(initialLocation, segments.build()));
  }

  public BlockSegment export(CFAEdge pEdge, String pType, String pAction) {
    return new BlockSegment(
        ImmutableList.of(new BlockWaypoint(pType, pAction, export(pEdge.getFileLocation()))));
  }

  public BlockLocation export(FileLocation pFileLocation) {
    return new BlockLocation(
        pFileLocation.getFileName().toString(),
        pFileLocation.getStartingLineInOrigin(),
        pFileLocation.getStartColumnInLine());
  }
}
