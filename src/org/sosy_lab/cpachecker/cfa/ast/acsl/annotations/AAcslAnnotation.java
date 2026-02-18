// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * This abstract class represents the different types of Acsl Annotations that can occur within an
 * annotated C program.
 *
 * <p>An Acsl Assertion can be: A single assertion, a loop annotation or a function contract.
 * (§2.4.1 Acsl standard v 1.23)
 *
 * <p>A loop annotation can have an arbirtary number of loop invariants and loop-assigns. A loop
 * invariant is a loop-clause. (§2.4.2 Acsl standard v 1.23)
 *
 * <p>A function contract can have an arbitray number of ensures, assigns and requires clauses.
 * Assigns and requires clauses are both simple clauses. (§2.3 Acsl standard v 1.23)
 */
public abstract sealed class AAcslAnnotation
    permits AcslAssertion,
        AcslFunctionContract,
        AcslLoopAnnotation,
        AcslLoopInvariant,
        AcslRequires,
        AcslEnsures,
        AcslAssigns {

  private final FileLocation fileLocation;

  protected AAcslAnnotation(FileLocation pFileLocation) {
    fileLocation = pFileLocation;
  }

  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public abstract boolean equals(Object pO);

  @Override
  public abstract int hashCode();

  public abstract String toAstString();
}
