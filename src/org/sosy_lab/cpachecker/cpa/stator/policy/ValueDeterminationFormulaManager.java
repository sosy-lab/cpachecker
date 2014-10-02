package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
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

/**
 * All our SSA-customization code should go there.
 */
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

  @SuppressWarnings("unused")
  public ValueDeterminationFormulaManager(
      PathFormulaManager pfmgr,
      FormulaManagerView fmgr,
      Configuration config,
      LogManager logger,
      ShutdownNotifier shutdownNotifier,
      MachineModel machineModel,
      CFA cfa,
      FormulaManager rfmgr,
      LinearConstraintManager lcmgr
  ) {
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
  // NOTE: I am doing something which seems quite silly here.
  // Iteration over all the nodes should not be required, I should iterate
  // only over the reachable ones [or better yet, the ones involved in a created
  // cycle.
  // But such an improvement can be left as a todo-item for later.
   */
  public List<BooleanFormula> valueDeterminationFormula(
      final Map<CFANode, Map<LinearExpression, CFAEdge>> policy
      ) throws CPATransferException, InterruptedException{

    List<BooleanFormula> constraints = new LinkedList<>();

    for (Entry<CFANode, Map<LinearExpression, CFAEdge>> entry : policy.entrySet()) {

      CFANode toNode = entry.getKey();
      for (Entry<LinearExpression, CFAEdge> incoming : entry.getValue().entrySet()) {

        LinearExpression template = incoming.getKey();
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
        if (policy.get(fromNode) == null) {
          // NOTE: nodes with no templates aren't in the policy.
          continue;
        }

        for (LinearExpression fromTemplate : policy.get(fromNode).keySet()) {

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

        logger.log(Level.FINE, "Output constraint = " + outConstraint);
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
   *    x@1000 = x@2 + 1
   */
  private PathFormula pathFormulaWithCustomIdx(
      CFAEdge edge, int startIdx, int stopIdx, String customPrefix)
      throws CPATransferException, InterruptedException {

    PathFormula p = pathFormulaWithCustomStartIdx(edge, startIdx);
    SSAMap customFromIdxSSAMap = p.getSsa();

    SSAMap.SSAMapBuilder newMapBuilder = customFromIdxSSAMap.builder();

    Formula edgeFormula = p.getFormula();

    List<Formula> fromVars = new LinkedList<>();
    List<Formula> toVars = new LinkedList<>();

    Set<String> allVars = fmgr.extractVariables(edgeFormula);
    for (String varNameWithIdx : allVars) {

      Pair<String, Integer> pair = FormulaManagerView.parseName(varNameWithIdx);
      Integer oldIdx = pair.getSecond(); // TODO: can be null?..
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
}
