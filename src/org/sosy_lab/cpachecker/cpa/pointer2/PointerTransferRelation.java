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

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.Pair;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetBot;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetTop;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;


public enum PointerTransferRelation implements TransferRelation {

  INSTANCE;

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState pState, Precision pPrecision,
      CFAEdge pCfaEdge) throws CPATransferException, InterruptedException {
    PointerState pointerState = (PointerState) pState;
    PointerState resultState = getAbstractSuccessor(pointerState, pPrecision, pCfaEdge);
    return resultState == null ? Collections.<AbstractState>emptySet() : Collections.<AbstractState>singleton(resultState);
  }

  private PointerState getAbstractSuccessor(PointerState pState, Precision pPrecision,
      CFAEdge pCfaEdge) throws CPATransferException, InterruptedException {
    PointerState resultState = pState;
    switch (pCfaEdge.getEdgeType()) {
    case AssumeEdge:
      break;
    case BlankEdge:
      break;
    case CallToReturnEdge:
      break;
    case DeclarationEdge:
      resultState = handleDeclarationEdge(pState, pPrecision, (CDeclarationEdge) pCfaEdge);
      break;
    case FunctionCallEdge:
      resultState = handleFunctionCallEdge(pState, pPrecision, ((CFunctionCallEdge) pCfaEdge));
      break;
    case FunctionReturnEdge:
      break;
    case MultiEdge:
      for (CFAEdge edge : ((MultiEdge) pCfaEdge)) {
        resultState = getAbstractSuccessor(resultState, pPrecision, edge);
      }
      break;
    case ReturnStatementEdge:
      break;
    case StatementEdge:
      resultState = handleStatementEdge(pState, pPrecision, (CStatementEdge) pCfaEdge);
      break;
    default:
      throw new UnrecognizedCCodeException("Unrecognized CFA edge.", pCfaEdge);
    }
    return resultState;
  }

  private PointerState handleFunctionCallEdge(PointerState pState, Precision pPrecision,
      CFunctionCallEdge pCFunctionCallEdge) throws UnrecognizedCCodeException {
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

      Type type = formalParam.getType();
      final String location;
      if (type.toString().startsWith("struct ") || type.toString().startsWith("union ")) {
        location = type.toString();
      } else {
        location = formalParam.getQualifiedName();
      } {
        newState = handleAssignment(pState, pPrecision, location, asLocations(actualParam, pState, 1));
      }
    }

    // Handle remaining formal parameters where no actual argument was provided
    for (CParameterDeclaration formalParam : FluentIterable.from(formalParams).skip(limit)) {
      Type type = formalParam.getType();
      final String location;
      if (type.toString().startsWith("struct ") || type.toString().startsWith("union ")) {
        location = type.toString();
      } else {
        location = formalParam.getQualifiedName();
      }
      newState = handleAssignment(pState, pPrecision, location, LocationSetBot.INSTANCE);
    }

    return newState;
  }

  private PointerState handleStatementEdge(PointerState pState, Precision pPrecision, CStatementEdge pCfaEdge) throws UnrecognizedCCodeException {
    if (pCfaEdge.getStatement() instanceof CAssignment) {
      CAssignment assignment = (CAssignment) pCfaEdge.getStatement();
      return handleAssignment(pState, pPrecision, assignment.getLeftHandSide(), assignment.getRightHandSide());
    }
    return pState;
  }

  private PointerState handleAssignment(PointerState pState, Precision pPrecision, CExpression pLeftHandSide, CRightHandSide pRightHandSide) throws UnrecognizedCCodeException {
    LocationSet locations = asLocations(pLeftHandSide, pState, 0);
    return handleAssignment(pState, pPrecision, locations, pRightHandSide);
  }

  private PointerState handleAssignment(PointerState pState, Precision pPrecision, LocationSet pLocationSet,
      CRightHandSide pRightHandSide) throws UnrecognizedCCodeException {
    final Iterable<String> locations;
    if (pLocationSet.isTop()) {
      locations = pState.getKnownLocations();
    } else if (pLocationSet instanceof ExplicitLocationSet) {
      locations = (ExplicitLocationSet) pLocationSet;
    } else {
      locations = Collections.<String>emptySet();
    }
    PointerState result = pState;
    for (String location : locations) {
      result = handleAssignment(result, pPrecision, location, pRightHandSide);
    }
    return result;
  }

  private PointerState handleAssignment(PointerState pState, Precision pPrecision, String pLhsLocation,
      CRightHandSide pRightHandSide) throws UnrecognizedCCodeException {
    return pState.addPointsToInformation(pLhsLocation, asLocations(pRightHandSide, pState, 1));
  }

  private PointerState handleAssignment(PointerState pState, Precision pPrecision, String pLeftHandSide, LocationSet pRightHandSide) throws UnrecognizedCCodeException {
    return pState.addPointsToInformation(pLeftHandSide, pRightHandSide);
  }

  private PointerState handleDeclarationEdge(final PointerState pState, Precision pPrecision, final CDeclarationEdge pCfaEdge) throws UnrecognizedCCodeException {
    if (!(pCfaEdge.getDeclaration() instanceof CVariableDeclaration)) {
      return pState;
    }
    Type type = pCfaEdge.getDeclaration().getType();
    CVariableDeclaration declaration = (CVariableDeclaration) pCfaEdge.getDeclaration();
    CInitializer initializer = declaration.getInitializer();
    if (initializer != null) {
      LocationSet rhs = initializer.accept(new CInitializerVisitor<LocationSet, UnrecognizedCCodeException>() {

        @Override
        public LocationSet visit(CBinaryExpression pIastBinaryExpression) throws UnrecognizedCCodeException {
          return asLocations(pIastBinaryExpression, pState, 1);
        }

        @Override
        public LocationSet visit(CCastExpression pIastCastExpression) throws UnrecognizedCCodeException {
          return asLocations(pIastCastExpression, pState, 1);
        }

        @Override
        public LocationSet visit(CCharLiteralExpression pIastCharLiteralExpression) throws UnrecognizedCCodeException {
          return asLocations(pIastCharLiteralExpression, pState, 1);
        }

        @Override
        public LocationSet visit(CFloatLiteralExpression pIastFloatLiteralExpression)
            throws UnrecognizedCCodeException {
          return asLocations(pIastFloatLiteralExpression, pState, 1);
        }

        @Override
        public LocationSet visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
            throws UnrecognizedCCodeException {
          return asLocations(pIastIntegerLiteralExpression, pState, 1);
        }

        @Override
        public LocationSet visit(CStringLiteralExpression pIastStringLiteralExpression)
            throws UnrecognizedCCodeException {
          return asLocations(pIastStringLiteralExpression, pState, 1);
        }

        @Override
        public LocationSet visit(CTypeIdExpression pIastTypeIdExpression) throws UnrecognizedCCodeException {
          return asLocations(pIastTypeIdExpression, pState, 1);
        }

        @Override
        public LocationSet visit(CTypeIdInitializerExpression pCTypeIdInitializerExpression)
            throws UnrecognizedCCodeException {
          return asLocations(pCTypeIdInitializerExpression, pState, 1);
        }

        @Override
        public LocationSet visit(CUnaryExpression pIastUnaryExpression) throws UnrecognizedCCodeException {
          return asLocations(pIastUnaryExpression, pState, 1);
        }

        @Override
        public LocationSet visit(CImaginaryLiteralExpression pIastLiteralExpression) throws UnrecognizedCCodeException {
          return asLocations(pIastLiteralExpression, pState, 1);
        }

        @Override
        public LocationSet visit(CArraySubscriptExpression pIastArraySubscriptExpression)
            throws UnrecognizedCCodeException {
          return asLocations(pIastArraySubscriptExpression, pState, 1);
        }

        @Override
        public LocationSet visit(CFieldReference pIastFieldReference) throws UnrecognizedCCodeException {
          return asLocations(pIastFieldReference, pState, 1);
        }

        @Override
        public LocationSet visit(CIdExpression pIastIdExpression) throws UnrecognizedCCodeException {
          return asLocations(pIastIdExpression, pState, 1);
        }

        @Override
        public LocationSet visit(CPointerExpression pPointerExpression) throws UnrecognizedCCodeException {
          return asLocations(pPointerExpression, pState, 1);
        }

        @Override
        public LocationSet visit(CComplexCastExpression pComplexCastExpression) throws UnrecognizedCCodeException {
          return asLocations(pComplexCastExpression, pState, 1);
        }

        @Override
        public LocationSet visit(CInitializerExpression pInitializerExpression) throws UnrecognizedCCodeException {
          return asLocations(pInitializerExpression.getExpression(), pState, 1);
        }

        @Override
        public LocationSet visit(CInitializerList pInitializerList) throws UnrecognizedCCodeException {
          return LocationSetTop.INSTANCE;
        }

        @Override
        public LocationSet visit(CDesignatedInitializer pCStructInitializerPart) throws UnrecognizedCCodeException {
          return LocationSetTop.INSTANCE;
        }

      });

      final String location;
      if (type.toString().startsWith("struct ") || type.toString().startsWith("union ")) {
        location = type.toString();
      } else {
        location = declaration.getQualifiedName();
      }
      return handleAssignment(pState, pPrecision, location, rhs);

    }
    return pState;
  }

  private static LocationSet toLocationSet(Iterable<? extends String> pLocations) {
    if (pLocations == null) {
      return LocationSetTop.INSTANCE;
    }
    Iterator<? extends String> locationIterator = pLocations.iterator();
    if (!locationIterator.hasNext()) {
      return LocationSetBot.INSTANCE;
    }
    return ExplicitLocationSet.from(pLocations);
  }

  private static LocationSet asLocations(final CRightHandSide pExpression, final PointerState pState, final int pDerefCounter) throws UnrecognizedCCodeException {
    return pExpression.accept(new CRightHandSideVisitor<LocationSet, UnrecognizedCCodeException>() {

      @Override
      public LocationSet visit(CArraySubscriptExpression pIastArraySubscriptExpression)
          throws UnrecognizedCCodeException {
        if (pIastArraySubscriptExpression.getSubscriptExpression() instanceof CLiteralExpression) {
          CLiteralExpression literal = (CLiteralExpression) pIastArraySubscriptExpression.getSubscriptExpression();
          if (literal instanceof CIntegerLiteralExpression && ((CIntegerLiteralExpression) literal).getValue().equals(BigInteger.ZERO)) {
            LocationSet starredLocations = asLocations(pIastArraySubscriptExpression.getArrayExpression(), pState, pDerefCounter);
            if (starredLocations.isBot() || starredLocations.isTop()) {
              return starredLocations;
            }
            if (!(starredLocations instanceof ExplicitLocationSet)) {
              return LocationSetTop.INSTANCE;
            }
            Set<String> result = new HashSet<>();
            for (String location : ((ExplicitLocationSet) starredLocations)) {
              LocationSet pointsToSet = pState.getPointsToSet(location);
              if (pointsToSet.isTop()) {
                for (String loc : pState.getKnownLocations()) {
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
      public LocationSet visit(final CFieldReference pIastFieldReference) throws UnrecognizedCCodeException {
        Type type = pIastFieldReference.getFieldOwner().getExpressionType();
        String prefix = type.toString();
        String infix = pIastFieldReference.isPointerDereference() ? "->" : ".";
        String suffix = pIastFieldReference.getFieldName();
        return toLocationSet(Collections.singleton(prefix + infix + suffix));
      }

      @Override
      public LocationSet visit(CIdExpression pIastIdExpression) throws UnrecognizedCCodeException {
        Type type = pIastIdExpression.getExpressionType();
        Collection<String> result;
        if (type.toString().startsWith("struct ") || type.toString().startsWith("union ")) {
          result = Collections.<String>singleton(type.toString());
        } else {
          CSimpleDeclaration declaration = pIastIdExpression.getDeclaration();
          if (declaration != null) {
            result = Collections.<String>singleton(declaration.getQualifiedName());
          } else {
            result = Collections.<String>singleton(pIastIdExpression.getName());
          }
        }
        for (int deref = pDerefCounter; deref > 0 && !result.isEmpty(); --deref) {
          Collection<String> newResult = new HashSet<>();
          for (String location : result) {
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
      public LocationSet visit(CPointerExpression pPointerExpression) throws UnrecognizedCCodeException {
        return asLocations(pPointerExpression.getOperand(), pState, pDerefCounter + 1);
      }

      @Override
      public LocationSet visit(CComplexCastExpression pComplexCastExpression) throws UnrecognizedCCodeException {
        return asLocations(pComplexCastExpression.getOperand(), pState, pDerefCounter);
      }

      @Override
      public LocationSet visit(CBinaryExpression pIastBinaryExpression) throws UnrecognizedCCodeException {
        return toLocationSet(Iterables.concat(
            toNormalSet(pState, asLocations(pIastBinaryExpression.getOperand1(), pState, pDerefCounter)),
            toNormalSet(pState, asLocations(pIastBinaryExpression.getOperand2(), pState, pDerefCounter))));
      }

      @Override
      public LocationSet visit(CCastExpression pIastCastExpression) throws UnrecognizedCCodeException {
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
      public LocationSet visit(CTypeIdExpression pIastTypeIdExpression) throws UnrecognizedCCodeException {
        return LocationSetBot.INSTANCE;
      }

      @Override
      public LocationSet visit(CTypeIdInitializerExpression pCTypeIdInitializerExpression)
          throws UnrecognizedCCodeException {
        return LocationSetBot.INSTANCE;
      }

      @Override
      public LocationSet visit(CUnaryExpression pIastUnaryExpression) throws UnrecognizedCCodeException {
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
        return LocationSetTop.INSTANCE;
      }});
  }

  /**
   * Gets the set of possible locations of the given expression. For the expression 'x', the
   * location is the identifier x. For the expression 's.a' the location is the identifier
   * t.a, where t is the type of s. For the expression '*p', the possible locations are the
   * points-to set of locations the expression 'p'.
   *
   * @param pState
   * @param pExpression
   * @return
   * @throws UnrecognizedCCodeException
   */
  public static LocationSet asLocations(CExpression pExpression, final PointerState pState) throws UnrecognizedCCodeException {
    return asLocations(pExpression, pState, 0);
  }

  public static Set<String> toNormalSet(PointerState pState, LocationSet pLocationSet) {
    if (pLocationSet.isBot()) {
      return Collections.emptySet();
    }
    Set<String> result = new HashSet<>();
    if (pLocationSet.isTop() || !(pLocationSet instanceof ExplicitLocationSet)) {
      Iterables.addAll(result, pState.getKnownLocations());
    } else {
      Iterables.addAll(result, ((ExplicitLocationSet) pLocationSet));
    }
    return result;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return Collections.singleton(pState);
  }

}
