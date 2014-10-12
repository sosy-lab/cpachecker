package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;


@Options(prefix="cpa.stator.policy")
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

  @Option(
      name="pathFocusing",
      description="Run (simplified) path focusing")
  private boolean pathFocusing = true;

  public ValueDeterminationFormulaManager(
      PathFormulaManager pfmgr,
      FormulaManagerView fmgr,
      Configuration config,
      LogManager logger,
      CFA cfa,
      FormulaManager rfmgr,
      LinearConstraintManager lcmgr
  ) throws InvalidConfigurationException{

    config.inject(this, ValueDeterminationFormulaManager.class);

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

    if (pathFocusing) {
      policy = pathFocusing(policy, focusedNode);
    }

    Map<CFANode, ? extends Map<LinearExpression, ? extends CFAEdge>> policyMap
        = policy.rowMap();

    List<BooleanFormula> constraints = new ArrayList<>();

    for (Entry<CFANode, ? extends Map<LinearExpression, ? extends CFAEdge>> entry : policyMap.entrySet()) {

      CFANode toNode = entry.getKey();
      for (Entry<LinearExpression, ? extends CFAEdge> incoming : entry.getValue().entrySet()) {


        LinearExpression template = incoming.getKey();
        if (toNode == focusedNode && !updated.contains(template)) continue;

        CFAEdge incomingEdge = incoming.getValue();

        String templatePrefix = String.format("tmpl_[%s]_", template);

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

    Set<String> allVars = fmgr.extractVariables(edgeFormula);
    for (String varNameWithIdx : allVars) {

      Pair<String, Integer> pair = FormulaManagerView.parseName(varNameWithIdx);
      Integer oldIdx = pair.getSecond(); // TODO: why can it be null?..
      if (oldIdx == null) {
        oldIdx = 0;
      }
      String varName = pair.getFirst();

      CType type = newMapBuilder.getType(varName);

      int newIdx;
      if (oldIdx != startIdx) {
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
        (int) Math.pow(10, magnitude)
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

  private Table<CFANode, LinearExpression, ? extends CFAEdge> pathFocusing(
      Table<CFANode, LinearExpression, ? extends CFAEdge> policy,
      CFANode focusedNode) {

    return fixpointFocusing(convert(policy), focusedNode);
  }

  /**
   * Change every edge to multi-edge.
   */
  private Table<CFANode, LinearExpression, MultiEdge> convert(
      Table<CFANode, LinearExpression, ? extends CFAEdge> t
  ) {
    Table<CFANode, LinearExpression, MultiEdge> out = HashBasedTable.create();
    for (Table.Cell<CFANode, LinearExpression, ? extends CFAEdge> cell : t.cellSet()) {
      CFAEdge edge = cell.getValue();
      out.put(
          cell.getRowKey(),
          cell.getColumnKey(),
          new MultiEdge(edge.getPredecessor(), edge.getSuccessor(), ImmutableList.of(edge)));
    }
    return out;
  }

  /**
   *
   * @param policy Policy.
   * @param focusedOn Loop head we are performing value determination on.
   * Can not be thrown out.
   *
   * @return Focused policy.
   * Usually should contain only one node?..
   */
  private Table<CFANode, LinearExpression, ? extends CFAEdge> fixpointFocusing(
      Table<CFANode, LinearExpression, MultiEdge> policy,
      final CFANode focusedOn
  ) {
    boolean changed = true; // For the initial iteration.
    while (changed) {

      changed = false;
      Multimap<CFANode, CFANode> incoming = HashMultimap.create();
      Multimap<CFANode, CFANode> outgoing = HashMultimap.create();

      // Step 1: Fill in [incoming] and [outgoing] maps in O(N).
      for (Table.Cell<CFANode, LinearExpression, MultiEdge> cell : policy.cellSet()) {
        CFANode to = cell.getRowKey();
        CFANode from = cell.getValue().getPredecessor();

        outgoing.put(from, to);
        incoming.put(to, from);
      }

      for (Entry<CFANode, Collection<CFANode>> e :  incoming.asMap().entrySet()) {
        final CFANode mid = e.getKey();

        // We don't try to eliminate the node we are focusing on.
        if (mid == focusedOn) continue;

        Collection<CFANode> incomingNodes = e.getValue();
        Collection<CFANode> outgoingNodes = outgoing.get(mid);
        assert (incomingNodes.size() != 0 && outgoingNodes.size() != 0);

        // A mid-node has only one incoming edge and only one
        // outgoing edge, and CAN be eliminated if all edges between <to>
        // and <from> nodes are the same.
        // We only need to update the policy on the to-node after the elimination.
        if (incomingNodes.size() == 1 && outgoingNodes.size() == 1) {
          CFANode from = incomingNodes.iterator().next();
          CFANode to = outgoingNodes.iterator().next();

          Map<LinearExpression, MultiEdge> midRow, toRow;
          midRow = policy.row(mid);
          toRow = policy.row(to);

          // Check that all edges to the mid- row are equal.
          final MultiEdge fromToMid = midRow.values().iterator().next();

          boolean allMidEqual = Iterables.all(
              midRow.values(),
              new Predicate<MultiEdge>() {
            public boolean apply(MultiEdge input) {
              return input.equals(fromToMid);
            }
          });

          // Can't handle disjunctions for now.
          if (!allMidEqual) continue;

          for (MultiEdge multiEdge : midRow.values()) {
            Preconditions.checkState(multiEdge.equals(
                midRow.values().iterator().next()));
          }

          final MultiEdge midToTo = toRow.values().iterator().next();
          boolean allEqual = Iterables.all(toRow.values(), new Predicate<MultiEdge>() {
            public boolean apply(MultiEdge input) {
              return input.equals(midToTo);
            }
          });

          // We can only change things if all edges are equal.
          if (!allEqual) continue;

          final MultiEdge fromToTo = new MultiEdge(
              from, to, ImmutableList.<CFAEdge>builder()
              .addAll(fromToMid.getEdges()).addAll(midToTo.getEdges()).build());

          // Remove the med row.
          policy.rowMap().remove(mid);

          // Update to-map.
          for (LinearExpression template : policy.row(to).keySet()) {
            policy.put(to, template, fromToTo);
          }

          changed = true;
          break;
        }
      }
    }
    return policy;
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
