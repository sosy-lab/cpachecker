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
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

public interface AddressEvaluator {

  public Collection<ValueAndSMGState> evaluateArraySubscriptAddress(
      SMGState pInitialSmgState, CExpression pExp);

  public Collection<ValueAndSMGState> evaluateAddress(
      SMGState pInitialSmgState, CExpression pOperand);

  public Collection<ValueAndSMGState> evaluateArrayAddress(
      SMGState pInitialSmgState, CExpression pOperand);

  public Collection<ValueAndSMGState> createAddress(SMGState pState, Value pValue);

  public Collection<ValueAndSMGState> getAddressOfField(
      SMGState pInitialSmgState, CFieldReference pFieldReference);

  public ValueAndSMGState handleUnknownDereference(SMGState pInitialSmgState);

  public ValueAndSMGState readValue(SMGState pState, Value value, CExpression pExp);

  public ValueAndSMGState readValue(
      SMGState pSmgState, SMGObject pVariableObject, CExpression pIdExpression);

  public BigInteger getBitSizeof(SMGState pInitialSmgState, CExpression pUnaryOperand);
}
