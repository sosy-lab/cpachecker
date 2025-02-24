// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Year;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqBinaryExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqStringLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqLeftHandSides;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClauseBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseLabel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.GhostVariableUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.GhostVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function.GhostFunctionVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.GhostPcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread.GhostThreadVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread.ThreadJoinsThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread.ThreadLocksMutex;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.function.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.function.SeqMainFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.function.SeqReachErrorFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.output.SequentializationWriter.FileExtension;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.pruning.SeqPruner;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.hard_coded.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.CSimpleDeclarationSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class Sequentialization {

  private static final ImmutableList<LineOfCode> licenseHeader =
      ImmutableList.of(
          LineOfCode.of(0, "// This file is part of CPAchecker,"),
          LineOfCode.of(0, "// a tool for configurable software verification:"),
          LineOfCode.of(0, "// https://cpachecker.sosy-lab.org"),
          LineOfCode.of(0, "//"),
          LineOfCode.of(
              0,
              "// SPDX-"
                  + "FileCopyrightText: "
                  + Year.now(ZoneId.systemDefault()).getValue()
                  + " Dirk Beyer <https://www.sosy-lab.org>"),
          LineOfCode.of(0, "//"),
          LineOfCode.of(0, "// SPDX-License-Identifier: " + "Apache-2.0"),
          LineOfCode.empty());

  private static final ImmutableList<LineOfCode> mporHeader =
      ImmutableList.of(
          LineOfCode.of(
              0,
              "// This sequentialization (transformation of a concurrent program into an"
                  + " equivalent"),
          LineOfCode.of(
              0,
              "// sequential program) was created by the MPORAlgorithm implemented in CPAchecker."),
          LineOfCode.of(0, "//"),
          LineOfCode.of(
              0,
              "// Assertion fails from the function "
                  + SeqToken.__SEQUENTIALIZATION_ERROR__
                  + " mark faulty sequentializations."),
          LineOfCode.of(0, "// All other assertion fails are induced by faulty input programs"),
          LineOfCode.empty());

  public static final String inputReachErrorDummy =
      buildReachErrorCall(SeqToken.__FILE_NAME_PLACEHOLDER__, -1, SeqToken.__PRETTY_FUNCTION__)
              .toASTString()
          + SeqSyntax.SEMICOLON;

  public static final String outputReachErrorDummy =
      buildReachErrorCall(
                  SeqToken.__FILE_NAME_PLACEHOLDER__, -1, SeqToken.__SEQUENTIALIZATION_ERROR__)
              .toASTString()
          + SeqSyntax.SEMICOLON;

  public static final int INIT_PC = 0;

  public static final int EXIT_PC = -1;

  private final ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> substitutions;

  private final MPOROptions options;

  private final String inputFileName;

  private final String outputFileName;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  private final LogManager logger;

  private final GhostPcVariables pcVariables;

  public Sequentialization(
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions,
      MPOROptions pOptions,
      String pInputFileName,
      String pOutputFileName,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger) {

    substitutions = pSubstitutions;
    inputFileName = pInputFileName;
    outputFileName = pOutputFileName;
    options = pOptions;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
    logger = pLogger;
    pcVariables =
        new GhostPcVariables(
            SeqLeftHandSides.buildPcLeftHandSides(pSubstitutions.size(), options.scalarPc));
  }

  @Override
  public String toString() {
    try {
      ImmutableList<LineOfCode> initProgram = initProgram();
      ImmutableList<LineOfCode> finalProgram = finalProgram(initProgram);
      return LineOfCodeUtil.buildString(finalProgram);
    } catch (UnrecognizedCodeException e) {
      throw new RuntimeException(e);
    }
  }

  /** Generates and returns the sequentialized program that contains dummy reach_error calls. */
  private ImmutableList<LineOfCode> initProgram() throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rProgram = ImmutableList.builder();
    ImmutableSet<MPORThread> threads = substitutions.keySet();

    // add all original program declarations that are not substituted
    rProgram.add(LineOfCode.of(0, SeqComment.UNCHANGED_DECLARATIONS));
    for (MPORThread thread : threads) {
      rProgram.addAll(createNonVarDeclarations(thread));
    }
    rProgram.add(LineOfCode.empty());

    // add all var substitute declarations in the order global - local - params - return_pc
    // global var substitutes
    rProgram.add(LineOfCode.of(0, SeqComment.GLOBAL_VAR_DECLARATIONS));
    MPORThread mainThread = MPORAlgorithm.getMainThread(threads);
    rProgram.addAll(
        createGlobalVarDeclarations(Objects.requireNonNull(substitutions.get(mainThread))));
    rProgram.add(LineOfCode.empty());
    // local var substitutes
    rProgram.add(LineOfCode.of(0, SeqComment.LOCAL_VAR_DECLARATIONS));
    for (CSimpleDeclarationSubstitution substitution : substitutions.values()) {
      rProgram.addAll(createLocalVarDeclarations(substitution));
    }
    rProgram.add(LineOfCode.empty());
    // parameter variables storing function arguments
    rProgram.add(LineOfCode.of(0, SeqComment.PARAMETER_VAR_SUBSTITUTES));
    for (CSimpleDeclarationSubstitution substitution : substitutions.values()) {
      rProgram.addAll(createParameterVarDeclarations(substitution));
    }
    rProgram.add(LineOfCode.empty());
    // ghost return pcs storing calling contexts
    rProgram.add(LineOfCode.of(0, SeqComment.RETURN_PCS));
    ImmutableMap<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>> returnPcVars =
        mapReturnPcVars(threads);
    for (ImmutableMap<CFunctionDeclaration, CIdExpression> map : returnPcVars.values()) {
      for (CIdExpression returnPc : map.values()) {
        rProgram.add(LineOfCode.of(0, returnPc.getDeclaration().toASTString()));
      }
    }
    rProgram.add(LineOfCode.empty());

    // add thread simulation vars
    rProgram.add(LineOfCode.of(0, SeqComment.THREAD_SIMULATION));
    ImmutableMap<ThreadEdge, SubstituteEdge> subEdges =
        SubstituteBuilder.substituteEdges(substitutions);
    GhostThreadVariables threadVars = GhostVariableUtil.buildThreadVariables(threads, subEdges);
    for (CIdExpression threadVar : threadVars.getIdExpressions()) {
      assert threadVar.getDeclaration() instanceof CVariableDeclaration;
      CVariableDeclaration varDeclaration = (CVariableDeclaration) threadVar.getDeclaration();
      rProgram.add(LineOfCode.of(0, varDeclaration.toASTString()));
    }
    rProgram.add(LineOfCode.empty());

    // add all custom function declarations
    rProgram.add(LineOfCode.of(0, SeqComment.CUSTOM_FUNCTION_DECLARATIONS));
    // reach_error, abort, assert, nondet_int may be duplicate depending on the input program
    rProgram.add(LineOfCode.of(0, SeqFunctionDeclaration.ASSERT_FAIL.toASTString()));
    rProgram.add(LineOfCode.of(0, SeqFunctionDeclaration.VERIFIER_NONDET_INT.toASTString()));
    rProgram.add(LineOfCode.of(0, SeqFunctionDeclaration.ABORT.toASTString()));
    rProgram.add(LineOfCode.of(0, SeqFunctionDeclaration.REACH_ERROR.toASTString()));
    rProgram.add(LineOfCode.of(0, SeqFunctionDeclaration.ASSUME.toASTString()));
    // main should always be duplicate
    rProgram.add(LineOfCode.of(0, SeqFunctionDeclaration.MAIN.toASTString()));
    rProgram.add(LineOfCode.empty());

    // add non main() function definitions
    SeqReachErrorFunction reachError = new SeqReachErrorFunction();
    SeqAssumeFunction assume = new SeqAssumeFunction(binaryExpressionBuilder);
    rProgram.addAll(reachError.buildDefinition());
    rProgram.add(LineOfCode.empty());
    rProgram.addAll(assume.buildDefinition());
    rProgram.add(LineOfCode.empty());

    // TODO the case clauses should be multimaps
    // create pruned (i.e. only non-blank) cases statements in main method
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> caseClauses =
        initCaseClauses(substitutions, subEdges, returnPcVars, threadVars);
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> prunedCaseClauses =
        SeqPruner.pruneCaseClauses(caseClauses);
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> finalCaseClauses =
        updateInitialLabels(prunedCaseClauses, logger);
    // optional: include loop invariant assertions over thread variables
    Optional<ImmutableList<SeqLogicalAndExpression>> loopInvariants =
        options.addLoopInvariants
            ? Optional.of(
                createLoopInvariants(
                    substitutions.keySet(), pcVariables, threadVars, binaryExpressionBuilder))
            : Optional.empty();
    // optional: include POR assumptions
    Optional<ImmutableList<SeqFunctionCallExpression>> porAssumptions =
        options.addPOR
            ? Optional.of(
                SeqAssumptionBuilder.createPORAssumptions(
                    finalCaseClauses, pcVariables, binaryExpressionBuilder))
            : Optional.empty();
    ImmutableList<SeqFunctionCallExpression> threadSimulationAssumptions =
        SeqAssumptionBuilder.createThreadSimulationAssumptions(
            pcVariables, threadVars, binaryExpressionBuilder);
    SeqMainFunction mainMethod =
        new SeqMainFunction(
            substitutions.size(),
            options,
            loopInvariants,
            threadSimulationAssumptions,
            porAssumptions,
            finalCaseClauses,
            pcVariables,
            binaryExpressionBuilder);
    rProgram.addAll(mainMethod.buildDefinition());

    return rProgram.build();
  }

  /**
   * Adds the license and sequentialization comments at the top of pInitProgram and replaces the
   * file name and line in {@code reach_error("__FILE_NAME_PLACEHOLDER__", -1,
   * "__SEQUENTIALIZATION_ERROR__");} with pOutputFileName and the actual line.
   */
  private ImmutableList<LineOfCode> finalProgram(ImmutableList<LineOfCode> pInitProgram) {
    // consider license and seq comment header for line numbers
    int currentLine = licenseHeader.size() + mporHeader.size() + 1;
    ImmutableList.Builder<LineOfCode> rProgram = ImmutableList.builder();
    rProgram.addAll(licenseHeader);
    rProgram.addAll(mporHeader);
    for (LineOfCode lineOfCode : pInitProgram) {
      // replace dummy line numbers (-1) with actual line numbers in the seq
      rProgram.add(replaceReachErrorDummies(lineOfCode, currentLine));
      currentLine++;
    }
    return rProgram.build();
  }

  /**
   * Replaces dummy calls to {@code reach_error}, or returns {@code pLineOfCode} as is if there is
   * none.
   */
  private LineOfCode replaceReachErrorDummies(LineOfCode pLineOfCode, int pLineNumber) {
    String code = pLineOfCode.code;

    if (code.contains(inputReachErrorDummy)) {
      // reach_error calls from the input program
      CFunctionCallExpression reachErrorCall =
          buildReachErrorCall(inputFileName, pLineNumber, SeqToken.__PRETTY_FUNCTION__);
      String replacement =
          code.replace(inputReachErrorDummy, reachErrorCall.toASTString() + SeqSyntax.SEMICOLON);
      return pLineOfCode.copyWithCode(replacement);

    } else if (code.contains(outputReachErrorDummy)) {
      // reach_error calls injected by the sequentialization
      CFunctionCallExpression reachErrorCall =
          buildReachErrorCall(
              outputFileName + FileExtension.I.suffix,
              pLineNumber,
              SeqToken.__SEQUENTIALIZATION_ERROR__);
      String replacement =
          code.replace(outputReachErrorDummy, reachErrorCall.toASTString() + SeqSyntax.SEMICOLON);
      return pLineOfCode.copyWithCode(replacement);
    }
    return pLineOfCode;
  }

  // TODO problem: methods such as pthread_cancel allow the termination of another thread.
  //  so if thread i waits for a mutex or a thread, then another thread can cancel i
  //  and the invariants will not hold
  //  -> once we support intermediary thread terminations, remove these invariants
  private ImmutableList<SeqLogicalAndExpression> createLoopInvariants(
      ImmutableSet<MPORThread> pThreads,
      GhostPcVariables pPcVariables,
      GhostThreadVariables pThreadVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqLogicalAndExpression> rAssertions = ImmutableList.builder();
    // add assertions over locks: ti_locks_m && pc[i] == -1) ==> assert_fail
    for (var lockedEntry : pThreadVariables.locks.entrySet()) {
      MPORThread thread = lockedEntry.getKey();
      CBinaryExpression pcNotExitPc =
          SeqBinaryExpression.buildPcUnequalExitPc(
              pPcVariables, thread.id, pBinaryExpressionBuilder);
      for (ThreadLocksMutex locks : lockedEntry.getValue().values()) {
        rAssertions.add(new SeqLogicalAndExpression(locks.idExpression, pcNotExitPc));
      }
    }
    // add assertions over joins: tj_joins_ti && pc[j] == -1 ==> assert_fail
    for (var joinsEntry : pThreadVariables.joins.entrySet()) {
      MPORThread thread = joinsEntry.getKey();
      CBinaryExpression pcNotExitPc =
          SeqBinaryExpression.buildPcUnequalExitPc(
              pPcVariables, thread.id, pBinaryExpressionBuilder);
      for (ThreadJoinsThread joins : joinsEntry.getValue().values()) {
        rAssertions.add(new SeqLogicalAndExpression(joins.idExpression, pcNotExitPc));
      }
    }
    return rAssertions.build();
  }

  /** Maps threads to the case clauses they potentially execute. */
  private ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> initCaseClauses(
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      ImmutableMap<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>>
          pReturnPcVariables,
      GhostThreadVariables pThreadVariables)
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rCaseClauses =
        ImmutableMap.builder();
    for (var entry : pSubstitutions.entrySet()) {
      MPORThread thread = entry.getKey();
      CSimpleDeclarationSubstitution substitution = entry.getValue();
      ImmutableList.Builder<SeqCaseClause> caseClauses = ImmutableList.builder();
      Set<ThreadNode> coveredNodes = new HashSet<>();

      GhostFunctionVariables functionVariables =
          GhostVariableUtil.buildFunctionVariables(
              thread, substitution, pSubstituteEdges, pReturnPcVariables);
      GhostVariables ghostVariables =
          new GhostVariables(functionVariables, pcVariables, pThreadVariables);

      caseClauses.addAll(
          SeqCaseClauseBuilder.buildCaseClauses(
              thread,
              pSubstitutions.keySet(),
              coveredNodes,
              pSubstituteEdges,
              ghostVariables,
              binaryExpressionBuilder));
      rCaseClauses.put(thread, caseClauses.build());
    }
    // modified reach_error result in unreachable statements of that function
    //  -> no validation of case clauses here
    return rCaseClauses.buildOrThrow();
  }

  /**
   * Ensures that the initial label {@code pc} for all threads is {@link
   * SeqCaseClauseBuilder#INIT_PC}.
   */
  private ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> updateInitialLabels(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses, LogManager pLogger) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rUpdated =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      boolean firstCase = true;
      ImmutableList.Builder<SeqCaseClause> updatedCases = ImmutableList.builder();
      // this approach (just taking the first case) is sound because the path up to the first
      //  non-blank case is deterministic (i.e. only 1 leaving edge)
      for (SeqCaseClause caseClause : entry.getValue()) {
        assert !caseClause.isPrunable()
            : "case clause is still prunable. did you use the pruned case clauses?";
        if (firstCase) {
          updatedCases.add(caseClause.cloneWithLabel(new SeqCaseLabel(Sequentialization.INIT_PC)));
          firstCase = false;
        } else {
          updatedCases.add(caseClause);
        }
      }
      rUpdated.put(entry.getKey(), updatedCases.build());
    }
    return SeqValidator.validateCaseClauses(rUpdated.buildOrThrow(), pLogger);
  }

  /**
   * Maps {@link CFunctionDeclaration}s to {@code return_pc} {@link CIdExpression}s for all threads.
   *
   * <p>E.g. the function {@code fib} in thread 0 is mapped to the expression of {@code int
   * __return_pc_t0_fib}.
   */
  private ImmutableMap<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>>
      mapReturnPcVars(ImmutableSet<MPORThread> pThreads) {

    ImmutableMap.Builder<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>> rVars =
        ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      ImmutableMap.Builder<CFunctionDeclaration, CIdExpression> returnPc = ImmutableMap.builder();
      for (CFunctionDeclaration function : thread.cfa.calledFuncs) {
        // no RETURN_PC for reach_error, the function never returns
        if (!function.getOrigName().equals(SeqToken.reach_error)) {
          CVariableDeclaration varDec =
              SeqVariableDeclaration.buildReturnPcVariableDeclaration(
                  thread.id, function.getName());
          returnPc.put(function, SeqIdExpression.buildIdExpression(varDec));
        }
      }
      rVars.put(thread, returnPc.buildOrThrow());
    }
    return rVars.buildOrThrow();
  }

  // LOC Creators =============================================================================

  /**
   * Creates {@link LineOfCode}s for all non-variable declarations (e.g. function and struct
   * declarations) for the given thread.
   */
  private ImmutableList<LineOfCode> createNonVarDeclarations(MPORThread pThread) {
    ImmutableList.Builder<LineOfCode> rNonVarDeclarations = ImmutableList.builder();
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      if (threadEdge.cfaEdge instanceof CDeclarationEdge decEdge) {
        CDeclaration dec = decEdge.getDeclaration();
        if (!(dec instanceof CVariableDeclaration)) {
          assert pThread.isMain(); // test if only main thread declares non-vars, e.g. functions
          rNonVarDeclarations.addAll(LineOfCodeUtil.buildLinesOfCode(threadEdge.cfaEdge.getCode()));
        }
      }
    }
    return rNonVarDeclarations.build();
  }

  /**
   * Creates {@link LineOfCode}s for all global variable declarations for based on the substitutions
   * of the main thread.
   */
  private ImmutableList<LineOfCode> createGlobalVarDeclarations(
      CSimpleDeclarationSubstitution pSubstitution) {

    ImmutableList.Builder<LineOfCode> rGlobalVarDeclarations = ImmutableList.builder();
    for (CIdExpression globalVar : pSubstitution.globalSubstitutes.orElseThrow().values()) {
      rGlobalVarDeclarations.add(LineOfCode.of(0, globalVar.getDeclaration().toASTString()));
    }
    return rGlobalVarDeclarations.build();
  }

  /**
   * Creates {@link LineOfCode}s for all local variable declarations for the given thread id based
   * on the given main thread.
   */
  private ImmutableList<LineOfCode> createLocalVarDeclarations(
      CSimpleDeclarationSubstitution pSubstitution) {

    ImmutableList.Builder<LineOfCode> rLocalVarDeclarations = ImmutableList.builder();
    for (CIdExpression localVar : pSubstitution.localSubstitutes.values()) {
      CVariableDeclaration varDeclaration =
          pSubstitution.castToVarDeclaration(localVar.getDeclaration());
      if (!MPORUtil.isConstCpaCheckerTmp(varDeclaration)) {
        rLocalVarDeclarations.add(LineOfCode.of(0, varDeclaration.toASTString()));
      }
    }
    return rLocalVarDeclarations.build();
  }

  private ImmutableList<LineOfCode> createParameterVarDeclarations(
      CSimpleDeclarationSubstitution pSubstitution) {

    ImmutableList.Builder<LineOfCode> rParameterVarDeclarations = ImmutableList.builder();
    for (CIdExpression param : pSubstitution.parameterSubstitutes.orElseThrow().values()) {
      rParameterVarDeclarations.add(LineOfCode.of(0, param.getDeclaration().toASTString()));
    }
    return rParameterVarDeclarations.build();
  }

  // Helpers for better Overview =================================================================

  /**
   * Returns the {@link CFunctionCallExpression} of {@code reach_error("{pFile}", {pLine},
   * "{pFunction}")}
   */
  public static CFunctionCallExpression buildReachErrorCall(
      String pFile, int pLine, String pFunction) {

    CStringLiteralExpression file =
        SeqStringLiteralExpression.buildStringLiteralExpression(
            SeqStringUtil.wrapInQuotationMarks(pFile));
    CIntegerLiteralExpression line =
        SeqIntegerLiteralExpression.buildIntegerLiteralExpression(pLine);
    CStringLiteralExpression function =
        SeqStringLiteralExpression.buildStringLiteralExpression(
            SeqStringUtil.wrapInQuotationMarks(pFunction));
    return new CFunctionCallExpression(
        FileLocation.DUMMY,
        SeqVoidType.VOID,
        SeqIdExpression.REACH_ERROR,
        ImmutableList.of(file, line, function),
        SeqFunctionDeclaration.REACH_ERROR);
  }
}
