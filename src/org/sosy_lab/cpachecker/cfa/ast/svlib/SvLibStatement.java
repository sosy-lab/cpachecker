// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.io.Serial;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract sealed class SvLibStatement implements SvLibAstNode
    permits SvLibCfaEdgeStatement, SvLibControlFlowStatement {

  @Serial private static final long serialVersionUID = -2682818218051235918L;
  private final FileLocation fileLocation;
  private final List<SvLibTagProperty> tagAttributes;
  private final List<SvLibTagReference> tagReferences;

  protected SvLibStatement(
      FileLocation pFileLocation,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences) {
    fileLocation = pFileLocation;
    tagAttributes = pTagAttributes;
    tagReferences = pTagReferences;
  }

  public List<SvLibTagProperty> getTagAttributes() {
    return tagAttributes;
  }

  public @NonNull List<@NonNull SvLibTagReference> getTagReferences() {
    return tagReferences;
  }

  public abstract <R, X extends Exception> R accept(SvLibStatementVisitor<R, X> v) throws X;

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    return pOther instanceof SvLibStatement other
        && tagAttributes.equals(other.tagAttributes)
        && tagReferences.equals(other.tagReferences);
  }

  @Override
  public int hashCode() {
    return 31 * tagAttributes.hashCode() + tagReferences.hashCode();
  }
}
