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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectSubstitution;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThreadUtil;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportFunctionDefinition;

public class SequentializationBuilder {

  public static String buildInputFunctionAndTypeDeclarations(
      MPOROptions pOptions, ImmutableList<MPORThread> pThreads) throws UnrecognizedCodeException {

    StringJoiner rDeclarations = new StringJoiner(System.lineSeparator());
    if (pOptions.comments()) {
      rDeclarations.add(SeqComment.INPUT_PROGRAM_DECLARATIONS.toASTString());
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
            if (declaration instanceof CTypeDeclaration typeDeclaration) {
              switch (typeDeclaration) {
                case CComplexTypeDeclaration complexTypeDeclaration -> {
                  CType typeSubstitute =
                      PthreadObjectSubstitution.substitutePthreadObjectTypes(
                          complexTypeDeclaration.getType(), CCompositeType.class);
                  CComplexTypeDeclaration newComplexTypeDeclaration =
                      new CComplexTypeDeclaration(
                          complexTypeDeclaration.getFileLocation(),
                          complexTypeDeclaration.isGlobal(),
                          (CComplexType) typeSubstitute);
                  rDeclarations.add(newComplexTypeDeclaration.toASTString());
                }
                case CTypeDefDeclaration typeDefDeclaration -> {
                  CType typeSubstitute =
                      PthreadObjectSubstitution.substitutePthreadObjectTypes(
                          typeDefDeclaration.getType(), CElaboratedType.class);
                  CTypeDefDeclaration newTypeDefDeclaration =
                      new CTypeDefDeclaration(
                          typeDefDeclaration.getFileLocation(),
                          typeDefDeclaration.isGlobal(),
                          typeSubstitute,
                          typeDefDeclaration.getName(),
                          typeDefDeclaration.getOrigName());
                  rDeclarations.add(newTypeDefDeclaration.toASTString());
                }
                default ->
                    throw new AssertionError("Unhandled CTypeDeclaration: " + typeDeclaration);
              }
            } else {
              rDeclarations.add(declaration.toASTString());
            }
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
      CVariableDeclaration variableDeclarationSubstitute =
          buildVariableDeclarationWithSubstituteType(globalDeclaration);
      rDeclarations.add(variableDeclarationSubstitute.toASTString());
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
        CVariableDeclaration variableDeclarationSubstitute =
            buildVariableDeclarationWithSubstituteType(localDeclaration);
        Optional<CVariableDeclaration> variableDeclaration =
            tryBuildInputLocalVariableDeclaration(variableDeclarationSubstitute);
        if (variableDeclaration.isPresent()) {
          rDeclarations.add(variableDeclaration.orElseThrow().toASTString());
        }
      }
    }
    return rDeclarations.toString();
  }

  private static Optional<CVariableDeclaration> tryBuildInputLocalVariableDeclaration(
      CVariableDeclaration pVariableDeclaration) {

    checkArgument(!pVariableDeclaration.isGlobal(), "pVariableDeclaration must be local");

    // try remove const qualifier from variable
    if (pVariableDeclaration.getType().getQualifiers().containsConst()) {
      // const CPAchecker_TMP variables are declared and initialized in the statement
      if (MPORUtil.isConstCpaCheckerTmp(pVariableDeclaration)) {
        return Optional.empty();
      } else {
        // create an identical copy of pVariableDeclaration, but remove const qualifier
        CType type = pVariableDeclaration.getType();
        CType typeWithoutConst = type.withQualifiersSetTo(type.getQualifiers().withoutConst());
        CVariableDeclaration variableDeclarationWithoutConst =
            new CVariableDeclaration(
                pVariableDeclaration.getFileLocation(),
                pVariableDeclaration.isGlobal(),
                pVariableDeclaration.getCStorageClass(),
                typeWithoutConst,
                pVariableDeclaration.getName(),
                pVariableDeclaration.getOrigName(),
                pVariableDeclaration.getQualifiedName(),
                pVariableDeclaration.getInitializer());
        return tryBuildInputLocalVariableDeclaration(variableDeclarationWithoutConst);
      }
    }

    // otherwise, for non-const variables
    CInitializer initializer = pVariableDeclaration.getInitializer();
    if (initializer == null) {
      // no initializer -> add declaration as is
      return Optional.of(pVariableDeclaration);
    }
    if (MPORUtil.isFunctionPointer(pVariableDeclaration.getInitializer())) {
      // function pointer initializer -> add declaration as is
      return Optional.of(pVariableDeclaration);
    }
    // everything else: add declaration without initializer (and assign later in statements)
    return Optional.of(MPORUtil.withInitializer(pVariableDeclaration, null));
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
        CVariableDeclaration variableDeclarationSubstitute =
            buildVariableDeclarationWithSubstituteType(parameterDeclaration);
        rDeclarations.add(
            MPORUtil.withInitializer(variableDeclarationSubstitute, null).toASTString());
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
      CVariableDeclaration variableDeclarationSubstitute =
          buildVariableDeclarationWithSubstituteType(
              (CVariableDeclaration) mainArg.getDeclaration());
      rDeclarations.add(variableDeclarationSubstitute.toASTString());
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
      CVariableDeclaration variableDeclarationSubstitute =
          buildVariableDeclarationWithSubstituteType(startRoutineArgDeclaration);
      rDeclarations.add(
          MPORUtil.withInitializer(variableDeclarationSubstitute, null).toASTString());
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
        CVariableDeclaration variableDeclarationSubstitute =
            buildVariableDeclarationWithSubstituteType(
                (CVariableDeclaration) exitVariable.orElseThrow().getDeclaration());
        rDeclarations.add(variableDeclarationSubstitute.toASTString());
      }
    }
    return rDeclarations.toString();
  }

  // CVariableDeclaration helper

  private static CVariableDeclaration buildVariableDeclarationWithSubstituteType(
      CVariableDeclaration pVariableDeclaration) {

    CType elaboratedtypeSubstitute =
        PthreadObjectSubstitution.substitutePthreadObjectTypes(
            pVariableDeclaration.getType(), CElaboratedType.class);
    CType typedefTypeSubstitute =
        PthreadObjectSubstitution.substitutePthreadObjectTypes(
            elaboratedtypeSubstitute, CTypedefType.class);
    return new CVariableDeclaration(
        pVariableDeclaration.getFileLocation(),
        pVariableDeclaration.isGlobal(),
        pVariableDeclaration.getCStorageClass(),
        typedefTypeSubstitute,
        pVariableDeclaration.getName(),
        pVariableDeclaration.getOrigName(),
        pVariableDeclaration.getQualifiedName(),
        pVariableDeclaration.getInitializer());
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
    if (pOptions.threadSimulationUnrolling()) {
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
    if (pOptions.threadSimulationUnrolling()) {
      for (CExportFunctionDefinition functionDefinition :
          pFields.threadSimulationFunctions.orElseThrow().values()) {
        rDefinitions.add(functionDefinition.toASTString());
      }
    }
    for (CExportFunctionDefinition definition :
        PthreadFunctionSubstitution.getAllFunctionDefinitions(pUtils.binaryExpressionBuilder())) {
      rDefinitions.add(definition.toASTString());
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

    // if the loop is finite, i.e., threadSimulationIterations is not 0, and
    // threadSimulationUnrolling is disabled,
    // then add the variable that is incremented with each iteration
    if (pOptions.threadSimulationIterations() != 0 && !pOptions.threadSimulationUnrolling()) {
      rDeclarations.add(SeqVariableDeclarations.ITERATION.toASTString());
    }

    if (pOptions.isPrevThreadVariableRequired()) {
      // prev_thread ghost variable
      CIntegerLiteralExpression numThreadsExpression =
          SeqExpressionBuilder.buildIntegerLiteralExpression(pFields.numThreads);
      // the initializer of prev_thread is dependent on the number of threads
      CInitializer prevThreadInitializer =
          new CInitializerExpression(FileLocation.DUMMY, numThreadsExpression);
      CVariableDeclaration prevThreadDeclaration =
          SeqDeclarationBuilder.buildVariableDeclaration(
              true,
              // prev_thread is always unsigned, NUM_THREADS is assigned if a thread terminates
              CNumericTypes.UNSIGNED_INT,
              SeqIdExpressions.PREV_THREAD.getName(),
              prevThreadInitializer);
      rDeclarations.add(prevThreadDeclaration.toASTString());
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
              pOptions,
              pFields.ghostElements.bitVectorVariables().orElseThrow(),
              pFields.clauses,
              pFields.machineModel,
              pFields.pointerAliasingMap);
      ImmutableList<CVariableDeclaration> bitVectorDeclarations =
          bitVectorDeclarationBuilder.buildBitVectorDeclarationsByEncoding();
      for (CVariableDeclaration bitVectorDeclaration : bitVectorDeclarations) {
        rDeclarations.add(bitVectorDeclaration.toASTString());
      }
    }

    // track active thread number via thread_count
    if (pOptions.executeSingleActiveThreadFirst()) {
      rDeclarations.add(SeqVariableDeclarations.THREAD_COUNT.toASTString());
    }

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
