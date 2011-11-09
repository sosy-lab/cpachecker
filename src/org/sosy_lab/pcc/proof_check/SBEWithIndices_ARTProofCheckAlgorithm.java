/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.pcc.proof_check;

import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.pcc.common.ARTEdge;
import org.sosy_lab.pcc.common.ARTNode;
import org.sosy_lab.pcc.common.PCCCheckResult;
import org.sosy_lab.pcc.common.WithOpDescriptionARTEdge;

public class SBEWithIndices_ARTProofCheckAlgorithm extends
    SBE_ARTProofCheckAlgorithm {

  public SBEWithIndices_ARTProofCheckAlgorithm(Configuration pConfig,
      LogManager pLogger, boolean pAlwaysAtLoops, boolean pAlwaysAtFunctions)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pAlwaysAtLoops, pAlwaysAtFunctions);

  }

  @Override
  protected boolean checkAbstraction(String pAbstraction) {
    if (pAbstraction.contains("|")) { return false; }
    return true;
  }

  @Override
  protected PCCCheckResult readEdges(Scanner pScan) {

    int source, target;
    String operation;
    CFAEdge cfaEdge;
    ARTNode nodeS, nodeT;
    ARTEdge edge;
    PCCCheckResult intermediateRes;

    while (pScan.hasNext()) {
      try {
        // read next edge
        source = pScan.nextInt();
        target = pScan.nextInt();
        nodeS = art.get(new Integer(source));
        nodeT = art.get(new Integer(target));
        if (nodeS != null && nodeT != null) {
          cfaEdge =
              nodeS.getCorrespondingCFANode().getEdgeTo(
                  nodeT.getCorrespondingCFANode());
        } else {
          return PCCCheckResult.UnknownCFAEdge;
        }

        operation = pScan.next();
        // check for correct operation
        String createdOperation = handler.getEdgeOperation(cfaEdge);
        if (createdOperation == null
            || !handler.isSameFormulaWithNormalizedIndices(createdOperation,
                operation)) { return PCCCheckResult.InvalidART; }

        // check for correct abstraction type of target node
        intermediateRes = checkTargetAbstractionType(nodeT, operation);
        if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }

        edge = new WithOpDescriptionARTEdge(target, cfaEdge, operation);
        nodeS.addEdge(edge);
      } catch (InputMismatchException e2) {
        return PCCCheckResult.UnknownCFAEdge;
      } catch (NoSuchElementException e3) {
        return PCCCheckResult.UnknownCFAEdge;
      } catch (IllegalArgumentException e4) {
        return PCCCheckResult.UnknownCFAEdge;
      }
    }
    return PCCCheckResult.Success;
  }

  @Override
  protected PCCCheckResult checkEdgeFormula(ARTNode pSource, ARTEdge pEdge,
      ARTNode pTarget) {
    try{
     return buildAndCheckFormula(pSource.getAbstraction(),
        ((WithOpDescriptionARTEdge) pEdge).getOperation(),
        pTarget.getAbstraction(),
        pEdge.getCorrespondingCFAEdge().getEdgeType() == CFAEdgeType.AssumeEdge);
    }catch(ClassCastException e){
      return PCCCheckResult.InvalidEdge;
    }
  }

  private PCCCheckResult buildAndCheckFormula(String pAbstractionLeft,
      String pOperation, String pAbstractionRight, boolean pAssume) {
    // check if operation fits to left abstraction
    if (handler.operationFitsToLeftAbstraction(pAbstractionLeft, pOperation,
        pAssume)) { return PCCCheckResult.InvalidEdge; }
    // check if right abstraction fits to left abstraction and operation
    if (handler.rightAbstractionFitsToOperationAndLeftAbstraction(
        pAbstractionLeft, pOperation, pAbstractionRight)) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
    Formula f =
        handler.buildEdgeInvariant(pAbstractionLeft, pOperation,
            pAbstractionRight);
    if (f == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
    if (f.isFalse()) {
      return PCCCheckResult.Success;
    } else {
      return PCCCheckResult.InvalidART;
    }
  }
}