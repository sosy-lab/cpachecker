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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IExpr;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.transformation.LoopAccelerationUtils.Coefficient;
import org.sosy_lab.cpachecker.cfa.transformation.LoopAccelerationUtils.RowSummand;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
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
        closedFormAffine(transformationData.A, transformationData.b, transformationData.varNames);
    if (closedFormOptional.isEmpty()) {
      return Optional.empty();
    }
    // each i-th row in closedForm represents the assignment statement of the i-th variable in transformationdata.x
    // f.e. let x = {x0, x1, x2} and closedForm = {{...}, {(0, x0, 0, lam0), (3, x1, 1, lam1), (-5, x2, 2, lam2)}, {...}}
    //   then we get x1 = 0 * n^0 * lam0^n * x0 + 3 * n * lam1^n * x1 - 5 * n^2 * lam2^n * x2
    ArrayList<ArrayList<RowSummand>> closedForm = closedFormOptional.orElseThrow();

    // insert n into the closed form and build the resulting assignment statements
    ImmutableList.Builder<CExpressionAssignmentStatement> assignmentStatements = ImmutableList.builder();
    int index = 0;
    for (ArrayList<RowSummand> assignment : closedForm) {
      List<Coefficient> assignmentWithn = simplifyClosedFormAssignment(transformationData.numberOfIterations, assignment, transformationData.x);
      CExpressionAssignmentStatement assignmentStatement =
          new CExpressionAssignmentStatement(
              FileLocation.DUMMY,
              new CIdExpression(
                  FileLocation.DUMMY,
                  transformationData.x[index].getExpressionType(),
                  transformationData.x[index].getName(),
                  transformationData.x[index].getDeclaration()
              ),
              expressionFromCoefficients(assignmentWithn));
      assignmentStatements.add(assignmentStatement);
      index++;
    }
    ImmutableList<CExpressionAssignmentStatement> assignments = assignmentStatements.build();

    // perform transformation
    ImmutableList.Builder<CFANode> nodes = ImmutableList.builder();
    ImmutableList.Builder<CFAEdge> edges = ImmutableList.builder();
    CFANode newEntryNode = CFANode.newDummyCFANode(pNode.getFunctionName());
    CFANode newExitNode = CFANode.newDummyCFANode(pNode.getFunctionName());

    CFANode currentNode = newEntryNode;
    CFANode nextNode;
    for (int i = 0; i < assignments.size(); i++) {
      if (i == assignments.size() - 1) {
        nextNode = newExitNode;
      } else {
        nextNode = CFANode.newDummyCFANode(pNode.getFunctionName());
        nodes.add(nextNode);
      }
      CFAEdge newEdge = new CStatementEdge(
          assignments.get(i).toString(),
          assignments.get(i),
          FileLocation.DUMMY,
          currentNode,
          nextNode
      );
      edges.add(newEdge);
      currentNode = nextNode;
    }

    SubCFA subCFA = new SubCFA(
        transformationData.loopNode,
        transformationData.nodeAfterLoop,
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

    CFANode nodeAfterLoop = null;

    // todo calculate n
    int n = 5;

    ImmutableSet<Loop> loops = pLoopStructure.getLoopsForLoopHead(pNode);
    Iterator<Loop> loopIterable = loops.iterator();
    for (Iterator<Loop> it = loopIterable; it.hasNext(); ) {
      Loop loop = it.next();
      ImmutableSet<CFAEdge> loopEdges = loop.getInnerLoopEdges();
      CFAEdge firstEdge = null;
      for (CFAEdge edge : pNode.getLeavingEdges()) {
        if (loopEdges.contains(edge)) {
          firstEdge = edge;
        } else {
          nodeAfterLoop = edge.getPredecessor();
        }
      }
      if (firstEdge == null) {return  Optional.empty(); }
      LoopAccelerationVisitor visitor = new LoopAccelerationVisitor(pNode);
      TraversalProcess traversalProcess = visitor.visitEdge(firstEdge);
      while (traversalProcess != TraversalProcess.ABORT) {
        if (visitor.getLastEdge().isPresent()) {
          traversalProcess = visitor.visitNode(visitor.getLastEdge().orElseThrow().getSuccessor());
        } else if (visitor.getLastNode().isPresent()) {
          traversalProcess = visitor.visitEdge(visitor.getLastNode().orElseThrow().getLeavingEdge(0));
        } else {
          break;
        }
      }
      boolean isAffineLoop = true;
      int[] b;
      int[][] A;
      CIdExpression[] x;
      if (! visitor.wasSuccesful() || nodeAfterLoop == null) {
        isAffineLoop = false;
      } else {
        // extract the loop as A * x + b from the collected statements
        ImmutableList<CExpressionAssignmentStatement> assignmentStatements =
            visitor.getStatements().build();
        x = new CIdExpression[assignmentStatements.size()];
        CExpression[] rightHandSides = new CExpression[assignmentStatements.size()];
        b = new int[assignmentStatements.size()];
        A = new int[assignmentStatements.size()][assignmentStatements.size()];
        int i = 0;
        for (CExpressionAssignmentStatement assignment : assignmentStatements) {
          if (assignment.getLeftHandSide() instanceof CIdExpression variableExpression) {
            x[i] = variableExpression;
          } else {
            isAffineLoop = false;
            break;
          }
          rightHandSides[i] = assignment.getRightHandSide();
          i++;
        }
        ArrayList<String> varNames = new ArrayList<>();
        for (i = 0; i < x.length; i++) {
          varNames.add(x[i].getName());
        }
        LoopAccelerationAffineLoopVisitor loopVisitor = new LoopAccelerationAffineLoopVisitor(varNames);
        if(loopVisitor.visit(rightHandSides) == TraversalProcess.ABORT) {
          isAffineLoop = false;
        }

        ImmutableMap<String, BigInteger[]> iterationMatrix = loopVisitor.getAssignments();
        for (i = 0; i < x.length; i++) {
          if (!iterationMatrix.containsKey(varNames.get(i))) {
            isAffineLoop = false;
            break;
          }
          BigInteger[] coefficients = iterationMatrix.get(varNames.get(i));
          for (int j = 0; j < coefficients.length; j++) {
            if (j == coefficients.length - 1) {
              b[i] = coefficients[j].intValue();
            } else {
              A[i][j] =  coefficients[j].intValue();
            }
          }
        }

        if (isAffineLoop) {
          return Optional.of(new TransformationData(n, pNode, nodeAfterLoop, Matrix.createMatrix(A), b, x, varNames.toArray(new String[x.length])));
        }
      }
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
      CFANode loopNode,
      CFANode nodeAfterLoop,
      Matrix A,
      int[] b,
      CIdExpression[] x,
      String[] varNames
  ) {}
}
