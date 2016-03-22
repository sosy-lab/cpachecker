/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoin;
import org.sosy_lab.cpachecker.util.refinement.Interpolant;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.collect.ImmutableMap;


public class SMGInterpolant implements Interpolant<SMGState> {

  private static final SMGInterpolant FALSE = new SMGInterpolant(null, null, null, 0);

  private final Map<SMGKnownSymValue, SMGKnownExpValue> explicitValues;
  private final CLangSMG heap;
  private final LogManager logger;
  private final int externalAllocationSize;


  public SMGInterpolant(Map<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues,
      CLangSMG pHeap,
      LogManager pLogger, int pExternalAllocationSize) {

    explicitValues = pExplicitValues;
    heap = pHeap;
    logger = pLogger;
    externalAllocationSize = pExternalAllocationSize;
  }

  @Override
  public SMGState reconstructState() {

    if(isFalse()) {
      throw new IllegalStateException("Can't reconstruct state from FALSE-interpolant");
    } else {
      // TODO Copy necessary?
      return new SMGState(new HashMap<>(explicitValues), new CLangSMG(heap), logger,
          externalAllocationSize);
    }
  }

  @Override
  public int getSize() {
    return isTrivial() ? 0 : heap.getHVEdges().size();
  }

  @Override
  public Set<MemoryLocation> getMemoryLocations() {
    return heap.getTrackedMemoryLocations();
  }

  @Override
  public boolean isTrue() {
    return !isFalse() && heap.getHVEdges().isEmpty();
  }

  @Override
  public boolean isFalse() {
    return heap == null;
  }

  @Override
  public boolean isTrivial() {
    return isTrue() || isFalse();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Interpolant<SMGState>> T join(T pOtherInterpolant) {
    assert pOtherInterpolant instanceof SMGInterpolant;

    SMGInterpolant other = (SMGInterpolant) pOtherInterpolant;

    return (T) join0(other);
  }

  private SMGInterpolant join0(SMGInterpolant other) {

    if (isFalse() || other.isFalse()) {
      return SMGInterpolant.FALSE;
    }

    SMGJoin join;

    try {
      join = new SMGJoin(heap, other.heap);
    } catch (SMGInconsistentException e) {
      throw new IllegalStateException("Can't join interpolants due to: " + e.getMessage());
    }


    Map<SMGKnownSymValue, SMGKnownExpValue> joinedExplicitValues = new HashMap<>();

    //FIXME join of explicit values
    joinedExplicitValues.putAll(explicitValues);
    joinedExplicitValues.putAll(other.explicitValues);


    return new SMGInterpolant(joinedExplicitValues, join.getJointSMG(), logger, externalAllocationSize);
  }

  public static SMGInterpolant createInitial(LogManager logger, MachineModel model,
      FunctionEntryNode pMainFunctionNode, int pExternalAllocationSize) {
    Map<SMGKnownSymValue, SMGKnownExpValue> explicitValues = ImmutableMap.of();
    CLangSMG heap = new CLangSMG(model);
    AFunctionDeclaration mainFunction = pMainFunctionNode.getFunctionDefinition();

    if (mainFunction instanceof CFunctionDeclaration) {
      heap.addStackFrame((CFunctionDeclaration) mainFunction);
    }

    return new SMGInterpolant(explicitValues, heap, logger, pExternalAllocationSize);
  }

  public static SMGInterpolant getFalseInterpolant() {
    return FALSE;
  }

  @Override
  public String toString() {

    if (isFalse()) {
      return "FALSE";
    } else {
      return heap.toString() + "\n" + explicitValues.toString();
    }
  }
}