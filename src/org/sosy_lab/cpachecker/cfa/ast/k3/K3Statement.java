// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.io.Serial;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract sealed class K3Statement implements K3AstNode
    permits K3ControlFlowStatement, K3CfaEdgeStatement {

  @Serial private static final long serialVersionUID = -2682818218051235918L;
  private final FileLocation fileLocation;
  private final List<K3TagProperty> tagAttributes;
  private final List<K3TagReference> tagReferences;

  protected K3Statement(
      FileLocation pFileLocation,
      List<K3TagProperty> pTagAttributes,
      List<K3TagReference> pTagReferences) {
    fileLocation = pFileLocation;
    tagAttributes = pTagAttributes;
    tagReferences = pTagReferences;
  }

  public List<K3TagProperty> getTagAttributes() {
    return tagAttributes;
  }

  public @NonNull List<@NonNull K3TagReference> getTagReferences() {
    return tagReferences;
  }

  public abstract <R, X extends Exception> R accept(K3StatementVisitor<R, X> v) throws X;

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    return pOther instanceof K3Statement other
        && tagAttributes.equals(other.tagAttributes)
        && tagReferences.equals(other.tagReferences);
  }

  @Override
  public int hashCode() {
    return 31 * tagAttributes.hashCode() + tagReferences.hashCode();
  }
}
