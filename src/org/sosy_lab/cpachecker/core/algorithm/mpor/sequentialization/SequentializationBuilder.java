// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.nondeterminism.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqMainFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqReachErrorFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadUtil;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SequentializationBuilder {

  public static ImmutableList<String> buildBitVectorTypeDeclarations() {
    ImmutableList.Builder<String> rBitVectorTypeDeclarations = ImmutableList.builder();
    for (BitVectorDataType bitVectorType : BitVectorDataType.values()) {
      CTypeDeclaration bitVectorTypeDeclaration = bitVectorType.buildDeclaration();
      rBitVectorTypeDeclarations.add(bitVectorTypeDeclaration.toASTString());
    }
    return rBitVectorTypeDeclarations.build();
  }

  public static ImmutableList<String> buildOriginalDeclarations(
      MPOROptions pOptions, ImmutableList<MPORThread> pThreads) {

    ImmutableList.Builder<String> rOriginalDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rOriginalDeclarations.add(SeqComment.UNCHANGED_DECLARATIONS);
    }
    // add all original program declarations that are not substituted
    for (MPORThread thread : pThreads) {
      ImmutableList<CDeclaration> nonVariableDeclarations =
          ThreadUtil.extractNonVariableDeclarations(thread);
      for (CDeclaration declaration : nonVariableDeclarations) {
        // add function and type declaration only if enabled in options
        if (!(declaration instanceof CFunctionDeclaration) || pOptions.inputFunctionDeclarations) {
          if (!(declaration instanceof CTypeDeclaration) || pOptions.inputTypeDeclarations) {
            rOriginalDeclarations.add(declaration.toASTString());
          }
        }
      }
    }
    return rOriginalDeclarations.build();
  }

  // Empty Function Declarations ===================================================================

  public static ImmutableList<String> buildEmptyInputFunctionDeclarations(
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    Set<CFunctionDeclaration> visited = new HashSet<>();
    ImmutableList.Builder<String> rEmptyFunctionDeclarations = ImmutableList.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      for (CFunctionDeclaration functionDeclaration : substituteEdge.accessedFunctionPointers) {
        if (visited.add(functionDeclaration)) {
          rEmptyFunctionDeclarations.add(
              buildEmptyFunctionDefinitionFromDeclaration(functionDeclaration));
        }
      }
    }
    return rEmptyFunctionDeclarations.build();
  }

  private static String buildEmptyFunctionDefinitionFromDeclaration(
      CFunctionDeclaration pDeclaration) {

    StringBuilder rDeclaration = new StringBuilder();
    rDeclaration.append(pDeclaration.getType().getReturnType().toASTString(""));
    rDeclaration.append(SeqSyntax.SPACE);
    rDeclaration.append(pDeclaration.getOrigName());
    // add parameters either with original or generic name, if rDeclaration without names
    rDeclaration.append(SeqSyntax.BRACKET_LEFT);
    for (int i = 0; i < pDeclaration.getParameters().size(); i++) {
      CParameterDeclaration parameter = pDeclaration.getParameters().get(i);
      rDeclaration
          .append(parameter.getType().getCanonicalType().toASTString(""))
          .append(SeqSyntax.SPACE);
      if (parameter.getName().isEmpty()) {
        rDeclaration.append(
            SeqNameUtil.buildParameterNameForEmptyFunctionDefinition(pDeclaration, i));
      } else {
        rDeclaration.append(parameter.getOrigName());
      }
      if (i != pDeclaration.getParameters().size() - 1) {
        rDeclaration.append(SeqSyntax.COMMA).append(SeqSyntax.SPACE);
      }
    }
    rDeclaration.append(SeqSyntax.BRACKET_RIGHT);
    // no body, only {}. the parser still accepts it, even with e.g. int return type
    rDeclaration.append(SeqSyntax.SPACE);
    rDeclaration.append(SeqSyntax.CURLY_BRACKET_LEFT);
    rDeclaration.append(SeqSyntax.CURLY_BRACKET_RIGHT);
    return rDeclaration.toString();
  }

  // Input Variable Declarations ===================================================================

  public static ImmutableList<String> buildInputGlobalVariableDeclarations(
      MPOROptions pOptions, MPORSubstitution pMainThreadSubstitution) {

    ImmutableList.Builder<String> rGlobalDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rGlobalDeclarations.add(SeqComment.GLOBAL_VAR_DECLARATIONS);
    }
    ImmutableList<CVariableDeclaration> globalDeclarations =
        pMainThreadSubstitution.getGlobalDeclarations();
    for (CVariableDeclaration globalDeclaration : globalDeclarations) {
      if (!PthreadUtil.isPthreadObjectType(globalDeclaration.getType())) {
        rGlobalDeclarations.add(globalDeclaration.toASTString());
      }
    }
    return rGlobalDeclarations.build();
  }

  public static ImmutableList<String> buildInputLocalVariableDeclarations(
      MPOROptions pOptions, ImmutableList<MPORSubstitution> pSubstitutions) {

    ImmutableList.Builder<String> rLocalDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rLocalDeclarations.add(SeqComment.LOCAL_VAR_DECLARATIONS);
    }
    for (MPORSubstitution substitution : pSubstitutions) {
      ImmutableList<CVariableDeclaration> localDeclarations = substitution.getLocalDeclarations();
      for (CVariableDeclaration localDeclaration : localDeclarations) {
        Optional<String> line = tryBuildInputLocalVariableDeclaration(localDeclaration);
        if (line.isPresent()) {
          rLocalDeclarations.add(line.orElseThrow());
        }
      }
    }
    return rLocalDeclarations.build();
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
        return Optional.of(pLocalVariableDeclaration.toASTStringWithoutInitializer());
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

  public static ImmutableList<String> buildInputParameterDeclarations(
      MPOROptions pOptions, ImmutableList<MPORSubstitution> pSubstitutions) {

    ImmutableList.Builder<String> rParameterDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rParameterDeclarations.add(SeqComment.PARAMETER_VAR_SUBSTITUTES);
    }
    for (MPORSubstitution substitution : pSubstitutions) {
      ImmutableList<CParameterDeclaration> parameterDeclarations =
          substitution.getSubstituteParameterDeclarations();
      for (CParameterDeclaration parameterDeclaration : parameterDeclarations) {
        if (!PthreadUtil.isPthreadObjectType(parameterDeclaration.getType())) {
          // CParameterDeclarations require addition semicolon
          rParameterDeclarations.add(parameterDeclaration.toASTString() + SeqSyntax.SEMICOLON);
        }
      }
    }
    return rParameterDeclarations.build();
  }

  /**
   * Adds the declarations of main function arguments, e.g. {@code int arg;} that are
   * non-deterministically initialized in {@code main()} later in the sequentialization.
   */
  public static ImmutableList<String> buildMainFunctionArgDeclarations(
      MPOROptions pOptions, MPORSubstitution pMainThreadSubstitution) {

    ImmutableList.Builder<String> rArgDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rArgDeclarations.add(SeqComment.MAIN_FUNCTION_ARG_SUBSTITUTES);
    }
    for (CIdExpression mainArg : pMainThreadSubstitution.mainFunctionArgSubstitutes.values()) {
      rArgDeclarations.add(mainArg.getDeclaration().toASTString());
    }
    return rArgDeclarations.build();
  }

  public static ImmutableList<String> buildStartRoutineArgDeclarations(
      MPOROptions pOptions, MPORSubstitution pMainThreadSubstitution) {

    ImmutableList.Builder<String> rStartRoutineArgDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rStartRoutineArgDeclarations.add(SeqComment.START_ROUTINE_ARG_SUBSTITUTES);
    }
    ImmutableList<CParameterDeclaration> startRoutineArgDeclarations =
        pMainThreadSubstitution.getSubstituteStartRoutineArgDeclarations();
    for (CParameterDeclaration startRoutineArgDeclaration : startRoutineArgDeclarations) {
      // TODO why exclude pthread objects here? add explaining comment
      if (!PthreadUtil.isPthreadObjectType(startRoutineArgDeclaration.getType())) {
        // add trailing ; as CParameterDeclaration is without semicolons
        rStartRoutineArgDeclarations.add(
            startRoutineArgDeclaration.toASTString() + SeqSyntax.SEMICOLON);
      }
    }
    return rStartRoutineArgDeclarations.build();
  }

  public static ImmutableList<String> buildStartRoutineExitDeclarations(
      MPOROptions pOptions, ImmutableList<MPORThread> pThreads) {

    ImmutableList.Builder<String> rStartRoutineExitDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rStartRoutineExitDeclarations.add(SeqComment.START_ROUTINE_EXIT_VARIABLES);
    }
    for (MPORThread thread : pThreads) {
      Optional<CIdExpression> exitVariable = thread.startRoutineExitVariable;
      if (exitVariable.isPresent()) {
        rStartRoutineExitDeclarations.add(
            exitVariable.orElseThrow().getDeclaration().toASTString());
      }
    }
    return rStartRoutineExitDeclarations.build();
  }

  // Function Declarations and Definitions =========================================================

  public static ImmutableList<String> buildFunctionDeclarations(MPOROptions pOptions) {
    ImmutableList.Builder<String> rFunctionDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rFunctionDeclarations.add(SeqComment.CUSTOM_FUNCTION_DECLARATIONS);
    }
    // reach_error, abort, assert, nondet_int may be duplicate depending on the input program
    rFunctionDeclarations.add(SeqFunctionDeclaration.ASSERT_FAIL.toASTString());
    if (pOptions.nondeterminismSigned) {
      rFunctionDeclarations.add(
          VerifierNondetFunctionType.INT.getFunctionDeclaration().toASTString());
    } else {
      rFunctionDeclarations.add(
          VerifierNondetFunctionType.UINT.getFunctionDeclaration().toASTString());
    }
    rFunctionDeclarations.add(SeqFunctionDeclaration.ABORT.toASTString());
    rFunctionDeclarations.add(SeqFunctionDeclaration.REACH_ERROR.toASTString());
    rFunctionDeclarations.add(SeqFunctionDeclaration.ASSUME.toASTString());
    // main should always be duplicate
    rFunctionDeclarations.add(SeqFunctionDeclaration.MAIN.toASTString());
    return rFunctionDeclarations.build();
  }

  public static ImmutableList<String> buildFunctionDefinitions(
      MPOROptions pOptions,
      SequentializationFields pFields,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<String> rFunctionDefinitions = ImmutableList.builder();
    if (pOptions.comments) {
      rFunctionDefinitions.add(SeqComment.CUSTOM_FUNCTION_DEFINITIONS);
    }
    // custom function definitions: reach_error(), assume(), main()
    SeqReachErrorFunction reachError = new SeqReachErrorFunction();
    rFunctionDefinitions.addAll(reachError.buildDefinition());
    SeqAssumeFunction assume = new SeqAssumeFunction(pBinaryExpressionBuilder);
    rFunctionDefinitions.addAll(assume.buildDefinition());
    // create clauses in main method
    SeqMainFunction mainFunction =
        new SeqMainFunction(pOptions, pFields, pBinaryExpressionBuilder, pLogger);
    rFunctionDefinitions.addAll(mainFunction.buildDefinition());
    return rFunctionDefinitions.build();
  }
}
