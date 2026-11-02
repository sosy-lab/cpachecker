// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.input_checks;

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
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
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
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class InputRejection {

  /** Handles input program rejections and throws a {@link UnsupportedCodeException} accordingly. */
  public static void handleRejections(CFA pInputCfa) throws UnsupportedCodeException {
    checkLanguageC(pInputCfa);
    checkUnsupportedMachineModels(pInputCfa);
    checkUnsupportedFunctions(pInputCfa);
    checkFunctionPointerInInitializer(pInputCfa);
    checkDuplicateStructMemberNames(pInputCfa);
    checkPthreadObjectArrays(pInputCfa);
    checkPthreadFunctionReturnValues(pInputCfa);
    // these are recursive and can be expensive, so they are last
    checkPthreadCreateLoops(pInputCfa);
    checkRecursiveFunctions(pInputCfa);
  }

  private static void rejectCfaEdge(CFAEdge pCfaEdge, InputMessage pMessage)
      throws UnsupportedCodeException {

    throw new UnsupportedCodeException(
        pMessage.formatMessage(
            pCfaEdge.getFileLocation().getStartingLineInOrigin(), pCfaEdge.getCode()),
        pCfaEdge);
  }

  private static void checkLanguageC(CFA pInputCfa) throws UnsupportedCodeException {
    Language language = pInputCfa.getMetadata().getInputLanguage();
    if (!language.equals(Language.C)) {
      throw new UnsupportedCodeException(InputMessage.LANGUAGE_NOT_C.formatMessage(), null);
    }
  }

  private static void checkUnsupportedMachineModels(CFA pInputCfa) throws UnsupportedCodeException {
    switch (pInputCfa.getMachineModel()) {
      case LINUX32, LINUX64 -> {}
      case ARM, ARM64 ->
          throw new UnsupportedCodeException(
              InputMessage.UNSUPPORTED_MACHINE_MODEL.formatMessage(pInputCfa.getMachineModel()),
              null);
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
              rejectCfaEdge(declarationEdge, InputMessage.DUPLICATE_STRUCT_MEMBER_NAMES);
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
              rejectCfaEdge(cfaEdge, InputMessage.UNSUPPORTED_FUNCTION);
            }
          }
        }
      }
    }
  }

  private static void checkFunctionPointerInInitializer(CFA pInputCfa)
      throws UnsupportedCodeException {

    for (CFAEdge cfaEdge : CFAUtils.allEdges(pInputCfa)) {
      if (cfaEdge instanceof CDeclarationEdge declarationEdge) {
        if (declarationEdge.getDeclaration() instanceof CVariableDeclaration variableDeclaration) {
          if (variableDeclaration.getInitializer()
              instanceof CInitializerExpression initializerExpression) {
            checkFunctionPointerInRightHandSide(initializerExpression.getExpression());
          }
        }
      }
    }
  }

  public static void checkFunctionPointerInRightHandSide(CRightHandSide pRightHandSide)
      throws UnsupportedCodeException {

    ImmutableSet<CType> allTypes =
        SeqPointerAliasingUtil.getAllTypesInType(
            pRightHandSide.getExpressionType(), ImmutableSet.of());

    for (CType type : allTypes) {
      if (type instanceof CPointerType pointerType) {
        ImmutableSet<CType> innerPointerTypes =
            SeqPointerAliasingUtil.getAllTypesInType(pointerType, ImmutableSet.of());
        if (innerPointerTypes.stream().anyMatch(t -> t instanceof CFunctionType)) {
          throw new UnsupportedCodeException(
              InputMessage.FUNCTION_POINTER_ASSIGNMENT.formatMessage(pRightHandSide), null);
        }
      }
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
      checkFunctionPointerInRightHandSide(parameterExpression);
    }
  }

  public static void checkMultipleDeclarationsInFieldReferenceOwner(CFieldReference pFieldReference)
      throws UnsupportedCodeException {

    ImmutableSet<CSimpleDeclaration> fieldOwnerDeclarations =
        SeqPointerAliasingUtil.getAllSimpleDeclarationsInExpression(pFieldReference, false);
    if (fieldOwnerDeclarations.size() > 1) {
      throw new UnsupportedCodeException(
          String.format(
              InputMessage.MULTIPLE_DECLARATIONS_IN_FIELD_REFERENCE_OWNER.formatMessage(),
              pFieldReference.getFileLocation().getStartingLineInOrigin(),
              pFieldReference.toASTString()),
          null);
    }
  }

  private static void checkRecursiveFunctions(CFA pInputCfa) throws UnsupportedCodeException {
    for (FunctionEntryNode entry : pInputCfa.entryNodes()) {
      Optional<FunctionExitNode> exit = entry.getExitNode();
      // "upcasting" exit from FunctionExitNode to CFANode is necessary here...
      if (MPORUtil.isSelfReachable(entry, exit.map(node -> node), new ArrayList<>(), entry)) {
        throw new UnsupportedCodeException(
            InputMessage.RECURSIVE_FUNCTION.formatMessage(
                entry.getFunction().getFileLocation().getStartingLineInOrigin(),
                entry.getFunctionName()),
            null);
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
            rejectCfaEdge(cfaEdge, InputMessage.PTHREAD_CREATE_LOOP);
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
            rejectCfaEdge(cfaEdge, InputMessage.PTHREAD_FUNCTION_RETURN_VALUE);
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
            for (CType nestedType :
                SeqPointerAliasingUtil.getAllTypesInType(arrayType, stopNames)) {
              for (PthreadObjectType pthreadObjectType : PthreadObjectType.values()) {
                if (!pthreadObjectType.isArraySupported()) {
                  if (nestedType.toString().strip().equals(pthreadObjectType.getName())) {
                    rejectCfaEdge(cfaEdge, InputMessage.PTHREAD_OBJECT_ARRAY);
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Checks that {@code pFieldMember}, i.e. the member with name {@code pFieldName}, is part of
   * {@code pType}.
   */
  public static void checkFieldMemberFound(
      Optional<CCompositeTypeMemberDeclaration> pFieldMember, CType pType, String pFieldName)
      throws UnsupportedCodeException {

    if (pFieldMember.isEmpty()) {
      throw new UnsupportedCodeException(
          InputMessage.FIELD_MEMBER_NOT_FOUND.formatMessage(pFieldName, pType), null);
    }
  }

  /** Checks that the left-hand side of a pointer assignment is actually a pointer. */
  public static void checkPointerAssignmentLeftHandSide(SeqMemoryLocation pLeftHandSide)
      throws UnsupportedCodeException {

    if (pLeftHandSide.declaration() != null
        && !isPointerOrArrayOfPointersType(pLeftHandSide.declaration().getType())
        && (pLeftHandSide.fieldMember().isEmpty()
            || !isPointerOrArrayOfPointersType(
                pLeftHandSide.fieldMember().orElseThrow().getType()))) {
      throw new UnsupportedCodeException(
          InputMessage.POINTER_ASSIGNMENT_TO_NON_POINTER.formatMessage(describe(pLeftHandSide)),
          null);
    }
  }

  /**
   * Checks that {@code pPointerDereference} is actually a pointer and that the {@link CType} of
   * {@code pTargetMemoryLocation}, i.e. of the memory location it points to, is compatible.
   */
  public static void checkPointerDereferenceTypes(
      SeqMemoryLocation pPointerDereference, SeqMemoryLocation pTargetMemoryLocation)
      throws UnsupportedCodeException {

    if (pPointerDereference.declaration() != null
        && !isPointerOrArrayType(pPointerDereference.declaration().getType())
        && (pPointerDereference.fieldMember().isEmpty()
            || !isPointerOrArrayType(pPointerDereference.fieldMember().orElseThrow().getType()))) {
      throw new UnsupportedCodeException(
          InputMessage.POINTER_DEREFERENCE_OF_NON_POINTER.formatMessage(
              describe(pPointerDereference)),
          null);
    }

    // function call right-hand sides are not tracked, e.g. malloc always returns (void *)
    if (pTargetMemoryLocation.functionCallExpression().isPresent()) {
      return;
    }
    // start_routine target memory locations are always (void *)
    if (pTargetMemoryLocation.callContext().isStartRoutineCallContext()) {
      return;
    }
    CType dereferenceType = pPointerDereference.getUnwrappedType();
    CType targetType = pTargetMemoryLocation.getUnwrappedType();
    if (!areTypesCompatible(dereferenceType, targetType)) {
      throw new UnsupportedCodeException(
          InputMessage.POINTER_DEREFERENCE_TYPE_MISMATCH.formatMessage(
              describe(pPointerDereference),
              dereferenceType,
              describe(pTargetMemoryLocation),
              targetType),
          null);
    }
  }

  /**
   * Returns whether a pointer to {@code pTypeA} may point to a memory location of {@code pTypeB}.
   *
   * <p>{@code void} is compatible with any type because {@code void *} is the generic object
   * pointer that can be converted to and from any other object pointer type, e.g. in {@code int
   * *array = (int *) malloc(size);}.
   */
  private static boolean areTypesCompatible(CType pTypeA, CType pTypeB) {
    return pTypeA instanceof CVoidType
        || pTypeB instanceof CVoidType
        || pTypeA.equals(pTypeB)
        || pTypeA.canBeAssignedFrom(pTypeB);
  }

  private static boolean isPointerOrArrayType(CType pType) {
    CType canonicalType = pType.getCanonicalType();
    return canonicalType instanceof CPointerType || canonicalType instanceof CArrayType;
  }

  private static boolean isPointerOrArrayOfPointersType(CType pType) {
    CType canonicalType = pType.getCanonicalType();
    // CArrayType.getType() corresponds to the CType of the arrays elements
    return canonicalType instanceof CPointerType
        || (canonicalType instanceof CArrayType arrayType
            && arrayType.getType().getCanonicalType() instanceof CPointerType);
  }

  /** Returns the name of {@code pMemoryLocation} as used in the input program. */
  private static String describe(SeqMemoryLocation pMemoryLocation) {
    if (pMemoryLocation.declaration() == null) {
      return pMemoryLocation.getName();
    }
    StringBuilder name = new StringBuilder(pMemoryLocation.declaration().getName());
    pMemoryLocation.fieldMember().ifPresent(member -> name.append(".").append(member.getName()));
    return name.toString();
  }

  /** Public, because checking is done in {@link MPORSubstitution}. */
  public static void checkPointerWrite(
      boolean pIsWrite, MPOROptions pOptions, CIdExpression pWrittenVariable)
      throws UnsupportedCodeException {

    if (pIsWrite) {
      if (!pOptions.allowPointerWrites()) {
        if (pWrittenVariable.getExpressionType() instanceof CPointerType) {
          throw new UnsupportedCodeException(
              InputMessage.POINTER_WRITE.formatMessage(
                  pWrittenVariable.getFileLocation().getStartingLineInOrigin(),
                  pWrittenVariable.toASTString()),
              null);
        }
      }
    }
  }

  public static void checkBinaryExpressionInRightHandSide(CRightHandSide pRightHandSide)
      throws UnsupportedCodeException {

    if (pRightHandSide instanceof CExpression expression) {
      if (expression.accept(new CBinaryExpressionVisitor())) {
        throw new UnsupportedCodeException(
            InputMessage.POINTER_WRITE_BINARY_EXPRESSION.formatMessage(
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
