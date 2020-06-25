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
package org.sosy_lab.cpachecker.cpa.sl;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.sl.SLState.SLStateError;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SLFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;

public class SLHeapDelegateImpl implements SLHeapDelegate, SLFormulaBuilder {

  private final LogManager logger;
  private final MachineModel machineModel;
  private final Solver solver;
  private final PathFormulaManager pfm;

  private final FormulaManagerView fm;
  // private final IntegerFormulaManager ifm;
  private final SLFormulaManager slfm;
  private final BooleanFormulaManager bfm;
  private final BitvectorFormulaManager bvfm;

  private SLState state;
  private PathFormula predPathFormula;
  private CFAEdge edge;

  private final BitvectorType heapAddressFormulaType;
  private final BitvectorType heapValueFormulaType;
  // private final FormulaType<IntegerFormula> heapValueFormulaType = FormulaType.IntegerType;
  // private final FormulaType<IntegerFormula> heapAddressFormulaType = FormulaType.IntegerType;

  private final Formula NOT_INITIALIZED;
  // private final Formula UNKNOWN;

  public SLHeapDelegateImpl(
      LogManager pLogger,
      Solver pSolver,
      MachineModel pMachineModel,
      PathFormulaManager pPfm) {
    logger = pLogger;
    solver = pSolver;
    pfm = pPfm;
    machineModel = pMachineModel;
    fm = solver.getFormulaManager();
    // ifm = fm.getIntegerFormulaManager();
    slfm = fm.getSLFormulaManager();
    bfm = fm.getBooleanFormulaManager();
    bvfm = fm.getBitvectorFormulaManager();

    heapAddressFormulaType =
        FormulaType.getBitvectorTypeWithSize(machineModel.getSizeofPtrInBits());
    heapValueFormulaType = FormulaType.getBitvectorTypeWithSize(machineModel.getSizeofCharInBits());
    NOT_INITIALIZED = bvfm.makeVariable(heapValueFormulaType, "__null");
    // UNKNOWN = slfm.makeNilElement(heapValueFormulaType);
  }

  @Override
  public void handleMalloc(CExpression pMemoryLocation, CExpression pSize)
      throws Exception {
    Formula loc = getFormulaForExpression(pMemoryLocation, false);
    BigInteger size = getValueForCExpression(pSize, true);
    addToMemory(state.getHeap(), loc, size);
  }

  @Override
  public SLStateError
      handleRealloc(
          CExpression pNewMemoryLocation,
          CExpression pOldMemoryLocation,
          CExpression pSize)
          throws Exception {
    final Formula loc = getFormulaForExpression(pNewMemoryLocation, false);
    final Formula oldLoc = getFormulaForExpression(pOldMemoryLocation, true);
    final BigInteger size = getValueForCExpression(pSize, true);

    Formula f = checkAllocation(state.getHeap(), oldLoc, null, true);
    if (f == null) {
      logger.log(Level.SEVERE, "REALLOC() failed - address not allocated.");
      return SLStateError.INVALID_READ;
    }
    removeFromMemory(state.getHeap(), oldLoc);
    addToMemory(state.getHeap(), loc, size);
    return null;
  }

  @Override
  public void handleCalloc(CExpression pMemoryLocation, CExpression pNum, CExpression pSize)
      throws Exception {
    final Formula loc = getFormulaForExpression(pMemoryLocation, false);
    final BigInteger num = getValueForCExpression(pNum, true);
    final BigInteger size = getValueForCExpression(pSize, false);
    addToMemory(state.getHeap(), loc, num.multiply(size), true);
  }

  @Override
  public void handleAlloca(CExpression pMemoryLocation, CExpression pSize) throws Exception {
    Formula loc = getFormulaForExpression(pMemoryLocation, false);
    BigInteger size = getValueForCExpression(pSize, true);
    addToMemory(state.getStack(), loc, size);
  }

  @Override
  public SLStateError handleFree(CExpression pLocation)
      throws Exception {
    Formula loc =
        checkAllocation(state.getHeap(), getFormulaForExpression(pLocation, true), null, true);
    if (loc == null) {
      return SLStateError.INVALID_FREE;
    }
    if (state.getAllocationSizes().get(loc) == null) {
      return SLStateError.INVALID_FREE;
    }
    removeFromMemory(state.getHeap(), loc);
    return null;
  }

  @Override
  public void handleDeclaration(CVariableDeclaration pDecl) throws Exception {
    String name = pDecl.getQualifiedName();
    // ignore non-program variables, e.g. from non-det functions.
    if (!pDecl.getName().startsWith("__") && !state.getDeclarations().add(pDecl)) {
      incSSAIndex(name);
    }
    CType type = pDecl.getType();
    BigInteger length = BigInteger.ONE;
    Formula f;
    if (type instanceof CArrayType || type instanceof CPointerType) {
      state.addInScopePtr(pDecl);

      if (type instanceof CArrayType) {
        CArrayType aType = (CArrayType) type;
        CType arrayType = aType.getType();
        type = aType.asPointerType();
        OptionalInt s = aType.getLengthAsInt();
        length =
            s.isPresent()
                ? BigInteger.valueOf(s.getAsInt())
                : getValueForCExpression(aType.getLength(), true);

        f = getFormulaForDeclaration(pDecl);
        addToMemory(state.getStack(), f, machineModel.getSizeof(arrayType).multiply(length));
      }
    }
    CExpression e = createSymbolicLocation(pDecl);
    f = getFormulaForExpression(e, false);

    addToMemory(state.getStack(), f, machineModel.getSizeof(type));
    CInitializer init = pDecl.getInitializer();
    if (init != null) {
      Formula val = NOT_INITIALIZED;
      if (init instanceof CInitializerExpression) {
        val =
            getFormulaForExpression(((CInitializerExpression) init).getExpression(), true);
        updateMemory(state.getStack(), f, val);
      } else if (init instanceof CInitializerList) {
        // TODO
        // CInitializerList iList = (CInitializerList) init;
        // for (CInitializer i : iList.getInitializers()) {
        //
        // }
      }
    }
  }

  @Override
  public SLStateError handleDereference(CExpression pExp, CExpression pOffset) throws Exception {
    Formula loc = createFormula(pExp, pOffset, true);
    CType type = ((CPointerType) pExp.getExpressionType()).getType();
    for (int i = 0; i < machineModel.getSizeof(type).intValueExact(); i++) {
      final Formula f = fm.makePlus(loc, fm.makeNumber(loc, Rational.of(i)));
      if (!isAllocated(f, true)) {
        return SLStateError.INVALID_READ;
      }
    }
    return null;
  }

  @Override
  public SLStateError
      handleDereferenceAssignment(CExpression pLHS, CExpression pOffset, CRightHandSide pRHS)
          throws Exception {
    // return handleDereference(pLHS, pOffset);
    final Formula loc = createFormula(pLHS, pOffset, true);

    CExpression rhs = null;
    Formula val;
    if (pRHS instanceof CFunctionCallExpression) {
      CFunctionCallExpression e = (CFunctionCallExpression) pRHS;
      CIdExpression idExp = (CIdExpression) e.getFunctionNameExpression();
      // val = getFormulaForDeclaration(e.getDeclaration());
      val = getFormulaForVariableName(idExp.getName(), false);

    } else {
      rhs = (CExpression) pRHS;
      val = getFormulaForExpression(rhs, true);
    }
    CType type = ((CPointerType) pLHS.getExpressionType()).getType();

    for (int i = 0; i < machineModel.getSizeof(type).intValueExact(); i++) {
      final Formula f = fm.makePlus(loc, fm.makeNumber(loc, Rational.of(i)));
      final Formula match = checkAllocation(f, val, true);
      if (match == null) {
        return SLStateError.INVALID_WRITE;
      }
    }
    return null;
  }


  @Override
  public SLStateError handleOutOfScopeVariable(CSimpleDeclaration pDecl) throws Exception {
    state.removeInScopePtr(pDecl);
    CType type = pDecl.getType();
    CExpression e = createSymbolicLocation(pDecl);
    Formula loc = getFormulaForExpression(e, false);
    removeFromMemory(state.getStack(), loc);
    if (type instanceof CArrayType) {
      removeFromMemory(state.getStack(), getFormulaForDeclaration(pDecl));
    }
    if (!(type instanceof CPointerType || type instanceof CArrayType)) {
      return null;
    }

    loc = getFormulaForVariableName(pDecl.getQualifiedName(), false);
    // check heap ptr alias
    Formula match = checkAllocation(state.getHeap(), loc, null, false);
    if (match != null) {
      // Check if a copy of the dropped heap pointer exists.
      for (CSimpleDeclaration ptr : state.getInScopePtrs()) {
        Formula tmp = getFormulaForVariableName(ptr.getQualifiedName(), false);
        if (checkEquivalence(loc, tmp, getPathFormula())) {
          return null;
        }
      }
      return SLStateError.MEMORY_LEAK;
    }
    return null;
  }

  private void removeFromMemory(Map<Formula, Formula> pMemory, Formula pMemoryLocation) {
    BigInteger size = state.getAllocationSizes().get(pMemoryLocation);
    for (int i = 0; i < size.intValueExact(); i++) {
      if (i == 0) {
        pMemory.remove(pMemoryLocation);
      } else {
        Formula tmp = fm.makePlus(pMemoryLocation, fm.makeNumber(pMemoryLocation, Rational.of(i)));
        pMemory.remove(tmp);
      }
    }
  }

  private Formula checkAllocation(Formula fLoc, Formula pVal, boolean usePredContext) {
    Timer timer = new Timer();
    timer.start();
    Formula res = checkAllocation(state.getHeap(), fLoc, pVal, usePredContext);
    if (res == null) {
      res = checkAllocation(state.getStack(), fLoc, pVal, usePredContext);
    }
    timer.stop();
    logger.log(Level.INFO, "Solvingtime_old: " + timer.toString());

    return res;
  }

  private Formula checkAllocation(
      Map<Formula, Formula> pMemory,
      Formula fLoc,
      Formula pVal,
      boolean usePredContext) {
    PathFormula context = usePredContext ? getPredPathFormula() : getPathFormula();
    // Syntactical check for performance.
    if (pMemory.containsKey(fLoc)) {
      if (pVal != null) {
        pMemory.put(fLoc, pVal);
      }
      return fLoc;
    }
    // Semantical check.
    for (Formula formulaOnHeap : pMemory.keySet()) {
      if (checkEquivalence(fLoc, formulaOnHeap, context)) {
        if (pVal != null) {
          pMemory.put(formulaOnHeap, pVal);
        }
        return formulaOnHeap;
      }
    }
    return null;
  }

  private void addToMemory(
      Map<Formula, Formula> pMemory,
      Formula pMemoryLocation,
      BigInteger pSize) {
    addToMemory(pMemory, pMemoryLocation, pSize, false);
  }

  private void addToMemory(
      Map<Formula, Formula> pMemory,
      Formula pMemoryLocation,
      BigInteger pSize,
      boolean initWithZero) {
    for (int i = 0; i < pSize.intValueExact(); i++) {
      Formula f = pMemoryLocation;
      if (i > 0) {
        // heap of bytes/chars.
        f = fm.makePlus(f, fm.makeNumber(f, Rational.of(i)));
      } else {
        state.getAllocationSizes().put(f, pSize);
      }
      pMemory.put(f, initWithZero ? fm.makeNumber(heapValueFormulaType, 0) : NOT_INITIALIZED);
    }
  }

  private void updateMemory(Map<Formula, Formula> pMemory, Formula pLoc, Formula pVal) {
    pMemory.put(pLoc, pVal);
  }

  // private void updateMemory(Formula pLoc, Formula pVal) {
  // if (heap.containsKey(pLoc)) {
  // heap.put(pLoc, pVal);
  // } else if (stack.containsKey(pLoc)) {
  // stack.put(pLoc, pVal);
  // }
  // }

  private boolean isAllocated(Formula pLoc, boolean usePredContext)
      throws Exception {
    return isAllocated(pLoc, usePredContext, state.getStack())
        || isAllocated(pLoc, usePredContext, state.getHeap());
  }

  private boolean isAllocated(Formula pLoc, boolean usePredContext, Map<Formula, Formula> pHeap) {
    PathFormula context = usePredContext ? getPredPathFormula() : getPathFormula();
    BooleanFormula heapFormula = createHeapFormula(pHeap);
    BooleanFormula toBeChecked = slfm.makePointsTo(pLoc, NOT_INITIALIZED); // TODO implement
                                                                           // wildcard
    try (ProverEnvironment prover =
        solver.newProverEnvironment(ProverOptions.ENABLE_SEPARATION_LOGIC)) {
      prover.addConstraint(context.getFormula());
      prover.addConstraint(slfm.makeStar(toBeChecked, heapFormula));
      return prover.isUnsat();
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return false;
  }

  // /***
  // * Generates a SL heap formula.
  // *
  // * @return Formula - key0->val0 * key1->val1 * ... * keyX->valX
  // */
  //
  // private BooleanFormula createHeapFormula() {
  // BooleanFormula stackFormula = createHeapFormula(stack);
  // BooleanFormula heapFormula = createHeapFormula(heap);
  // return slfm.makeStar(stackFormula, heapFormula);
  // }

  private BooleanFormula createHeapFormula(Map<Formula, Formula> pHeap) {
    BooleanFormula formula = slfm.makeEmptyHeap(heapAddressFormulaType, heapValueFormulaType);
    for (Formula f : pHeap.keySet()) {
      Formula target = pHeap.get(f);
      BooleanFormula ptsTo = slfm.makePointsTo(f, target);
      formula = slfm.makeStar(formula, ptsTo);
    }
    return formula;
  }

  private boolean checkEquivalence(Formula pF0, Formula pF1, PathFormula pContext) {
    if (!fm.getFormulaType(pF0).equals(fm.getFormulaType(pF1))) {
      return false;
    }
    BooleanFormula tmp = fm.makeEqual(pF0, pF1);
    try (ProverEnvironment prover = solver.newProverEnvironment()) {
      prover.addConstraint(pContext.getFormula());
      prover.addConstraint(tmp);
      if (prover.isUnsat()) {
        return false;
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    // Check tautology.
    try (ProverEnvironment prover = solver.newProverEnvironment()) {
      prover.addConstraint(pContext.getFormula());
      prover.addConstraint(bfm.not(tmp));
      return prover.isUnsat();
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return false;
  }

  private CExpression createSymbolicLocation(CSimpleDeclaration pDecl) {

    CIdExpression e = new CIdExpression(FileLocation.DUMMY, pDecl);
    CType t = new CPointerType(false, false, pDecl.getType());
    return new CUnaryExpression(FileLocation.DUMMY, t, e, UnaryOperator.AMPER);
  }

  private Formula createFormula(CExpression pExp, CExpression pOffset, boolean usePredContext)
      throws Exception {
    Formula loc = getFormulaForExpression(pExp, usePredContext);
    if (pOffset != null) {
      Formula offset = getFormulaForExpression(pOffset, true); // always pred ssa index.
      CPointerType type;
      CType t = pExp.getExpressionType();
      if (t instanceof CArrayType) {
        type = ((CArrayType) t).asPointerType();
      } else {
        type = (CPointerType) pExp.getExpressionType();
      }
      BigInteger typeSize = machineModel.getSizeof(type.getType());
      if (typeSize.longValue() > 1) {
        offset = fm.makeMultiply(offset, fm.makeNumber(offset, Rational.ofBigInteger(typeSize)));
      }
      loc = fm.makePlus(loc, offset);
    }
    return loc;
  }

  private void incSSAIndex(String pVar) {
    SSAMap ssaMap = getPathFormula().getSsa();
    int scopeIndex = pVar.indexOf("::") + 2;
    String name = pVar.substring(0, scopeIndex) + "&" + pVar.substring(scopeIndex, pVar.length());
    CType type = ssaMap.getType(name);
    int index = ssaMap.getIndex(name) + 1;
    SSAMapBuilder b = ssaMap.builder();
    updateSSAMap(b.setIndex(name, type, index).build());
  }

  @Override
  public BigInteger getValueForCExpression(CExpression pExp, boolean usePredContext)
      throws Exception {
    PathFormula context = usePredContext ? getPredPathFormula() : state.getPathFormula();
    Formula f = pfm.expressionToFormula(context, pExp, edge);
    final String dummyVarName = "0_allocationSize"; // must not be a valid C variable name.
    f = fm.makeEqual(bvfm.makeVariable(32, dummyVarName), f);

    try (ProverEnvironment env = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      env.addConstraint(context.getFormula());
      env.addConstraint((BooleanFormula) f);
      if (!env.isUnsat()) {
        List<ValueAssignment> assignments = env.getModelAssignments();
        for (ValueAssignment a : assignments) {
          if (a.getKey().toString().equals(dummyVarName)) {
            return (BigInteger) a.getValue();
          }
        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    logger.log(
        Level.SEVERE,
        "Numeric value of expression " + pExp.toString() + " could not be determined.");
    return null;
  }

  @Override
  public Formula getFormulaForVariableName(String pVariable, boolean usePredContext) {
    PathFormula context = usePredContext ? getPredPathFormula() : state.getPathFormula();
    // String var = addFctScope ? functionName + "::" + pVariable : pVariable;
    CType type = context.getSsa().getType(pVariable);
    return pfm.makeFormulaForVariable(context, pVariable, type);
  }

  @Override
  public Formula getFormulaForDeclaration(CSimpleDeclaration pDecl) {
    CType type = pDecl.getType();
    return pfm.makeFormulaForVariable(state.getPathFormula(), pDecl.getQualifiedName(), type);
  }

  @Override
  public Formula getFormulaForExpression(CExpression pExp, boolean usePredContext)
      throws UnrecognizedCodeException {
    PathFormula context = usePredContext ? getPredPathFormula() : state.getPathFormula();
    return pfm.expressionToFormula(context, pExp, edge);
  }

  @Override
  public PathFormula getPathFormula() {
    return state.getPathFormula();
  }

  @Override
  public PathFormula getPredPathFormula() {
    return predPathFormula;
  }

  @SuppressWarnings("deprecation")
  @Override
  public void updateSSAMap(SSAMap pMap) {
    state.setPathFormula(pfm.makeNewPathFormula(state.getPathFormula(), pMap)); // TODO
  }

  @Override
  public void setContext(SLState pState, CFAEdge pEdge) {
    state = pState;
    edge = pEdge;
    predPathFormula = state.getPathFormula();
    try {
      PathFormula pathFormula = pfm.makeAnd(getPredPathFormula(), pEdge);
      state.setPathFormula(pathFormula);
    } catch (CPATransferException | InterruptedException e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
  }

  @Override
  public void clearContext() {
    state = null;
    predPathFormula = null;
    edge = null;

  }
}
