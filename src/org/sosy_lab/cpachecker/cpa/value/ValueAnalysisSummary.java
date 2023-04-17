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
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ValueAnalysisSummary {

  private Block block;
  private final ValueAnalysisState EntryValueState;
  private final ValueAnalysisState ExitValueState;

  public ValueAnalysisSummary(
      Block pBlock, ValueAnalysisState pEntryValueState, ValueAnalysisState pExitValueState) {
    block = pBlock;
    EntryValueState = pEntryValueState;
    ExitValueState = pExitValueState;
  }

  public ValueAnalysisSummary(
      ValueAnalysisState pEntryValueState, ValueAnalysisState pExitValueState) {
    EntryValueState = pEntryValueState;
    ExitValueState = pExitValueState;
  }

  public ValueAnalysisState getEntryState() {
    return EntryValueState;
  }

  public ValueAnalysisState getExitState() {
    return ExitValueState;
  }

  public Block getBlock() {
    return block;
  }

  public void setBlock(Block pBlock) {
    block = pBlock;
  }

  public ValueAnalysisState applyToState(ValueAnalysisState pEntryState) {
    var state = ValueAnalysisState.copyOf(pEntryState);
    for (var variable : block.getVariables()) {
      var memLoc = MemoryLocation.fromQualifiedName(variable);
      state.forget(memLoc);
    }

    for (var x : ExitValueState.getConstants()) {
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

    str += "\n" + EntryValueState.toDOTLabel() + "\n";
    str += ExitValueState.toDOTLabel() + "\n";

    return str;
  }
}
