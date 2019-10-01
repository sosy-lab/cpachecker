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

import com.google.common.collect.Iterables;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
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
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
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
  private final Set<String> declarations = new TreeSet<>();

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
    NOT_INITIALIZED = ifm.makeVariable("__null");
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
    String name = pDecl.getQualifiedName();
    if (!declarations.add(name)) {
      incSSAIndex(name);
    }
    CType type = pDecl.getType();
    BigInteger length = BigInteger.ONE;
    Formula f;
    if (type instanceof CArrayType || type instanceof CPointerType) {
      inScopePtrs.add(pDecl);

      if (type instanceof CArrayType) {
        CArrayType aType = (CArrayType) type;
        type = aType.getType();
        OptionalInt s = aType.getLengthAsInt();
        length =
            s.isPresent()
                ? BigInteger.valueOf(s.getAsInt())
                : builder.getValueForCExpression(aType.getLength(), true);

        f = builder.getFormulaForDeclaration(pDecl);
        addToMemory(stack, f, machineModel.getSizeof(type).multiply(length));
      }
    }
    CExpression e = createSymbolicLocation(pDecl);
    f = builder.getFormulaForExpression(e, false);

    addToMemory(stack, f, machineModel.getSizeof(type).multiply(length));
    CInitializer init = pDecl.getInitializer();
    if (init != null) {
      Formula val = NOT_INITIALIZED;
      if (init instanceof CInitializerExpression) {
        val =
            builder.getFormulaForExpression(((CInitializerExpression) init).getExpression(), true);
      }
      updateMemory(stack, f, val);
    }
  }

  @Override
  public SLStateError handleDereference(CExpression pExp, CExpression pOffset) throws Exception {
    // Formula loc;
    // if (pExp instanceof CPointerExpression) {
    // loc = createFormula(((CPointerExpression) pExp).getOperand(), pOffset, true);
    // loc = checkAllocation(loc, null, true);
    // if(loc == null) {
    // return SLStateError.INVALID_DEREF;
    // }
    // if(heap.containsKey(loc) ) {
    // loc = heap.get(loc);
    // } else if (stack.containsKey(loc)) {
    // loc = stack.get(loc);
    // }
    // return isAllocated(loc, true) ? null : SLStateError.INVALID_DEREF;
    // }

    Formula loc = createFormula(pExp, pOffset, true);
    return isAllocated(loc, true) ? null : SLStateError.INVALID_DEREF;

  }

  @Override
  public SLStateError
      handleDereferenceAssignment(CExpression pLHS, CExpression pOffset, CExpression pRHS)
          throws Exception {
    final Formula loc = createFormula(pLHS, pOffset, true);
    final Formula val = builder.getFormulaForExpression(pRHS, true);
    // if (pLHS instanceof CPointerExpression) {
    // loc = createFormula(((CPointerExpression) pLHS).getOperand(), pOffset, true);
    // loc = checkAllocation(loc, null, true);
    // if (loc == null) {
    // return SLStateError.INVALID_DEREF;
    // }
    // if (heap.containsKey(loc)) {
    // loc = heap.get(loc);
    // } else if (stack.containsKey(loc)) {
    // loc = stack.get(loc);
    // } else {
    // return SLStateError.INVALID_DEREF;
    // }
    // } else {
    //
    // }
    final Formula match = checkAllocation(loc, val, true);
    return match == null ? SLStateError.INVALID_DEREF : null;
  }

  @Override
  public SLStateError handleOutOfScopeVariable(CSimpleDeclaration pDecl) throws Exception {
    inScopePtrs.remove(pDecl);
    CType type = pDecl.getType();
    CExpression e = createSymbolicLocation(pDecl);
    Formula loc = builder.getFormulaForExpression(e, false);
    removeFromMemory(stack, loc);
    if (type instanceof CArrayType) {
      removeFromMemory(stack, builder.getFormulaForDeclaration(pDecl));
    }
    if (!(type instanceof CPointerType || type instanceof CArrayType)) {
      return null;
    }

    loc = builder.getFormulaForVariableName(pDecl.getQualifiedName(), false);
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
    Timer timer = new Timer();
    timer.start();

    Formula res = checkAllocation0(fLoc, usePredContext, heap.keySet());
    if (res == null) {
      res = checkAllocation0(fLoc, usePredContext, stack.keySet());
    }
    timer.stop();
    logger.log(Level.INFO, "Solvingtime_new: " + timer.toString());

    timer = new Timer();
    timer.start();
    res = checkAllocation(heap, fLoc, pVal, usePredContext);
    if (res == null) {
     res = checkAllocation(stack, fLoc, pVal, usePredContext);
    }
    timer.stop();
    logger.log(Level.INFO, "Solvingtime_old: " + timer.toString());

    return res;
  }

  private Formula
      checkAllocation0(Formula pLoc, boolean usePredContext, Collection<Formula> pHeap) {
    if (pHeap.isEmpty()) {
      return null;
    }

    if (pHeap.size() == 1) {
      Map<Formula, Formula> heap = new HashMap<>();
      pHeap.stream().forEach(f -> heap.put(f, null));
      if (isAllocated(pLoc, usePredContext, heap)) {
        return pHeap.iterator().next();
      } else {
        return null;
      }
    }
    // Split heap in two sub-heaps and check each of them.
    for (Collection<Formula> subHeap : Iterables.partition(pHeap, pHeap.size() / 2)) {
      Map<Formula, Formula> tmp = new HashMap<>();
      // TODO convert map to collection in isAllocated?
      // leave out values as they are coped by pathformula.
      subHeap.stream().forEach(f -> tmp.put(f, null));
      if (isAllocated(pLoc, usePredContext, tmp)) {
        return checkAllocation0(pLoc, usePredContext, subHeap);
      }
    }
    return null;
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

  // private void updateMemory(Formula pLoc, Formula pVal) {
  // if (heap.containsKey(pLoc)) {
  // heap.put(pLoc, pVal);
  // } else if (stack.containsKey(pLoc)) {
  // stack.put(pLoc, pVal);
  // }
  // }

  private boolean isAllocated(Formula pLoc, boolean usePredContext)
      throws Exception {
    return isAllocated(pLoc, usePredContext, stack) || isAllocated(pLoc, usePredContext, heap);
  }

  private boolean isAllocated(Formula pLoc, boolean usePredContext, Map<Formula, Formula> pHeap) {
    PathFormula context = usePredContext ? builder.getPredPathFormula() : builder.getPathFormula();
    BooleanFormula heapFormula = createHeapFormula(pHeap);
    BooleanFormula toBeChecked = slfm.makePointsTo(pLoc, ifm.makeNumber(0));
    try (ProverEnvironment prover = solver.newProverEnvironment()) {
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
    BooleanFormula formula = slfm.makeEmptyHeap(heapAddresFormulaType, heapValueFormulaType);
    for (Formula f : pHeap.keySet()) {
      Formula target = pHeap.get(f);
      BooleanFormula ptsTo = slfm.makePointsTo(f, target != null ? target : ifm.makeNumber(0));
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

  private CExpression createSymbolicLocation(CSimpleDeclaration pDecl) {

    CIdExpression e = new CIdExpression(FileLocation.DUMMY, pDecl);
    CType t = new CPointerType(false, false, pDecl.getType());
    return new CUnaryExpression(FileLocation.DUMMY, t, e, UnaryOperator.AMPER);
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

  private void incSSAIndex(String pVar) {
    SSAMap ssaMap = builder.getPathFormula().getSsa();
    int scopeIndex = pVar.indexOf("::") + 2;
    String name = pVar.substring(0, scopeIndex) + "&" + pVar.substring(scopeIndex, pVar.length());
    CType type = ssaMap.getType(name);
    int index = ssaMap.getIndex(name) + 1;
    SSAMapBuilder b = ssaMap.builder();
    builder.updateSSAMap(b.setIndex(name, type, index).build());
  }

}
