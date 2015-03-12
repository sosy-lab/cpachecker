package org.sosy_lab.cpachecker.cpa.policyiteration;

import static com.google.common.collect.Iterables.getOnlyElement;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class FormulaSlicingManager {
  private final LogManager logger;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final PathFormulaManager pfmgr;
  private final Solver solver;
  private final Timer slicingTime = new Timer();

  private static final String POINTER_ADDR_VAR_NAME = "ADDRESS_OF";
  private static final String INTERMEDIATE_VAR_PREFIX = "__SLICE_INTERMEDIATE_";

  public FormulaSlicingManager(LogManager pLogger,
      PathFormulaManager pPfmgr,
      Solver pSolver) {
    logger = pLogger;
    solver = pSolver;
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pfmgr = pPfmgr;
  }

  public Timer getSlicingTime() {
    return slicingTime;
  }

  /**
   * @return Over-approximation of the formula {@code f} which deals only
   * with pointers.
   */
  public BooleanFormula pointerFormulaSlice(BooleanFormula f) {
    Set<String> closure = findClosure(f, new Predicate<String>() {
      @Override
      public boolean apply(String input) {
        return input.contains(POINTER_ADDR_VAR_NAME);
      }
    });
    logger.log(Level.FINE, "Closure =", closure);
    BooleanFormula slice = new RecursiveSliceVisitor(ImmutableSet.copyOf(closure)).visit(f);
    logger.log(Level.FINE, "Produced =", slice);
    return slice;
  }

  /**
   * @return Closure with respect to interacts-relation of
   * <b>uninstantiated</b> variables in {@code f} which satisfy the condition
   * {@code seedCondition}.
   */
  private Set<String> findClosure(BooleanFormula f, Predicate<String> seedCondition) {
    Set<String> closure = new HashSet<>();
    Collection<BooleanFormula> atoms = fmgr.extractAtoms(f, false, false);
    boolean changed = true;
    while (changed) {
      changed = false;
      for (BooleanFormula atom : atoms) {
        Set<String> variableNames = fmgr.extractFunctionNames(atom);
        for (String s : variableNames) {
          if (seedCondition.apply(s) || closure.contains(s)) {
            changed |= closure.addAll(variableNames);
            break;
          }
        }
      }
    }
    return closure;
  }

  private class RecursiveSliceVisitor extends BooleanFormulaManagerView.BooleanFormulaTransformationVisitor {

    private final boolean isInsideNot;
    private final Set<String> closure;

    // We need to handle negated formulas differently from non-negated formulas,
    // and we need a separate super.cache for negated/non-negated formulas
    // (Example: in ((a & b) | (!a & c)), "a" needs to be replaced once by "true"
    // and once by "false").
    // Thus we need two visitor instances with different settings for isInsideNot,
    // and they both delegate to the other when encountering a negation.
    private RecursiveSliceVisitor visitorForNegatedFormula;

    RecursiveSliceVisitor(Set<String> pClosure) {
      this(false, pClosure);

      visitorForNegatedFormula = new RecursiveSliceVisitor(true, pClosure);
      visitorForNegatedFormula.visitorForNegatedFormula = this;
    }

    RecursiveSliceVisitor(boolean pIsInsideNot, Set<String> pClosure) {
      super(fmgr, new HashMap<BooleanFormula, BooleanFormula>());
      isInsideNot = pIsInsideNot;
      closure = pClosure;
    }

    @Override
    protected BooleanFormula visitAtom(BooleanFormula f) {
      Formula uninstantiatedF = fmgr.uninstantiate(f);
      Set<String> containedVariables = fmgr.extractFunctionNames(uninstantiatedF);
      if (!Sets.intersection(closure, containedVariables).isEmpty()) {
        return f;
      } else {
        // Hack to propagate the call variables,
        if (containedVariables.size() == 2) {
          Iterator<String> iterator = containedVariables.iterator();
          String first = iterator.next();
          String second = iterator.next();
          if (first.contains("::") && second.contains("::") &&
              !first.substring(0, first.indexOf("::")).equals(
              second.substring(0, second.indexOf("::"))
          )) {
            return f;
          } else {
            return bfmgr.makeBoolean(!isInsideNot);
          }
        } else {
          return bfmgr.makeBoolean(!isInsideNot);
        }
      }
    }

    @Override
    protected BooleanFormula visitNot(BooleanFormula pOperand) {
      return bfmgr.not(visitorForNegatedFormula.visitIfNotSeen(pOperand));
    }
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
      return bfmgr.makeBoolean(true);
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
      slicingTime.start();
      isInductive = solver.isUnsat(test);
    } catch (SolverException e) {
      throw new CPATransferException("Failed checking unsat", e);
    } finally {
      slicingTime.stop();
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
