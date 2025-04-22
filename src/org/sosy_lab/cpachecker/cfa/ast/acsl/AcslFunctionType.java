// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.types.AbstractFunctionType;
import org.sosy_lab.cpachecker.cfa.types.Type;

public final class AcslFunctionType extends AbstractFunctionType implements AcslType {

  @Serial private static final long serialVersionUID = -81455024380112316L;

  public AcslFunctionType(
      Type pReturnType, List<? extends Type> pParameters, boolean pTakesVarArgs) {
    super(pReturnType, pParameters, pTakesVarArgs);
  }
}
