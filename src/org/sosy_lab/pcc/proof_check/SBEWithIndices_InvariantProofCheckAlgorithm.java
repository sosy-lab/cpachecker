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

import java.util.Hashtable;
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
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.pcc.common.PCCCheckResult;
import org.sosy_lab.pcc.common.Pair;
import org.sosy_lab.pcc.common.Separators;

public class SBEWithIndices_InvariantProofCheckAlgorithm extends
    SBE_InvariantProofCheckAlgorithm {

  private Hashtable<String, Formula[]> edgeOperations =
      new Hashtable<String, Formula[]>();

  public SBEWithIndices_InvariantProofCheckAlgorithm(Configuration pConfig,
      LogManager pLogger, String pProverType, boolean pAlwaysAtLoops, boolean pAlwaysAtFunctions)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pProverType, pAlwaysAtLoops, pAlwaysAtFunctions);
  }

  @Override
  protected PCCCheckResult readEdges(Scanner pScan) {
    int source, target, sourceEdge, numOps;
    CFANode nodeS, nodeT, nodeSourceOpEdge;
    String op;
    PCCCheckResult intermediateRes;
    Formula[] operations;
    Vector<CFANode> successors;
    boolean notEdge;
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
        if (nodeS == null || nodeSourceOpEdge == null || nodeT == null) {
          logger.log(Level.INFO, "One identifier of edge " + source + "#" + sourceEdge + "#" + target
              + " is not a CFA node or a CFA node without abstraction.");
          return PCCCheckResult.UnknownCFAEdge;
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
        // get all operations with respective SSA indices
        numOps = pScan.nextInt();
        if (numOps < 1) { return PCCCheckResult.UnknownCFAEdge; }
        operations = new Formula[numOps];
        for (int i = 0; i < numOps; i++) {
          op = pScan.next();
          if (op.length() == 0) {
            operations[i] = null;
          } else {
            operations[i] = handler.createFormula(op);
          }
        }
        //check edge
        intermediateRes =
            structuralCheckEdge(nodeS, nodeSourceOpEdge, nodeT, operations);
        if (intermediateRes != PCCCheckResult.Success) {
          logger.log(Level.SEVERE, "Not a valid edge.");
          return intermediateRes;
        }
        //add edge
        if (edges.contains(source + Separators.commonSeparator + sourceEdge + Separators.commonSeparator + target)) { return PCCCheckResult.ElementAlreadyRead; }
        edges.add(source + Separators.commonSeparator + sourceEdge + Separators.commonSeparator + target);
        edgeOperations.put(source + Separators.commonSeparator + sourceEdge + Separators.commonSeparator + target,
            operations);
      } catch (IllegalArgumentException e3) {
        return PCCCheckResult.UnknownCFAEdge;
      } catch (InputMismatchException e2) {
        return PCCCheckResult.UnknownCFAEdge;
      } catch (NoSuchElementException e1) {
        return PCCCheckResult.UnknownCFAEdge;
      }
    }
    return structuralCheckCoverageOfCFAEdges();
  }

  protected PCCCheckResult structuralCheckEdge(CFANode pSource,
      CFANode pEdgeSource, CFANode pTarget, Formula[] operations) {
    CFAEdge edge = retrieveOperationEdge(pSource, pEdgeSource, pTarget);
    if (edge == null) { return PCCCheckResult.InvalidEdge; }
    // get operation formula
    String builtOp = handler.getEdgeOperation(edge);
    //check operations
    for (int i = 0; i < operations.length; i++) {
      if (operations[i] == null) {
        if (!handler.isSameFormulaWithNormalizedIndices("", builtOp)) { return PCCCheckResult.InvalidOperation; }
      } else {
        if (!handler.isSameFormulaWithNormalizedIndices(
            operations[i].toString(), builtOp)) { return PCCCheckResult.InvalidOperation; }
      }
    }
    return PCCCheckResult.Success;
  }

  @SuppressWarnings("unused")
  private Formula buildRightFormula(Vector<Pair<Formula, int[]>> pInvariantT,
      int pTargetNode) {
    Formula[] subFormulae = new Formula[pInvariantT.size()];
    Pair<Formula, int[]> current;
    for (int i = 0; i < pInvariantT.size(); i++) {
      current = pInvariantT.get(i);
      subFormulae[i] =
          addStackInvariant(current.getFirst(), current.getSecond(), false,
              pTargetNode);
      if (subFormulae[i] == null) { return null; }
    }
    return handler.buildDisjunction(subFormulae);
  }

  @Override
  protected boolean checkAbstraction(String pAbstraction) {
    if (pAbstraction.contains("|")) { return false; }
    return true;
  }

  @Override
  protected PCCCheckResult proveEdge(String pEdge, int pSource, int pTarget,
      Vector<Pair<String, int[]>> pInvariantS, CFAEdge pCfaEdge,
      Vector<Pair<String, int[]>> pInvariantT) {
    logger.log(Level.INFO, "Check single edge " + pEdge);
    Formula proof, left, right, completeOperation;
    Formula[] edgeFormulae;
    String edgeOp;

    boolean successfulEdgeProof, successfulAbstraction;
    edgeFormulae = edgeOperations.get(pEdge);
    // iterate over all source abstractions
    for (int i = 0; i < pInvariantS.size(); i++) {
      // iterate over all operation
      successfulAbstraction = false;
      // build left formula
      left =
          addStackInvariant(handler.createFormula(pInvariantS.get(i).getFirst()), pInvariantS.get(i)
              .getSecond(), true, pSource);
      for (int k = 0; k < edgeFormulae.length; k++) {
        if (edgeFormulae[k] == null) {
          edgeOp = "";
        } else {
          edgeOp = edgeFormulae[k].toString();
        }
        if (!handler.operationFitsToLeftAbstraction(pInvariantS.get(i)
            .getFirst().toString(), edgeOp,
            pCfaEdge.getEdgeType() == CFAEdgeType.AssumeEdge || pCfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge)) {
          continue;
        }
        // add stack operation to edge formula
        if (pCfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
          completeOperation =
              addStackOperation(edgeFormulae[k],
                  pInvariantS.get(i).getSecond(), true,
                  pCfaEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge,
                  pCfaEdge.getPredecessor().getLeavingSummaryEdge().getSuccessor()
                      .getNodeNumber());
        } else {
          completeOperation =
              addStackOperation(edgeFormulae[k],
                  pInvariantS.get(i).getSecond(), false,
                  pCfaEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge, -1);
        }
        successfulEdgeProof = false;
        // iterate over all target abstraction at least one item
        for (int j = 0; !successfulEdgeProof && j < pInvariantT.size(); j++) {
          // check if right abstraction fits to left abstraction and operation
          if (!handler.rightAbstractionFitsToOperationAndLeftAbstraction(
              pInvariantS.get(i).getFirst().toString(),
              edgeOp, pInvariantT.get(j).getFirst()
                  .toString())) {
            continue;
          }
          // build right formula
          right =
              addStackInvariant(handler.createFormula(pInvariantT.get(j).getFirst()), pInvariantT.get(j)
                  .getSecond(), false, pTarget);
          if (left == null || completeOperation == null || right == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
          // create proof formula
          proof = handler.buildEdgeInvariant(left, completeOperation, right);
          if (proof == null || !handler.isFalse(proof)) {
            return PCCCheckResult.InvalidFormulaSpecificationInProof;
          } else {
            successfulEdgeProof = true;
          }
        }
        if (successfulEdgeProof) {
          successfulAbstraction = true;
        }
      }
      if (!successfulAbstraction) { return PCCCheckResult.InvalidART; }
    }
    return PCCCheckResult.Success;
  }
}