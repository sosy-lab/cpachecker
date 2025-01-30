package org.sosy_lab.cpachecker.core.algorithm.instrumentation;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
public class LoopConditionChecker {

    public static VariableBoundInfo distanceCompatible(CFANode loopHead) {
      

      
  
        CFAEdge firstLeavingEdge = loopHead.getLeavingEdge(0);
  
        if (firstLeavingEdge instanceof CAssumeEdge assumeEdge) {
          CExpression expression = assumeEdge.getExpression();
  
          if (expression instanceof CBinaryExpression binaryExpression) {
            BinaryOperator operator = binaryExpression.getOperator();
  
            if (operator == BinaryOperator.LESS_THAN
                || operator == BinaryOperator.GREATER_THAN
                || operator == BinaryOperator.LESS_EQUAL
                || operator == BinaryOperator.GREATER_EQUAL) {
  
              if (binaryExpression.getOperand1() instanceof CIdExpression operand1
                  && binaryExpression.getOperand2() instanceof CIdExpression operand2) {
                return new VariableBoundInfo(operand1.getName(), operand2.getName());
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
