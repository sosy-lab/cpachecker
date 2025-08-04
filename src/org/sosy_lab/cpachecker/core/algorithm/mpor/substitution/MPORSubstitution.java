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
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection.InputRejection;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class MPORSubstitution {

  /** Whether this instance servers as a dummy, used only for debugging. */
  @SuppressWarnings("unused")
  private final boolean isDummy;

  private final MPOROptions options;

  public final MPORThread thread;

  /**
   * The map of global variable declarations to their substitutes. {@link Optional#empty()} if this
   * instance serves as a dummy.
   */
  private final ImmutableMap<CVariableDeclaration, CIdExpression> globalSubstitutes;

  /** The map of thread local variable declarations to their substitutes. */
  private final ImmutableMap<CVariableDeclaration, LocalVariableDeclarationSubstitute>
      localSubstitutes;

  /**
   * The map of parameter to variable declaration substitutes. {@link Optional#empty()} if this
   * instance serves as a dummy.
   */
  public final ImmutableMap<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
      parameterSubstitutes;

  public final ImmutableMap<CParameterDeclaration, CIdExpression> mainFunctionArgSubstitutes;

  public final ImmutableMap<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
      startRoutineArgSubstitutes;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  private final LogManager logger;

  public MPORSubstitution(
      boolean pIsDummy,
      MPOROptions pOptions,
      MPORThread pThread,
      ImmutableMap<CVariableDeclaration, CIdExpression> pGlobalSubstitutes,
      ImmutableMap<CVariableDeclaration, LocalVariableDeclarationSubstitute> pLocalSubstitutes,
      ImmutableMap<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
          pParameterSubstitutes,
      ImmutableMap<CParameterDeclaration, CIdExpression> pMainFunctionArgSubstitutes,
      ImmutableMap<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
          pStartRoutineArgSubstitutes,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger) {

    isDummy = pIsDummy;
    options = pOptions;
    thread = pThread;
    globalSubstitutes = pGlobalSubstitutes;
    localSubstitutes = pLocalSubstitutes;
    parameterSubstitutes = pParameterSubstitutes;
    mainFunctionArgSubstitutes = pMainFunctionArgSubstitutes;
    startRoutineArgSubstitutes = pStartRoutineArgSubstitutes;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
    logger = pLogger;
  }

  /**
   * If applicable, adds the {@link CVariableDeclaration} of {@code pIdExpression} to the respective
   * sets. {@code pIsWrite} is used to determine whether the expression to substitute is written,
   * i.e. a LHS in an assignment.
   */
  private void handleGlobalVariableAccesses(
      CIdExpression pIdExpression,
      boolean pIsWrite,
      Optional<Set<CVariableDeclaration>> pWrittenGlobalVariables,
      Optional<Set<CVariableDeclaration>> pAccessedGlobalVariables) {

    // writing pointers (aliasing) may not be allowed -> reject program
    InputRejection.checkPointerWrite(pIsWrite, options, pIdExpression, logger);

    // otherwise, if applicable, add declaration to global reads/writes
    if (pIdExpression.getDeclaration() instanceof CVariableDeclaration variableDeclaration) {
      if (variableDeclaration.isGlobal()) {
        // treat pthread_mutex_t lock/unlock as writes, otherwise interleavings are lost
        boolean isMutex =
            PthreadObjectType.PTHREAD_MUTEX_T.equalsType(variableDeclaration.getType());
        if (pAccessedGlobalVariables.isPresent()) {
          pAccessedGlobalVariables.orElseThrow().add(variableDeclaration);
        }
        if (pWrittenGlobalVariables.isPresent() && (pIsWrite || isMutex)) {
          pWrittenGlobalVariables.orElseThrow().add(variableDeclaration);
        }
      }
    }
  }

  private void handlePointerAssignment(
      CExpressionAssignmentStatement pAssignment,
      Optional<Map<CVariableDeclaration, CVariableDeclaration>> pPointerAssignments) {

    if (pPointerAssignments.isEmpty()) {
      return;
    }
    CLeftHandSide leftHandSide = pAssignment.getLeftHandSide();
    if (leftHandSide instanceof CIdExpression lhsId) {
      if (lhsId.getDeclaration() instanceof CVariableDeclaration lhsDeclaration) {
        if (lhsDeclaration.getType() instanceof CPointerType) {
          CExpression rightHandSide = pAssignment.getRightHandSide();
          // unary expression i.e. '&var'
          if (rightHandSide instanceof CUnaryExpression unaryExpression) {
            if (unaryExpression.getOperator().equals(UnaryOperator.AMPER)) {
              if (unaryExpression.getOperand() instanceof CIdExpression rhsId) {
                if (rhsId.getDeclaration() instanceof CVariableDeclaration rhsDeclaration) {
                  pPointerAssignments.orElseThrow().put(lhsDeclaration, rhsDeclaration);
                }
              }
            }
            // id expression i.e. another pointer assigned to the pointer
          } else if (rightHandSide instanceof CIdExpression rhsId) {
            if (rhsId.getDeclaration() instanceof CVariableDeclaration rhsDeclaration) {
              if (rhsDeclaration.getType() instanceof CPointerType) {
                pPointerAssignments.orElseThrow().put(lhsDeclaration, rhsDeclaration);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Substitutes the given expression, and tracks if any global variable was substituted alongside
   * in {@code pAccessedGlobalVariables}. {@code pIsWrite} is used to determine whether the
   * expression to substitute * is written, i.e. a LHS in an assignment.
   */
  public CExpression substitute(
      final CExpression pExpression,
      final Optional<ThreadEdge> pCallContext,
      boolean pIsWrite,
      boolean pIsUnaryAmper,
      Optional<Map<CVariableDeclaration, CVariableDeclaration>> pPointerAssignments,
      Optional<Set<CVariableDeclaration>> pWrittenGlobalVariables,
      Optional<Set<CVariableDeclaration>> pAccessedGlobalVariables,
      Optional<Set<CFunctionDeclaration>> pAccessedFunctionPointers) {

    // shortcut for optimization: never substitute pure int or strings
    if (pExpression instanceof CIntegerLiteralExpression) {
      return pExpression;
    } else if (pExpression instanceof CStringLiteralExpression) {
      return pExpression;
    }

    FileLocation fileLocation = pExpression.getFileLocation();
    CType type = pExpression.getExpressionType();

    if (pExpression instanceof CIdExpression idExpression) {
      CSimpleDeclaration declaration = idExpression.getDeclaration();
      if (isSubstitutable(declaration)) {
        handleGlobalVariableAccesses(
            idExpression, pIsWrite, pWrittenGlobalVariables, pAccessedGlobalVariables);
        return getVariableSubstitute(idExpression.getDeclaration(), pCallContext);
      }
      // when accessing function pointers e.g. &func. this is also possible without the unary amper
      // operator '&', but the example tasks used only this expression, so we restrict it.
      if (pIsUnaryAmper) {
        if (declaration instanceof CFunctionDeclaration functionDeclaration) {
          if (pAccessedFunctionPointers.isPresent()) {
            pAccessedFunctionPointers.orElseThrow().add(functionDeclaration);
          }
        }
      }

    } else if (pExpression instanceof CBinaryExpression binary) {
      // recursively substitute operands of binary expressions
      CExpression op1 =
          substitute(
              binary.getOperand1(),
              pCallContext,
              // binary expressions are never LHS in assignments -> no write
              false,
              pIsUnaryAmper,
              pPointerAssignments,
              pWrittenGlobalVariables,
              pAccessedGlobalVariables,
              pAccessedFunctionPointers);
      CExpression op2 =
          substitute(
              binary.getOperand2(),
              pCallContext,
              // binary expressions are never LHS in assignments -> no write
              false,
              pIsUnaryAmper,
              pPointerAssignments,
              pWrittenGlobalVariables,
              pAccessedGlobalVariables,
              pAccessedFunctionPointers);
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
              true,
              pIsUnaryAmper,
              pPointerAssignments,
              pWrittenGlobalVariables,
              pAccessedGlobalVariables,
              pAccessedFunctionPointers);
      // the subscript is not a LHS in an assignment -> no write
      CExpression subscriptSubstitute =
          substitute(
              subscriptExpression,
              pCallContext,
              false,
              pIsUnaryAmper,
              pPointerAssignments,
              pWrittenGlobalVariables,
              pAccessedGlobalVariables,
              pAccessedFunctionPointers);
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
              pIsWrite,
              pIsUnaryAmper,
              pPointerAssignments,
              pWrittenGlobalVariables,
              pAccessedGlobalVariables,
              pAccessedFunctionPointers);
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
              // unary expressions such as '&var' are never LHS in assignments -> no write
              false,
              unary.getOperator().equals(UnaryOperator.AMPER),
              pPointerAssignments,
              pWrittenGlobalVariables,
              pAccessedGlobalVariables,
              pAccessedFunctionPointers),
          unary.getOperator());

    } else if (pExpression instanceof CPointerExpression pointer) {
      return new CPointerExpression(
          pointer.getFileLocation(),
          pointer.getExpressionType(),
          substitute(
              pointer.getOperand(),
              pCallContext,
              pIsWrite,
              pIsUnaryAmper,
              pPointerAssignments,
              pWrittenGlobalVariables,
              pAccessedGlobalVariables,
              pAccessedFunctionPointers));

    } else if (pExpression instanceof CCastExpression cast) {
      return new CCastExpression(
          cast.getFileLocation(),
          cast.getCastType(),
          substitute(
              cast.getOperand(),
              pCallContext,
              // cast expressions are never LHS -> no write
              pIsWrite,
              pIsUnaryAmper,
              pPointerAssignments,
              pWrittenGlobalVariables,
              pAccessedGlobalVariables,
              pAccessedFunctionPointers));
    }

    return pExpression;
  }

  CStatement substitute(
      CStatement pStatement,
      Optional<ThreadEdge> pCallContext,
      Optional<Map<CVariableDeclaration, CVariableDeclaration>> pPointerAssignments,
      Optional<Set<CVariableDeclaration>> pWrittenGlobalVariables,
      Optional<Set<CVariableDeclaration>> pGlobalVariables,
      Optional<Set<CFunctionDeclaration>> pAccessedFunctionPointers) {

    FileLocation fileLocation = pStatement.getFileLocation();

    // e.g. n = fib(42); or arr[n] = fib(42);
    if (pStatement instanceof CFunctionCallAssignmentStatement functionCallAssignment) {
      CLeftHandSide leftHandSide = functionCallAssignment.getLeftHandSide();
      CFunctionCallExpression rightHandSide = functionCallAssignment.getRightHandSide();
      if (leftHandSide instanceof CIdExpression idExpression) {
        CExpression substitute =
            substitute(
                idExpression,
                pCallContext,
                false,
                false,
                pPointerAssignments,
                pWrittenGlobalVariables,
                pGlobalVariables,
                pAccessedFunctionPointers);
        if (substitute instanceof CIdExpression idExpressionSubstitute) {
          return new CFunctionCallAssignmentStatement(
              fileLocation,
              idExpressionSubstitute,
              substitute(
                  rightHandSide,
                  pCallContext,
                  pPointerAssignments,
                  pWrittenGlobalVariables,
                  pGlobalVariables,
                  pAccessedFunctionPointers));
        }
      } else if (leftHandSide instanceof CArraySubscriptExpression arraySubscriptExpression) {
        CExpression substitute =
            substitute(
                arraySubscriptExpression,
                pCallContext,
                false,
                false,
                pPointerAssignments,
                pWrittenGlobalVariables,
                pGlobalVariables,
                pAccessedFunctionPointers);
        if (substitute instanceof CArraySubscriptExpression arraySubscriptExpressionSubstitute) {
          return new CFunctionCallAssignmentStatement(
              fileLocation,
              arraySubscriptExpressionSubstitute,
              substitute(
                  rightHandSide,
                  pCallContext,
                  pPointerAssignments,
                  pWrittenGlobalVariables,
                  pGlobalVariables,
                  pAccessedFunctionPointers));
        }
      }

      // e.g. fib(42);
    } else if (pStatement instanceof CFunctionCallStatement functionCall) {
      return new CFunctionCallStatement(
          functionCall.getFileLocation(),
          substitute(
              functionCall.getFunctionCallExpression(),
              pCallContext,
              pPointerAssignments,
              pWrittenGlobalVariables,
              pGlobalVariables,
              pAccessedFunctionPointers));

      // e.g. int x = 42;
    } else if (pStatement instanceof CExpressionAssignmentStatement assignment) {
      handlePointerAssignment(assignment, pPointerAssignments);
      CLeftHandSide leftHandSide = assignment.getLeftHandSide();
      CExpression rightHandSide = assignment.getRightHandSide();
      CExpression substitute =
          substitute(
              leftHandSide,
              pCallContext,
              true,
              false,
              pPointerAssignments,
              pWrittenGlobalVariables,
              pGlobalVariables,
              pAccessedFunctionPointers);
      if (substitute instanceof CLeftHandSide leftHandSideSubstitute) {
        return new CExpressionAssignmentStatement(
            fileLocation,
            leftHandSideSubstitute,
            // for the RHS, it's not a left hand side of an assignment
            substitute(
                rightHandSide,
                pCallContext,
                false,
                false,
                pPointerAssignments,
                pWrittenGlobalVariables,
                pGlobalVariables,
                pAccessedFunctionPointers));
      }

    } else if (pStatement instanceof CExpressionStatement expression) {
      return new CExpressionStatement(
          fileLocation,
          substitute(
              expression.getExpression(),
              pCallContext,
              false,
              false,
              pPointerAssignments,
              pWrittenGlobalVariables,
              pGlobalVariables,
              pAccessedFunctionPointers));
    }

    return pStatement;
  }

  CFunctionCallExpression substitute(
      CFunctionCallExpression pFunctionCallExpression,
      Optional<ThreadEdge> pCallContext,
      Optional<Map<CVariableDeclaration, CVariableDeclaration>> pPointerAssignments,
      Optional<Set<CVariableDeclaration>> pWrittenGlobalVariables,
      Optional<Set<CVariableDeclaration>> pGlobalVariables,
      Optional<Set<CFunctionDeclaration>> pAccessedFunctionPointers) {

    // substitute all parameters in the function call expression
    List<CExpression> parameters = new ArrayList<>();
    for (CExpression expression : pFunctionCallExpression.getParameterExpressions()) {
      parameters.add(
          substitute(
              expression,
              pCallContext,
              false,
              false,
              pPointerAssignments,
              pWrittenGlobalVariables,
              pGlobalVariables,
              pAccessedFunctionPointers));
    }
    // substitute the function name in case it is a variable storing a function pointer
    // e.g. pthread-driver-races/char_pc8736x_gpio_pc8736x_gpio_configure_pc8736x_gpio_get
    // -> int _whoop_init_result = _whoop_init(); (transformed to (*_whoop_init)())
    CExpression functionSubstitute =
        substitute(
            pFunctionCallExpression.getFunctionNameExpression(),
            pCallContext,
            false,
            false,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            pAccessedFunctionPointers);
    return new CFunctionCallExpression(
        pFunctionCallExpression.getFileLocation(),
        pFunctionCallExpression.getExpressionType(),
        functionSubstitute,
        parameters,
        pFunctionCallExpression.getDeclaration());
  }

  CReturnStatement substitute(
      CReturnStatement pReturnStatement,
      Optional<ThreadEdge> pCallContext,
      Optional<Map<CVariableDeclaration, CVariableDeclaration>> pPointerAssignments,
      Optional<Set<CVariableDeclaration>> pWrittenGlobalVariables,
      Optional<Set<CVariableDeclaration>> pGlobalVariables,
      Optional<Set<CFunctionDeclaration>> pAccessedFunctionPointers) {

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
                  expression,
                  pCallContext,
                  false,
                  false,
                  pPointerAssignments,
                  pWrittenGlobalVariables,
                  pGlobalVariables,
                  pAccessedFunctionPointers)),
          pReturnStatement.asAssignment());
    }
  }

  /** Returns the global, local or param {@link CIdExpression} substitute of pSimpleDeclaration. */
  private CIdExpression getVariableSubstitute(
      CSimpleDeclaration pSimpleDeclaration, Optional<ThreadEdge> pCallContext) {

    if (pSimpleDeclaration instanceof CVariableDeclaration variableDeclaration) {
      if (localSubstitutes.containsKey(variableDeclaration)) {
        LocalVariableDeclarationSubstitute localSubstitute =
            Objects.requireNonNull(localSubstitutes.get(variableDeclaration));
        ImmutableMap<Optional<ThreadEdge>, CIdExpression> substitutes =
            Objects.requireNonNull(localSubstitute.substitutes);
        return Objects.requireNonNull(substitutes.get(pCallContext));
      } else {
        checkArgument(
            globalSubstitutes.containsKey(variableDeclaration),
            "no substitute found for %s",
            variableDeclaration.toASTString());
        return Objects.requireNonNull(globalSubstitutes.get(variableDeclaration));
      }

    } else if (pSimpleDeclaration instanceof CParameterDeclaration parameterDeclaration) {
      if (pCallContext.isEmpty()) {
        // no call context -> main function argument
        return mainFunctionArgSubstitutes.get(parameterDeclaration);
      }
      // normal function called within thread, including start_routines, always have call context
      ThreadEdge callContext = pCallContext.orElseThrow();

      if (parameterSubstitutes.containsKey(callContext)) {
        return getParameterSubstituteByCallContext(callContext, parameterDeclaration);

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

  public CIdExpression getParameterSubstituteByCallContext(
      ThreadEdge pCallContext, CParameterDeclaration pParameterDeclaration) {

    ImmutableMap<CParameterDeclaration, CIdExpression> substitutes =
        Objects.requireNonNull(parameterSubstitutes.get(pCallContext));
    if (substitutes.containsKey(pParameterDeclaration)) {
      return Objects.requireNonNull(substitutes.get(pParameterDeclaration));
    } else {
      // no substitute found -> function declaration contains only parameter types, not names
      // e.g. pthread-driver-races/char_pc8736x_gpio_pc8736x_gpio_configure_pc8736x_gpio_get
      // -> void assume_abort_if_not(int);
      assert pCallContext.cfaEdge instanceof CFunctionCallEdge
          : "call context of parameter declaration must be CFunctionCallEdge";
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) pCallContext.cfaEdge;
      List<CParameterDeclaration> parameterDeclarations =
          functionCallEdge.getFunctionCallExpression().getDeclaration().getParameters();
      // search for the corresponding parameter, throw if not found
      for (CParameterDeclaration parameterDeclarationWithoutName : parameterDeclarations) {
        if (substitutes.containsKey(parameterDeclarationWithoutName)) {
          return Objects.requireNonNull(substitutes.get(parameterDeclarationWithoutName));
        }
      }
    }
    throw new IllegalArgumentException(
        "parameter declaration could not be found for given call context");
  }

  public CVariableDeclaration getLocalVariableDeclarationSubstitute(
      CVariableDeclaration pLocalDeclaration, Optional<ThreadEdge> pCallContext) {

    CIdExpression idExpression = getVariableSubstitute(pLocalDeclaration, pCallContext);
    return (CVariableDeclaration) idExpression.getDeclaration();
  }

  public ImmutableSet<CVariableDeclaration> getGlobalVariablesUsedInLocalVariableDeclaration(
      CVariableDeclaration pLocalDeclaration) {

    checkArgument(
        localSubstitutes.containsKey(pLocalDeclaration),
        "could not find pLocalDeclaration substitute");
    return Objects.requireNonNull(localSubstitutes.get(pLocalDeclaration)).accessedGlobalVariables;
  }

  public <T extends CSimpleDeclaration> T castTo(
      CSimpleDeclaration pSimpleDeclaration, Class<T> pClass) {
    checkArgument(
        pClass.isInstance(pSimpleDeclaration),
        "pSimpleDeclaration must be an instance of %s",
        pClass.getSimpleName());
    return pClass.cast(pSimpleDeclaration);
  }

  /**
   * Whether {@code pSimpleDeclaration} is a {@link CVariableDeclaration} or {@link
   * CParameterDeclaration}. Other declarations such as {@link CFunctionDeclaration}s are not
   * substituted.
   */
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
    for (LocalVariableDeclarationSubstitute localSubstitute : localSubstitutes.values()) {
      for (CIdExpression idExpression : localSubstitute.substitutes.values()) {
        CVariableDeclaration variableDeclaration =
            castTo(idExpression.getDeclaration(), CVariableDeclaration.class);
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
