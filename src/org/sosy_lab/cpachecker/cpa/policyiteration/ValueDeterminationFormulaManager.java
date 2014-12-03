package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.ArrayList;
import java.util.HashSet;
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
import org.sosy_lab.cpachecker.cpa.policyiteration.PolicyState.PolicyAbstractedState;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;


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
  private final FormulaManagerFactory formulaManagerFactory;
  private final PolicyIterationStatistics statistics;

  private final int threshold;

  private static final String BOUND_VAR_NAME = "BOUND_[%s]_[%s]";
  private static final String TEMPLATE_PREFIX = "[%s]_";
  private final ShutdownNotifier shutdownNotifier;

  @SuppressWarnings("unused")
  public ValueDeterminationFormulaManager(
      PathFormulaManager pfmgr,
      FormulaManagerView fmgr,
      Configuration config,
      LogManager logger,
      CFA cfa,
      FormulaManager rfmgr,
      LinearConstraintManager lcmgr,
      FormulaManagerFactory pFormulaManagerFactory,
      ShutdownNotifier pShutdownNotifier,
      PolicyIterationStatistics pStatistics) throws InvalidConfigurationException{

    this.pfmgr = pfmgr;
    this.fmgr = fmgr;
    this.rfmgr = fmgr.getRationalFormulaManager();
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.logger = logger;
    this.formulaManager = rfmgr;
    this.lcmgr = lcmgr;
    formulaManagerFactory = pFormulaManagerFactory;
    shutdownNotifier = pShutdownNotifier;
    statistics = pStatistics;

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
      final Map<LinearExpression, PolicyBound> updated
  ) throws CPATransferException, InterruptedException{
    List<BooleanFormula> constraints = new ArrayList<>();

    for (Entry<CFANode, PolicyAbstractedState> entry : policy.entrySet()) {
      CFANode toNode = entry.getKey();
      PolicyState state = entry.getValue();
      Preconditions.checkState(state.isAbstract());

      for (Entry<LinearExpression, PolicyBound> incoming : state.asAbstracted()) {
        LinearExpression template = incoming.getKey();
        String templatePrefix = String.format(TEMPLATE_PREFIX, template);

        if (toNode == focusedNode && !updated.containsKey(template)) {

          // Insert the invariant from the previous constraints.
          // Do not follow the trace.
          PolicyBound bound = incoming.getValue();
          NumeralFormula templateFormula = lcmgr.linearExpressionToFormula(
              template, SSAMap.emptySSAMap(), templatePrefix);
          BooleanFormula constraint = rfmgr.lessOrEquals(
              templateFormula, rfmgr.makeNumber(bound.bound.toString())
          );
          constraints.add(constraint);
        } else {
          CFAEdge trace = incoming.getValue().trace;


          CFANode fromNode = trace.getPredecessor();
          int toNodeNo = toNode.getNodeNumber();
          int toNodePrimeNo = toPrime(toNodeNo);

          PathFormula edgePathFormula = pathFormulaWithCustomIdx(
              trace,
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

          for (Entry<LinearExpression, PolicyBound> fromEntry : policy.get(fromNode)) {
            LinearExpression fromTemplate = fromEntry.getKey();

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
    }
    return constraints;
  }

  /**
   * Perform the associated maximization.
   */
  PolicyAbstractedState valueDeterminationMaximization(
      PolicyAbstractedState prevState,
      Set<Template> templates,
      Map<LinearExpression, PolicyBound> updated,
      CFANode node,
      List<BooleanFormula> pValueDeterminationConstraints,
      int epsilon
  )
      throws InterruptedException, CPATransferException {

    ImmutableMap.Builder<LinearExpression, PolicyBound> builder = ImmutableMap.builder();
    Set<LinearExpression> unbounded = new HashSet<>();

    // Maximize for each template subject to the overall constraints.
    statistics.valueDeterminationSolverTimer.start();
    statistics.valueDetCalls++;
    try (OptEnvironment solver = formulaManagerFactory.newOptEnvironment()) {
      shutdownNotifier.shutdownIfNecessary();

      for (BooleanFormula constraint : pValueDeterminationConstraints) {
        solver.addConstraint(constraint);
      }

      for (Entry<LinearExpression, PolicyBound> policyValue : updated.entrySet()) {
        LinearExpression template = policyValue.getKey();
        CFAEdge policyEdge = policyValue.getValue().trace;
        logger.log(Level.FINE,
            "# Value determination: optimizing for template" , template);

        NumeralFormula objective = rfmgr.makeVariable(
            absDomainVarName(node, template));

        solver.push();
        solver.maximize(objective);

        OptEnvironment.OptStatus result = solver.check();
        switch (result) {
          case OPT:
            builder.put(
                template, PolicyBound.of(policyEdge, solver.value(epsilon)));
            break;
          case UNBOUNDED:
            unbounded.add(template);
            break;
          case UNSAT:
            throw new SolverException("" +
                "Unexpected solver state, value determination problem" +
                " should be feasible");
          case UNDEF:
            throw new SolverException("Unexpected solver status");
        }
        solver.pop();
      }
    } catch (SolverException e) {
      throw new CPATransferException("Failed maximization", e);
    } finally {
      statistics.valueDeterminationSolverTimer.stop();
    }

    return prevState.withUpdates(builder.build(), unbounded, templates);
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
}
