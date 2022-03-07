// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import java.math.BigInteger;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

public interface AddressEvaluator {

  Collection<CValueAndSMGState>
      evaluateArraySubscriptAddress(SMGState pInitialSmgState, CExpression pExp);

  Collection<CValueAndSMGState>
      evaluateAddress(SMGState pInitialSmgState, CExpression pOperand);

  Collection<CValueAndSMGState>
      evaluateArrayAddress(SMGState pInitialSmgState, CExpression pOperand);

  Collection<CValueAndSMGState> createAddress(SMGState pState, CValue pValue);

  Collection<CValueAndSMGState>
      getAddressOfField(SMGState pInitialSmgState, CFieldReference pFieldReference);

  CValueAndSMGState handleUnknownDereference(SMGState pInitialSmgState);

  CValueAndSMGState readValue(SMGState pState, CValue value, CExpression pExp);

  CValueAndSMGState
      readValue(SMGState pSmgState, SMGObject pVariableObject, CExpression pIdExpression);

  BigInteger getBitSizeof(SMGState pInitialSmgState, CExpression pUnaryOperand);
}
