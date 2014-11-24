package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;


// TODO: don't perform namespacing if the node has only one incoming edge.
// why don't we namespace on edges instead of namespacing on templates?
public class ValueDeterminationFormulaManager {
  private final PathFormulaManager pfmgr;
  private final FormulaManager formulaManager;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final NumeralFormulaManagerView<NumeralFormula, RationalFormula> rfmgr;
  private final LogManager logger;
  private final LinearConstraintManager lcmgr;

  private final int threshold;

  private static final String BOUND_VAR_NAME = "BOUND_[%s]_[%s]";
  private static final String TEMPLATE_PREFIX = "[%s]_";

  @SuppressWarnings("unused")
  public ValueDeterminationFormulaManager(
      PathFormulaManager pfmgr,
      FormulaManagerView fmgr,
      Configuration config,
      LogManager logger,
      CFA cfa,
      FormulaManager rfmgr,
      LinearConstraintManager lcmgr
  ) throws InvalidConfigurationException{

    this.pfmgr = pfmgr;
    this.fmgr = fmgr;
    this.rfmgr = fmgr.getRationalFormulaManager();
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.logger = logger;
    this.formulaManager = rfmgr;
    this.lcmgr = lcmgr;

    threshold = getThreshold(cfa);
  }

  /**
   * Convert a value determination problem into a single formula.
   *
   * @param policy Selected policy
   * @return Global constraint for value determination.
   * @throws CPATransferException
   * @throws InterruptedException
   */
  public List<BooleanFormula> valueDeterminationFormula(
      Table<CFANode, LinearExpression, ? extends CFAEdge> policy,
      final CFANode focusedNode,
      final Set<LinearExpression> updated
      ) throws CPATransferException, InterruptedException{

    Map<CFANode, ? extends Map<LinearExpression, ? extends CFAEdge>> policyMap
        = policy.rowMap();

    List<BooleanFormula> constraints = new ArrayList<>();

    for (Entry<CFANode, ? extends Map<LinearExpression, ? extends CFAEdge>> entry : policyMap.entrySet()) {

      CFANode toNode = entry.getKey();
      for (Entry<LinearExpression, ? extends CFAEdge> incoming : entry.getValue().entrySet()) {
        LinearExpression template = incoming.getKey();

        // Don't perform value determination on templates not updated during
        // the iteration.
        if (toNode == focusedNode && !updated.contains(template)) {
          continue;
        }

        CFAEdge incomingEdge = incoming.getValue();

        String templatePrefix = String.format(TEMPLATE_PREFIX, template);

        CFANode fromNode = incomingEdge.getPredecessor();
        int toNodeNo = toNode.getNodeNumber();
        int toNodePrimeNo = toPrime(toNodeNo);

        PathFormula edgePathFormula = pathFormulaWithCustomIdx(
            incomingEdge,
            toNodeNo,
            toNodePrimeNo,
            templatePrefix
        );
        BooleanFormula edgeFormula = edgePathFormula.getFormula();

        if (!edgeFormula.equals(bfmgr.makeBoolean(true))) {
          constraints.add(edgeFormula);
        }
        if (policyMap.get(fromNode) == null) {
          // NOTE: nodes with no templates aren't in the policy.
          continue;
        }

        for (LinearExpression fromTemplate : policyMap.get(fromNode).keySet()) {

          // Add input constraints on the edge variables.
          NumeralFormula edgeInput = lcmgr.linearExpressionToFormula(
              fromTemplate,
              SSAMap.emptySSAMap().withDefault(toNodeNo),
              templatePrefix
          );

          BooleanFormula f = rfmgr.lessOrEquals(
              edgeInput,
              rfmgr.makeVariable(absDomainVarName(fromNode, fromTemplate)));

          constraints.add(f);
        }

        NumeralFormula outExpr = lcmgr.linearExpressionToFormula(
            template, edgePathFormula.getSsa(), templatePrefix);

        NumeralFormula out = rfmgr.makeVariable(absDomainVarName(toNode, template));

        BooleanFormula outConstraint = rfmgr.equal(outExpr, out);

        logger.log(Level.FINE, "Output constraint = ", outConstraint);
        constraints.add(outConstraint);
      }
    }
    return constraints;
  }

  /**
   * Create a path formula for the edge, specifying <i>both</i> custom
   * from-index and the custom to-index.
   * E.g. for statement {@code x++}, start index set to 2 and stop index set to 1000
   * will produce:
   *
   *    {@code x@1000 = x@2 + 1}
   */
  private PathFormula pathFormulaWithCustomIdx(
      CFAEdge edge, int startIdx, int stopIdx, String customPrefix)
      throws CPATransferException, InterruptedException {

    PathFormula p = pathFormulaWithCustomStartIdx(edge, startIdx);
    SSAMap customFromIdxSSAMap = p.getSsa();

    SSAMap.SSAMapBuilder newMapBuilder = customFromIdxSSAMap.builder();

    Formula edgeFormula = p.getFormula();

    List<Formula> fromVars = new ArrayList<>();
    List<Formula> toVars = new ArrayList<>();

    Set<String> allVars = fmgr.extractVariableNames(edgeFormula);
    for (String varNameWithIdx : allVars) {

      Pair<String, Integer> pair = FormulaManagerView.parseName(varNameWithIdx);
      Integer oldIdx = pair.getSecond();
      if (oldIdx == null) {
        oldIdx = 0;
      }
      String varName = pair.getFirst();

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

      fromVars.add(makeVariable(varName, oldIdx, ""));

      toVars.add(makeVariable(varName, newIdx, customPrefix));
    }

    BooleanFormula innerFormula = formulaManager.getUnsafeFormulaManager().substitute(
        p.getFormula(), fromVars, toVars
    );

    return new PathFormula(
        innerFormula,
        newMapBuilder.build(),
        p.getPointerTargetSet(),
        p.getLength());
  }

  private NumeralFormula makeVariable(String variable, int idx, String namespace) {
    return rfmgr.makeVariable(namespace + variable, idx);
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

  String absDomainVarName(CFANode node, LinearExpression template) {
    return String.format(BOUND_VAR_NAME, node.getNodeNumber(), template);
  }

  /**
   * @return the subset of the <code>abstractStates</code> related to the given
   * <code>valueDeterminationNode</code> and the set of <code>updates</code>.
   *
   * <p>Note that the returned set is usually an over-approximation, because we
   * are not tracking the actual relationships between variables.
   */
  Table<CFANode, LinearExpression, CFAEdge> findRelated(
      final Map<CFANode, PolicyAbstractState> abstractStates,
      final CFANode focusedNode,
      final Map<LinearExpression, PolicyBound> updated) throws InterruptedException {

    Table<CFANode, LinearExpression, CFAEdge> out = HashBasedTable.create();
    Set<CFANode> visited = Sets.newHashSet();

    // Problems started when the same thing was added twice to the queue...
    LinkedHashSet<CFANode> queue = new LinkedHashSet<>();
    queue.add(focusedNode);

    while (!queue.isEmpty()) {
      Iterator<CFANode> it = queue.iterator();
      CFANode node = it.next();
      it.remove();

      visited.add(node);

      PolicyAbstractState state = abstractStates.get(node);
      for (Map.Entry<LinearExpression, PolicyBound> entry : state) {
        LinearExpression template = entry.getKey();
        PolicyBound stateBound = entry.getValue();

        CFAEdge edge;

        // For the value determination node only track the updated edges.
        // NOTE: don't forget to add the constraints from others though!
        if (node == focusedNode) {

          // Actually the same issue here, we need to use the merged node
          // instead of the original node the first time around.
          PolicyBound bound = updated.get(template);

          // Only keep track of templates which were updated.
          if (bound == null) {
            continue;
          }
          edge = bound.trace;
        } else {
          edge = stateBound.trace;
        }

        // Put things related to the node.
        out.put(node, template, edge);

        CFANode toVisit = edge.getPredecessor();
        if (!visited.contains(toVisit)) {
          queue.add(toVisit);
        }
      }
    }
    return out;
  }

  /**
   * Useful for debugging.
   */
  @SuppressWarnings("unused")
  private void pprintPolicyTable(Table<CFANode, LinearExpression, ? extends CFAEdge> t) {
    Map<CFANode, ? extends Map<LinearExpression, ? extends CFAEdge>> m = t.rowMap();
    for (CFANode node : m.keySet()) {
      Map<LinearExpression, ? extends CFAEdge> data = m.get(node);
      logger.log(Level.FINE, node, ": \n");
      for (Entry<LinearExpression, ? extends CFAEdge> entry : data.entrySet()) {
        CFAEdge edge = entry.getValue();

        logger.log(Level.FINE, "\t", entry.getKey(), edge.getCode(),
            edge.getPredecessor(), "->", edge.getSuccessor());
      }
    }
  }
}
