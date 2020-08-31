// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.sl;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.cpa.sl.CToFormulaConverterWithSL.AllocationCheckProcedure;
import org.sosy_lab.cpachecker.cpa.sl.SLState.SLStateError;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SLFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;

/**
 * This class is used to pass relevant arguments to the {@link ExpressionToFormulaVisitor}.
 */
public class SLMemoryDelegate implements PointerTargetSetBuilder, StatisticsProvider {

  private final Solver solver;
  private final SLState state;
  private final FormulaManagerView fm;
  private final SLFormulaManager slfm;
  private final BooleanFormulaManagerView bfm;
  private final SLStatistics stats;
  private final MachineModel machineModel;
  private final LogManager logger;
  private final AllocationCheckProcedure acProc;


  private final BitvectorType heapAddressFormulaType;
  private final BitvectorType heapValueFormulaType;

  public SLMemoryDelegate(
      Solver pSolver,
      SLState pState,
      MachineModel pMachineModel,
      LogManager pLogger,
      SLStatistics pStats,
      AllocationCheckProcedure pProcedure) {
    solver = pSolver;
    state = pState;
    fm = solver.getFormulaManager();
    slfm = fm.getSLFormulaManager();
    bfm = fm.getBooleanFormulaManager();
    stats = pStats;
    machineModel = pMachineModel;
    logger = pLogger;
    acProc = pProcedure;

    heapAddressFormulaType =
        FormulaType.getBitvectorTypeWithSize(machineModel.getSizeofPtrInBits());
    heapValueFormulaType = FormulaType.getBitvectorTypeWithSize(machineModel.getSizeofCharInBits());
  }

  @Override
  public PointerTargetSet build() {
    return PointerTargetSet.emptyPointerTargetSet();
  }

  /**
   * Checks whether the given location is allocated in the memory and returns the value of that
   * location.
   *
   * @param pLoc The location to be checked.
   * @param segmentSize The memory segment size according to the type.
   * @return The value the pointer is pointing to or null.
   */
  public Optional<Formula> dereference(Formula pLoc, int segmentSize) {
    Optional<Formula> allocatedLoc = checkAllocation(pLoc);
    if (allocatedLoc.isPresent()) {
      Formula loc = allocatedLoc.get();
      if(state.getHeap().containsKey(loc) ) {
        return Optional.of(getValueForLocation(state.getHeap(), loc, segmentSize));
      } else {
        return Optional.of(getValueForLocation(state.getStack(), loc, segmentSize));
      }
    } else {
      return Optional.empty();
    }
  }

  public Optional<Formula> dereference(Formula pLoc, Formula pOffset, int segmentSize) {
    Formula loc = fm.makeMultiply(pOffset, fm.makeNumber(fm.getFormulaType(pOffset), segmentSize));
    loc = fm.makePlus(pLoc, loc);
    return dereference(loc, segmentSize);
  }

  /**
   * Allocation check of the given location.
   *
   * @param pLoc The location to be checked if allocated.
   * @return The allocated equivalent in the memory if allocated. Empty if not allocated.
   */
  public Optional<Formula> checkAllocation(Formula pLoc) {
    // Trivial checks first to increase performance.
    if (state.getHeap().containsKey(pLoc)) {
      return Optional.of(pLoc);
    }
    if (state.getStack().containsKey(pLoc)) {
      return Optional.of(pLoc);
    }

    Optional<Formula> allocatedLoc = checkAllocation(state.getHeap(), pLoc);
    if (allocatedLoc.isPresent()) {
      return allocatedLoc;
    }
    allocatedLoc = checkAllocation(state.getStack(), pLoc);
    if (allocatedLoc.isPresent()) {
      return allocatedLoc;
    }
    return Optional.empty();
  }

  public Optional<Formula> checkHeapAllocation(Formula pLoc) {
    return checkAllocation(state.getHeap(), pLoc);
  }

  public Optional<Formula> checkAllocation(Formula pLoc, Formula pOffset, int segmentSize) {
    Formula loc = fm.makeMultiply(pOffset, fm.makeNumber(fm.getFormulaType(pOffset), segmentSize));
    loc = fm.makePlus(pLoc, loc);
    return checkAllocation(loc);
  }


  /**
   * Checks whether the given location is allocated in the memory and assigns the given value to
   * that location.
   *
   * @param pLoc The location to be checked.
   * @param pVal The value to be written to memory.
   * @param segmentSize The memory segment according to the type.
   * @return True if successful, false otherwise.
   */
  public boolean dereferenceAssign(Formula pLoc, Formula pVal, int segmentSize) {
    Optional<Formula> allocatedLoc = checkAllocation(pLoc);
    if (allocatedLoc.isPresent()) {
      Formula loc = allocatedLoc.get();
      if (state.getHeap().containsKey(loc)) {
        return assignValueToLocation(state.getHeap(), allocatedLoc.get(), pVal, segmentSize);
      } else {
        return assignValueToLocation(state.getStack(), allocatedLoc.get(), pVal, segmentSize);
      }
    } else {
      return false;
    }

  }

  private Formula
      getValueForLocation(LinkedHashMap<Formula, Formula> pMemory, Formula pLoc, int size) {
    int bytesToProcess = size;
    boolean found = false;
    List<Formula> byteLocs = new LinkedList<>();
    for (Formula key : pMemory.keySet()) {
      if(bytesToProcess == 0) {
        break;
      }
      if (found || key.equals(pLoc)) {
        found = true;
        byteLocs.add(key);
        bytesToProcess--;
      }
    }
    Formula res = pMemory.get(byteLocs.remove(0));
    for (Formula loc : byteLocs) {
      res = fm.makeConcat(pMemory.get(loc), res);
    }
    return res;
  }

  private boolean
      assignValueToLocation(Map<Formula, Formula> pMemory, Formula pLoc, Formula pVal, int size) {
    int bytesToProcess = size;
    boolean found = false;
    List<Formula> byteLocs = new LinkedList<>();
    for (Formula key : pMemory.keySet()) {
      if (bytesToProcess == 0) {
        break;
      }
      if (found || key.equals(pLoc)) {
        found = true;
        byteLocs.add(key);
        bytesToProcess--;
      }
    }
    if (!found) {
      return false;
    }
    for (int i = 0; i < size; i++) {
      int lsb = i * heapValueFormulaType.getSize();
      int msb = lsb + heapValueFormulaType.getSize() - 1;
      Formula nthByte = fm.makeExtract(pVal, msb, lsb, true);
      pMemory.put(byteLocs.get(i), nthByte);
    }
    return true;
  }

  private Optional<Formula>
      checkAllocation(Map<Formula, Formula> pMemory, Formula fLoc) {
    if (pMemory.isEmpty()) {
      return Optional.empty();
    }
    // Syntactical check for performance.
    if (pMemory.containsKey(fLoc)) {
      return Optional.of(fLoc);
    }
    // Semantical check.
    switch (acProc) {
      case SL:
      case SMT:
        for (Formula formulaInMemory : pMemory.keySet()) {
          boolean match =
              acProc == AllocationCheckProcedure.SMT
                  ? checkEquivalenceSMT(fLoc, formulaInMemory)
                  : checkEquivalenceSL(fLoc, formulaInMemory, pMemory);
          if (match) {
            return Optional.of(formulaInMemory);
          }

        }
        break;
      case SMT_MODELSAT:
        Formula loc = getLocation(pMemory, fLoc);
        if (loc != null) {
          return Optional.of(loc);
        }
    }
    return Optional.empty();
  }

  @SuppressWarnings("unused")
  /**
   * Implementation of Sigma |- Allocated(E) as UnSAT(E -> - * Sigma).
   *
   * @param pMemory
   * @param pLoc
   * @return true if allocated, false otherwise.
   */
  private boolean isAllocated(Map<Formula, Formula> pMemory, Formula pLoc) {
    BooleanFormula symHeap = makeSLFormula(pMemory);
    BooleanFormula toCheck =
        slfm.makePointsTo(pLoc, fm.makeVariable(heapValueFormulaType, "__dummyVal"));
    try (ProverEnvironment prover =
        solver.newProverEnvironment(
            ProverOptions.ENABLE_SEPARATION_LOGIC)) {
      prover.addConstraint(slfm.makeStar(toCheck, symHeap));
      stats.startSolverTime();
      return prover.isUnsat();
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    } finally {
      stats.stopSolverTime();
    }
    return false;
  }

  /**
   * Checks for location match on the heap by generating a model rather than checking each pair individually.
   * However, segmentation faults occur on CVC4 solver side.
   */
  private Formula getLocation(Map<Formula, Formula> pMemory, Formula pLoc) {
    Set<Formula> keys = pMemory.keySet();
    Formula[] keyArray = keys.toArray(new Formula[keys.size()]);
    String dummyVar = "__DummVarV_#";
    BooleanFormula f = bfm.makeTrue();
    // Construct formula: ((Loc = key_0) <=> v_0) & ... & ((Loc = key_n) <=> v_n)
    for (int i = 0; i < keyArray.length; i++) {
     BooleanFormula tmp = bfm.makeVariable(dummyVar + i);
      f = fm.makeAnd(f, fm.makeEqual(fm.makeEqual(pLoc, keyArray[i]), tmp));
    }

    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.addConstraint(f);

      stats.startSolverTime();
      // Generate model
      if (!prover.isUnsat()) {
        List<ValueAssignment> assignments = prover.getModelAssignments();
        for (ValueAssignment a : assignments) {
          String var = a.getName();
          if (var.contains(dummyVar)) {
            // return key_n for v_n = true
            if(bfm.isTrue((BooleanFormula) a.getValueAsFormula())) {
              int index = Integer.parseInt(var.substring(var.indexOf('#') + 1));
              return keyArray[index];
            }
          }

        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    } finally {
      stats.stopSolverTime();
    }
    return null;
  }

  private boolean
      checkEquivalenceSL(Formula pLoc, Formula pHeaplet, Map<Formula, Formula> pMemory) {
    BooleanFormula toCheck =
        slfm.makePointsTo(pLoc, fm.makeVariable(heapValueFormulaType, "__dummyVal"));
    BooleanFormula onHeap = slfm.makePointsTo(pHeaplet, pMemory.get(pHeaplet));
    try (ProverEnvironment prover =
        solver.newProverEnvironment(ProverOptions.ENABLE_SEPARATION_LOGIC)) {
      prover.addConstraint(slfm.makeStar(toCheck, onHeap));
      stats.startSolverTime();
      return prover.isUnsat();
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    } finally {
      stats.stopSolverTime();
    }
    return false;
  }

  private boolean checkEquivalenceSMT(Formula pF0, Formula pF1) {
    FormulaType<Formula> type0 = fm.getFormulaType(pF0);
    FormulaType<Formula> type1 = fm.getFormulaType(pF1);
    if (!type0.equals(type1)) {
      return false;
    }
    BooleanFormula tmp = fm.makeEqual(pF0, pF1);
    try (ProverEnvironment prover =
        solver.newProverEnvironment()) {
      prover.addConstraint(tmp);
      stats.startSolverTime();
      if (prover.isUnsat()) {
        return false;
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
      return false;
    } finally {
      stats.stopSolverTime();
    }
    // Check tautology.
    try (ProverEnvironment prover = solver.newProverEnvironment()) {
      prover.addConstraint(fm.makeNot(tmp));
      stats.startSolverTime();
      return prover.isUnsat();
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    } finally {
      stats.stopSolverTime();
    }
    return false;

  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  public void addError(SLStateError pError) {
    state.addError(pError);
  }

  private void allocate(LinkedHashMap<Formula, Formula> pMemory, Formula var, int size) {
    BitvectorFormula key = (BitvectorFormula) var;
    assert fm.getFormulaType(key).equals(heapAddressFormulaType) : String
        .format("Type:%s Var:%s HeapType:%s", fm.getFormulaType(key), var, heapAddressFormulaType);
    pMemory.put(var, fm.makeNumber(heapValueFormulaType, 0L));
    state.getAllocationSizes().put(var, BigInteger.valueOf(size));
    for (int i = 1; i < size; i++) {
      Formula loc = fm.makePlus(var, fm.makeNumber(heapAddressFormulaType, i));
      pMemory.put(loc, fm.makeNumber(heapValueFormulaType, 0L));
    }
  }

  private void allocateOnStack(Formula var, int size) {
    allocate(state.getStack(), var, size);
  }

  private void allocateOnHeap(Formula var, int size) {
    allocate(state.getHeap(), var, size);
  }

  public boolean
      deallocate(LinkedHashMap<Formula, Formula> pMemory, Formula var, boolean checkLeak) {
    BigInteger size = state.getAllocationSizes().remove(var);
    if (size == null) {
      logger.log(Level.SEVERE, "Deallocate: " + var + " not found in SSAMap");
      return false;
    }
    if (!pMemory.containsKey(var)) {
      return false;
    }
    Formula loc = var;
    Formula val =
        getValueForLocation(
            pMemory,
            loc,
            heapAddressFormulaType.getSize() / heapValueFormulaType.getSize());
    for (int i = 0; i < size.intValueExact(); i++) {
      if (i > 0) {
        loc = fm.makePlus(var, fm.makeNumber(heapAddressFormulaType, i));
      }
      pMemory.remove(loc);
    }
    // Check for leak.
    if (checkLeak) {
      Optional<Formula> heapPtr = checkHeapAllocation(val);
      if (heapPtr.isPresent()) {
        if (!isReachable(new HashSet<>(), firstByte((BitvectorFormula) heapPtr.get()))) {
          state.addError(SLStateError.MEMORY_LEAK);
        }
      }
    }

    return true;
  }

  public boolean deallocateFromStack(Formula pVar, boolean checkLeak) {
    return deallocate(state.getStack(), pVar, checkLeak);
  }

  public boolean deallocateFromHeap(Formula pVar) {
    return deallocate(state.getHeap(), pVar, true);
  }

  public BigInteger calculateValue(Formula pVal) {
    final String dummyVarName = "0_calculatedValue"; // must not be a valid C variable name.
    assert pVal instanceof BitvectorFormula;
    BooleanFormula eq = fm.makeEqual(fm.makeVariable(fm.getFormulaType(pVal), dummyVarName), pVal);
    try (ProverEnvironment env =
        solver.newProverEnvironment(
            ProverOptions.GENERATE_MODELS,
            ProverOptions.ENABLE_SEPARATION_LOGIC)) {
      env.addConstraint(makeSLFormula());
      // env.addConstraint(makeConstraints());
      env.addConstraint(eq);
      stats.startSolverTime();
      if (!env.isUnsat()) {
        List<ValueAssignment> assignments = env.getModelAssignments();
        for (ValueAssignment a : assignments) {
          if (a.getName().equals(dummyVarName)) {
            return (BigInteger) a.getValue();
          }
        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    } finally {
      stats.stopSolverTime();
    }
    logger.log(
        Level.SEVERE,
        "Numeric value of formula " + pVal.toString() + " could not be determined.");
    return null;
  }

  public void handleMalloc(Formula pLoc, int pSize) {
    allocateOnHeap(pLoc, pSize);
  }

  public void handleAlloca(Formula pLoc, int pSize, String functionName) {
    allocateOnStack(pLoc, pSize);
    state.addAlloca(pLoc, functionName);
  }

  public void releaseAllocas(String functionScope) {
    if (state.getAllocas().containsKey(functionScope)) {
      Set<Formula> segments = state.getAllocas().get(functionScope);
      for (Formula formula : segments) {
        deallocateFromStack(formula, true);
      }
    }
  }

  public void handleVarDeclaration(Formula pVar, CType pType) {
    int size = 0;
    if (pType.isIncomplete()) {
      if (pType instanceof CArrayType) {
        CArrayType type = (CArrayType) pType;
        size = type.getLengthAsInt().getAsInt();
      }
    } else {
      size = machineModel.getSizeof(pType).intValueExact();
    }
    allocateOnStack(pVar, size);
  }

  public boolean handleOutOfScopeVar(Formula var, CType type) {
    boolean checkLeak = type instanceof CPointerType || type instanceof CArrayType;
    return deallocateFromStack(var, checkLeak);
  }

  private boolean isReachable(Set<Formula> visited, Formula loc) {
    if (visited.contains(loc)) {
      return false;
    }

    for (Entry<Formula, Formula> entry : state.getStack().entrySet()) {
      if (checkEquivalenceSMT(loc, entry.getValue())) {
        return true;
      }
    }
    for (Entry<Formula, Formula> entry : state.getHeap().entrySet()) {
      if (checkEquivalenceSMT(loc, entry.getValue())) {
        visited.add(loc);
        return isReachable(visited, firstByte((BitvectorFormula) entry.getKey()));
      }
    }
    return false;
  }

  private Formula firstByte(BitvectorFormula pVal) {
    BitvectorFormulaManagerView bvfm = fm.getBitvectorFormulaManager();
    return bvfm.extract(pVal, heapValueFormulaType.getSize() - 1, 0, true);
  }

  public boolean handleFree(Formula pLoc) {
    Optional<Formula> loc = checkAllocation(state.getHeap(), pLoc);
    if (loc.isPresent()) {
      deallocateFromHeap(loc.get());
      return true;
    }
    return false;
  }

  public BooleanFormula makeSLFormula() {
    BooleanFormula heap = makeSLFormula(state.getHeap());
    BooleanFormula stack = makeSLFormula(state.getStack());
    BooleanFormula res;
    if (heap == null && stack == null) {
      res = slfm.makeEmptyHeap(heapAddressFormulaType, heapValueFormulaType);
    } else if (heap == null) {
      res = stack;
    } else if (stack == null) {
      res = heap;
    } else {
      res = slfm.makeStar(heap, stack);
    }
    // BooleanFormula constraints = fm.getBooleanFormulaManager().and(state.getConstraints());
    // return fm.makeAnd(res, constraints);
    return res;
  }

  private BooleanFormula makeSLFormula(Map<Formula, Formula> pMemory) {
    BooleanFormula formula = null;
    for (Formula f : pMemory.keySet()) {
      BitvectorFormula key = (BitvectorFormula) f;
      assert fm.getFormulaType(key).equals(heapAddressFormulaType) : key;
      BitvectorFormula target = (BitvectorFormula) pMemory.get(f);
      BooleanFormula ptsTo = slfm.makePointsTo(f, target);
      if (formula == null) {
        formula = ptsTo;
      } else {
        formula = slfm.makeStar(formula, ptsTo);
      }

    }
    return formula;
  }

  public CType makeLocationTypeForVariableType(CType type) {
    return new CPointerType(type.isConst(), type.isVolatile(), type);
  }

  public void handleRealloc(Formula pLoc, Formula pOldLoc, int pSize) {
    Optional<Formula> loc = checkHeapAllocation(pOldLoc);
    if (loc.isPresent()) {
      deallocateFromHeap(loc.get());
      allocateOnHeap(pLoc, pSize);
    } else {
      addError(SLStateError.INVALID_FREE);
    }
  }

  public void checkMemLeak() {
    if (!state.getHeap().isEmpty()) {
      state.addError(SLStateError.MEMORY_LEAK);
    }

  }
}
