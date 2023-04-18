// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ValueAnalysisSummary {

  private Block block;
  private final ValueAnalysisState entryValueState;
  private final ValueAnalysisState exitValueState;

  public ValueAnalysisSummary(
      Block pBlock, ValueAnalysisState pEntryValueState, ValueAnalysisState pExitValueState) {
    block = pBlock;
    entryValueState = pEntryValueState;
    exitValueState = pExitValueState;
  }

  public ValueAnalysisSummary(
      ValueAnalysisState pEntryValueState, ValueAnalysisState pExitValueState) {
    entryValueState = pEntryValueState;
    exitValueState = pExitValueState;
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

    // also add the type of the parameters and return var to the summary
    if (pBlock.getCallNode() instanceof FunctionEntryNode pEntryNode) {
      for (var parameter : pEntryNode.getFunction().getParameters()) {
        var location = MemoryLocation.fromQualifiedName(parameter.getQualifiedName());
        var type = parameter.getType();
        assignType(location, type);
      }

      if (pEntryNode.getReturnVariable().isPresent()) {
        var returnVar = pEntryNode.getReturnVariable().get();
        var returnVarLocation = MemoryLocation.fromQualifiedName(returnVar.getQualifiedName());
        var returnType = returnVar.getType();
        assignType(returnVarLocation, returnType);
      }
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

  public boolean isEntryState(ValueAnalysisState state) {
    if (state.getSize() != entryValueState.getSize())
      return false;

    // add type information, if still missing
    for (var x : entryValueState.getConstants()) {
      var location = x.getKey();
      var type = x.getValue().getType();
      if (type == null && state.contains(location)) {
        type = state.getTypeForMemoryLocation(location);
        assignType(location, type);
      }
    }

    return entryValueState.equals(state);
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

    str += "\n" + entryValueState.toDOTLabel() + "\n";
    str += exitValueState.toDOTLabel() + "\n";

    return str;
  }
}
