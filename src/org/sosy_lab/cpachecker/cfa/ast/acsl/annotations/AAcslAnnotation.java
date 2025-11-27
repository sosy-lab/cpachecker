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

public abstract sealed class AAcslAnnotation permits AcslAssertion {

  private final FileLocation fileLocation;

  protected AAcslAnnotation(FileLocation pFileLocation) {
    fileLocation = pFileLocation;
  }

  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public boolean equals(Object pO) {

    return pO instanceof AAcslAnnotation other && fileLocation.equals(other.fileLocation);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(fileLocation);
  }

  abstract String toAstString();
}
