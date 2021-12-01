// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.blockgraph;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class Block {
  private final CFANode entry;

  private final ImmutableSet<CFANode> nodes;

  private final Map<CFANode, Block> exits = new HashMap<>();

  private final Set<Block> predecessors = new HashSet<>();

  private Optional<BlockGraph> nested = Optional.empty();

  private boolean complete = false;

  public Block(final CFANode pEntry, final Set<CFANode> pNodes) {
    entry = pEntry;
    nodes = ImmutableSet.copyOf(pNodes);
  }

  public void addExit(final CFANode location, final Block successor) {
    Preconditions.checkState(!complete);
    exits.put(location, successor);
  }

  public void addPredecessor(final Block block) {
    Preconditions.checkState(!complete);
    predecessors.add(block);
  }

  public BlockGraph getNestedGraph() {
    Preconditions.checkState(complete);
    Preconditions.checkState(nested.isPresent());
    return nested.orElseThrow();
  }

  public void setNestedGraph(@SuppressWarnings("unused") final BlockGraph pGraph) {
    Preconditions.checkState(!complete);
    // Todo: Re-introduce nested graphs which also support nested loops. 
    // nested = Optional.of(pGraph);
  }

  public void complete() {
    complete = true;
  }

  public boolean isComplete() {
    return complete;
  }

  public CFANode getEntry() {
    return entry;
  }

  public boolean contains(final CFANode pNode) {
    return nodes.contains(pNode);
  }

  public Map<CFANode, Block> getExits() {
    return exits;
  }

  public ImmutableSet<Block> getPredecessors() {
    Preconditions.checkState(complete);
    return ImmutableSet.copyOf(predecessors);
  }
}
