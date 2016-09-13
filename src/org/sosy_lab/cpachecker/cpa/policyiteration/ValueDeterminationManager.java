package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.templates.Template;
import org.sosy_lab.cpachecker.util.templates.TemplateToFormulaConversionManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

@Options(prefix="cpa.lpi")
public class ValueDeterminationManager {

  @Option(secure=true,
      description="Attach extra invariant from other CPAs during the value "
          + "determination computation")
  private boolean attachExtraInvariantDuringValueDetermination = true;

  /** Dependencies */
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final LogManager logger;
  private final PathFormulaManager pfmgr;
  private final StateFormulaConversionManager stateFormulaConversionManager;
  private final TemplateToFormulaConversionManager templateToFormulaConversionManager;

  /** Constants */
  private static final String BOUND_VAR_NAME = "BOUND_[%s]_[%s]";
  private static final String VISIT_PREFIX = "[%d]_";

  public ValueDeterminationManager(
      Configuration pConfiguration,
      FormulaManagerView fmgr,
      LogManager logger,
      PathFormulaManager pPfmgr,
      StateFormulaConversionManager pStateFormulaConversionManager,
      TemplateToFormulaConversionManager pTemplateToFormulaConversionManager) throws InvalidConfigurationException {
    templateToFormulaConversionManager = pTemplateToFormulaConversionManager;
    pConfiguration.inject(this);

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
  ValueDeterminationConstraints valueDeterminationFormulaCheap(
      PolicyAbstractedState newState,
      PolicyAbstractedState siblingState,
      PolicyAbstractedState stateWithUpdates,
      Set<Template> updated
  ) {
    return valueDeterminationFormula(newState, siblingState, stateWithUpdates, updated, false);
  }

  /**
   * Sound value determination procedure.
   */
  ValueDeterminationConstraints valueDeterminationFormula(
      PolicyAbstractedState newState,
      PolicyAbstractedState siblingState,
      PolicyAbstractedState stateWithUpdates,
      Set<Template> updated
  ) {
    return valueDeterminationFormula(newState, siblingState, stateWithUpdates, updated, true);
  }

  /**
   * Convert a value determination problem into a single formula.
   *
   * @param mergedState Newly created state
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
      PolicyAbstractedState newState,
      PolicyAbstractedState siblingState,
      PolicyAbstractedState mergedState,
      Set<Template> updated,
      boolean useUniquePrefix
  ) {
    Set<BooleanFormula> outConstraints = new HashSet<>();

    Map<Integer, PolicyAbstractedState> stronglyConnectedComponent = findScc(newState, siblingState);

    Table<Template, Integer, Formula> outVars = HashBasedTable.create();

    long uniquePrefix = 0;

    Set<Integer> visitedLocationIDs = new HashSet<>();

    LinkedHashSet<PolicyAbstractedState> queue = new LinkedHashSet<>();
    queue.add(mergedState);

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
            ||  (state == mergedState
                    && !updated.contains(template)
                    && !bound.isComputedByValueDetermination())

            // Backpointer is not in the found strongly connected component.
            || !stronglyConnectedComponent.containsKey(backpointer.getLocationID());

        // Update the queue, check visited.
        if (!valueIsFixed &&
            bound.getDependencies().contains(template)

            // todo note: it is implicitly assumed that by processing backpointers we should get
            // the latest version of the state for each location ID,
            // as we would simply ignore the second one.
            // perhaps it could be more fruitful to make this assumption explicit.
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
   * Add to map everything in the strongly connected between {@code sibling}
   * and {@code newState}.
   */
  private Map<Integer, PolicyAbstractedState> findScc(
      PolicyAbstractedState newState,
      PolicyAbstractedState sibling
  ) {
    Map<Integer, PolicyAbstractedState> map = new HashMap<>();
    PolicyAbstractedState s = newState;
    while (s != sibling) {
      s = s.getGeneratingState().get().getBackpointerState();

      // If we have multiple matches, only take the latest one.
      if (!map.containsKey(s.getLocationID())) {
        map.put(s.getLocationID(), s);
      }
    }
    return map;
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
   *                   and will not change during this run of value
   *                   determination.
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
        bound.getPredecessor(), fmgr, attachExtraInvariantDuringValueDetermination);

    Formula policyOutTemplate = addPrefix(
        templateToFormulaConversionManager.toFormula(pfmgr, fmgr, template, policyFormula),
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
          templateToFormulaConversionManager.toFormula(
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
        BOUND_VAR_NAME, locId, template.toString());
  }

}
