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
import java.util.List;
import org.sosy_lab.cpachecker.cfa.types.AbstractFunctionType;

public final class AcslPredicateType extends AbstractFunctionType implements AcslType {

  @Serial private static final long serialVersionUID = -814550244571276L;

  public AcslPredicateType(List<AcslType> pParameters, boolean pTakesVarArgs) {
    super(AcslBuiltinLogicType.BOOLEAN, pParameters, pTakesVarArgs);
    checkNotNull(pParameters);
    checkNotNull(pTakesVarArgs);
  }
}
