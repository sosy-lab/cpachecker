package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;
import org.sosy_lab.cpachecker.util.rationals.LinearConstraint;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import com.google.common.collect.ImmutableMap;

/**
 * Transfer relation for policy iteration.
 */
@Options(prefix="cpa.stator.policy")
public class PolicyTransferRelation  extends
    SingleEdgeTransferRelation implements TransferRelation {


  private final PathFormulaManager pfmgr;
  private final FormulaManagerFactory formulaManagerFactory;
  private final BooleanFormulaManagerView bfmgr;

  private final LinearConstraintManager lcmgr;
  private final LogManager logger;
  private final PolicyAbstractDomain abstractDomain;
  private final FormulaManagerView fmgr;
  private final TemplateManager templateManager;

  /**
   * Lazy evaluation: postpones the analysis until the communication
   * phase with other states.
   */
  public static class LazyState implements AbstractState {
    final PolicyAbstractState previousState;
    public LazyState(PolicyAbstractState pState) {
      previousState = pState;
    }
  }

  @SuppressWarnings("unused")
  public PolicyTransferRelation(
          Configuration config,
          FormulaManagerView formulaManager,
          FormulaManagerFactory formulaManagerFactory,
          PathFormulaManager pfmgr,
          LogManager logger,
          PolicyAbstractDomain abstractDomain,
          LinearConstraintManager lcmgr,
          TemplateManager templateManager
      )
      throws InvalidConfigurationException {

    config.inject(this, PolicyTransferRelation.class);
    fmgr = formulaManager;
    this.pfmgr = pfmgr;
    this.formulaManagerFactory = formulaManagerFactory;
    bfmgr = formulaManager.getBooleanFormulaManager();
    this.lcmgr = lcmgr;
    this.logger = logger;
    this.abstractDomain = abstractDomain;
    this.templateManager = templateManager;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState,
      Precision precision,
      CFAEdge edge
      ) throws CPATransferException, InterruptedException {

    // Lazy evaluation: postpone the analysis until {@code strengthen} is called.
    return Collections.singleton(new LazyState((PolicyAbstractState) pState));
  }

  /**
   * Strengthening is used for communicating the analysis details between
   * various CPAs.
   */
  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      List<AbstractState> otherStates,
      CFAEdge cfaEdge,
      Precision precision) throws CPATransferException, InterruptedException {
    LazyState previousState = (LazyState) state;
    BooleanFormula additionalConstraints = bfmgr.makeBoolean(true);
    for (AbstractState otherState : otherStates) {
      if (otherState instanceof FormulaReportingState) {
        FormulaReportingState fState = (FormulaReportingState) otherState;
        additionalConstraints = bfmgr.and(additionalConstraints,
            fState.getFormulaApproximation(fmgr));
      }
    }
    return getAbstractSuccessors(previousState.previousState,
        cfaEdge, additionalConstraints);
  }

  public Collection<PolicyAbstractState> getAbstractSuccessors(
      PolicyAbstractState prevState,
      CFAEdge edge,
      BooleanFormula additionalConstraints
  ) throws CPATransferException, InterruptedException {

    logger.log(Level.FINE, ">>> Processing statement: " + edge.getCode()
     + " for to-node: " + edge.getSuccessor());

    CFANode toNode = edge.getSuccessor();

    // Formula representing the edge.
    PathFormula edgeFormula = pfmgr.makeFormulaForPath(
        Collections.singletonList(edge));

    /** Propagating templates */
    PolicyAbstractState.Templates toTemplates = templateManager.updatePrecisionForEdge(
        prevState.getTemplates(), edge);

    /** Propagate the invariants */
    ImmutableMap.Builder<LinearExpression, PolicyTemplateBound> newStateData;
    newStateData = ImmutableMap.builder();

    for (LinearExpression template : toTemplates) {
      try (OptEnvironment solver = formulaManagerFactory.newOptEnvironment()) {

        // Constraints imposed by other CPAs.
        solver.addConstraint(additionalConstraints);

        // Constraints imposed by the previous state.
        SSAMap ssaMap = edgeFormula.getSsa();

        // All the used variables in the additional constraints from other states.
        Set<String> usedVariablesWithIdx = fmgr.extractVariables(additionalConstraints);

        // Update SSA map.
        for (String var : usedVariablesWithIdx) {
          Pair<String, Integer> p = FormulaManagerView.parseName(var);
          Integer idx = p.getSecond();
          String varName = p.getFirst();

          // TODO: a hack to detect a pointer.
          if (varName != null && !varName.contains("*") && idx != null && idx > 1) {
            // TODO: ctype?? is it even used anywhere?
            SSAMap.SSAMapBuilder builder = ssaMap.builder();
            CType intType = new CSimpleType(
              false, false, CBasicType.INT, false, false, true, false, false, false, false
            );
            builder = builder.setIndex(varName, intType, idx);
            ssaMap = builder.build();
          }
        }

        SSAMap inputSSA = SSAMap.emptySSAMap().withDefault(1);

        // Constraints from the previous state.
        for (Map.Entry<LinearExpression, PolicyTemplateBound> item : prevState) {
          // TODO: Do not re-add the constraints for each optimization.
          // If anything we can store them in a list.
          LinearExpression expr = item.getKey();
          ExtendedRational bound = item.getValue().bound;

          LinearConstraint constraint = new LinearConstraint(expr, bound);
          solver.addConstraint(
              lcmgr.linearConstraintToFormula(constraint, inputSSA));
        }

        // Constraints imposed by the edge.
        solver.addConstraint(edgeFormula.getFormula());

        ExtendedRational value = lcmgr.maximize(solver, template, ssaMap);

        // If the state is not reachable, bail early.
        if (value == ExtendedRational.NEG_INFTY) {
          logger.log(Level.FINE, "# Stopping, unfeasible branch.");
          return Collections.emptyList();
        } else if (value != ExtendedRational.INFTY) {
          PolicyTemplateBound constraint = PolicyTemplateBound.of(edge, value);
          logger.log(Level.FINE, "# Updating constraint on node " + toNode  +
              " template " + template + " to " + constraint);
          newStateData.put(template, constraint);
        }

        // Note: this is a hack.
        // If the policy for the old-state is prevailing, the join
        // operator will reset the global object to the "good" state.
        abstractDomain.setPolicyForTemplate(toNode, template, edge);

      } catch (Exception e) {
        e.printStackTrace();
        throw new CPATransferException("Failed solving", e);
      }
    }

    PolicyAbstractState newState = PolicyAbstractState.withState(
        newStateData.build(), toTemplates, toNode);

    logger.log(Level.FINE, "# New state = " + newState);
    return Collections.singleton(newState);
  }
}
