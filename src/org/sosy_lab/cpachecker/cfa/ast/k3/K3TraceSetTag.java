// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import com.google.common.base.Joiner;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3TraceSetTag extends K3SelectTraceComponent {
  @Serial private static final long serialVersionUID = 5489687141447266694L;
  private final String tagName;
  private final List<K3TagProperty> attributes;

  public K3TraceSetTag(
      FileLocation pFileLocation, String pTagName, List<K3TagProperty> pAttributes) {
    super(pFileLocation);
    tagName = pTagName;
    attributes = pAttributes;
  }

  @Override
  <R, X extends Exception> R accept(K3TraceElementVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(set-tag "
        + tagName
        + " "
        + Joiner.on(" ").join(attributes.stream().map(K3AstNode::toASTString).toList())
        + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  public String getTagName() {
    return tagName;
  }

  public List<K3TagProperty> getAttributes() {
    return attributes;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + tagName.hashCode();
    result = prime * result + attributes.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3TraceSetTag other
        && tagName.equals(other.tagName)
        && attributes.equals(other.attributes);
  }
}
