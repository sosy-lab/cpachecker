// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hipparchus.linear.RealMatrix;
import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IAST;
import org.matheclipse.core.interfaces.IExpr;
import org.matheclipse.core.interfaces.IExpr.COMPARE_TERNARY;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;

public class LoopAccelerationUtils {

  public static Optional<ArrayList<ArrayList<RowSummand>>> closedFormAffine(Matrix A, int[] b, String[] x) {
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
                  ? new RowSummand(summand.coeff, "", summand.power, summand.lambda)
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
   *
   * @param A homogenous iteration matrix
   * @param x array of variable names
   * @return
   */
  public static Optional<ArrayList<ArrayList<RowSummand>>> closedFormLinear(Matrix A, String[] x) {
    assert (A.getRowNum() == A.getColumnNum()) : "Error: A is not a square matrix!";
    assert (A.getRowNum() == x.length);

    ExprEvaluator util = new ExprEvaluator(false, (short) 100);
    try{
      IExpr n = util.eval("n");

      // 1. calculate the jordan form of the loop: A = P * J * Pinv
      IExpr jordanForm = util.eval("JordanDecomposition(" + A + ")");
      //assert (jordanForm.isListOfMatrices() && jordanForm.size() == 2) : "Error: jordan decomposition failed in LoopAccelerationProgramTransformation!";
      Matrix P = Matrix.createMatrix(jordanForm.first().toIntMatrix());
      Matrix J = Matrix.createMatrix(jordanForm.last().toIntMatrix());
      RealMatrix Pinv = util.eval("Inverse("+P+")").toRealMatrix();
      IExpr P2 = jordanForm.first();
      IExpr J2 = jordanForm.last();
      IExpr Pinv2 = util.eval(F.Inverse(P2));
      int d = A.getColumnNum();
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
            if (P.getElement(i,k) != 0) {
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
            //if (Pinv.getElement(k,j) != 0) {
            if (Pinv.getEntry(k,j) != 0) {
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
      String variable,
      int power,
      IExpr lambda
  ) {}

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
  public static List<Coefficient> simplifyClosedFormAssignment(int n, List<RowSummand> rowSummand, CIdExpression[] variables) {
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
      if (summand.variable.isEmpty()) {
        coefficients.add(new Coefficient(summand.coeff.toIntDefault(), null));
      } else {
        for (CIdExpression variable : variables) {
          if (variable.getName().equals(summand.variable)) {
            coefficients.add(new Coefficient(summand.coeff.toIntDefault(), variable));
            break;
          }
        }
      }
    }

    return coefficients;
  }

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
