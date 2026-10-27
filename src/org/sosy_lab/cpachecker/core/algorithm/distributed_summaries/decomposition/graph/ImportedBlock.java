// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import java.util.List;
import org.jspecify.annotations.Nullable;

public record ImportedBlock(
    List<String> code,
    List<String> predecessors,
    List<String> successors,
    int startNode,
    int endNode,
    List<List<Integer>> edges,
    int abstractionLocation,
    List<String> loopPredecessors,
    List<String> loopSuccessors,
    // set by the block-graph-only export if a specification was given, see BlockGraph#containsTarget
    @Nullable Boolean containsTarget) {}
