// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection;

import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.dataflow.qual.TerminatesExecution;
import org.sosy_lab.common.log.LogManager;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFuncType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class InputRejection {

  public enum InputRejectionMessage {
    LANGUAGE_NOT_C("MPOR only supports language C", false),
    // TODO this can probably be removed entirely since CPAchecker handles this
    MULTIPLE_INPUT_FILES("MPOR only supports exactly one input file", false),
    NOT_CONCURRENT(
        "MPOR expects concurrent C program with at least one pthread_create call", false),
    PTHREAD_CREATE_LOOP(
        "MPOR does not support pthread_create calls in loops (or recursive functions)", false),
    NO_PTHREAD_OBJECT_ARRAYS("MPOR does not support arrays of pthread objects in line ", true),
    UNSUPPORTED_FUNCTION("MPOR does not support the function in line ", true),
    PTHREAD_RETURN_VALUE(
        "MPOR does not support pthread method return value assignments in line ", true),
    RECURSIVE_FUNCTION("MPOR does not support the (in)direct recursive function in line ", true),
    // TODO test if this can be removed entirely
    NO_FUNC_EXIT_NODE(
        "MPOR expects the main function and all start routines to contain a FunctionExitNode",
        false);

    public final String message;

    private final boolean containsLineAndCode;

    InputRejectionMessage(String pMessage, boolean pContainsLineAndCode) {
      message = pMessage;
      containsLineAndCode = pContainsLineAndCode;
    }

    public String formatMessage() {
      if (containsLineAndCode) {
        return message + "%s: %s";
      } else {
        return message;
      }
    }
  }

  private static LogManager logger;

  /**
   * Handles initial (i.e. more may come at later stages of the MPOR transformation) input program
   * rejections and throws an {@link IllegalArgumentException} if the input program...
   *
   * <ul>
   *   <li>is not in C
   *   <li>contains multiple files
   *   <li>has no call to {@code pthread_create} i.e. is not concurrent
   *   <li>uses arrays for {@code pthread_t} or {@code pthread_mutex_t} identifiers
   *   <li>stores the return value of any pthread method call
   *   <li>contains any unsupported {@code pthread} function, see {@link PthreadFuncType}
   *   <li>contains a {@code pthread_create} call in a loop
   *   <li>contains a recursive function call (both direct and indirect)
   * </ul>
   */
  public static void handleInitialRejections(LogManager pLogger, CFA pInputCfa) {
    logger = pLogger;
    checkLanguageC(pInputCfa);
    checkIsParallelProgram(pInputCfa);
    checkUnsupportedFunctions(pInputCfa);
    checkPthreadObjectArrays(pInputCfa);
    checkPthreadFunctionReturnValues(pInputCfa);
    // these are recursive and can be expensive, so they are last
    checkPthreadCreateLoops(pInputCfa);
    checkRecursiveFunctions(pInputCfa);
  }

  @TerminatesExecution
  private static void handleRejection(InputRejectionMessage pMessage, Object... args) {
    String formatted = String.format(pMessage.formatMessage(), args);
    // using RuntimeException because checkArgument throws IllegalArgumentExceptions
    logger.logUserException(Level.SEVERE, new RuntimeException(), formatted);
    throw new RuntimeException(formatted);
  }

  private static void checkLanguageC(CFA pInputCfa) {
    Language language = pInputCfa.getMetadata().getInputLanguage();
    if (!language.equals(Language.C)) {
      handleRejection(InputRejectionMessage.LANGUAGE_NOT_C);
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
      handleRejection(InputRejectionMessage.NOT_CONCURRENT);
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
                    InputRejectionMessage.NO_PTHREAD_OBJECT_ARRAYS,
                    Integer.toString(edge.getLineNumber()),
                    edge.getCode());
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
            handleRejection(
                InputRejectionMessage.UNSUPPORTED_FUNCTION, edge.getLineNumber(), edge.getCode());
          }
        }
      }
    }
  }

  private static void checkPthreadFunctionReturnValues(CFA pInputCfa) {
    for (CFAEdge edge : CFAUtils.allEdges(pInputCfa)) {
      if (PthreadFuncType.callsAnyPthreadFunc(edge)) {
        if (edge.getRawAST().orElseThrow() instanceof CFunctionCallAssignmentStatement) {
          handleRejection(
              InputRejectionMessage.PTHREAD_RETURN_VALUE, edge.getLineNumber(), edge.getCode());
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
          handleRejection(InputRejectionMessage.PTHREAD_CREATE_LOOP);
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
            InputRejectionMessage.RECURSIVE_FUNCTION,
            entry.getFunction().getFileLocation().getStartingLineNumber(),
            entry.getFunctionName());
      }
    }
  }

  // TODO this can probably be removed entirely -> take a look how the exitnode is used
  //  at the moment just for the TSOs, which are not used
  /**
   * Tries to extract the FunctionExitNode from the given FunctionEntryNode and throws an {@link
   * IllegalArgumentException} if there is none.
   */
  public static FunctionExitNode getFunctionExitNode(FunctionEntryNode pFunctionEntryNode) {
    if (pFunctionEntryNode.getExitNode().isEmpty()) {
      handleRejection(InputRejectionMessage.NO_FUNC_EXIT_NODE);
    }
    return pFunctionEntryNode.getExitNode().orElseThrow();
  }
}
