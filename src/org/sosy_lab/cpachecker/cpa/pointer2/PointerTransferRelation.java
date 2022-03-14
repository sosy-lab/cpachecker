// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer2;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetBot;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetTop;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class PointerTransferRelation extends SingleEdgeTransferRelation {

  static final TransferRelation INSTANCE = new PointerTransferRelation();

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    PointerState pointerState = (PointerState) pState;
    PointerState resultState = getAbstractSuccessor(pointerState, pCfaEdge);
    return resultState == null
        ? ImmutableSet.of()
        : Collections.<AbstractState>singleton(resultState);
  }

  private PointerState getAbstractSuccessor(PointerState pState, CFAEdge pCfaEdge)
      throws CPATransferException {

    PointerState resultState = pState;
    switch (pCfaEdge.getEdgeType()) {
      case AssumeEdge:
        resultState = handleAssumeEdge(pState, (AssumeEdge) pCfaEdge);
        break;
      case BlankEdge:
        break;
      case CallToReturnEdge:
        break;
      case DeclarationEdge:
        resultState = handleDeclarationEdge(pState, (CDeclarationEdge) pCfaEdge);
        break;
      case FunctionCallEdge:
        resultState = handleFunctionCallEdge(pState, ((CFunctionCallEdge) pCfaEdge));
        break;
      case FunctionReturnEdge:
        resultState = handleFunctionReturnEdge(pState, ((CFunctionReturnEdge) pCfaEdge));
        break;
      case ReturnStatementEdge:
        resultState = handleReturnStatementEdge(pState, (CReturnStatementEdge) pCfaEdge);
        break;
      case StatementEdge:
        resultState = handleStatementEdge(pState, (CStatementEdge) pCfaEdge);
        break;
      default:
        throw new UnrecognizedCodeException("Unrecognized CFA edge.", pCfaEdge);
    }
    return resultState;
  }

  private PointerState handleFunctionReturnEdge(PointerState pState, CFunctionReturnEdge pCfaEdge)
      throws UnrecognizedCodeException {
    CFunctionSummaryEdge summaryEdge = pCfaEdge.getSummaryEdge();
    CFunctionCall callEdge = summaryEdge.getExpression();

    if (callEdge instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement callAssignment = (CFunctionCallAssignmentStatement) callEdge;
      Optional<MemoryLocation> returnVar =
          getFunctionReturnVariable(summaryEdge.getFunctionEntry());

      assert returnVar.isPresent()
          : "Return edge with assignment, but no return variable: " + summaryEdge;

      LocationSet pointedTo = pState.getPointsToSet(returnVar.orElseThrow());

      return handleAssignment(pState, callAssignment.getLeftHandSide(), pointedTo);
    } else {
      return pState;
    }
  }

  private PointerState handleAssumeEdge(PointerState pState, AssumeEdge pAssumeEdge)
      throws UnrecognizedCodeException {
    AExpression expression = pAssumeEdge.getExpression();
    if (expression instanceof ABinaryExpression) {
      ABinaryExpression binOp = (ABinaryExpression) expression;
      if (binOp.getOperator() == BinaryOperator.EQUALS) {
        Optional<Boolean> areEq = areEqual(pState, binOp.getOperand1(), binOp.getOperand2());
        if (areEq.isPresent() && areEq.orElseThrow() != pAssumeEdge.getTruthAssumption()) {
          return null;
        }
      }
    }
    return pState;
  }

  private Optional<Boolean> areEqual(
      PointerState pPointerState, AExpression pOperand1, AExpression pOperand2)
      throws UnrecognizedCodeException {
    if (pOperand1 instanceof CBinaryExpression) {
      CBinaryExpression op1 = (CBinaryExpression) pOperand1;
      if (op1.getOperator() == BinaryOperator.EQUALS) {
        if (pOperand2 instanceof CIntegerLiteralExpression) {
          CIntegerLiteralExpression op2 = (CIntegerLiteralExpression) pOperand2;
          if (op2.getValue().equals(BigInteger.ZERO)) {
            return negate(areEqual(pPointerState, op1.getOperand1(), op1.getOperand2()));
          }
        } else if (pOperand2 instanceof CCharLiteralExpression) {
          CCharLiteralExpression op2 = (CCharLiteralExpression) pOperand2;
          if (op2.getCharacter() == 0) {
            return negate(areEqual(pPointerState, op1.getOperand1(), op1.getOperand2()));
          }
        }
      }
      return Optional.empty();
    }
    if (pOperand1 instanceof CExpression && pOperand2 instanceof CExpression) {
      CExpression operand1 = (CExpression) pOperand1;
      CExpression operand2 = (CExpression) pOperand2;
      LocationSet op1LocationSet = asLocations(operand1, pPointerState);
      LocationSet op2LocationSet = asLocations(operand2, pPointerState);
      if (op1LocationSet instanceof ExplicitLocationSet
          && op2LocationSet instanceof ExplicitLocationSet) {
        if (op1LocationSet.equals(op2LocationSet)) {
          if (operand1 instanceof CIdExpression && operand2 instanceof CIdExpression) {
            return Optional.of(true);
          }
          if (operand1 instanceof CFieldReference && operand2 instanceof CFieldReference) {
            CFieldReference op1 = (CFieldReference) operand1;
            CFieldReference op2 = (CFieldReference) operand2;
            if (op1.isPointerDereference() == op2.isPointerDereference()) {
              return areEqual(pPointerState, op1.getFieldOwner(), op2.getFieldOwner());
            }
          }
        }
      }
      if (operand1 instanceof CUnaryExpression && op2LocationSet instanceof ExplicitLocationSet) {
        CUnaryExpression op1 = (CUnaryExpression) operand1;
        if (op1.getOperator() == UnaryOperator.AMPER) {
          return pointsTo(pPointerState, (ExplicitLocationSet) op2LocationSet, op1.getOperand());
        }
      }
      if (operand2 instanceof CUnaryExpression && op1LocationSet instanceof ExplicitLocationSet) {
        CUnaryExpression op2 = (CUnaryExpression) operand2;
        if (op2.getOperator() == UnaryOperator.AMPER) {
          return pointsTo(pPointerState, (ExplicitLocationSet) op1LocationSet, op2.getOperand());
        }
      }
    }
    return Optional.empty();
  }

  private static Optional<Boolean> pointsTo(
      PointerState pState, ExplicitLocationSet pLocations, CExpression pCandidateTarget)
      throws UnrecognizedCodeException {
    if (pLocations.getSize() == 1) {
      LocationSet candidateTargets = asLocations(pCandidateTarget, pState);
      if (candidateTargets instanceof ExplicitLocationSet) {
        ExplicitLocationSet explicitCandidateTargets = (ExplicitLocationSet) candidateTargets;
        MemoryLocation location = pLocations.iterator().next();
        LocationSet actualTargets = pState.getPointsToSet(location);
        if (actualTargets.isBot()) {
          return Optional.empty();
        }
        if (actualTargets instanceof ExplicitLocationSet && !explicitCandidateTargets.isBot()) {
          boolean containsAny = false;
          boolean containsAll = true;
          for (MemoryLocation candidateTarget : explicitCandidateTargets) {
            boolean contains = actualTargets.mayPointTo(candidateTarget);
            containsAny = containsAny || contains;
            containsAll = containsAll && contains;
          }
          if (!containsAny) {
            return Optional.of(false);
          }
          if (containsAll && ((ExplicitLocationSet) actualTargets).getSize() == 1) {
            if (isStructOrUnion(pCandidateTarget.getExpressionType())) {
              return Optional.empty();
            }
            return Optional.of(true);
          }
        }
      }
    }
    return Optional.empty();
  }

  private Optional<Boolean> negate(Optional<Boolean> pAreEqual) {
    return pAreEqual.map(b -> !b);
  }

  private PointerState handleReturnStatementEdge(PointerState pState, CReturnStatementEdge pCfaEdge)
      throws UnrecognizedCodeException {
    if (!pCfaEdge.getExpression().isPresent()) {
      return pState;
    }
    Optional<MemoryLocation> returnVariable =
        getFunctionReturnVariable(pCfaEdge.getSuccessor().getEntryNode());
    if (!returnVariable.isPresent()) {
      return pState;
    }
    return handleAssignment(
        pState, returnVariable.orElseThrow(), pCfaEdge.getExpression().orElseThrow());
  }

  private Optional<MemoryLocation> getFunctionReturnVariable(FunctionEntryNode pFunctionEntryNode) {
    Optional<? extends AVariableDeclaration> returnVariable =
        pFunctionEntryNode.getReturnVariable();
    if (!returnVariable.isPresent()) {
      return Optional.empty();
    } else {
      return Optional.of(MemoryLocation.forDeclaration(returnVariable.get()));
    }
  }

  private PointerState handleFunctionCallEdge(
      PointerState pState, CFunctionCallEdge pCFunctionCallEdge) throws UnrecognizedCodeException {
    PointerState newState = pState;
    List<CParameterDeclaration> formalParams =
        pCFunctionCallEdge.getSuccessor().getFunctionParameters();
    List<CExpression> actualParams = pCFunctionCallEdge.getArguments();
    int limit = Math.min(formalParams.size(), actualParams.size());
    formalParams = FluentIterable.from(formalParams).limit(limit).toList();
    actualParams = FluentIterable.from(actualParams).limit(limit).toList();

    // Handle the mapping of arguments to formal parameters
    for (Pair<CParameterDeclaration, CExpression> param :
        Pair.zipList(formalParams, actualParams)) {
      CExpression actualParam = param.getSecond();
      CParameterDeclaration formalParam = param.getFirst();
      MemoryLocation location = toLocation(formalParam);
      newState = handleAssignment(newState, location, asLocations(actualParam, pState, 1));
    }

    // Handle remaining formal parameters where no actual argument was provided
    for (CParameterDeclaration formalParam : FluentIterable.from(formalParams).skip(limit)) {
      MemoryLocation location = toLocation(formalParam);
      newState = handleAssignment(newState, location, LocationSetBot.INSTANCE);
    }

    return newState;
  }

  private MemoryLocation toLocation(AbstractSimpleDeclaration pDeclaration) {
    return toLocation(pDeclaration.getType(), pDeclaration.getQualifiedName());
  }

  private MemoryLocation toLocation(
      CCompositeType pParent, CCompositeTypeMemberDeclaration pMemberDecl) {
    CType memberType = pMemberDecl.getType().getCanonicalType();
    if (memberType instanceof CCompositeType) {
      return toLocation(pMemberDecl.getType(), pMemberDecl.getName());
    }
    return fieldReferenceToMemoryLocation(pParent, false, pMemberDecl.getName());
  }

  private MemoryLocation toLocation(Type pType, String name) {
    Type type = pType;
    if (type instanceof CType) {
      type = ((CType) type).getCanonicalType();
    }
    if (isStructOrUnion(type)) {
      return MemoryLocation.parseExtendedQualifiedName(
          type.toString()); // TODO find a better way to handle this
    }
    return MemoryLocation.parseExtendedQualifiedName(name);
  }

  private static boolean isStructOrUnion(Type pType) {
    Type type = pType instanceof CType ? ((CType) pType).getCanonicalType() : pType;
    if (type instanceof CComplexType) {
      return EnumSet.of(ComplexTypeKind.STRUCT, ComplexTypeKind.UNION)
          .contains(((CComplexType) type).getKind());
    }
    return false;
  }

  private PointerState handleStatementEdge(PointerState pState, CStatementEdge pCfaEdge)
      throws UnrecognizedCodeException {
    if (pCfaEdge.getStatement() instanceof CAssignment) {
      CAssignment assignment = (CAssignment) pCfaEdge.getStatement();

      if (assignment instanceof CFunctionCallAssignmentStatement) {
        // we don't consider summary edges, so if we encounter a function call assignment edge,
        // this means that the called function is not defined.
        // If the function returns a non-deterministic pointer,
        // handle it that way. Otherwise, assume that no existing variable is pointed to.
        if (isNondetPointerReturn(
            ((CFunctionCallAssignmentStatement) assignment)
                .getFunctionCallExpression()
                .getFunctionNameExpression())) {
          return handleAssignment(pState, assignment.getLeftHandSide(), LocationSetTop.INSTANCE);
        } else {
          return pState;
        }

      } else {
        return handleAssignment(
            pState, assignment.getLeftHandSide(), assignment.getRightHandSide());
      }
    }
    return pState;
  }

  private boolean isNondetPointerReturn(CExpression pFunctionNameExpression) {
    if (pFunctionNameExpression instanceof CIdExpression) {
      String functionName = ((CIdExpression) pFunctionNameExpression).getName();
      return functionName.equals("__VERIFIER_nondet_pointer");
    } else {
      return false;
    }
  }

  private PointerState handleAssignment(
      PointerState pState, CExpression pLeftHandSide, CRightHandSide pRightHandSide)
      throws UnrecognizedCodeException {
    return handleAssignment(pState, pLeftHandSide, asLocations(pRightHandSide, pState, 1));
  }

  private PointerState handleAssignment(
      PointerState pState, CExpression pLeftHandSide, LocationSet pRightHandSide)
      throws UnrecognizedCodeException {

    LocationSet locationSet = asLocations(pLeftHandSide, pState);
    final Iterable<MemoryLocation> locations;
    if (locationSet.isTop()) {
      locations = pState.getKnownLocations();
    } else if (locationSet instanceof ExplicitLocationSet) {
      locations = (ExplicitLocationSet) locationSet;
    } else {
      locations = ImmutableSet.of();
    }
    PointerState result = pState;
    for (MemoryLocation location : locations) {
      result = handleAssignment(result, location, pRightHandSide);
    }
    return result;
  }

  private PointerState handleAssignment(
      PointerState pState, MemoryLocation pLhsLocation, CRightHandSide pRightHandSide)
      throws UnrecognizedCodeException {
    return handleAssignment(pState, pLhsLocation, asLocations(pRightHandSide, pState, 1));
  }

  private PointerState handleAssignment(
      PointerState pState, MemoryLocation pLeftHandSide, LocationSet pRightHandSide) {
    return pState.addPointsToInformation(pLeftHandSide, pRightHandSide);
  }

  private PointerState handleDeclarationEdge(
      final PointerState pState, final CDeclarationEdge pCfaEdge) throws UnrecognizedCodeException {
    if (!(pCfaEdge.getDeclaration() instanceof CVariableDeclaration)) {
      return pState;
    }
    CVariableDeclaration declaration = (CVariableDeclaration) pCfaEdge.getDeclaration();
    CInitializer initializer = declaration.getInitializer();
    if (initializer != null) {
      MemoryLocation location = toLocation(declaration);
      return handleWithInitializer(pState, location, declaration.getType(), initializer);
    }
    return pState;
  }

  private PointerState handleWithInitializer(
      PointerState pState, MemoryLocation pLeftHandSide, CType pType, CInitializer pInitializer)
      throws UnrecognizedCodeException {
    if (pInitializer instanceof CInitializerList
        && pType.getCanonicalType() instanceof CCompositeType) {
      CCompositeType compositeType = (CCompositeType) pType.getCanonicalType();
      if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
        CInitializerList initializerList = (CInitializerList) pInitializer;
        Iterator<CCompositeTypeMemberDeclaration> memberDecls =
            compositeType.getMembers().iterator();
        Iterator<CInitializer> initializers = initializerList.getInitializers().iterator();
        PointerState current = pState;
        while (memberDecls.hasNext() && initializers.hasNext()) {
          CCompositeTypeMemberDeclaration memberDecl = memberDecls.next();
          CInitializer initializer = initializers.next();
          if (initializer != null) {
            current =
                handleWithInitializer(
                    current,
                    toLocation(compositeType, memberDecl),
                    memberDecl.getType(),
                    initializer);
          }
        }
        return current;
      }
    }
    LocationSet rhs =
        pInitializer.accept(
            new CInitializerVisitor<LocationSet, UnrecognizedCodeException>() {

              @Override
              public LocationSet visit(CInitializerExpression pInitializerExpression)
                  throws UnrecognizedCodeException {
                return asLocations(pInitializerExpression.getExpression(), pState, 1);
              }

              @Override
              public LocationSet visit(CInitializerList pInitializerList)
                  throws UnrecognizedCodeException {
                return LocationSetTop.INSTANCE;
              }

              @Override
              public LocationSet visit(CDesignatedInitializer pCStructInitializerPart)
                  throws UnrecognizedCodeException {
                return LocationSetTop.INSTANCE;
              }
            });

    return handleAssignment(pState, pLeftHandSide, rhs);
  }

  private static LocationSet toLocationSet(Iterable<? extends MemoryLocation> pLocations) {
    if (pLocations == null) {
      return LocationSetTop.INSTANCE;
    }
    Iterator<? extends MemoryLocation> locationIterator = pLocations.iterator();
    if (!locationIterator.hasNext()) {
      return LocationSetBot.INSTANCE;
    }
    return ExplicitLocationSet.from(pLocations);
  }

  public static MemoryLocation fieldReferenceToMemoryLocation(CFieldReference pFieldReference) {
    return fieldReferenceToMemoryLocation(
        pFieldReference.getFieldOwner().getExpressionType(),
        pFieldReference.isPointerDereference(),
        pFieldReference.getFieldName());
  }

  private static MemoryLocation fieldReferenceToMemoryLocation(
      CType pFieldOwnerType, boolean pIsPointerDeref, String pFieldName) {
    CType type = pFieldOwnerType.getCanonicalType();
    final String prefix;
    if (pIsPointerDeref) {
      if (!(type instanceof CPointerType)) {
        throw new AssertionError();
      }
      final CType innerType = ((CPointerType) type).getType().getCanonicalType();
      if (innerType instanceof CPointerType) {
        throw new AssertionError();
      }
      prefix = innerType.toString();
    } else {
      prefix = type.toString();
    }
    String infix = ".";
    String suffix = pFieldName;
    // TODO use offsets instead
    return MemoryLocation.parseExtendedQualifiedName(prefix + infix + suffix);
  }

  private static LocationSet asLocations(
      final CRightHandSide pExpression, final PointerState pState, final int pDerefCounter)
      throws UnrecognizedCodeException {
    return pExpression.accept(
        new CRightHandSideVisitor<LocationSet, UnrecognizedCodeException>() {

          @Override
          public LocationSet visit(CArraySubscriptExpression pIastArraySubscriptExpression)
              throws UnrecognizedCodeException {
            if (pIastArraySubscriptExpression.getSubscriptExpression()
                instanceof CLiteralExpression) {
              CLiteralExpression literal =
                  (CLiteralExpression) pIastArraySubscriptExpression.getSubscriptExpression();
              if (literal instanceof CIntegerLiteralExpression
                  && ((CIntegerLiteralExpression) literal).getValue().equals(BigInteger.ZERO)) {
                LocationSet starredLocations =
                    asLocations(
                        pIastArraySubscriptExpression.getArrayExpression(), pState, pDerefCounter);
                if (starredLocations.isBot() || starredLocations.isTop()) {
                  return starredLocations;
                }
                if (!(starredLocations instanceof ExplicitLocationSet)) {
                  return LocationSetTop.INSTANCE;
                }
                Set<MemoryLocation> result = new HashSet<>();
                for (MemoryLocation location : ((ExplicitLocationSet) starredLocations)) {
                  LocationSet pointsToSet = pState.getPointsToSet(location);
                  if (pointsToSet.isTop()) {
                    result.addAll(pState.getKnownLocations());
                    break;
                  } else if (!pointsToSet.isBot() && pointsToSet instanceof ExplicitLocationSet) {
                    ExplicitLocationSet explicitLocationSet = (ExplicitLocationSet) pointsToSet;
                    Iterables.addAll(result, explicitLocationSet);
                  }
                }
                return toLocationSet(result);
              }
            }
            return LocationSetTop.INSTANCE;
          }

          @Override
          public LocationSet visit(final CFieldReference pIastFieldReference)
              throws UnrecognizedCodeException {
            MemoryLocation memoryLocation = fieldReferenceToMemoryLocation(pIastFieldReference);
            return toLocationSet(Collections.singleton(memoryLocation));
          }

          @Override
          public LocationSet visit(CIdExpression pIastIdExpression)
              throws UnrecognizedCodeException {
            Type type = pIastIdExpression.getExpressionType();
            final MemoryLocation location;
            if (isStructOrUnion(type)) {
              location =
                  MemoryLocation.parseExtendedQualifiedName(
                      type.toString()); // TODO find a better way to handle this
            } else {
              CSimpleDeclaration declaration = pIastIdExpression.getDeclaration();
              if (declaration != null) {
                location = MemoryLocation.forDeclaration(declaration);
              } else {
                location = MemoryLocation.forIdentifier(pIastIdExpression.getName());
              }
            }
            return visit(location);
          }

          private LocationSet visit(MemoryLocation pLocation) {
            Collection<MemoryLocation> result = Collections.singleton(pLocation);
            for (int deref = pDerefCounter; deref > 0 && !result.isEmpty(); --deref) {
              Collection<MemoryLocation> newResult = new HashSet<>();
              for (MemoryLocation location : result) {
                LocationSet targets = pState.getPointsToSet(location);
                if (targets.isTop() || targets.isBot()) {
                  return targets;
                }
                if (!(targets instanceof ExplicitLocationSet)) {
                  return LocationSetTop.INSTANCE;
                }
                Iterables.addAll(newResult, ((ExplicitLocationSet) targets));
              }
              result = newResult;
            }
            return toLocationSet(result);
          }

          @Override
          public LocationSet visit(CPointerExpression pPointerExpression)
              throws UnrecognizedCodeException {
            return asLocations(pPointerExpression.getOperand(), pState, pDerefCounter + 1);
          }

          @Override
          public LocationSet visit(CComplexCastExpression pComplexCastExpression)
              throws UnrecognizedCodeException {
            return asLocations(pComplexCastExpression.getOperand(), pState, pDerefCounter);
          }

          @Override
          public LocationSet visit(CBinaryExpression pIastBinaryExpression)
              throws UnrecognizedCodeException {
            return toLocationSet(
                Iterables.concat(
                    toNormalSet(
                        pState,
                        asLocations(pIastBinaryExpression.getOperand1(), pState, pDerefCounter)),
                    toNormalSet(
                        pState,
                        asLocations(pIastBinaryExpression.getOperand2(), pState, pDerefCounter))));
          }

          @Override
          public LocationSet visit(CCastExpression pIastCastExpression)
              throws UnrecognizedCodeException {
            return asLocations(pIastCastExpression.getOperand(), pState, pDerefCounter);
          }

          @Override
          public LocationSet visit(CCharLiteralExpression pIastCharLiteralExpression) {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CFloatLiteralExpression pIastFloatLiteralExpression) {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CStringLiteralExpression pIastStringLiteralExpression) {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CTypeIdExpression pIastTypeIdExpression) {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CUnaryExpression pIastUnaryExpression)
              throws UnrecognizedCodeException {
            if (pDerefCounter > 0 && pIastUnaryExpression.getOperator() == UnaryOperator.AMPER) {
              return asLocations(pIastUnaryExpression.getOperand(), pState, pDerefCounter - 1);
            }
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CImaginaryLiteralExpression PIastLiteralExpression) {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CFunctionCallExpression pIastFunctionCallExpression)
              throws UnrecognizedCodeException {
            CFunctionDeclaration declaration = pIastFunctionCallExpression.getDeclaration();
            if (declaration == null) {
              LocationSet result =
                  pIastFunctionCallExpression.getFunctionNameExpression().accept(this);
              if (result.isTop() || result.isBot()) {
                return result;
              }
              return toLocationSet(
                  FluentIterable.from(toNormalSet(pState, result)).filter(Predicates.notNull()));
            }
            return visit(MemoryLocation.forDeclaration(declaration));
          }

          @Override
          public LocationSet visit(CAddressOfLabelExpression pAddressOfLabelExpression)
              throws UnrecognizedCodeException {
            throw new UnrecognizedCodeException(
                "Address of labels not supported by pointer analysis", pAddressOfLabelExpression);
          }
        });
  }

  /**
   * Gets the set of possible locations of the given expression. For the expression 'x', the
   * location is the identifier x. For the expression 's.a' the location is the identifier t.a,
   * where t is the type of s. For the expression '*p', the possible locations are the points-to set
   * of locations the expression 'p'.
   */
  public static LocationSet asLocations(CExpression pExpression, final PointerState pState)
      throws UnrecognizedCodeException {
    return asLocations(pExpression, pState, 0);
  }

  /**
   * Gets the locations represented by the given location set considering the context of the given
   * state. The returned iterable is guaranteed to be free of duplicates.
   *
   * @param pState the context.
   * @param pLocationSet the location set.
   * @return the locations represented by the given location set.
   */
  public static Iterable<MemoryLocation> toNormalSet(
      PointerState pState, LocationSet pLocationSet) {
    if (pLocationSet.isBot()) {
      return ImmutableSet.of();
    }
    if (pLocationSet.isTop() || !(pLocationSet instanceof ExplicitLocationSet)) {
      return pState.getKnownLocations();
    }
    return (ExplicitLocationSet) pLocationSet;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      @Nullable CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    if (pCfaEdge != null) {
      Optional<AFunctionCall> functionCall = asFunctionCall(pCfaEdge);
      if (functionCall.isPresent()) {
        AFunctionCallExpression functionCallExpression =
            functionCall.orElseThrow().getFunctionCallExpression();
        AExpression functionNameExpression = functionCallExpression.getFunctionNameExpression();
        if (functionNameExpression instanceof CPointerExpression) {
          CExpression derefNameExpr = ((CPointerExpression) functionNameExpression).getOperand();
          if (derefNameExpr instanceof CFieldReference) {
            CFieldReference fieldReference = (CFieldReference) derefNameExpr;
            Optional<CallstackState> callstackState = find(pOtherStates, CallstackState.class);
            if (callstackState.isPresent()) {
              return strengthenFieldReference(
                  (PointerState) pState, callstackState.orElseThrow(), fieldReference);
            }
          }
        }
      }
    }
    return super.strengthen(pState, pOtherStates, pCfaEdge, pPrecision);
  }

  private static Optional<AFunctionCall> asFunctionCall(CFAEdge pEdge) {
    if (pEdge instanceof AStatementEdge) {
      AStatementEdge statementEdge = (AStatementEdge) pEdge;
      if (statementEdge.getStatement() instanceof AFunctionCall) {
        return Optional.of((AFunctionCall) statementEdge.getStatement());
      }
    } else if (pEdge instanceof FunctionCallEdge) {
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) pEdge;
      return Optional.of(functionCallEdge.getSummaryEdge().getExpression());
    } else if (pEdge instanceof FunctionSummaryEdge) {
      FunctionSummaryEdge functionSummaryEdge = (FunctionSummaryEdge) pEdge;
      return Optional.of(functionSummaryEdge.getExpression());
    }
    return Optional.empty();
  }

  private static Collection<? extends AbstractState> strengthenFieldReference(
      PointerState pPointerState, CallstackState pCallstackState, CFieldReference pFieldReference) {
    MemoryLocation memoryLocation =
        PointerTransferRelation.fieldReferenceToMemoryLocation(pFieldReference);
    LocationSet targets = pPointerState.getPointsToSet(memoryLocation);
    if (targets instanceof ExplicitLocationSet) {
      ExplicitLocationSet explicitTargets = ((ExplicitLocationSet) targets);
      for (MemoryLocation target : explicitTargets) {
        if (target.getIdentifier().equals(pCallstackState.getCurrentFunction())) {
          return Collections.singleton(pPointerState);
        }
      }
      return ImmutableSet.of();
    }
    return Collections.singleton(pPointerState);
  }

  private static <T> Optional<T> find(Iterable<? super T> pIterable, Class<T> pClass) {
    return from(pIterable).filter(pClass).first().toJavaUtil();
  }
}
