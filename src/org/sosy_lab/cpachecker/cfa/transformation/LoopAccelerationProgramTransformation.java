// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import static org.sosy_lab.cpachecker.cfa.transformation.LoopAccelerationUtils.closedFormAffine;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IExpr;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.transformation.LoopAccelerationUtils.RowSummand;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class LoopAccelerationProgramTransformation extends ProgramTransformation{

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
    Optional<ArrayList<ArrayList<RowSummand>>> closedFormOptional =
        closedFormAffine(transformationData.A, transformationData.b, transformationData.x);
    if (closedFormOptional.isEmpty()) {
      return Optional.empty();
    }
    // each i-th row in closedForm represents the assignment statement of the i-th variable in transformationdata.x
    // f.e. let x = {x0, x1, x2} and closedForm = {{...}, {(0, x0, 0, lam0), (3, x1, 1, lam1), (-5, x2, 2, lam2)}, {...}}
    //   then we get x1 = 0 * n^0 * lam0^n * x0 + 3 * n * lam1^n * x1 - 5 * n^2 * lam2^n * x2
    ArrayList<ArrayList<RowSummand>> closedForm = closedFormOptional.orElseThrow();
    ImmutableList.Builder<CExpressionAssignmentStatement> assignmentStatements = ImmutableList.builder();
    int counter = 0;
    for (ArrayList<RowSummand> assignment : closedForm) {
      CExpressionAssignmentStatement assignmentStatement =
          new CExpressionAssignmentStatement(
              FileLocation.DUMMY,
              new CIdExpression(
                  FileLocation.DUMMY,
                  null,
                  transformationData.x[counter],
                  null
              ),
              null);
      assignmentStatements.add(assignmentStatement);
      counter++;
    }

    // perform transformation
    ImmutableList.Builder<CFANode> nodes = ImmutableList.builder();
    ImmutableList.Builder<CFAEdge> edges = ImmutableList.builder();
    CFANode newEntryNode = CFANode.newDummyCFANode(pNode.getFunctionName());
    CFANode newExitNode = CFANode.newDummyCFANode(pNode.getFunctionName());
    CFANode nodeBeforeLoop = null;  // todo must be set in canBeApplied
    CFANode nodeAfterLoop = null;   // todo must be set in canBeApplied
//    nodes.add(newEntryNode, newExitNode);
//    CFANode currentNode = newEntryNode;
//    for (CStatement assignment : transformationData.assignments()) {
//      CStatement acceleratedAssignment = calculateAcceleratedStatement(transformationData.numberOfIterations, assignment);
//      if (transformationData.assignments.getLast() == assignment) {
//        CStatementEdge newEdge = new CStatementEdge(
//            null,
//            acceleratedAssignment,
//            FileLocation.DUMMY,
//            currentNode,
//            newExitNode
//        );
//        edges.add(newEdge);
//      } else {
//        CFANode newNode = CFANode.newDummyCFANode(pNode.getFunctionName());
//        nodes.add(newNode);
//        CStatementEdge newEdge = new CStatementEdge(
//            null,
//            acceleratedAssignment,
//            FileLocation.DUMMY,
//            currentNode,
//            newNode
//        );
//        edges.add(newEdge);
//        currentNode = newNode;
//      }
//    }

    SubCFA subCFA = new SubCFA(
        nodeBeforeLoop,
        nodeAfterLoop,
        newEntryNode,
        newExitNode,
        ProgramTransformationEnum.LOOP_ACCELERATION,
        ProgramTransformationBehaviour.PRECISE,
        ImmutableSet.copyOf(nodes.build()),
        ImmutableSet.copyOf(edges.build())
    );
    return Optional.of(new ProgramTransformationInformation(subCFA, null));
  }

  private static Optional<TransformationData> canBeApplied(CFANode pNode, LoopStructure pLoopStructure) {
    // pNode must be a loop start
    if (!pNode.isLoopStart()) {
      return Optional.empty();
    }

    ImmutableSet<Loop> loops = pLoopStructure.getLoopsForLoopHead(pNode);
    Iterator<Loop> loopIterable = loops.iterator();
    for (Iterator<Loop> it = loopIterable; it.hasNext(); ) {
      Loop loop = it.next();

    }

    return Optional.empty();
  }

  /**
   * Representation of an affine loop A * x + b with n iterations.
   * @param numberOfIterations
   * @param A square Matrix with d rows and columns
   * @param b integer vector with length d
   * @param x vector of d variables
   */
  private record TransformationData(
      int numberOfIterations,
      Matrix A,
      int[] b,
      String[] x
  ) {}
}
