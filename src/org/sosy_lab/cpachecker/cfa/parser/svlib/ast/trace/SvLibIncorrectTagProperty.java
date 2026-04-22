// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace;

import com.google.common.base.Joiner;
import java.io.Serial;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAstNode;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;

public final class SvLibIncorrectTagProperty extends SvLibViolatedProperty {
  @Serial private static final long serialVersionUID = 5489687141447266694L;
  private final Optional<SvLibTagReference> svLibTagReference;
  private final Set<SvLibTagProperty> violatedProperties;

  public SvLibIncorrectTagProperty(
      FileLocation pFileLocation,
      SvLibTagReference pSvLibTagReference,
      Set<SvLibTagProperty> pViolatedTerm) {
    super(pFileLocation);
    svLibTagReference = Optional.of(pSvLibTagReference);
    violatedProperties = pViolatedTerm;
  }

  public SvLibIncorrectTagProperty(
      FileLocation pFileLocation, Set<SvLibTagProperty> pViolatedTerm) {
    super(pFileLocation);
    svLibTagReference = Optional.empty();
    violatedProperties = pViolatedTerm;
  }

  public Optional<SvLibTagReference> getSvLibTagReference() {
    return svLibTagReference;
  }

  public Set<SvLibTagProperty> getViolatedProperties() {
    return violatedProperties;
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
    return "(incorrect-annotation "
        + (svLibTagReference.isPresent() ? svLibTagReference.orElseThrow().getTagName() : "")
        + " "
        + Joiner.on(" ").join(violatedProperties.stream().map(SvLibAstNode::toASTString).toList())
        + ")";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + svLibTagReference.hashCode();
    result = prime * result + violatedProperties.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibIncorrectTagProperty other
        && svLibTagReference.equals(other.svLibTagReference)
        && violatedProperties.equals(other.violatedProperties);
  }
}
