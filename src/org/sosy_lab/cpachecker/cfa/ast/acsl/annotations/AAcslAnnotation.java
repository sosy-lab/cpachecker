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
 */
public abstract sealed class AAcslAnnotation
    permits AcslAssertion,
        AcslAssigns,
        AcslEnsures,
        AcslFunctionContract,
        AcslLoopInvariant,
        AcslRequires {

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
