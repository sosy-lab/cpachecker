// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition;

import java.util.Objects;

class AlgSelectionBooleanVector {

  private final boolean hasAliasing;
  private final boolean hasArray;
  private final boolean hasComposite;
  private final boolean hasFloat;
  private final boolean hasLoop;
  private final boolean hasSingleLoop;

  private AlgSelectionBooleanVector(boolean hasAliasing, boolean hasArray, boolean hasComposite,
      boolean hasFloat, boolean hasLoop, boolean hasSingleLoop) {
    this.hasAliasing = hasAliasing;
    this.hasArray = hasArray;
    this.hasComposite = hasComposite;
    this.hasFloat = hasFloat;
    this.hasLoop = hasLoop;
    this.hasSingleLoop = hasSingleLoop;
  }

  static AlgSelectionBooleanVector init(boolean hasAliasing, boolean hasArray, boolean hasComposite,
      boolean hasFloat, boolean hasLoop, boolean hasSingleLoop) {
    return new AlgSelectionBooleanVector(hasAliasing, hasArray, hasComposite, hasFloat, hasLoop,
        hasSingleLoop);
  }

  static AlgSelectionBooleanVector init(int hasAliasing, int hasArray, int hasComposite,
      int hasFloat, int hasLoop, int hasSingleLoop) {
    return new AlgSelectionBooleanVector(hasAliasing == 1, hasArray == 1, hasComposite == 1,
        hasFloat == 1, hasLoop == 1, hasSingleLoop == 1);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof AlgSelectionBooleanVector that)) {
      return false;
    }
    return Objects.hash(hasAliasing, hasArray, hasComposite, hasFloat, hasLoop, hasSingleLoop) ==
        Objects.hash(that.hasAliasing, that.hasArray, that.hasComposite, that.hasFloat, that.hasLoop, that.hasSingleLoop);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hasAliasing, hasArray, hasComposite, hasFloat, hasLoop, hasSingleLoop);
  }
}
