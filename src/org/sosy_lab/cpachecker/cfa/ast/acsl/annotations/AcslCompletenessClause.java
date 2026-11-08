// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * A clause that states that the given behaviors (or all behaviors, if none are given) are complete
 * or disjoint.
 */
public final class AcslCompletenessClause extends AAcslAnnotation {

  public enum Kind {
    COMPLETE,
    DISJOINT
  }

  private final Kind kind;
  private final ImmutableList<String> behaviors;

  public AcslCompletenessClause(
      FileLocation pFileLocation, Kind pKind, ImmutableList<String> pBehaviors) {
    super(pFileLocation);
    kind = checkNotNull(pKind);
    behaviors = checkNotNull(pBehaviors);
  }

  public Kind getKind() {
    return kind;
  }

  public ImmutableList<String> getBehaviors() {
    return behaviors;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof AcslCompletenessClause other
        && kind == other.kind
        && behaviors.equals(other.behaviors);
  }

  @Override
  public int hashCode() {
    return 31 * kind.hashCode() + behaviors.hashCode();
  }

  @Override
  public String toAstString() {
    return (kind == Kind.COMPLETE ? "complete" : "disjoint")
        + " behaviors"
        + (behaviors.isEmpty() ? "" : " " + Joiner.on(", ").join(behaviors))
        + ";";
  }
}
