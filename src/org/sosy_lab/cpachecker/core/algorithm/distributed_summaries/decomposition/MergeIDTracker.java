// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;

public class MergeIDTracker {

  private Map<String, String> map;

  MergeIDTracker(Iterable<String> initialIDs) {
    map = new HashMap<>();
    for (String id : initialIDs) {
      map.put(id, id);
    }
  }

  /** returns the new block id the given block has been merged into */
  String resolve(String id) {
    String parent = map.get(id);

    if (parent.equals(id)) {
      return id;
    }

    String res = resolve(parent);
    // flatten the lookup tree
    map.put(id, res);

    return res;
  }

  void merge(Iterable<String> oldIDs, String newID) {
    for (String id : oldIDs) {
      // assert that these IDs do not already have been replaced
      assert map.replace(id, id, newID);
    }
    assert map.put(newID, newID) == null;
  }

  ImmutableSet<BlockNode> mapBlockNodes(Iterable<BlockNode> nodes) {
    return FluentIterable.from(nodes)
        .transform(
            node ->
                new BlockNode(
                    node.getId(),
                    node.getInitialLocation(),
                    node.getFinalLocation(),
                    node.getNodes(),
                    node.getEdges(),
                    transformedImmutableSetCopy(node.getPredecessorIds(), this::resolve),
                    transformedImmutableSetCopy(node.getLoopPredecessorIds(), this::resolve),
                    transformedImmutableSetCopy(node.getSuccessorIds(), this::resolve),
                    transformedImmutableSetCopy(node.getLoopSuccessorIds(), this::resolve)))
        .toSet();
  }
}
