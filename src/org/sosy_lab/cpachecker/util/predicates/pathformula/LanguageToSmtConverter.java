// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableSortedSet;
import java.io.PrintStream;
import java.util.NavigableSet;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMapMerger.MergeResult;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerBase;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.smg.datastructures.PersistentStack;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public abstract class LanguageToSmtConverter<T extends Type> {

  // Index that is used to read from variables that were not assigned yet
  private static final int VARIABLE_UNINITIALIZED = 1;

  // Index to be used for first assignment to a variable (must be higher than
  // VARIABLE_UNINITIALIZED!)
  private static final int VARIABLE_FIRST_ASSIGNMENT = 2;

  /** Produces a fresh new SSA index for an assignment and updates the SSA map. */
  protected int makeFreshIndex(String name, T type, SSAMapBuilder ssa) {
    int idx = getFreshIndex(name, type, ssa);
    ssa.setIndex(name, type, idx);
    return idx;
  }

  /**
   * Produces a fresh new SSA index for an assignment, but does _not_ update the SSA map. Usually
   * you should use {@link #makeFreshIndex(String, Type, SSAMapBuilder)} instead, because using
   * variables with indices that are not stored in the SSAMap is not a good idea (c.f. the comment
   * inside getIndex()). If you use this method, you need to make sure to update the SSAMap
   * correctly.
   */
  @SuppressWarnings("unused") // The parameter in the middle is there only to prohibit subclassing
  protected int getFreshIndex(String name, T pType, SSAMapBuilder ssa) {
    int idx = ssa.getFreshIndex(name);
    if (idx <= 0) {
      idx = LanguageToSmtConverter.VARIABLE_FIRST_ASSIGNMENT;
    }
    return idx;
  }

  /**
   * This method returns the index of the given variable in the ssa map, if there is none, it
   * creates one with the value 1.
   *
   * <p>Note that this not check whether the variable has always the same type. It is the caller's
   * responsibility to ensure that.
   *
   * @return the index of the variable
   */
  public int getExistingOrNewIndex(String name, T type, SSAMapBuilder ssa) {
    int idx = ssa.getIndex(name);
    if (idx <= 0) {
      idx = LanguageToSmtConverter.VARIABLE_UNINITIALIZED;

      // It is important to store the index in the variable here.
      // If getIndex() was called with a specific name,
      // this means that name@idx will appear in formulas.
      // Thus, we need to make sure that calls to FormulaManagerView.instantiate()
      // will also add indices for this name,
      // which it does exactly if the name is in the SSAMap.
      ssa.setIndex(name, type, idx);
    }

    return idx;
  }

  public PersistentStack<SSAMap> handleSsaStack(
      CFAEdge pEdge,
      Constraints pConstraints,
      PathFormula oldFormula,
      SSAMap newSsa,
      PointerTargetSet newPts,
      FormulaManagerView fmgr) {
    return switch (pEdge.getEdgeType()) {
      case FunctionCallEdge -> oldFormula.getSsaStack().pushAndCopy(newSsa);
      case FunctionReturnEdge -> {
        // We now need to reset all SSA indices of local variables of the caller function to the
        // state before the call, because after a return edge we are back in the caller
        // function. This is necessary because otherwise we would have wrong SSA indices for
        // these variables, which can lead to unsound results.
        //
        // For example, if we have a
        // function "f" (f_old) with a local variable "x", and we call "f" (f_new)
        // then after the return edge of f_new we are back in f_old, but if we do not reset the
        // SSA
        // indices of "x", then we would have the SSA index of "x" from the f_new call,
        // and not the ones for f_old.
        //
        // All other variables (globals and locals of other functions) keep the indices they got
        // during the callee's execution. In particular the local variables of functions further
        // up in the call stack are not touched here, since they may need to be reset when
        // returning from their frames later on.
        final SSAMapBuilder functionReturnSsaBuilder = newSsa.builder();

        // If the analysis starts in the middle of a CFA, the first edge can be a return edge
        // while the stack does not contain a frame for the caller. In this case we do not know
        // the values the caller's variables had before the call, so instead of resetting them we
        // give them fresh unconstrained indices, which overapproximates the state of the caller
        // function. The same applies to variables the caller has no entry for because they were
        // only created inside the (recursive) callee.
        final boolean hasCallerFrame = oldFormula.getSsaStack().size() > 1;
        final SSAMap callerSsa =
            hasCallerFrame ? oldFormula.getSsaStack().popAndCopy().peek() : SSAMap.emptySSAMap();

        final NavigableSet<String> knownVariables =
            ImmutableSortedSet.<String>naturalOrder()
                .addAll(callerSsa.allVariables())
                .addAll(newSsa.allVariables())
                .build();

        // Only the variables of the caller function are reset or havocked, which we can identify
        // by the variable name starting with the function name of the caller function (which
        // is the successor function of the return edge).
        for (String var :
            CFAUtils.filterVariablesOfFunction(
                knownVariables, pEdge.getSuccessor().getFunctionName())) {
          if (newSsa.getIndex(var) != oldFormula.getTopmostStackSsa().getIndex(var)) {
            // The variable was written while handling the return, i.e., it was assigned the
            // return value in a statement like `a = f();`. Then it already holds the correct
            // value for the caller and must not be reset.
            continue;
          }

          if (!callerSsa.containsVariable(var)) {
            // The caller has no information about this variable, so we cannot reset it to its
            // value from before the call. Instead we give it a fresh index without adding any
            // constraint, so a later use in the caller reads an unconstrained value. We must not
            // delete the variable instead: that would restart its index counter, and a later
            // assignment could then reuse indices that already occur in the formulas of the
            // frames we returned from, wrongly equating unrelated values.
            @SuppressWarnings("unchecked")
            T varType = (T) newSsa.getType(var);
            makeFreshIndex(var, varType, functionReturnSsaBuilder);
          } else if (
          // If we are not in a recursive call, then we do not need to reset the index, we know
          // this since if the same variable has not been written we are not in a recursive call
          newSsa.getIndex(var) != callerSsa.getIndex(var)
              // The reset is only sound for the plain SSA copy of a variable. A variable whose
              // address has been taken lives in the memory encoding instead, where the callee may
              // legitimately have changed it through a pointer into the caller's frame, so its
              // memory contents must not be equated across the call. Its base is registered with
              // the call stack depth of the caller's frame, which newPts holds again after leaving
              // the callee. (Reaching this point implies a recursive call, so the caller function
              // is still on the call stack of newPts.)
              && !newPts.isActualBase(new PointerBase(var, newPts.getCallStackDepth(var)))) {

            // The SSAMap is not polymorphic so it does not know that it should only contain a T.
            @SuppressWarnings("unchecked")
            T varType = (T) callerSsa.getType(var);
            Verify.verify(
                varType == newSsa.getType(var),
                "Variable %s has different types in caller and callee SSA",
                var);

            makeFreshIndex(var, varType, functionReturnSsaBuilder);
            // Now make it such that the new variable is equal to the old one
            // Both sides use newPts: the variable belongs to the caller, so both formulas must be
            // built with the caller's view of the pointer target set.
            pConstraints.addConstraint(
                fmgr.makeEqual(
                    makeFormulaForVariable(callerSsa, newPts, var, varType),
                    makeFormulaForVariable(
                        functionReturnSsaBuilder.build(), newPts, var, varType)));
          }
        }

        // It is not necessary to update the variables of the callee, since if the program is a
        // valid C program does need to be written before being read, since reading from an
        // unitialized variable is undefined behavior.
        // However, we still do this to be safer against bugs.
        if (!pEdge
            .getPredecessor()
            .getFunctionName()
            .equals(pEdge.getSuccessor().getFunctionName())) {
          for (String var :
              CFAUtils.filterVariablesOfFunction(
                  knownVariables, pEdge.getPredecessor().getFunctionName())) {
            @SuppressWarnings("unchecked")
            T varType = (T) newSsa.getType(var);
            makeFreshIndex(var, varType, functionReturnSsaBuilder);
          }
        }

        // Now the current state of the caller is the rebuilt SSA map. Pop the callee frame and
        // the stale caller frame (if present) and replace them with it.
        final PersistentStack<SSAMap> remainingStack =
            hasCallerFrame
                ? oldFormula.getSsaStack().popAndCopy().popAndCopy()
                : PersistentStack.of();
        yield remainingStack.pushAndCopy(functionReturnSsaBuilder.build());
      }
      default -> oldFormula.getSsaStack().popAndCopy().pushAndCopy(newSsa);
    };
  }

  public abstract FormulaType<?> getFormulaTypeFromType(T type);

  public abstract PathFormula makeAnd(
      PathFormula pOldFormula, CFAEdge pEdge, ErrorConditions pErrorConditions)
      throws UnrecognizedCodeException, InterruptedException;

  public abstract MergeResult<PointerTargetSet> mergePointerTargetSets(
      PointerTargetSet pPts1, PointerTargetSet pPts2, SSAMapBuilder pNewSSA)
      throws InterruptedException;

  public abstract BooleanFormula makeSsaUpdateTerm(
      String pSymbolName, Type pSymbolType, int pOldIndex, int pNewIndex, PointerTargetSet pOldPts)
      throws InterruptedException;

  public abstract Formula makeFormulaForVariable(
      SSAMap pSsa, PointerTargetSet pPointerTargetSet, String pVarName, T pType);

  public abstract Formula makeFormulaForUninstantiatedVariable(
      String pVarName, T pType, PointerTargetSet pContextPTS, boolean pForcePointerDereference);

  public abstract Formula buildTermFromPathFormula(
      PathFormula pFormula, CIdExpression pExpr, CFAEdge pEdge) throws UnrecognizedCodeException;

  public abstract void printStatistics(PrintStream pOut);
}
