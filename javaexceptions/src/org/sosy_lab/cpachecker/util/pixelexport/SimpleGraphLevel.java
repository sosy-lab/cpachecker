// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.pixelexport;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableIntArray;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.awt.Color;
import java.util.Collection;
import org.sosy_lab.cpachecker.util.Pair;

public class SimpleGraphLevel implements GraphLevel {

  private final int width;

  public SimpleGraphLevel(final int pWidth) {
    checkArgument(pWidth >= 0);
    width = pWidth;
  }

  @Override
  public Color getBackgroundColor() {
    return GraphToPixelsWriter.COLOR_BACKGROUND;
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public Collection<Pair<ImmutableIntArray, Color>> getGroups() {
    return ImmutableList.of();
  }

  public static class Builder<NodeType> implements GraphLevel.Builder<NodeType> {

    private int width = 0;

    @Override
    public SimpleGraphLevel build() {
      return new SimpleGraphLevel(width);
    }

    @Override
    @CanIgnoreReturnValue
    public Builder<NodeType> node() {
      width++;
      return this;
    }

    @Override
    @CanIgnoreReturnValue
    public Builder<NodeType> addMarkings(NodeType pNode) {
      // nothing needs to be done
      return this;
    }
  }
}
