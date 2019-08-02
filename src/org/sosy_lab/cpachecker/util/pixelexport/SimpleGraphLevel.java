/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.pixelexport;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.awt.Color;
import java.util.Collection;
import java.util.List;
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
  public Collection<Pair<List<Integer>, Color>> getGroups() {
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
