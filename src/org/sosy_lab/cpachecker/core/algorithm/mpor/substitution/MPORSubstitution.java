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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
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
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
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
   * The map of parameter to variable declaration substitutes. The {@link ThreadEdge}s allow
   * call-context sensitive parameter substitutes.
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

  // Tracker Functions =============================================================================

  /**
   * If applicable, adds the {@link CVariableDeclaration} of {@code pIdExpression} to the respective
   * sets. {@code pIsWrite} is used to determine whether the expression to substitute is written,
   * i.e. a LHS in an assignment.
   */
  private void trackGlobalVariableAccesses(
      CIdExpression pIdExpression,
      boolean pIsWrite,
      boolean pIsPointerDereference,
      boolean pIsFieldReference,
      Optional<MPORSubstitutionTracker> pTracker) {

    // writing pointers (aliasing) may not be allowed -> reject program
    InputRejection.checkPointerWrite(pIsWrite, options, pIdExpression, logger);

    // exclude field references, we track field members separately. field owner is tracked via the
    // CIdExpression, e.g. if we assign struct_a = struct_b without any field reference.
    if (pTracker.isEmpty() || pIsFieldReference) {
      return;
    }
    // otherwise, if applicable, add declaration to global reads/writes
    if (pIdExpression.getDeclaration() instanceof CVariableDeclaration variableDeclaration) {
      if (variableDeclaration.isGlobal()) {
        // exclude pointer dereferences, they are handled separately
        if (!pIsPointerDereference) {
          pTracker.orElseThrow().addAccessedGlobalVariable(variableDeclaration);
          CType type = variableDeclaration.getType();
          boolean isMutex = PthreadObjectType.PTHREAD_MUTEX_T.equalsType(type);
          // treat pthread_mutex_t lock/unlock as writes, otherwise interleavings are lost
          if (pIsWrite || isMutex) {
            pTracker.orElseThrow().addWrittenGlobalVariable(variableDeclaration);
          }
        }
      }
    }
  }

  private void trackGlobalVariableAccessedInLocalVariableDeclaration(
      LocalVariableDeclarationSubstitute pLocalSubstitute,
      Optional<MPORSubstitutionTracker> pTracker) {

    if (pTracker.isEmpty()) {
      return;
    }
    for (CVariableDeclaration globalVariable : pLocalSubstitute.accessedGlobalVariables) {
      pTracker.orElseThrow().addAccessedGlobalVariable(globalVariable);
    }
  }

  private void trackMainFunctionArg(
      CParameterDeclaration pMainFunctionArg, Optional<MPORSubstitutionTracker> pTracker) {

    if (pTracker.isEmpty()) {
      return;
    }
    pTracker.orElseThrow().addAccessedMainFunctionArg(pMainFunctionArg);
  }

  private void trackPointerAssignment(
      CExpressionAssignmentStatement pAssignment, Optional<MPORSubstitutionTracker> pTracker) {

    if (pTracker.isEmpty()) {
      return;
    }
    CLeftHandSide leftHandSide = pAssignment.getLeftHandSide();
    if (leftHandSide instanceof CIdExpression lhsId) {
      if (lhsId.getDeclaration() instanceof CVariableDeclaration lhsDeclaration) {
        if (lhsDeclaration.getType() instanceof CPointerType) {
          CExpression rightHandSide = pAssignment.getRightHandSide();
          Optional<CSimpleDeclaration> pointerDeclaration =
              MPORUtil.tryGetPointerDeclaration(rightHandSide);
          if (pointerDeclaration.isPresent()) {
            pTracker
                .orElseThrow()
                .addPointerAssignment(lhsDeclaration, pointerDeclaration.orElseThrow());
          } else {
            Optional<Entry<CSimpleDeclaration, CCompositeTypeMemberDeclaration>>
                fieldMemberPointer = MPORUtil.tryGetFieldMemberPointer(rightHandSide);
            if (fieldMemberPointer.isPresent()) {
              pTracker
                  .orElseThrow()
                  .addPointerFieldMemberAssignment(
                      lhsDeclaration,
                      fieldMemberPointer.orElseThrow().getKey(),
                      fieldMemberPointer.orElseThrow().getValue());
            }
          }
        }
      }
    }
  }

  private void trackPointerAssignmentInVariableDeclaration(
      CVariableDeclaration pVariableDeclaration, Optional<MPORSubstitutionTracker> pTracker) {

    if (pTracker.isEmpty()) {
      return;
    }
    if (pVariableDeclaration.getType() instanceof CPointerType) {
      CInitializer initializer = pVariableDeclaration.getInitializer();
      if (initializer instanceof CInitializerExpression initializerExpression) {
        Optional<CSimpleDeclaration> initializerDeclaration =
            MPORUtil.tryGetPointerDeclaration(initializerExpression.getExpression());
        if (initializerDeclaration.isPresent()) {
          CSimpleDeclaration pointerDeclaration = initializerDeclaration.orElseThrow();
          if (isSubstitutable(pointerDeclaration)) {
            pTracker.orElseThrow().addPointerAssignment(pVariableDeclaration, pointerDeclaration);
          }
        }
      }
    }
  }

  // TODO also need CFieldReference, CArraySubscriptExpression here
  private void trackPointerDereference(
      CPointerExpression pPointerExpression,
      boolean pIsWrite,
      Optional<MPORSubstitutionTracker> pTracker) {

    if (pPointerExpression.getOperand() instanceof CIdExpression idExpression) {
      // do not consider CFunctionDeclarations
      if (isSubstitutable(idExpression.getDeclaration())) {
        if (pTracker.isPresent()) {
          if (pIsWrite) {
            pTracker.orElseThrow().addWrittenPointerDereference(idExpression.getDeclaration());
          }
          pTracker.orElseThrow().addAccessedPointerDereference(idExpression.getDeclaration());
        }
      }
    }
  }

  private void trackFieldReference(
      CFieldReference pFieldReference,
      boolean pIsWrite,
      Optional<MPORSubstitutionTracker> pTracker) {

    if (pTracker.isEmpty()) {
      return;
    }
    // TODO distinguish idExpression.declaration -> is parameter / variable?
    // CIdExpression is e.g. 'queue' in 'queue->amount'
    if (pFieldReference.getFieldOwner() instanceof CIdExpression idExpression) {
      // TODO add different sets in tracker for pointer derefs vs. variables?
      // typedef is e.g. 'QType' or for pointers 'QType*'
      if (idExpression.getExpressionType() instanceof CTypedefType typedefType) {
        trackFieldReferenceByTypedefType(
            pFieldReference, idExpression, typedefType, pIsWrite, pTracker.orElseThrow());

      } else if (idExpression.getExpressionType() instanceof CPointerType pointerType) {
        if (pointerType.getType() instanceof CTypedefType typedefType) {
          trackFieldReferenceByTypedefType(
              pFieldReference, idExpression, typedefType, pIsWrite, pTracker.orElseThrow());
        }
      }

    } else if (pFieldReference.getFieldOwner() instanceof CFieldReference fieldReference) {
      // recursively handle inner structs until outer struct is found, e.g. outer.inner.member
      trackFieldReference(fieldReference, pIsWrite, pTracker);
    }
  }

  private void trackFieldReferenceByTypedefType(
      CFieldReference pFieldReference,
      CIdExpression pIdExpression,
      CTypedefType pTypedefType,
      boolean pIsWrite,
      MPORSubstitutionTracker pTracker) {

    // elaborated type is e.g. struct __anon_type_QType
    if (pTypedefType.getRealType() instanceof CElaboratedType elaboratedType) {
      // composite type contains the composite type members, e.g. 'amount'
      if (elaboratedType.getRealType() instanceof CCompositeType compositeType) {
        for (CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
          if (memberDeclaration.getName().equals(pFieldReference.getFieldName())) {
            // TODO need tracking based on declaration so that we map field declaration -> member
            if (pIsWrite) {
              pTracker.addWrittenFieldMember(pIdExpression.getDeclaration(), memberDeclaration);
            }
            pTracker.addAccessedFieldMember(pIdExpression.getDeclaration(), memberDeclaration);
          }
        }
      }
    }
  }

  // Substitute Functions ==========================================================================

  /**
   * Substitutes the given expression, and tracks if any global variable was substituted alongside
   * in {@code pAccessedGlobalVariables}. {@code pIsWrite} is used to determine whether the
   * expression to substitute * is written, i.e. a LHS in an assignment.
   */
  public CExpression substitute(
      final CExpression pExpression,
      final Optional<ThreadEdge> pCallContext,
      boolean pIsWrite,
      boolean pIsPointerDereference,
      boolean pIsFieldReference,
      boolean pIsUnaryAmper,
      Optional<MPORSubstitutionTracker> pTracker) {

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
        trackGlobalVariableAccesses(
            idExpression, pIsWrite, pIsPointerDereference, pIsFieldReference, pTracker);
        return getVariableSubstitute(idExpression.getDeclaration(), pCallContext, pTracker);
      }
      // when accessing function pointers e.g. &func. this is also possible without the unary amper
      // operator '&', but the example tasks used only this expression, so we restrict it.
      if (pIsUnaryAmper) {
        if (declaration instanceof CFunctionDeclaration functionDeclaration) {
          if (pTracker.isPresent()) {
            pTracker.orElseThrow().addAccessedFunctionPointer(functionDeclaration);
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
              false,
              false,
              pIsUnaryAmper,
              pTracker);
      CExpression op2 =
          substitute(
              binary.getOperand2(),
              pCallContext,
              // binary expressions are never LHS in assignments -> no write
              false,
              false,
              false,
              pIsUnaryAmper,
              pTracker);
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
          substitute(arrayExpression, pCallContext, true, false, false, pIsUnaryAmper, pTracker);
      // the subscript is not a LHS in an assignment -> no write
      CExpression subscriptSubstitute =
          substitute(
              subscriptExpression, pCallContext, false, false, false, pIsUnaryAmper, pTracker);
      // only create a new expression if any expr was substituted (compare references)
      if (arraySubstitute != arrayExpression || subscriptSubstitute != subscriptExpression) {
        return new CArraySubscriptExpression(
            fileLocation, type, arraySubstitute, subscriptSubstitute);
      }

    } else if (pExpression instanceof CFieldReference fieldReference) {
      trackFieldReference(fieldReference, pIsWrite, pTracker);
      CExpression fieldOwnerSubstitute =
          substitute(
              fieldReference.getFieldOwner(),
              pCallContext,
              pIsWrite,
              // field->member <==> (*field).member, i.e. pIsPointerDereference may be false here,
              // but the field reference may actually be a pointer dereference
              fieldReference.isPointerDereference(),
              true,
              pIsUnaryAmper,
              pTracker);
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
              false,
              false,
              unary.getOperator().equals(UnaryOperator.AMPER),
              pTracker),
          unary.getOperator());

    } else if (pExpression instanceof CPointerExpression pointer) {
      trackPointerDereference(pointer, pIsWrite, pTracker);
      return new CPointerExpression(
          pointer.getFileLocation(),
          pointer.getExpressionType(),
          substitute(
              pointer.getOperand(), pCallContext, pIsWrite, true, false, pIsUnaryAmper, pTracker));

    } else if (pExpression instanceof CCastExpression cast) {
      return new CCastExpression(
          cast.getFileLocation(),
          cast.getCastType(),
          substitute(
              cast.getOperand(),
              pCallContext,
              // cast expressions are never LHS -> no write
              pIsWrite,
              pIsPointerDereference,
              false,
              pIsUnaryAmper,
              pTracker));
    }

    return pExpression;
  }

  CStatement substitute(
      CStatement pStatement,
      Optional<ThreadEdge> pCallContext,
      Optional<MPORSubstitutionTracker> pTracker) {

    FileLocation fileLocation = pStatement.getFileLocation();

    // e.g. n = fib(42); or arr[n] = fib(42);
    if (pStatement instanceof CFunctionCallAssignmentStatement functionCallAssignment) {
      CLeftHandSide leftHandSide = functionCallAssignment.getLeftHandSide();
      CFunctionCallExpression rightHandSide = functionCallAssignment.getRightHandSide();
      // TODO need CFieldReference, CPointerExpression, ... here too
      if (leftHandSide instanceof CIdExpression idExpression) {
        CExpression substitute =
            substitute(idExpression, pCallContext, false, false, false, false, pTracker);
        if (substitute instanceof CIdExpression idExpressionSubstitute) {
          return new CFunctionCallAssignmentStatement(
              fileLocation,
              idExpressionSubstitute,
              substitute(rightHandSide, pCallContext, pTracker));
        }
      } else if (leftHandSide instanceof CArraySubscriptExpression arraySubscriptExpression) {
        CExpression substitute =
            substitute(
                arraySubscriptExpression, pCallContext, false, false, false, false, pTracker);
        if (substitute instanceof CArraySubscriptExpression arraySubscriptExpressionSubstitute) {
          return new CFunctionCallAssignmentStatement(
              fileLocation,
              arraySubscriptExpressionSubstitute,
              substitute(rightHandSide, pCallContext, pTracker));
        }
      }

      // e.g. fib(42);
    } else if (pStatement instanceof CFunctionCallStatement functionCall) {
      return new CFunctionCallStatement(
          functionCall.getFileLocation(),
          substitute(functionCall.getFunctionCallExpression(), pCallContext, pTracker));

      // e.g. int x = 42;
    } else if (pStatement instanceof CExpressionAssignmentStatement assignment) {
      trackPointerAssignment(assignment, pTracker);
      CLeftHandSide leftHandSide = assignment.getLeftHandSide();
      CExpression rightHandSide = assignment.getRightHandSide();
      CExpression substitute =
          substitute(leftHandSide, pCallContext, true, false, false, false, pTracker);
      if (substitute instanceof CLeftHandSide leftHandSideSubstitute) {
        return new CExpressionAssignmentStatement(
            fileLocation,
            leftHandSideSubstitute,
            // for the RHS, it's not a left hand side of an assignment
            substitute(rightHandSide, pCallContext, false, false, false, false, pTracker));
      }

    } else if (pStatement instanceof CExpressionStatement expression) {
      return new CExpressionStatement(
          fileLocation,
          substitute(
              expression.getExpression(), pCallContext, false, false, false, false, pTracker));
    }

    return pStatement;
  }

  CFunctionCallExpression substitute(
      CFunctionCallExpression pFunctionCallExpression,
      Optional<ThreadEdge> pCallContext,
      Optional<MPORSubstitutionTracker> pTracker) {

    // substitute all parameters in the function call expression
    List<CExpression> parameters = new ArrayList<>();
    for (CExpression expression : pFunctionCallExpression.getParameterExpressions()) {
      parameters.add(substitute(expression, pCallContext, false, false, false, false, pTracker));
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
            false,
            false,
            pTracker);
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
      Optional<MPORSubstitutionTracker> pTracker) {

    if (pReturnStatement.getReturnValue().isEmpty()) {
      // return as-is if there is no expression to substitute
      return pReturnStatement;
    } else {
      CExpression expression = pReturnStatement.getReturnValue().orElseThrow();
      // TODO it would be cleaner to also substitute the assignment...
      return new CReturnStatement(
          pReturnStatement.getFileLocation(),
          Optional.of(substitute(expression, pCallContext, false, false, false, false, pTracker)),
          pReturnStatement.asAssignment());
    }
  }

  /** Returns the global, local or param {@link CIdExpression} substitute of pSimpleDeclaration. */
  private CIdExpression getVariableSubstitute(
      CSimpleDeclaration pSimpleDeclaration,
      Optional<ThreadEdge> pCallContext,
      Optional<MPORSubstitutionTracker> pTracker) {

    if (pSimpleDeclaration instanceof CVariableDeclaration variableDeclaration) {
      if (localSubstitutes.containsKey(variableDeclaration)) {
        LocalVariableDeclarationSubstitute localSubstitute =
            Objects.requireNonNull(localSubstitutes.get(variableDeclaration));
        ImmutableMap<Optional<ThreadEdge>, CIdExpression> substitutes =
            Objects.requireNonNull(localSubstitute.substitutes);
        trackGlobalVariableAccessedInLocalVariableDeclaration(localSubstitute, pTracker);
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
        trackMainFunctionArg(parameterDeclaration, pTracker);
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

  public CVariableDeclaration getVariableDeclarationSubstitute(
      CVariableDeclaration pVariableDeclaration,
      Optional<ThreadEdge> pCallContext,
      Optional<MPORSubstitutionTracker> pTracker) {

    trackPointerAssignmentInVariableDeclaration(pVariableDeclaration, pTracker);
    CIdExpression idExpression =
        getVariableSubstitute(pVariableDeclaration, pCallContext, pTracker);
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
  public ImmutableList<CParameterDeclaration> getParameterDeclarations() {
    ImmutableList.Builder<CParameterDeclaration> rParameterDeclarations = ImmutableList.builder();
    for (ImmutableMap<CParameterDeclaration, CIdExpression> substitutes :
        parameterSubstitutes.values()) {
      for (CIdExpression parameter : substitutes.values()) {
        checkArgument(parameter.getDeclaration() instanceof CParameterDeclaration);
        rParameterDeclarations.add((CParameterDeclaration) parameter.getDeclaration());
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
