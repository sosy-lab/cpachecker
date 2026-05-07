// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class InputRejection {

  enum InputRejectionMessage {
    FUNCTION_POINTER_ASSIGNMENT("MPOR does not support function pointers in assignments: ", false),
    INVALID_OPTIONS("Invalid MPOR options, see above errors.", false),
    LANGUAGE_NOT_C("MPOR only supports language C", false),
    NOT_CONCURRENT(
        "MPOR expects concurrent C program with at least one pthread_create call", false),
    DUPLICATE_STRUCT_MEMBER_NAMES(
        "MPOR does not support non unique nested struct member names in line ", true),
    MULTIPLE_DECLARATIONS_IN_FIELD_REFERENCE_OWNER(
        "MPOR does not support multiple declarations in the owner expression of field references in"
            + " line ",
        true),
    NO_PTHREAD_OBJECT_ARRAYS(
        "MPOR does not support arrays of pthread objects or arrays of structs with inner pthread"
            + " objects in line ",
        true),
    POINTER_WRITE_BINARY_EXPRESSION(
        "MPOR does not support binary expressions as assignments to pointers in line ", true),
    POINTER_WRITE(
        "allowPointerWrites is disabled, but the input program contains a pointer write in line ",
        true),
    PTHREAD_CREATE_LOOP(
        "MPOR does not support pthread_create calls in loops (or recursive functions)", false),
    PTHREAD_OBJECT_POINTER_ALIASING("MPOR does not aliasing for pthread object pointers:", false),
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

  /** Handles input program rejections and throws a {@link UnsupportedCodeException} accordingly. */
  public static void handleRejections(CFA pInputCfa) throws UnsupportedCodeException {
    checkLanguageC(pInputCfa);
    checkIsParallelProgram(pInputCfa);
    checkUnsupportedFunctions(pInputCfa);
    checkFunctionPointerInitializer(pInputCfa);
    checkDuplicateStructMemberNames(pInputCfa);
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

  private static void checkDuplicateStructMemberNames(CFA pInputCfa)
      throws UnsupportedCodeException {

    ImmutableSet<String> stopNames = PthreadObjectType.getAllPthreadObjectTypeNames();
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pInputCfa)) {
      if (cfaEdge instanceof CDeclarationEdge declarationEdge) {
        if (declarationEdge.getDeclaration() instanceof CVariableDeclaration variableDeclaration) {
          ImmutableList<CCompositeTypeMemberDeclaration> memberDeclarations =
              SeqPointerAliasingUtil.getAllCompositeTypeMemberDeclarationsInType(
                  variableDeclaration.getType(), stopNames);
          Set<String> memberNames = new HashSet<>();
          for (CCompositeTypeMemberDeclaration memberDeclaration : memberDeclarations) {
            if (!memberNames.add(memberDeclaration.getName())) {
              rejectCfaEdge(declarationEdge, InputRejectionMessage.DUPLICATE_STRUCT_MEMBER_NAMES);
            }
          }
        }
      }
    }
  }

  private static void checkPthreadObjectArrays(CFA pInputCfa) throws UnsupportedCodeException {
    ImmutableSet<String> stopNames = PthreadObjectType.getAllPthreadObjectTypeNames();
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pInputCfa)) {
      if (cfaEdge instanceof CDeclarationEdge declarationEdge) {
        if (declarationEdge.getDeclaration() instanceof CVariableDeclaration variableDeclaration) {
          if (variableDeclaration.getType() instanceof CArrayType arrayType) {
            ImmutableSet<CType> nestedTypes =
                SeqPointerAliasingUtil.getAllTypesInType(arrayType, stopNames);
            if (nestedTypes.stream().anyMatch(t -> PthreadUtil.isAnyPthreadObjectType(t))) {
              rejectCfaEdge(cfaEdge, InputRejectionMessage.NO_PTHREAD_OBJECT_ARRAYS);
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

  private static void checkFunctionPointerInitializer(CFA pInputCfa)
      throws UnsupportedCodeException {

    for (CFAEdge cfaEdge : CFAUtils.allEdges(pInputCfa)) {
      if (cfaEdge instanceof CDeclarationEdge declarationEdge) {
        if (declarationEdge.getDeclaration() instanceof CVariableDeclaration variableDeclaration) {
          if (variableDeclaration.getInitializer()
              instanceof CInitializerExpression initializerExpression) {
            checkFunctionPointerRightHandSide(initializerExpression.getExpression());
          }
        }
      }
    }
  }

  public static void checkFunctionPointerRightHandSide(CExpression pRightHandSide)
      throws UnsupportedCodeException {

    ImmutableSet<CSimpleDeclaration> declarations =
        SeqPointerAliasingUtil.getAllSimpleDeclarationsInExpression(pRightHandSide, true);
    if (declarations.stream().anyMatch(d -> d instanceof CFunctionDeclaration)) {
      throw new UnsupportedCodeException(
          InputRejectionMessage.FUNCTION_POINTER_ASSIGNMENT.message + pRightHandSide.toASTString(),
          null);
    }
  }

  public static void checkFunctionPointerParameter(CFunctionCallExpression pFunctionCallExpression)
      throws UnsupportedCodeException {

    // calls to pthread functions with start_routine pointers are allowed
    if (PthreadUtil.isCallToAnyPthreadFunctionWithObjectType(
        pFunctionCallExpression, PthreadObjectType.START_ROUTINE)) {
      return;
    }
    for (CExpression parameterExpression : pFunctionCallExpression.getParameterExpressions()) {
      checkFunctionPointerRightHandSide(parameterExpression);
    }
  }

  public static void checkMultipleDeclarationsInFieldReferenceOwner(CFieldReference pFieldReference)
      throws UnsupportedCodeException {

    ImmutableSet<CSimpleDeclaration> fieldOwnerDeclarations =
        SeqPointerAliasingUtil.getAllSimpleDeclarationsInExpression(pFieldReference, false);
    if (fieldOwnerDeclarations.size() > 1) {
      throw new UnsupportedCodeException(
          String.format(
              InputRejectionMessage.MULTIPLE_DECLARATIONS_IN_FIELD_REFERENCE_OWNER.formatMessage(),
              pFieldReference.getFileLocation().getStartingLineInOrigin(),
              pFieldReference.toASTString()),
          null);
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

  public static void checkPointerWriteBinaryExpression(
      CLeftHandSide pLeftHandSide, CExpression pRightHandSide) throws UnsupportedCodeException {

    if (pLeftHandSide.getExpressionType() instanceof CPointerType) {
      if (pRightHandSide.accept(new CBinaryExpressionVisitor())) {
        throw new UnsupportedCodeException(
            String.format(
                InputRejectionMessage.POINTER_WRITE_BINARY_EXPRESSION.formatMessage(),
                pRightHandSide.getFileLocation().getStartingLineInOrigin(),
                pRightHandSide.toASTString()),
            null);
      }
    }
  }

  /**
   * Returns true if any of the nested expressions inside a given {@link CExpression} is a {@link
   * CBinaryExpression}.
   */
  private static final class CBinaryExpressionVisitor
      extends DefaultCExpressionVisitor<Boolean, UnsupportedCodeException> {

    @Override
    public Boolean visit(CArraySubscriptExpression pArraySubscriptExpression)
        throws UnsupportedCodeException {
      return pArraySubscriptExpression.getSubscriptExpression().accept(this);
    }

    @Override
    public Boolean visit(CFieldReference pFieldReference) throws UnsupportedCodeException {
      return pFieldReference.getFieldOwner().accept(this);
    }

    @Override
    public Boolean visit(CPointerExpression pPointerExpression) throws UnsupportedCodeException {
      return pPointerExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CComplexCastExpression pComplexCastExpression)
        throws UnsupportedCodeException {
      return pComplexCastExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CBinaryExpression pBinaryExpression) {
      return true;
    }

    @Override
    public Boolean visit(CCastExpression pCastExpression) throws UnsupportedCodeException {
      return pCastExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CUnaryExpression pUnaryExpression) throws UnsupportedCodeException {
      return pUnaryExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CIdExpression pIdExpression) {
      return false; // CIdExpressions are never CBinaryExpressions
    }

    @Override
    protected Boolean visitDefault(CExpression pExpression) {
      return false; // ignore
    }
  }
}
