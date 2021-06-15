// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pendingException;

import com.google.common.collect.Lists;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldAccess;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodOrConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.PendingExceptionOfJIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.PendingExceptionOfJRunTimeType;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodCallEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.rtt.RTTState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class PendingExceptionTransferRelation
    extends ForwardingTransferRelation<PendingExceptionState, PendingExceptionState, Precision> {

  @Override
  protected PendingExceptionState handleDeclarationEdge(
      JDeclarationEdge cfaEdge, JDeclaration decl) {

    AInitializer initializer = null;
    if (decl instanceof JVariableDeclaration) {
      initializer = ((JVariableDeclaration) decl).getInitializer();
    }

    // Note array name and array size
    if (decl.getType() instanceof JArrayType && initializer != null) {
      assert initializer instanceof JInitializerExpression
          : "Initializer should be of type JInitializerExpression. If not, cases need to be added";
      JExpression jArrayCreationExpression = ((JInitializerExpression) initializer).getExpression();
      List<BigInteger> arrayLengths = new ArrayList<>();
      if (jArrayCreationExpression instanceof JArrayCreationExpression) {
        arrayLengths =
            convertLengthList(((JArrayCreationExpression) jArrayCreationExpression).getLength());
      } else if (jArrayCreationExpression instanceof JArrayInitializer) {
        arrayLengths.add(
            BigInteger.valueOf(
                ((JArrayInitializer) jArrayCreationExpression).getInitializerExpressions().size()));
      } else if (jArrayCreationExpression instanceof JIdExpression) {
        arrayLengths.add(BigInteger.valueOf(0L));
      }
      assert state != null;
      state.getArrays().put(getScopedVariableName(functionName, decl.getName()), arrayLengths);
    }
    // Check array name and size
    else if (initializer instanceof JInitializerExpression
        && ((JInitializerExpression) initializer).getExpression()
            instanceof JArraySubscriptExpression) {

      String nameOfArray = getNameOfArrayFromInitializer((JInitializerExpression) initializer);
      List<BigInteger> dimensions =
          state.getArrays().get(getScopedVariableName(functionName, nameOfArray));
      boolean arrayAccessOutOfBounds =
          isArrayAccessOutOfBounds(
              (JArraySubscriptExpression) ((JInitializerExpression) initializer).getExpression(),
              dimensions);
      if (arrayAccessOutOfBounds) {
        addArrayIndexOutOfBoundsExceptionToState(state);
      }
    }

    return state;
  }

  private List<BigInteger> convertLengthList(List<JExpression> lengthList) {
    List<BigInteger> result = new ArrayList<>(lengthList.size());
    for (JExpression expression : lengthList) {
      if (expression instanceof JIntegerLiteralExpression) {
        result.add(((JIntegerLiteralExpression) expression).getValue());
      }
    }
    return result;
  }

  private boolean isArrayAccessOutOfBounds(
      JArraySubscriptExpression pJArraySubscriptExpression, List<BigInteger> dimensionsOfArray) {

    JArraySubscriptExpression currentJArraySubscriptExpression = pJArraySubscriptExpression;
    if (dimensionsOfArray == null) {
      return false;
    }
    for (BigInteger dimensionOfArray : Lists.reverse(dimensionsOfArray)) {
      JExpression subscriptExpression = currentJArraySubscriptExpression.getSubscriptExpression();
      if (subscriptExpression instanceof JIdExpression) {
        JVariableDeclaration declaration =
            (JVariableDeclaration) ((JIdExpression) subscriptExpression).getDeclaration();
        if (declaration == null || declaration.getInitializer() == null) {
          return true;
        }
        AInitializer initializer = declaration.getInitializer();
        subscriptExpression = ((JInitializerExpression) initializer).getExpression();
      }
      if (subscriptExpression instanceof JIdExpression) { // Not defined value
        return true;
      }
      JIntegerLiteralExpression currentSubscriptExpression =
          (JIntegerLiteralExpression) subscriptExpression;
      int sizeOfArray = dimensionOfArray.intValue();
      int access = currentSubscriptExpression.getValue().intValue();
      if (access >= sizeOfArray) {
        return true;
      }
      JExpression nextArrayExpression = currentJArraySubscriptExpression.getArrayExpression();
      if (nextArrayExpression instanceof JArraySubscriptExpression) {
        currentJArraySubscriptExpression = (JArraySubscriptExpression) nextArrayExpression;
      } else {
        break;
      }
    }
    return false;
  }

  private String getNameOfArrayFromInitializer(JInitializerExpression pInitializer) {
    JExpression arrayExpression =
        ((JArraySubscriptExpression) pInitializer.getExpression()).getArrayExpression();
    assert arrayExpression instanceof JIdExpression
        || arrayExpression instanceof JArraySubscriptExpression;
    if (arrayExpression instanceof JIdExpression) {
      return ((JIdExpression) arrayExpression).getName();
    } else {
      return ((JIdExpression) ((JArraySubscriptExpression) arrayExpression).getArrayExpression())
          .getName();
    }
  }

  @Override
  protected @Nullable PendingExceptionState handleAssumption(
      JAssumeEdge cfaEdge, JExpression expression, boolean truthAssumption) {
    assert state != null;
    if (expression instanceof JRunTimeTypeEqualsType
        && ((JRunTimeTypeEqualsType) expression).getRunTimeTypeExpression()
            instanceof PendingExceptionOfJRunTimeType) {

      final String s = ((JRunTimeTypeEqualsType) expression).getTypeDef().toASTString("");
      final boolean b = state.getPendingExceptions().containsValue(s);
      if (!((truthAssumption && b) || (!truthAssumption && !b))) {
        return null;
      }
    }

    return state;
  }

  @Override
  protected PendingExceptionState handleStatementEdge(
      JStatementEdge cfaEdge, JStatement statement) {
    assert state != null;

    if (statement instanceof JMethodInvocationStatement) {
      JSimpleDeclaration simpleDeclaration =
          ((JMethodInvocationStatement) statement).getFunctionCallExpression().getDeclaration();
      if (simpleDeclaration != null) {
        state.setMethodInvocationObject(
            getScopedVariableName(functionName, simpleDeclaration.getName()));
      }
    }
    if (statement instanceof JExpressionAssignmentStatement) {
      JLeftHandSide leftHandSide = ((JExpressionAssignmentStatement) statement).getLeftHandSide();
      if ((leftHandSide instanceof PendingExceptionOfJIdExpression)) {

        final JExpression rightHandSide =
            ((JExpressionAssignmentStatement) statement).getRightHandSide();

        assert isThrowable(rightHandSide.getExpressionType());

        String variableName = getScopedVariableName(functionName, rightHandSide.toString());
        state.getPendingExceptions().put(variableName, "");
        state.increaseCounterExceptionsCaught();
      } else if (leftHandSide instanceof JArraySubscriptExpression) {
        JExpression arrayExpression =
            ((JArraySubscriptExpression) leftHandSide).getArrayExpression();
        while (arrayExpression instanceof JArraySubscriptExpression) {
          arrayExpression = ((JArraySubscriptExpression) arrayExpression).getArrayExpression();
        }
        String name = ((JIdExpression) arrayExpression).getName();
        String scopedVariableName = getScopedVariableName(functionName, name);
        boolean arrayAccessOutOfBounds =
            isArrayAccessOutOfBounds(
                (JArraySubscriptExpression) leftHandSide,
                state.getArrays().get(scopedVariableName));
        if (arrayAccessOutOfBounds) {
          addArrayIndexOutOfBoundsExceptionToState(state);
          state.increaseCounterExceptionsCaught();
        }
      }
      if (leftHandSide instanceof JFieldAccess) {
        if (((JFieldAccess) leftHandSide).getDeclaration().getInitializer() == null) {
          addNullPointerExceptionToState(state);
        }
      }
    }
    return state;
  }

  private static void addNullPointerExceptionToState(PendingExceptionState pState) {
    Class<?> cls;
    try {
      cls = Class.forName("java.lang.NullPointerException");
    } catch (ClassNotFoundException e) {
      return;
    }
    addExceptionFromClassToState(pState, cls);
  }

  private static void addArrayIndexOutOfBoundsExceptionToState(PendingExceptionState pState) {
    Class<?> cls;
    try {
      cls = Class.forName("java.lang.ArrayIndexOutOfBoundsException");
    } catch (ClassNotFoundException e) {
      return;
    }
    addExceptionFromClassToState(pState, cls);
  }

  private static void addExceptionFromClassToState(PendingExceptionState pState, Class<?> pCls) {
    Map<String, String> pendingExceptions = pState.getPendingExceptions();
    pendingExceptions.put(PendingExceptionState.PENDING_EXCEPTION, pCls.getName());
    Class<?> parent = pCls.getSuperclass();
    int counter = 1;
    do {
      pendingExceptions.put(PendingExceptionState.PENDING_EXCEPTION + counter, parent.getName());
      parent = parent.getSuperclass();
      counter++;
    } while (!parent.equals(Object.class));
    pState.increaseCounterExceptionsCaught();
  }

  @Override
  protected PendingExceptionState handleFunctionCallEdge(
      JMethodCallEdge cfaEdge,
      List<JExpression> arguments,
      List<JParameterDeclaration> parameters,
      String calledFunctionName) {
    return state;
  }

  @Override
  protected PendingExceptionState handleReturnStatementEdge(JReturnStatementEdge cfaEdge) {
    return state;
  }

  @Override
  protected PendingExceptionState handleFunctionReturnEdge(
      JMethodReturnEdge cfaEdge,
      JMethodSummaryEdge fnkCall,
      JMethodOrConstructorInvocation summaryExpr,
      String callerFunctionName) {
    return state;
  }

  private boolean isThrowable(JType pJType) {

    if (!(pJType instanceof JClassType)) {
      return false;
    }

    JClassType parentClass = ((JClassType) pJType).getParentClass();

    while (parentClass != null && !parentClass.toString().equals("java.lang.Throwable")) {
      parentClass = parentClass.getParentClass();
    }

    if (parentClass != null && parentClass.toString().equals("java.lang.Throwable")) {
      return true;
    }
    return false;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      @Nullable CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    for (AbstractState abstractState : pOtherStates) {
      if ((abstractState instanceof RTTState)) {
        if (pCfaEdge instanceof JStatementEdge
            && ((JStatementEdge) pCfaEdge).getStatement() instanceof JMethodInvocationStatement) {
          checkForMethodInvocationOnNullObject(
              (PendingExceptionState) pState, (RTTState) abstractState);
        }
        if (pCfaEdge instanceof JDeclarationEdge) {
          AInitializer initializer = null;
          JDeclaration decl = ((JDeclarationEdge) pCfaEdge).getDeclaration();
          if (decl instanceof JVariableDeclaration) {
            initializer = ((JVariableDeclaration) decl).getInitializer();
          }
          if (initializer instanceof JInitializerExpression
              && ((JInitializerExpression) initializer).getExpression() instanceof JFieldAccess) {
            String name =
                ((JFieldAccess) ((JInitializerExpression) initializer).getExpression())
                    .getReferencedVariable()
                    .getName();
            int i = decl.getQualifiedName().indexOf(":");
            String localFunctionName = decl.getQualifiedName().substring(0, i);
            String value =
                ((RTTState) abstractState)
                    .getConstantsMap()
                    .get(getScopedVariableName(localFunctionName, name));
            if ("null".equals(value)) {
              addNullPointerExceptionToState((PendingExceptionState) pState);
            }
          }
        }

        for (String variableName :
            ((PendingExceptionState) pState).getPendingExceptions().keySet()) {
          if (((RTTState) abstractState).getConstantsMap().containsKey(variableName)) {
            String value = ((RTTState) abstractState).getConstantsMap().get(variableName);
            ((PendingExceptionState) pState)
                .getPendingExceptions()
                .put(variableName, ((RTTState) abstractState).getRunTimeClassOfUniqueObject(value));
            ((PendingExceptionState) pState).increaseCounterExceptionsCaught();
          }
        }
      }
    }
    return super.strengthen(pState, pOtherStates, pCfaEdge, pPrecision);
  }

  private void checkForMethodInvocationOnNullObject(
      PendingExceptionState pState, RTTState pRTTState) {
    String methodInvocationObject = pState.getMethodInvocationObject();
    Map<String, String> constantsMap = pRTTState.getConstantsMap();
    if (constantsMap.containsKey(methodInvocationObject)) {
      if ("null".equals(constantsMap.get(methodInvocationObject))) {
        addNullPointerExceptionToState(pState);
        pState.setMethodInvocationObject("");
      }
    }
  }

  private static String getScopedVariableName(String pFunctionName, String pVariableName) {
    String variableDelimiter = "::";
    return pFunctionName + variableDelimiter + pVariableName;
  }
}
