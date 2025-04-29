// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslStringLiteralTerm extends AcslLiteralTerm {

  @Serial private static final long serialVersionUID = -81455024380987676L;

  private final String value;

  public AcslStringLiteralTerm(FileLocation pFileLocation, AcslType pType, String pValue) {
    super(pFileLocation, pType);
    value = pValue;
    checkNotNull(pFileLocation);
    checkNotNull(pType);
    checkNotNull(pValue);
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public <R, X extends Exception> R accept(AcslTermVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}
