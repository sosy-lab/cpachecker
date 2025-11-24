// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import java.util.ArrayList;
import java.util.List;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class MPORSubstitution {

  /** Whether this instance servers as a dummy, used only for debugging. */
  @SuppressWarnings("unused")
  private final boolean isDummy;

  private final MPOROptions options;

  public final MPORThread thread;

  // TODO these data structures are bloating up a bit, we should use custom classes.

  /**
   * The map of global variable declarations to their substitutes. {@link Optional#empty()} if this
   * instance serves as a dummy.
   */
  private final ImmutableMap<CVariableDeclaration, CIdExpression> globalVariableSubstitutes;

  /** The map of call context-sensitive thread local variable declarations to their substitutes. */
  private final ImmutableTable<
          Optional<CFAEdgeForThread>, CVariableDeclaration, LocalVariableDeclarationSubstitute>
      localVariableSubstitutes;

  /**
   * The map of parameter to variable declaration substitutes. The {@link CFAEdgeForThread}s allow
   * call-context sensitive parameter substitutes.
   */
  public final ImmutableTable<CFAEdgeForThread, CParameterDeclaration, ImmutableList<CIdExpression>>
      parameterSubstitutes;

  /** Note that main functions cannot take variadic arguments. */
  public final ImmutableMap<CParameterDeclaration, CIdExpression> mainFunctionArgSubstitutes;

  /** Note that main functions cannot take variadic arguments. */
  public final ImmutableTable<CFAEdgeForThread, CParameterDeclaration, CIdExpression>
      startRoutineArgSubstitutes;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  private final LogManager logger;

  MPORSubstitution(
      boolean pIsDummy,
      MPOROptions pOptions,
      MPORThread pThread,
      ImmutableMap<CVariableDeclaration, CIdExpression> pGlobalVariableSubstitutes,
      ImmutableTable<
              Optional<CFAEdgeForThread>, CVariableDeclaration, LocalVariableDeclarationSubstitute>
          pLocalVariableDeclarationSubstitutes,
      ImmutableTable<CFAEdgeForThread, CParameterDeclaration, ImmutableList<CIdExpression>>
          pParameterSubstitutes,
      ImmutableMap<CParameterDeclaration, CIdExpression> pMainFunctionArgSubstitutes,
      ImmutableTable<CFAEdgeForThread, CParameterDeclaration, CIdExpression>
          pStartRoutineArgSubstitutes,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger) {

    isDummy = pIsDummy;
    options = pOptions;
    thread = pThread;
    globalVariableSubstitutes = pGlobalVariableSubstitutes;
    localVariableSubstitutes = pLocalVariableDeclarationSubstitutes;
    parameterSubstitutes = pParameterSubstitutes;
    mainFunctionArgSubstitutes = pMainFunctionArgSubstitutes;
    startRoutineArgSubstitutes = pStartRoutineArgSubstitutes;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
    logger = pLogger;
  }

  // Substitute Functions ==========================================================================

  public CExpression substitute(
      CExpression pExpression,
      boolean pIsDeclaration,
      boolean pIsWrite,
      boolean pIsPointerDereference,
      boolean pIsFieldReference) {

    return substitute(
        pExpression,
        Optional.empty(),
        pIsDeclaration,
        pIsWrite,
        pIsPointerDereference,
        pIsFieldReference,
        Optional.empty());
  }

  public CExpression substituteWithCallContext(
      CExpression pExpression,
      // the call context is still optional, e.g. for expression directly inside main()
      Optional<CFAEdgeForThread> pCallContext,
      boolean pIsDeclaration,
      boolean pIsWrite,
      boolean pIsPointerDereference,
      boolean pIsFieldReference) {

    return substitute(
        pExpression,
        pCallContext,
        pIsDeclaration,
        pIsWrite,
        pIsPointerDereference,
        pIsFieldReference,
        Optional.empty());
  }

  public CExpression substituteWithTracker(
      CExpression pExpression,
      Optional<CFAEdgeForThread> pCallContext,
      boolean pIsDeclaration,
      boolean pIsWrite,
      boolean pIsPointerDereference,
      boolean pIsFieldReference,
      MPORSubstitutionTracker pTracker) {

    return substitute(
        pExpression,
        pCallContext,
        pIsDeclaration,
        pIsWrite,
        pIsPointerDereference,
        pIsFieldReference,
        Optional.of(pTracker));
  }

  /**
   * Substitutes the given expression, and tracks if any global variable was substituted alongside
   * in {@code pAccessedGlobalVariables}. {@code pIsWrite} is used to determine whether the
   * expression to substitute * is written, i.e. a LHS in an assignment.
   */
  private CExpression substitute(
      CExpression pExpression,
      Optional<CFAEdgeForThread> pCallContext,
      boolean pIsDeclaration,
      boolean pIsWrite,
      boolean pIsPointerDereference,
      boolean pIsFieldReference,
      Optional<MPORSubstitutionTracker> pTracker) {

    FileLocation fileLocation = pExpression.getFileLocation();
    CType type = pExpression.getExpressionType();

    switch (pExpression) {
      // shortcut for optimization: never substitute pure int or strings
      case CIntegerLiteralExpression ignored -> {
        return pExpression;
      }
      case CStringLiteralExpression ignored -> {
        return pExpression;
      }
      case CIdExpression idExpression -> {
        CSimpleDeclaration declaration = idExpression.getDeclaration();
        if (SubstituteUtil.isSubstitutable(declaration)) {
          MPORSubstitutionTrackerUtil.trackDeclarationAccess(
              options,
              idExpression,
              pIsWrite,
              pIsPointerDereference,
              pIsFieldReference,
              pTracker,
              logger);
          return getSimpleDeclarationSubstitute(
              idExpression.getDeclaration(), pIsDeclaration, pCallContext, pTracker);
        }
      }
      case CBinaryExpression binary -> {
        // recursively substitute operands of binary expressions
        CExpression op1 =
            substitute(
                binary.getOperand1(),
                pCallContext,
                pIsDeclaration,
                // binary expressions are never LHS in assignments -> no write
                false,
                false,
                false,
                pTracker);
        CExpression op2 =
            substitute(
                binary.getOperand2(),
                pCallContext,
                pIsDeclaration,
                // binary expressions are never LHS in assignments -> no write
                false,
                false,
                false,
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
      }
      case CArraySubscriptExpression arraySubscript -> {
        MPORSubstitutionTrackerUtil.trackPointerDereferenceByArraySubscriptExpression(
            arraySubscript, pIsWrite, pTracker);
        CExpression arrayExpression = arraySubscript.getArrayExpression();
        CExpression subscriptExpression = arraySubscript.getSubscriptExpression();
        CExpression arraySubstitute =
            substitute(
                arrayExpression, pCallContext, pIsDeclaration, pIsWrite, false, false, pTracker);
        // the subscript is not a LHS in an assignment -> no write
        CExpression subscriptSubstitute =
            substitute(
                subscriptExpression, pCallContext, pIsDeclaration, false, false, false, pTracker);
        // only create a new expression if any expr was substituted (compare references)
        if (arraySubstitute != arrayExpression || subscriptSubstitute != subscriptExpression) {
          return new CArraySubscriptExpression(
              fileLocation, type, arraySubstitute, subscriptSubstitute);
        }
      }
      case CFieldReference fieldReference -> {
        MPORSubstitutionTrackerUtil.trackFieldReference(fieldReference, pIsWrite, pTracker);
        CExpression fieldOwnerSubstitute =
            substitute(
                fieldReference.getFieldOwner(),
                pCallContext,
                pIsDeclaration,
                pIsWrite,
                // field->member <==> (*field).member, i.e. pIsPointerDereference may be false here,
                // but the field reference may actually be a pointer dereference
                fieldReference.isPointerDereference(),
                true,
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
      }
      case CUnaryExpression unary -> {
        return new CUnaryExpression(
            unary.getFileLocation(),
            unary.getExpressionType(),
            substitute(
                unary.getOperand(),
                pCallContext,
                pIsDeclaration,
                // unary expressions such as '&var' are never LHS in assignments -> no write
                false,
                false,
                false,
                pTracker),
            unary.getOperator());
      }
      case CPointerExpression pointer -> {
        MPORSubstitutionTrackerUtil.trackPointerDereferenceByPointerExpression(
            pointer, pIsWrite, pTracker);
        return new CPointerExpression(
            pointer.getFileLocation(),
            pointer.getExpressionType(),
            substitute(
                pointer.getOperand(),
                pCallContext,
                pIsDeclaration,
                pIsWrite,
                true,
                false,
                pTracker));
      }
      case CCastExpression cast -> {
        return new CCastExpression(
            cast.getFileLocation(),
            cast.getCastType(),
            substitute(
                cast.getOperand(),
                pCallContext,
                pIsDeclaration,
                // cast expressions are never LHS -> no write
                pIsWrite,
                pIsPointerDereference,
                false,
                pTracker));
      }
      default -> {}
    }
    return pExpression;
  }

  CStatement substitute(
      CStatement pStatement,
      Optional<CFAEdgeForThread> pCallContext,
      Optional<MPORSubstitutionTracker> pTracker) {

    switch (pStatement) {

      // e.g. n = fib(42); or arr[n] = fib(42);
      case CFunctionCallAssignmentStatement functionCallAssignment -> {
        CLeftHandSide leftHandSide = functionCallAssignment.getLeftHandSide();
        CExpression leftHandSideSubstitute =
            substitute(leftHandSide, pCallContext, false, true, false, false, pTracker);
        return new CFunctionCallAssignmentStatement(
            pStatement.getFileLocation(),
            (CLeftHandSide) leftHandSideSubstitute,
            substitute(functionCallAssignment.getRightHandSide(), pCallContext, pTracker));
      }

      // e.g. fib(42);
      case CFunctionCallStatement functionCall -> {
        return new CFunctionCallStatement(
            functionCall.getFileLocation(),
            substitute(functionCall.getFunctionCallExpression(), pCallContext, pTracker));
      }

      // e.g. int x = 42;
      case CExpressionAssignmentStatement assignment -> {
        MPORSubstitutionTrackerUtil.trackPointerAssignment(assignment, pTracker);
        CLeftHandSide leftHandSide = assignment.getLeftHandSide();
        CExpression rightHandSide = assignment.getRightHandSide();
        CExpression leftHandSideSubstitute =
            substitute(leftHandSide, pCallContext, false, true, false, false, pTracker);
        return new CExpressionAssignmentStatement(
            pStatement.getFileLocation(),
            (CLeftHandSide) leftHandSideSubstitute,
            // for the RHS, it's not a left hand side of an assignment
            substitute(rightHandSide, pCallContext, false, false, false, false, pTracker));
      }

      case CExpressionStatement expression -> {
        return new CExpressionStatement(
            pStatement.getFileLocation(),
            substitute(
                expression.getExpression(), pCallContext, false, false, false, false, pTracker));
      }
      default -> {}
    }
    return pStatement;
  }

  private CFunctionCallExpression substitute(
      CFunctionCallExpression pFunctionCallExpression,
      Optional<CFAEdgeForThread> pCallContext,
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
      Optional<CFAEdgeForThread> pCallContext,
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

  /**
   * Returns the global, local or parameter {@link CIdExpression} substitute for {@code
   * pSimpleDeclaration}.
   */
  private CIdExpression getSimpleDeclarationSubstitute(
      CSimpleDeclaration pSimpleDeclaration,
      boolean pIsDeclaration,
      Optional<CFAEdgeForThread> pCallContext,
      Optional<MPORSubstitutionTracker> pTracker) {

    if (pSimpleDeclaration instanceof CVariableDeclaration variableDeclaration) {
      if (localVariableSubstitutes.contains(pCallContext, variableDeclaration)) {
        LocalVariableDeclarationSubstitute localSubstitute =
            Objects.requireNonNull(localVariableSubstitutes.get(pCallContext, variableDeclaration));
        MPORSubstitutionTrackerUtil.trackContentFromLocalVariableDeclaration(
            pIsDeclaration, localSubstitute, pTracker);
        return localSubstitute.getIdExpression();
      } else {
        return Objects.requireNonNull(globalVariableSubstitutes.get(variableDeclaration));
      }

    } else if (pSimpleDeclaration instanceof CParameterDeclaration parameterDeclaration) {
      if (pCallContext.isEmpty()) {
        // no call context -> main function argument
        MPORSubstitutionTrackerUtil.trackMainFunctionArg(parameterDeclaration, pTracker);
        return mainFunctionArgSubstitutes.get(parameterDeclaration);
      }
      // normal function called within thread, including start_routines, always have call context
      CFAEdgeForThread callContext = pCallContext.orElseThrow();
      if (parameterSubstitutes.containsRow(callContext)) {
        ImmutableList<CIdExpression> parameterDeclarationSubstitutes =
            getParameterDeclarationSubstitute(callContext, parameterDeclaration);
        // this means we only support substituting parameters that are not variadic.
        // i.e. a variadic function can be called, but its body not handled (at the moment)
        return Objects.requireNonNull(parameterDeclarationSubstitutes).getFirst();

      } else if (startRoutineArgSubstitutes.containsRow(callContext)) {
        return startRoutineArgSubstitutes.get(callContext, parameterDeclaration);
      }
      throw new IllegalArgumentException("parameter declaration could not be found");
    }
    throw new IllegalArgumentException(
        "pSimpleDeclaration must be CVariable- or CParameterDeclaration");
  }

  CVariableDeclaration getVariableDeclarationSubstitute(
      CVariableDeclaration pVariableDeclaration,
      Optional<CFAEdgeForThread> pCallContext,
      Optional<MPORSubstitutionTracker> pTracker) {

    MPORSubstitutionTrackerUtil.trackPointerAssignmentInVariableDeclaration(
        pVariableDeclaration, pTracker);
    CIdExpression idExpression =
        getSimpleDeclarationSubstitute(pVariableDeclaration, true, pCallContext, pTracker);
    return (CVariableDeclaration) idExpression.getDeclaration();
  }

  // CParameterDeclaration substitutes =============================================================

  public ImmutableList<CIdExpression> getParameterDeclarationSubstitute(
      CFAEdgeForThread pCallContext, CParameterDeclaration pParameterDeclaration) {

    if (parameterSubstitutes.containsColumn(pParameterDeclaration)) {
      return parameterSubstitutes.get(pCallContext, pParameterDeclaration);
    } else {
      // no substitute found -> function declaration contains only parameter types, not names
      // e.g. pthread-driver-races/char_pc8736x_gpio_pc8736x_gpio_configure_pc8736x_gpio_get
      // -> void assume_abort_if_not(int);
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) pCallContext.cfaEdge;
      List<CParameterDeclaration> parameterDeclarations =
          functionCallEdge.getFunctionCallExpression().getDeclaration().getParameters();
      // search for the corresponding parameter, throw if not found
      for (CParameterDeclaration parameterDeclarationWithoutName : parameterDeclarations) {
        if (parameterSubstitutes.containsColumn(parameterDeclarationWithoutName)) {
          return parameterSubstitutes.get(pCallContext, parameterDeclarationWithoutName);
        }
      }
    }
    throw new IllegalArgumentException(
        "parameter declaration could not be found for given call context");
  }

  // Declaration Extraction ========================================================================

  public ImmutableList<CVariableDeclaration> getGlobalVariableDeclarationSubstitutes() {
    ImmutableList.Builder<CVariableDeclaration> rGlobalDeclarations = ImmutableList.builder();
    for (CIdExpression globalVariable : globalVariableSubstitutes.values()) {
      rGlobalDeclarations.add((CVariableDeclaration) globalVariable.getDeclaration());
    }
    return rGlobalDeclarations.build();
  }

  public ImmutableList<CVariableDeclaration> getLocalVariableDeclarationSubstitutes() {
    ImmutableList.Builder<CVariableDeclaration> rLocalDeclarations = ImmutableList.builder();
    for (LocalVariableDeclarationSubstitute localSubstitute : localVariableSubstitutes.values()) {
      rLocalDeclarations.add(localSubstitute.getSubstituteVariableDeclaration());
    }
    return rLocalDeclarations.build();
  }

  /**
   * Note that these are not {@link CParameterDeclaration} but {@link CSimpleDeclaration} because
   * they are treated as variables in the sequentialization (inlining functions).
   */
  public ImmutableList<CVariableDeclaration> getParameterDeclarationSubstitutes() {
    ImmutableList.Builder<CVariableDeclaration> rParameterDeclarations = ImmutableList.builder();
    for (var cell : parameterSubstitutes.cellSet()) {
      for (CIdExpression idExpression : cell.getValue()) {
        rParameterDeclarations.add((CVariableDeclaration) idExpression.getDeclaration());
      }
    }
    return rParameterDeclarations.build();
  }

  public ImmutableList<CVariableDeclaration> getStartRoutineArgDeclarationSubstitutes() {
    ImmutableList.Builder<CVariableDeclaration> rStartRoutineArgDeclarations =
        ImmutableList.builder();
    for (var cell : startRoutineArgSubstitutes.cellSet()) {
      rStartRoutineArgDeclarations.add((CVariableDeclaration) cell.getValue().getDeclaration());
    }
    return rStartRoutineArgDeclarations.build();
  }

  public MPORThread getThread() {
    return thread;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        isDummy,
        options,
        thread,
        globalVariableSubstitutes,
        localVariableSubstitutes,
        parameterSubstitutes,
        mainFunctionArgSubstitutes,
        startRoutineArgSubstitutes);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof MPORSubstitution other
        && isDummy == other.isDummy
        && options.equals(other.options)
        && thread.equals(other.thread)
        && globalVariableSubstitutes.equals(other.globalVariableSubstitutes)
        && localVariableSubstitutes.equals(other.localVariableSubstitutes)
        && parameterSubstitutes.equals(other.parameterSubstitutes)
        && mainFunctionArgSubstitutes.equals(other.mainFunctionArgSubstitutes)
        && startRoutineArgSubstitutes.equals(other.startRoutineArgSubstitutes);
  }
}
