// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract sealed class AAcslClause
    permits AAcslSimpleClause, AcslDecreasesClause, AcslRequiresClause, AcslTerminatesClause {
  private final FileLocation fileLocation;

  protected AAcslClause(FileLocation pFileLocation) {
    fileLocation = pFileLocation;
  }

  public FileLocation getFileLocation() {
    return fileLocation;
  }
}
