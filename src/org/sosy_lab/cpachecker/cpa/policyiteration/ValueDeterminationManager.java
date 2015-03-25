package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

public class ValueDeterminationManager {

  /** Dependencies */
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final LogManager logger;
  private final TemplateManager templateManager;

  /** Constants */
  private static final String BOUND_VAR_NAME = "BOUND_[%s]_[%s]";
  private static final String VISIT_PREFIX = "[%d]_";

  public ValueDeterminationManager(
      FormulaManagerView fmgr,
      LogManager logger,
      TemplateManager pTemplateManager) {

    this.fmgr = fmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.logger = logger;
    templateManager = pTemplateManager;
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
   * @return Global constraint for value determination and types of the abstract
   * domain elements.
   */
  public Pair<ImmutableMap<String, FormulaType<?>>, Set<BooleanFormula>> valueDeterminationFormula(
      PolicyAbstractedState stateWithUpdates,
      final Map<Template, PolicyBound> updated,
      boolean useUniquePrefix
  ) {
    Set<BooleanFormula> constraints = new HashSet<>();
    Map<String, FormulaType<?>> types = new HashMap<>();

    long uniquePrefix = 0;

    Set<PolicyAbstractedState> visitedStates = new HashSet<>();
    LinkedHashSet<PolicyAbstractedState> queue = new LinkedHashSet<>();
    queue.add(stateWithUpdates);
    Set<String> visited = new HashSet<>();

    while (!queue.isEmpty()) {
      Iterator<PolicyAbstractedState> it = queue.iterator();
      PolicyAbstractedState state = it.next();
      it.remove();

      for (Entry<Template, PolicyBound> incoming : state) {
        Template template = incoming.getKey();
        PolicyBound bound = incoming.getValue();
        PolicyAbstractedState backpointer = bound.getPredecessor();

        // Update the queue, check visited.
        if ((state != stateWithUpdates || updated.containsKey(template))
            && bound.dependsOnInitial()) {

          if (!visitedStates.contains(backpointer)) {
            queue.add(backpointer);
          }
        }


        // Give the element to the constraint generator.
        String prefix;
        if (useUniquePrefix) {
          // This creates A LOT of constraints.
          // Which is bad => consequently, everything times out.
          prefix = String.format(VISIT_PREFIX, ++uniquePrefix);
        } else {
          prefix = String.format(VISIT_PREFIX, bound.serializePolicy(state));
        }

        generateConstraintsFromPolicyBound(
            bound,
            state,

            template,
            backpointer,
            prefix,
            stateWithUpdates,
            constraints, types, visited,
            updated
        );
      }

      visitedStates.add(state);
    }

    return Pair.of(ImmutableMap.copyOf(types), constraints);
  }

  /**
   * Process and add constraints from a single policy.
   *
   * @param bound {@link PolicyBound} to generate constraints from
   * @param template Associated template
   * @param prefix Unique namespace for the policy
   * @param toState Abstracted state from which the bound originates
   * @param incomingState Backpointer for the policy
   */
  private void generateConstraintsFromPolicyBound(
      PolicyBound bound,
      PolicyAbstractedState toState,

      Template template,
      PolicyAbstractedState incomingState,

      final String prefix,
      PolicyAbstractedState stateWithUpdates,
      Set<BooleanFormula> constraints,
      Map<String, FormulaType<?>> types,
      Set<String> visited,
      final Map<Template, PolicyBound> updated
  ) {
    final Function<String, String> addPrefix = new Function<String, String>() {
          @Override
          public String apply(String pInput) {
            return prefix + pInput;
          }
        };
    PathFormula policyFormula = bound.getFormula();
    PathFormula startPathFormula = bound.getStartPathFormula();

    final String abstractDomainElement = absDomainVarName(toState, template);
    final Formula policyOutTemplate = fmgr.renameFreeVariablesAndUFs(
        templateManager.toFormula(template, policyFormula), addPrefix);
    final Formula abstractDomainFormula =
        fmgr.makeVariable(fmgr.getFormulaType(policyOutTemplate),
            abstractDomainElement);
    types.put(abstractDomainElement, fmgr.getFormulaType(policyOutTemplate));

    // Shortcut: don't follow the nodes not in the policy, as the value
    // determination does not update them.
    if (toState == stateWithUpdates && !updated.containsKey(template)) {
      visited.add(prefix);
      return;
    }

    // Shortcut: if the bound is not dependent on the initial value,
    // just add the numeric constraint and don't process the input policies.
    if (!bound.dependsOnInitial()) {
      logger.log(Level.FINE, "Template does not depend on initial condition,"
          + "skipping");
      BooleanFormula constraint = fmgr.makeEqual(abstractDomainFormula,
              fmgr.makeNumber(policyOutTemplate, bound.getBound()));
      constraints.add(constraint);
      return;
    }

    BooleanFormula outConstraint = fmgr.makeEqual(
        policyOutTemplate, abstractDomainFormula);
    constraints.add(outConstraint);

    BooleanFormula namespacedPolicy =
        fmgr.renameFreeVariablesAndUFs(policyFormula.getFormula(), addPrefix);

    // Optimization.
    if (!(namespacedPolicy.equals(bfmgr.makeBoolean(true))
        || visited.contains(prefix))) {
      constraints.add(namespacedPolicy);
    }

    // Process incoming constraints on the policy start.
    for (Entry<Template, PolicyBound> incomingConstraint : incomingState) {

      Template incomingTemplate = incomingConstraint.getKey();

      String prevAbstractDomainElement = absDomainVarName(
          incomingState,
          incomingTemplate);

      Formula incomingTemplateFormula = fmgr.renameFreeVariablesAndUFs(
          templateManager.toFormula(
              incomingTemplate,
              startPathFormula
          ), addPrefix);

      Formula upperBound;
      if (incomingState == stateWithUpdates && !updated.containsKey(
          incomingTemplate)) {

        upperBound = fmgr.makeNumber(incomingTemplateFormula,
            stateWithUpdates.getBound(incomingTemplate).get().getBound());

      } else {
        upperBound = fmgr.makeVariable(
            fmgr.getFormulaType(incomingTemplateFormula),
            prevAbstractDomainElement);
      }
      BooleanFormula constraint = fmgr.makeLessOrEqual(
          incomingTemplateFormula, upperBound, true);
      constraints.add(constraint);
    }


    visited.add(prefix);
  }

  /**
   * @return Variable name representing the bound in the abstract domain
   * for the given template for the given state.
   */
  public String absDomainVarName(PolicyAbstractedState state, Template template) {
    return String.format(BOUND_VAR_NAME, state.getUniqueID(), template);
  }

}
