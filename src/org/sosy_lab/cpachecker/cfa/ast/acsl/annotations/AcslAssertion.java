// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;

/** This class is a reperesentation of Acsl assertion annotations. */
public final class AcslAssertion extends AAcslAnnotation {

  private final AcslPredicate predicate;

  public AcslAssertion(FileLocation pFileLocation, AcslPredicate pPredicate) {
    super(pFileLocation);
    predicate = pPredicate;
  }

  public AcslPredicate getPredicate() {
    return predicate;
  }

  @Override
  public String toAstString() {
    return "assert " + predicate.toASTString() + ";";
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof AcslAssertion other && predicate.equals(other.predicate);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    int prime = 31;
    hash = prime * hash * Objects.hashCode(predicate);
    return hash;
  }
}
