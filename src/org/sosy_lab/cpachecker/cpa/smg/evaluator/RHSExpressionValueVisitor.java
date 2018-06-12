/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import java.util.Collections;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.smg.SMGBuiltins;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

class RHSExpressionValueVisitor extends ExpressionValueVisitor {

  private final SMGRightHandSideEvaluator smgRightHandSideEvaluator;

  public RHSExpressionValueVisitor(SMGRightHandSideEvaluator pSmgRightHandSideEvaluator,
      CFAEdge pEdge, SMGState pSmgState) {
    super(pSmgRightHandSideEvaluator, pEdge, pSmgState);
    smgRightHandSideEvaluator = pSmgRightHandSideEvaluator;
  }

  @Override
  public List<? extends SMGValueAndState> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws CPATransferException {

    CExpression fileNameExpression = pIastFunctionCallExpression.getFunctionNameExpression();
    String functionName = fileNameExpression.toASTString();

    //TODO extreme code sharing ...

    // If Calloc and Malloc have not been properly declared,
    // they may be shown to return void
    SMGBuiltins builtins = smgRightHandSideEvaluator.smgTransferRelation.builtins;
    if (builtins.isABuiltIn(functionName)) {
      if (builtins.isConfigurableAllocationFunction(functionName)) {
        smgRightHandSideEvaluator.smgTransferRelation.possibleMallocFail = true;
        return builtins.evaluateConfigurableAllocationFunction(
            pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
      }
      if (builtins.isExternalAllocationFunction(functionName)) {
        return builtins.evaluateExternalAllocation(pIastFunctionCallExpression, getInitialSmgState());
      }
      switch (functionName) {
      case "__VERIFIER_BUILTIN_PLOT":
        builtins.evaluateVBPlot(pIastFunctionCallExpression, getInitialSmgState());
        break;
      case "__builtin_alloca":
        smgRightHandSideEvaluator.smgTransferRelation.possibleMallocFail = true;
          return builtins.evaluateAlloca(
              pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
      case "printf":
          return Collections.singletonList(SMGValueAndState.of(getInitialSmgState()));
      default:
        if (builtins.isNondetBuiltin(functionName)) {
            return Collections.singletonList(SMGValueAndState.of(getInitialSmgState()));
        } else {
          throw new AssertionError("Unexpected function handled as a builtin: " + functionName);
        }
      }
    } else {
      switch (smgRightHandSideEvaluator.options.getHandleUnknownFunctions()) {
        case STRICT:
          throw new CPATransferException(
              "Unknown function '"
                  + functionName
                  + "' may be unsafe. See the cpa.smg.handleUnknownFunction option.");
        case ASSUME_SAFE:
          return Collections.singletonList(SMGValueAndState.of(getInitialSmgState()));
        case ASSUME_EXTERNAL_ALLOCATED:
          return smgRightHandSideEvaluator.handleSafeExternFuction(
              pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
        default:
          throw new AssertionError(
              "Unhandled enum value in switch: "
                  + smgRightHandSideEvaluator.options.getHandleUnknownFunctions());
      }
    }

    return Collections.singletonList(SMGValueAndState.of(getInitialSmgState()));
  }
}