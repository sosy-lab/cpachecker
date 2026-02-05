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

/** This class is a representation of Acsl loopInvariant */
public final class AcslLoopInvariant extends AAcslAnnotation {
  private final AcslPredicate predicate;

  public AcslLoopInvariant(FileLocation pFileLocation, AcslPredicate pPredicate) {
    super(pFileLocation);
    predicate = pPredicate;
  }

  @Override
  public String toAstString() {
    return "loop invariant " + predicate.toASTString() + ";";
  }

  public AcslPredicate getPredicate() {
    return predicate;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof AcslLoopInvariant other && predicate.equals(other.predicate);
  }

  @Override
  public int hashCode() {
    return 31 * predicate.hashCode();
  }
}
