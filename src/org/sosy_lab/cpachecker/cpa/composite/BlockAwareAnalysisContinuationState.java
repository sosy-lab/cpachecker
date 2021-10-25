// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

/** Todo: Docs + Justification for this class! */
public class BlockAwareAnalysisContinuationState extends BlockAwareARGState {
  BlockAwareAnalysisContinuationState(
      ARGState pState, Block pBlock, AnalysisDirection pDirection) {
    super(pState, pBlock, pDirection);
  }

  public static BlockAwareAnalysisContinuationState create(
      ARGState pState, Block pBlock, AnalysisDirection pDirection) {
    return new BlockAwareAnalysisContinuationState(pState, pBlock, pDirection);
  }

  public boolean isTarget() {
    return false;
  }
}
