// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;

public final class AcslDecreasesClause extends AAcslClause {

  private final AcslTerm term;
  @Nullable private final String id;

  private AcslDecreasesClause(FileLocation pFileLocation, AcslTerm pTerm, @Nullable String pId) {
    super(pFileLocation);
    term = pTerm;
    id = pId;
  }

  public AcslTerm getTerm() {
    return term;
  }

  public @Nullable String getId() {
    return id;
  }
}
