// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.pixelexport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GraphStructure implements Iterable<GraphLevel> {
  private List<GraphLevel> levels = new ArrayList<>();
  private int maxWidth;

  public void addLevel(GraphLevel pLevel) {
    levels.add(pLevel);
    int width = pLevel.getWidth();
    maxWidth = Math.max(maxWidth, width);
  }

  @Override
  public Iterator<GraphLevel> iterator() {
    return levels.iterator();
  }

  public int getMaxWidth() {
    return maxWidth;
  }

  public int getDepth() {
    return levels.size();
  }
}
