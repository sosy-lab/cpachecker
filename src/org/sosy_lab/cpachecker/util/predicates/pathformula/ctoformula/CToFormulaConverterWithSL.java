// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializers;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.sl.SLMemoryDelegate;
import org.sosy_lab.cpachecker.cpa.sl.SLState;
import org.sosy_lab.cpachecker.cpa.sl.SLStatistics;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder.DummyPointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryError;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

/**
 * Converts a CExpression to a formula representing the memory location.
 */
@Options(prefix = "cpa.sl")
public final class CToFormulaConverterWithSL extends CtoFormulaConverter {

  private final Solver solver;
  private final SLStatistics stats;
  @Option(
    secure = true,
    description = "States whether allocation checks are solved with a SMT or SL solver. "
        + "Further a SMT check is divided into multiple equialence checks for each key "
        + "on the heap (SMT) or a single solver call based on model generation "
        + "(SMT_MODELSAT). For the SL approach, the solver chosen by the option "
        + "solver.solver has to support SL.")
  private AllocationCheckProcedure allocationCheckProcedure = AllocationCheckProcedure.SL;

  public enum AllocationCheckProcedure {
    SL,
    SMT,
    SMT_MODELSAT
  }

  private SLMemoryDelegate delegate;

  public CToFormulaConverterWithSL(
      Solver pSolver,
      SLStatistics pStats,
      MachineModel pMachineModel,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      AnalysisDirection pDirection,
      Configuration pConfig)
      throws InvalidConfigurationException {
    super(
        new FormulaEncodingOptions(pConfig),
        pSolver.getFormulaManager(),
        pMachineModel,
        Optional.empty(),
        pLogger,
        pShutdownNotifier,
        new CtoFormulaTypeHandler(pLogger, pMachineModel),
        pDirection);
    pConfig.inject(this);
    solver = pSolver;
    stats = pStats;
  }

  @Override
  protected CRightHandSideVisitor<Formula, UnrecognizedCodeException> createCRightHandSideVisitor(
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts,
      Constraints pConstraints,
      ErrorConditions pErrorConditions) {
    return new SLRhsToFormulaVisitor(
        this,
        fmgr,
        pEdge,
        pFunction,
        pSsa,
        delegate,
        pConstraints);
  }

  @Override
  protected CLeftHandSideVisitor<Formula, UnrecognizedCodeException> createCLeftHandSideVisitor(
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts,
      Constraints pConstraints,
      ErrorConditions pErrorConditions) {
    return new SLLhsToFormulaVisitor(
        this,
        pEdge,
        pFunction,
        pSsa,
        pConstraints,
        pErrorConditions,
        solver.getFormulaManager(),
        delegate);
  }


  private SLMemoryDelegate makeDelegate(SLState pContext) {
    assert pContext != null;
    return new SLMemoryDelegate(
        solver,
        pContext,
        machineModel,
        logger,
        stats,
        allocationCheckProcedure);
  }

  public SLState makeAnd(SLState pState, CFAEdge pEdge)
      throws UnrecognizedCodeException, UnrecognizedCFAEdgeException, InterruptedException {
    delegate = makeDelegate(pState);
    String function =
        (pEdge.getPredecessor() != null) ? pEdge.getPredecessor().getFunctionName() : null;

    SSAMapBuilder ssa = pState.getSsaMap().builder();

    // Handle the edge. Side-effects on SLMemoryDelegate context.
    createFormulaForEdge(
        pEdge,
        function,
        ssa,
        DummyPointerTargetSetBuilder.INSTANCE,
        new Constraints(bfmgr),
        ErrorConditions.dummyInstance(bfmgr));

    // Apply SSA changes before OutOfScope check
    SSAMap newSsa = ssa.build();
    SLState newState = new SLState.Builder(delegate.getState(), true).ssaMap(newSsa).build();
    delegate = makeDelegate(newState);

    // OutOfScope check.
    for (CSimpleDeclaration v : pEdge.getSuccessor().getOutOfScopeVariables()) {
      if (v instanceof CVariableDeclaration && !((CVariableDeclaration) v).isGlobal()) {
        handleOutOfScopeVar(pEdge, delegate, (CVariableDeclaration) v, ssa);
      }
    }
    return delegate.getState();
  }

  private void handleOutOfScopeVar(
      CFAEdge pEdge,
      SLMemoryDelegate pDelegate,
      CVariableDeclaration pVar,
      SSAMapBuilder pSsa)
      throws UnrecognizedCodeException {

    CType type = pVar.getType();
    CType t = pDelegate.makeLocationTypeForVariableType(type);
    Formula var =
        makeVariable(UnaryOperator.AMPER.getOperator() + pVar.getQualifiedName(), t, pSsa);

    boolean success = pDelegate.handleOutOfScopeVar(var, pVar.getType());
    if (!success) {
      throw new UnrecognizedCodeException("Could not deallocate " + var, pEdge);
    }
  }

  @Override
  protected BooleanFormula makeDeclaration(
      CDeclarationEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts,
      Constraints pConstraints,
      ErrorConditions pErrorConditions)
      throws UnrecognizedCodeException, InterruptedException {
    if (!(pEdge.getDeclaration() instanceof CVariableDeclaration)) {
      // struct prototype, function declaration, typedef etc.
      logger.log(Level.FINEST, pEdge, "Ignoring declaration");
      return bfmgr.makeTrue();
    }
    CVariableDeclaration decl = (CVariableDeclaration) pEdge.getDeclaration();
    final String varName = decl.getQualifiedName();

    CType type = decl.getType();
    String varNameWithAmper = UnaryOperator.AMPER.getOperator() + varName;
    CType t = delegate.makeLocationTypeForVariableType(type);
    Formula var = makeFreshVariable(varNameWithAmper, t, pSsa);
    delegate.handleVarDeclaration(var, type);

    // Initializer
    for (CAssignment assignment : CInitializers.convertToAssignments(decl, pEdge)) {
      makeAssignment(
          assignment.getLeftHandSide(),
          assignment.getLeftHandSide(),
          assignment.getRightHandSide(),
          pEdge,
          pFunction,
          pSsa,
          pPts,
          pConstraints,
          pErrorConditions);
    }
    return bfmgr.makeTrue();
  }

  @Override
  protected BooleanFormula makeAssignment(
      CLeftHandSide lhs,
      CLeftHandSide lhsForChecking,
      CRightHandSide rhs,
      CFAEdge edge,
      String function,
      SSAMapBuilder ssa,
      PointerTargetSetBuilder pts,
      Constraints constraints,
      ErrorConditions errorConditions)
      throws UnrecognizedCodeException, InterruptedException {
    if (!isRelevantLeftHandSide(lhsForChecking)) {
      // Optimization for unused variables and fields
      return bfmgr.makeTrue();
    }

    CType lhsType = lhs.getExpressionType().getCanonicalType();

    if (lhsType instanceof CArrayType) {
      // Probably a (string) initializer, ignore assignments to arrays
      // as they cannot behandled precisely anyway.
      return bfmgr.makeTrue();
    }

    if (rhs instanceof CExpression) {
      rhs = makeCastFromArrayToPointerIfNecessary((CExpression) rhs, lhsType);
    }

    Formula l = null, r = null;
    if (direction == AnalysisDirection.BACKWARD) {
      l = buildLvalueTerm(lhs, edge, function, ssa, pts, constraints, errorConditions);
      r = buildTerm(rhs, edge, function, ssa, pts, constraints, errorConditions);
    } else {
      r = buildTerm(rhs, edge, function, ssa, pts, constraints, errorConditions);
      l = buildLvalueTerm(lhs, edge, function, ssa, pts, constraints, errorConditions);
    }
    if (l == null) {
      return bfmgr.makeFalse();
    }

    r = makeCast(rhs.getExpressionType(), lhsType, r, constraints, edge);

    // Skip main return statement assignment.
    if (!(edge instanceof CReturnStatementEdge && function.equals("main"))) {
      if (!delegate.dereferenceAssign(l, r, getSizeof(lhsType))) {
        delegate.addError(MemoryError.INVALID_WRITE);
      }
    }
    return bfmgr.makeTrue();
  }

  @Override
  protected BooleanFormula makePredicate(
      CExpression pExp,
      boolean pIsTrue,
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts,
      Constraints pConstraints,
      ErrorConditions pErrorConditions)
      throws UnrecognizedCodeException, InterruptedException {
    BooleanFormula constraint =
        super.makePredicate(
            pExp,
            pIsTrue,
            pEdge,
            pFunction,
            pSsa,
            pPts,
            pConstraints,
            pErrorConditions);
    SLState context = delegate.getState();
    context = new SLState.Builder(context, true).addConstraint(constraint).build();
    delegate = makeDelegate(context);
    return constraint;
  }

  @Override
  protected BooleanFormula makeFunctionCall(
      CFunctionCallEdge edge,
      String callerFunction,
      SSAMapBuilder ssa,
      PointerTargetSetBuilder pts,
      Constraints constraints,
      ErrorConditions errorConditions)
      throws UnrecognizedCodeException, InterruptedException {
    List<CExpression> actualParams = edge.getArguments();
    CFunctionEntryNode fn = edge.getSuccessor();
    List<CParameterDeclaration> formalParams = fn.getFunctionParameters();
    if (formalParams.size() != actualParams.size()) {
      throw new UnrecognizedCodeException(
          "Number of parameters on function call does " + "not match function definition",
          edge);
    }

    for (int i = 0; i < formalParams.size(); i++) {
      CParameterDeclaration param = formalParams.get(i);
      String varNameWithAmper = UnaryOperator.AMPER.getOperator() + param.getQualifiedName();
      CType t = delegate.makeLocationTypeForVariableType(param.getType());
      Formula var = makeFreshVariable(varNameWithAmper, t, ssa);
      CType paramType = param.getType();
      if (paramType.isIncomplete()) {
        paramType = actualParams.get(i).getExpressionType();
      }
      delegate.handleVarDeclaration(var, paramType);

      CIdExpression lhs = new CIdExpression(param.getFileLocation(), param);
      makeAssignment(
          lhs,
          lhs,
          actualParams.get(i),
          edge,
          callerFunction,
          ssa,
          pts,
          constraints,
          errorConditions);
    }
    // Retval allocation??
    com.google.common.base.Optional<CVariableDeclaration> ret = fn.getReturnVariable();
    if (ret.isPresent()) {
      CVariableDeclaration decl = ret.get();
      String varNameWithAmper = UnaryOperator.AMPER.getOperator() + decl.getQualifiedName();
      CType t = delegate.makeLocationTypeForVariableType(decl.getType());
      Formula var = makeFreshVariable(varNameWithAmper, t, ssa);
      delegate.handleVarDeclaration(var, decl.getType());
    }

    return bfmgr.makeTrue();
  }

  @Override
  protected BooleanFormula makeExitFunction(
      CFunctionSummaryEdge pCe,
      String pCalledFunction,
      SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts,
      Constraints pConstraints,
      ErrorConditions pErrorConditions)
      throws UnrecognizedCodeException, InterruptedException {
    BooleanFormula res =
        super.makeExitFunction(pCe, pCalledFunction, pSsa, pPts, pConstraints, pErrorConditions);

    // Deallocate parameters
    CFunctionEntryNode fe = pCe.getFunctionEntry();
    for (CParameterDeclaration param : fe.getFunctionParameters()) {
      String varNameWithAmper = UnaryOperator.AMPER.getOperator() + param.getQualifiedName();
      CType t = delegate.makeLocationTypeForVariableType(param.getType());
      Formula var = makeVariable(varNameWithAmper, t, pSsa);
      delegate.deallocateFromStack(var, false);
    }
    // Deallocate temp return variable.
    com.google.common.base.Optional<CVariableDeclaration> ret = fe.getReturnVariable();
    if (ret.isPresent()) {
      String varNameWithAmper = UnaryOperator.AMPER.getOperator() + ret.get().getQualifiedName();
      CType t = delegate.makeLocationTypeForVariableType(ret.get().getType());
      Formula var = makeVariable(varNameWithAmper, t, pSsa);
      delegate.deallocateFromStack(var, false);
    }
    // Deallocate alloca segments.
    delegate.releaseAllocas(pCalledFunction);
    return res;
  }
}
