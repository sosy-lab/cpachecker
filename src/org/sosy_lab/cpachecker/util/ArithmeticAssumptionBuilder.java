package org.sosy_lab.cpachecker.util;

import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.simplification.ExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public abstract class ArithmeticAssumptionBuilder {

  @Option(description = "Track overflows in left-shift operations.")
  public boolean trackLeftShifts = true;

  @Option(description = "Track overflows in additive(+/-) operations.")
  public boolean trackAdditiveOperations = true;

  @Option(description = "Track overflows in multiplication operations.")
  public boolean trackMultiplications = true;

  @Option(description = "Track overflows in division(/ or %) operations.")
  public boolean trackDivisions = true;

  @Option(description = "Track overflows in binary expressions involving pointers.")
  public boolean trackPointers = false;

  @Option(description = "Simplify overflow assumptions.")
  public boolean simplifyExpressions = true;


  public Map<CType, CLiteralExpression> width;
  public OverflowAssumptionManager ofmgr;
  public OverflowAssumptionManager ufmgr;
  public ExpressionSimplificationVisitor simplificationVisitor;
  public MachineModel machineModel;
  public Optional<LiveVariables> liveVariables;
  public LogManager logger;



  public boolean isBinaryExpressionThatMayOverflow(CExpression pExp) {
    if (pExp instanceof CBinaryExpression) {
      CBinaryExpression binexp = (CBinaryExpression) pExp;
      CExpression op1 = binexp.getOperand1();
      CExpression op2 = binexp.getOperand2();
      if (op1.getExpressionType() instanceof CPointerType
          || op2.getExpressionType() instanceof CPointerType) {
        // There are no classical arithmetic overflows in binary operations involving pointers,
        // since pointer types are not necessarily signed integer types as far as ISO/IEC 9899:2018
        // (C17) is concerned. So we do not track this by default, but make it configurable:
        return trackPointers;
      } else {
        return true;
  }
    } else {
      return false;
    }
  }

  /**
   * Whether the given operator can create new expression.
   */
  public boolean resultCanOverflow(CExpression expr) {
    if (expr instanceof CBinaryExpression) {
      switch (((CBinaryExpression) expr).getOperator()) {
        case MULTIPLY:
        case DIVIDE:
        case PLUS:
        case MINUS:
        case SHIFT_LEFT:
        case SHIFT_RIGHT:
          return true;
        case LESS_THAN:
        case GREATER_THAN:
        case LESS_EQUAL:
        case GREATER_EQUAL:
        case BINARY_AND:
        case BINARY_XOR:
        case BINARY_OR:
        case EQUALS:
        case NOT_EQUALS:
        default:
          return false;
      }
    } else if (expr instanceof CUnaryExpression) {
      switch (((CUnaryExpression) expr).getOperator()) {
        case MINUS:
          return true;
        default:
          return false;
      }
    }
    return false;
  }
}
