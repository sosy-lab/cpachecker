// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import static org.sosy_lab.cpachecker.core.AnalysisDirection.BACKWARD;
import static org.sosy_lab.cpachecker.core.AnalysisDirection.FORWARD;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.SimpleTargetInformation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

/**
 * Todo: Docs + Justification for inheritance instead of composition!
 */
public class BlockAwareCompositeState extends CompositeState {
  private static final long serialVersionUID = -7445431074342419279L;
  private final Block block;

  private final AnalysisDirection direction;

  BlockAwareCompositeState(
      final List<AbstractState> elements, final Block pBlock, final AnalysisDirection pDirection) {
    super(elements);
    block = pBlock;
    direction = pDirection;
  }

  public static BlockAwareCompositeState create(
      final CompositeState pWrappedState,
      final Block pBlock,
      final AnalysisDirection pDirection) {
    return new BlockAwareCompositeState(pWrappedState.getWrappedStates(), pBlock, pDirection);
  }
  
  public static ARGState createAndWrap(
      final List<AbstractState> pComponentCPAs,
      final Block pBlock,
      final AnalysisDirection pDirection) {
    return new ARGState(new BlockAwareCompositeState(pComponentCPAs, pBlock, pDirection), null);
  }

  public static ARGState createAndWrap(
      final CompositeState pWrappedState, final Block pBlock, final AnalysisDirection pDirection) {
    return createAndWrap(pWrappedState, pBlock, pDirection, null);
  }

  public static ARGState createAndWrap(
      final CompositeState pWrappedState,
      final Block pBlock,
      final AnalysisDirection pDirection,
      @Nullable final ARGState pParent) {
    BlockAwareCompositeState blockAwareState =
        new BlockAwareCompositeState(pWrappedState.getWrappedStates(), pBlock, pDirection);

    return new ARGState(blockAwareState, pParent);
  }

  @Override
  public boolean isTarget() {
    boolean defaultTarget = super.isTarget();

    final CFANode location = extractLocation(this);
    if (location == null) {
      return defaultTarget;
    }

    if (direction == BACKWARD && block.getEntry() == location) {
      return true;
    }

    boolean programExit = block.getExits().isEmpty() && location.getNumLeavingEdges() == 0;
    if (direction == FORWARD && block.getExits().containsKey(location) || programExit) {
      return true;
    }

    return defaultTarget;
  }

  @Override
  public @NonNull Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    final CFANode location = extractLocation(this);
    if (location == null) {
      return super.getTargetInformation();
    }

    if (location == block.getEntry()) {
      return Set.of(SimpleTargetInformation.create("Analysis reached block entry"));
    } else if (block.getExits().containsKey(location)) {
      return Set.of(SimpleTargetInformation.create("Analysis reached block exit"));
    }

    return super.getTargetInformation();
  }
}
