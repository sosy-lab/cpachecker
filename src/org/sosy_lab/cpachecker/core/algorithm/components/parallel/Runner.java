// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.parallel;

import java.util.concurrent.ConcurrentHashMap;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode.BlockNodeId;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class Runner {

  private final Dispatcher dispatcher;

  private final BlockNode block;
  private final Algorithm analysis;

  private final BooleanFormulaManagerView bmgr;
  private final ConcurrentHashMap<BlockNodeId, BooleanFormula> postConditionUpdates;
  private final ConcurrentHashMap<BlockNodeId, BooleanFormula> preConditionUpdates;

  public Runner(
      BlockNode pBlock,
      Algorithm pAlgorithm,
      Dispatcher pDispatcher,
      BooleanFormulaManagerView pBooleanFormulaManager) {
    dispatcher = pDispatcher;
    block = pBlock;
    analysis = pAlgorithm;
    bmgr = pBooleanFormulaManager;
    postConditionUpdates = new ConcurrentHashMap<>();
    preConditionUpdates = new ConcurrentHashMap<>();

    preConditionUpdates.put(block.getId(), bmgr.makeTrue());
    postConditionUpdates.put(block.getId(), bmgr.makeTrue());
  }

  public void analyzeBlock() {

    BooleanFormula postcondition = postConditionUpdates.get(block.getId());
    block
        .getSuccessors()
        .forEach(node -> dispatcher.sendPostConditionTo(node.getId(), postcondition));
  }

  public BooleanFormula getPostCondition() {
    if (postConditionUpdates.containsKey(block.getId())) {
      throw new AssertionError("postConditionUpdates must contain own post-condition");
    }
    return postConditionUpdates.get(block.getId());
  }

  public BooleanFormula getPreCondition() {
    if (preConditionUpdates.containsKey(block.getId())) {
      throw new AssertionError("preConditionUpdates must contain own pre-condition");
    }
    return preConditionUpdates.get(block.getId());
  }

  public synchronized void updatePreCondition(BlockNodeId id, BooleanFormula formula) {
    preConditionUpdates.put(id, formula);
  }

  public synchronized void updatePostCondition(BlockNodeId id, BooleanFormula formula) {
    postConditionUpdates.put(id, formula);
  }
}
