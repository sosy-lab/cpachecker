// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.refiner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

public abstract class ConcreteErrorPathAllocator<S extends AbstractState> {

  private final Class<S> cls; // needed for generic type

  protected final AssumptionToEdgeAllocator assumptionToEdgeAllocator;

  protected ConcreteErrorPathAllocator(
      Class<S> pCls, AssumptionToEdgeAllocator pAssumptionToEdgeAllocator) {
    cls = pCls;
    assumptionToEdgeAllocator = pAssumptionToEdgeAllocator;
  }

  public CFAPathWithAssumptions allocateAssignmentsToPath(List<Pair<S, List<CFAEdge>>> pPath) {
    ConcreteStatePath concreteStatePath = createConcreteStatePath(pPath);
    return CFAPathWithAssumptions.of(concreteStatePath, assumptionToEdgeAllocator);
  }

  public ConcreteStatePath allocateAssignmentsToPath(ARGPath pPath) {
    List<Pair<S, List<CFAEdge>>> path = new ArrayList<>(pPath.size());
    PathIterator it = pPath.fullPathIterator();
    while (it.hasNext()) {
      List<CFAEdge> innerEdges = new ArrayList<>();
      do {
        it.advance();
        innerEdges.add(it.getIncomingEdge());
      } while (!it.isPositionWithState());
      S state = AbstractStates.extractStateByType(it.getAbstractState(), cls);
      if (state == null) {
        return null;
      }
      path.add(Pair.of(state, innerEdges));
    }
    return createConcreteStatePath(path);
  }

  protected abstract ConcreteStatePath createConcreteStatePath(List<Pair<S, List<CFAEdge>>> pPath);

  protected boolean isDeclarationValueKnown(
      CDeclarationEdge pCfaEdge, Set<CLeftHandSide> pAlreadyAssigned) {
    CDeclaration dcl = pCfaEdge.getDeclaration();
    if (dcl instanceof CVariableDeclaration) {
      CIdExpression idExp = new CIdExpression(dcl.getFileLocation(), dcl);
      return isLeftHandSideValueKnown(idExp, pAlreadyAssigned);
    }
    // only variable declaration matter for value analysis
    return true;
  }

  protected boolean isLeftHandSideValueKnown(
      CLeftHandSide pLHS, Set<CLeftHandSide> pAlreadyAssigned) {
    ValueKnownVisitor v = new ValueKnownVisitor(pAlreadyAssigned);
    return pLHS.accept(v);
  }

  /**
   * Checks, if we know a value. This is the case, if the value will not be assigned in the future.
   * Since we traverse the multi edge from bottom to top, this means if a left hand side, that was
   * already assigned, may not be part of the Left Hand Side we want to know the value of.
   */
  private static class ValueKnownVisitor extends DefaultCExpressionVisitor<Boolean, NoException> {

    private final Set<CLeftHandSide> alreadyAssigned;

    public ValueKnownVisitor(Set<CLeftHandSide> pAlreadyAssigned) {
      alreadyAssigned = pAlreadyAssigned;
    }

    @Override
    protected Boolean visitDefault(CExpression pExp) {
      return true;
    }

    @Override
    public Boolean visit(CArraySubscriptExpression pE) {
      return !alreadyAssigned.contains(pE);
    }

    @Override
    public Boolean visit(CBinaryExpression pE) {
      return pE.getOperand1().accept(this) && pE.getOperand2().accept(this);
    }

    @Override
    public Boolean visit(CCastExpression pE) {
      return pE.getOperand().accept(this);
    }

    // TODO Complex Cast
    @Override
    public Boolean visit(CFieldReference pE) {
      return !alreadyAssigned.contains(pE);
    }

    @Override
    public Boolean visit(CIdExpression pE) {
      return !alreadyAssigned.contains(pE);
    }

    @Override
    public Boolean visit(CPointerExpression pE) {
      return !alreadyAssigned.contains(pE);
    }

    @Override
    public Boolean visit(CUnaryExpression pE) {
      return pE.getOperand().accept(this);
    }
  }
}
