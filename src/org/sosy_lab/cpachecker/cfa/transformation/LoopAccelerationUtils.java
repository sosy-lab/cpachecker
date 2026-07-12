// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IAST;
import org.matheclipse.core.interfaces.IExpr;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.transformation.AffineLoopClosedFormRepresentation.RowSummand;
import org.sosy_lab.cpachecker.cfa.transformation.AffineLoopClosedFormRepresentation.Summand;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;

public class LoopAccelerationUtils {

  /**
   * Calculate the closed form of an affine loop A * x + b.
   * @param pLoop the affine loop as AffineLoopRepresentation
   * @return closed form as a matrix of RowSummands
   */
  public static Optional<AffineLoopClosedFormRepresentation> closedFormAffine(AffineLoopRepresentation pLoop) {
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

      Optional<AffineLoopClosedFormRepresentation> closedFormOptional = closedFormLinear(new AffineLoopRepresentation(Ahom.build(), xhom.build(), ImmutableList.of()));

      if(closedFormOptional.isPresent()) {
        // drop the last row & remove occurrences of x_fresh
        AffineLoopClosedFormRepresentation closedForm = closedFormOptional.orElseThrow();
        return Optional.of(closedForm.withoutVariable(x_fresh));
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
  public static Optional<AffineLoopClosedFormRepresentation> closedFormLinear(AffineLoopRepresentation pLoop) {
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
      int d = pLoop.getVariables().size();
      boolean negativeEigenvalue = false;

      // 2. compute information about every jordan block of J
      List<BlockInfo> blockInfos = new ArrayList<>();
      int m = 1;
      for (int i = 0; i < d; i = i + m) {
        IExpr lambda = util.eval(getMatrixEntry(J2, i, i));
        if (lambda.lessThan(0).isTrue()) {
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
      AffineLoopClosedFormRepresentation.Builder closedFormBuilder = new AffineLoopClosedFormRepresentation.Builder(pLoop.getVariables());
      for (BlockInfo block : blockInfos) {
        for (int i = 0; i < block.blockSize; i++) {
          for (int j = 0; j < block.blockSize; j++) {
            int r = j - i;
            if (r >= 0) {
              IExpr poly;
              poly = util.eval("1");
              if (r != 0) {
                for (int k = 0; k < r; k++) {
                  poly = poly.times(n.subtract(util.eval(String.valueOf(k))));
                }
                poly = poly.divide(util.eval(String.valueOf(r)).factorial());
                poly = F.evalSimplify(poly);
              }
              // TODO check if this is correct
              int deg = r;

              if (!block.lambda.isZero()) {
                IExpr coeffList = util.eval(F.CoefficientList(poly, n));
                for (int s = 0; s < deg + 1; s++) {
                  IExpr coeff = coeffList.getAt(s+1).multiply(block.lambda.pow(-1 * r));
                  if (!coeff.isZero()) {
                    closedFormBuilder.addSummand(new Summand(coeff, s, block.lambda), block.startIndex + i, block.startIndex + j);
                  }
                }
              }
            }
          }
        }
      }

      // 5. Compute M = P * J^n
      closedFormBuilder.initTmpMatrix();
      for (int i = 0; i < d; i++) {
        for (int j = 0; j < d; j++) {
          List<Summand> acc = new ArrayList<>();
          for (int k = 0; k < d; k++) {
            if (! getMatrixEntry(P2, i, k).isZero()) {
              List<Summand> JnSummand = closedFormBuilder.getSummands(k, j);
              for (Summand summand : JnSummand) {
                if (!summand.coeff().isZero()) {
                  acc.add(new Summand(summand.coeff().multiply(getMatrixEntry(P2, i, k)), summand.power(), summand.lambda()));
                }
              }
            }
          }
          closedFormBuilder.setTmpSummands(acc, i, j);
        }
      }
      closedFormBuilder.setTmpMatrix();

      // 6. Compute A^n = M * Pinv
      closedFormBuilder.initTmpMatrix();
      for (int i = 0; i < d; i++) {
        for (int j = 0; j < d; j++) {
          List<Summand> acc = new ArrayList<>();
          for (int k = 0; k < d; k++) {
            if (! getMatrixEntry(Pinv2, k,j).isZero()) {
              List<Summand> JnSummand = closedFormBuilder.getSummands(i, k);
              for (Summand summand : JnSummand) {
                if (!summand.coeff().isZero()) {
                  acc.add(new Summand(summand.coeff().multiply(getMatrixEntry(Pinv2, k, j)), summand.power(), summand.lambda()));
                }
              }
            }
          }
          closedFormBuilder.setTmpSummands(acc, i, j);
        }
      }
      closedFormBuilder.setTmpMatrix();

      // 7. compute row summands: A^n * x

      // finished
      return Optional.of(closedFormBuilder.build());
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
   * @param rowSummand list of RowSummands which get assigned to the same variable
   * @return a list of Coefficients (int, variable)
   */
  public static List<Coefficient> simplifyClosedFormAssignment(int n, List<RowSummand> rowSummand) {
    ImmutableList.Builder<Coefficient> coefficients = new ImmutableList.Builder<>();
    ArrayList<RowSummand> tmpList = new ArrayList<>();
    ExprEvaluator util = new ExprEvaluator(false, (short) 100);
    for (RowSummand summand : rowSummand) {
      IExpr coeff = summand.coeff().multiply((util.eval(String.valueOf(n)).pow(summand.power())).multiply(summand.lambda().pow(n)));
      boolean alreadyPresent = false;
      for (RowSummand summand2 : tmpList) {
        if (summand2.variable() == summand.variable()) {
          tmpList.add(new RowSummand(summand2.coeff().plus(coeff), summand.variable(), 0, null));
          tmpList.remove(summand2);
          alreadyPresent = true;
        }
      }
      if (!alreadyPresent) {
        tmpList.add(new RowSummand(coeff, summand.variable(), 0, null));
      }
    }

    for (RowSummand summand : tmpList) {
        coefficients.add(new Coefficient(summand.coeff().toIntDefault(), summand.variable()));
    }

    return coefficients.build();
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
