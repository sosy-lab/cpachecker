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
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.pcc.common.PCCCheckResult;
import org.sosy_lab.pcc.common.Pair;
import org.sosy_lab.pcc.common.Separators;

public class SBEWithoutIndices_InvariantProofCheckAlgorithm extends
    SBE_InvariantProofCheckAlgorithm {

  public SBEWithoutIndices_InvariantProofCheckAlgorithm(Configuration pConfig,
      LogManager pLogger, String pProverType, boolean pAlwaysAtLoops, boolean pAlwaysAtFunctions)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pProverType, pAlwaysAtLoops, pAlwaysAtFunctions);
  }

  @Override
  protected boolean checkAbstraction(String pAbstraction) {
    if (pAbstraction.contains(Separators.SSAIndexSeparator)) { return false; }
    return true;
  }

  @Override
  protected PCCCheckResult readEdges(Scanner pScan) {
    int source, target, sourceEdge;
    CFANode nodeS, nodeT, nodeSourceOpEdge;
    CFAEdge edge;
    boolean notEdge;
    Vector<CFANode> successors;

    while (pScan.hasNext()) {
      try {
        // read next edge
        logger.log(Level.INFO, "Read next edge.");
        source = pScan.nextInt();
        sourceEdge = pScan.nextInt();
        target = pScan.nextInt();
        nodeS = reachableCFANodes.get(source);

        nodeSourceOpEdge = allCFANodes.get(sourceEdge);
        nodeT = reachableCFANodes.get(target);
        if (nodeS == null
            || nodeSourceOpEdge == null || nodeT == null) {
          logger.log(Level.SEVERE, "Edge " + source + "#" + sourceEdge + "#" + target
              + "not possible because one is no CFA node or is not an abstraction node.");
          return PCCCheckResult.UnknownCFAEdge;
        }

        //check edge
        edge = retrieveOperationEdge(nodeS, nodeSourceOpEdge, nodeT);
        if (edge == null) {
          logger.log(Level.SEVERE, "Edge " + source + "#" + sourceEdge + "#" + target + " is not correct edge in CFA.");
          return PCCCheckResult.InvalidEdge;
        }
        if (allCFANodes.get(source).getNumLeavingEdges() > 0
            && allCFANodes.get(source).getLeavingEdge(0) instanceof FunctionReturnEdge) {
          successors = getDirectSuccessors(source, sourceEdge, target);
          if (successors == null) {
            logger.log(Level.INFO, "No valid edge.");
            return PCCCheckResult.UnknownCFAEdge;
          }
          notEdge = true;
          for (int i = 0; i < successors.size(); i++) {
            if (!allInvariantFormulaeFalse(source, successors.get(i).getNodeNumber())) {
              notEdge = false;
            }
          }
          if (notEdge) {
            logger.log(Level.INFO, "No edge expected because source node should not be reachable.");
            return PCCCheckResult.UnknownCFAEdge;
          }
        } else {

          if (allInvariantFormulaeFalse(source, -1)) {
            logger.log(Level.INFO, "No edge expected because source node should not be reachable.");
            return PCCCheckResult.UnknownCFAEdge;
          }
        }

        //add edge
        if (edges.contains(source + Separators.commonSeparator + sourceEdge + Separators.commonSeparator + target)) { return PCCCheckResult.ElementAlreadyRead; }
        edges.add(source + Separators.commonSeparator + sourceEdge + Separators.commonSeparator + target);
      } catch (IllegalArgumentException e3) {
        return PCCCheckResult.UnknownCFAEdge;
      } catch (InputMismatchException e2) {
        return PCCCheckResult.UnknownCFAEdge;
      } catch (NoSuchElementException e1) {
        return PCCCheckResult.UnknownCFAEdge;
      }
    }
    logger.log(Level.INFO, "Check if all necessary edges are contained in proof.");
    return structuralCheckCoverageOfCFAEdges();
  }

  @Override
  protected PCCCheckResult proveEdge(String pEdge, int pSource, int pTarget,
      Vector<Pair<String, int[]>> pInvariantS, CFAEdge pCfaEdge,
      Vector<Pair<String, int[]>> pInvariantT) {
    // build right formula without indices
    logger.log(Level.INFO, "Check a single edge.");
    Formula[] rightAbstraction = new Formula[pInvariantT.size()];
    Formula operation, right, rightInstantiated, left, proof;
    PathFormula op;
    Pair<Formula, SSAMap> resultAbs;
    // add stack part
    for (int i = 0; i < pInvariantT.size(); i++) {
      rightAbstraction[i] =
          addStackInvariant(handler.createFormula(pInvariantT.get(i).getFirst()), pInvariantT.get(i)
              .getSecond(), false, pTarget);
      if (rightAbstraction[i] == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
    }
    right = handler.buildDisjunction(rightAbstraction);
    if (right == null) {
      logger.log(Level.SEVERE, "Cannot build formula of right invariant.");
      return PCCCheckResult.InvalidFormulaSpecificationInProof;
    }
    // iterate over all left abstractions
    for (int i = 0; i < pInvariantS.size(); i++) {
      // instantiate left abstraction
      resultAbs = handler.addIndices(null, pInvariantS.get(i).getFirst());
      if (resultAbs == null) {
        logger.log(Level.SEVERE, "Cannot build formula of left invariant.");
        return PCCCheckResult.InvalidFormulaSpecificationInProof;
      }
      left = resultAbs.getFirst();
      if (left == null) {
        logger.log(Level.SEVERE, "Cannot build formula of left invariant.");
        return PCCCheckResult.InvalidFormulaSpecificationInProof;
      }
      // add stack invariant
      left =
          addStackInvariant(left, pInvariantS.get(i).getSecond(), true, pSource);
      if (left == null) {
        logger.log(Level.SEVERE, "Cannot build formula of left invariant.");
        return PCCCheckResult.InvalidFormulaSpecificationInProof;
      }
      // get operation with indices
      op = handler.getEdgeOperationFormula(resultAbs.getSecond(), pCfaEdge);
      if (op == null) {
        logger.log(Level.SEVERE, "Cannot build formula for operation.");
        return PCCCheckResult.InvalidFormulaSpecificationInProof;
      }
      operation = op.getFormula();
      if (operation == null) {
        logger.log(Level.SEVERE, "Cannot build formula for operation.");
        return PCCCheckResult.InvalidFormulaSpecificationInProof;
      }
      // add stack operation to edge formula
      if (pCfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
        operation =
            addStackOperation(operation, pInvariantS.get(i).getSecond(), true,
                pCfaEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge,
                pCfaEdge.getPredecessor().getLeavingSummaryEdge().getSuccessor().getNodeNumber());
      } else {
        operation =
            addStackOperation(operation, pInvariantS.get(i).getSecond(), false,
                pCfaEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge, -1);
      }
      if (operation == null) {
        logger.log(Level.SEVERE, "Cannot build formula for operation.");
        return PCCCheckResult.InvalidFormulaSpecificationInProof;
      }
      // instantiate right abstraction
      resultAbs = handler.addIndices(op.getSsa(), right);
      if (resultAbs == null) {
        logger.log(Level.SEVERE, "Cannot build formula of left invariant.");
        return PCCCheckResult.InvalidFormulaSpecificationInProof;
      }
      rightInstantiated = resultAbs.getFirst();
      if (rightInstantiated == null) {
        logger.log(Level.SEVERE, "Cannot build formula of left invariant.");
        return PCCCheckResult.InvalidFormulaSpecificationInProof;
      }

      // build edge invariant
      proof = handler.buildEdgeInvariant(left, operation, rightInstantiated);
      if (proof == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
      if (!handler.isFalse(proof)) {
        logger.log(Level.SEVERE, "Proof for edge failed.");
        return PCCCheckResult.InvalidART;
      }
    }
    return PCCCheckResult.Success;
  }
}
