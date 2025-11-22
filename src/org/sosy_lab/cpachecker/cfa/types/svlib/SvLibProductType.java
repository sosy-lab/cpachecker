// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.svlib;

import com.google.common.collect.ImmutableList;
import java.io.Serial;

/**
 * A product type in SV-LIB, representing a tuple of types, should only ever be used for {@link
 * org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTermTuple} and similar classes which are classes
 * internal to CPAchecker. In particular this type should never appear on a {@link
 * org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm}.
 */
public final class SvLibProductType implements SvLibType {

  @Serial private static final long serialVersionUID = 265032494258079236L;
  private final ImmutableList<SvLibType> elementTypes;

  public SvLibProductType(ImmutableList<SvLibType> pElementTypes) {
    elementTypes = ImmutableList.copyOf(pElementTypes);
  }

  @Override
  public String toASTString(String declarator) {
    return declarator
        + " ["
        + String.join(", ", elementTypes.stream().map(SvLibType::toString).toList())
        + "]";
  }

  @Override
  public String toASTString() {
    return "[" + String.join(", ", elementTypes.stream().map(SvLibType::toString).toList()) + "]";
  }

  public ImmutableList<SvLibType> getElementTypes() {
    return elementTypes;
  }

  @Override
  public int hashCode() {
    return elementTypes.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibProductType other && elementTypes.equals(other.elementTypes);
  }
}
