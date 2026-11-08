// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;

/** The assumes clause of a behavior, which states when the behavior applies. */
public final class AcslAssumes extends AAcslAnnotation {
  private final AcslPredicate predicate;

  public AcslAssumes(FileLocation pFileLocation, AcslPredicate pPredicate) {
    super(pFileLocation);
    predicate = checkNotNull(pPredicate);
  }

  public AcslPredicate getPredicate() {
    return predicate;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof AcslAssumes other && predicate.equals(other.predicate);
  }

  @Override
  public int hashCode() {
    return 31 * predicate.hashCode() + 3;
  }

  @Override
  public String toAstString() {
    return "assumes " + predicate.toASTString() + ";";
  }
}
