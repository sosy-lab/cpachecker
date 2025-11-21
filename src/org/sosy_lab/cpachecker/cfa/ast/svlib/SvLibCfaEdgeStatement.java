// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;

public abstract sealed class SvLibCfaEdgeStatement implements AStatement {
  @Serial private static final long serialVersionUID = 5250154309306501123L;

  private final FileLocation fileLocation;
  private final ImmutableList<SvLibTagProperty> tagAttributes;
  private final ImmutableList<SvLibTagReference> tagReferences;

  protected SvLibCfaEdgeStatement(
      FileLocation pFileLocation,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences) {
    fileLocation = pFileLocation;
    tagAttributes = ImmutableList.copyOf(pTagAttributes);
    tagReferences = ImmutableList.copyOf(pTagReferences);
  }

  public abstract <R, X extends Exception> R accept(SvLibCfaEdgeStatementVisitor<R, X> v) throws X;

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibCfaEdgeStatement && super.equals(pO);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
