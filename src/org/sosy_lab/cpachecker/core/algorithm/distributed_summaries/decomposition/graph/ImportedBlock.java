// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import java.util.List;

public record ImportedBlock(
    List<String> code,
    List<String> predecessors,
    List<String> successors,
    int startNode,
    int endNode,
    List<List<Integer>> edges,
    int abstractionLocation,
    List<String> loopPredecessors) {}
