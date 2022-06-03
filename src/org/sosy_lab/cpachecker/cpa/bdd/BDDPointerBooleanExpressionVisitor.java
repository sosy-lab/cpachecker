// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bdd;

import com.google.common.collect.Iterables;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class BDDPointerBooleanExpressionVisitor extends BDDBooleanExpressionVisitor {

  private final PointerState pointerInfo;

  protected BDDPointerBooleanExpressionVisitor(
      PredicateManager pPredMgr,
      RegionManager pRmgr,
      VariableTrackingPrecision pPrecision,
      CFANode pLocation,
      PointerState pPointerInfo) {
    super(pPredMgr, pRmgr, pPrecision, pLocation);
    pointerInfo = pPointerInfo;
  }

  @Override
  public Region visit(CPointerExpression e) {
    ExplicitLocationSet explicitSet = null;
    try {
      explicitSet = BDDTransferRelation.getLocationsForLhs(pointerInfo, e);
    } catch (UnrecognizedCodeException exception) {
      throw new AssertionError(exception);
    }

    if (explicitSet != null && explicitSet.getSize() == 1) {
      MemoryLocation memLoc = Iterables.getOnlyElement(explicitSet);
      final Region[] result =
          predMgr.createPredicate(
              memLoc.getExtendedQualifiedName(),
              e.getExpressionType(),
              location,
              BOOLEAN_SIZE,
              precision);
      if (result == null) {
        return null;
      } else {
        assert result.length == BOOLEAN_SIZE;
        return result[0];
      }
    }
    return visitDefault(e);
  }
}
