/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;

/**
 * State shared between all (formula) managers in this package that create a formula (part) for an
 * edge.
 */
class EdgeManagerContext {
  final GlobalManagerContext global;
  final CFAEdge edge;
  final String function;
  final SSAMapBuilder ssa;
  final Constraints constraints;

  @SuppressWarnings("WeakerAccess")
  final ErrorConditions errorConditions; // not used yet, but exists for future use

  final VariableIndexManager varIdMgr;
  final VariableManager varMgr;
  final JSExpressionFormulaManager exprMgr;
  final AssignmentManager assignmentMgr;
  final ObjectFormulaManager objMgr;
  final PropertyAccessManager propMgr;
  final VariableScopeManager scopeMgr;
  final JSFunctionDeclarationFormulaManager jsFunDeclMgr;

  EdgeManagerContext(
      final GlobalManagerContext pGlobal,
      final CFAEdge pEdge,
      final String pFunction,
      final SSAMapBuilder pSsa,
      final Constraints pConstraints,
      final ErrorConditions pErrorConditions) {
    global = pGlobal;
    edge = pEdge;
    function = pFunction;
    ssa = pSsa;
    constraints = pConstraints;
    errorConditions = pErrorConditions;

    // The following constructors should not access one of the other fields initialized below,
    // since they might not be defined yet and lead to a NullPointerException later.
    varIdMgr = new VariableIndexManager(this);
    varMgr = new VariableManager(this);
    exprMgr = new ExpressionToFormulaVisitor(this);
    assignmentMgr = new AssignmentManager(this);
    objMgr = new ObjectFormulaManager(this);
    propMgr = new PropertyAccessManager(this);
    scopeMgr = new VariableScopeManager(this);
    jsFunDeclMgr = new JSFunctionDeclarationFormulaManager(this);
  }

  EdgeManagerContext copy(final String pFunction) {
    return new EdgeManagerContext(global, edge, pFunction, ssa, constraints, errorConditions);
  }
}
