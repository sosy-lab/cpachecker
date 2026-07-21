// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import static org.sosy_lab.cpachecker.cfa.transformation.LoopAccelerationUtils.closedFormAffine;
import static org.sosy_lab.cpachecker.cfa.transformation.LoopAccelerationUtils.expressionFromCoefficients;
import static org.sosy_lab.cpachecker.cfa.transformation.LoopAccelerationUtils.simplifyClosedFormAssignment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.transformation.LoopAccelerationUtils.Coefficient;
import org.sosy_lab.cpachecker.cfa.transformation.AffineLoopClosedFormRepresentation.RowSummand;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class LoopAccelerationProgramTransformation extends ProgramTransformation{

  public LoopAccelerationProgramTransformation() {
    super(ProgramTransformationEnum.LOOP_ACCELERATION);
  }

  @Override
  public Optional<ProgramTransformationInformation> transform(CFA pCFA, CFANode pNode) {

    // get the loop structure
    Optional<LoopStructure> maybeLoopStructure = pCFA.getLoopStructure();
    LoopStructure loopStructure;
    if (maybeLoopStructure.isPresent()) {
      loopStructure = maybeLoopStructure.orElseThrow();
    } else {
      return Optional.empty();
    }

    // check if the loop iterations can be calculated and the loop accelerated
    Optional<TransformationData> transformationDataOptional = canBeApplied(pNode, loopStructure);
    TransformationData transformationData;
    if (transformationDataOptional.isEmpty()) {
      return Optional.empty();
    } else {
      transformationData = transformationDataOptional.orElseThrow();
    }

    // calculate the closed form of the loop
    Optional<AffineLoopClosedFormRepresentation> closedFormOptional =
        closedFormAffine(transformationData.loopRepresentation);
    if (closedFormOptional.isEmpty()) {
      return Optional.empty();
    }
    // each i-th row in closedForm represents the assignment statement of the i-th variable in transformationdata.x
    // f.e. let x = {x0, x1, x2} and closedForm = {{...}, {(0, x0, 0, lam0), (3, x1, 1, lam1), (-5, x2, 2, lam2)}, {...}}
    //   then we get x1 = 0 * n^0 * lam0^n * x0 + 3 * n * lam1^n * x1 - 5 * n^2 * lam2^n * x2
    AffineLoopClosedFormRepresentation closedForm = closedFormOptional.orElseThrow();

    // perform transformation
    ImmutableList.Builder<CFANode> nodes = ImmutableList.builder();
    ImmutableList.Builder<CFAEdge> edges = ImmutableList.builder();
    CFANode newEntryNode = CFANode.newDummyCFANode(pNode.getFunctionName());
    CFANode newExitNode = CFANode.newDummyCFANode(pNode.getFunctionName());
    nodes.add(newEntryNode, newExitNode);
    // todo get n
    CExpression numberOfIterations = new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, BigInteger.TWO);

    // add edges for each variable, row summand pair
    CFANode currentNode = newEntryNode;
    for (Entry<CIdExpression, ImmutableList<RowSummand>> entry : closedForm.getClosedForm().entrySet()) {
      Optional<CFAEdge> newEdge;
      try {
        newEdge = AffineLoopClosedFormRepresentation.getRowSummandStatements(
          entry.getKey(),
          entry.getValue(),
          numberOfIterations,
          currentNode
        );
      } catch (UnrecognizedCodeException pE) {
        throw new RuntimeException(pE);
      }
      if (newEdge.isPresent()) {
        edges.add(newEdge.orElseThrow());
        nodes.add(newEdge.orElseThrow().getSuccessor());
        currentNode = newEdge.orElseThrow().getSuccessor();
      }
    }

    // catch all blank edge for now
    BlankEdge dummyEdge = new BlankEdge(
        "finish loop acceleration",
        FileLocation.DUMMY,
        nodes.build().getLast(),
        newExitNode,
        "finish loop acceleration"
        );
    nodes.build().getLast().addLeavingEdge(dummyEdge);
    newExitNode.addEnteringEdge(dummyEdge);
    edges.add(dummyEdge);

    SubCFA subCFA = new SubCFA(
        transformationData.loopHead,
        transformationData.nodeAfterLoop,
        newEntryNode,
        newExitNode,
        ProgramTransformationEnum.LOOP_ACCELERATION,
        ImmutableSet.copyOf(nodes.build()),
        ImmutableSet.copyOf(edges.build())
    );
    return Optional.of(
        new ProgramTransformationInformation(subCFA, new LoopAccelerationRecovery()));
  }

  private static Optional<TransformationData> canBeApplied(CFANode pNode, LoopStructure pLoopStructure) {
    // pNode must be a loop start
    if (!pNode.isLoopStart()) {
      return Optional.empty();
    }

    ImmutableSet<Loop> loops = pLoopStructure.getLoopsForLoopHead(pNode);
    for (Loop loop : loops) {
      CFANode nodeAfterLoop = null;
      CFAEdge loopConditionEdge = null;
      CExpression loopCondition = null;
      Optional<CExpression> iterations = Optional.empty();

      // get some needed edges, nodes and expressions
      ImmutableSet<CFAEdge> loopEdges = loop.getInnerLoopEdges();
      for (CFAEdge edge : pNode.getLeavingEdges()) {
        if (loopEdges.contains(edge)) {
          loopConditionEdge = edge;
          loopCondition = ((CAssumeEdge) edge).getExpression();
        } else {
          nodeAfterLoop = edge.getSuccessor();
        }
      }
      if (loopConditionEdge == null || nodeAfterLoop == null) {
        continue;
      }

      // visit each edge and check that the loop has one AssumeEdge followed by only assignments to
      // variables
      LoopAccelerationVisitor visitor = new LoopAccelerationVisitor(pNode);
      TraversalProcess traversalProcess = visitor.visitEdge(loopConditionEdge);
      while (traversalProcess == TraversalProcess.CONTINUE) {
        if (visitor.getLastEdge().isPresent()) {
          traversalProcess =
              visitor.visitNode(visitor.getLastEdge().orElseThrow().getSuccessor());
        } else if (visitor.getLastNode().isPresent()) {
          traversalProcess =
              visitor.visitEdge(visitor.getLastNode().orElseThrow().getLeavingEdge(0));
        } else {
          break;
        }
      }
      if (traversalProcess == TraversalProcess.ABORT) {
        continue;
      }

      // extract the loop as A * x + b from the collected statements
      ImmutableList.Builder<CIdExpression> variables = ImmutableList.builder();
      ImmutableList.Builder<CExpression> assignments = ImmutableList.builder();
      ImmutableList<CExpressionAssignmentStatement> assignmentStatements =
          visitor.getStatements().build();
      boolean isAffineLoop = true;
      for (CExpressionAssignmentStatement assignment : assignmentStatements) {
        if (assignment.getLeftHandSide() instanceof CIdExpression variableExpression) {
          variables.add(variableExpression);
        } else {
          isAffineLoop = false;
          break;
        }
        assignments.add(assignment.getRightHandSide());
      }
      if (!isAffineLoop) {continue;}
      LoopAccelerationAffineLoopVisitor loopVisitor =
          new LoopAccelerationAffineLoopVisitor(variables.build());
      if (loopVisitor.visit(assignments.build()) == TraversalProcess.ABORT) {
        continue;
      }

      ImmutableMap<CIdExpression, List<BigInteger>> iterationMatrix = loopVisitor.getAssignments();

      return Optional.of(
        new TransformationData(5, pNode, nodeAfterLoop, AffineLoopRepresentation.fromIterationMatrixMap(iterationMatrix)));
    }

    return Optional.empty();
  }

  /**
   *
   * @param numberOfIterations
   * @param loopHead
   * @param nodeAfterLoop
   * @param loopRepresentation
   */
  private record TransformationData(
      int numberOfIterations,
      CFANode loopHead,
      CFANode nodeAfterLoop,
      AffineLoopRepresentation loopRepresentation
  ) {}
}
