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
import org.sosy_lab.java_smt.api.BooleanFormula;

public class Dispatcher {

  private final ConcurrentHashMap<BlockNodeId, Runner> registeredRunners;

  public Dispatcher() {
    registeredRunners = new ConcurrentHashMap<>();
  }

  public synchronized Runner register(BlockNode pNode, Algorithm pAlgorithm) {
    Runner runner = new Runner(pNode, pAlgorithm, this, null);
    registeredRunners.put(pNode.getId(), runner);
    return runner;
  }

  public synchronized void sendPreConditionTo(BlockNodeId id, BooleanFormula message) {
    runnerFor(id).updatePostCondition(id, message);
  }

  public synchronized void sendPostConditionTo(BlockNodeId id, BooleanFormula message) {
    runnerFor(id).updatePreCondition(id, message);
  }

  public void start() {
    registeredRunners.values().forEach(Runner::analyzeBlock);
  }

  private synchronized Runner runnerFor(BlockNodeId id) {
    if (!registeredRunners.containsKey(id)) {
      throw new AssertionError("Runner for id " + id + " does not exist");
    }
    return registeredRunners.get(id);
  }
}
