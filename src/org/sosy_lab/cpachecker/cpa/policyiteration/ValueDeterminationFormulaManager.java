package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.policyiteration.PolicyState.PolicyAbstractedState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

import com.google.common.base.Preconditions;


public class ValueDeterminationFormulaManager {

  /** Dependencies */
  private final PathFormulaManager pfmgr;
  private final FormulaManager formulaManager;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final LogManager logger;
  private final TemplateManager templateManager;

  /** Private variables. */
  private final int threshold;

  /** Constants */
  private static final String BOUND_VAR_NAME = "BOUND_[%s]_[%s]";
  private static final String EDGE_PREFIX = "[%s]_";

  public ValueDeterminationFormulaManager(
      PathFormulaManager pfmgr,
      FormulaManagerView fmgr,
      LogManager logger,
      CFA cfa,
      FormulaManager rfmgr,
      TemplateManager pTemplateManager
  ) throws InvalidConfigurationException{

    this.pfmgr = pfmgr;
    this.fmgr = fmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.logger = logger;
    this.formulaManager = rfmgr;
    templateManager = pTemplateManager;

    threshold = getThreshold(cfa);
  }

  /**
   * Convert a value determination problem into a single formula.
   *
   * @param policy Selected policy.
   * The abstract state associated with the <code>focusedNode</code>
   * is the <b>new</b> state, with <code>updated</code> applied.
   *
   * @return Global constraint for value determination.
   * @throws CPATransferException
   * @throws InterruptedException
   */
  public List<BooleanFormula> valueDeterminationFormula(
      Map<CFANode, PolicyAbstractedState> policy,
      final CFANode focusedNode,
      final Map<Template, PolicyBound> updated
  ) throws CPATransferException, InterruptedException{
    List<BooleanFormula> constraints = new ArrayList<>();

    for (Entry<CFANode, PolicyAbstractedState> entry : policy.entrySet()) {
      CFANode toNode = entry.getKey();
      PolicyState state = entry.getValue();
      Preconditions.checkState(state.isAbstract());
      Set<String> visitedEdges = new HashSet<>();

      for (Entry<Template, PolicyBound> incoming : state.asAbstracted()) {
        Template template = incoming.getKey();
        PolicyBound bound = incoming.getValue();

        // Prefix the constraints by the edges.
        // We encode the whole paths, as things might differ inside the path.
        String edgePrefix = String.format(EDGE_PREFIX, bound.toPathString());

        if (toNode == focusedNode && !updated.containsKey(template)) {

          // Insert the invariant from the previous constraint.
          Formula templateFormula = templateManager.toFormula(
              template,
              new PathFormula(
                  bfmgr.makeBoolean(true),
                  SSAMap.emptySSAMap(),
                  PointerTargetSet.emptyPointerTargetSet(),
                  0
              ),
              edgePrefix, bound.trace);
          BooleanFormula constraint = fmgr.makeLessOrEqual(
              templateFormula,
              fmgr.makeNumber(templateFormula, bound.bound), true
          );

          constraints.add(constraint);
        } else {
          CFAEdge trace = incoming.getValue().trace;

          CFANode fromNode = trace.getPredecessor();
          int toNodeNo = toNode.getNodeNumber();
          int toNodePrimeNo = toPrime(toNodeNo);

          PathFormula edgePathFormula = pathFormulaWithCustomIdxAndPrefix(
              trace,
              toNodeNo,
              toNodePrimeNo,
              edgePrefix
          );
          BooleanFormula edgeFormula = edgePathFormula.getFormula();

          // Optimization.
          if (!(edgeFormula.equals(bfmgr.makeBoolean(true))
                || visitedEdges.contains(edgePrefix))) {

            // Check for visited.
            constraints.add(edgeFormula);
          }
          if (policy.get(fromNode) == null) {
            // NOTE: nodes with no templates aren't in the policy.
            continue;
          }

          Formula outExpr = templateManager.toFormula(
              template, edgePathFormula, edgePrefix, bound.trace);
          String varName = absDomainVarName(toNode, template);
          BooleanFormula outConstraint;

          outConstraint = fmgr.makeEqual(
              outExpr,
              fmgr.makeVariable(fmgr.getFormulaType(outExpr), varName)
          );

          logger.log(Level.FINE, "Output constraint = ", outConstraint);
          constraints.add(outConstraint);
        }
        visitedEdges.add(edgePrefix);
      }
    }
    return constraints;
  }

  /**
   * Perform the associated maximization.
   */

  /**
   * Create a path formula for the edge, specifying <i>both</i> custom
   * from-index and the custom to-index.
   * E.g. for statement {@code x++}, start index set to 2 and stop index set to 1000
   * will produce:
   *
   *    {@code x@1000 = x@2 + 1}
   */
  private PathFormula pathFormulaWithCustomIdxAndPrefix(
      CFAEdge edge, int startIdx, int stopIdx, String customPrefix)
      throws CPATransferException, InterruptedException {

    PathFormula p = pathFormulaWithCustomStartIdx(edge, startIdx);
    SSAMap customFromIdxSSAMap = p.getSsa();

    SSAMap.SSAMapBuilder newMapBuilder = customFromIdxSSAMap.builder();

    final BooleanFormula edgeFormula = p.getFormula();

    List<Formula> fromVars = new ArrayList<>();
    List<Formula> toVars = new ArrayList<>();

    Set<Triple<Formula, String, Integer>> allVars = fmgr.extractVariables(edgeFormula);
    for (Triple<Formula, String, Integer> e : allVars) {

      Formula formula = e.getFirst();
      Integer oldIdx = e.getThird();
      if (oldIdx == null) {
        oldIdx = 0;
      }
      String varName = e.getSecond();

      CType type = newMapBuilder.getType(varName);
      if (type == null) {
        // A hack. I'm not using types inside the SSAMap, but SSAMap complaints
        // if it gets null.
        type = CNumericTypes.DOUBLE;
      }

      int newIdx;
      if (oldIdx == customFromIdxSSAMap.getIndex(varName)) {

        newIdx = stopIdx;
        newMapBuilder = newMapBuilder.setIndex(varName, type, newIdx);
      } else {
        newIdx = oldIdx;
      }

      fromVars.add(formula);
      toVars.add(makeVariable(formula, varName, newIdx, customPrefix));
    }

    BooleanFormula innerFormula = formulaManager.getUnsafeFormulaManager().substitute(
        edgeFormula, fromVars, toVars
    );

    return new PathFormula(
        innerFormula,
        newMapBuilder.build(),
        p.getPointerTargetSet(),
        p.getLength());
  }

  private Formula makeVariable(
        Formula pFormula, String variable, int idx, String namespace) {
    return fmgr.makeVariable(fmgr.getFormulaType(pFormula), namespace + variable, idx);
  }

  /**
   * Creates a {@link PathFormula} with SSA indexing starting
   * from the specified value.
   * E.g. for {@code x++} and starting index set to 1000 will produce:
   *
   *    x@1001 = x@1000 + 1
   */
  private PathFormula pathFormulaWithCustomStartIdx(CFAEdge edge, int startIdx)
      throws CPATransferException, InterruptedException {
    PathFormula empty = pfmgr.makeEmptyPathFormula();
    PathFormula emptyWithCustomSSA = pfmgr.makeNewPathFormula(
        empty,
        SSAMap.emptySSAMap().withDefault(startIdx));

    return pfmgr.makeAnd(emptyWithCustomSSA, edge);
  }

  /**
   * The formula encoding uses separate numbering conventions for variables
   * associated with the node "input" and the variables associated with the
   * node "output".
   * The later numbering starts with <getThreshold>.
   * The threshold is guaranteed to be a multiple of 10 and bigger than the
   * number of nodes, and at least a thousand (for readability).
   */
  private int getThreshold(CFA cfa) {
    double magnitude = Math.log10(cfa.getAllNodes().size());
    return Math.max(
        1000,
        (int)Math.pow(10, magnitude)
    );
  }

  /**
   * Convert the number from the "input" numbering convention to the "output"
   * numbering convention.
   */
  private int toPrime(int no) {
    return threshold + no;
  }

  String absDomainVarName(CFANode node, Template template) {
    return String.format(BOUND_VAR_NAME, node.getNodeNumber(), template);
  }
}
