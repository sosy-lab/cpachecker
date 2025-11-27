// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;

public final class AcslLoopInvariant extends AAcslAnnotation {
  private final AcslPredicate predicate;

  public AcslLoopInvariant(FileLocation pFileLocation, AcslPredicate pPredicate) {
    super(pFileLocation);
    predicate = pPredicate;
  }

  @Override
  String toAstString() {
    return "loop invariant " + predicate.toASTString() + ";";
  }

  public AcslPredicate getPredicate() {
    return predicate;
  }

  @Override
  public boolean equals(Object pO) {
    if (!(pO instanceof AcslLoopInvariant that)) return false;
    if (!super.equals(pO)) return false;

    return predicate.equals(that.predicate);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + predicate.hashCode();
    return result;
  }
}
