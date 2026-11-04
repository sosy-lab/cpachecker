// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.specification;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAstNodeVisitor;

public final class SvLibRecurringTag implements SvLibLivenessTag {

  @Serial private static final long serialVersionUID = -6110539240398436514L;
  private final FileLocation fileLocation;

  public SvLibRecurringTag(FileLocation pFileLocation) {
    fileLocation = pFileLocation;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return ":recurring";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return ":recurring";
  }

  @Override
  public boolean equals(Object pO) {
    return pO instanceof SvLibRecurringTag;
  }

  @Override
  public int hashCode() {
    return SvLibRecurringTag.class.hashCode();
  }
}
