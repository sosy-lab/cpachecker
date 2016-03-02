package org.sosy_lab.cpachecker.util.predicates.pathformula;

import static com.google.common.collect.Iterables.getOnlyElement;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

import com.google.common.base.Function;
import com.google.common.collect.Sets;

public class FormulaInductivenessCheck {

  private final FormulaManagerView fmgr;
  private final PathFormulaManager pfmgr;
  private final Solver solver;

  private static final String INTERMEDIATE_VAR_PREFIX = "__SLICE_INTERMEDIATE_";

  public FormulaInductivenessCheck(PathFormulaManager pPfmgr, Solver pSolver) {
    solver = pSolver;
    fmgr = solver.getFormulaManager();
    pfmgr = pPfmgr;
  }

  /**
   * Check whether a formula is inductive at a given CFANode.
   * @return A new version of the formula
   * that only contains current variables (no variables with outdated SSA indices),
   * or "true" if the formula is not inductive.
   */
  public BooleanFormula getInductiveVersionOf(PathFormula formula, CFANode node)
      throws CPATransferException, InterruptedException {
    final SSAMap ssa = formula.getSsa();
    final BooleanFormula slice = formula.getFormula();

    Set<String> outVariables = new HashSet<>();
    final Set<String> intermediateVariables = new HashSet<>();

    // Rename all non-final variables.
    for (String var : fmgr.extractFunctionNames(slice)) {
      Pair<String, Integer> fullName = FormulaManagerView.parseName(var);
      String varName = fullName.getFirst();
      Integer ssaIndex = fullName.getSecond();

      // Non-final variable.
      if (ssaIndex != null && (ssa.containsVariable(varName)) &&
          ssaIndex < ssa.getIndex(varName)) {
        intermediateVariables.add(var);
      } else {
        outVariables.add(var);
      }
    }
    BooleanFormula sliceRenamed = fmgr.renameFreeVariablesAndUFs(slice,
        new Function<String, String>() {
              @Override
              public String apply(String pInput) {
                return intermediateVariables.contains(pInput)
                    ? INTERMEDIATE_VAR_PREFIX + pInput
                    : pInput;
              }
            }
        );

    if (isInductive(node, outVariables, formula.updateFormula(sliceRenamed))) {
      return fmgr.simplify(sliceRenamed);
    } else {
      return fmgr.getBooleanFormulaManager().makeBoolean(true);
    }
  }

  private boolean isInductive(CFANode pNode, Set<String> pOutVariables,
      PathFormula formulaSlice)
      throws CPATransferException, InterruptedException {

    Set<CFAEdge> edges = getRelated(pNode);
    for (CFAEdge edge : edges) {
      boolean isInductive = testInductivenessUnderEdge(
        formulaSlice, edge, pOutVariables
      );
      if (!isInductive) {
        return false;
      }
    }
    return true;
  }


  /**
   * @return whether {@code formulaSlice} is inductive under {@code edge}:
   * that is, whether {@code formulaSlice /\ edge /\ NOT formulaSlice} is
   * unsatisfiable.
   */
  private boolean testInductivenessUnderEdge(
      PathFormula formulaSlice,
      CFAEdge edge,
      final Set<String> outVars
  ) throws CPATransferException, InterruptedException {
    PathFormula prefix = pfmgr.makeAnd(formulaSlice, edge);
    final SSAMap outSSA = prefix.getSsa();

    // To generate suffix:
    // 1: apply a second rename to intermediate variables.
    // 2: rename output variables to be input ones (get version from SSA).
    BooleanFormula formulaSliceSuffix = fmgr.renameFreeVariablesAndUFs(formulaSlice.getFormula(),
        new Function<String, String>() {
          @Override
          public String apply(String pInput) {
            if (pInput.startsWith(INTERMEDIATE_VAR_PREFIX)) {
              return pInput + "'";

            } else if (outVars.contains(pInput)) {
              return getOnlyElement(fmgr.instantiate(Collections.singleton(pInput), outSSA));
            }
            return pInput;
          }
        });

    BooleanFormula test = fmgr.makeAnd(prefix.getFormula(),
        fmgr.makeNot(formulaSliceSuffix));

    // Slice is inductive if {@code test} is unsatisfiable.
    boolean isInductive;
    try {
      isInductive = solver.isUnsat(test);
    } catch (SolverException e) {
      throw new CPATransferException("Failed checking unsat", e);
    }
    return isInductive;
  }

  /**
   * @return strongly connected set of edges (for each edge {@code e} in the returned set,
   * there exists a path from {@code node} to {@code e} and from {@code e} to
   * {@code node}.
   */
  private Set<CFAEdge> getRelated(CFANode node) {
    // A := DFS forwards (all edges reachable from N)
    // B := DFS backwards (all edges from where we can reach N)
    // return A intersection B

    EdgeCollectingCFAVisitor v = new EdgeCollectingCFAVisitor();
    CFATraversal.dfs().ignoreSummaryEdges().traverse(node, v);
    Set<CFAEdge> reachableEdges = new HashSet<>(v.getVisitedEdges());

    EdgeCollectingCFAVisitor v2 = new EdgeCollectingCFAVisitor();
    CFATraversal.dfs().ignoreSummaryEdges().backwards().traverse(node, v2);
    Set<CFAEdge> canReachEdges = new HashSet<>(v2.getVisitedEdges());

    return Sets.intersection(reachableEdges, canReachEdges);
  }
}
