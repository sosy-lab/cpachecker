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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.SeqBitVectorEncoding;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class InputRejection {

  public enum InputRejectionMessage {
    INVALID_OPTIONS("Invalid MPOR options, see above errors.", false),
    LANGUAGE_NOT_C("MPOR only supports language C", false),
    NOT_CONCURRENT(
        "MPOR expects concurrent C program with at least one pthread_create call", false),
    PTHREAD_CREATE_LOOP(
        "MPOR does not support pthread_create calls in loops (or recursive functions)", false),
    NO_PTHREAD_OBJECT_ARRAYS("MPOR does not support arrays of pthread objects in line ", true),
    UNSUPPORTED_FUNCTION("MPOR does not support the function in line ", true),
    PTHREAD_RETURN_VALUE(
        "MPOR does not support pthread method return value assignments in line ", true),
    RECURSIVE_FUNCTION("MPOR does not support the (in)direct recursive function in line ", true);

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

  /**
   * Handles input program rejections and throws an {@link IllegalArgumentException} if the input
   * program...
   *
   * <ul>
   *   <li>is not in C
   *   <li>has no call to {@code pthread_create} i.e. is not concurrent
   *   <li>uses arrays for {@code pthread_t} or {@code pthread_mutex_t} identifiers
   *   <li>stores the return value of any pthread method call
   *   <li>contains any unsupported {@code pthread} function, see {@link PthreadFunctionType}
   *   <li>contains a {@code pthread_create} call in a loop
   *   <li>contains a recursive function call (both direct and indirect)
   * </ul>
   */
  public static void handleRejections(LogManager pLogger, MPOROptions pOptions, CFA pInputCfa) {
    checkOptions(pLogger, pOptions);
    checkLanguageC(pLogger, pInputCfa);
    checkIsParallelProgram(pLogger, pInputCfa);
    checkUnsupportedFunctions(pLogger, pInputCfa);
    checkPthreadObjectArrays(pLogger, pInputCfa);
    checkPthreadFunctionReturnValues(pLogger, pInputCfa);
    // these are recursive and can be expensive, so they are last
    checkPthreadCreateLoops(pLogger, pInputCfa);
    checkRecursiveFunctions(pLogger, pInputCfa);
  }

  @TerminatesExecution
  private static void handleRejection(
      LogManager pLogger, InputRejectionMessage pMessage, Object... args) {
    String formatted = String.format(pMessage.formatMessage(), args);
    // using RuntimeException because checkArgument throws IllegalArgumentExceptions
    pLogger.logUserException(
        Level.SEVERE, new RuntimeException(), String.format(pMessage.formatMessage(), args));
    // we need the error message here too for unit tests (matching error messages to programs)
    throw new RuntimeException(formatted);
  }

  private static void checkOptions(LogManager pLogger, MPOROptions pOptions) {
    if (pOptions.porBitVector && pOptions.porBitVectorEncoding.equals(SeqBitVectorEncoding.NONE)) {
      pLogger.log(
          Level.SEVERE,
          "porBitVector is enabled, but porBitVectorEncoding is not set. Either disable"
              + " porBitVector or specify porBitVectorEncoding.");
      handleRejection(pLogger, InputRejectionMessage.INVALID_OPTIONS);
    }
  }

  private static void checkLanguageC(LogManager pLogger, CFA pInputCfa) {
    Language language = pInputCfa.getMetadata().getInputLanguage();
    if (!language.equals(Language.C)) {
      handleRejection(pLogger, InputRejectionMessage.LANGUAGE_NOT_C);
    }
  }

  private static void checkIsParallelProgram(LogManager pLogger, CFA pInputCfa) {
    boolean isParallel = false;
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pInputCfa)) {
      if (PthreadUtil.callsPthreadFunction(cfaEdge, PthreadFunctionType.PTHREAD_CREATE)) {
        isParallel = true;
        break;
      }
    }
    if (!isParallel) {
      handleRejection(pLogger, InputRejectionMessage.NOT_CONCURRENT);
    }
  }

  private static void checkPthreadObjectArrays(LogManager pLogger, CFA pInputCfa) {
    for (CFAEdge edge : CFAUtils.allEdges(pInputCfa)) {
      if (edge instanceof CDeclarationEdge decEdge) {
        if (decEdge.getDeclaration() instanceof CVariableDeclaration varDec) {
          if (varDec.getType() instanceof CArrayType arrayType) {
            if (arrayType.getType() instanceof CTypedefType typedefType) {
              String typedefName = typedefType.getName();
              if (typedefName.equals(PthreadObjectType.PTHREAD_T.name)
                  || typedefName.equals(PthreadObjectType.PTHREAD_MUTEX_T.name)) {
                handleRejection(
                    pLogger,
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

  private static void checkUnsupportedFunctions(LogManager pLogger, CFA pInputCfa) {
    for (CFAEdge edge : CFAUtils.allEdges(pInputCfa)) {
      for (PthreadFunctionType funcType : PthreadFunctionType.values()) {
        if (!funcType.isSupported) {
          if (PthreadUtil.callsPthreadFunction(edge, funcType)) {
            handleRejection(
                pLogger,
                InputRejectionMessage.UNSUPPORTED_FUNCTION,
                edge.getLineNumber(),
                edge.getCode());
          }
        }
      }
    }
  }

  // TODO 0 == pthread_create (thread creation success) can be simulated by if (nondet_int) pc1 = 0
  //  i.e. it is not guaranteed that thread starts etc.
  private static void checkPthreadFunctionReturnValues(LogManager pLogger, CFA pInputCfa) {
    for (CFAEdge edge : CFAUtils.allEdges(pInputCfa)) {
      if (PthreadUtil.callsAnyPthreadFunction(edge)) {
        if (edge.getRawAST().orElseThrow() instanceof CFunctionCallAssignmentStatement) {
          handleRejection(
              pLogger,
              InputRejectionMessage.PTHREAD_RETURN_VALUE,
              edge.getLineNumber(),
              edge.getCode());
        }
      }
    }
  }

  /**
   * Recursively checks if any {@code pthread_create} call in pInputCfa can be reached from itself,
   * i.e. if it is in a loop (or in a recursive call).
   */
  private static void checkPthreadCreateLoops(LogManager pLogger, CFA pInputCfa) {
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pInputCfa)) {
      if (PthreadUtil.callsPthreadFunction(cfaEdge, PthreadFunctionType.PTHREAD_CREATE)) {
        if (MPORUtil.isSelfReachable(cfaEdge, Optional.empty(), new ArrayList<>(), cfaEdge)) {
          handleRejection(pLogger, InputRejectionMessage.PTHREAD_CREATE_LOOP);
        }
      }
    }
  }

  private static void checkRecursiveFunctions(LogManager pLogger, CFA pInputCfa) {
    for (FunctionEntryNode entry : pInputCfa.entryNodes()) {
      Optional<FunctionExitNode> exit = entry.getExitNode();
      // "upcasting" exit from FunctionExitNode to CFANode is necessary here...
      if (MPORUtil.isSelfReachable(entry, exit.map(node -> node), new ArrayList<>(), entry)) {
        handleRejection(
            pLogger,
            InputRejectionMessage.RECURSIVE_FUNCTION,
            entry.getFunction().getFileLocation().getStartingLineNumber(),
            entry.getFunctionName());
      }
    }
  }
}
