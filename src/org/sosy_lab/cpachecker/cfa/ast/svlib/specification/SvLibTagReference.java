// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.specification;

import java.io.Serial;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAstNodeVisitor;

public final class SvLibTagReference implements SvLibTagAttribute {
  @Serial private static final long serialVersionUID = 7437989844963398076L;
  private final String tagName;
  private final FileLocation fileLocation;

  public SvLibTagReference(String pTagName, FileLocation pFileLocation) {
    tagName = pTagName;
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
    return ":tag " + tagName;
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return ":tag " + tagName;
  }

  public @NonNull String getTagName() {
    return tagName;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof SvLibTagReference other && tagName.equals(other.tagName);
  }

  @Override
  public int hashCode() {
    return tagName.hashCode();
  }
}
