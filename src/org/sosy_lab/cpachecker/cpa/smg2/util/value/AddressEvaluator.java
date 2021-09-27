// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

public interface AddressEvaluator {

  public Collection<CValueAndSMGState>
      evaluateArraySubscriptAddress(SMGState pInitialSmgState, CExpression pExp);

  public Collection<CValueAndSMGState>
      evaluateAddress(SMGState pInitialSmgState, CExpression pOperand);

  public Collection<CValueAndSMGState>
      evaluateArrayAddress(SMGState pInitialSmgState, CExpression pOperand);

  public Collection<CValueAndSMGState> createAddress(SMGState pState, CValue pValue);

  public Collection<CValueAndSMGState>
      getAddressOfField(SMGState pInitialSmgState, CExpression pFieldReference);

  public CValueAndSMGState handleUnknownDereference(SMGState pInitialSmgState);

  public CValueAndSMGState readValue(SMGState pState, CValue value, CExpression pExp);

  public CValueAndSMGState
      readValue(SMGState pSmgState, SMGObject pVariableObject, CExpression pIdExpression);

  public long getBitSizeof(SMGState pInitialSmgState, CExpression pUnaryOperand);
}
