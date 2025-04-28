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

  // TODO public final boolean isDummy (for debugging)

  public final MPORThread thread;

  /**
   * The map of global variable declarations to their substitutes. {@link Optional#empty()} if this
   * instance serves as a dummy.
   */
  public final ImmutableMap<CVariableDeclaration, CIdExpression> globalSubstitutes;

  // TODO what if multiple declarations have no call context - duplicate key in map?
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

  public final ImmutableMap<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
      startRoutineArgSubstitutes;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  public MPORSubstitution(
      MPORThread pThread,
      ImmutableMap<CVariableDeclaration, CIdExpression> pGlobalSubstitutes,
      ImmutableMap<CVariableDeclaration, ImmutableMap<Optional<ThreadEdge>, CIdExpression>>
          pLocalSubstitutes,
      ImmutableMap<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
          pParameterSubstitutes,
      ImmutableMap<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
          pStartRoutineArgSubstitutes,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    thread = pThread;
    globalSubstitutes = pGlobalSubstitutes;
    localSubstitutes = pLocalSubstitutes;
    parameterSubstitutes = pParameterSubstitutes;
    startRoutineArgSubstitutes = pStartRoutineArgSubstitutes;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
  }

  /**
   * Substitutes the given expression, and tracks if any global variable was substituted alongside
   * in {@code pAccessedGlobalVariables}.
   */
  public CExpression substitute(
      final CExpression pExpression,
      final Optional<ThreadEdge> pCallContext,
      boolean pIsAssignmentLeftHandSide,
      Optional<ImmutableList.Builder<CVariableDeclaration>> pWrittenGlobalVariables,
      Optional<ImmutableList.Builder<CVariableDeclaration>> pAccessedGlobalVariables) {

    checkArgument(
        !pIsAssignmentLeftHandSide || pWrittenGlobalVariables.isPresent(),
        "if pIsAssignmentLeftHandSide is true, pWrittenGlobalVariables must be present");

    FileLocation fileLocation = pExpression.getFileLocation();
    CType type = pExpression.getExpressionType();

    if (pExpression instanceof CIdExpression idExpression) {
      if (isSubstitutable(idExpression.getDeclaration())) {
        if (idExpression.getDeclaration() instanceof CVariableDeclaration variableDeclaration) {
          if (variableDeclaration.isGlobal()) {
            if (pAccessedGlobalVariables.isPresent()) {
              pAccessedGlobalVariables.orElseThrow().add(variableDeclaration);
            }
            if (pWrittenGlobalVariables.isPresent() && pIsAssignmentLeftHandSide) {
              pWrittenGlobalVariables.orElseThrow().add(variableDeclaration);
            }
          }
        }
        return getVariableSubstitute(idExpression.getDeclaration(), pCallContext);
      }

    } else if (pExpression instanceof CBinaryExpression binary) {
      // recursively substitute operands of binary expressions
      CExpression op1 =
          substitute(
              binary.getOperand1(),
              pCallContext,
              // binary expressions are never LHS in assignments
              false,
              pWrittenGlobalVariables,
              pAccessedGlobalVariables);
      CExpression op2 =
          substitute(
              binary.getOperand2(),
              pCallContext,
              // binary expressions are never LHS in assignments
              false,
              pWrittenGlobalVariables,
              pAccessedGlobalVariables);
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
      CExpression arraySubstitute =
          substitute(
              arrayExpression,
              pCallContext,
              pIsAssignmentLeftHandSide,
              pWrittenGlobalVariables,
              pAccessedGlobalVariables);
      // the subscript is not a LHS in an assignment
      CExpression subscriptSubstitute =
          substitute(
              subscriptExpression,
              pCallContext,
              false,
              pWrittenGlobalVariables,
              pAccessedGlobalVariables);
      // only create a new expression if any expr was substituted (compare references)
      if (arraySubstitute != arrayExpression || subscriptSubstitute != subscriptExpression) {
        return new CArraySubscriptExpression(
            fileLocation, type, arraySubstitute, subscriptSubstitute);
      }

    } else if (pExpression instanceof CFieldReference fieldReference) {
      CExpression fieldOwnerSubstitute =
          substitute(
              fieldReference.getFieldOwner(),
              pCallContext,
              pIsAssignmentLeftHandSide,
              pWrittenGlobalVariables,
              pAccessedGlobalVariables);
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
          substitute(
              unary.getOperand(),
              pCallContext,
              // unary expressions such as '&var' are never LHS in assignments
              false,
              pWrittenGlobalVariables,
              pAccessedGlobalVariables),
          unary.getOperator());

    } else if (pExpression instanceof CPointerExpression pointer) {
      return new CPointerExpression(
          pointer.getFileLocation(),
          pointer.getExpressionType(),
          substitute(
              pointer.getOperand(),
              pCallContext,
              pIsAssignmentLeftHandSide,
              pWrittenGlobalVariables,
              pAccessedGlobalVariables));

    } else if (pExpression instanceof CCastExpression cast) {
      return new CCastExpression(
          cast.getFileLocation(),
          cast.getCastType(),
          substitute(
              cast.getOperand(),
              pCallContext,
              // cast expressions are never LHS
              false,
              pWrittenGlobalVariables,
              pAccessedGlobalVariables));
    }

    return pExpression;
  }

  public CStatement substitute(
      CStatement pStatement,
      Optional<ThreadEdge> pCallContext,
      Optional<ImmutableList.Builder<CVariableDeclaration>> pWrittenGlobalVariables,
      Optional<ImmutableList.Builder<CVariableDeclaration>> pGlobalVariables) {

    FileLocation fileLocation = pStatement.getFileLocation();

    // e.g. n = fib(42); or arr[n] = fib(42);
    if (pStatement instanceof CFunctionCallAssignmentStatement functionCallAssignment) {
      CLeftHandSide leftHandSide = functionCallAssignment.getLeftHandSide();
      if (leftHandSide instanceof CIdExpression idExpression) {
        CExpression substitute =
            substitute(idExpression, pCallContext, true, pWrittenGlobalVariables, pGlobalVariables);
        if (substitute instanceof CIdExpression idExpressionSubstitute) {
          CFunctionCallExpression rightHandSide = functionCallAssignment.getRightHandSide();
          return new CFunctionCallAssignmentStatement(
              fileLocation,
              idExpressionSubstitute,
              substitute(rightHandSide, pCallContext, pWrittenGlobalVariables, pGlobalVariables));
        }
      } else if (leftHandSide instanceof CArraySubscriptExpression arraySubscriptExpression) {
        CExpression substitute =
            substitute(
                arraySubscriptExpression,
                pCallContext,
                true,
                pWrittenGlobalVariables,
                pGlobalVariables);
        if (substitute instanceof CArraySubscriptExpression arraySubscriptExpressionSubstitute) {
          CFunctionCallExpression rightHandSide = functionCallAssignment.getRightHandSide();
          return new CFunctionCallAssignmentStatement(
              fileLocation,
              arraySubscriptExpressionSubstitute,
              substitute(rightHandSide, pCallContext, pWrittenGlobalVariables, pGlobalVariables));
        }
      }

      // e.g. fib(42);
    } else if (pStatement instanceof CFunctionCallStatement functionCall) {
      return new CFunctionCallStatement(
          functionCall.getFileLocation(),
          substitute(
              functionCall.getFunctionCallExpression(),
              pCallContext,
              pWrittenGlobalVariables,
              pGlobalVariables));

      // e.g. int x = 42;
    } else if (pStatement instanceof CExpressionAssignmentStatement expressionAssignment) {
      CLeftHandSide leftHandSide = expressionAssignment.getLeftHandSide();
      CExpression rightHandSide = expressionAssignment.getRightHandSide();
      CExpression substitute =
          substitute(leftHandSide, pCallContext, true, pWrittenGlobalVariables, pGlobalVariables);
      if (substitute instanceof CLeftHandSide leftHandSideSubstitute) {
        return new CExpressionAssignmentStatement(
            fileLocation,
            leftHandSideSubstitute,
            // for the RHS, it's not a left hand side of an assignment
            substitute(
                rightHandSide, pCallContext, false, pWrittenGlobalVariables, pGlobalVariables));
      }

    } else if (pStatement instanceof CExpressionStatement expression) {
      return new CExpressionStatement(
          fileLocation,
          substitute(
              expression.getExpression(),
              pCallContext,
              false,
              pWrittenGlobalVariables,
              pGlobalVariables));
    }

    return pStatement;
  }

  public CFunctionCallExpression substitute(
      CFunctionCallExpression pFunctionCallExpression,
      Optional<ThreadEdge> pCallContext,
      Optional<ImmutableList.Builder<CVariableDeclaration>> pWrittenGlobalVariables,
      Optional<ImmutableList.Builder<CVariableDeclaration>> pGlobalVariables) {

    // substitute all parameters in the function call expression
    List<CExpression> parameters = new ArrayList<>();
    for (CExpression expression : pFunctionCallExpression.getParameterExpressions()) {
      parameters.add(
          substitute(expression, pCallContext, false, pWrittenGlobalVariables, pGlobalVariables));
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
      Optional<ImmutableList.Builder<CVariableDeclaration>> pWrittenGlobalVariables,
      Optional<ImmutableList.Builder<CVariableDeclaration>> pGlobalVariables) {

    if (pReturnStatement.getReturnValue().isEmpty()) {
      // return as-is if there is no expression to substitute
      return pReturnStatement;
    } else {
      CExpression expression = pReturnStatement.getReturnValue().orElseThrow();
      // TODO it would be cleaner to also substitute the assignment...
      return new CReturnStatement(
          pReturnStatement.getFileLocation(),
          Optional.of(
              substitute(
                  expression, pCallContext, false, pWrittenGlobalVariables, pGlobalVariables)),
          pReturnStatement.asAssignment());
    }
  }

  /** Returns the global, local or param {@link CIdExpression} substitute of pSimpleDeclaration. */
  private CIdExpression getVariableSubstitute(
      CSimpleDeclaration pSimpleDeclaration, Optional<ThreadEdge> pCallContext) {

    if (pSimpleDeclaration instanceof CVariableDeclaration variableDeclaration) {
      if (localSubstitutes.containsKey(variableDeclaration)) {
        ImmutableMap<Optional<ThreadEdge>, CIdExpression> substitutes =
            Objects.requireNonNull(localSubstitutes.get(variableDeclaration));
        return Objects.requireNonNull(substitutes.get(pCallContext));
      } else {
        checkArgument(
            globalSubstitutes.containsKey(variableDeclaration),
            "no substitute found for %s",
            variableDeclaration.toASTString());
        return Objects.requireNonNull(globalSubstitutes.get(variableDeclaration));
      }

    } else if (pSimpleDeclaration instanceof CParameterDeclaration parameterDeclaration) {
      assert pCallContext.isPresent() : "must have call context to substitute parameter";
      // normal function called within thread always has call context
      ThreadEdge callContext = pCallContext.orElseThrow();
      if (parameterSubstitutes.containsKey(callContext)) {
        ImmutableMap<CParameterDeclaration, CIdExpression> substitutes =
            Objects.requireNonNull(parameterSubstitutes.get(callContext));
        return Objects.requireNonNull(substitutes.get(parameterDeclaration));
      } else if (startRoutineArgSubstitutes.containsKey(callContext)) {
        ImmutableMap<CParameterDeclaration, CIdExpression> substitutes =
            Objects.requireNonNull(startRoutineArgSubstitutes.get(callContext));
        assert substitutes.size() == 1 : "start_routines can have only one parameter";
        return Objects.requireNonNull(substitutes.get(parameterDeclaration));
      }
      throw new IllegalArgumentException("parameter declaration could not be found");
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

  public ImmutableList<CVariableDeclaration> getStartRoutineArgDeclarations() {
    ImmutableList.Builder<CVariableDeclaration> rStartRoutineArgDeclarations =
        ImmutableList.builder();
    for (ImmutableMap<CParameterDeclaration, CIdExpression> substitutes :
        startRoutineArgSubstitutes.values()) {
      for (CIdExpression startRoutineArg : substitutes.values()) {
        CVariableDeclaration declaration =
            castTo(startRoutineArg.getDeclaration(), CVariableDeclaration.class);
        rStartRoutineArgDeclarations.add(declaration);
      }
    }
    return rStartRoutineArgDeclarations.build();
  }

  public MPORThread getThread() {
    return thread;
  }
}
