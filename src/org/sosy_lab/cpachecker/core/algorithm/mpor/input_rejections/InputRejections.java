// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejections;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.util.ArrayList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cmdline.Output;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORStatics;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFuncType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class InputRejections {

  public static final String LANGUAGE_NOT_C = "MPOR only supports language C";

  public static final String MULTIPLE_INPUT_FILES = "MPOR only supports exactly one input file";

  public static final String NOT_PARALLEL =
      "MPOR expects parallel C program with at least one pthread_create call";

  public static final String PTHREAD_CREATE_LOOP =
      "MPOR does not support pthread_create calls in loops (or recursive functions)";

  public static final String NO_PTHREAD_OBJECT_ARRAYS =
      "MPOR does not support arrays of pthread objects in line ";

  private static final String NO_PTHREAD_OBJECT_ARRAYS_FORMAT = NO_PTHREAD_OBJECT_ARRAYS + "%s: %s";

  public static final String UNSUPPORTED_FUNCTION = "MPOR does not support the function in line ";

  private static final String UNSUPPORTED_FUNCTION_FORMAT = UNSUPPORTED_FUNCTION + "%s: %s";

  public static final String PTHREAD_RETURN_VALUE =
      "MPOR does not support pthread method return value assignments in line ";

  private static final String PTHREAD_RETURN_VALUE_FORMAT = PTHREAD_RETURN_VALUE + "%s: %s";

  public static final String RECURSIVE_FUNCTION =
      "MPOR does not support the (in)direct recursive function in line ";

  private static final String RECURSIVE_FUNCTION_FORMAT = RECURSIVE_FUNCTION + "%s: %s";

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
    checkPthreadObjectArrays(pInputCfa);
    checkPthreadFunctionReturnValues(pInputCfa);
    // these are recursive and can be expensive, so they are last
    checkPthreadCreateLoops(pInputCfa);
    checkRecursiveFunctions(pInputCfa);
  }

  @FormatMethod
  private static void handleRejection(@FormatString final String pMessage, Object... args) {
    switch (MPORStatics.instanceType()) {
      case PRODUCTION -> throw Output.fatalError(pMessage, args);
      case TEST -> throw new RuntimeException(String.format(pMessage, args));
      default -> throw Output.fatalError("Invalid InstanceType: %s", MPORStatics.instanceType());
    }
  }

  private static void checkLanguageC(CFA pInputCfa) {
    Language language = pInputCfa.getMetadata().getInputLanguage();
    if (!language.equals(Language.C)) {
      handleRejection(LANGUAGE_NOT_C);
    }
  }

  private static void checkOneInputFile(CFA pInputCfa) {
    if (pInputCfa.getFileNames().size() != 1) {
      handleRejection(MULTIPLE_INPUT_FILES);
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
      handleRejection(NOT_PARALLEL);
    }
  }

  private static void checkPthreadObjectArrays(CFA pInputCfa) {
    for (CFAEdge edge : CFAUtils.allEdges(pInputCfa)) {
      if (edge instanceof CDeclarationEdge decEdge) {
        if (decEdge.getDeclaration() instanceof CVariableDeclaration varDec) {
          if (varDec.getType() instanceof CArrayType arrayType) {
            if (arrayType.getType() instanceof CTypedefType typedefType) {
              String typedefName = typedefType.getName();
              if (typedefName.equals(PthreadObjectType.PTHREAD_T.name)
                  || typedefName.equals(PthreadObjectType.PTHREAD_MUTEX_T.name)) {
                handleRejection(
                    NO_PTHREAD_OBJECT_ARRAYS_FORMAT, edge.getLineNumber(), edge.getCode());
              }
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
            handleRejection(UNSUPPORTED_FUNCTION_FORMAT, edge.getLineNumber(), edge.getCode());
          }
        }
      }
    }
  }

  private static void checkPthreadFunctionReturnValues(CFA pInputCfa) {
    for (CFAEdge edge : CFAUtils.allEdges(pInputCfa)) {
      if (PthreadFuncType.callsAnyPthreadFunc(edge)) {
        if (edge.getRawAST().orElseThrow() instanceof CFunctionCallAssignmentStatement) {
          handleRejection(PTHREAD_RETURN_VALUE_FORMAT, edge.getLineNumber(), edge.getCode());
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
          handleRejection(PTHREAD_CREATE_LOOP);
        }
      }
    }
  }

  private static void checkRecursiveFunctions(CFA pInputCfa) {
    for (FunctionEntryNode entry : pInputCfa.entryNodes()) {
      Optional<FunctionExitNode> exit = entry.getExitNode();
      // "upcasting" exit from FunctionExitNode to CFANode is necessary here...
      if (MPORUtil.isSelfReachable(entry, exit.map(node -> node), new ArrayList<>(), entry)) {
        handleRejection(
            RECURSIVE_FUNCTION_FORMAT,
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
      handleRejection(NO_FUNC_EXIT_NODE);
    }
    return pFunctionEntryNode.getExitNode().orElseThrow();
  }
}