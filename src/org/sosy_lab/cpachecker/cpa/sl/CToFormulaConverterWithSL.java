// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.sl;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.sl.SLState.SLStateError;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

/**
 * Converts a CExpression to a formula representing the memory location.
 */
public final class CToFormulaConverterWithSL extends CtoFormulaConverter {

  private Solver solver;

  public CToFormulaConverterWithSL(
      FormulaEncodingOptions pOptions,
      Solver pSolver,
      MachineModel pMachineModel,
      Optional<VariableClassification> pVariableClassification,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CtoFormulaTypeHandler pTypeHandler,
      AnalysisDirection pDirection) {
    super(
        pOptions,
        pSolver.getFormulaManager(),
        pMachineModel,
        pVariableClassification,
        pLogger,
        pShutdownNotifier,
        pTypeHandler,
        pDirection);
    solver = pSolver;
  }

  @Override
  protected CRightHandSideVisitor<Formula, UnrecognizedCodeException> createCRightHandSideVisitor(
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts,
      Constraints pConstraints,
      ErrorConditions pErrorConditions) {
    return new SLRhsToFormulaVisitor(this, fmgr, pEdge, pFunction, pSsa, pPts, pConstraints);
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
        pPts,
        pConstraints,
        pErrorConditions);
  }

  @Override
  protected PointerTargetSetBuilder createPointerTargetSetBuilder(PointerTargetSet pPts) {
    assert context instanceof SLState;
    return makeDelegate();
  }

  private SLMemoryDelegate makeDelegate() {
    assert context != null;
    return new SLMemoryDelegate(solver, (SLState) context, machineModel, logger);
  }

  @Override
  public PathFormula
      makeAnd(PathFormula pOldFormula, CFAEdge pEdge, ErrorConditions pErrorConditions)
          throws UnrecognizedCodeException, UnrecognizedCFAEdgeException, InterruptedException {
    String function =
        (pEdge.getPredecessor() != null) ? pEdge.getPredecessor().getFunctionName() : null;

    SSAMapBuilder ssa = pOldFormula.getSsa().builder();
    Constraints constraints = new Constraints(bfmgr);
    PointerTargetSetBuilder pts = createPointerTargetSetBuilder(pOldFormula.getPointerTargetSet());

    // handle the edge
    BooleanFormula edgeFormula =
        createFormulaForEdge(pEdge, function, ssa, pts, constraints, pErrorConditions);

    edgeFormula = bfmgr.and(edgeFormula, constraints.get());

    SSAMap newSsa = ssa.build();
    PointerTargetSet newPts = pts.build();

    if (bfmgr.isTrue(edgeFormula)
        && (newSsa == pOldFormula.getSsa())
        && newPts.equals(pOldFormula.getPointerTargetSet())) {
      // formula is just "true" and rest is equal
      // i.e. no writes to SSAMap, no branching and length should stay the same
      return pOldFormula;
    }

    BooleanFormula newFormula = bfmgr.makeTrue();
    int newLength = pOldFormula.getLength() + 1;

    PathFormula res = new PathFormula(newFormula, newSsa, newPts, newLength);
    SLState state = (SLState) context;
    state.setPathFormula(res); // Update SSAIndices.
    SLMemoryDelegate delegate = makeDelegate();
    for (CSimpleDeclaration v : pEdge.getSuccessor().getOutOfScopeVariables()) {
      if (v instanceof CVariableDeclaration) {
        handleOutOfScopeVar(pEdge, state, delegate, (CVariableDeclaration) v, ssa);
      }
    }
    return res;
  }

  private void handleOutOfScopeVar(
      CFAEdge pEdge,
      SLState pState,
      SLMemoryDelegate pDelegate,
      CVariableDeclaration pVar,
      SSAMapBuilder pSsa)
      throws UnrecognizedCodeException {

    PathFormula pf = pState.getPathFormula();
    // Remove from stack.
    CUnaryExpression expLoc = createSymbolicLocation(pVar);
    Formula loc = buildTermFromPathFormula(pf, expLoc, pEdge);
    pDelegate.deallocateFromStack(loc);

    // Check for memory leak.
    CType type = pVar.getType();
    if (!(type instanceof CPointerType || type instanceof CArrayType)) {
      return;
    }
    loc = makeVariable(pVar.getQualifiedName(), type, pSsa);
    // Check if the discarded pointer was a heap pointer alias.
    Optional<Formula> match = pDelegate.checkAllocation(loc, getSizeof(type));
    if (match.isPresent()) {
      // Check if another alias exists.
      for (CSimpleDeclaration ptr : pState.getInScopePtrs()) {
        Formula tmp =
            makeFormulaForVariable(
                pf.getSsa(),
                pf.getPointerTargetSet(),
                ptr.getQualifiedName(),
                type);
        if (pDelegate.checkEquivalence(loc, tmp)) {
          return;
        }
      }
      // No alias found => Leak.
      pDelegate.addError(SLStateError.MEMORY_LEAK);
    }

  }

  private CUnaryExpression createSymbolicLocation(CSimpleDeclaration pDecl) {
    CIdExpression e = new CIdExpression(FileLocation.DUMMY, pDecl);
    CType t = new CPointerType(false, false, e.getExpressionType());
    return new CUnaryExpression(FileLocation.DUMMY, t, e, UnaryOperator.AMPER);
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

    SLState state = (SLState) context;
    CType type = decl.getType();
    int size = getSizeof(type);
    if (type instanceof CArrayType || type instanceof CPointerType) {
      state.addInScopePtr(decl);
      if (type instanceof CArrayType) {
        type = ((CArrayType) type).asPointerType();
      }
    }
    String varNameWithAmper = UnaryOperator.AMPER.getOperator() + varName;
    SLMemoryDelegate delegate = makeDelegate();
    CType t = delegate.makeLocationTypeForVariableType(type);
    Formula var = makeFreshVariable(varNameWithAmper, t, pSsa);
    delegate.handleVarDeclaration(var, size);

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

    SLMemoryDelegate delegate = makeDelegate();
    delegate.dereferenceAssign(l, r, getSizeof(lhsType));

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
    ((SLState) context).addConstraint(constraint);
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
    SLMemoryDelegate delegate = makeDelegate();
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
      Formula var = makeFreshVariable(varNameWithAmper, param.getType(), ssa);
      delegate.handleVarDeclaration(var, getSizeof(param.getType()));

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
      Formula var = makeFreshVariable(varNameWithAmper, decl.getType(), ssa);
      delegate.handleVarDeclaration(var, getSizeof(decl.getType()));
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
    SLMemoryDelegate delegate = makeDelegate();
    // Deallocate parameters
    CFunctionEntryNode fe = pCe.getFunctionEntry();
    for (CParameterDeclaration param : fe.getFunctionParameters()) {
      String varNameWithAmper = UnaryOperator.AMPER.getOperator() + param.getQualifiedName();
      Formula var = makeVariable(varNameWithAmper, param.getType(), pSsa);
      delegate.deallocateFromStack(var);
    }
    // Deallocate temp return variable.
    com.google.common.base.Optional<CVariableDeclaration> ret = fe.getReturnVariable();
    if (ret.isPresent()) {
      String varNameWithAmper = UnaryOperator.AMPER.getOperator() + ret.get().getQualifiedName();
      Formula var = makeVariable(varNameWithAmper, ret.get().getType(), pSsa);
      delegate.deallocateFromStack(var);
    }
    // Deallocate alloca segments.
    delegate.releaseAllocas(pCalledFunction);
    return res;
  }


}
