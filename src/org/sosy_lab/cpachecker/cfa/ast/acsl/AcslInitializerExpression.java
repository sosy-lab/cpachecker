// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslInitializerExpression extends AInitializerExpression
    implements AcslInitializer {

  @Serial private static final long serialVersionUID = -814550243123276L;

  public AcslInitializerExpression(FileLocation pFileLocation, AcslExpression pExpression) {
    super(pFileLocation, pExpression);
  }

  @Override
  public AcslExpression getExpression() {
    return (AcslExpression) super.getExpression();
  }

  @Override
  public <R, X extends Exception> R accept(AcslInitializerVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}
