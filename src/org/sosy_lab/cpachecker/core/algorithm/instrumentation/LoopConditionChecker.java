package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.instrumentation.InstrumentationAutomaton.InstrumentationProperty;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.checkerframework.checker.units.qual.C;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression.ABinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;

public class LoopConditionChecker {

  public static VariableBoundInfo
      distanceCompatible(CFANode loopHead, InstrumentationProperty property) {
    switch (property) {
      case DISTANCE:
        return distanceCompatibleBase(loopHead, false, false);

      case DISTANCE2:
        return distanceCompatibleBase(loopHead, true, false);

      case DISTANCE3:
        return distanceCompatibleBase(loopHead, true, true);

      default:
        // TODO Throw exception
        break;
    }
    return null;
  }

  private static VariableBoundInfo distanceCompatibleBase(
      CFANode loopHead,
      boolean hasBinaryExpressions,
      boolean hasMultipleVariables) {
    CFAEdge firstLeavingEdge = loopHead.getLeavingEdge(0);

    if (firstLeavingEdge instanceof AssumeEdge assumeEdge) {
      AExpression expression = assumeEdge.getExpression();
      // ==================//
      // Language C //
      // ==================//
      if (expression instanceof CBinaryExpression binaryExpression) {
        BinaryOperator operator = binaryExpression.getOperator();

        if (operator == BinaryOperator.LESS_THAN
            || operator == BinaryOperator.GREATER_THAN
            || operator == BinaryOperator.LESS_EQUAL
            || operator == BinaryOperator.GREATER_EQUAL) {

          CExpression operand1 = binaryExpression.getOperand1();
          CExpression operand2 = binaryExpression.getOperand2();

          String leftVar;
          String rightVar;
          if (hasBinaryExpressions) {
            leftVar = extractCVariable(operand1, hasMultipleVariables);
            rightVar = extractCVariable(operand2, hasMultipleVariables);

            if (leftVar != null && rightVar != null) {
              return new VariableBoundInfo(leftVar, rightVar);
            } else if (leftVar != null) {
              rightVar = extractCConstant(operand2);
              if (rightVar != null) {
                return new VariableBoundInfo(leftVar, rightVar);
              }
            } else if (rightVar != null) {
              leftVar = extractCConstant(operand1);
              if (leftVar != null) {
                return new VariableBoundInfo(leftVar, rightVar);
              }
            }
          } else {
            if ((operand1 instanceof CIdExpression cVarExp1)
                && (operand2 instanceof CIdExpression cVarExp2)) {
              return new VariableBoundInfo(cVarExp1.getName(), cVarExp2.getName());
            } else if ((operand1 instanceof CLiteralExpression cVarExp1)
                && (operand2 instanceof CIdExpression cVarExp2)) {
              return new VariableBoundInfo(extractCConstant(cVarExp1), cVarExp2.getName());
            } else if ((operand1 instanceof CIdExpression cVarExp1)
                && (operand2 instanceof CLiteralExpression cVarExp2)) {
              return new VariableBoundInfo(cVarExp1.getName(), extractCConstant(cVarExp2));
            }
          }

        }

      }
    }
    return null;
  }

  private static String extractCConstant(CExpression expression) {
    // ======================//
    // Supported DataTypes //
    // -Integer //
    // -Float //
    // ======================//
    if (expression instanceof CIntegerLiteralExpression integerExpression) {
      return integerExpression.toASTString();
    }
    if (expression instanceof CFloatLiteralExpression floatExpression) {
      return floatExpression.toASTString();
    }
    if (expression instanceof CBinaryExpression binaryExpression) {
      CExpression operandLeft = binaryExpression.getOperand1();
      CExpression operandRight = binaryExpression.getOperand2();
      BinaryOperator operator = binaryExpression.getOperator();

      //
      if (operator == BinaryOperator.PLUS
          || operator == BinaryOperator.MINUS
          || operator == BinaryOperator.MULTIPLY
          || operator == BinaryOperator.DIVIDE) {
        String left = extractCConstant(operandLeft);
        String right = extractCConstant(operandRight);
        if ((left != null) && (right != null)) {
          return "(" + left + " " + operator.getOperator() + " " + right + ")";
        }
      }
    }
    return null;
  }

  private static String extractCVariable(CExpression expression, boolean hasMultipleVariables) {
    if (expression instanceof CIdExpression idExpression) {
      return idExpression.getName();
    }
    if (expression instanceof CBinaryExpression binaryExpression) {
      CExpression left = binaryExpression.getOperand1();
      CExpression right = binaryExpression.getOperand2();
      BinaryOperator operator = binaryExpression.getOperator();

      if ((operator == BinaryOperator.PLUS
          || operator == BinaryOperator.MINUS
          || operator == BinaryOperator.MULTIPLY
          || operator == BinaryOperator.DIVIDE)) {
        String leftVar = extractCVariable(left, hasMultipleVariables);
        String rightVar = extractCVariable(right, hasMultipleVariables);

        if (hasMultipleVariables) {
          if ((leftVar != null) && (rightVar != null)) {
            return "(" + leftVar + " " + operator.getOperator() + " " + rightVar + ")";
          } else {
            if (leftVar != null) {
              rightVar = extractCConstant(right);
              if (rightVar != null) {
                return "(" + leftVar + " " + operator.getOperator() + " " + rightVar + ")";
              }
            }
            if (rightVar != null) {
              leftVar = extractCConstant(left);
              if (leftVar != null) {
                return "(" + leftVar + " " + operator.getOperator() + " " + rightVar + ")";
              }
            }
          }
        } else {
          if ((leftVar != null) ^ (rightVar != null)) {
            if (leftVar != null) {
              rightVar = extractCConstant(right);
              if (rightVar != null) {
                return "(" + leftVar + " " + operator.getOperator() + " " + rightVar + ")";
              }
            }
            if (rightVar != null) {
              leftVar = extractCConstant(left);
              if (leftVar != null) {
                return "(" + leftVar + " " + operator.getOperator() + " " + rightVar + ")";
              }
            }
          }

        }

      }
    }
    return null;
  }

  public static class VariableBoundInfo {
    private final String var1;
    private final String var2;

    public VariableBoundInfo(String var1, String var2) {
      this.var1 = var1;
      this.var2 = var2;
    }

    public String getVar1() {
      return var1;
    }

    public String getVar2() {
      return var2;
    }

    @Override
    public String toString() {
      return "VariableBoundInfo{var1='" + var1 + "', var2='" + var2 + "'}";
    }
  }
}
