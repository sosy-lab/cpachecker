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
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslIntegerLiteralTerm extends AcslLiteralTerm {

  @Serial private static final long serialVersionUID = -814512301151276L;

  private final BigInteger value;

  public AcslIntegerLiteralTerm(FileLocation pFileLocation, AcslType pType, BigInteger pValue) {
    super(pFileLocation, pType);
    value = pValue;
    checkNotNull(pFileLocation);
    checkNotNull(pValue);
    checkNotNull(pType);
  }

  public static final AcslIntegerLiteralTerm ZERO =
      new AcslIntegerLiteralTerm(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO);

  public static final AcslIntegerLiteralTerm ONE =
      new AcslIntegerLiteralTerm(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ONE);

  public static final AcslIntegerLiteralTerm TWO =
      new AcslIntegerLiteralTerm(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.TWO);

  @Override
  public BigInteger getValue() {
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
