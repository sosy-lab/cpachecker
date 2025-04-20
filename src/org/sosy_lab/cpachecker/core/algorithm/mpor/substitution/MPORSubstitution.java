// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class MPORSubstitution {

  public final MPORThread thread;

  /**
   * The map of global variable declarations to their substitutes. {@link Optional#empty()} if this
   * instance serves as a dummy.
   */
  public final ImmutableMap<CVariableDeclaration, CIdExpression> globalSubstitutes;

  /**
   * The map of thread local variable declarations to their substitutes. Not every local variable
   * declaration has a calling context, hence {@link Optional}s.
   */
  public final ImmutableMap<CVariableDeclaration, ImmutableMap<Optional<ThreadEdge>, CIdExpression>>
      localSubstitutes;

  /**
   * The map of parameter to variable declaration substitutes. {@link Optional#empty()} if this
   * instance serves as a dummy.
   */
  public final ImmutableMap<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
      parameterSubstitutes;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  public MPORSubstitution(
      MPORThread pThread,
      ImmutableMap<CVariableDeclaration, CIdExpression> pGlobalSubstitutes,
      ImmutableMap<CVariableDeclaration, ImmutableMap<Optional<ThreadEdge>, CIdExpression>>
          pLocalSubstitutes,
      ImmutableMap<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
          pParameterSubstitutes,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    thread = pThread;
    globalSubstitutes = pGlobalSubstitutes;
    localSubstitutes = pLocalSubstitutes;
    parameterSubstitutes = pParameterSubstitutes;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
  }

  public CExpression substitute(
      final CExpression pExpression,
      final Optional<ThreadEdge> pCallContext,
      Optional<ImmutableList.Builder<CVariableDeclaration>> pGlobalVariables) {

    FileLocation fileLocation = pExpression.getFileLocation();
    CType type = pExpression.getExpressionType();

    if (pExpression instanceof CIdExpression idExpression) {
      if (isSubstitutable(idExpression.getDeclaration())) {
        if (idExpression.getDeclaration() instanceof CVariableDeclaration variableDeclaration) {
          if (variableDeclaration.isGlobal() && pGlobalVariables.isPresent()) {
            pGlobalVariables.orElseThrow().add(variableDeclaration);
          }
        }
        return getVariableSubstitute(idExpression.getDeclaration(), pCallContext);
      }

    } else if (pExpression instanceof CBinaryExpression binary) {
      // recursively substitute operands of binary expressions
      CExpression op1 = substitute(binary.getOperand1(), pCallContext, pGlobalVariables);
      CExpression op2 = substitute(binary.getOperand2(), pCallContext, pGlobalVariables);
      // only create a new expression if any operand was substituted (compare references)
      if (op1 != binary.getOperand1() || op2 != binary.getOperand2()) {
        try {
          return binaryExpressionBuilder.buildBinaryExpression(op1, op2, binary.getOperator());
        } catch (UnrecognizedCodeException e) {
          // "convert" exception -> no UnrecognizedCodeException in signature
          throw new RuntimeException(e);
        }
      }

    } else if (pExpression instanceof CArraySubscriptExpression arraySubscript) {
      CExpression arrayExpression = arraySubscript.getArrayExpression();
      CExpression subscriptExpression = arraySubscript.getSubscriptExpression();
      CExpression arraySubstitute = substitute(arrayExpression, pCallContext, pGlobalVariables);
      CExpression subscriptSubstitute =
          substitute(subscriptExpression, pCallContext, pGlobalVariables);
      // only create a new expression if any expr was substituted (compare references)
      if (arraySubstitute != arrayExpression || subscriptSubstitute != subscriptExpression) {
        return new CArraySubscriptExpression(
            fileLocation, type, arraySubstitute, subscriptSubstitute);
      }

    } else if (pExpression instanceof CFieldReference fieldReference) {
      CExpression fieldOwnerSubstitute =
          substitute(fieldReference.getFieldOwner(), pCallContext, pGlobalVariables);
      // only create a new expression if any expr was substituted (compare references)
      if (fieldOwnerSubstitute != fieldReference.getFieldOwner()) {
        return new CFieldReference(
            fileLocation,
            fieldReference.getExpressionType(),
            fieldReference.getFieldName(),
            fieldOwnerSubstitute,
            fieldReference.isPointerDereference());
      }

    } else if (pExpression instanceof CUnaryExpression unary) {
      return new CUnaryExpression(
          unary.getFileLocation(),
          unary.getExpressionType(),
          substitute(unary.getOperand(), pCallContext, pGlobalVariables),
          unary.getOperator());

    } else if (pExpression instanceof CPointerExpression pointer) {
      return new CPointerExpression(
          pointer.getFileLocation(),
          pointer.getExpressionType(),
          substitute(pointer.getOperand(), pCallContext, pGlobalVariables));

    } else if (pExpression instanceof CCastExpression cast) {
      return new CCastExpression(
          cast.getFileLocation(),
          cast.getCastType(),
          substitute(cast.getOperand(), pCallContext, pGlobalVariables));
    }

    return pExpression;
  }

  public CStatement substitute(
      CStatement pStatement,
      Optional<ThreadEdge> pCallContext,
      Optional<ImmutableList.Builder<CVariableDeclaration>> pGlobalVariables) {

    FileLocation fileLocation = pStatement.getFileLocation();

    // e.g. n = fib(42); or arr[n] = fib(42);
    if (pStatement instanceof CFunctionCallAssignmentStatement functionCallAssignment) {
      CLeftHandSide leftHandSide = functionCallAssignment.getLeftHandSide();
      if (leftHandSide instanceof CIdExpression idExpression) {
        CExpression substitute = substitute(idExpression, pCallContext, pGlobalVariables);
        if (substitute instanceof CIdExpression idExpressionSubstitute) {
          CFunctionCallExpression rightHandSide = functionCallAssignment.getRightHandSide();
          return new CFunctionCallAssignmentStatement(
              fileLocation,
              idExpressionSubstitute,
              substitute(rightHandSide, pCallContext, pGlobalVariables));
        }
      } else if (leftHandSide instanceof CArraySubscriptExpression arraySubscriptExpression) {
        CExpression substitute =
            substitute(arraySubscriptExpression, pCallContext, pGlobalVariables);
        if (substitute instanceof CArraySubscriptExpression arraySubscriptExpressionSubstitute) {
          CFunctionCallExpression rightHandSide = functionCallAssignment.getRightHandSide();
          return new CFunctionCallAssignmentStatement(
              fileLocation,
              arraySubscriptExpressionSubstitute,
              substitute(rightHandSide, pCallContext, pGlobalVariables));
        }
      }

      // e.g. fib(42);
    } else if (pStatement instanceof CFunctionCallStatement functionCall) {
      return new CFunctionCallStatement(
          functionCall.getFileLocation(),
          substitute(functionCall.getFunctionCallExpression(), pCallContext, pGlobalVariables));

    } else if (pStatement instanceof CExpressionAssignmentStatement expressionAssignment) {
      CLeftHandSide leftHandSide = expressionAssignment.getLeftHandSide();
      CExpression rightHandSide = expressionAssignment.getRightHandSide();
      CExpression substitute = substitute(leftHandSide, pCallContext, pGlobalVariables);
      if (substitute instanceof CLeftHandSide leftHandSideSubstitute) {
        return new CExpressionAssignmentStatement(
            fileLocation,
            leftHandSideSubstitute,
            substitute(rightHandSide, pCallContext, pGlobalVariables));
      }

    } else if (pStatement instanceof CExpressionStatement expression) {
      return new CExpressionStatement(
          fileLocation, substitute(expression.getExpression(), pCallContext, pGlobalVariables));
    }

    return pStatement;
  }

  public CFunctionCallExpression substitute(
      CFunctionCallExpression pFunctionCallExpression,
      Optional<ThreadEdge> pCallContext,
      Optional<ImmutableList.Builder<CVariableDeclaration>> pGlobalVariables) {

    // substitute all parameters in the function call expression
    List<CExpression> parameters = new ArrayList<>();
    for (CExpression expression : pFunctionCallExpression.getParameterExpressions()) {
      parameters.add(substitute(expression, pCallContext, pGlobalVariables));
    }
    return new CFunctionCallExpression(
        pFunctionCallExpression.getFileLocation(),
        pFunctionCallExpression.getExpressionType(),
        pFunctionCallExpression.getFunctionNameExpression(),
        parameters,
        pFunctionCallExpression.getDeclaration());
  }

  public CReturnStatement substitute(
      CReturnStatement pReturnStatement,
      Optional<ThreadEdge> pCallContext,
      Optional<ImmutableList.Builder<CVariableDeclaration>> pGlobalVariables) {

    if (pReturnStatement.getReturnValue().isEmpty()) {
      // return as-is if there is no expression to substitute
      return pReturnStatement;
    } else {
      CExpression expression = pReturnStatement.getReturnValue().orElseThrow();
      // TODO it would be cleaner to also substitute the assignment...
      return new CReturnStatement(
          pReturnStatement.getFileLocation(),
          Optional.of(substitute(expression, pCallContext, pGlobalVariables)),
          pReturnStatement.asAssignment());
    }
  }

  /** Returns the global, local or param {@link CIdExpression} substitute of pSimpleDeclaration. */
  private CIdExpression getVariableSubstitute(
      CSimpleDeclaration pSimpleDeclaration, Optional<ThreadEdge> pCallContext) {

    if (pSimpleDeclaration instanceof CVariableDeclaration variableDeclaration) {
      if (localSubstitutes.containsKey(variableDeclaration)) {
        return Objects.requireNonNull(localSubstitutes.get(variableDeclaration)).get(pCallContext);
      } else {
        checkArgument(
            globalSubstitutes.containsKey(variableDeclaration),
            "no substitute found for %s",
            pSimpleDeclaration.toASTString());
        return globalSubstitutes.get(variableDeclaration);
      }

    } else if (pSimpleDeclaration instanceof CParameterDeclaration parameterDeclaration) {
      assert pCallContext.isPresent();
      ThreadEdge callContext = pCallContext.orElseThrow();
      checkArgument(
          parameterSubstitutes.containsKey(callContext),
          "no substitute found for %s",
          pSimpleDeclaration.toASTString());
      ImmutableMap<CParameterDeclaration, CIdExpression> substitutes =
          Objects.requireNonNull(parameterSubstitutes.get(callContext));
      return substitutes.get(parameterDeclaration);
    }

    throw new IllegalArgumentException(
        "pSimpleDeclaration must be CVariable- or CParameterDeclaration");
  }

  public CVariableDeclaration getVariableDeclarationSubstitute(
      CSimpleDeclaration pSimpleDeclaration, Optional<ThreadEdge> pCallContext) {

    CIdExpression idExpression = getVariableSubstitute(pSimpleDeclaration, pCallContext);
    return (CVariableDeclaration) idExpression.getDeclaration();
  }

  public <T extends CSimpleDeclaration> T castTo(
      CSimpleDeclaration pSimpleDeclaration, Class<T> pClass) {
    checkArgument(
        pClass.isInstance(pSimpleDeclaration),
        "pSimpleDeclaration must be an instance of %s",
        pClass.getSimpleName());
    return pClass.cast(pSimpleDeclaration);
  }

  private boolean isSubstitutable(CSimpleDeclaration pSimpleDeclaration) {
    return pSimpleDeclaration instanceof CVariableDeclaration
        || pSimpleDeclaration instanceof CParameterDeclaration;
  }

  // Declaration Extraction ========================================================================

  public ImmutableList<CVariableDeclaration> getGlobalDeclarations() {
    ImmutableList.Builder<CVariableDeclaration> rGlobalDeclarations = ImmutableList.builder();
    for (CIdExpression globalVariable : globalSubstitutes.values()) {
      CVariableDeclaration variableDeclaration =
          castTo(globalVariable.getDeclaration(), CVariableDeclaration.class);
      rGlobalDeclarations.add(variableDeclaration);
    }
    return rGlobalDeclarations.build();
  }

  public ImmutableList<CVariableDeclaration> getLocalDeclarations() {
    ImmutableList.Builder<CVariableDeclaration> rLocalDeclarations = ImmutableList.builder();
    for (ImmutableMap<Optional<ThreadEdge>, CIdExpression> localVariables :
        localSubstitutes.values()) {
      for (CIdExpression localVariable : localVariables.values()) {
        CVariableDeclaration variableDeclaration =
            castTo(localVariable.getDeclaration(), CVariableDeclaration.class);
        rLocalDeclarations.add(variableDeclaration);
      }
    }
    return rLocalDeclarations.build();
  }

  /**
   * Note that these are not {@link CParameterDeclaration} but {@link CVariableDeclaration} because
   * they are treated as variables in the sequentialization (cf. inlining functions).
   */
  public ImmutableList<CVariableDeclaration> getParameterDeclarations() {
    ImmutableList.Builder<CVariableDeclaration> rParameterDeclarations = ImmutableList.builder();
    for (ImmutableMap<CParameterDeclaration, CIdExpression> substitutes :
        parameterSubstitutes.values()) {
      for (CIdExpression parameter : substitutes.values()) {
        CVariableDeclaration declaration =
            castTo(parameter.getDeclaration(), CVariableDeclaration.class);
        rParameterDeclarations.add(declaration);
      }
    }
    return rParameterDeclarations.build();
  }

  public MPORThread getThread() {
    return thread;
  }
}
