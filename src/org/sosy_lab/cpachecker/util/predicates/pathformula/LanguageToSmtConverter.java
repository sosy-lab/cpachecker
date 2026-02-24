// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula;

import com.google.common.collect.ImmutableList;
import java.io.PrintStream;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMapMerger.MergeResult;
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

  public Pair<PersistentStack<SSAMap>, ImmutableList<BooleanFormula>>
      handleSsaStackForFunctionReturn(
          CFAEdge pEdge,
          PathFormula oldFormula,
          SSAMap newSsa,
          PointerTargetSet newPts,
          FormulaManagerView fmgr) {
    return switch (pEdge.getEdgeType()) {
      case FunctionCallEdge ->
          Pair.of(
              oldFormula.getSsaStack().pushAndCopy(newSsa),
              ImmutableList.of(fmgr.getBooleanFormulaManager().makeTrue()));
      case FunctionReturnEdge -> {
        // We now need to reset all SSA indices of local variables of the caller function to the
        // state before the call, because after a return edge we are back in the caller
        // function. This is necessary because otherwise we would have wrong SSA indices for
        // these variables, which can lead to unsound results.
        //
        // For example, if we have a
        // function "f" with a local variable "x", and we call "f" (f_new) from the same
        // function "f" (f_old),
        // then after the return edge of f_old we are back in f_new, but if we do not reset the
        // SSA
        // indices of "x", then we would have the SSA index of "x" from the f_new call,
        // and not the ones for f_old.
        //
        // Note that we need to keep track of all local variables which were not reset
        // since they may be used further up in the call-stack, and we may need to reset them
        // then.
        final SSAMapBuilder functionReturnSsaBuilder = newSsa.builder();
        final SSAMap callerSsa;
        if (oldFormula.getSsaStack().size() == 1) {
          // This can happen if the analysis starts in the middle of a CFA, and the first edge
          // is a return edge.
          // In this case, we just use an empty SSA, which means that we do not have any
          // information about the caller function, which means we are overapproximating
          // the state of the caller function, but this is the best we can do in this case.
          callerSsa = SSAMap.emptySSAMap();
        } else {
          callerSsa = oldFormula.getSsaStack().popAndCopy().peek();
        }

        ImmutableList.Builder<BooleanFormula> constraintsBuilder = ImmutableList.builder();
        for (String var : callerSsa.allVariables()) {

          // Compute the representation of the return term as a string. This is the easiest
          // way to commonly handle C and SV-LIB, since SV-LIB can return a tuple and C
          // only returns a single variable. One could change the interface, but that seems
          // unnecessarily complex, since the variable in the SSAMap is still definitely a string.
          String leftHandSide = "";
          if (((FunctionReturnEdge) pEdge).getFunctionCall()
              instanceof AFunctionCallAssignmentStatement pAssignmentStatement) {
            leftHandSide =
                pAssignmentStatement
                    .getLeftHandSide()
                    .toASTString(AAstNodeRepresentation.QUALIFIED);
          }

          if (
          // Only reset the variables of the caller function, which we can identify
          // by the variable name starting with the function name of the caller function (which
          // is the successor function of the return edge).
          var.startsWith(pEdge.getSuccessor().getFunctionName() + "::")
              // In addition, we should only update those which are not being
              // written by the return function, for example for `a = f(a);`,
              // we do not want to reset the index of the `a` to the state before the call, since
              // it is being written by the return function, and thus we need to keep the new index
              // for`a`
              && !leftHandSide.contains(var.replace("::", "__"))
              // If we are not in a recursive call, then we do not need to reset the index, we know
              // this since if the same variable has not been written we are not in a recursive call
              && newSsa.getIndex(var) != callerSsa.getIndex(var)) {
            functionReturnSsaBuilder.setIndex(
                var,
                callerSsa.getType(var),
                // we need to guarantee that we are monotonically increasing in the SSA indices,
                // because otherwise we can return from a recursive call into a previous state
                // making the whole formula unsat.
                newSsa.getIndex(var) + 1);
            // Now make it such that the new variable is equal to the old one
            constraintsBuilder.add(
                fmgr.makeEqual(
                    makeFormulaForVariable(
                        callerSsa,
                        oldFormula.getPointerTargetSet(),
                        var,
                        (CType) callerSsa.getType(var)),
                    makeFormulaForVariable(
                        functionReturnSsaBuilder.build(),
                        newPts,
                        var,
                        (CType) newSsa.getType(var))));
          }
        }

        final PersistentStack<SSAMap> callerStack;
        if (oldFormula.getSsaStack().size() == 1) {
          // We reset to the caller SSA, but we do not pop anything, because we do not have the
          // information about the caller function.
          callerStack = PersistentStack.<SSAMap>of().pushAndCopy(functionReturnSsaBuilder.build());
        } else {
          // Now the current state of the caller is the new ssa
          callerStack =
              oldFormula
                  .getSsaStack()
                  .popAndCopy()
                  .popAndCopy()
                  .pushAndCopy(functionReturnSsaBuilder.build());
        }

        yield Pair.of(callerStack, constraintsBuilder.build());
      }
      default ->
          Pair.of(
              oldFormula.getSsaStack().popAndCopy().pushAndCopy(newSsa),
              ImmutableList.of(fmgr.getBooleanFormulaManager().makeTrue()));
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
      SSAMap pSsa, PointerTargetSet pPointerTargetSet, String pVarName, CType pType);

  public abstract Formula makeFormulaForUninstantiatedVariable(
      String pVarName, CType pType, PointerTargetSet pContextPTS, boolean pForcePointerDereference);

  public abstract Formula buildTermFromPathFormula(
      PathFormula pFormula, CIdExpression pExpr, CFAEdge pEdge) throws UnrecognizedCodeException;

  public abstract void printStatistics(PrintStream pOut);
}
