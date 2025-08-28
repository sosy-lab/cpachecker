// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class LocalVariableDeclarationSubstitute {

  // TODO what if multiple declarations have no call context - duplicate key in map?
  /** Not every local variable declaration has a calling context, hence {@link Optional}s. */
  public final ImmutableMap<Optional<ThreadEdge>, CIdExpression> substitutes;

  private final Optional<MPORSubstitutionTracker> tracker;

  public LocalVariableDeclarationSubstitute(
      ImmutableMap<Optional<ThreadEdge>, CIdExpression> pSubstitutes,
      Optional<MPORSubstitutionTracker> pTracker) {

    substitutes = pSubstitutes;
    tracker = pTracker;
  }

  public boolean isTrackerPresent() {
    return tracker.isPresent();
  }

  public MPORSubstitutionTracker getTracker() {
    assert tracker.isPresent() : "cannot get tracker, tracker is not present";
    return tracker.orElseThrow();
  }
}
