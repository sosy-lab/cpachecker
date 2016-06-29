package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

public class ValueDeterminationManager {

  /** Dependencies */
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final LogManager logger;
  private final PathFormulaManager pfmgr;
  private final StateFormulaConversionManager stateFormulaConversionManager;

  /** Constants */
  private static final String BOUND_VAR_NAME = "BOUND_[%s]_[%s]";
  private static final String VISIT_PREFIX = "[%d]_";

  public ValueDeterminationManager(
      FormulaManagerView fmgr,
      LogManager logger,
      PathFormulaManager pPfmgr,
      StateFormulaConversionManager pStateFormulaConversionManager) {

    this.fmgr = fmgr;
    stateFormulaConversionManager = pStateFormulaConversionManager;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.logger = logger;
    pfmgr = pPfmgr;
  }

  static class ValueDeterminationConstraints {
    final ImmutableTable<Template, Integer, Formula> outVars;
    final ImmutableSet<BooleanFormula> constraints;

    private ValueDeterminationConstraints(
        ImmutableTable<Template, Integer, Formula> pOutVars,
        ImmutableSet<BooleanFormula> pConstraints) {
      outVars = pOutVars;
      constraints = pConstraints;
    }
  }

  /**
   * Cheaper version of value determination.
   * May under-approximate the true result (the resulting constraint system is
   * strictly stronger due to sharing variables).
   */
  public ValueDeterminationConstraints valueDeterminationFormulaCheap(
      PolicyAbstractedState stateWithUpdates,
      Set<Template> updated
  ) {
    return valueDeterminationFormula(stateWithUpdates, updated, false);
  }

  /**
   * Sound value determination procedure.
   */
  public ValueDeterminationConstraints valueDeterminationFormula(
      PolicyAbstractedState stateWithUpdates,
      Set<Template> updated
  ) {
    return valueDeterminationFormula(stateWithUpdates, updated, true);
  }

  /**
   * Convert a value determination problem into a single formula.
   *
   * @param stateWithUpdates Newly created state
   * @param updated Set of updates templates for the {@code stateWithUpdates}
   * @param useUniquePrefix Flag on whether to use a unique prefix for each policy
   *
   * The abstract state associated with the <code>focusedNode</code>
   * is the <b>new</b> state, with <code>updated</code> applied.
   *
   * @return Global constraint for value determination and
   * table <code>(template + location) -> formula</code> denoting the abstract
   * value.
   */
  private ValueDeterminationConstraints valueDeterminationFormula(
      PolicyAbstractedState stateWithUpdates,
      Set<Template> updated,
      boolean useUniquePrefix
  ) {
    Set<BooleanFormula> outConstraints = new HashSet<>();
    Table<Template, Integer, Formula> outVars = HashBasedTable.create();

    long uniquePrefix = 0;

    Set<Integer> visitedLocationIDs = new HashSet<>();

    LinkedHashSet<PolicyAbstractedState> queue = new LinkedHashSet<>();
    queue.add(stateWithUpdates);

    while (!queue.isEmpty()) {
      Iterator<PolicyAbstractedState> it = queue.iterator();
      PolicyAbstractedState state = it.next();
      it.remove();

      visitedLocationIDs.add(state.getLocationID());

      for (Entry<Template, PolicyBound> incoming : state) {
        Template template = incoming.getKey();
        PolicyBound bound = incoming.getValue();
        PolicyAbstractedState backpointer = bound.getPredecessor();

        boolean valueIsFixed = bound.getDependencies().isEmpty()
            ||  (state == stateWithUpdates
              && !updated.contains(template)
              && !bound.isComputedByValueDetermination()
        );

        // Update the queue, check visited.
        if (!valueIsFixed &&
            bound.getDependencies().contains(template)
            && !visitedLocationIDs.contains(backpointer.getLocationID())) {

          queue.add(backpointer);
        }

        // Give the element to the constraint generator.
        String prefix = useUniquePrefix ?
                        String.format(VISIT_PREFIX, ++uniquePrefix) :

                        // Merge variables sharing the same policy.
                        String.format(VISIT_PREFIX, bound.serializePolicy(state));

        generateConstraintsFromPolicyBound(
            bound,
            state.getLocationID(),
            template,
            backpointer.getLocationID(),
            prefix,
            valueIsFixed,
            outConstraints,
            outVars
        );
      }
    }

    return new ValueDeterminationConstraints(
        ImmutableTable.copyOf(outVars),
        ImmutableSet.copyOf(outConstraints));
  }

  /**
   * Process and add constraints from a single policy.
   *
   * @param bound {@link PolicyBound} to generate constraints from
   * @param locationID location ID associated with the to-state.
   * @param template Template associated to {@code bound}
   * @param policyBackpointerLocationID Location ID associated to backpointer.
   * @param prefix Unique namespace for the policy
   * @param valueFixed Flag to indicate that the policy value is fixed
   *                       and does not depend on anything.
   * @param outConstraints Output set to write constraints to.
   * @param outVars Output table to record generated variables.
   */
  private void generateConstraintsFromPolicyBound(
      PolicyBound bound,
      int locationID,
      Template template,
      int policyBackpointerLocationID,
      String prefix,
      boolean valueFixed,
      Set<BooleanFormula> outConstraints,
      Table<Template, Integer, Formula> outVars
  ) {
    PathFormula policyFormula = bound.getFormula();

    PathFormula startPathFormula = stateFormulaConversionManager.getPathFormula(
        bound.getPredecessor(), fmgr, true);

    Formula policyOutTemplate = addPrefix(
        stateFormulaConversionManager.toFormula(pfmgr, fmgr, template, policyFormula),
        prefix);
    Formula outVar =
        fmgr.makeVariable(fmgr.getFormulaType(policyOutTemplate),
            absDomainVarName(locationID, template));
    outVars.put(template, locationID, outVar);

    if (valueFixed) {
      logger.log(Level.FINE, "Fixed value for template", template);
      BooleanFormula constraint = fmgr.makeLessOrEqual(outVar,
              fmgr.makeNumber(policyOutTemplate, bound.getBound()), true);
      outConstraints.add(constraint);
      return;
    }

    BooleanFormula outConstraint = fmgr.makeLessOrEqual(outVar,
        policyOutTemplate, true);
    outConstraints.add(outConstraint);

    BooleanFormula namespacedPolicy = addPrefix(policyFormula.getFormula(), prefix);

    if (!bfmgr.isTrue(namespacedPolicy)) {
      outConstraints.add(namespacedPolicy);
    }

    // Process incoming constraints on the policy start.
    for (Template incomingTemplate : bound.getDependencies()) {
      String prevAbstractDomainElement = absDomainVarName(
          policyBackpointerLocationID, incomingTemplate);

      Formula incomingTemplateFormula = addPrefix(
          stateFormulaConversionManager.toFormula(
              pfmgr, fmgr,
              incomingTemplate,
              startPathFormula
          ),
          prefix);

      Formula upperBound = fmgr.makeVariable(
            fmgr.getFormulaType(incomingTemplateFormula),
            prevAbstractDomainElement);
      BooleanFormula constraint = fmgr.makeLessOrEqual(
          incomingTemplateFormula, upperBound, true);
      outConstraints.add(constraint);
    }
  }

  private <T extends Formula> T addPrefix(T formula, String prefix) {
    return fmgr.renameFreeVariablesAndUFs(formula, v -> prefix + v);
  }

  /**
   * @return Variable name representing the bound in the abstract domain
   * for the given template for the given state.
   */
  private String absDomainVarName(int locId, Template template) {
    return String.format(
        BOUND_VAR_NAME, locId, template.toFormulaString());
  }

}
