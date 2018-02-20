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

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.smg.SMGBuiltins;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndStateList;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

class RHSExpressionValueVisitor extends ExpressionValueVisitor {

  private final SMGRightHandSideEvaluator smgRightHandSideEvaluator;

  public RHSExpressionValueVisitor(SMGRightHandSideEvaluator pSmgRightHandSideEvaluator,
      CFAEdge pEdge, SMGState pSmgState) {
    super(pSmgRightHandSideEvaluator, pEdge, pSmgState);
    smgRightHandSideEvaluator = pSmgRightHandSideEvaluator;
  }

  @Override
  public SMGValueAndStateList visit(CFunctionCallExpression pIastFunctionCallExpression)
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
        SMGAddressValueAndStateList configAllocEdge = builtins.evaluateConfigurableAllocationFunction(
            pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
        return configAllocEdge;
      }
      if (builtins.isExternalAllocationFunction(functionName)) {
        SMGAddressValueAndStateList extAllocEdge = builtins.evaluateExternalAllocation(
            pIastFunctionCallExpression, getInitialSmgState());
        return extAllocEdge;
      }
      switch (functionName) {
      case "__VERIFIER_BUILTIN_PLOT":
        builtins.evaluateVBPlot(pIastFunctionCallExpression, getInitialSmgState());
        break;
      case "__builtin_alloca":
        smgRightHandSideEvaluator.smgTransferRelation.possibleMallocFail = true;
        SMGAddressValueAndStateList allocEdge = builtins.evaluateAlloca(pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
        return allocEdge;
      case "printf":
        return SMGValueAndStateList.of(getInitialSmgState());
      default:
        if (builtins.isNondetBuiltin(functionName)) {
          return SMGValueAndStateList.of(getInitialSmgState());
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
          return SMGValueAndStateList.of(getInitialSmgState());
        case ASSUME_EXTERNAL_ALLOCATED:
          return smgRightHandSideEvaluator.smgTransferRelation.handleSafeExternFuction(
              pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
        default:
          throw new AssertionError(
              "Unhandled enum value in switch: "
                  + smgRightHandSideEvaluator.options.getHandleUnknownFunctions());
      }
    }

    return SMGValueAndStateList.of(getInitialSmgState());
  }
}