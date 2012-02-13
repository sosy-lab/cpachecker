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
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.pcc.common.ARTNode;
import org.sosy_lab.pcc.common.PCCCheckResult;
import org.sosy_lab.pcc.common.Pair;
import org.sosy_lab.pcc.common.Separators;
import org.sosy_lab.pcc.common.WithCorrespondingCFAEdgeARTEdge;

public class SBEWithoutIndices_ARTProofCheckAlgorithm extends
    SBE_ARTProofCheckAlgorithm {

  public SBEWithoutIndices_ARTProofCheckAlgorithm(Configuration pConfig,
      LogManager pLogger, String pProverType, boolean pAlwaysAtLoops, boolean pAlwaysAtFunctions)
      throws InvalidConfigurationException{
    super(pConfig, pLogger, pProverType, pAlwaysAtLoops, pAlwaysAtFunctions);
  }

  @Override
  protected boolean checkAbstraction(String pAbstraction) {
    if (pAbstraction.contains(Separators.SSAIndexSeparator)) { return false; }
    return true;
  }

  @Override
  protected PCCCheckResult readEdges(Scanner pScan) {
    int source, target;
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
          logger.log(Level.SEVERE, "Edge " + source + "#" + target + " has no corresponding CFA edge.");
          return PCCCheckResult.UnknownCFAEdge;
        }
        // check abstraction type
        intermediateRes = checkTargetAbstractionType(nodeT, handler.getEdgeOperation(cfaEdge));
        if (intermediateRes != PCCCheckResult.Success) {
          logger.log(Level.SEVERE, "Wrong abstraction type for ART node " + nodeT);
          return intermediateRes;
        }
        // add edge
        edge = new WithCorrespondingCFAEdgeARTEdge(target, cfaEdge);
        if(!nodeS.isEdgeContained(edge)){
          nodeS.addEdge(edge);
        }else{ /*return PCCCheckResult.ElementAlreadyRead;*/}
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
    // instantiate left abstraction
    Pair<Formula, SSAMap> resultAbs =
        handler.addIndices(null, pSource.getAbstraction());
    if (resultAbs == null) {
      logger.log(Level.SEVERE, "Cannot build abstraction of ART node " + pSource);
      return PCCCheckResult.InvalidFormulaSpecificationInProof;
    }
    Formula left = resultAbs.getFirst();
    if (left == null) {
      logger.log(Level.SEVERE, "Cannot build abstraction of ART node " + pSource);
      return PCCCheckResult.InvalidFormulaSpecificationInProof;
    }
    // get operation with indices
    PathFormula op =
        handler.getEdgeOperationFormula(resultAbs.getSecond(),
            pEdge.getCorrespondingCFAEdge());
    if (op == null) {
      logger.log(Level.SEVERE, "Cannot build operation.");
      return PCCCheckResult.InvalidFormulaSpecificationInProof;
    }

    // instantiate right abstraction
    resultAbs = handler.addIndices(op.getSsa(), pTarget.getAbstraction());
    if (resultAbs == null) {
      logger.log(Level.SEVERE, "Cannot build abstraction of ART node " + pTarget);
      return PCCCheckResult.InvalidFormulaSpecificationInProof;
    }
    Formula right = resultAbs.getFirst();
    if (right == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
    Formula proof = handler.buildEdgeInvariant(left, op.getFormula(), right);
    if (proof == null) {
      logger.log(Level.SEVERE, "Cannot build abstraction of ART node " + pTarget);
      return PCCCheckResult.InvalidFormulaSpecificationInProof;
    }
    if (handler.isFalse(proof)) {
      return PCCCheckResult.Success;
    } else {
      logger.log(Level.SEVERE, "Formula cannot be proven.");
      return PCCCheckResult.InvalidART;
    }
  }

}
