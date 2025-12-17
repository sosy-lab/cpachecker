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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class InputRejection {

  enum InputRejectionMessage {
    FUNCTION_POINTER_IN_ASSIGNMENT(
        "MPOR does not support function pointers in assignments: ", false),
    FUNCTION_POINTER_PARAMETER("MPOR does not support function pointers as parameters: ", false),
    INVALID_OPTIONS("Invalid MPOR options, see above errors.", false),
    LANGUAGE_NOT_C("MPOR only supports language C", false),
    NOT_CONCURRENT(
        "MPOR expects concurrent C program with at least one pthread_create call", false),
    NO_PTHREAD_OBJECT_ARRAYS("MPOR does not support arrays of pthread objects in line ", true),
    POINTER_WRITE(
        "allowPointerWrites is disabled, but the input program contains a pointer write in line ",
        true),
    PTHREAD_CREATE_LOOP(
        "MPOR does not support pthread_create calls in loops (or recursive functions)", false),
    PTHREAD_RETURN_VALUE(
        "MPOR does not support pthread method return value assignments in line ", true),
    RECURSIVE_FUNCTION("MPOR does not support the (in)direct recursive function in line ", true),
    UNSUPPORTED_FUNCTION("MPOR does not support the function in line ", true);

    final String message;

    final boolean containsLineAndCode;

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
   * Handles input program rejections and throws a {@link UnsupportedCodeException} if the input
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
  public static void handleRejections(CFA pInputCfa) throws UnsupportedCodeException {
    checkLanguageC(pInputCfa);
    checkIsParallelProgram(pInputCfa);
    checkUnsupportedFunctions(pInputCfa);
    checkPthreadObjectArrays(pInputCfa);
    checkPthreadFunctionReturnValues(pInputCfa);
    // these are recursive and can be expensive, so they are last
    checkPthreadCreateLoops(pInputCfa);
    checkRecursiveFunctions(pInputCfa);
  }

  private static void rejectCfaEdge(CFAEdge pCfaEdge, InputRejectionMessage pMessage)
      throws UnsupportedCodeException {

    throw new UnsupportedCodeException(
        String.format(pMessage.formatMessage(), pCfaEdge.getLineNumber(), pCfaEdge.getCode()),
        pCfaEdge);
  }

  private static void checkLanguageC(CFA pInputCfa) throws UnsupportedCodeException {
    Language language = pInputCfa.getMetadata().getInputLanguage();
    if (!language.equals(Language.C)) {
      throw new UnsupportedCodeException(InputRejectionMessage.LANGUAGE_NOT_C.message, null);
    }
  }

  private static void checkIsParallelProgram(CFA pInputCfa) throws UnsupportedCodeException {
    boolean isParallel = false;
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pInputCfa)) {
      Optional<CFunctionCall> functionCall = PthreadUtil.tryGetFunctionCallFromCfaEdge(cfaEdge);
      if (functionCall.isPresent()) {
        if (PthreadUtil.isCallToPthreadFunction(
            functionCall.orElseThrow(), PthreadFunctionType.PTHREAD_CREATE)) {
          isParallel = true;
          break;
        }
      }
    }
    if (!isParallel) {
      throw new UnsupportedCodeException(InputRejectionMessage.NOT_CONCURRENT.message, null);
    }
  }

  private static void checkPthreadObjectArrays(CFA pInputCfa) throws UnsupportedCodeException {
    for (CFAEdge edge : CFAUtils.allEdges(pInputCfa)) {
      if (edge instanceof CDeclarationEdge decEdge) {
        if (decEdge.getDeclaration() instanceof CVariableDeclaration varDec) {
          if (varDec.getType() instanceof CArrayType arrayType) {
            if (arrayType.getType() instanceof CTypedefType typedefType) {
              if (PthreadUtil.isAnyPthreadObjectType(typedefType)) {
                rejectCfaEdge(edge, InputRejectionMessage.NO_PTHREAD_OBJECT_ARRAYS);
              }
            }
          }
        }
      }
    }
  }

  private static void checkUnsupportedFunctions(CFA pInputCfa) throws UnsupportedCodeException {
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pInputCfa)) {
      for (PthreadFunctionType functionType : PthreadFunctionType.values()) {
        if (!functionType.isSupported) {
          Optional<CFunctionCall> functionCall = PthreadUtil.tryGetFunctionCallFromCfaEdge(cfaEdge);
          if (functionCall.isPresent()) {
            if (PthreadUtil.isCallToPthreadFunction(functionCall.orElseThrow(), functionType)) {
              rejectCfaEdge(cfaEdge, InputRejectionMessage.UNSUPPORTED_FUNCTION);
            }
          }
        }
      }
    }
  }

  private static void checkPthreadFunctionReturnValues(CFA pInputCfa)
      throws UnsupportedCodeException {
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pInputCfa)) {
      Optional<CFunctionCall> functionCall = PthreadUtil.tryGetFunctionCallFromCfaEdge(cfaEdge);
      if (functionCall.isPresent()) {
        if (PthreadUtil.isCallToAnyPthreadFunction(functionCall.orElseThrow())) {
          if (cfaEdge.getRawAST().orElseThrow() instanceof CFunctionCallAssignmentStatement) {
            rejectCfaEdge(cfaEdge, InputRejectionMessage.PTHREAD_RETURN_VALUE);
          }
        }
      }
    }
  }

  /**
   * Recursively checks if any {@code pthread_create} call in pInputCfa can be reached from itself,
   * i.e. if it is in a loop (or in a recursive call).
   */
  private static void checkPthreadCreateLoops(CFA pInputCfa) throws UnsupportedCodeException {
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pInputCfa)) {
      Optional<CFunctionCall> functionCall = PthreadUtil.tryGetFunctionCallFromCfaEdge(cfaEdge);
      if (functionCall.isPresent()) {
        if (PthreadUtil.isCallToPthreadFunction(
            functionCall.orElseThrow(), PthreadFunctionType.PTHREAD_CREATE)) {
          if (MPORUtil.isSelfReachable(cfaEdge, Optional.empty(), new ArrayList<>(), cfaEdge)) {
            rejectCfaEdge(cfaEdge, InputRejectionMessage.PTHREAD_CREATE_LOOP);
          }
        }
      }
    }
  }

  private static void checkRecursiveFunctions(CFA pInputCfa) throws UnsupportedCodeException {
    for (FunctionEntryNode entry : pInputCfa.entryNodes()) {
      Optional<FunctionExitNode> exit = entry.getExitNode();
      // "upcasting" exit from FunctionExitNode to CFANode is necessary here...
      if (MPORUtil.isSelfReachable(entry, exit.map(node -> node), new ArrayList<>(), entry)) {
        throw new UnsupportedCodeException(
            String.format(
                InputRejectionMessage.RECURSIVE_FUNCTION.formatMessage(),
                entry.getFunction().getFileLocation().getStartingLineInOrigin(),
                entry.getFunctionName()),
            null);
      }
    }
  }

  /** Public, because checking is done in {@link MPORSubstitution}. */
  public static void checkPointerWrite(
      boolean pIsWrite, MPOROptions pOptions, CIdExpression pWrittenVariable)
      throws UnsupportedCodeException {

    if (pIsWrite) {
      if (!pOptions.allowPointerWrites()) {
        if (pWrittenVariable.getExpressionType() instanceof CPointerType) {
          throw new UnsupportedCodeException(
              String.format(
                  InputRejectionMessage.POINTER_WRITE.formatMessage(),
                  pWrittenVariable.getFileLocation().getStartingLineInOrigin(),
                  pWrittenVariable.toASTString()),
              null);
        }
      }
    }
  }

  public static void checkFunctionPointerInAssignment(CSimpleDeclaration pRightHandSide)
      throws UnsupportedCodeException {

    if (pRightHandSide instanceof CFunctionDeclaration) {
      throw new UnsupportedCodeException(
          InputRejectionMessage.FUNCTION_POINTER_IN_ASSIGNMENT.message
              + pRightHandSide.toASTString(),
          null);
    }
  }

  public static void checkFunctionPointerParameter(CFunctionCallExpression pFunctionCallExpression)
      throws UnsupportedCodeException {

    for (CExpression parameterExpression : pFunctionCallExpression.getParameterExpressions()) {
      checkFunctionPointerParameter(parameterExpression, pFunctionCallExpression);
      if (parameterExpression instanceof CUnaryExpression unaryExpression) {
        checkFunctionPointerParameter(unaryExpression.getOperand(), pFunctionCallExpression);
      }
    }
  }

  private static void checkFunctionPointerParameter(
      CExpression pParameterExpression, CFunctionCallExpression pFunctionCallExpression)
      throws UnsupportedCodeException {

    if (pParameterExpression instanceof CIdExpression idExpression) {
      if (idExpression.getDeclaration() instanceof CFunctionDeclaration) {
        throw new UnsupportedCodeException(
            InputRejectionMessage.FUNCTION_POINTER_PARAMETER.message
                + pFunctionCallExpression.toASTString(),
            null);
      }
    }
  }
}
