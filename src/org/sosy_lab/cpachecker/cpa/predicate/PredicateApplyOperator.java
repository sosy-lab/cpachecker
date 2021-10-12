/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.EmptyEdge;
import org.sosy_lab.cpachecker.core.defaults.WrapperCFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ApplyOperator;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractEdge.FormulaDescription;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.AbstractionState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.NonAbstractionState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "cpa.predicate")
public class PredicateApplyOperator implements ApplyOperator {

  private final String rename(String name) {
    return name + "__ENV";
  }

  private final String localRename(String name) {
    if (name.contains("::")) {
      // local var, rename
      if (name.contains("@")) {
        // instantiated
        List<String> parts = Splitter.on('@').splitToList(name);
        String newName = rename(parts.get(0)) + "@";
        if (parts.size() > 1) {
          // @ may be last symbol
          newName += parts.get(1);
        }
        return newName;
      } else {
        return rename(name);
      }
    } else {
      return name;
    }
  };

  private class ExpressionTransformer
      implements CRightHandSideVisitor<Pair<CExpression, Boolean>, NoException> {

    private final CFAEdge edge;

    public ExpressionTransformer(CFAEdge pEdge) {
      edge = pEdge;
    }

    @Override
    public Pair<CExpression, Boolean> visit(CArraySubscriptExpression pIastArraySubscriptExpression)
        throws NoException {

      CExpression arrayExpr = pIastArraySubscriptExpression.getArrayExpression();
      Pair<CExpression, Boolean> newArrayExp = arrayExpr.accept(this);

      Boolean isGlobal = newArrayExp.getSecond();
      Pair<CExpression, Boolean> newSubsExp =
          pIastArraySubscriptExpression.getSubscriptExpression().accept(this);

      FileLocation loc = pIastArraySubscriptExpression.getFileLocation();
      CType type = pIastArraySubscriptExpression.getExpressionType();

      return Pair.of(
          new CArraySubscriptExpression(loc, type, newArrayExp.getFirst(), newSubsExp.getFirst()),
          isGlobal);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CFieldReference pIastFieldReference)
        throws NoException {

      CExpression owner = pIastFieldReference.getFieldOwner();
      Pair<CExpression, Boolean> newOwner = owner.accept(this);

      FileLocation loc = pIastFieldReference.getFileLocation();
      CType type = pIastFieldReference.getExpressionType();
      String name = pIastFieldReference.getFieldName();
      boolean deref = pIastFieldReference.isPointerDereference();

      CFieldReference result = new CFieldReference(loc, type, name, newOwner.getFirst(), deref);
      return Pair.of(result, newOwner.getSecond() || deref);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CIdExpression pIastIdExpression) throws NoException {
      CSimpleDeclaration decl = pIastIdExpression.getDeclaration();
      if (decl instanceof CDeclaration) {
        if (((CDeclaration) decl).isGlobal()) {
          return Pair.of(pIastIdExpression, true);
        } else if (decl instanceof CVariableDeclaration) {
          FileLocation loc = pIastIdExpression.getFileLocation();
          CType type = ((CVariableDeclaration) decl).getType();
          String name = rename(((CVariableDeclaration) decl).getName());
          String origName = rename(((CVariableDeclaration) decl).getOrigName());
          String qualifName = rename(((CVariableDeclaration) decl).getQualifiedName());

          CDeclaration newDecl =
              new CVariableDeclaration(
                  loc,
                  false,
                  CStorageClass.AUTO,
                  type,
                  name,
                  origName,
                  qualifName,
                  null);

          return Pair.of(new CIdExpression(loc, newDecl), false);
        }
      } else if (decl instanceof CParameterDeclaration) {
        FileLocation loc = pIastIdExpression.getFileLocation();
        CParameterDeclaration pDecl = ((CParameterDeclaration) decl);
        CType type = pDecl.getType();
        String name = rename(pDecl.getName());
        String origName = rename(pDecl.getOrigName());
        String qualName = rename(pDecl.getQualifiedName());

        CVariableDeclaration newDecl =
            new CVariableDeclaration(
                loc,
                false,
                CStorageClass.AUTO,
                type,
                name,
                origName,
                qualName,
                null);

        return Pair.of(new CIdExpression(loc, newDecl), false);
      }

      return Pair.of(pIastIdExpression, false);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CPointerExpression pPointerExpression)
        throws NoException {
      CExpression owner = pPointerExpression.getOperand();
      Pair<CExpression, Boolean> newOwner = owner.accept(this);

      FileLocation loc = pPointerExpression.getFileLocation();
      CType type = pPointerExpression.getExpressionType();

      CPointerExpression result = new CPointerExpression(loc, type, newOwner.getFirst());
      return Pair.of(result, true);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CComplexCastExpression pComplexCastExpression)
        throws NoException {
      CExpression operand = pComplexCastExpression.getOperand();
      Pair<CExpression, Boolean> newOperand = operand.accept(this);

      FileLocation loc = pComplexCastExpression.getFileLocation();
      CType type = pComplexCastExpression.getExpressionType();
      CType newType = pComplexCastExpression.getType();
      boolean real = pComplexCastExpression.isRealCast();

      CComplexCastExpression result =
          new CComplexCastExpression(loc, type, newOperand.getFirst(), newType, real);
      return Pair.of(result, newOperand.getSecond());
    }

    @Override
    public Pair<CExpression, Boolean> visit(CBinaryExpression pIastBinaryExpression)
        throws NoException {
      CExpression operand1 = pIastBinaryExpression.getOperand1();
      CExpression operand2 = pIastBinaryExpression.getOperand2();
      Pair<CExpression, Boolean> newOperand1 = operand1.accept(this);
      Pair<CExpression, Boolean> newOperand2 = operand2.accept(this);

      FileLocation loc = pIastBinaryExpression.getFileLocation();
      CType type = pIastBinaryExpression.getExpressionType();
      CType calcType = pIastBinaryExpression.getCalculationType();
      BinaryOperator operator = pIastBinaryExpression.getOperator();

      CBinaryExpression result =
          new CBinaryExpression(
              loc,
              type,
              calcType,
              newOperand1.getFirst(),
              newOperand2.getFirst(),
              operator);
      return Pair.of(result, newOperand1.getSecond() || newOperand2.getSecond());
    }

    @Override
    public Pair<CExpression, Boolean> visit(CCastExpression pIastCastExpression)
        throws NoException {
      CExpression operand = pIastCastExpression.getOperand();
      Pair<CExpression, Boolean> newOperand = operand.accept(this);

      if (newOperand == null) {
        return null;
      }

      FileLocation loc = pIastCastExpression.getFileLocation();
      CType type = pIastCastExpression.getExpressionType();

      CCastExpression result = new CCastExpression(loc, type, newOperand.getFirst());
      return Pair.of(result, newOperand.getSecond());
    }

    @Override
    public Pair<CExpression, Boolean> visit(CCharLiteralExpression pIastCharLiteralExpression)
        throws NoException {
      return Pair.of(pIastCharLiteralExpression, false);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CFloatLiteralExpression pIastFloatLiteralExpression)
        throws NoException {
      return Pair.of(pIastFloatLiteralExpression, false);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
        throws NoException {
      return Pair.of(pIastIntegerLiteralExpression, false);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CStringLiteralExpression pIastStringLiteralExpression)
        throws NoException {
      return Pair.of(pIastStringLiteralExpression, false);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CTypeIdExpression pIastTypeIdExpression)
        throws NoException {
      return Pair.of(pIastTypeIdExpression, false);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CUnaryExpression pIastUnaryExpression)
        throws NoException {
      CExpression operand = pIastUnaryExpression.getOperand();
      Pair<CExpression, Boolean> newOperand = operand.accept(this);

      if (newOperand == null) {
        return null;
      }

      FileLocation loc = pIastUnaryExpression.getFileLocation();
      CType type = pIastUnaryExpression.getExpressionType();
      UnaryOperator operator = pIastUnaryExpression.getOperator();

      CUnaryExpression result = new CUnaryExpression(loc, type, newOperand.getFirst(), operator);
      return Pair.of(result, newOperand.getSecond());
    }

    @Override
    public Pair<CExpression, Boolean> visit(CImaginaryLiteralExpression PIastLiteralExpression)
        throws NoException {
      return Pair.of(PIastLiteralExpression, false);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CAddressOfLabelExpression pAddressOfLabelExpression)
        throws NoException {
      return Pair.of(pAddressOfLabelExpression, false);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CFunctionCallExpression pIastFunctionCallExpression)
        throws NoException {
      CFunctionCallExpression fExp = pIastFunctionCallExpression;
      CIdExpression rhs;

      CFunctionSummaryEdge summary = ((CFunctionReturnEdge) edge).getSummaryEdge();
      final Optional<CVariableDeclaration> returnVariableDeclaration =
          summary.getFunctionEntry().getReturnVariable();

      assert returnVariableDeclaration.isPresent();

      rhs = new CIdExpression(fExp.getFileLocation(), returnVariableDeclaration.get());

      return Pair.of(rhs, false);
    }

  }

  @Option(
    secure = true,
    name = "useUndefFunctions",
    description = "Use undefined assignments (havoc abstraction)")
  private boolean useUndefFuncs = true;

  @Option(
    secure = true,
    description = "do not consider abstraction from inference objects")
  private boolean emptyAbstraction = false;

  @Option(secure = true, description = "reset shared variables values")
  private boolean havocAbstraction2 = false;

  @Option(secure = true, description = "apply only if state is not true (relevant)")
  private boolean applyOptimization = true;

  private final Solver solver;
  private final BooleanFormulaManager mngr;
  private final FormulaManagerView fmngr;
  private final PathFormulaManager pmngr;
  // private final PredicateAbstractionManager amngr;

  final Timer creationTimer = new Timer();
  final Timer convertingTimer = new Timer();

  public PredicateApplyOperator(
      Solver s,
      FormulaManagerView pFormulaManager,
      PathFormulaManager pPathFormulaManager,
      Configuration pConfig) {
    solver = s;
    mngr = solver.getFormulaManager().getBooleanFormulaManager();
    fmngr = pFormulaManager;
    pmngr = pPathFormulaManager;
    try {
      pConfig.inject(this);
    } catch (InvalidConfigurationException e) {
      // Can do nothing
    }
  }

  @Override
  public AbstractState apply(AbstractState pState1, AbstractState pState2) {
    if (pState1 instanceof PredicateAbstractState && pState2 instanceof PredicateProjectedState) {
      PredicateAbstractState state1 = (PredicateAbstractState) pState1;
      PredicateProjectedState state2 = (PredicateProjectedState) pState2;

      AbstractEdge edge;
      if (applyOptimization && mngr.isTrue(state1.getAbstractionFormula().asFormula())) {
        // Any effect does nothing with true formula
        edge = EmptyEdge.getInstance();
      } else if (compatible(state1, state2)) {
        edge = state2.getAbstractEdge();
      } else {
        return null;
      }

      if (state1 instanceof NonAbstractionState) {
        return new PredicateNonAbstractionStateWithEdge(state1, edge);
      } else if (state1 instanceof AbstractionState) {
        return new PredicateAbstractionStateWithEdge(state1, edge);
      } else {
        throw new UnsupportedOperationException("Unknown abstract state: " + state1.getClass());
      }
    }
    return null;
  }

  private boolean compatible(PredicateAbstractState pState1, PredicateProjectedState pState2) {

    BooleanFormula stateFormula = pState1.getAbstractionFormula().asFormula();
    BooleanFormula objectFormula = pState2.getGuard();

    BooleanFormula f = mngr.and(stateFormula, objectFormula);

    try {
      return !solver.isUnsat(f);

    } catch (SolverException | InterruptedException e) {
      Preconditions.checkArgument(false);
    }
    return false;
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild) {
    throw new UnsupportedOperationException(
        "Projection without edge is not supported for Predicate Analysis");
  }

  private AbstractEdge prepareEdge(AbstractEdge pEdge) {
    CStatement stmnt;

    if (pEdge == EmptyEdge.getInstance()) {
      return EmptyEdge.getInstance();
    }
    CFAEdge edge = ((WrapperCFAEdge) pEdge).getCFAEdge();

    creationTimer.start();
    if (edge instanceof CFunctionReturnEdge) {
      CFunctionSummaryEdge summary = ((CFunctionReturnEdge) edge).getSummaryEdge();
      stmnt = summary.getExpression();
    } else if (edge instanceof CStatementEdge) {
      stmnt = ((CStatementEdge) edge).getStatement();
    } else {
      creationTimer.stop();
      return EmptyEdge.getInstance();
    }

    if (stmnt instanceof CAssignment) {
      CExpression exp = ((CAssignment) stmnt).getLeftHandSide();

      ExpressionTransformer transformer = new ExpressionTransformer(edge);
      Pair<CExpression, Boolean> left = exp.accept(transformer);

      if (left.getSecond()) {

        CRightHandSide right = ((CAssignment) stmnt).getRightHandSide();
        CAssignment newAssignement;

        if (havocAbstraction2) {
          creationTimer.stop();
          return PredicateAbstractEdge.getHavocEdgeInstance();
        } else if (useUndefFuncs
            || (stmnt instanceof CFunctionCallAssignmentStatement
                && edge instanceof CStatementEdge)) {
          CFunctionCallExpression fExp = prepareUndefFunctionFor(right);

          newAssignement =
              new CFunctionCallAssignmentStatement(
                  fExp.getFileLocation(),
                  (CLeftHandSide) left.getFirst(),
                  fExp);
        } else {
          Pair<CExpression, Boolean> newRight = right.accept(transformer);

          newAssignement =
              new CExpressionAssignmentStatement(
                  newRight.getFirst().getFileLocation(),
                  (CLeftHandSide) left.getFirst(),
                  newRight.getFirst());
        }
        creationTimer.stop();

        convertingTimer.start();
        AFunctionDeclaration func = edge.getSuccessor().getFunction();
        CFAEdge fakeEdge =
            new CStatementEdge(
                "environment",
                newAssignement,
                newAssignement.getFileLocation(),
                new CFANode(func),
                new CFANode(func));

        PathFormula pFormula = pmngr.makeEmptyPathFormula();

        try {
          pFormula = pmngr.makeAnd(pFormula, fakeEdge);
        } catch (CPATransferException e) {
          return EmptyEdge.getInstance();
        } catch (InterruptedException e) {
          return EmptyEdge.getInstance();
        }
        convertingTimer.stop();

        if (mngr.isTrue(pFormula.getFormula())) {
          return EmptyEdge.getInstance();
        }


        FormulaDescription desc =
            new FormulaDescription(newAssignement, pFormula.getFormula(), getTypes(pFormula));

        return new PredicateAbstractEdge(Collections.singleton(desc));
      }
    }
    // TODO Assumptions!
    creationTimer.stop();
    return EmptyEdge.getInstance();
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild, AbstractEdge pEdge) {
    AbstractEdge newEdge = prepareEdge(pEdge);
    PredicateAbstractState parent = (PredicateAbstractState) pParent;
    BooleanFormula guard = prepareFormula(parent.getAbstractionFormula().asFormula());

    return new PredicateProjectedState(newEdge, guard);
  }

  static CFunctionCallExpression prepareUndefFunctionFor(CRightHandSide right) {

    FileLocation loc = right.getFileLocation();
    CType type = right.getExpressionType();

    CFunctionType fType =
        new CFunctionType(right.getExpressionType(), ImmutableList.of(), false);
    String retType = fType.getReturnType().getCanonicalType().toString();
    retType = retType.replace(" ", "_").replace("(", "").replace(")", "").replace("*", "_p_");
    CFunctionDeclaration funcDecl =
        new CFunctionDeclaration(
            loc,
            fType,
            "__VERIFIER_nondet_" + retType,
            ImmutableList.of());
    CExpression name = new CIdExpression(loc, funcDecl);

    return new CFunctionCallExpression(loc, type, name, ImmutableList.of(), funcDecl);
  }

  private BooleanFormula prepareFormula(BooleanFormula formula) {
    if (emptyAbstraction) {
      return mngr.makeTrue();
    }
    BooleanFormula result = fmngr.renameFreeVariablesAndUFs(formula, this::localRename);
    return result;
  }

  @Override
  public boolean isInvariantToEffects(AbstractState pState) {
    PredicateAbstractState state = (PredicateAbstractState) pState;
    if (mngr.isTrue(state.getAbstractionFormula().asFormula())) {
      return true;
    }
    return false;
  }

  public Map<String, CType> getTypes(PathFormula pFormula) {
    Map<String, CType> result = new TreeMap<>();
    BooleanFormula bFormula = pFormula.getFormula();
    SSAMap ssa = pFormula.getSsa();
    Collection<String> varsAndUFs = fmngr.extractFunctionNames(bFormula);

    for (String var : varsAndUFs) {
      Pair<String, OptionalInt> parsed = FormulaManagerView.parseName(var);
      String varName = parsed.getFirst();
      CType type = ssa.getType(varName);
      if (type != null) {
        // Type may be null, if it is ADRESS_OF, thus it type does not need
        result.put(varName, type);
      }
    }
    return result;
  }

  @Override
  public boolean canBeAnythingApplied(AbstractState pState) {
    return true;
  }

}
