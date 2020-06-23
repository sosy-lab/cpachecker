// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.pixelexport;

import com.google.common.primitives.ImmutableIntArray;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.awt.Color;
import java.util.Collection;
import org.sosy_lab.cpachecker.util.Pair;

public interface GraphLevel {

  Color getBackgroundColor();

  int getWidth();

  Collection<Pair<ImmutableIntArray, Color>> getGroups();

  interface Builder<NodeType> {
    @CanIgnoreReturnValue
    Builder<NodeType> node();

    @CanIgnoreReturnValue
    Builder<NodeType> addMarkings(NodeType node);

    GraphLevel build();
  }
}
