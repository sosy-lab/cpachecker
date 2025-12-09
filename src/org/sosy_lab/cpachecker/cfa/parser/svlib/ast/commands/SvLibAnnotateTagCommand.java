// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;

public final class SvLibAnnotateTagCommand implements SvLibCommand {
  @Serial private static final long serialVersionUID = 5333102692293273124L;

  private final SvLibTagReference tagReference;
  private final ImmutableList<SvLibTagProperty> tags;
  private final FileLocation fileLocation;

  public SvLibAnnotateTagCommand(
      SvLibTagReference pTagReference, List<SvLibTagProperty> pTags, FileLocation pFileLocation) {
    tagReference = pTagReference;
    tags = ImmutableList.copyOf(pTags);
    fileLocation = pFileLocation;
  }

  public SvLibTagReference getTagReference() {
    return tagReference;
  }

  public ImmutableList<SvLibTagProperty> getTags() {
    return tags;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    return "(annotate-tag "
        + tagReference.getTagName()
        + " "
        + Joiner.on(" ").join(FluentIterable.from(tags).transform(SvLibTagProperty::toASTString))
        + ")";
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + tagReference.hashCode();
    result = prime * result + tags.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof SvLibAnnotateTagCommand other
        && tagReference.equals(other.tagReference)
        && tags.equals(other.tags);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibCommandVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}
