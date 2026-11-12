// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.CharMatcher;
import java.io.Serial;
import java.math.BigDecimal;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslType;

public final class AcslRealLiteralTerm extends AcslLiteralTerm {

  @Serial private static final long serialVersionUID = -9116381648021444709L;

  private final BigDecimal value;

  public AcslRealLiteralTerm(FileLocation pFileLocation, AcslType pType, BigDecimal pValue) {
    super(checkNotNull(pFileLocation), checkNotNull(pType));
    checkArgument(
        AcslType.isNumeric(pType) && !AcslType.isIntegral(pType),
        "A real literal has a real type, not %s",
        pType);
    value = checkNotNull(pValue);
  }

  @Override
  public BigDecimal getValue() {
    return value;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    String text = value.toString();
    // Without a point or an exponent, the constant would be an integer
    return CharMatcher.anyOf(".E").matchesAnyOf(text) ? text : text + ".";
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
