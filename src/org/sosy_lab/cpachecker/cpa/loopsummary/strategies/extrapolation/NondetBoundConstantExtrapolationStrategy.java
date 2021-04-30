// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary.strategies.extrapolation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.GhostCFA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class NondetBoundConstantExtrapolationStrategy extends ConstantExtrapolationStrategy {

  public NondetBoundConstantExtrapolationStrategy(
      final LogManager pLogger, ShutdownNotifier pShutdownNotifier, int strategyIndex) {
    super(pLogger, pShutdownNotifier, strategyIndex);
  }

  // TODO: When this Strategy is used, loop unrolling starts occuring continously, since first the
  // predicate refinement is done before the loop summary refinement

  protected Optional<GhostCFA> summaryCFA(
      final AbstractState pState,
      final Map<String, Integer> loopVariableDelta,
      final Integer loopBranchIndex) {
    CFANode startNode = CFANode.newDummyCFANode("Ghost in the shell");

    String variableName =
        "tmpVarForLoopBoundWithExtraUniqueIdentifierIfThisVaribeleNameWasAlreadyTakenSomethingIsWrongWhithYourCode";
    CVariableDeclaration pc =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            true,
            CStorageClass.EXTERN,
            CNumericTypes.LONG_LONG_INT, // TODO Improve this
            variableName,
            variableName,
            variableName,
            null);
    CIdExpression leftHandSide = new CIdExpression(FileLocation.DUMMY, pc);
    CFunctionCallExpression rightHandSide =
        new CFunctionCallExpression(
            FileLocation.DUMMY,
            CNumericTypes.LONG_LONG_INT,
            new CIdExpression(
                FileLocation.DUMMY,
                new CFunctionDeclaration(
                    FileLocation.DUMMY,
                    new CFunctionTypeWithNames(
                        CNumericTypes.LONG_LONG_INT, new ArrayList<CParameterDeclaration>(), false),
                    "__VERIFIER_nondet_long_long",
                    new ArrayList<CParameterDeclaration>())),
            new ArrayList<CExpression>(),
            new CFunctionDeclaration(
                FileLocation.DUMMY,
                new CFunctionTypeWithNames(
                    CNumericTypes.LONG_LONG_INT, new ArrayList<CParameterDeclaration>(), false),
                "__VERIFIER_nondet_long_long", // TODO See if this is the correct Syntax
                "__VERIFIER_nondet_long_long",
                new ArrayList<CParameterDeclaration>())); // TODO Improve this
    CFunctionCallAssignmentStatement cStatementEdge =
        new CFunctionCallAssignmentStatement(FileLocation.DUMMY, leftHandSide, rightHandSide);

    CType calculationType =
        new CSimpleType(false, false, CBasicType.INT, true, true, false, false, false, false, true);
    CType expressionType =
        new CSimpleType(false, false, CBasicType.INT, true, true, false, false, false, false, true);

    CExpression loopBound =
        new CBinaryExpression(
            FileLocation.DUMMY,
            expressionType,
            calculationType,
            new CIdExpression(FileLocation.DUMMY, pc),
            CIntegerLiteralExpression.createDummyLiteral(0, CNumericTypes.INT),
            BinaryOperator.MINUS);

    Optional<GhostCFA> superOptionalGhostCFA =
        super.summaryCFA(pState, loopVariableDelta, loopBound, 1, loopBranchIndex);

    GhostCFA superGhostCFA;
    if (superOptionalGhostCFA.isEmpty()) {
      return Optional.empty();
    } else {
      superGhostCFA = superOptionalGhostCFA.orElseThrow();
    }

    CFAEdge dummyEdge =
        new CStatementEdge(
            variableName + " = NONDET",
            cStatementEdge,
            FileLocation.DUMMY,
            startNode,
            superGhostCFA.getStartNode());

    startNode.addLeavingEdge(dummyEdge);
    superGhostCFA.getStartNode().addEnteringEdge(dummyEdge);

    return Optional.of(new GhostCFA(startNode, superGhostCFA.getStopNode()));
  }

  @Override
  public Optional<Collection<? extends AbstractState>> summarizeLoopState(
      final AbstractState pState, final Precision pPrecision, TransferRelation pTransferRelation)
      throws CPATransferException, InterruptedException {

    Optional<Integer> loopBranchIndexOptional =
        getLoopBranchIndex(AbstractStates.extractLocation(pState));
    Integer loopBranchIndex;

    if (loopBranchIndexOptional.isEmpty()) {
      return Optional.empty();
    } else {
      loopBranchIndex = loopBranchIndexOptional.orElseThrow();
    }

    if (!linearArithmeticExpressionsLoop(AbstractStates.extractLocation(pState), loopBranchIndex)) {
      return Optional.empty();
    }

    Map<String, Integer> loopVariableDelta = getLoopVariableDeltas(pState, loopBranchIndex);

    GhostCFA ghostCFA;
    Optional<GhostCFA> ghostCFASuccess =
        this.summaryCFA(pState, loopVariableDelta, loopBranchIndex);

    if (ghostCFASuccess.isEmpty()) {
      return Optional.empty();
    } else {
      ghostCFA = ghostCFASuccess.orElseThrow();
    }

    Collection<AbstractState> realStatesEndCollection =
        transverseGhostCFA(ghostCFA, pState, pPrecision, pTransferRelation, loopBranchIndex);

    return Optional.of(realStatesEndCollection);
  }

  @Override
  public boolean isPrecise() {
    return false;
  }
}
