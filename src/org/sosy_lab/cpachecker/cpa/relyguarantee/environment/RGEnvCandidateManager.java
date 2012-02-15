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
package org.sosy_lab.cpachecker.cpa.relyguarantee.environment;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.ParallelCFAS;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.SSAMapManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

/**
 * Defines partial order on environmental candidates.
 */
public class RGEnvCandidateManager {

  private final FormulaManager fManager;
  private final PathFormulaManager pfManager;
  private final RGAbstractionManager absManager;
  private final SSAMapManager ssaManager;
  private final TheoremProver thmProver;
  private final RegionManager rManager;
  private final ParallelCFAS pcfas;
  private final LogManager logger;

  public RGEnvCandidateManager(FormulaManager fManager, PathFormulaManager pfManager, RGAbstractionManager absManager, SSAMapManager ssaManager, TheoremProver thmProver, RegionManager rManager, ParallelCFAS pPcfa, Configuration config,  LogManager logger) {
    this.fManager = fManager;
    this.pfManager = pfManager;
    this.absManager  = absManager;
    this.ssaManager = ssaManager;
    this.thmProver = thmProver;
    this.rManager  = rManager;
    this.pcfas = pPcfa;
    this.logger = logger;
  }

  /**
   * Return true only if the element is the smallest one w.r.t to the partial order.
   * @param c
   * @return
   */
  public boolean isBottom(RGEnvCandidate c){

    CFAEdge op = c.getOperation();

    Formula absF = c.getRgElement().getAbstractionFormula().asFormula();
    Formula f = c.getRgElement().getPathFormula().getFormula();

    if (absF.isFalse() || f.isFalse()){
      return true;
    }

    return false;
  }


  /**
   * Return true only if c1 is less or equal than c2.
   * @param c1
   * @param c2
   * @return
   */
  public boolean isLessOrEqual(RGEnvCandidate c1, RGEnvCandidate c2) {
    /*
     * c1 less or equal than c2 only if
     * (abs1 & pf1) -> (abs2 & pf2) and
     * op1 = op2
     */

    if (c1.equals(c2)){
      return true;
    }

    /* destroyed element is covered by some other, but not
     * necessarily c2 */
    if (c1.getElement().isDestroyed()){
      return true;
    }

    CFAEdge op1 = c1.getOperation();
    CFAEdge op2 = c2.getOperation();

    if (!op1.equals(op2)){
      return false;
    }

    Formula f1 = c1.getRgElement().getPathFormula().getFormula();
    Formula f2 = c1.getRgElement().getPathFormula().getFormula();

    if (f1.isTrue() && f2.isTrue()){
      // can compare BDDs
      Region r1 = c1.getRgElement().getAbstractionFormula().asRegion();
      Region r2 = c2.getRgElement().getAbstractionFormula().asRegion();


      if (!rManager.entails(r1, r2)){
        // (abs1 & pf1) does not imply (abs2 & pf2)
        return false;
      } else {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns true iff edge is an assignment to a non-global variable
   */
  private boolean isLocalAssigment(CFAEdge edge) {
    String var = getLhsVariable(edge);
    if (var == null || !pcfas.getGlobalVariables().contains(var)){
      return true;
    }
    return false;
  }

  /**
   * Get the variable in the lhs of an expression or return null
   */
  private String getLhsVariable(CFAEdge edge){
    IASTNode node = edge.getRawAST();
    if (node instanceof IASTExpressionAssignmentStatement) {
      IASTExpressionAssignmentStatement stmNode = (IASTExpressionAssignmentStatement) node;
      if (stmNode.getLeftHandSide() instanceof IASTIdExpression) {
        IASTIdExpression idExp = (IASTIdExpression) stmNode.getLeftHandSide();
        return new String(idExp.getName());
      }
    }
    return null;
  }

}
