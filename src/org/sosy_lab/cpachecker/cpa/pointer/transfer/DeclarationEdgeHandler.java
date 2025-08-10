// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.transfer;

import static org.sosy_lab.cpachecker.cpa.pointer.utils.ReferenceLocationsResolver.getReferencedLocations;

import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.pointer.PointerAnalysisState;
import org.sosy_lab.cpachecker.cpa.pointer.location.DeclaredVariableLocation;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public final class DeclarationEdgeHandler implements TransferRelationEdgeHandler<CDeclarationEdge> {
  private PointerTransferOptions options;

  public DeclarationEdgeHandler(PointerTransferOptions pOptions) {
    options = pOptions;
  }

  @Override
  public PointerAnalysisState handleEdge(PointerAnalysisState pState, CDeclarationEdge pCfaEdge)
      throws CPATransferException {
    if (pCfaEdge.getDeclaration() instanceof CVariableDeclaration declaration) {

      Type type = declaration.getType().getCanonicalType();
      if (!(type instanceof CPointerType)) {
        return pState;
      }

      CInitializer initializer = declaration.getInitializer();

      if (initializer != null) {
        LocationSet pointsToSet =
            initializer.accept(
                new CInitializerVisitor<LocationSet, CPATransferException>() {

                  @Override
                  public LocationSet visit(CInitializerExpression pInitializerExpression)
                      throws CPATransferException {
                    if (pInitializerExpression.getExpression()
                        instanceof CIntegerLiteralExpression) {
                      return LocationSetFactory.withTop();
                    }

                    return getReferencedLocations(
                        pInitializerExpression.getExpression(), pState, true, pCfaEdge, options);
                  }

                  @Override
                  public LocationSet visit(CInitializerList pInitializerList) {
                    return LocationSetFactory.withTop();
                  }

                  @Override
                  public LocationSet visit(CDesignatedInitializer pCStructInitializerPart) {
                    return LocationSetFactory.withTop();
                  }
                });
        if (pointsToSet.isTop()) {
          return pState;
        }
        DeclaredVariableLocation pointerLocation =
            new DeclaredVariableLocation(MemoryLocation.forDeclaration(declaration));
        return new PointerAnalysisState(
            pState.getPointsToMap().putAndCopy(pointerLocation, pointsToSet));
      }
      return pState;
    }
    return pState;
  }
}
