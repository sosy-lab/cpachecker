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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.sl.SLState.SLStateError;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SLFormulaManager;

public class SLHeapDelegateImpl implements SLHeapDelegate {

  private final LogManager logger;
  private final MachineModel machineModel;
  private final Solver solver;
  private final SLFormulaBuilder builder;

  private final FormulaManagerView fm;
  private final BitvectorFormulaManager bvfm;
  private final IntegerFormulaManager ifm;
  private final SLFormulaManager slfm;
  private final BooleanFormulaManager bfm;

  private Map<Formula, Formula> heap = new HashMap<>();
  @Override
  public Map<Formula, Formula> getHeap() {
    return heap;
  }

  private Map<Formula, Formula> stack = new HashMap<>();
  @Override
  public Map<Formula, Formula> getStack() {
    return stack;
  }

  private final Map<Formula, BigInteger> allocationSizes = new HashMap<>();
  private final Set<CSimpleDeclaration> inScopePtrs = new HashSet<>();

  // private final FormulaType<BitvectorFormula> heapValueFormulaType =
//      FormulaType.getBitvectorTypeWithSize(16); // TODO BitVector Support
  private final FormulaType<IntegerFormula> heapValueFormulaType = FormulaType.IntegerType;
  private final FormulaType<IntegerFormula> heapAddresFormulaType = FormulaType.IntegerType;
  private final Formula NOT_INITIALIZED;

  public SLHeapDelegateImpl(
      LogManager pLogger,
      Solver pSolver,
      MachineModel pMachineModel,
      SLFormulaBuilder pBuilder) {
    logger = pLogger;
    solver = pSolver;
    builder = pBuilder;
    machineModel = pMachineModel;
    fm = solver.getFormulaManager();
    bvfm = fm.getBitvectorFormulaManager();
    ifm = fm.getIntegerFormulaManager();
    slfm = fm.getSLFormulaManager();
    bfm = fm.getBooleanFormulaManager();
    NOT_INITIALIZED = ifm.makeVariable("0_null");
  }

  @Override
  public void handleMalloc(CExpression pMemoryLocation, CExpression pSize)
      throws Exception {
    Formula loc = builder.getFormulaForExpression(pMemoryLocation, false);
    BigInteger size = builder.getValueForCExpression(pSize, true);
    addToMemory(heap, loc, size);
  }

  @Override
  public SLStateError
      handleRealloc(
          CExpression pNewMemoryLocation,
          CExpression pOldMemoryLocation,
          CExpression pSize)
          throws Exception {
    final Formula loc = builder.getFormulaForExpression(pNewMemoryLocation, false);
    final Formula oldLoc = builder.getFormulaForExpression(pOldMemoryLocation, true);
    final BigInteger size = builder.getValueForCExpression(pSize, true);

    Formula f = checkAllocation(heap, oldLoc, null, true);
    if (f == null) {
      logger.log(Level.SEVERE, "REALLOC() failed - address not allocated.");
      return SLStateError.INVALID_DEREF;
    }
    removeFromMemory(heap, oldLoc);
    addToMemory(heap, loc, size);
    return null;
  }

  @Override
  public void handleCalloc(CExpression pMemoryLocation, CExpression pNum, CExpression pSize)
      throws Exception {
    final Formula loc = builder.getFormulaForExpression(pMemoryLocation, false);
    final BigInteger num = builder.getValueForCExpression(pNum, true);
    final BigInteger size = builder.getValueForCExpression(pSize, false);
    addToMemory(heap, loc, num.multiply(size), true);
  }

  @Override
  public SLStateError handleFree(CExpression pLocation)
      throws Exception {
    Formula loc =
        checkAllocation(heap, builder.getFormulaForExpression(pLocation, true), null, true);
    if (loc == null) {
      return SLStateError.INVALID_DEREF;
    }
    removeFromMemory(heap, loc);
    return null;
  }

  @Override
  public void handleDeclaration(CVariableDeclaration pDecl) throws Exception {
    CType type = pDecl.getType();
    if (type instanceof CArrayType || type instanceof CPointerType) {
      inScopePtrs.add(pDecl);

      if (type instanceof CArrayType) {
        CArrayType aType = (CArrayType) type;
        type = aType.asPointerType();
        OptionalInt s = aType.getLengthAsInt();
        final BigInteger length =
            s.isPresent()
                ? BigInteger.valueOf(s.getAsInt())
                : builder.getValueForCExpression(aType.getLength(), true);
        final Formula loc =
            builder.getFormulaForVariableName(pDecl.getQualifiedName(), false);
        BigInteger size = length.multiply(machineModel.getSizeof(aType.getType()));
        addToMemory(stack, loc, size);
      }
    }
    CExpression e = SLHeapDelegateImpl.createSymbolicLocation(pDecl);
    Formula f = builder.getFormulaForExpression(e, false);
    addToMemory(stack, f, machineModel.getSizeof(pDecl.getType()));
    CInitializer init = pDecl.getInitializer();
    if (init != null) {
      Formula val = NOT_INITIALIZED;
      if (init instanceof CInitializerExpression) {
        val =
            builder.getFormulaForExpression(((CInitializerExpression) init).getExpression(), true);
      }
      updateMemory(stack, f, val);
    }
    PointerTargetSet pts = builder.getPredPathFormula().getPointerTargetSet();
  }

  @Override
  public SLStateError handleDereference(CExpression pExp, CExpression pOffset) throws Exception {
    Formula loc;
    // if (pExp instanceof CPointerExpression) {
    // loc = createFormula(((CPointerExpression) pExp).getOperand(), pOffset, true);
    // loc = checkAllocation(loc, null, true);
    // if(loc == null) {
    // return SLStateError.INVALID_DEREF;
    // }
    // if(heap.containsKey(loc) ) {
    // loc = heap.get(loc);
    // } else if (stack.containsKey(loc)) {
    //
    // }
    // } else {
      loc = createFormula(pExp, pOffset, true);
      return isAllocated(loc, true) ? null : SLStateError.INVALID_DEREF;
    // }
  }

  @Override
  public SLStateError
      handleDereferenceAssignment(CExpression pLHS, CExpression pOffset, CExpression pRHS)
          throws Exception {
    Formula loc = createFormula(pLHS, pOffset, false);
    Formula val = builder.getFormulaForExpression(pRHS, true);
    return isAllocated(loc, false) ? null : SLStateError.INVALID_DEREF;
    // return checkAllocation(loc, val, false) != null ? null : SLStateError.INVALID_DEREF;
  }

  @Override
  public SLStateError handleOutOfScopeVariable(CSimpleDeclaration pDecl) throws Exception {
    inScopePtrs.remove(pDecl);
    CType type = pDecl.getType();
    CExpression e = SLHeapDelegateImpl.createSymbolicLocation(pDecl);
    Formula loc = builder.getFormulaForExpression(e, false);
    removeFromMemory(stack, loc);
    if (!(type instanceof CPointerType || type instanceof CArrayType)) {
      return null;
    }
    loc = builder.getFormulaForVariableName(pDecl.getQualifiedName(), false);
    if (type instanceof CArrayType) {
      removeFromMemory(stack, loc);
    }
    // check heap ptr alias
    Formula match = checkAllocation(heap, loc, null, false);
    if (match != null) {
      // Check if a copy of the dropped heap pointer exists.
      for (CSimpleDeclaration ptr : inScopePtrs) {
        Formula tmp = builder.getFormulaForVariableName(ptr.getQualifiedName(), false);
        if (checkEquivalence(loc, tmp, builder.getPathFormula())) {
          return null;
        }
      }
      return SLStateError.UNFREED_MEMORY;
    }
    return null;
  }

  private void removeFromMemory(Map<Formula, Formula> pMemory, Formula pMemoryLocation) {
    BigInteger size = allocationSizes.get(pMemoryLocation);
    for (int i = 0; i < size.intValueExact(); i++) {
      if (i == 0) {
        pMemory.remove(pMemoryLocation);
      } else {
        Formula tmp =
            bvfm.add((BitvectorFormula) pMemoryLocation, bvfm.makeBitvector(size.bitLength(), i));
        pMemory.remove(tmp);
      }
    }
  }

  private Formula checkAllocation(
      Formula fLoc,
      Formula pVal,
      boolean usePredContext) {
    Formula res = checkAllocation(heap, fLoc, pVal, usePredContext);
    return res == null ? checkAllocation(stack, fLoc, pVal, usePredContext) : res;
  }

  private Formula checkAllocation(
      Map<Formula, Formula> pMemory,
      Formula fLoc,
      Formula pVal,
      boolean usePredContext) {
    PathFormula context = usePredContext ? builder.getPredPathFormula() : builder.getPathFormula();
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
        f = bvfm.add((BitvectorFormula) f, bvfm.makeBitvector(16, i));
      } else {
        allocationSizes.put(f, pSize);
      }
      pMemory.put(f, initWithZero ? ifm.makeNumber(0) : NOT_INITIALIZED);
    }
  }

  private void updateMemory(Map<Formula, Formula> pMemory, Formula pLoc, Formula pVal) {
    pMemory.put(pLoc, pVal);
  }

  private boolean isAllocated(Formula pLocation, boolean usePredContext)
      throws Exception {
    PathFormula context = usePredContext ? builder.getPredPathFormula() : builder.getPathFormula();
    BooleanFormula heapFormula = getHeapFormula();
    BooleanFormula toBeChecked = slfm.makePointsTo(pLocation, ifm.makeNumber(0));
    try (ProverEnvironment prover = solver.newProverEnvironment()) {
      prover.addConstraint(context.getFormula());
      prover.addConstraint(slfm.makeStar(toBeChecked, heapFormula));
      return prover.isUnsat();
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return false;
  }

  /***
   * Generates a SL heap formula.
   *
   * @return Formula - key0->val0 * key1->val1 * ... * keyX->valX
   */

  private BooleanFormula getHeapFormula() {
    BooleanFormula formula = slfm.makeEmptyHeap(heapAddresFormulaType, heapValueFormulaType);
    Map<Formula, Formula> mem = new HashMap<>(stack);
    mem.putAll(heap);
    for (Formula f : mem.keySet()) {
      Formula target = mem.get(f);
      BooleanFormula ptsTo =
          slfm.makePointsTo(f, target != null ? target : ifm.makeNumber(0));
      formula = slfm.makeStar(formula, ptsTo);
    }
    return formula;
  }

  private boolean checkEquivalence(Formula pF0, Formula pF1, PathFormula pContext) {
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

  private static CExpression createSymbolicLocation(CSimpleDeclaration pDecl) {
    CIdExpression e = new CIdExpression(FileLocation.DUMMY, pDecl);
    return new CUnaryExpression(FileLocation.DUMMY, pDecl.getType(), e, UnaryOperator.AMPER);
  }

  private Formula createFormula(CExpression pExp, CExpression pOffset, boolean usePredContext)
      throws Exception {
    Formula loc = builder.getFormulaForExpression(pExp, usePredContext);
    if (pOffset != null) {
      Formula offset = builder.getFormulaForExpression(pOffset, true); // always pred ssa index.
      loc = fm.makePlus(loc, offset);
    }
    return loc;
  }

}
