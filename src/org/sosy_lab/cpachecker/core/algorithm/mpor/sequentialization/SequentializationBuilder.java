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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.SeqBitVectorDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.SeqBitVectorDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqFunctionDeclarations;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqVariableDeclarations;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqAssumeFunctionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqMainFunctionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThreadUtil;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportFunctionDefinition;

public class SequentializationBuilder {

  public static String buildBitVectorTypeDeclarations() {
    StringJoiner rDeclarations = new StringJoiner(System.lineSeparator());
    for (SeqBitVectorDataType bitVectorType : SeqBitVectorDataType.values()) {
      CTypeDeclaration bitVectorTypeDeclaration = bitVectorType.buildDeclaration();
      rDeclarations.add(bitVectorTypeDeclaration.toASTString());
    }
    return rDeclarations.toString();
  }

  public static String buildInputFunctionAndTypeDeclarations(
      MPOROptions pOptions, ImmutableList<MPORThread> pThreads) throws UnrecognizedCodeException {

    StringJoiner rDeclarations = new StringJoiner(System.lineSeparator());
    if (pOptions.comments()) {
      rDeclarations.add(SeqComment.UNCHANGED_DECLARATIONS.toASTString());
    }
    // add all original program declarations that are not substituted
    for (MPORThread thread : pThreads) {
      ImmutableList<CDeclaration> nonVariableDeclarations =
          MPORThreadUtil.extractNonVariableDeclarations(thread);
      for (CDeclaration declaration : nonVariableDeclarations) {
        // add function and type declaration only if enabled in options
        if (!(declaration instanceof CFunctionDeclaration)
            || pOptions.inputFunctionDeclarations()) {
          if (!(declaration instanceof CTypeDeclaration) || pOptions.inputTypeDeclarations()) {
            rDeclarations.add(declaration.toASTString());
          }
        }
      }
    }
    return rDeclarations.toString();
  }

  // Input Variable Declarations ===================================================================

  public static String buildInputGlobalVariableDeclarations(
      MPOROptions pOptions, MPORSubstitution pMainThreadSubstitution)
      throws UnrecognizedCodeException {

    StringJoiner rDeclarations = new StringJoiner(System.lineSeparator());
    if (pOptions.comments()) {
      rDeclarations.add(SeqComment.GLOBAL_VAR_DECLARATIONS.toASTString());
    }
    ImmutableList<CVariableDeclaration> globalDeclarations =
        pMainThreadSubstitution.getGlobalVariableDeclarationSubstitutes();
    for (CVariableDeclaration globalDeclaration : globalDeclarations) {
      if (!PthreadUtil.isAnyPthreadObjectType(globalDeclaration.getType())) {
        rDeclarations.add(globalDeclaration.toASTString());
      }
    }
    return rDeclarations.toString();
  }

  public static String buildInputLocalVariableDeclarations(
      MPOROptions pOptions, ImmutableList<MPORSubstitution> pSubstitutions)
      throws UnrecognizedCodeException {

    StringJoiner rDeclarations = new StringJoiner(System.lineSeparator());
    if (pOptions.comments()) {
      rDeclarations.add(SeqComment.LOCAL_VAR_DECLARATIONS.toASTString());
    }
    for (MPORSubstitution substitution : pSubstitutions) {
      ImmutableList<CVariableDeclaration> localDeclarations =
          substitution.getLocalVariableDeclarationSubstitutes();
      for (CVariableDeclaration localDeclaration : localDeclarations) {
        Optional<String> line = tryBuildInputLocalVariableDeclaration(pOptions, localDeclaration);
        if (line.isPresent()) {
          rDeclarations.add(line.orElseThrow());
        }
      }
    }
    return rDeclarations.toString();
  }

  private static Optional<String> tryBuildInputLocalVariableDeclaration(
      MPOROptions pOptions, CVariableDeclaration pLocalVariableDeclaration) {

    checkArgument(!pLocalVariableDeclaration.isGlobal(), "pLocalVariableDeclaration must be local");
    // try remove const qualifier from variable
    if (pLocalVariableDeclaration.getType().getQualifiers().containsConst()) {
      // const CPAchecker_TMP variables are declared and initialized in the statement
      if (MPORUtil.isConstCpaCheckerTmp(pLocalVariableDeclaration)) {
        return Optional.empty();
      } else {
        return tryBuildInputConstLocalVariableDeclaration(pOptions, pLocalVariableDeclaration);
      }
    }
    // otherwise, for non-const variables
    if (!PthreadUtil.isAnyPthreadObjectType(pLocalVariableDeclaration.getType())) {
      CInitializer initializer = pLocalVariableDeclaration.getInitializer();
      if (initializer == null) {
        // no initializer -> add declaration as is
        return Optional.of(pLocalVariableDeclaration.toASTString());
      }
      if (MPORUtil.isFunctionPointer(pLocalVariableDeclaration.getInitializer())) {
        // function pointer initializer -> add declaration as is
        return Optional.of(pLocalVariableDeclaration.toASTString());
      }
      // everything else: add declaration without initializer (and assign later in statements)
      return Optional.of(
          SeqStringUtil.getVariableDeclarationASTStringWithoutInitializer(
              pLocalVariableDeclaration, AAstNodeRepresentation.DEFAULT));
    }
    return Optional.empty();
  }

  private static Optional<String> tryBuildInputConstLocalVariableDeclaration(
      MPOROptions pOptions, CVariableDeclaration pLocalVariableDeclaration) {

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
    return tryBuildInputLocalVariableDeclaration(pOptions, variableDeclarationWithoutConst);
  }

  // Input Parameter Declarations ==================================================================

  public static String buildInputParameterDeclarations(
      MPOROptions pOptions, ImmutableList<MPORSubstitution> pSubstitutions)
      throws UnrecognizedCodeException {

    StringJoiner rDeclarations = new StringJoiner(System.lineSeparator());
    if (pOptions.comments()) {
      rDeclarations.add(SeqComment.PARAMETER_VAR_SUBSTITUTES.toASTString());
    }
    for (MPORSubstitution substitution : pSubstitutions) {
      ImmutableList<CVariableDeclaration> parameterDeclarations =
          substitution.getParameterDeclarationSubstitutes();
      for (CVariableDeclaration parameterDeclaration : parameterDeclarations) {
        // exclude all pthread objects such as pthread_mutex_t, they are not required in the output
        if (!PthreadUtil.isAnyPthreadObjectType(parameterDeclaration.getType())) {
          rDeclarations.add(
              SeqStringUtil.getVariableDeclarationASTStringWithoutInitializer(
                  parameterDeclaration, AAstNodeRepresentation.DEFAULT));
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
      MPOROptions pOptions, MPORSubstitution pMainThreadSubstitution)
      throws UnrecognizedCodeException {

    StringJoiner rDeclarations = new StringJoiner(System.lineSeparator());
    if (pOptions.comments()) {
      rDeclarations.add(SeqComment.MAIN_FUNCTION_ARG_SUBSTITUTES.toASTString());
    }
    for (CIdExpression mainArg : pMainThreadSubstitution.mainFunctionArgSubstitutes.values()) {
      rDeclarations.add(mainArg.getDeclaration().toASTString());
    }
    return rDeclarations.toString();
  }

  public static String buildStartRoutineArgDeclarations(
      MPOROptions pOptions, MPORSubstitution pMainThreadSubstitution)
      throws UnrecognizedCodeException {

    StringJoiner rDeclarations = new StringJoiner(System.lineSeparator());
    if (pOptions.comments()) {
      rDeclarations.add(SeqComment.START_ROUTINE_ARG_SUBSTITUTES.toASTString());
    }
    ImmutableList<CVariableDeclaration> startRoutineArgDeclarations =
        pMainThreadSubstitution.getStartRoutineArgDeclarationSubstitutes();
    for (CVariableDeclaration startRoutineArgDeclaration : startRoutineArgDeclarations) {
      // TODO why exclude pthread objects here? add explaining comment
      if (!PthreadUtil.isAnyPthreadObjectType(startRoutineArgDeclaration.getType())) {
        rDeclarations.add(
            SeqStringUtil.getVariableDeclarationASTStringWithoutInitializer(
                startRoutineArgDeclaration, AAstNodeRepresentation.DEFAULT));
      }
    }
    return rDeclarations.toString();
  }

  public static String buildStartRoutineExitDeclarations(
      MPOROptions pOptions, ImmutableList<MPORThread> pThreads) throws UnrecognizedCodeException {

    StringJoiner rDeclarations = new StringJoiner(System.lineSeparator());
    if (pOptions.comments()) {
      rDeclarations.add(SeqComment.START_ROUTINE_EXIT_VARIABLES.toASTString());
    }
    for (MPORThread thread : pThreads) {
      Optional<CIdExpression> exitVariable = thread.startRoutineExitVariable();
      if (exitVariable.isPresent()) {
        rDeclarations.add(exitVariable.orElseThrow().getDeclaration().toASTString());
      }
    }
    return rDeclarations.toString();
  }

  // Function Declarations and Definitions =========================================================

  public static String buildFunctionDeclarations(
      MPOROptions pOptions, SequentializationFields pFields) throws UnrecognizedCodeException {

    StringJoiner rDeclarations = new StringJoiner(System.lineSeparator());
    if (pOptions.comments()) {
      rDeclarations.add(SeqComment.CUSTOM_FUNCTION_DECLARATIONS.toASTString());
    }

    // reach_error, abort, assert, nondet_int may be duplicate depending on the input program
    if (pOptions.nondeterminismSigned()) {
      rDeclarations.add(VerifierNondetFunctionType.INT.getFunctionDeclaration().toASTString());
    } else {
      rDeclarations.add(VerifierNondetFunctionType.UINT.getFunctionDeclaration().toASTString());
    }
    rDeclarations.add(SeqThreadStatementBuilder.REACH_ERROR_FUNCTION_DECLARATION.toASTString());
    rDeclarations.add(SeqAssumeFunctionBuilder.ASSUME_FUNCTION_DECLARATION.toASTString());
    rDeclarations.add(SeqAssumeFunctionBuilder.ABORT_FUNCTION_DECLARATION.toASTString());

    // malloc is required for valid-memsafety tasks
    rDeclarations.add(SeqFunctionDeclarations.MALLOC.toASTString());

    // thread simulation functions, only enabled when loop is unrolled
    if (pOptions.loopUnrolling()) {
      pFields
          .threadSimulationFunctions
          .orElseThrow()
          .values()
          .forEach(f -> rDeclarations.add(f.getDeclaration().toASTString()));
    }
    // main should always be duplicate
    rDeclarations.add(SeqMainFunctionBuilder.MAIN_FUNCTION_DECLARATION.toASTString());
    return rDeclarations.toString();
  }

  public static String buildFunctionDefinitions(
      MPOROptions pOptions, SequentializationFields pFields, SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    StringJoiner rDefinitions = new StringJoiner(System.lineSeparator());
    if (pOptions.comments()) {
      rDefinitions.add(SeqComment.CUSTOM_FUNCTION_DEFINITIONS.toASTString());
    }
    // custom function definitions: assume(), main()
    rDefinitions.add(
        SeqAssumeFunctionBuilder.buildFunctionDefinition(pUtils.binaryExpressionBuilder())
            .toASTString());
    // create separate thread simulation function definitions, if enabled
    if (pOptions.loopUnrolling()) {
      for (CExportFunctionDefinition functionDefinition :
          pFields.threadSimulationFunctions.orElseThrow().values()) {
        rDefinitions.add(functionDefinition.toASTString());
      }
    }
    // create clauses in main method
    rDefinitions.add(
        SeqMainFunctionBuilder.buildFunctionDefinition(pOptions, pFields, pUtils).toASTString());
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

    StringJoiner rDeclarations = new StringJoiner(System.lineSeparator());

    // if the loop is finite, i.e., loopIterations is not 0, and loopUnrolling is disabled,
    // then add the variable that is incremented with each iteration
    if (pOptions.loopIterations() != 0 && !pOptions.loopUnrolling()) {
      rDeclarations.add(SeqVariableDeclarations.ITERATION.toASTString());
    }

    if (pOptions.reduceLastThreadOrder()) {
      // LAST_THREAD ghost variable
      CIntegerLiteralExpression numThreadsExpression =
          SeqExpressionBuilder.buildIntegerLiteralExpression(pFields.numThreads);
      // the initializer of LAST_THREAD is dependent on the number of threads
      CInitializer lastThreadInitializer =
          new CInitializerExpression(FileLocation.DUMMY, numThreadsExpression);
      CVariableDeclaration lastThreadDeclaration =
          SeqDeclarationBuilder.buildVariableDeclaration(
              true,
              // LAST_THREAD is always unsigned, NUM_THREADS is assigned if a thread terminates
              CNumericTypes.UNSIGNED_INT,
              SeqIdExpressions.LAST_THREAD.getName(),
              lastThreadInitializer);
      rDeclarations.add(lastThreadDeclaration.toASTString());
    }

    // next_thread
    if (pOptions.nondeterminismSource().isNextThreadNondeterministic()) {
      CVariableDeclaration nextThreadDeclaration =
          SeqDeclarationBuilder.buildVariableDeclaration(
              true,
              pOptions.nondeterminismSigned() ? CNumericTypes.INT : CNumericTypes.UNSIGNED_INT,
              SeqIdExpressions.NEXT_THREAD.getName(),
              SeqInitializers.INT_0);
      rDeclarations.add(nextThreadDeclaration.toASTString());
    }

    // pc variable(s)
    if (pOptions.comments()) {
      rDeclarations.add(SeqComment.PC_DECLARATION.toASTString());
    }
    ImmutableList<CVariableDeclaration> pcDeclarations =
        pFields.ghostElements.getPcVariables().pcDeclarations();
    for (CVariableDeclaration pcDeclaration : pcDeclarations) {
      rDeclarations.add(pcDeclaration.toASTString());
    }

    // if enabled: bit vectors
    if (pOptions.isAnyBitVectorReductionEnabled()) {
      SeqBitVectorDeclarationBuilder bitVectorDeclarationBuilder =
          new SeqBitVectorDeclarationBuilder(
              pOptions.bitVectorEncoding(),
              pOptions.reduceIgnoreSleep(),
              pOptions.reductionMode(),
              pFields.ghostElements.bitVectorVariables().orElseThrow(),
              pFields.clauses,
              pFields.memoryModel.orElseThrow());
      ImmutableList<CVariableDeclaration> bitVectorDeclarations =
          bitVectorDeclarationBuilder.buildBitVectorDeclarationsByEncoding();
      for (CVariableDeclaration bitVectorDeclaration : bitVectorDeclarations) {
        rDeclarations.add(bitVectorDeclaration.toASTString());
      }
    }

    // track active thread number via thread_count
    rDeclarations.add(SeqVariableDeclarations.THREAD_COUNT.toASTString());

    // if enabled: round_max and round
    if (pOptions.nondeterminismSource().isNumStatementsNondeterministic()) {
      rDeclarations.add(SeqVariableDeclarations.ROUND.toASTString());
      rDeclarations.add(
          SeqDeclarationBuilder.buildVariableDeclaration(
                  true,
                  pOptions.nondeterminismSigned() ? CNumericTypes.INT : CNumericTypes.UNSIGNED_INT,
                  SeqIdExpressions.ROUND_MAX.getName(),
                  SeqInitializers.INT_0)
              .toASTString());
    }

    // thread synchronization variables (e.g. mutex_locked)
    if (pOptions.comments()) {
      rDeclarations.add(SeqComment.THREAD_SIMULATION_VARIABLES.toASTString());
    }
    for (CSimpleDeclaration declaration :
        pFields.ghostElements.threadSyncFlags().getDeclarations(pOptions)) {
      rDeclarations.add(declaration.toASTString());
    }
    return rDeclarations.toString();
  }
}
