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

import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.pcc.common.PCCCheckResult;
import org.sosy_lab.pcc.common.Pair;


public class SBEWithoutIndices2a_InvariantProofCheckAlgorithm extends SBEWithoutIndices2_InvariantProofCheckAlgorithm {

  public SBEWithoutIndices2a_InvariantProofCheckAlgorithm(Configuration pConfig, LogManager pLogger,
      String pProverType,
      boolean pAlwaysAtLoops, boolean pAlwaysAtFunctions) throws InvalidConfigurationException {
    super(pConfig, pLogger, pProverType, pAlwaysAtLoops, pAlwaysAtFunctions);
  }

  @Override
  protected PCCCheckResult proveEdge(String pEdge, int pSource, int pTarget,
      Vector<Pair<String, int[]>> pInvariantS, CFAEdge pCfaEdge,
      Vector<Pair<String, int[]>> pInvariantT) {
    logger.log(Level.INFO, "Check a single edge.");
    // build right formula without indices
    Formula[] abstraction = new Formula[pInvariantT.size()];
    Formula operation, right, rightInstantiated, left, proof;
    PathFormula op;
    Pair<Formula, SSAMap> resultAbs;
    // add stack part
    for (int i = 0; i < pInvariantT.size(); i++) {
      abstraction[i] =
          addStackInvariant(handler.createFormula(pInvariantT.get(i).getFirst()), pInvariantT.get(i)
              .getSecond(), false, pTarget);
      if (abstraction[i] == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
    }
    right = handler.buildDisjunction(abstraction);
    if (right == null) {
      logger.log(Level.SEVERE, "Cannot build formula of right invariant.");
      return PCCCheckResult.InvalidFormulaSpecificationInProof;
    }
    // build left formula without indices
    abstraction = new Formula[pInvariantS.size()];
    // add stack part
    for (int i = 0; i < pInvariantS.size(); i++) {
      abstraction[i] =
          addStackInvariant(handler.createFormula(pInvariantS.get(i).getFirst()), pInvariantS.get(i)
              .getSecond(), true, pSource);
      if (abstraction[i] == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
      // add stack part of operation
      if (pCfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
        abstraction[i] =
            addStackOperation(abstraction[i], pInvariantS.get(i).getSecond(), true,
                pCfaEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge,
                pCfaEdge.getPredecessor().getLeavingSummaryEdge().getSuccessor()
                    .getNodeNumber());
      } else {
        abstraction[i] =
            addStackOperation(abstraction[i], pInvariantS.get(i).getSecond(), false,
                pCfaEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge, -1);
      }
      if (abstraction[i] == null) {
        logger.log(Level.SEVERE, "Cannot add formula for stack operation.");
        return PCCCheckResult.InvalidFormulaSpecificationInProof;
      }

    }
    left = handler.buildDisjunction(abstraction);
    if (left == null) {
      logger.log(Level.SEVERE, "Cannot build formula of right invariant.");
      return PCCCheckResult.InvalidFormulaSpecificationInProof;
    }
    // add indices
    resultAbs = handler.addIndices(null, left);
    if (resultAbs == null) {
      logger.log(Level.SEVERE, "Cannot build formula of left invariant.");
      return PCCCheckResult.InvalidFormulaSpecificationInProof;
    }
    left = resultAbs.getFirst();
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

    // build and check edge invariant
    proof = handler.buildEdgeInvariant(left, operation, rightInstantiated);
    if (proof == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
    if (!handler.isFalse(proof)) {
      logger.log(Level.SEVERE, "Proof for edge failed.");
      return PCCCheckResult.InvalidART;
    }

    return PCCCheckResult.Success;
  }
}
