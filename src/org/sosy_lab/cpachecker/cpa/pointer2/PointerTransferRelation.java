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
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
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
import org.sosy_lab.cpachecker.cpa.pointer2.util.Location;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetBot;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetTop;
import org.sosy_lab.cpachecker.cpa.pointer2.util.Struct;
import org.sosy_lab.cpachecker.cpa.pointer2.util.Union;
import org.sosy_lab.cpachecker.cpa.pointer2.util.Variable;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;


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
      final Location location;
      if (type.toString().startsWith("struct ")) {
        location = new Variable(type.toString());
      } else if (type.toString().startsWith("union ")) {
        location = new Variable(type.toString());
      } else {
        location = new Variable(formalParam.getQualifiedName());
      }
      newState = handleAssignment(pState, pPrecision, location, getLocationsOf(pState, actualParam));
    }

    // Handle remaining formal parameters where no actual argument was provided
    for (CParameterDeclaration formalParam : FluentIterable.from(formalParams).skip(limit)) {
      Type type = formalParam.getType();
      final Location location;
      if (type.toString().startsWith("struct ")) {
        location = new Variable(type.toString());
      } else if (type.toString().startsWith("union ")) {
        location = new Variable(type.toString());
      } else {
        location = new Variable(formalParam.getQualifiedName());
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
    Iterable<Location> locations = asLocations(pLeftHandSide, pState);
    if (locations != null) {
      return handleAssignment(pState, pPrecision, locations, pRightHandSide);
    }
    return handleAssignment(pState, pPrecision, LocationSetTop.INSTANCE, pRightHandSide);
  }

  private PointerState handleAssignment(PointerState pState, Precision pPrecision, LocationSet pLocationSet,
      CRightHandSide pRightHandSide) throws UnrecognizedCCodeException {
    final Iterable<Location> locations;
    if (pLocationSet.isTop()) {
      locations = pState.getKnownLocations();
    } else if (pLocationSet instanceof ExplicitLocationSet) {
      ExplicitLocationSet explicitLocationSet = (ExplicitLocationSet) pLocationSet;
      locations = explicitLocationSet.getElements();
    } else {
      locations = Collections.<Location>emptySet();
    }
    PointerState result = pState;
    for (Location location : locations) {
      result = handleAssignment(result, pPrecision, location, pRightHandSide);
    }
    return result;
  }

  private PointerState handleAssignment(PointerState pState, Precision pPrecision, Iterable<Location> pLeftHandSide, CRightHandSide pRightHandSide) throws UnrecognizedCCodeException {
    PointerState result = pState;
    for (Location lhsLocation : pLeftHandSide) {
      result = handleAssignment(pState, pPrecision, lhsLocation, pRightHandSide);
    }
    return result;
  }

  private PointerState handleAssignment(PointerState pState, Precision pPrecision, Location pLhsLocation,
      CRightHandSide pRightHandSide) throws UnrecognizedCCodeException {
    return pState.addPointsToInformation(pLhsLocation, getLocationsOf(pState, pRightHandSide));
  }

  private PointerState handleAssignment(PointerState pState, Precision pPrecision, Location pLeftHandSide, LocationSet pRightHandSide) throws UnrecognizedCCodeException {
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
          return getLocationsOf(pState, pIastBinaryExpression);
        }

        @Override
        public LocationSet visit(CCastExpression pIastCastExpression) throws UnrecognizedCCodeException {
          return getLocationsOf(pState, pIastCastExpression);
        }

        @Override
        public LocationSet visit(CCharLiteralExpression pIastCharLiteralExpression) throws UnrecognizedCCodeException {
          return getLocationsOf(pState, pIastCharLiteralExpression);
        }

        @Override
        public LocationSet visit(CFloatLiteralExpression pIastFloatLiteralExpression)
            throws UnrecognizedCCodeException {
          return getLocationsOf(pState, pIastFloatLiteralExpression);
        }

        @Override
        public LocationSet visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
            throws UnrecognizedCCodeException {
          return getLocationsOf(pState, pIastIntegerLiteralExpression);
        }

        @Override
        public LocationSet visit(CStringLiteralExpression pIastStringLiteralExpression)
            throws UnrecognizedCCodeException {
          return getLocationsOf(pState, pIastStringLiteralExpression);
        }

        @Override
        public LocationSet visit(CTypeIdExpression pIastTypeIdExpression) throws UnrecognizedCCodeException {
          return getLocationsOf(pState, pIastTypeIdExpression);
        }

        @Override
        public LocationSet visit(CTypeIdInitializerExpression pCTypeIdInitializerExpression)
            throws UnrecognizedCCodeException {
          return getLocationsOf(pState, pCTypeIdInitializerExpression);
        }

        @Override
        public LocationSet visit(CUnaryExpression pIastUnaryExpression) throws UnrecognizedCCodeException {
          return getLocationsOf(pState, pIastUnaryExpression);
        }

        @Override
        public LocationSet visit(CImaginaryLiteralExpression pIastLiteralExpression) throws UnrecognizedCCodeException {
          return getLocationsOf(pState, pIastLiteralExpression);
        }

        @Override
        public LocationSet visit(CArraySubscriptExpression pIastArraySubscriptExpression)
            throws UnrecognizedCCodeException {
          return getLocationsOf(pState, pIastArraySubscriptExpression);
        }

        @Override
        public LocationSet visit(CFieldReference pIastFieldReference) throws UnrecognizedCCodeException {
          return getLocationsOf(pState, pIastFieldReference);
        }

        @Override
        public LocationSet visit(CIdExpression pIastIdExpression) throws UnrecognizedCCodeException {
          return getLocationsOf(pState, pIastIdExpression);
        }

        @Override
        public LocationSet visit(CPointerExpression pPointerExpression) throws UnrecognizedCCodeException {
          return getLocationsOf(pState, pPointerExpression);
        }

        @Override
        public LocationSet visit(CComplexCastExpression pComplexCastExpression) throws UnrecognizedCCodeException {
          return getLocationsOf(pState, pComplexCastExpression);
        }

        @Override
        public LocationSet visit(CInitializerExpression pInitializerExpression) throws UnrecognizedCCodeException {
          return getLocationsOf(pState, pInitializerExpression.getExpression());
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

      final Location location;
      if (type.toString().startsWith("struct ")) {
        location = new Variable(type.toString());
      } else if (type.toString().startsWith("union ")) {
        location = new Variable(type.toString());
      } else {
        location = new Variable(declaration.getQualifiedName());
      }
      return handleAssignment(pState, pPrecision, location, rhs);

    }
    return pState;
  }

  public static Iterable<Location> asLocations(CExpression pExpression, final PointerState pState) throws UnrecognizedCCodeException {
    return pExpression.accept(new CExpressionVisitor<Iterable<Location>, UnrecognizedCCodeException>() {

      @Override
      public Iterable<Location> visit(CArraySubscriptExpression pIastArraySubscriptExpression)
          throws UnrecognizedCCodeException {
        if (pIastArraySubscriptExpression.getSubscriptExpression() instanceof CLiteralExpression) {
          CLiteralExpression literal = (CLiteralExpression) pIastArraySubscriptExpression.getSubscriptExpression();
          if (literal instanceof CIntegerLiteralExpression && ((CIntegerLiteralExpression) literal).getValue().equals(BigInteger.ZERO)) {
            Iterable<Location> starredLocations = asLocations(pIastArraySubscriptExpression.getArrayExpression(), pState);
            if (starredLocations == null) {
              return null;
            }
            Set<Location> result = new HashSet<>();
            for (Location location : starredLocations) {
              LocationSet pointsToSet = pState.getPointsToSet(location);
              if (pointsToSet.isTop()) {
                for (Location loc : pState.getKnownLocations()) {
                  result.add(loc);
                }
                break;
              } else if (!pointsToSet.isBot() && pointsToSet instanceof ExplicitLocationSet) {
                ExplicitLocationSet explicitLocationSet = (ExplicitLocationSet) pointsToSet;
                result.addAll(explicitLocationSet.getElements());
              }
            }
            return result;
          }
        }
        return null;
      }

      @Override
      public Iterable<Location> visit(final CFieldReference pIastFieldReference) throws UnrecognizedCCodeException {
        Iterable<Location> ownerLocations = asLocations(pIastFieldReference.getFieldOwner(), pState);
        if (ownerLocations == null) {
          return null;
        }
        return FluentIterable.from(ownerLocations).transform(new Function<Location, Location>() {

          @Override
          public Location apply(@Nullable Location pInput) {
            if (pInput == null) {
              return null;
            }
            return new Variable(pInput.getId()  + "." + pIastFieldReference.getFieldName());
          }

        });
      }

      @Override
      public Iterable<Location> visit(CIdExpression pIastIdExpression) throws UnrecognizedCCodeException {
        Type type = pIastIdExpression.getExpressionType();
        if (type.toString().startsWith("struct ")) {
          return Collections.<Location>singleton(new Struct(type.toString()));
        }
        if (type.toString().startsWith("union ")) {
          return Collections.<Location>singleton(new Union(type.toString()));
        }
        CSimpleDeclaration declaration = pIastIdExpression.getDeclaration();
        if (declaration != null) {
          return Collections.<Location>singleton(new Variable(declaration.getQualifiedName()));
        }
        return Collections.<Location>singleton(new Variable(pIastIdExpression.getName()));
      }

      @Override
      public Iterable<Location> visit(CPointerExpression pPointerExpression) throws UnrecognizedCCodeException {
        Iterable<Location> starredLocations = asLocations(pPointerExpression.getOperand(), pState);
        if (starredLocations == null) {
          return null;
        }
        Set<Location> result = new HashSet<>();
        for (Location location : starredLocations) {
          LocationSet pointsToSet = pState.getPointsToSet(location);
          if (pointsToSet.isTop()) {
            for (Location loc : pState.getKnownLocations()) {
              result.add(loc);
            }
            break;
          } else if (!pointsToSet.isBot() && pointsToSet instanceof ExplicitLocationSet) {
            ExplicitLocationSet explicitLocationSet = (ExplicitLocationSet) pointsToSet;
            result.addAll(explicitLocationSet.getElements());
          }
        }
        return result;
      }

      @Override
      public Iterable<Location> visit(CComplexCastExpression pComplexCastExpression) throws UnrecognizedCCodeException {
        return asLocations(pComplexCastExpression.getOperand(), pState);
      }

      @Override
      public Iterable<Location> visit(CBinaryExpression pIastBinaryExpression) throws UnrecognizedCCodeException {
        return null;
      }

      @Override
      public Iterable<Location> visit(CCastExpression pIastCastExpression) throws UnrecognizedCCodeException {
        return asLocations(pIastCastExpression.getOperand(), pState);
      }

      @Override
      public Iterable<Location> visit(CCharLiteralExpression pIastCharLiteralExpression)
          throws UnrecognizedCCodeException {
        return null;
      }

      @Override
      public Iterable<Location> visit(CFloatLiteralExpression pIastFloatLiteralExpression)
          throws UnrecognizedCCodeException {
        return null;
      }

      @Override
      public Iterable<Location> visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
          throws UnrecognizedCCodeException {
        return null;
      }

      @Override
      public Iterable<Location> visit(CStringLiteralExpression pIastStringLiteralExpression)
          throws UnrecognizedCCodeException {
        return null;
      }

      @Override
      public Iterable<Location> visit(CTypeIdExpression pIastTypeIdExpression) throws UnrecognizedCCodeException {
        return null;
      }

      @Override
      public Iterable<Location> visit(CTypeIdInitializerExpression pCTypeIdInitializerExpression)
          throws UnrecognizedCCodeException {
        return null;
      }

      @Override
      public Iterable<Location> visit(CUnaryExpression pIastUnaryExpression) throws UnrecognizedCCodeException {
        return null;
      }

      @Override
      public Iterable<Location> visit(CImaginaryLiteralExpression PIastLiteralExpression)
          throws UnrecognizedCCodeException {
        return null;
      }});
  }

  /**
   * Gets the set of possible locations the expression value may point to. For the expression 'x', the
   * location is the points-to set of x. For the expression 's.a' the location is the points-to set of
   * t.a, where t is the type of s. For the expression '*p', the possible locations are the sum of the
   * points-to sets of locations the value of p points to. For '&v', the location is v.
   *
   * @param pState
   * @param pExpression
   * @return
   * @throws UnrecognizedCCodeException
   */
  private static LocationSet getLocationsOf(final PointerState pState, CRightHandSide pExpression) throws UnrecognizedCCodeException {
    return pExpression.accept(new CRightHandSideVisitor<LocationSet, UnrecognizedCCodeException>() {

      @Override
      public LocationSet visit(CArraySubscriptExpression pIastArraySubscriptExpression) throws UnrecognizedCCodeException {
        return LocationSetTop.INSTANCE;
      }

      @Override
      public LocationSet visit(final CFieldReference pIastFieldReference) throws UnrecognizedCCodeException {
        Iterable<Location> fieldReferenceAsLocations = asLocations(pIastFieldReference, pState);
        if (fieldReferenceAsLocations == null) {
          return LocationSetTop.INSTANCE;
        }
        LocationSet result = LocationSetBot.INSTANCE;
        for (Location fieldReferenceAsLocation : fieldReferenceAsLocations) {
          result = result.addElements(pState.getPointsToSet(fieldReferenceAsLocation));
        }
        return result;
      }

      @Override
      public LocationSet visit(CIdExpression pIastIdExpression) throws UnrecognizedCCodeException {
        Iterable<Location> isAsLocations = asLocations(pIastIdExpression, pState);
        if (isAsLocations == null) {
          return LocationSetTop.INSTANCE;
        }
        LocationSet result = LocationSetBot.INSTANCE;
        for (Location isAsLocation : isAsLocations) {
          result = result.addElements(pState.getPointsToSet(isAsLocation));
        }
        return result;
      }

      @Override
      public LocationSet visit(CPointerExpression pPointerExpression) throws UnrecognizedCCodeException {
        LocationSet variableLocations = getLocationsOf(pState, pPointerExpression.getOperand());
        if (variableLocations.isTop()) {
          return LocationSetTop.INSTANCE;
        } else if (variableLocations.isBot()) {
          return LocationSetBot.INSTANCE;
        }
        ExplicitLocationSet explicitLocationSet = (ExplicitLocationSet) variableLocations;
        LocationSet result = LocationSetBot.INSTANCE;
        for (Location pointerVariableLocation : explicitLocationSet.getElements()) {
          result = result.addElements(pState.getPointsToSet(pointerVariableLocation));
        }
        return result;
      }

      @Override
      public LocationSet visit(CComplexCastExpression pComplexCastExpression) throws UnrecognizedCCodeException {
        return pComplexCastExpression.getOperand().accept(this);
      }

      @Override
      public LocationSet visit(CBinaryExpression pIastBinaryExpression) throws UnrecognizedCCodeException {
        return LocationSetTop.INSTANCE;
      }

      @Override
      public LocationSet visit(CCastExpression pIastCastExpression) throws UnrecognizedCCodeException {
        return pIastCastExpression.getOperand().accept(this);
      }

      @Override
      public LocationSet visit(CCharLiteralExpression pIastCharLiteralExpression) throws UnrecognizedCCodeException {
        return LocationSetTop.INSTANCE;
      }

      @Override
      public LocationSet visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws UnrecognizedCCodeException {
        return LocationSetTop.INSTANCE;
      }

      @Override
      public LocationSet visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) throws UnrecognizedCCodeException {
        return LocationSetTop.INSTANCE;
      }

      @Override
      public LocationSet visit(CStringLiteralExpression pIastStringLiteralExpression) throws UnrecognizedCCodeException {
        return LocationSetTop.INSTANCE;
      }

      @Override
      public LocationSet visit(CTypeIdExpression pIastTypeIdExpression) throws UnrecognizedCCodeException {
        return LocationSetTop.INSTANCE;
      }

      @Override
      public LocationSet visit(CTypeIdInitializerExpression pCTypeIdInitializerExpression) throws UnrecognizedCCodeException {
        return LocationSetTop.INSTANCE;
      }

      @Override
      public LocationSet visit(CUnaryExpression pIastUnaryExpression) throws UnrecognizedCCodeException {
        if (pIastUnaryExpression.getOperator().equals(UnaryOperator.AMPER)) {
          // For '&x', the location the value of the expression points to is x
          CExpression target = pIastUnaryExpression.getOperand();
          if (target instanceof CIdExpression) {
            CIdExpression idExpression = (CIdExpression) target;
            Type type = pIastUnaryExpression.getExpressionType();
            if (type.toString().startsWith("struct ")) {
              return ExplicitLocationSet.from(new Struct(type.toString()));
            }
            if (type.toString().startsWith("union ")) {
              return ExplicitLocationSet.from(new Union(type.toString()));
            }
            CSimpleDeclaration declaration = idExpression.getDeclaration();
            if (declaration != null) {
              return ExplicitLocationSet.from((new Variable(declaration.getQualifiedName())));
            }
            return ExplicitLocationSet.from(new Variable(idExpression.getName()));
          }
        }
        return LocationSetTop.INSTANCE;
      }

      @Override
      public LocationSet visit(CImaginaryLiteralExpression pIastLiteralExpression) throws UnrecognizedCCodeException {
        return LocationSetTop.INSTANCE;
      }

      @Override
      public LocationSet visit(CFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCCodeException {
        return LocationSetTop.INSTANCE;
      }

    });
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return Collections.singleton(pState);
  }

}
