// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.primitives.ImmutableIntArray;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.pixelexport.GraphLevel;
import org.sosy_lab.cpachecker.util.pixelexport.GraphToPixelsWriter;
import org.sosy_lab.cpachecker.util.pixelexport.SimpleGraphLevel;

/**
 * Class for creating a pixel graphic from an {@link org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet}.
 *
 * <p>There are different options for modifying how the pixel graphic looks like.
 *
 * <p>An ARG is represented as a tree structure. The depth of an ARG node in the tree equals its
 * shortest distance from the root of the ARG.
 */
public class ARGToPixelsWriter extends GraphToPixelsWriter<ARGState> {

  private static final Color COLOR_TARGET = Color.RED;
  private static final Color COLOR_HIGHLIGHT = Color.BLUE;
  private static final Color COLOR_NOTEXPANDED = Color.ORANGE;
  private static final Color COLOR_COVERED = Color.GREEN;

  public ARGToPixelsWriter(Configuration pConfig) throws InvalidConfigurationException {
    super(pConfig);
  }

  @Override
  public ARGLevel.Builder getLevelBuilder() {
    return ARGLevel.builder();
  }

  @Override
  public Iterable<ARGState> getChildren(final ARGState pParent) {
    return pParent.getChildren();
  }

  private static class ARGLevel extends SimpleGraphLevel {

    private final ImmutableIntArray targetIndices;
    private final ImmutableIntArray highlightIndices;
    private final ImmutableIntArray notExpandedIndices;
    private final ImmutableIntArray coveredIndices;

    private ARGLevel(
        int pWidth,
        ImmutableIntArray pTargets,
        ImmutableIntArray pNotExpanded,
        ImmutableIntArray pHighlights,
        ImmutableIntArray pCovered) {
      super(pWidth);
      targetIndices = checkNotNull(pTargets);
      notExpandedIndices = checkNotNull(pNotExpanded);
      highlightIndices = checkNotNull(pHighlights);
      coveredIndices = checkNotNull(pCovered);
    }

    @Override
    public Collection<Pair<ImmutableIntArray, Color>> getGroups() {
      Deque<Pair<ImmutableIntArray, Color>> groups = new ArrayDeque<>(4);
      if (!highlightIndices.isEmpty()) {
        groups.add(Pair.of(highlightIndices, COLOR_HIGHLIGHT));
      }
      if (!notExpandedIndices.isEmpty()) {
        groups.add(Pair.of(notExpandedIndices, COLOR_NOTEXPANDED));
      }
      if (!targetIndices.isEmpty()) {
        groups.add(Pair.of(targetIndices, COLOR_TARGET));
      }
      if (!coveredIndices.isEmpty()) {
        groups.add(Pair.of(coveredIndices, COLOR_COVERED));
      }

      return groups;
    }

    @Override
    public Color getBackgroundColor() {
      Color color;
      if (!targetIndices.isEmpty()) {
        color = COLOR_TARGET;
      } else if (!highlightIndices.isEmpty()) {
        color = COLOR_HIGHLIGHT;
      } else if (notExpandedIndices.length() > coveredIndices.length()) {
        color = COLOR_NOTEXPANDED;
      } else if (!coveredIndices.isEmpty()) {
        color = COLOR_COVERED;
      } else {
        return COLOR_BACKGROUND;
      }

      return new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
    }

    static Builder builder() {
      return new Builder();
    }

    public static class Builder implements GraphLevel.Builder<ARGState> {

      private int width = 0;
      private final ImmutableIntArray.Builder targets = ImmutableIntArray.builder();
      private final ImmutableIntArray.Builder notExpanded = ImmutableIntArray.builder();
      private final ImmutableIntArray.Builder highlights = ImmutableIntArray.builder();
      private final ImmutableIntArray.Builder covered = ImmutableIntArray.builder();

      @Override
      public ARGLevel build() {
        return new ARGLevel(
            width, targets.build(), notExpanded.build(), highlights.build(), covered.build());
      }

      @CanIgnoreReturnValue
      @Override
      public Builder addMarkings(ARGState pNode) {

        if (pNode.isTarget()) {
          targets.add(width);
        }

        if (!pNode.wasExpanded()) {
          notExpanded.add(width);
        }

        if (pNode.shouldBeHighlighted()) {
          highlights.add(width);
        }

        if (pNode.isCovered()) {
          covered.add(width);
        }
        return this;
      }

      @CanIgnoreReturnValue
      @Override
      public Builder node() {
        width++;
        return this;
      }
    }
  }
}
