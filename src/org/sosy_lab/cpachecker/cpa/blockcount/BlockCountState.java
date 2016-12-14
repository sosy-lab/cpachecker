/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.blockcount;

import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.util.assumptions.PreventingHeuristic;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

import javax.annotation.Nullable;

public class BlockCountState
    implements AbstractState, AvoidanceReportingState, Partitionable, Comparable<BlockCountState> {

  private final int count;

  private final boolean stop;

  private BlockCountState(int pCount) {
    this(pCount, false);
  }

  private BlockCountState(int pCount, boolean pStop) {
    Preconditions.checkArgument(pCount >= 0, "Count must be positive.");
    count = pCount;
    stop = pStop;
  }

  public int getCount() {
    return count;
  }

  BlockCountState stop() {
    if (stop) {
      return this;
    }
    return new BlockCountState(count, true);
  }

  BlockCountState incrementCount() {
    return new BlockCountState(count + 1);
  }

  @Override
  public String toString() {
    return String.format("Number of blocks: %d", count);
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof BlockCountState) {
      return count == ((BlockCountState) pObj).count;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return (count << 1) | (stop ? 1 : 0);
  }

  static BlockCountState of(int pCount) {
    return of(pCount, false);
  }

  static BlockCountState of(int pCount, boolean pStop) {
    return new BlockCountState(pCount, pStop);
  }

  @Override
  public BooleanFormula getReasonFormula(FormulaManagerView manager) {
    BooleanFormulaManager bfmgr = manager.getBooleanFormulaManager();
    BooleanFormula reasonFormula = bfmgr.makeTrue();
    if (stop) {
      reasonFormula =
          bfmgr.and(reasonFormula, PreventingHeuristic.BLOCKCOUNT.getFormula(manager, count));
    }
    return reasonFormula;
  }

  public boolean isStopState() {
    return stop;
  }

  @Override
  public boolean mustDumpAssumptionForAvoidance() {
    return isStopState();
  }

  @Override
  @Nullable
  public Object getPartitionKey() {
    return this;
  }

  @Override
  public int compareTo(BlockCountState pO) {
    return ComparisonChain.start()
        .compare(count, pO.count)
        .compareFalseFirst(stop, pO.stop)
        .result();
  }
}
