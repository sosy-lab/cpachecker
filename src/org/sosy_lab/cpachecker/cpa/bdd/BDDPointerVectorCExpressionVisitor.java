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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class BDDPointerVectorCExpressionVisitor extends BDDVectorCExpressionVisitor {

  private final PointerState pointerInfo;

  protected BDDPointerVectorCExpressionVisitor(
      PredicateManager pPredMgr,
      VariableTrackingPrecision pPrecision,
      BitvectorManager pBVmgr,
      MachineModel pMachineModel,
      CFANode pLocation,
      PointerState pPointerInfo) {
    super(pPredMgr, pPrecision, pBVmgr, pMachineModel, pLocation);
    pointerInfo = pPointerInfo;
  }

  @Override
  public Region[] visit(CPointerExpression e) {
    ExplicitLocationSet explicitSet = null;
    try {
      explicitSet = BDDTransferRelation.getLocationsForLhs(pointerInfo, e);
    } catch (UnrecognizedCodeException exception) {
      throw new AssertionError(exception);
    }

    if (explicitSet != null && explicitSet.getSize() == 1) {
      MemoryLocation memLoc = Iterables.getOnlyElement(explicitSet);
      return predMgr.createPredicate(
          memLoc.getAsSimpleString(),
          e.getExpressionType(),
          location,
          getSize(e.getExpressionType()),
          precision);
    }
    return visitDefault(e);
  }
}
