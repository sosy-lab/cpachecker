/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.pcc.common.ARTNode;
import org.sosy_lab.pcc.common.PCCCheckResult;
import org.sosy_lab.pcc.common.WithCorrespondingCFAEdgeARTEdge;
import org.sosy_lab.pcc.common.WithOpDescriptionARTSBEEdge;

public class SBEWithIndices_ARTProofCheckAlgorithm extends
    SBE_ARTProofCheckAlgorithm {

  public SBEWithIndices_ARTProofCheckAlgorithm(Configuration pConfig,
      LogManager pLogger, String pProverType, boolean pAlwaysAtLoops, boolean pAlwaysAtFunctions)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pProverType, pAlwaysAtLoops, pAlwaysAtFunctions);

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
    WithCorrespondingCFAEdgeARTEdge edge;
    PCCCheckResult intermediateRes;

    while (pScan.hasNext()) {
      try {
        // read next edge
        logger.log(Level.INFO, "Read next edge");
        source = pScan.nextInt();
        target = pScan.nextInt();
        nodeS = art.get(new Integer(source));
        nodeT = art.get(new Integer(target));
        if (nodeS != null && nodeT != null) {
          cfaEdge =
              nodeS.getCorrespondingCFANode().getEdgeTo(
                  nodeT.getCorrespondingCFANode());
        } else {
          logger.log(Level.SEVERE,
              "Cannot get corresponding CFA edge because either source or target node of edge is no valid ART node.");
                    return PCCCheckResult.UnknownCFAEdge;
        }

        operation = pScan.next();
        // check for correct operation
        String createdOperation = handler.getEdgeOperation(cfaEdge);
        if (createdOperation == null
            || !handler.isSameFormulaWithNormalizedIndices(createdOperation,
                operation)) {
          logger.log(Level.SEVERE, " Operation " + createdOperation + " does not fit to ART edge");
          return PCCCheckResult.InvalidART;
        }

        // check for correct abstraction type of target node
        intermediateRes = checkTargetAbstractionType(nodeT, operation);
        if (intermediateRes != PCCCheckResult.Success) {
          logger.log(Level.SEVERE, "Wrong abstraction type for ART node " + nodeT);
          return intermediateRes;
        }
        edge = new WithOpDescriptionARTSBEEdge(target, cfaEdge, operation);
        if(nodeS.isEdgeContained(edge)){
          //return PCCCheckResult.ElementAlreadyRead;
        }else{
        nodeS.addEdge(edge);}
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
  protected PCCCheckResult checkEdgeFormula(ARTNode pSource, WithCorrespondingCFAEdgeARTEdge pEdge,
      ARTNode pTarget) {
    try {
      return buildAndCheckFormula(pSource.getAbstraction(),
          ((WithOpDescriptionARTSBEEdge) pEdge).getOperation(),
          pTarget.getAbstraction(),
          pEdge.getCorrespondingCFAEdge().getEdgeType() == CFAEdgeType.AssumeEdge);
    } catch (ClassCastException e) {
      return PCCCheckResult.InvalidEdge;
    }
  }

  private PCCCheckResult buildAndCheckFormula(String pAbstractionLeft,
      String pOperation, String pAbstractionRight, boolean pAssume) {
    // check if operation fits to left abstraction
    if (!handler.operationFitsToLeftAbstraction(pAbstractionLeft, pOperation,
        pAssume)) {
      logger.log(Level.SEVERE, "Operation does not fit to left abstraction.");
      return PCCCheckResult.InvalidEdge;
    }
    // check if right abstraction fits to left abstraction and operation
    if (!handler.rightAbstractionFitsToOperationAndLeftAbstraction(
        pAbstractionLeft, pOperation, pAbstractionRight)) {
      logger.log(Level.SEVERE, "Right abstraction cannot be constructed in a correct ART.");
      return PCCCheckResult.InvalidFormulaSpecificationInProof;
    }
    Formula f =
        handler.buildEdgeInvariant(pAbstractionLeft, pOperation,
            pAbstractionRight);
    if (f == null) {
      logger.log(Level.WARNING, "Cannot build proof formula.");
      return PCCCheckResult.InvalidFormulaSpecificationInProof;
    }
    if (handler.isFalse(f)) {
      return PCCCheckResult.Success;
    } else {
      logger.log(Level.SEVERE, "("+ pAbstractionLeft + " & " + pOperation +")" + " -> " + "(" +pAbstractionRight
          + ") cannot be proven.");
      return PCCCheckResult.InvalidART;
    }
  }
}