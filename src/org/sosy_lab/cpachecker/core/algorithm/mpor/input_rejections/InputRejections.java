// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejections;

import java.util.ArrayList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cmdline.Output;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORStatics;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFuncType;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class InputRejections {

  public static final String LANGUAGE_NOT_C = "MPOR only supports language C";

  public static final String MULTIPLE_INPUT_FILES = "MPOR only supports exactly one input file";

  public static final String NOT_PARALLEL =
      "MPOR expects parallel C program with at least one pthread_create call";

  public static final String PTHREAD_CREATE_LOOP =
      "MPOR does not support pthread_create calls in loops (or recursive functions)";

  public static final String NO_PTHREAD_T_ARRAYS =
      "MPOR does not support arrays as pthread_t parameters in line ";

  public static final String NO_PTHREAD_MUTEX_T_ARRAYS =
      "MPOR does not support arrays as pthread_mutex_t parameters in line ";

  public static final String UNSUPPORTED_FUNC = "MPOR does not support the function in line ";

  public static final String PTHREAD_RETURN_VALUES =
      "MPOR does not support pthread method return value assignments in line ";

  public static final String RECURSIVE_FUNCTION =
      "MPOR does not support the (in)direct recursive function in line ";

  public static final String NO_FUNC_EXIT_NODE =
      "MPOR expects the main function and all start routines to contain a FunctionExitNode";

  /**
   * Handles initial (i.e. more may come at later stages of the MPOR transformation) input program
   * rejections and throws an {@link IllegalArgumentException} if the input program...
   *
   * <ul>
   *   <li>is not in C
   *   <li>contains multiple files
   *   <li>has no call to {@code pthread_create} i.e. is not parallel
   *   <li>uses arrays for {@code pthread_t} or {@code pthread_mutex_t} identifiers
   *   <li>stores the return value of any pthread method call
   *   <li>contains any unsupported {@code pthread} function, see {@link PthreadFuncType}
   *   <li>contains a {@code pthread_create} call in a loop
   *   <li>contains a recursive function call (both direct and indirect)
   * </ul>
   */
  public static void handleInitialRejections(CFA pInputCfa) {
    // TODO check for preprocessed files (all files must have .i ending)
    checkLanguageC(pInputCfa);
    checkOneInputFile(pInputCfa);
    checkIsParallelProgram(pInputCfa);
    checkUnsupportedFunctions(pInputCfa);
    checkPthreadArrayIdentifiers(pInputCfa);
    checkPthreadFunctionReturnValues(pInputCfa);
    // these are recursive and can be expensive, so they are last
    checkPthreadCreateLoops(pInputCfa);
    checkRecursiveFunctions(pInputCfa);
  }

  private static void checkLanguageC(CFA pInputCfa) {
    Language language = pInputCfa.getMetadata().getInputLanguage();
    if (!language.equals(Language.C)) {
      switch (MPORStatics.instanceType()) {
        case PRODUCTION -> throw Output.fatalError(LANGUAGE_NOT_C);
        case TEST -> throw new RuntimeException(LANGUAGE_NOT_C);
      }
    }
  }

  private static void checkOneInputFile(CFA pInputCfa) {
    if (pInputCfa.getFileNames().size() != 1) {
      switch (MPORStatics.instanceType()) {
        case PRODUCTION -> throw Output.fatalError(MULTIPLE_INPUT_FILES);
        case TEST -> throw new RuntimeException(MULTIPLE_INPUT_FILES);
      }
    }
  }

  private static void checkIsParallelProgram(CFA pInputCfa) {
    boolean isParallel = false;
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pInputCfa)) {
      if (PthreadFuncType.callsPthreadFunc(cfaEdge, PthreadFuncType.PTHREAD_CREATE)) {
        isParallel = true;
        break;
      }
    }
    if (!isParallel) {
      switch (MPORStatics.instanceType()) {
        case PRODUCTION -> throw Output.fatalError(NOT_PARALLEL);
        case TEST -> throw new RuntimeException(NOT_PARALLEL);
      }
    }
  }

  private static void checkPthreadArrayIdentifiers(CFA pInputCfa) {
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pInputCfa)) {
      for (PthreadFuncType funcType : PthreadFuncType.values()) {
        if (funcType.isSupported && PthreadFuncType.callsPthreadFunc(cfaEdge, funcType)) {
          if (funcType.hasPthreadTIndex()) {
            int pthreadTIndex = funcType.getPthreadTIndex();
            CExpression parameter = CFAUtils.getParameterAtIndex(cfaEdge, pthreadTIndex);
            if (isArraySubscriptExpression(parameter)) {
              throw Output.fatalError(
                  NO_PTHREAD_T_ARRAYS + "%s: %s", cfaEdge.getLineNumber(), cfaEdge.getCode());
            }
          }
          if (funcType.hasPthreadMutexTIndex()) {
            int pthreadMutexTIndex = funcType.getPthreadMutexTIndex();
            CExpression parameter = CFAUtils.getParameterAtIndex(cfaEdge, pthreadMutexTIndex);
            if (isArraySubscriptExpression(parameter)) {
              throw Output.fatalError(
                  NO_PTHREAD_MUTEX_T_ARRAYS + "%s: %s", cfaEdge.getLineNumber(), cfaEdge.getCode());
            }
          }
        }
      }
    }
  }

  private static void checkUnsupportedFunctions(CFA pInputCfa) {
    for (CFAEdge edge : CFAUtils.allEdges(pInputCfa)) {
      for (PthreadFuncType funcType : PthreadFuncType.values()) {
        if (!funcType.isSupported) {
          if (PthreadFuncType.callsPthreadFunc(edge, funcType)) {
            throw Output.fatalError(
                UNSUPPORTED_FUNC + "%s: %s", edge.getLineNumber(), edge.getCode());
          }
        }
      }
    }
  }

  private static void checkPthreadFunctionReturnValues(CFA pInputCfa) {
    for (CFAEdge edge : CFAUtils.allEdges(pInputCfa)) {
      if (PthreadFuncType.callsAnyPthreadFunc(edge)) {
        if (edge.getRawAST().orElseThrow() instanceof CFunctionCallAssignmentStatement) {
          throw Output.fatalError(
              PTHREAD_RETURN_VALUES + "%s: %s", edge.getLineNumber(), edge.getCode());
        }
      }
    }
  }

  /**
   * Recursively checks if any {@code pthread_create} call in pInputCfa can be reached from itself,
   * i.e. if it is in a loop (or in a recursive call).
   */
  private static void checkPthreadCreateLoops(CFA pInputCfa) {
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pInputCfa)) {
      if (PthreadFuncType.callsPthreadFunc(cfaEdge, PthreadFuncType.PTHREAD_CREATE)) {
        if (MPORUtil.isSelfReachable(cfaEdge, Optional.empty(), new ArrayList<>(), cfaEdge)) {
          switch (MPORStatics.instanceType()) {
            case PRODUCTION -> throw Output.fatalError(PTHREAD_CREATE_LOOP);
            case TEST -> throw new RuntimeException(PTHREAD_CREATE_LOOP);
          }
        }
      }
    }
  }

  private static void checkRecursiveFunctions(CFA pInputCfa) {
    for (FunctionEntryNode entry : pInputCfa.entryNodes()) {
      Optional<FunctionExitNode> exit = entry.getExitNode();
      // "upcasting" exit from FunctionExitNode to CFANode is necessary here...
      if (MPORUtil.isSelfReachable(entry, exit.map(node -> node), new ArrayList<>(), entry)) {
        throw Output.fatalError(
            RECURSIVE_FUNCTION + "%s: %s",
            entry.getFunction().getFileLocation().getStartingLineNumber(),
            entry.getFunctionName());
      }
    }
  }

  /**
   * Tries to extract the FunctionExitNode from the given FunctionEntryNode and throws an {@link
   * IllegalArgumentException} if there is none.
   */
  public static FunctionExitNode getFunctionExitNode(FunctionEntryNode pFunctionEntryNode) {
    if (pFunctionEntryNode.getExitNode().isEmpty()) {
      throw Output.fatalError(NO_FUNC_EXIT_NODE);
    }
    return pFunctionEntryNode.getExitNode().orElseThrow();
  }

  // Helpers =======================================================================================

  private static boolean isArraySubscriptExpression(CExpression pExpression) {
    if (pExpression instanceof CArraySubscriptExpression) {
      return true;
    } else if (pExpression instanceof CUnaryExpression unary) {
      if (unary.getOperand() instanceof CArraySubscriptExpression) {
        return true;
      }
    }
    return false;
  }
}
