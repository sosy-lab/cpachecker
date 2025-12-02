// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAstNode;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;

public final class SvLibTraceUsingAnnotation extends SvLibTraceComponent {
  @Serial private static final long serialVersionUID = 5489687141447266694L;
  private final String tagName;
  private final ImmutableList<SvLibTagProperty> attributes;

  public SvLibTraceUsingAnnotation(
      FileLocation pFileLocation, String pTagName, List<SvLibTagProperty> pAttributes) {
    super(pFileLocation);
    tagName = pTagName;
    attributes = ImmutableList.copyOf(pAttributes);
  }

  @Override
  <R, X extends Exception> R accept(SvLibTraceComponentVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public String toASTString() {
    return "(using-annotation "
        + tagName
        + " "
        + Joiner.on(" ").join(attributes.stream().map(SvLibAstNode::toASTString).toList())
        + ")";
  }

  public String getTagName() {
    return tagName;
  }

  public ImmutableList<SvLibTagProperty> getAttributes() {
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

    return obj instanceof SvLibTraceUsingAnnotation other
        && tagName.equals(other.tagName)
        && attributes.equals(other.attributes);
  }
}
