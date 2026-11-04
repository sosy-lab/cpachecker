// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import com.google.common.collect.ImmutableMap;
import java.io.Serial;
import java.util.Map;
import java.util.Map.Entry;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibArrayType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

/**
 * The value of an array as SMT-LIB solvers write it in a model: a constant array with stores,
 * {@code (store ((as const (Array Int Int)) 0) 1 5)}.
 */
public final class SvLibArrayConstantTerm implements SvLibConstantTerm {

  @Serial private static final long serialVersionUID = -3415262519284187720L;
  private final SvLibSmtLibArrayType type;
  private final SvLibConstantTerm defaultElement;
  private final ImmutableMap<SvLibConstantTerm, SvLibConstantTerm> elements;
  private final FileLocation fileLocation;

  public SvLibArrayConstantTerm(
      SvLibSmtLibArrayType pType,
      SvLibConstantTerm pDefaultElement,
      Map<SvLibConstantTerm, SvLibConstantTerm> pElements,
      FileLocation pFileLocation) {
    type = pType;
    defaultElement = pDefaultElement;
    elements = ImmutableMap.copyOf(pElements);
    fileLocation = pFileLocation;
  }

  /** The elements at the indices that do not have the default element. */
  @Override
  public ImmutableMap<SvLibConstantTerm, SvLibConstantTerm> getValue() {
    return elements;
  }

  public SvLibConstantTerm getDefaultElement() {
    return defaultElement;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibTermVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public @NonNull SvLibType getExpressionType() {
    return type;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibExpressionVisitor<R, X> v) throws X {
    return v.accept(this);
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
    String array =
        "((as const "
            + type.toASTString()
            + ") "
            + defaultElement.toASTString(pAAstNodeRepresentation)
            + ")";
    for (Entry<SvLibConstantTerm, SvLibConstantTerm> element : elements.entrySet()) {
      array =
          "(store "
              + array
              + " "
              + element.getKey().toASTString(pAAstNodeRepresentation)
              + " "
              + element.getValue().toASTString(pAAstNodeRepresentation)
              + ")";
    }
    return array;
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  @Override
  public int hashCode() {
    return (type.hashCode() * 31 + defaultElement.hashCode()) * 31 + elements.hashCode();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibArrayConstantTerm other
        && type.equals(other.type)
        && defaultElement.equals(other.defaultElement)
        && elements.equals(other.elements);
  }
}
