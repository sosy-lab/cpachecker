// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.specification;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTermVisitor;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public final class SvLibAtTerm implements SvLibRelationalTerm {

  @Serial private static final long serialVersionUID = 5381549261475877405L;
  private final FileLocation fileLocation;
  private final SvLibTagReference tagReference;
  private final SvLibIdTerm term;

  public SvLibAtTerm(
      FileLocation pFileLocation, SvLibTagReference pTagReference, SvLibIdTerm pTerm) {
    fileLocation = pFileLocation;
    tagReference = pTagReference;
    term = pTerm;
  }

  @Override
  public SvLibType getExpressionType() {
    return term.getExpressionType();
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(at "
        + term.toASTString(pAAstNodeRepresentation)
        + " "
        + tagReference.getTagName()
        + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibTermVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibExpressionVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public int hashCode() {
    return term.hashCode() + 31 * tagReference.hashCode();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibAtTerm other
        && term.equals(other.term)
        && tagReference.equals(other.tagReference);
  }

  public SvLibIdTerm getTerm() {
    return term;
  }

  public SvLibTagReference getTagReference() {
    return tagReference;
  }
}
