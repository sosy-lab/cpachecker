// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import java.util.HashMap;
import java.util.HashSet;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ValueAnalysisSummary {

  private Block block;
  private final ValueAnalysisState entryValueState;
  private final ValueAnalysisState exitValueState;
  private final VariableTrackingPrecision precision;

  public ValueAnalysisSummary(
      Block pBlock,
      ValueAnalysisState pEntryValueState,
      ValueAnalysisState pExitValueState,
      VariableTrackingPrecision pPrecision) {
    block = pBlock;
    entryValueState = pEntryValueState;
    exitValueState = pExitValueState;
    precision = pPrecision;
  }

  public ValueAnalysisSummary(
      ValueAnalysisState pEntryValueState, ValueAnalysisState pExitValueState, VariableTrackingPrecision pPrecision) {
    entryValueState = pEntryValueState;
    exitValueState = pExitValueState;
    precision = pPrecision;
  }

  public ValueAnalysisState getEntryState() {
    return entryValueState;
  }

  public ValueAnalysisState getExitState() {
    return exitValueState;
  }

  public Block getBlock() {
    return block;
  }

  public void setBlock(Block pBlock) {
    block = pBlock;
  }

  public VariableTrackingPrecision getPrecision() {
    return precision;
  }

  public void assignTypes(HashMap<MemoryLocation, Type> pLocationToType) {
    var trackedLocations = new HashSet<>(entryValueState.getTrackedMemoryLocations());
    trackedLocations.addAll(exitValueState.getTrackedMemoryLocations());
    for (var trackedLocation : trackedLocations) {
      if (pLocationToType.containsKey(trackedLocation)) {
        assignType(trackedLocation, pLocationToType.get(trackedLocation));
      }
    }
    if (block.getCallNode() instanceof FunctionEntryNode entryNode && entryNode.getReturnVariable().isPresent()) {
      var returnVar = entryNode.getReturnVariable().orElseThrow();
      var location = MemoryLocation.fromQualifiedName(returnVar.getQualifiedName());
      var type = returnVar.getType();
      assignType(location, type);
    }
  }

  private void assignType(MemoryLocation pLocation, Type pType) {
    if (entryValueState.contains(pLocation)) {
      var value = entryValueState.getValueFor(pLocation);
      entryValueState.assignConstant(pLocation, value, pType);
    }
    if (exitValueState.contains(pLocation)) {
      var value = exitValueState.getValueFor(pLocation);
      exitValueState.assignConstant(pLocation, value, pType);
    }
  }

  public ValueAnalysisState applyToState(ValueAnalysisState pEntryState) {
    var state = ValueAnalysisState.copyOf(pEntryState);
    for (var variable : block.getVariables()) {
      var location = MemoryLocation.fromQualifiedName(variable);
      state.forget(location);
    }

    for (var x : exitValueState.getConstants()) {
      var location = x.getKey();
      var value = x.getValue().getValue();
      var type = x.getValue().getType();
      state.assignConstant(location, value, type);
    }
    return state;
  }

  @Override
  public String toString() {
    CFANode EntryNode = block.getCallNode();
    String str = EntryNode.getFunctionName();

    if (!(EntryNode instanceof FunctionEntryNode)) {
      str += " " + EntryNode.getLeavingEdge(0).getLineNumber();
    }

    str += "\n" + precision.toString();
    str += "\n" + entryValueState.toDOTLabel() + "\n";
    str += exitValueState.toDOTLabel() + "\n";

    return str;
  }
}
