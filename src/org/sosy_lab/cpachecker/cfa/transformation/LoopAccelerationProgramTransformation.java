// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

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
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
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


    // perform transformation
    ImmutableList.Builder<CFANode> nodes = ImmutableList.builder();
    ImmutableList.Builder<CFAEdge> edges = ImmutableList.builder();
    CFANode newEntryNode = CFANode.newDummyCFANode(pNode.getFunctionName());
    CFANode newExitNode = CFANode.newDummyCFANode(pNode.getFunctionName());
    nodes.add(newEntryNode, newExitNode);
    CFANode currentNode = newEntryNode;
    for (CStatement assignment : transformationData.assignments()) {
      CStatement acceleratedAssignment = calculateAcceleratedStatement(transformationData.numberOfIterations, assignment);
      if (transformationData.assignments.getLast() == assignment) {
        CStatementEdge newEdge = new CStatementEdge(
            null,
            acceleratedAssignment,
            FileLocation.DUMMY,
            currentNode,
            newExitNode
        );
        edges.add(newEdge);
      } else {
        CFANode newNode = CFANode.newDummyCFANode(pNode.getFunctionName());
        nodes.add(newNode);
        CStatementEdge newEdge = new CStatementEdge(
            null,
            acceleratedAssignment,
            FileLocation.DUMMY,
            currentNode,
            newNode
        );
        edges.add(newEdge);
        currentNode = newNode;
      }
    }

    SubCFA subCFA = new SubCFA(
        transformationData.nodeBeforeLoop,
        transformationData.nodeAfterLoop,
        newEntryNode,
        newExitNode,
        ProgramTransformationEnum.LOOP_ACCELERATION,
        transformationData.behaviour,
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

  private Optional<ArrayList<ArrayList<RowSummand>>> closedFormAffine(Matrix A, int[] b, String[] x) {
    assert (A.getRowNum() == A.getColumnNum()) : "Error: A is not a square matrix!";
    assert (A.getRowNum() == b.length);
    assert (b.length == x.length);

    ExprEvaluator util = new ExprEvaluator(false, (short) 100);
    try{
      IExpr n = util.eval("n");
      int d = A.getRowNum();

      // 0. make A * x + b homogenous
      IExpr x_fresh = util.eval("x_fresh");
      int[][] Ahom = new int[d+1][d+1];
      for(int i = 0; i < d; i++){
        for(int j = 0; j < d; j++){
          Ahom[i][j] = A.getElement(i, j);
        }
        Ahom[i][d] = b[i];
      }
      Ahom[d][d] = 1;
      String[] xhom = new String[d+1];
      for(int i = 0; i < d; i++){
        xhom[i] = x[i];
      }
      xhom[d] = "x_fresh";

      Optional<ArrayList<ArrayList<RowSummand>>> closedFormOptional = closedFormLinear(Matrix.createMatrix(Ahom), xhom);

      if(closedFormOptional.isPresent()) {
        // drop the last row
        ArrayList<ArrayList<RowSummand>> closedForm = closedFormOptional.orElseThrow();
        closedForm.removeLast();
        // remove occurrences of x_fresh
        for (ArrayList<RowSummand> row : closedForm) {
          row.replaceAll(
              (RowSummand summand) ->
                  summand.variable.equals("x_fresh")
                      ? new RowSummand(summand.coeff, null, summand.power, summand.lambda)
                      : summand);
        }

        return Optional.of(closedForm);
      } else {
        return Optional.empty();
      }

    } catch(Exception e){
      return Optional.empty();
    }
  }

  private Optional<ArrayList<ArrayList<RowSummand>>> closedFormLinear(Matrix A, String[] x) {
    assert (A.getRowNum() == A.getColumnNum()) : "Error: A is not a square matrix!";
    assert (A.getRowNum() == x.length);

    ExprEvaluator util = new ExprEvaluator(false, (short) 100);
    try{
      IExpr n = util.eval("n");

      // 1. calculate the jordan form of the loop: A = P * J * Pinv
      IExpr jordanForm = util.eval("JordanDecomposition(" + A + ")");
      assert (jordanForm.isListOfMatrices() && jordanForm.size() == 2) : "Error: jordan decomposition failed in LoopAccelerationProgramTransformation!";
      Matrix P = Matrix.createMatrix(jordanForm.first().toIntMatrix());
      Matrix J = Matrix.createMatrix(jordanForm.last().toIntMatrix());
      Matrix Pinv = Matrix.createMatrix(util.eval("Inverse("+P+")").toIntMatrix());
      int d = A.getColumnNum();
      boolean negativeEigenvalue = false;

      // 2. compute information about every jordan block of J
      List<BlockInfo> blockInfos = new ArrayList<>();
      int m = 1;
      for (int i = 0; i < d; i = i + m) {
        int lamba = J.getElement(i,i);
        if (lamba < 0) {
          negativeEigenvalue = true;
        }
        m = 1;
        while (i + m < d &&
            J.getElement(i + m - 1, i + m) == 1) {
          m++;
        }
        blockInfos.add(new BlockInfo(i, m, lamba));
      }

      // 3. N0 = maximal size among zero eigenvalue blocks
      int N0 = 0;
      for (BlockInfo block : blockInfos) {
        if (block.lambda == 0) {
          N0 = Math.max(N0, block.blockSize);
        }
      }

      // 4. Compute J^n
      List<Summand>[][] Jn = new ArrayList[d][d];
      for (BlockInfo block : blockInfos) {
        for (int i = 0; i < block.startIndex; i++) {
          for (int j = 0; j < block.startIndex; j++) {
            int r = j - i;
            if (r < 0) {
            Jn[i][j] = new ArrayList<>();
            } else {
              IExpr poly;
              if (r == 0) {
                poly = util.eval("1");
              } else {
                poly = util.eval("1");
                for (int k = 0; k < r; k++) {
                  poly = poly.times(n.subtract(util.eval(String.valueOf(k))));
                }
                poly = util.eval("Divide("+poly+", Factorial("+r+"))");
                poly = poly.divide(util.eval(String.valueOf(r)).factorial());
                poly = F.evalSimplify(poly);
              }
              // TODO check if this is correct
              int deg = r;

              List<Summand> summands = new ArrayList<>();
              if (block.lambda != 0) {
                IExpr coeffsExpr = F.CoefficientList(poly, n);
                int[] coeffs = coeffsExpr.toIntVector();
                for (int s = 0; s < deg; s++) {
                  // TODO cant this be calculated directly??
                  // IExpr coeffExpr =
                  // F.evalSimplify(util.eval(String.valueOf(coeff)).times(util.eval(String.valueOf(block.lambda)).pow(-1 * r)));
                  int coeff = coeffs[s] * ((int)Math.pow(block.lambda, -1 * r));
                  if (coeff != 0) {
                    summands.add(new Summand(coeff, s, block.lambda));
                  }
                }
              }
              Jn[block.startIndex + i][block.startIndex + j] = summands;
            }
          }
        }
      }

      // 5. Compute M = P * J^n
      List<Summand>[][] M = new ArrayList[d][d];
      for (int i = 0; i < d; i++) {
        for (int j = 0; j < d; j++) {
          List<Summand> acc = new ArrayList<>();
          for (int k = 0; k < d; k++) {
            if (P.getElement(i,j) != 0) {
              List<Summand> JnSummand = Jn[k][j];
              for (Summand summand : JnSummand) {
                if (summand.coeff != 0) {
                  acc.add(new Summand(summand.coeff * P.getElement(i,j), summand.power, summand.lambda));
                }
              }
            }
          }
          M[i][j] = acc;
        }
      }

      // 6. Compute A^n = M * Pinv
      List<Summand>[][] An = new ArrayList[d][d];
      for (int i = 0; i < d; i++) {
        for (int j = 0; j < d; j++) {
          List<Summand> acc = new ArrayList<>();
          for (int k = 0; k < d; k++) {
            if (Pinv.getElement(k,j) != 0) {
              List<Summand> JnSummand = M[i][k];
              for (Summand summand : JnSummand) {
                if (summand.coeff != 0) {
                  acc.add(new Summand(summand.coeff * Pinv.getElement(k,j), summand.power, summand.lambda));
                }
              }
            }
          }
          An[i][j] = acc;
        }
      }

      // 7. compute row summands: A^n * x
      ArrayList<ArrayList<RowSummand>> rowSummands = new ArrayList<>();
      for (int i = 0; i < d; i++) {
        ArrayList<RowSummand> row = new ArrayList<>();
        for (int j = 0; j < d; j++) {
          for (Summand summand : An[i][j]) {
            if (summand.coeff != 0) {
              row.add(new RowSummand(
                  summand.coeff,
                  x[j],
                  summand.power,
                  summand.lambda));
            }
          }
        }
        rowSummands.add(row);
      }

      // finished
      return Optional.of(rowSummands);
    } catch (Exception pE) {
      return Optional.empty();
    }
  }

  private record BlockInfo (
      int startIndex,
      int blockSize,
      int lambda
  ) {}

  private record Summand (
    int coeff,
    int power,
    int lambda
  ) {}

  private record RowSummand (
      int coeff,
      String variable,
      int power,
      int lambda
  ) {}

  private static CStatement calculateAcceleratedStatement(int pIterations, CStatement pStatement) {
    return pStatement;
  }

  private record TransformationData(
      int numberOfIterations,
      ProgramTransformationBehaviour behaviour,
      CFANode nodeBeforeLoop,
      CFANode nodeAfterLoop,
      List<CFAEdge> edgesToRemove,
      List<CStatement> assignments
  ) {}
}
