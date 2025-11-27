// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslRequires extends AAcslAnnotation {
  private AcslRequires(FileLocation pFileLocation) {
    super(pFileLocation);
  }

  @Override
  public boolean equals(Object pO) {
    return false;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  String toAstString() {
    return "";
  }
}
