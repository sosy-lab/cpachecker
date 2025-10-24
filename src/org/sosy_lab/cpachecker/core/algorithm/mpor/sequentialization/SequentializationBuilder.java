// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqInitializerBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqFunctionDeclarations;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqVariableDeclarations;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqMainFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqReachErrorFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqThreadSimulationFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.declaration.SeqBitVectorDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.declaration.SeqBitVectorDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThreadUtil;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SequentializationBuilder {

  public static String buildBitVectorTypeDeclarations() {
    StringJoiner rDeclarations = new StringJoiner(SeqSyntax.NEWLINE);
    for (BitVectorDataType bitVectorType : BitVectorDataType.values()) {
      CTypeDeclaration bitVectorTypeDeclaration = bitVectorType.buildDeclaration();
      rDeclarations.add(bitVectorTypeDeclaration.toASTString());
    }
    return rDeclarations.toString();
  }

  public static String buildInputFunctionAndTypeDeclarations(
      MPOROptions pOptions, ImmutableList<MPORThread> pThreads) {

    StringJoiner rDeclarations = new StringJoiner(SeqSyntax.NEWLINE);
    if (pOptions.comments) {
      rDeclarations.add(SeqComment.UNCHANGED_DECLARATIONS);
    }
    // add all original program declarations that are not substituted
    for (MPORThread thread : pThreads) {
      ImmutableList<CDeclaration> nonVariableDeclarations =
          MPORThreadUtil.extractNonVariableDeclarations(thread);
      for (CDeclaration declaration : nonVariableDeclarations) {
        // add function and type declaration only if enabled in options
        if (!(declaration instanceof CFunctionDeclaration) || pOptions.inputFunctionDeclarations) {
          if (!(declaration instanceof CTypeDeclaration) || pOptions.inputTypeDeclarations) {
            rDeclarations.add(declaration.toASTString());
          }
        }
      }
    }
    return rDeclarations.toString();
  }

  // Input Variable Declarations ===================================================================

  public static String buildInputGlobalVariableDeclarations(
      MPOROptions pOptions, MPORSubstitution pMainThreadSubstitution) {

    StringJoiner rDeclarations = new StringJoiner(SeqSyntax.NEWLINE);
    if (pOptions.comments) {
      rDeclarations.add(SeqComment.GLOBAL_VAR_DECLARATIONS);
    }
    ImmutableList<CVariableDeclaration> globalDeclarations =
        pMainThreadSubstitution.getGlobalDeclarations();
    for (CVariableDeclaration globalDeclaration : globalDeclarations) {
      if (!PthreadUtil.isPthreadObjectType(globalDeclaration.getType())) {
        rDeclarations.add(globalDeclaration.toASTString());
      }
    }
    return rDeclarations.toString();
  }

  public static String buildInputLocalVariableDeclarations(
      MPOROptions pOptions, ImmutableList<MPORSubstitution> pSubstitutions) {

    StringJoiner rDeclarations = new StringJoiner(SeqSyntax.NEWLINE);
    if (pOptions.comments) {
      rDeclarations.add(SeqComment.LOCAL_VAR_DECLARATIONS);
    }
    for (MPORSubstitution substitution : pSubstitutions) {
      ImmutableList<CVariableDeclaration> localDeclarations = substitution.getLocalDeclarations();
      for (CVariableDeclaration localDeclaration : localDeclarations) {
        Optional<String> line = tryBuildInputLocalVariableDeclaration(localDeclaration);
        if (line.isPresent()) {
          rDeclarations.add(line.orElseThrow());
        }
      }
    }
    return rDeclarations.toString();
  }

  private static Optional<String> tryBuildInputLocalVariableDeclaration(
      CVariableDeclaration pLocalVariableDeclaration) {

    checkArgument(!pLocalVariableDeclaration.isGlobal(), "pLocalVariableDeclaration must be local");
    // try remove const qualifier from variable
    if (pLocalVariableDeclaration.getType().getQualifiers().containsConst()) {
      return tryBuildInputConstLocalVariableDeclaration(pLocalVariableDeclaration);
    }
    // otherwise, for non-const variables
    if (!PthreadUtil.isPthreadObjectType(pLocalVariableDeclaration.getType())) {
      CInitializer initializer = pLocalVariableDeclaration.getInitializer();
      if (initializer == null) {
        // no initializer -> add declaration as is
        return Optional.of(pLocalVariableDeclaration.toASTString());

      } else if (MPORUtil.isFunctionPointer(pLocalVariableDeclaration.getInitializer())) {
        // function pointer initializer -> add declaration as is
        return Optional.of(pLocalVariableDeclaration.toASTString());

      } else if (!MPORUtil.isConstCpaCheckerTmp(pLocalVariableDeclaration)) {
        // const CPAchecker_TMP variables are declared and initialized directly in the case.
        // everything else: add declaration without initializer (and assign later in cases)
        return Optional.of(
            pLocalVariableDeclaration.toASTStringWithoutInitializer(
                AAstNodeRepresentation.DEFAULT));
      }
    }
    return Optional.empty();
  }

  private static Optional<String> tryBuildInputConstLocalVariableDeclaration(
      CVariableDeclaration pLocalVariableDeclaration) {

    checkArgument(!pLocalVariableDeclaration.isGlobal(), "pLocalVariableDeclaration must be local");
    checkArgument(
        pLocalVariableDeclaration.getType().getQualifiers().containsConst(),
        "pLocalVariableDeclaration must be const");

    // create an identical copy of pLocalVariableDeclaration, but remove const qualifier
    CType type = pLocalVariableDeclaration.getType();
    CType typeWithoutConst = type.withQualifiersSetTo(type.getQualifiers().withoutConst());
    CVariableDeclaration variableDeclarationWithoutConst =
        new CVariableDeclaration(
            pLocalVariableDeclaration.getFileLocation(),
            pLocalVariableDeclaration.isGlobal(),
            pLocalVariableDeclaration.getCStorageClass(),
            typeWithoutConst,
            pLocalVariableDeclaration.getName(),
            pLocalVariableDeclaration.getOrigName(),
            pLocalVariableDeclaration.getQualifiedName(),
            pLocalVariableDeclaration.getInitializer());
    return tryBuildInputLocalVariableDeclaration(variableDeclarationWithoutConst);
  }

  // Input Parameter Declarations ==================================================================

  public static String buildInputParameterDeclarations(
      MPOROptions pOptions, ImmutableList<MPORSubstitution> pSubstitutions) {

    StringJoiner rDeclarations = new StringJoiner(SeqSyntax.NEWLINE);
    if (pOptions.comments) {
      rDeclarations.add(SeqComment.PARAMETER_VAR_SUBSTITUTES);
    }
    for (MPORSubstitution substitution : pSubstitutions) {
      ImmutableList<CParameterDeclaration> parameterDeclarations =
          substitution.getSubstituteParameterDeclarations();
      for (CParameterDeclaration parameterDeclaration : parameterDeclarations) {
        if (!PthreadUtil.isPthreadObjectType(parameterDeclaration.getType())) {
          // CParameterDeclarations require addition semicolon
          rDeclarations.add(parameterDeclaration.toASTString() + SeqSyntax.SEMICOLON);
        }
      }
    }
    return rDeclarations.toString();
  }

  /**
   * Adds the declarations of main function arguments, e.g. {@code int arg;} that are
   * non-deterministically initialized in {@code main()} later in the sequentialization.
   */
  public static String buildMainFunctionArgDeclarations(
      MPOROptions pOptions, MPORSubstitution pMainThreadSubstitution) {

    StringJoiner rDeclarations = new StringJoiner(SeqSyntax.NEWLINE);
    if (pOptions.comments) {
      rDeclarations.add(SeqComment.MAIN_FUNCTION_ARG_SUBSTITUTES);
    }
    for (CIdExpression mainArg : pMainThreadSubstitution.mainFunctionArgSubstitutes.values()) {
      rDeclarations.add(mainArg.getDeclaration().toASTString());
    }
    return rDeclarations.toString();
  }

  public static String buildStartRoutineArgDeclarations(
      MPOROptions pOptions, MPORSubstitution pMainThreadSubstitution) {

    StringJoiner rDeclarations = new StringJoiner(SeqSyntax.NEWLINE);
    if (pOptions.comments) {
      rDeclarations.add(SeqComment.START_ROUTINE_ARG_SUBSTITUTES);
    }
    ImmutableList<CParameterDeclaration> startRoutineArgDeclarations =
        pMainThreadSubstitution.getSubstituteStartRoutineArgDeclarations();
    for (CParameterDeclaration startRoutineArgDeclaration : startRoutineArgDeclarations) {
      // TODO why exclude pthread objects here? add explaining comment
      if (!PthreadUtil.isPthreadObjectType(startRoutineArgDeclaration.getType())) {
        // add trailing ; as CParameterDeclaration is without semicolons
        rDeclarations.add(startRoutineArgDeclaration.toASTString() + SeqSyntax.SEMICOLON);
      }
    }
    return rDeclarations.toString();
  }

  public static String buildStartRoutineExitDeclarations(
      MPOROptions pOptions, ImmutableList<MPORThread> pThreads) {

    StringJoiner rDeclarations = new StringJoiner(SeqSyntax.NEWLINE);
    if (pOptions.comments) {
      rDeclarations.add(SeqComment.START_ROUTINE_EXIT_VARIABLES);
    }
    for (MPORThread thread : pThreads) {
      Optional<CIdExpression> exitVariable = thread.startRoutineExitVariable;
      if (exitVariable.isPresent()) {
        rDeclarations.add(exitVariable.orElseThrow().getDeclaration().toASTString());
      }
    }
    return rDeclarations.toString();
  }

  // Function Declarations and Definitions =========================================================

  public static String buildFunctionDeclarations(
      MPOROptions pOptions, SequentializationFields pFields) {

    StringJoiner rDeclarations = new StringJoiner(SeqSyntax.NEWLINE);
    if (pOptions.comments) {
      rDeclarations.add(SeqComment.CUSTOM_FUNCTION_DECLARATIONS);
    }

    // reach_error, abort, assert, nondet_int may be duplicate depending on the input program
    if (pOptions.nondeterminismSigned) {
      rDeclarations.add(VerifierNondetFunctionType.INT.getFunctionDeclaration().toASTString());
    } else {
      rDeclarations.add(VerifierNondetFunctionType.UINT.getFunctionDeclaration().toASTString());
    }
    rDeclarations.add(SeqFunctionDeclarations.REACH_ERROR.toASTString());
    rDeclarations.add(SeqFunctionDeclarations.ASSERT_FAIL.toASTString());
    rDeclarations.add(SeqFunctionDeclarations.ASSUME.toASTString());
    rDeclarations.add(SeqFunctionDeclarations.ABORT.toASTString());

    // malloc is required for valid-memsafety tasks
    rDeclarations.add(SeqFunctionDeclarations.MALLOC.toASTString());

    // thread simulation functions, only enabled with loop is unrolled
    if (pOptions.loopUnrolling) {
      for (MPORThread thread : pFields.threads) {
        CFunctionDeclaration threadSimulationFunctionDeclaration =
            SeqDeclarationBuilder.buildThreadSimulationFunctionDeclaration(thread.getId());
        rDeclarations.add(threadSimulationFunctionDeclaration.toASTString());
      }
    }
    // main should always be duplicate
    rDeclarations.add(SeqFunctionDeclarations.MAIN.toASTString());
    return rDeclarations.toString();
  }

  public static String buildFunctionDefinitions(
      MPOROptions pOptions, SequentializationFields pFields, SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    StringJoiner rDefinitions = new StringJoiner(SeqSyntax.NEWLINE);
    if (pOptions.comments) {
      rDefinitions.add(SeqComment.CUSTOM_FUNCTION_DEFINITIONS);
    }
    // custom function definitions: reach_error(), assume(), main()
    SeqReachErrorFunction reachError = new SeqReachErrorFunction();
    rDefinitions.add(reachError.buildDefinition());

    CBinaryExpression condEqualsZeroExpression =
        pUtils
            .getBinaryExpressionBuilder()
            .buildBinaryExpression(
                SeqIdExpressions.COND, SeqIntegerLiteralExpressions.INT_0, BinaryOperator.EQUALS);
    SeqAssumeFunction assume = new SeqAssumeFunction(condEqualsZeroExpression);
    rDefinitions.add(assume.buildDefinition());
    // create separate thread simulation functions, if enabled
    if (pOptions.loopUnrolling) {
      for (SeqThreadSimulationFunction threadSimulation : pFields.threadSimulationFunctions) {
        rDefinitions.add(threadSimulation.buildDefinition());
      }
    }
    // create clauses in main method
    SeqMainFunction mainFunction = new SeqMainFunction(pOptions, pFields, pUtils);
    rDefinitions.add(mainFunction.buildDefinition());
    return rDefinitions.toString();
  }

  // Thread Simulation Variables ===================================================================

  /**
   * Returns the {@link String} for thread simulation variable declarations. These are local to the
   * {@code main} function. Variables that are used in other functions are declared beforehand as
   * global variables.
   */
  static String buildThreadSimulationVariableDeclarations(
      MPOROptions pOptions, SequentializationFields pFields) throws UnrecognizedCodeException {

    StringJoiner rDeclarations = new StringJoiner(SeqSyntax.NEWLINE);

    // last_thread is always unsigned, we assign NUM_THREADS if the current thread terminates
    if (pOptions.reduceLastThreadOrder) {
      CIntegerLiteralExpression numThreadsExpression =
          SeqExpressionBuilder.buildIntegerLiteralExpression(pFields.numThreads);
      CInitializer lastThreadInitializer =
          SeqInitializerBuilder.buildInitializerExpression(numThreadsExpression);
      CVariableDeclaration lastThreadDeclaration =
          SeqDeclarationBuilder.buildLastThreadDeclaration(lastThreadInitializer);
      rDeclarations.add(lastThreadDeclaration.toASTString());
    }

    // next_thread
    if (pOptions.nondeterminismSource.isNextThreadNondeterministic()) {
      rDeclarations.add(SeqDeclarationBuilder.buildNextThreadDeclaration(pOptions).toASTString());
    }

    // pc variable(s)
    if (pOptions.comments) {
      rDeclarations.add(SeqComment.PC_DECLARATION);
    }
    ImmutableList<CVariableDeclaration> pcDeclarations =
        SeqDeclarationBuilder.buildPcDeclarations(pOptions, pFields);
    for (CVariableDeclaration pcDeclaration : pcDeclarations) {
      rDeclarations.add(pcDeclaration.toASTString());
    }

    // if enabled: bit vectors
    if (pOptions.isAnyReductionEnabled()) {
      ImmutableList<SeqBitVectorDeclaration> bitVectorDeclarations =
          SeqBitVectorDeclarationBuilder.buildBitVectorDeclarationsByEncoding(pOptions, pFields);
      for (SeqBitVectorDeclaration bitVectorDeclaration : bitVectorDeclarations) {
        rDeclarations.add(bitVectorDeclaration.toASTString());
      }
    }

    // active_thread_count / cnt
    if (pOptions.isThreadCountRequired()) {
      rDeclarations.add(SeqVariableDeclarations.CNT.toASTString());
    }

    // if enabled: round_max and round
    if (pOptions.nondeterminismSource.isNumStatementsNondeterministic()) {
      rDeclarations.add(SeqVariableDeclarations.ROUND.toASTString());
      rDeclarations.add(SeqDeclarationBuilder.buildRoundMaxDeclaration(pOptions).toASTString());
    }

    // thread synchronization variables (e.g. mutex_locked)
    if (pOptions.comments) {
      rDeclarations.add(SeqComment.THREAD_SIMULATION_VARIABLES);
    }
    for (CSimpleDeclaration declaration :
        pFields.ghostElements.getThreadSyncFlags().getDeclarations(pOptions)) {
      rDeclarations.add(declaration.toASTString());
    }
    return rDeclarations.toString();
  }
}
