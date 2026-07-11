// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.hipparchus.linear.RealMatrix;
import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IAST;
import org.matheclipse.core.interfaces.IExpr;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;

public class LoopAccelerationUtils {

  /**
   * Calculate the closed form of an affine loop A * x + b.
   * @param pLoop the affine loop as AffineLoopRepresentation
   * @return closed form as a matrix of RowSummands
   */
  public static Optional<ArrayList<ArrayList<RowSummand>>> closedFormAffine(AffineLoopRepresentation pLoop) {
    List<List<Integer>> A = (List<List<Integer>>) pLoop.getIteraionMatrix();
    List<CIdExpression> x = pLoop.getVariables();
    List<Integer> b = pLoop.getIterationConstants();

    if (b.isEmpty()) return closedFormLinear(pLoop);
    ExprEvaluator util = new ExprEvaluator(false, (short) 100);
    try{
      int d = A.size();

      // make A * x + b homogenous
      ImmutableList.Builder<ImmutableList<Integer>> Ahom = new ImmutableList.Builder<>();
      for (int i = 0; i < d; i++) {
        ImmutableList.Builder<Integer> newRow = new ImmutableList.Builder<>();
        newRow.addAll(A.get(i));
        newRow.add(b.get(i));
        Ahom.add(newRow.build());
      }
      ImmutableList.Builder<Integer> extraRow = new ImmutableList.Builder<>();
      extraRow.addAll(Collections.nCopies(d, 0));
      extraRow.add(1);
      Ahom.add(extraRow.build());


      CIdExpression x_fresh = new CIdExpression(
          FileLocation.DUMMY,
          CNumericTypes.INT,
          "x_fresh",
          new CVariableDeclaration(
              FileLocation.DUMMY,
              true,
              CStorageClass.AUTO,
              CNumericTypes.INT,
              "x_fresh",
              "x_fresh",
              "x_fresh",
              null)
      );
      ImmutableList.Builder<CIdExpression> xhom = new ImmutableList.Builder<>();
      xhom.addAll(x);
      xhom.add(x_fresh);

      Optional<ArrayList<ArrayList<RowSummand>>> closedFormOptional = closedFormLinear(new AffineLoopRepresentation(Ahom.build(), xhom.build(), ImmutableList.of()));

      if(closedFormOptional.isPresent()) {
        // drop the last row
        ArrayList<ArrayList<RowSummand>> closedForm = closedFormOptional.orElseThrow();
        closedForm.removeLast();
        // remove occurrences of x_fresh
        for (ArrayList<RowSummand> row : closedForm) {
          row.replaceAll(
              (RowSummand summand) ->
                  summand.variable == x_fresh
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

  /**
   * Calculate the closed form of a linear loop A * x.
   * @param pLoop the linear loop as AffineLoopRepresentation with an empty iteration constants list
   * @return closed form as a matrix of RowSummands
   */
  public static Optional<ArrayList<ArrayList<RowSummand>>> closedFormLinear(AffineLoopRepresentation pLoop) {
    if (! pLoop.getIterationConstants().isEmpty()) return closedFormAffine(pLoop);
    ExprEvaluator util = new ExprEvaluator(false, (short) 100);
    try{
      IExpr n = util.eval("n");

      // 1. calculate the jordan form of the loop: A = P * J * Pinv
      IExpr A = util.eval(pLoop.printMatrix());
      IExpr jordanForm = util.eval(F.JordanDecomposition(A));
      IExpr P2 = jordanForm.first();
      IExpr J2 = jordanForm.last();
      IExpr Pinv2 = util.eval(F.Inverse(P2));
      int d = A.size();
      boolean negativeEigenvalue = false;

      // 2. compute information about every jordan block of J
      List<BlockInfo> blockInfos = new ArrayList<>();
      int m = 1;
      for (int i = 0; i < d; i = i + m) {
        IExpr lambda = util.eval(getMatrixEntry(J2, i, i));
        if (lambda.less(util.eval("0")).isTrue()) {
          negativeEigenvalue = true;
        }
        m = 1;
        while (i + m < d &&
            util.eval(getMatrixEntry(J2, i + m - 1, i + m)).isOne()) {
          m++;
        }
        blockInfos.add(new BlockInfo(i, m, lambda));
      }

      // 3. N0 = maximal size among zero eigenvalue blocks
      int N0 = 0;
      for (BlockInfo block : blockInfos) {
        if (block.lambda.isZero()) {
          N0 = Math.max(N0, block.blockSize);
        }
      }

      // 4. Compute J^n
      ArrayList<Summand>[][] Jn = (ArrayList<Summand>[][]) new ArrayList[d][d];
      for (int i = 0; i < d; i++) {
        for (int j = 0; j < d; j++) {
          Jn[i][j] = new ArrayList<>();
        }
      }
      for (BlockInfo block : blockInfos) {
        for (int i = 0; i < block.blockSize; i++) {
          for (int j = 0; j < block.blockSize; j++) {
            int r = j - i;
            if (r >= 0) {
              IExpr poly;
              if (r == 0) {
                poly = util.eval("1");
              } else {
                poly = util.eval("1");
                for (int k = 0; k < r; k++) {
                  poly = poly.times(n.subtract(util.eval(String.valueOf(k))));
                }
                poly = poly.divide(util.eval(String.valueOf(r)).factorial());
                poly = F.evalSimplify(poly);
              }
              // TODO check if this is correct
              int deg = r;

              ArrayList<Summand> summands = new ArrayList<>();
              if (!block.lambda.isZero()) {
                IExpr coeffList = util.eval(F.CoefficientList(poly, n));
                for (int s = 0; s < deg + 1; s++) {
                  IExpr coeff = coeffList.getAt(s+1).multiply(block.lambda.pow(-1 * r));
                  if (!coeff.isZero()) {
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
      List<Summand>[][] M = (ArrayList<Summand>[][]) new ArrayList[d][d];
      for (int i = 0; i < d; i++) {
        for (int j = 0; j < d; j++) {
          M[i][j] = new ArrayList<>();
        }
      }
      for (int i = 0; i < d; i++) {
        for (int j = 0; j < d; j++) {
          List<Summand> acc = new ArrayList<>();
          for (int k = 0; k < d; k++) {
            if (! getMatrixEntry(P2, i, k).isZero()) {
              List<Summand> JnSummand = Jn[k][j];
              for (Summand summand : JnSummand) {
                if (!summand.coeff.isZero()) {
                  acc.add(new Summand(summand.coeff.multiply(getMatrixEntry(P2, i, k)), summand.power, summand.lambda));
                }
              }
            }
          }
          M[i][j] = acc;
        }
      }

      // 6. Compute A^n = M * Pinv
      List<Summand>[][] An = (ArrayList<Summand>[][]) new ArrayList[d][d];
      for (int i = 0; i < d; i++) {
        for (int j = 0; j < d; j++) {
          An[i][j] = new ArrayList<>();
        }
      }
      for (int i = 0; i < d; i++) {
        for (int j = 0; j < d; j++) {
          List<Summand> acc = new ArrayList<>();
          for (int k = 0; k < d; k++) {
            if (! getMatrixEntry(Pinv2, k,j).isZero()) {
              List<Summand> JnSummand = M[i][k];
              for (Summand summand : JnSummand) {
                if (!summand.coeff.isZero()) {
                  acc.add(new Summand(summand.coeff.multiply(getMatrixEntry(Pinv2, k, j)), summand.power, summand.lambda));
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
            if (!summand.coeff.isZero()) {
              row.add(new RowSummand(
                  summand.coeff,
                  pLoop.getVariables().get(j),
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
      IExpr lambda
  ) {}

  /**
   * Representation of a summand without variable: coeff * n^power * lambda^n
   * @param coeff
   * @param power
   * @param lambda
   */
  private record Summand (
      IExpr coeff,
      int power,
      IExpr lambda
  ) {}

  /**
   * Representation of a summand with the corresponding variable: coeff * variable * n^power * lambda^n
   * @param coeff
   * @param variable
   * @param power
   * @param lambda
   */
  public record RowSummand (
      IExpr coeff,
      CIdExpression variable,
      int power,
      IExpr lambda
  ) {}

  /**
   * Representation of a single coefficient, variable pair.
   * @param coeff int
   * @param variable CIdExpression
   */
  public record Coefficient (
      int coeff,
      CIdExpression variable
  ) {}

  private static IExpr getMatrixEntry(IExpr matrix, int i, int j) {
    IAST m = (IAST) matrix;
    return m.get(i + 1).get(j + 1);
  }

  /**
   * Calculate the exact value of a coefficient for n loop iterations.
   * @param n the number of loop iterations
   * @param rowSummand values from calculating the closed form
   * @return coefficient expression
   */
  public static List<Coefficient> simplifyClosedFormAssignment(int n, List<RowSummand> rowSummand, List<CIdExpression> variables) {
    ArrayList<Coefficient> coefficients = new ArrayList<>();
    ArrayList<RowSummand> tmpList = new ArrayList<>();
    ExprEvaluator util = new ExprEvaluator(false, (short) 100);
    for (RowSummand summand : rowSummand) {
      IExpr coeff = summand.coeff.multiply((util.eval(String.valueOf(n)).pow(summand.power)).multiply(summand.lambda.pow(n)));
      boolean alreadyPresent = false;
      for (RowSummand summand2 : tmpList) {
        if (summand2.variable.equals(summand.variable)) {
          tmpList.add(new RowSummand(summand2.coeff().plus(coeff), summand.variable, 0, null));
          tmpList.remove(summand2);
          alreadyPresent = true;
        }
      }
      if (!alreadyPresent) {
        tmpList.add(new RowSummand(coeff, summand.variable, 0, null));
      }
    }

    for (RowSummand summand : tmpList) {
      if (summand.variable == null) {
        coefficients.add(new Coefficient(summand.coeff.toIntDefault(), null));
      } else {
        for (CIdExpression variable : variables) {
          if (variable == summand.variable) {
            coefficients.add(new Coefficient(summand.coeff.toIntDefault(), variable));
            break;
          }
        }
      }
    }

    return coefficients;
  }

  /**
   * Create the CRightHandSide for a list of Coefficients.
   * @param coefficients list of coefficient, variable pairs
   * @return a CRightHandSide expression for an assignment statement
   */
  public static CExpression expressionFromCoefficients(List<Coefficient> coefficients) {
    int num = coefficients.size();
    if (num == 1) {
      // constant
      if (coefficients.getFirst().variable == null) {
        return new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            BigInteger.valueOf(coefficients.getFirst().coeff)
        );
      }
      return new CBinaryExpression(
          FileLocation.DUMMY,
          CNumericTypes.INT,
          CNumericTypes.INT,
          new CIntegerLiteralExpression(
              FileLocation.DUMMY,
              CNumericTypes.INT,
              BigInteger.valueOf(coefficients.getFirst().coeff)),
          coefficients.getFirst().variable,
          BinaryOperator.MULTIPLY);

    } else {
      return new CBinaryExpression(
          FileLocation.DUMMY,
          CNumericTypes.INT,
          CNumericTypes.INT,
          expressionFromCoefficients(coefficients.subList(0, 1)),
          expressionFromCoefficients(coefficients.subList(1, num)),
          BinaryOperator.PLUS
      );
    }
  }
}
