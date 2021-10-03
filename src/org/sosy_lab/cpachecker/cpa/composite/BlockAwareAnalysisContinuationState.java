// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/** Todo: Docs + Justification for this class! */
public class BlockAwareAnalysisContinuationState extends BlockAwareCompositeState {
  BlockAwareAnalysisContinuationState(
      List<AbstractState> elements, Block pBlock, AnalysisDirection pDirection) {
    super(elements, pBlock, pDirection);
  }

  public static BlockAwareAnalysisContinuationState create(
      List<AbstractState> elements, Block pBlock, AnalysisDirection pDirection) {
    return new BlockAwareAnalysisContinuationState(elements, pBlock, pDirection);
  }

  public boolean isTarget() {
    return false;
  }
}
