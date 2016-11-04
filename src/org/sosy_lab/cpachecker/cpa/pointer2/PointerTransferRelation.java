/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.pointer2;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
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
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
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
    return resultState == null ? Collections.<AbstractState>emptySet() : Collections.<AbstractState>singleton(resultState);
  }

  private PointerState getAbstractSuccessor(PointerState pState, CFAEdge pCfaEdge)
      throws CPATransferException {

    PointerState resultState = pState;
    switch (pCfaEdge.getEdgeType()) {
    case AssumeEdge:
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
      break;
    case ReturnStatementEdge:
      resultState = handleReturnStatementEdge(pState, (CReturnStatementEdge) pCfaEdge);
      break;
    case StatementEdge:
      resultState = handleStatementEdge(pState, (CStatementEdge) pCfaEdge);
      break;
    default:
      throw new UnrecognizedCCodeException("Unrecognized CFA edge.", pCfaEdge);
    }
    return resultState;
  }

  private PointerState handleReturnStatementEdge(PointerState pState, CReturnStatementEdge pCfaEdge) throws UnrecognizedCCodeException {
    if (!pCfaEdge.getExpression().isPresent()) {
      return pState;
    }
    Optional<? extends AVariableDeclaration> returnVariable = pCfaEdge.getSuccessor().getEntryNode().getReturnVariable();
    if (!returnVariable.isPresent()) {
      return pState;
    }
    return handleAssignment(pState,
        MemoryLocation.valueOf(returnVariable.get().getQualifiedName()),
        pCfaEdge.getExpression().get());
  }

  private PointerState handleFunctionCallEdge(PointerState pState, CFunctionCallEdge pCFunctionCallEdge) throws UnrecognizedCCodeException {
    PointerState newState = pState;
    List<CParameterDeclaration> formalParams = pCFunctionCallEdge.getSuccessor().getFunctionParameters();
    List<CExpression> actualParams = pCFunctionCallEdge.getArguments();
    int limit = Math.min(formalParams.size(), actualParams.size());
    formalParams = FluentIterable.from(formalParams).limit(limit).toList();
    actualParams = FluentIterable.from(actualParams).limit(limit).toList();

    // Handle the mapping of arguments to formal parameters
    for (Pair<CParameterDeclaration, CExpression> param : Pair.zipList(formalParams, actualParams)) {
      CExpression actualParam = param.getSecond();
      CParameterDeclaration formalParam = param.getFirst();
      MemoryLocation location = toLocation(formalParam);
      newState = handleAssignment(pState, location, asLocations(actualParam, pState, 1));
    }

    // Handle remaining formal parameters where no actual argument was provided
    for (CParameterDeclaration formalParam : FluentIterable.from(formalParams).skip(limit)) {
      MemoryLocation location = toLocation(formalParam);
      newState = handleAssignment(pState, location, LocationSetBot.INSTANCE);
    }

    return newState;
  }

  private MemoryLocation toLocation(AbstractSimpleDeclaration pDeclaration) {
    return toLocation(pDeclaration.getType(), pDeclaration.getQualifiedName());
  }

  private MemoryLocation toLocation(
      CCompositeType pParent, CCompositeTypeMemberDeclaration pMemberDecl) {
    Type memberType = pMemberDecl.getType();
    if (memberType instanceof CType) {
      memberType = ((CType) memberType).getCanonicalType();
    }
    if (memberType instanceof CCompositeType) {
      return toLocation(pMemberDecl.getType(), pMemberDecl.getName());
    }
    return fieldReferenceToMemoryLocation(pParent, false, pMemberDecl.getName()).get();
  }

  private MemoryLocation toLocation(Type pType, String name) {
    Type type = pType;
    if (type instanceof CType) {
      type = ((CType) type).getCanonicalType();
    }
    if (type.toString().startsWith("struct ") || type.toString().startsWith("union ")) {
      return MemoryLocation.valueOf(type.toString()); // TODO find a better way to handle this
    }
    return MemoryLocation.valueOf(name);
  }

  private PointerState handleStatementEdge(PointerState pState, CStatementEdge pCfaEdge) throws UnrecognizedCCodeException {
    if (pCfaEdge.getStatement() instanceof CAssignment) {
      CAssignment assignment = (CAssignment) pCfaEdge.getStatement();
      return handleAssignment(pState, assignment.getLeftHandSide(), assignment.getRightHandSide());
    }
    return pState;
  }

  private PointerState handleAssignment(PointerState pState, CExpression pLeftHandSide, CRightHandSide pRightHandSide) throws UnrecognizedCCodeException {
    LocationSet locations = asLocations(pLeftHandSide, pState, 0);
    return handleAssignment(pState, locations, pRightHandSide);
  }

  private PointerState handleAssignment(PointerState pState, LocationSet pLocationSet, CRightHandSide pRightHandSide) throws UnrecognizedCCodeException {
    final Iterable<MemoryLocation> locations;
    if (pLocationSet.isTop()) {
      locations = pState.getKnownLocations();
    } else if (pLocationSet instanceof ExplicitLocationSet) {
      locations = (ExplicitLocationSet) pLocationSet;
    } else {
      locations = Collections.<MemoryLocation>emptySet();
    }
    PointerState result = pState;
    for (MemoryLocation location : locations) {
      result = handleAssignment(result, location, pRightHandSide);
    }
    return result;
  }

  private PointerState handleAssignment(PointerState pState, MemoryLocation pLhsLocation, CRightHandSide pRightHandSide) throws UnrecognizedCCodeException {
    return pState.addPointsToInformation(pLhsLocation, asLocations(pRightHandSide, pState, 1));
  }

  private PointerState handleAssignment(PointerState pState, MemoryLocation pLeftHandSide, LocationSet pRightHandSide) {
    return pState.addPointsToInformation(pLeftHandSide, pRightHandSide);
  }

  private PointerState handleDeclarationEdge(final PointerState pState, final CDeclarationEdge pCfaEdge) throws UnrecognizedCCodeException {
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
      throws UnrecognizedCCodeException {
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
            new CInitializerVisitor<LocationSet, UnrecognizedCCodeException>() {

              @Override
              public LocationSet visit(CInitializerExpression pInitializerExpression)
                  throws UnrecognizedCCodeException {
                return asLocations(pInitializerExpression.getExpression(), pState, 1);
              }

              @Override
              public LocationSet visit(CInitializerList pInitializerList)
                  throws UnrecognizedCCodeException {
                return LocationSetTop.INSTANCE;
              }

              @Override
              public LocationSet visit(CDesignatedInitializer pCStructInitializerPart)
                  throws UnrecognizedCCodeException {
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

  public static Optional<MemoryLocation> fieldReferenceToMemoryLocation(
      CFieldReference pFieldReference) {
    return fieldReferenceToMemoryLocation(
        pFieldReference.getFieldOwner().getExpressionType(),
        pFieldReference.isPointerDereference(),
        pFieldReference.getFieldName());
  }

  private static Optional<MemoryLocation> fieldReferenceToMemoryLocation(
      CType pFieldOwnerType, boolean pIsPointerDeref, String pFieldName) {
    Type type = pFieldOwnerType.getCanonicalType();
    final String prefix;
    if (pIsPointerDeref && type instanceof CType) {
      if (!(type instanceof CPointerType)) {
        assert false;
        return Optional.empty();
      }
      final CType innerType = ((CPointerType) type).getType().getCanonicalType();
      if (innerType instanceof CPointerType) {
        assert false;
        return Optional.empty();
      }
      prefix = innerType.toString();
    } else {
      prefix = type.toString();
    }
    String infix = ".";
    String suffix = pFieldName;
    // TODO use offsets instead
    return Optional.of(MemoryLocation.valueOf(prefix + infix + suffix));
  }

  private static LocationSet asLocations(final CRightHandSide pExpression, final PointerState pState, final int pDerefCounter) throws UnrecognizedCCodeException {
    return pExpression.accept(
        new CRightHandSideVisitor<LocationSet, UnrecognizedCCodeException>() {

          @Override
          public LocationSet visit(CArraySubscriptExpression pIastArraySubscriptExpression)
              throws UnrecognizedCCodeException {
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
                    for (MemoryLocation loc : pState.getKnownLocations()) {
                      result.add(loc);
                    }
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
              throws UnrecognizedCCodeException {
            Optional<MemoryLocation> memoryLocation =
                fieldReferenceToMemoryLocation(pIastFieldReference);
            if (!memoryLocation.isPresent()) {
              return LocationSetTop.INSTANCE;
            }
            return toLocationSet(Collections.singleton(memoryLocation.get()));
          }

          @Override
          public LocationSet visit(CIdExpression pIastIdExpression)
              throws UnrecognizedCCodeException {
            Type type = pIastIdExpression.getExpressionType();
            final MemoryLocation location;
            if (type.toString().startsWith("struct ") || type.toString().startsWith("union ")) {
              location =
                  MemoryLocation.valueOf(type.toString()); // TODO find a better way to handle this
            } else {
              CSimpleDeclaration declaration = pIastIdExpression.getDeclaration();
              if (declaration != null) {
                location = MemoryLocation.valueOf(declaration.getQualifiedName());
              } else {
                location = MemoryLocation.valueOf(pIastIdExpression.getName());
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
              throws UnrecognizedCCodeException {
            return asLocations(pPointerExpression.getOperand(), pState, pDerefCounter + 1);
          }

          @Override
          public LocationSet visit(CComplexCastExpression pComplexCastExpression)
              throws UnrecognizedCCodeException {
            return asLocations(pComplexCastExpression.getOperand(), pState, pDerefCounter);
          }

          @Override
          public LocationSet visit(CBinaryExpression pIastBinaryExpression)
              throws UnrecognizedCCodeException {
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
              throws UnrecognizedCCodeException {
            return asLocations(pIastCastExpression.getOperand(), pState, pDerefCounter);
          }

          @Override
          public LocationSet visit(CCharLiteralExpression pIastCharLiteralExpression)
              throws UnrecognizedCCodeException {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CFloatLiteralExpression pIastFloatLiteralExpression)
              throws UnrecognizedCCodeException {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
              throws UnrecognizedCCodeException {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CStringLiteralExpression pIastStringLiteralExpression)
              throws UnrecognizedCCodeException {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CTypeIdExpression pIastTypeIdExpression)
              throws UnrecognizedCCodeException {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CUnaryExpression pIastUnaryExpression)
              throws UnrecognizedCCodeException {
            if (pDerefCounter > 0 && pIastUnaryExpression.getOperator() == UnaryOperator.AMPER) {
              return asLocations(pIastUnaryExpression.getOperand(), pState, pDerefCounter - 1);
            }
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CImaginaryLiteralExpression PIastLiteralExpression)
              throws UnrecognizedCCodeException {
            return LocationSetBot.INSTANCE;
          }

          @Override
          public LocationSet visit(CFunctionCallExpression pIastFunctionCallExpression)
              throws UnrecognizedCCodeException {
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
            return visit(MemoryLocation.valueOf(declaration.getQualifiedName()));
          }

          @Override
          public LocationSet visit(CAddressOfLabelExpression pAddressOfLabelExpression)
              throws UnrecognizedCCodeException {
            throw new UnrecognizedCCodeException(
                "Address of labels not supported by pointer analysis", pAddressOfLabelExpression);
          }
        });
  }

  /**
   * Gets the set of possible locations of the given expression. For the expression 'x', the
   * location is the identifier x. For the expression 's.a' the location is the identifier
   * t.a, where t is the type of s. For the expression '*p', the possible locations are the
   * points-to set of locations the expression 'p'.
   */
  public static LocationSet asLocations(CExpression pExpression, final PointerState pState) throws UnrecognizedCCodeException {
    return asLocations(pExpression, pState, 0);
  }

  /**
   * Gets the locations represented by the given location set considering the
   * context of the given state. The returned iterable is guaranteed to be free
   * of duplicates.
   *
   * @param pState the context.
   * @param pLocationSet the location set.
   *
   * @return the locations represented by the given location set.
   */
  public static Iterable<MemoryLocation> toNormalSet(PointerState pState, LocationSet pLocationSet) {
    if (pLocationSet.isBot()) {
      return Collections.emptySet();
    }
    if (pLocationSet.isTop() || !(pLocationSet instanceof ExplicitLocationSet)) {
      return pState.getKnownLocations();
    }
    return (ExplicitLocationSet) pLocationSet;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      List<AbstractState> pOtherStates,
      @Nullable CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    if (pCfaEdge != null) {
      Optional<AFunctionCall> functionCall = asFunctionCall(pCfaEdge);
      if (functionCall.isPresent()) {
        AFunctionCallExpression functionCallExpression =
            functionCall.get().getFunctionCallExpression();
        AExpression functionNameExpression = functionCallExpression.getFunctionNameExpression();
        if (functionNameExpression instanceof CPointerExpression) {
          CExpression derefNameExpr = ((CPointerExpression) functionNameExpression).getOperand();
          if (derefNameExpr instanceof CFieldReference) {
            CFieldReference fieldReference = (CFieldReference) derefNameExpr;
            Optional<CallstackState> callstackState = find(pOtherStates, CallstackState.class);
            if (callstackState.isPresent()) {
              return strengthenFieldReference(
                  (PointerState) pState, callstackState.get(), fieldReference);
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
    Optional<MemoryLocation> memoryLocation =
        PointerTransferRelation.fieldReferenceToMemoryLocation(pFieldReference);
    if (!memoryLocation.isPresent()) {
      return Collections.singleton(pPointerState);
    }
    LocationSet targets = pPointerState.getPointsToSet(memoryLocation.get());
    if (targets instanceof ExplicitLocationSet) {
      ExplicitLocationSet explicitTargets = ((ExplicitLocationSet) targets);
      for (MemoryLocation target : explicitTargets) {
        if (target.getIdentifier().equals(pCallstackState.getCurrentFunction())) {
          return Collections.singleton(pPointerState);
        }
      }
      return Collections.emptySet();
    }
    return Collections.singleton(pPointerState);
  }

  private static <T> Optional<T> find(Iterable<? super T> pIterable, Class<T> pClass) {
    Object result = Iterables.find(pIterable, Predicates.instanceOf(pClass), null);
    if (result == null) {
      return Optional.empty();
    }
    return Optional.of(pClass.cast(result));
  }
}
