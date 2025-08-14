// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.nondeterminism.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.SeqASTNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.declaration.SeqBitVectorDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqMainFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqReachErrorFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.ThreadSimulationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadUtil;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class LineOfCodeUtil {

  public static ImmutableList<LineOfCode> buildBitVectorTypeDeclarations() {
    ImmutableList.Builder<LineOfCode> rBitVectorTypeDeclarations = ImmutableList.builder();
    for (BitVectorDataType bitVectorType : BitVectorDataType.values()) {
      CTypeDeclaration bitVectorTypeDeclaration = bitVectorType.buildDeclaration();
      rBitVectorTypeDeclarations.add(LineOfCode.of(bitVectorTypeDeclaration.toASTString()));
    }
    return rBitVectorTypeDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildOriginalDeclarations(
      MPOROptions pOptions, ImmutableList<MPORThread> pThreads) {

    ImmutableList.Builder<LineOfCode> rOriginalDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rOriginalDeclarations.add(LineOfCode.of(SeqComment.UNCHANGED_DECLARATIONS));
    }
    // add all original program declarations that are not substituted
    for (MPORThread thread : pThreads) {
      ImmutableList<CDeclaration> nonVariableDeclarations =
          ThreadUtil.extractNonVariableDeclarations(thread);
      for (CDeclaration declaration : nonVariableDeclarations) {
        // add function and type declaration only if enabled in options
        if (!(declaration instanceof CFunctionDeclaration) || pOptions.inputFunctionDeclarations) {
          if (!(declaration instanceof CTypeDeclaration) || pOptions.inputTypeDeclarations) {
            rOriginalDeclarations.add(LineOfCode.of(declaration.toASTString()));
          }
        }
      }
    }
    if (pOptions.comments) {
      rOriginalDeclarations.add(LineOfCode.empty());
    }
    return rOriginalDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildEmptyInputFunctionDeclarations(
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    Set<CFunctionDeclaration> visited = new HashSet<>();
    ImmutableList.Builder<LineOfCode> rEmptyFunctionDeclarations = ImmutableList.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      for (CFunctionDeclaration functionDeclaration : substituteEdge.accessedFunctionPointers) {
        if (visited.add(functionDeclaration)) {
          String emptyDefinition =
              SeqStringUtil.buildEmptyFunctionDefinitionFromDeclaration(functionDeclaration);
          rEmptyFunctionDeclarations.add(LineOfCode.of(emptyDefinition));
        }
      }
    }
    return rEmptyFunctionDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildInputGlobalVariableDeclarations(
      MPOROptions pOptions, MPORSubstitution pMainThreadSubstitution) {

    ImmutableList.Builder<LineOfCode> rGlobalDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rGlobalDeclarations.add(LineOfCode.of(SeqComment.GLOBAL_VAR_DECLARATIONS));
    }
    ImmutableList<CVariableDeclaration> globalDeclarations =
        pMainThreadSubstitution.getGlobalDeclarations();
    for (CVariableDeclaration globalDeclaration : globalDeclarations) {
      if (!PthreadUtil.isPthreadObjectType(globalDeclaration.getType())) {
        rGlobalDeclarations.add(LineOfCodeUtil.buildLineOfCode(globalDeclaration));
      }
    }
    if (pOptions.comments) {
      rGlobalDeclarations.add(LineOfCode.empty());
    }
    return rGlobalDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildInputLocalVariableDeclarations(
      MPOROptions pOptions, ImmutableList<MPORSubstitution> pSubstitutions) {

    ImmutableList.Builder<LineOfCode> rLocalDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rLocalDeclarations.add(LineOfCode.of(SeqComment.LOCAL_VAR_DECLARATIONS));
    }
    for (MPORSubstitution substitution : pSubstitutions) {
      ImmutableList<CVariableDeclaration> localDeclarations = substitution.getLocalDeclarations();
      for (CVariableDeclaration localDeclaration : localDeclarations) {
        if (!PthreadUtil.isPthreadObjectType(localDeclaration.getType())) {
          CInitializer initializer = localDeclaration.getInitializer();
          if (initializer == null) {
            // no initializer -> add declaration as is
            rLocalDeclarations.add(LineOfCode.of(localDeclaration.toASTString()));

          } else if (MPORUtil.isFunctionPointer(localDeclaration.getInitializer())) {
            // function pointer initializer -> add declaration as is
            rLocalDeclarations.add(LineOfCode.of(localDeclaration.toASTString()));

          } else if (!MPORUtil.isConstCpaCheckerTmp(localDeclaration)) {
            // const CPAchecker_TMP variables are declared and initialized directly in the case.
            // everything else: add declaration without initializer (and assign later in cases)
            rLocalDeclarations.add(LineOfCode.of(localDeclaration.toASTStringWithoutInitializer()));
          }
        }
      }
    }
    if (pOptions.comments) {
      rLocalDeclarations.add(LineOfCode.empty());
    }
    return rLocalDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildInputParameterDeclarations(
      MPOROptions pOptions, ImmutableList<MPORSubstitution> pSubstitutions) {

    ImmutableList.Builder<LineOfCode> rParameterDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rParameterDeclarations.add(LineOfCode.of(SeqComment.PARAMETER_VAR_SUBSTITUTES));
    }
    for (MPORSubstitution substitution : pSubstitutions) {
      ImmutableList<CParameterDeclaration> parameterDeclarations =
          substitution.getParameterDeclarations();
      for (CParameterDeclaration parameterDeclaration : parameterDeclarations) {
        if (!PthreadUtil.isPthreadObjectType(parameterDeclaration.getType())) {
          // CParameterDeclarations require addition semicolon
          rParameterDeclarations.add(LineOfCode.of(parameterDeclaration.toASTString() + SeqSyntax.SEMICOLON));
        }
      }
    }
    if (pOptions.comments) {
      rParameterDeclarations.add(LineOfCode.empty());
    }
    return rParameterDeclarations.build();
  }

  /**
   * Adds the declarations of main function arguments, e.g. {@code int arg;} that are
   * non-deterministically initialized in {@code main()} later in the sequentialization.
   */
  public static ImmutableList<LineOfCode> buildMainFunctionArgDeclarations(
      MPOROptions pOptions, MPORSubstitution pMainThreadSubstitution) {

    ImmutableList.Builder<LineOfCode> rArgDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rArgDeclarations.add(LineOfCode.of(SeqComment.MAIN_FUNCTION_ARG_SUBSTITUTES));
    }
    for (CIdExpression mainArg : pMainThreadSubstitution.mainFunctionArgSubstitutes.values()) {
      rArgDeclarations.add(LineOfCodeUtil.buildLineOfCode(mainArg.getDeclaration()));
    }
    if (pOptions.comments) {
      rArgDeclarations.add(LineOfCode.empty());
    }
    return rArgDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildStartRoutineArgDeclarations(
      MPOROptions pOptions, MPORSubstitution pMainThreadSubstitution) {

    ImmutableList.Builder<LineOfCode> rStartRoutineArgDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rStartRoutineArgDeclarations.add(LineOfCode.of(SeqComment.START_ROUTINE_ARG_SUBSTITUTES));
    }
    ImmutableList<CVariableDeclaration> startRoutineArgDeclarations =
        pMainThreadSubstitution.getStartRoutineArgDeclarations();
    for (CVariableDeclaration startRoutineArgDeclaration : startRoutineArgDeclarations) {
      // TODO why exclude pthread objects here? add explaining comment
      if (!PthreadUtil.isPthreadObjectType(startRoutineArgDeclaration.getType())) {
        rStartRoutineArgDeclarations.add(
            LineOfCodeUtil.buildLineOfCode(startRoutineArgDeclaration));
      }
    }
    if (pOptions.comments) {
      rStartRoutineArgDeclarations.add(LineOfCode.empty());
    }
    return rStartRoutineArgDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildStartRoutineExitDeclarations(
      MPOROptions pOptions, ImmutableList<MPORThread> pThreads) {

    ImmutableList.Builder<LineOfCode> rStartRoutineExitDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rStartRoutineExitDeclarations.add(LineOfCode.of(SeqComment.START_ROUTINE_EXIT_VARIABLES));
    }
    for (MPORThread thread : pThreads) {
      Optional<CIdExpression> exitVariable = thread.startRoutineExitVariable;
      if (exitVariable.isPresent()) {
        rStartRoutineExitDeclarations.add(
            LineOfCode.of(exitVariable.orElseThrow().getDeclaration().toASTString()));
      }
    }
    if (pOptions.comments) {
      rStartRoutineExitDeclarations.add(LineOfCode.empty());
    }
    return rStartRoutineExitDeclarations.build();
  }

  // TODO since conflict evaluations are not in a separate function, put all this into main()
  /**
   * Creates all thread simulation variables that are global, so that they can be used in functions
   * separate from the {@code main} function. Accessing them directly should be more efficient than
   * passing them as parameters.
   */
  public static ImmutableList<LineOfCode> buildGlobalThreadSimulationVariableDeclarations(
      MPOROptions pOptions,
      ImmutableList<CVariableDeclaration> pPcDeclarations,
      ImmutableList<SeqBitVectorDeclaration> pBitVectorDeclarations)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rDeclarations = ImmutableList.builder();

    // last_thread
    if (pOptions.conflictReduction) {
      if (pOptions.signedNondet) {
        rDeclarations.add(LineOfCode.of(SeqVariableDeclaration.LAST_THREAD_SIGNED.toASTString()));
      } else {
        rDeclarations.add(LineOfCode.of(SeqVariableDeclaration.LAST_THREAD_UNSIGNED.toASTString()));
      }
    }

    // next_thread
    if (pOptions.nondeterminismSource.isNextThreadNondeterministic()) {
      if (pOptions.signedNondet) {
        rDeclarations.add(LineOfCode.of(SeqVariableDeclaration.NEXT_THREAD_SIGNED.toASTString()));
      } else {
        rDeclarations.add(LineOfCode.of(SeqVariableDeclaration.NEXT_THREAD_UNSIGNED.toASTString()));
      }
    }

    // pc
    if (pOptions.comments) {
      rDeclarations.add(LineOfCode.empty());
      rDeclarations.add(LineOfCode.of(SeqComment.PC_DECLARATION));
    }
    for (CVariableDeclaration pcDeclaration : pPcDeclarations) {
      rDeclarations.add(LineOfCode.of(pcDeclaration.toASTString()));
    }

    // if enabled: bit vectors (for partial order reductions)
    if (pOptions.areBitVectorsEnabled()) {
      for (SeqBitVectorDeclaration bitVectorDeclaration : pBitVectorDeclarations) {
        rDeclarations.add(LineOfCode.of(bitVectorDeclaration.toASTString()));
      }
    }

    return rDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildFunctionDeclarations(MPOROptions pOptions) {
    ImmutableList.Builder<LineOfCode> rFunctionDeclarations = ImmutableList.builder();
    if (pOptions.comments) {
      rFunctionDeclarations.add(LineOfCode.of(SeqComment.CUSTOM_FUNCTION_DECLARATIONS));
    }
    // reach_error, abort, assert, nondet_int may be duplicate depending on the input program
    rFunctionDeclarations.add(LineOfCode.of(SeqFunctionDeclaration.ASSERT_FAIL.toASTString()));
    if (pOptions.signedNondet) {
      rFunctionDeclarations.add(
          LineOfCode.of(VerifierNondetFunctionType.INT.getFunctionDeclaration().toASTString()));
    } else {
      rFunctionDeclarations.add(
          LineOfCode.of(VerifierNondetFunctionType.UINT.getFunctionDeclaration().toASTString()));
    }
    rFunctionDeclarations.add(LineOfCode.of(SeqFunctionDeclaration.ABORT.toASTString()));
    rFunctionDeclarations.add(LineOfCode.of(SeqFunctionDeclaration.REACH_ERROR.toASTString()));
    rFunctionDeclarations.add(LineOfCode.of(SeqFunctionDeclaration.ASSUME.toASTString()));
    // main should always be duplicate
    rFunctionDeclarations.add(LineOfCode.of(SeqFunctionDeclaration.MAIN.toASTString()));
    if (pOptions.comments) {
      rFunctionDeclarations.add(LineOfCode.empty());
    }
    return rFunctionDeclarations.build();
  }

  public static ImmutableList<LineOfCode> buildFunctionDefinitions(
      MPOROptions pOptions,
      ImmutableList<MPORSubstitution> pSubstitutions,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      Optional<BitVectorVariables> pBitVectorVariables,
      PcVariables pPcVariables,
      ThreadSimulationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rFunctionDefinitions = ImmutableList.builder();
    if (pOptions.comments) {
      rFunctionDefinitions.add(LineOfCode.of(SeqComment.CUSTOM_FUNCTION_DEFINITIONS));
    }
    // custom function definitions: reach_error(), assume(), main()
    SeqReachErrorFunction reachError = new SeqReachErrorFunction();
    rFunctionDefinitions.addAll(reachError.buildDefinition());
    SeqAssumeFunction assume = new SeqAssumeFunction(pBinaryExpressionBuilder);
    rFunctionDefinitions.addAll(assume.buildDefinition());
    // create clauses in main method
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> clauses =
        SeqThreadStatementClauseBuilder.buildClauses(
            pOptions,
            pSubstitutions,
            pSubstituteEdges,
            pBitVectorVariables,
            pPcVariables,
            pThreadSimulationVariables,
            pBinaryExpressionBuilder,
            pLogger);
    // TODO don't call this method twice but pass parameter
    ImmutableSetMultimap<CVariableDeclaration, CVariableDeclaration> pointerAssignments =
        SubstituteUtil.mapPointerAssignments(pSubstituteEdges.values());
    SeqMainFunction mainFunction =
        new SeqMainFunction(
            pOptions,
            pSubstitutions,
            clauses,
            pointerAssignments,
            pBitVectorVariables,
            pPcVariables,
            pThreadSimulationVariables,
            pBinaryExpressionBuilder,
            pLogger);
    rFunctionDefinitions.addAll(mainFunction.buildDefinition());
    return rFunctionDefinitions.build();
  }

  // Helpers =======================================================================================

  /** Create and return the {@link String} for {@code pLinesOfCode}. */
  public static String buildString(ImmutableList<LineOfCode> pLinesOfCode) {
    StringBuilder rString = new StringBuilder();
    for (LineOfCode lineOfCode : pLinesOfCode) {
      rString.append(lineOfCode.toString());
    }
    return rString.toString();
  }

  /** Create and return the {@link ImmutableList} for {@code pString} that is split on newlines. */
  public static ImmutableList<LineOfCode> buildLinesOfCodeFromCAstNodes(String pString) {
    ImmutableList.Builder<LineOfCode> rLinesOfCode = ImmutableList.builder();
    for (String line : SeqStringUtil.splitOnNewline(pString)) {
      rLinesOfCode.add(LineOfCode.of(line.trim()));
    }
    return rLinesOfCode.build();
  }

  /** Return the list of {@link LineOfCode} for {@code pAstNodes}. */
  public static ImmutableList<LineOfCode> buildLinesOfCodeFromCAstNodes(
      ImmutableList<? extends CAstNode> pAstNodes) {

    ImmutableList.Builder<LineOfCode> rLinesOfCode = ImmutableList.builder();
    for (CAstNode astNode : pAstNodes) {
      rLinesOfCode.add(buildLineOfCode(astNode));
    }
    return rLinesOfCode.build();
  }

  /** Return the list of {@link LineOfCode} for {@code pSeqAstNodes}. */
  public static ImmutableList<LineOfCode> buildLinesOfCodeFromSeqAstNodes(
      ImmutableList<? extends SeqASTNode> pSeqAstNodes) throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rLinesOfCode = ImmutableList.builder();
    for (SeqASTNode astNode : pSeqAstNodes) {
      rLinesOfCode.add(LineOfCode.of(astNode.toASTString()));
    }
    return rLinesOfCode.build();
  }

  /** Return the single {@link LineOfCode} for pAstNode. */
  public static <T extends CAstNode> LineOfCode buildLineOfCode(T pAstNode) {
    return LineOfCode.of(pAstNode.toASTString());
  }
}
