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
import java.util.Set;
import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IAST;
import org.matheclipse.core.interfaces.IExpr;
import org.matheclipse.core.interfaces.ISymbol;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.transformation.AffineLoopClosedFormRepresentation.Summand;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;

public class LoopAccelerationUtils {

  /**
   * Calculate the closed form of an affine loop A * x + b.
   *
   * @param pLoop the affine loop as AffineLoopRepresentation
   * @return closed form as a matrix of RowSummands
   */
  public static Optional<AffineLoopClosedFormRepresentation> closedFormAffine(
      AffineLoopRepresentation pLoop) {
    List<List<Integer>> A = (List<List<Integer>>) pLoop.getIterationMatrix();
    List<CIdExpression> x = pLoop.getVariables();
    List<Integer> b = pLoop.getIterationConstants();

    if (b.isEmpty()) return closedFormLinear(pLoop);
    try {
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

      CIdExpression x_fresh =
          new CIdExpression(
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
                  null));
      ImmutableList.Builder<CIdExpression> xhom = new ImmutableList.Builder<>();
      xhom.addAll(x);
      xhom.add(x_fresh);

      Optional<AffineLoopClosedFormRepresentation> closedFormOptional =
          closedFormLinear(
              new AffineLoopRepresentation(Ahom.build(), xhom.build(), ImmutableList.of()));

      if (closedFormOptional.isPresent()) {
        // drop the last row & remove occurrences of x_fresh
        AffineLoopClosedFormRepresentation closedForm = closedFormOptional.orElseThrow();
        return Optional.of(closedForm.withoutVariable(x_fresh));
      } else {
        return Optional.empty();
      }

    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Calculate the closed form of a linear loop A * x.
   *
   * @param pLoop the linear loop as AffineLoopRepresentation with an empty iteration constants list
   * @return closed form as a matrix of RowSummands
   */
  public static Optional<AffineLoopClosedFormRepresentation> closedFormLinear(
      AffineLoopRepresentation pLoop) {
    if (!pLoop.getIterationConstants().isEmpty()) return closedFormAffine(pLoop);
    ExprEvaluator util = new ExprEvaluator(false, (short) 100);
    try {
      IExpr n = util.eval("n");

      // 1. calculate the jordan form of the loop: A = P * J * Pinv
      IExpr A = util.eval(pLoop.printMatrix());
      IExpr jordanForm = util.eval(F.JordanDecomposition(A));
      IExpr P = jordanForm.first();
      IExpr J = jordanForm.last();
      IExpr Pinv = util.eval(F.Inverse(P));
      int d = pLoop.getVariables().size();
      boolean negativeEigenvalue = false;

      // 2. compute information about every jordan block of J
      List<BlockInfo> blockInfos = new ArrayList<>();
      int m = 1;
      for (int i = 0; i < d; i = i + m) {
        IExpr lambda = util.eval(getMatrixEntry(J, i, i));
        // only support -1, 0, 1 as eigenvalues
        if (!(lambda.isMinusOne() || lambda.isOne() || lambda.isZero())) {
          return Optional.empty();
        }
        m = 1;
        while (i + m < d && util.eval(getMatrixEntry(J, i + m - 1, i + m)).isOne()) {
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
      AffineLoopClosedFormRepresentation.Builder closedFormBuilder =
          new AffineLoopClosedFormRepresentation.Builder(pLoop.getVariables());
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
                  IExpr coeff = coeffList.getAt(s + 1).multiply(block.lambda.pow(-1 * r));
                  if (!coeff.isZero()) {
                    closedFormBuilder.addSummand(
                        new Summand(coeff, s, block.lambda),
                        block.startIndex + i,
                        block.startIndex + j);
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
            if (!getMatrixEntry(P, i, k).isZero()) {
              List<Summand> JnSummand = closedFormBuilder.getSummands(k, j);
              for (Summand summand : JnSummand) {
                if (!summand.coeff().isZero()) {
                  acc.add(
                      new Summand(
                          summand.coeff().multiply(getMatrixEntry(P, i, k)),
                          summand.power(),
                          summand.lambda()));
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
            if (!getMatrixEntry(Pinv, k, j).isZero()) {
              List<Summand> JnSummand = closedFormBuilder.getSummands(i, k);
              for (Summand summand : JnSummand) {
                if (!summand.coeff().isZero()) {
                  acc.add(
                      new Summand(
                          summand.coeff().multiply(getMatrixEntry(Pinv, k, j)),
                          summand.power(),
                          summand.lambda()));
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

  private record BlockInfo(int startIndex, int blockSize, IExpr lambda) {}

  /**
   * Representation of a single coefficient, variable pair.
   *
   * @param coeff int
   * @param variable CIdExpression
   */
  public record Coefficient(int coeff, CIdExpression variable) {}

  private static IExpr getMatrixEntry(IExpr matrix, int i, int j) {
    IAST m = (IAST) matrix;
    return m.get(i + 1).get(j + 1);
  }

  /**
   * This function tries to calculate CExpressions for a number of loop iterations for which the
   * guard becomes true/false. Only for loop guards of the form a (< | <= |!= | > | >=) b where a
   * and b are terms in integer arithmetic.
   *
   * @param pLoopCondition the loop guard as CExpression
   * @param pClosedForm the closed form representation of the loop
   * @return an Optional containing a List of possible loop iteration CExpressions on success or
   *     Optional.empty()
   */
  public static Optional<ImmutableList<CExpression>> getNumberOfIterations(
      CExpression pLoopCondition, AffineLoopClosedFormRepresentation pClosedForm) {
    ExprEvaluator util = new ExprEvaluator();
    IExpr symbolicExpression;
    ImmutableList<BinaryOperator> supportedBinaryOperators =
        ImmutableList.of(
            BinaryOperator.EQUALS,
            BinaryOperator.GREATER_EQUAL,
            BinaryOperator.GREATER_THAN,
            BinaryOperator.LESS_EQUAL,
            BinaryOperator.LESS_THAN,
            BinaryOperator.NOT_EQUALS);
    ImmutableList.Builder<CIdExpression> encounteredVariables = ImmutableList.builder();

    // First create a symbolic expression for the falsified loop guard
    // Only supporting loop guards: arithm. expression <,<=,!=,>,>= arithm. expression
    switch (pLoopCondition) {
      case CBinaryExpression binaryExpression:
        if (!supportedBinaryOperators.contains(binaryExpression.getOperator()))
          return Optional.empty();
        IntegerArithmaticExpressionVisitor lhsVisitor = new IntegerArithmaticExpressionVisitor();
        IntegerArithmaticExpressionVisitor rhsVisitor = new IntegerArithmaticExpressionVisitor();
        try {
          IExpr lhs = lhsVisitor.visit(binaryExpression.getOperand1());
          IExpr rhs = rhsVisitor.visit(binaryExpression.getOperand2());
          symbolicExpression =
              switch (binaryExpression.getOperator()) {
                case LESS_THAN -> F.Equal(lhs, rhs);
                case LESS_EQUAL -> F.Equal(lhs, F.Plus(rhs, F.C1));
                case GREATER_THAN -> F.Equal(lhs, rhs);
                case GREATER_EQUAL -> F.Equal(lhs, F.Subtract(rhs, F.C1));
                case NOT_EQUALS -> F.Equal(lhs, rhs);
                default -> null;
              };
          if (symbolicExpression == null || lhsVisitor.failed() || rhsVisitor.failed()) {
            return Optional.empty();
          }
          encounteredVariables.addAll(lhsVisitor.getEncounteredVariables());
          encounteredVariables.addAll(rhsVisitor.getEncounteredVariables());
        } catch (Exception pE) {
          return Optional.empty();
        }
        break;
      case CIdExpression idExpression:
        IntegerArithmaticExpressionVisitor idVisitor = new IntegerArithmaticExpressionVisitor();
        try {
          symbolicExpression = idVisitor.visit(idExpression);
          if (idVisitor.failed()) return Optional.empty();
          // the proposition a is equal to a != 0
          symbolicExpression = symbolicExpression.unequalTo(F.C0);
          encounteredVariables.addAll(idVisitor.getEncounteredVariables());
        } catch (Exception pE) {
          return Optional.empty();
        }
        break;
      case CIntegerLiteralExpression literalExpression:
        IntegerArithmaticExpressionVisitor literalVisitor =
            new IntegerArithmaticExpressionVisitor();
        try {
          symbolicExpression = literalVisitor.visit(literalExpression);
          if (literalVisitor.failed()) return Optional.empty();
          // a constant integer != 0 as loop guard results in an infinite loop
          if (!symbolicExpression.isZero()) return Optional.empty();
          // a constant 0 means we never enter the loop
          return Optional.of(ImmutableList.of(literalExpression));
        } catch (Exception pE) {
          return Optional.empty();
        }
      default:
        return Optional.empty();
    }

    // insert closed form of accelerated variables
    ISymbol iterVar = F.$s("LOOPACCELERATIONITERATIONS");
    for (CIdExpression var : encounteredVariables.build()) {
      IExpr assignment = pClosedForm.assignmentSymbolicExpression(var, iterVar);
      symbolicExpression = util.eval(F.ReplaceAll(symbolicExpression, assignment));
    }

    // two cases: n is even or odd
    // Relevant because of the term (-1)^n
    ISymbol kEven = F.$s("kEven");
    ISymbol kOdd = F.$s("kOdd");
    IExpr kEvenIdentity = F.Rule(iterVar, F.Times(F.C2, kEven));
    IExpr kOddIdentity = F.Rule(iterVar, F.Plus(F.C1, F.Times(F.C2, kOdd)));
    IExpr newRuleEven = F.Rule(F.Power(F.CN1, iterVar), F.C1);
    IExpr newRuleOdd = F.Rule(F.Power(F.CN1, iterVar), F.CN1);
    IExpr symbolicExpressionEven = util.eval(F.ReplaceAll(symbolicExpression, newRuleEven));
    IExpr symbolicExpressionOdd = util.eval(F.ReplaceAll(symbolicExpression, newRuleOdd));
    symbolicExpressionEven = util.eval(F.ReplaceAll(symbolicExpressionEven, kEvenIdentity));
    symbolicExpressionOdd = util.eval(F.ReplaceAll(symbolicExpressionOdd, kOddIdentity));

    // solve for n_even/n_odd
    // with Solve as String input, because F.Solve behaves differently
    IExpr solutionEven = util.eval("Solve(" + symbolicExpressionEven + ", " + kEven + ")");
    IExpr solutionOdd = util.eval("Solve(" + symbolicExpressionOdd + ", " + kOdd + ")");

    // reinsert n for n_even = n/2, n_odd = (n-1)/2
    ImmutableList.Builder<IExpr> solutionsBuilder = ImmutableList.builder();
    if (solutionEven.first().isEmpty() && solutionOdd.first().isEmpty()) {
      // cannot determine the number of iterations
      return Optional.empty();
    } else {
      // collect all possible solutions
      IExpr solutionsEven = solutionEven.first();
      if (solutionsEven.isListOfRules()) {
        while (!solutionsEven.isEmpty()) {
          IExpr solution = solutionsEven.first();
          solutionsBuilder.add(util.eval(F.Times(F.C2, solution.second())));
          solutionsEven = solutionsEven.rest();
        }
      }
      IExpr solutionsOdd = solutionOdd.first();
      if (solutionsOdd.isListOfRules()) {
        while (!solutionsOdd.isEmpty()) {
          IExpr solution = solutionsOdd.first();
          solutionsBuilder.add(
              util.eval(F.Simplify(F.Times(F.C2, F.Plus(solution.second(), F.C1)))));
          solutionsOdd = solutionsOdd.rest();
        }
      }
      // create CExpressions for all possible solutions
      ImmutableList.Builder<CExpression> possibleSolutions = ImmutableList.builder();
      ImmutableList<IExpr> solutions = solutionsBuilder.build();
      if (solutions.isEmpty()) return Optional.empty();
      for (IExpr solution : solutions) {
        Optional<CExpression> expression =
            expressionFromIExpr(solution, pClosedForm.getVariables());
        if (expression.isPresent()) {
          possibleSolutions.add(expression.orElseThrow());
        }
      }
      return Optional.of(possibleSolutions.build());
    }
  }

  public static Optional<CExpression> expressionFromIExpr(
      IExpr pExpression, Set<CIdExpression> pVariables) {
    if (pExpression.isNumber()) {
      if (pExpression.isInteger()) {
        return Optional.of(
            new CIntegerLiteralExpression(
                FileLocation.DUMMY,
                CNumericTypes.INT,
                BigInteger.valueOf(pExpression.toLongDefault())));
      } else if (pExpression.isRational() || pExpression.isReal()) {
        return Optional.of(
            new CFloatLiteralExpression(
                FileLocation.DUMMY,
                MachineModel.LINUX64,
                CNumericTypes.DOUBLE,
                FloatValue.fromDouble(pExpression.toDoubleDefault())));
      }
      return Optional.empty();
    } else if (pExpression.isSymbol()) {
      for (CIdExpression var : pVariables) {
        if (var.getName().equals(pExpression.toString())) {
          return Optional.of(var);
        }
      }
      return Optional.empty();
    } else if (pExpression.isPlus() || pExpression.isTimes()) {
      if (pExpression.argSize() != 2) return Optional.empty();
      Optional<CExpression> lhs = expressionFromIExpr(pExpression.first(), pVariables);
      Optional<CExpression> rhs = expressionFromIExpr(pExpression.second(), pVariables);
      if (lhs.isEmpty() || rhs.isEmpty()) return Optional.empty();
      CBinaryExpressionBuilder expressionBuilder =
          new CBinaryExpressionBuilder(MachineModel.LINUX64, LogManager.createNullLogManager());
      CBinaryExpression cBinaryExpression;
      try {
        cBinaryExpression =
            expressionBuilder.buildBinaryExpression(
                lhs.orElseThrow(),
                rhs.orElseThrow(),
                pExpression.isPlus() ? BinaryOperator.PLUS : BinaryOperator.MULTIPLY);
      } catch (UnrecognizedCodeException pE) {
        return Optional.empty();
      }
      return Optional.of(cBinaryExpression);
    }
    return Optional.empty();
  }

  /**
   * For two int expressions a b return the smaller number of a,b greater 0 or 0 if a <= 0 and b <=
   * 0.
   *
   * @param pIntExpression1 CExpression with type int
   * @param pIntExpression2 CExpression with type int
   * @return a Cexpression equivalent to min(a,b), for a,b > 0
   * @throws UnrecognizedCodeException
   */
  public static CExpression minOfTwoIntGreaterZero(
      CExpression pIntExpression1, CExpression pIntExpression2) throws UnrecognizedCodeException {
    CBinaryExpressionBuilder binaryExpressionBuilder =
        new CBinaryExpressionBuilder(MachineModel.LINUX64, LogManager.createNullLogManager());
    return binaryExpressionBuilder.buildBinaryExpression(
        binaryExpressionBuilder.buildBinaryExpression(
            // (a > 0) * (b <= 0) * a
            binaryExpressionBuilder.buildBinaryExpression(
                binaryExpressionBuilder.buildBinaryExpression(
                    pIntExpression1,
                    new CIntegerLiteralExpression(
                        FileLocation.DUMMY, CNumericTypes.INT, BigInteger.ZERO),
                    BinaryOperator.GREATER_THAN),
                binaryExpressionBuilder.buildBinaryExpression(
                    binaryExpressionBuilder.buildBinaryExpression(
                        pIntExpression2,
                        new CIntegerLiteralExpression(
                            FileLocation.DUMMY, CNumericTypes.INT, BigInteger.ZERO),
                        BinaryOperator.LESS_EQUAL),
                    pIntExpression1,
                    BinaryOperator.MULTIPLY),
                BinaryOperator.MULTIPLY),
            // (a > 0) * (b > 0) * (a <= b) * a
            binaryExpressionBuilder.buildBinaryExpression(
                binaryExpressionBuilder.buildBinaryExpression(
                    pIntExpression1,
                    new CIntegerLiteralExpression(
                        FileLocation.DUMMY, CNumericTypes.INT, BigInteger.ZERO),
                    BinaryOperator.GREATER_THAN),
                binaryExpressionBuilder.buildBinaryExpression(
                    binaryExpressionBuilder.buildBinaryExpression(
                        pIntExpression2,
                        new CIntegerLiteralExpression(
                            FileLocation.DUMMY, CNumericTypes.INT, BigInteger.ZERO),
                        BinaryOperator.GREATER_THAN),
                    binaryExpressionBuilder.buildBinaryExpression(
                        binaryExpressionBuilder.buildBinaryExpression(
                            pIntExpression1, pIntExpression2, BinaryOperator.LESS_EQUAL),
                        pIntExpression1,
                        BinaryOperator.MULTIPLY),
                    BinaryOperator.MULTIPLY),
                BinaryOperator.MULTIPLY),
            BinaryOperator.PLUS),
        binaryExpressionBuilder.buildBinaryExpression(
            // (b > 0) * (a <= 0) * b
            binaryExpressionBuilder.buildBinaryExpression(
                binaryExpressionBuilder.buildBinaryExpression(
                    pIntExpression2,
                    new CIntegerLiteralExpression(
                        FileLocation.DUMMY, CNumericTypes.INT, BigInteger.ZERO),
                    BinaryOperator.GREATER_THAN),
                binaryExpressionBuilder.buildBinaryExpression(
                    binaryExpressionBuilder.buildBinaryExpression(
                        pIntExpression1,
                        new CIntegerLiteralExpression(
                            FileLocation.DUMMY, CNumericTypes.INT, BigInteger.ZERO),
                        BinaryOperator.LESS_EQUAL),
                    pIntExpression2,
                    BinaryOperator.MULTIPLY),
                BinaryOperator.MULTIPLY),
            // (b > 0) * (a > 0) * (b < a) * b
            binaryExpressionBuilder.buildBinaryExpression(
                binaryExpressionBuilder.buildBinaryExpression(
                    pIntExpression2,
                    new CIntegerLiteralExpression(
                        FileLocation.DUMMY, CNumericTypes.INT, BigInteger.ZERO),
                    BinaryOperator.GREATER_THAN),
                binaryExpressionBuilder.buildBinaryExpression(
                    binaryExpressionBuilder.buildBinaryExpression(
                        pIntExpression1,
                        new CIntegerLiteralExpression(
                            FileLocation.DUMMY, CNumericTypes.INT, BigInteger.ZERO),
                        BinaryOperator.GREATER_THAN),
                    binaryExpressionBuilder.buildBinaryExpression(
                        binaryExpressionBuilder.buildBinaryExpression(
                            pIntExpression2, pIntExpression1, BinaryOperator.LESS_THAN),
                        pIntExpression2,
                        BinaryOperator.MULTIPLY),
                    BinaryOperator.MULTIPLY),
                BinaryOperator.MULTIPLY),
            BinaryOperator.PLUS),
        BinaryOperator.PLUS);
  }
}
