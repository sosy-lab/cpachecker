// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import java.util.List;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

/**
 * Todo: Docs + Justification for this class!
 */
public class BlockAwareAnalysisContinuationState extends BlockAwareCompositeState {
  BlockAwareAnalysisContinuationState(
      List<AbstractState> elements, Block pBlock, AnalysisDirection pDirection) {
    super(elements, pBlock, pDirection);
  }

  public static ARGState createAndWrap(
      final CompositeState pWrappedState,
      final Block pBlock,
      final AnalysisDirection pDirection,
      @Nullable final ARGState pParent) {
    BlockAwareAnalysisContinuationState blockAwareState =
        new BlockAwareAnalysisContinuationState(pWrappedState.getWrappedStates(), pBlock,
            pDirection); 
    
    return new ARGState(blockAwareState, pParent);
  }

  public static ARGState createAndWrap(
      final CompositeState pWrappedState,
      final Block pBlock,
      final AnalysisDirection pDirection) {
    return createAndWrap(pWrappedState, pBlock, pDirection, null);
  }

  public static ARGState createFromSource(
      final AbstractState pSource,
      final Block pBlock,
      final AnalysisDirection pDirection) {
    assert pSource instanceof ARGState;
    ARGState argState = (ARGState) pSource;
    
    assert argState.getWrappedState() instanceof BlockAwareCompositeState; 
    CompositeState compositeState = (CompositeState) argState.getWrappedState();
    
    return createAndWrap(compositeState, pBlock, pDirection, null);
  }
  
  public boolean isTarget() {
    return false;
  }
}
