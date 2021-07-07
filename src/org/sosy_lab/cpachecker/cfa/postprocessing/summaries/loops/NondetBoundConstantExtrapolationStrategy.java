// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import java.util.ArrayList;
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
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class NondetBoundConstantExtrapolationStrategy extends ConstantExtrapolationStrategy {

  public NondetBoundConstantExtrapolationStrategy(
      final LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependencyInterface pStrategyDependencies) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies);
  }

  // TODO: When this Strategy is used, loop unrolling starts occuring continously, since first the
  // predicate refinement is done before the loop summary refinement
  //
  // TODO: This strategy will always result in an overflow since the bound is nondet, meaning the
  // extrapolation is nondet, which will always generate an overflow. Does it still make sense
  // to include this as a valid Strategy?

  protected Optional<GhostCFA> summaryCFA(
      final CFANode loopStartNode,
      final Map<String, Integer> loopVariableDelta,
      final Integer loopBranchIndex) {
    CFANode startNode = CFANode.newDummyCFANode("Ghost in the shell");

    String variableName =
        "tmpVarForLoopBoundWithExtraUniqueIdentifierIfThisVaribeleNameWasAlreadyTakenSomethingIsWrongWithYourCode";
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
        super.summaryCFA(loopStartNode, loopVariableDelta, loopBound, 1, -1, loopBranchIndex);

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
            superGhostCFA.getStartGhostCfaNode());

    startNode.addLeavingEdge(dummyEdge);
    superGhostCFA.getStartGhostCfaNode().addEnteringEdge(dummyEdge);

    // TODO Fix this by adding a Check that the negated entry condition is satisfied.

    CFANode afterLoopNode = loopStartNode.getLeavingEdge(1 - loopBranchIndex).getSuccessor();

    return Optional.of(
        new GhostCFA(startNode, superGhostCFA.getStopGhostCfaNode(), loopStartNode, afterLoopNode));
  }

  @Override
  public Optional<GhostCFA> summarize(final CFANode loopStartNode) {

    if (loopStartNode.getNumLeavingEdges() != 1) {
      return Optional.empty();
    }

    if (!loopStartNode.getLeavingEdge(0).getDescription().equals("while")) {
      return Optional.empty();
    }

    CFANode loopStartNodeLocal = loopStartNode.getLeavingEdge(0).getSuccessor();

    Optional<Integer> loopBranchIndexOptional = getLoopBranchIndex(loopStartNodeLocal);
    Integer loopBranchIndex;

    if (loopBranchIndexOptional.isEmpty()) {
      return Optional.empty();
    } else {
      loopBranchIndex = loopBranchIndexOptional.orElseThrow();
    }

    if (!linearArithmeticExpressionsLoop(loopStartNodeLocal, loopBranchIndex)) {
      return Optional.empty();
    }

    Map<String, Integer> loopVariableDelta =
        getLoopVariableDeltas(loopStartNodeLocal, loopBranchIndex);

    GhostCFA ghostCFA;
    Optional<GhostCFA> ghostCFASuccess =
        this.summaryCFA(loopStartNodeLocal, loopVariableDelta, loopBranchIndex);

    if (ghostCFASuccess.isEmpty()) {
      return Optional.empty();
    } else {
      ghostCFA = ghostCFASuccess.orElseThrow();
    }

    return Optional.of(ghostCFA);
  }

  @Override
  public boolean isPrecise() {
    return false;
  }
}
