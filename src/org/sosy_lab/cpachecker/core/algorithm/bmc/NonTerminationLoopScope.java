// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 CPAchecker contributors
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

record NonTerminationLoopScope(
    Loop loop, ImmutableSet<CFANode> loopHeads, ImmutableSet<CFANode> loopNodes) {

  static NonTerminationLoopScope of(Loop pLoop) {
    return new NonTerminationLoopScope(
        pLoop,
        ImmutableSet.copyOf(pLoop.getLoopHeads()),
        ImmutableSet.copyOf(pLoop.getLoopNodes()));
  }
}
