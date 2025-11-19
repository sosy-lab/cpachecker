// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements;

import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibCfaEdgeStatementVisitor;
import org.sosy_lab.cpachecker.core.specification.svlib.ast.SvLibTagProperty;
import org.sosy_lab.cpachecker.core.specification.svlib.ast.SvLibTagReference;

public abstract non-sealed class SvLibCfaEdgeStatement extends SvLibStatement
    implements AStatement {
  @Serial private static final long serialVersionUID = 5250154309306501123L;

  protected SvLibCfaEdgeStatement(
      FileLocation pFileLocation,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences) {
    super(pFileLocation, pTagAttributes, pTagReferences);
  }

  public <R, X extends Exception> R accept(SvLibCfaEdgeStatementVisitor<R, X> v) throws X {
    return accept((SvLibStatementVisitor<R, X>) v);
  }

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
