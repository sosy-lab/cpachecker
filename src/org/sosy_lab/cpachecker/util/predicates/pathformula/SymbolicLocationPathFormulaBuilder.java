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
package org.sosy_lab.cpachecker.util.predicates.pathformula;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SymbolicLocationPathFormulaBuilder extends DefaultPathFormulaBuilder {

  private final CBinaryExpressionBuilder cBinaryExpressionBuilder;

  private static class SymbolicLocationPathFormulaAndBuilder extends SymbolicLocationPathFormulaBuilder {

    private final DefaultPathFormulaBuilder previousPathFormula;

    private final CFAEdge edge;

    protected SymbolicLocationPathFormulaAndBuilder(
        final DefaultPathFormulaBuilder pPathFormulaAndBuilder,
        final CFAEdge pEdge,
        final CBinaryExpressionBuilder pCBinaryExpressionBuilder) {
      super(pCBinaryExpressionBuilder);
      this.previousPathFormula = pPathFormulaAndBuilder;
      this.edge = pEdge;
    }

    @Override
    protected PathFormula buildImplementation(
        final PathFormulaManager pPfmgr, final PathFormula pPathFormula)
        throws CPATransferException, InterruptedException {
      // Get the pathFormula up until this edge:
      PathFormula pathFormula = previousPathFormula.build(pPfmgr, pPathFormula);
      // Return extended path formula:
      return addNewPartsToPathFormula(pPfmgr, pathFormula);
    }

    private PathFormula addNewPartsToPathFormula(
        final PathFormulaManager pPfmgr, final PathFormula pathFormula)
        throws CPATransferException, InterruptedException {
      // add edge with symbolic location semantics: %pc==OLD -> edge -> %pc = NEW
      PathFormula newPathFormula;
      newPathFormula = pPfmgr.makeAnd(pathFormula, makeProgramCounterAssumption(edge));
      newPathFormula = pPfmgr.makeAnd(newPathFormula, edge);
      newPathFormula = pPfmgr.makeAnd(newPathFormula, makeProgramCounterAssignment(edge));
      return newPathFormula;
    }
  }

  private static class SymbolicLocationPathFormulaOrBuilder
      extends SymbolicLocationPathFormulaBuilder {
    private final PathFormulaBuilder first;
    private final PathFormulaBuilder second;

    protected SymbolicLocationPathFormulaOrBuilder(
        final PathFormulaBuilder first,
        final PathFormulaBuilder second,
        final CBinaryExpressionBuilder cBinaryExpressionBuilder) {
      super(cBinaryExpressionBuilder);
      this.first = first;
      this.second = second;
    }

    @Override
    protected PathFormula buildImplementation(PathFormulaManager pPfmgr, PathFormula pathFormula)
        throws CPATransferException, InterruptedException {
      PathFormula result =
          pPfmgr.makeOr(first.build(pPfmgr, pathFormula), second.build(pPfmgr, pathFormula));
      return result;
    }
  }

  @Override
  public PathFormulaBuilder makeAnd(CFAEdge pEdge) {
    return new SymbolicLocationPathFormulaAndBuilder(this, pEdge, cBinaryExpressionBuilder);
  }

  @Override
  public PathFormulaBuilder makeOr(PathFormulaBuilder other) {
    return new SymbolicLocationPathFormulaOrBuilder(this, other, cBinaryExpressionBuilder);
  }

  public SymbolicLocationPathFormulaBuilder(CBinaryExpressionBuilder pCBinaryExpressionBuilder) {
    cBinaryExpressionBuilder = pCBinaryExpressionBuilder;
  }

  public static class Factory implements PathFormulaBuilderFactory {

    private final CBinaryExpressionBuilder cBinaryExpressionBuilder;

    public Factory(CBinaryExpressionBuilder pCBinaryExpressionBuilder) {
      cBinaryExpressionBuilder = pCBinaryExpressionBuilder;
    }

    @Override
    public DefaultPathFormulaBuilder create() {
      return new SymbolicLocationPathFormulaBuilder(cBinaryExpressionBuilder);
    }
  }

  private CLeftHandSide getPcAsLeftHandSide() {
    CVariableDeclaration pc =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            true,
            CStorageClass.EXTERN,
            CNumericTypes.UNSIGNED_INT,
            "%pc",
            "%pc",
            "%pc",
            null /*CDefaults.forType(CNumericTypes.UNSIGNED_INT, FileLocation.DUMMY)*/);
    return new CIdExpression(FileLocation.DUMMY, pc);
  }

  public CFAEdge makeProgramCounterAssumption(CFAEdge cfaEdge) throws UnrecognizedCodeException {

    CFANode predecessorNode = ((AbstractCFAEdge) cfaEdge).getPredecessor();
    CFANode successorNode = ((AbstractCFAEdge) cfaEdge).getSuccessor();

    // Build assertion %pc == rightHandSide:
    CExpression rightHandSide =
        CIntegerLiteralExpression.createDummyLiteral(
            predecessorNode.getNodeNumber(), CNumericTypes.SIGNED_INT);
    CBinaryExpression assertion =
        cBinaryExpressionBuilder.buildBinaryExpression(
            getPcAsLeftHandSide(), rightHandSide, BinaryOperator.EQUALS);
    CAssumeEdge oldPcValueAsumeEdge =
        new CAssumeEdge(
            assertion.toASTString(),
            FileLocation.DUMMY,
            predecessorNode,
            successorNode,
            assertion,
            true);
    return oldPcValueAsumeEdge;
  }

  public CFAEdge makeProgramCounterAssignment(CFAEdge cfaEdge) {

    CFANode predecessorNode = ((AbstractCFAEdge) cfaEdge).getPredecessor();
    CFANode successorNode = ((AbstractCFAEdge) cfaEdge).getSuccessor();

    // Build assignment %pc = rightHandSide:
    CExpression rightHandSide =
        CIntegerLiteralExpression.createDummyLiteral(
            ((AbstractCFAEdge) cfaEdge).getSuccessor().getNodeNumber(), CNumericTypes.SIGNED_INT);
    CAssignment pcTransfer =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, getPcAsLeftHandSide(), rightHandSide);
    CStatementEdge assignNewPcValueStatementEdge =
        new CStatementEdge(
            pcTransfer.toASTString(),
            pcTransfer,
            FileLocation.DUMMY,
            predecessorNode,
            successorNode);
    return assignNewPcValueStatementEdge;
  }
}
