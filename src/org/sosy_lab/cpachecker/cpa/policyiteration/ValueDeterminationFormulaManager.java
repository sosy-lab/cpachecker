package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

public class ValueDeterminationFormulaManager {

  /** Dependencies */
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final LogManager logger;
  private final TemplateManager templateManager;

  /** Constants */
  private static final String BOUND_VAR_NAME = "BOUND_[%s]_[%s]";
  private static final String VISIT_PREFIX = "[%d]_";

  public ValueDeterminationFormulaManager(
      FormulaManagerView fmgr,
      LogManager logger,
      TemplateManager pTemplateManager) throws InvalidConfigurationException{

    this.fmgr = fmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.logger = logger;
    templateManager = pTemplateManager;
  }

  /**
   * Convert a value determination problem into a single formula.
   *
   * @param policy Selected policy.
   * The abstract state associated with the <code>focusedNode</code>
   * is the <b>new</b> state, with <code>updated</code> applied.
   *
   * @return Global constraint for value determination and types of the abstract
   * domain elements.
   *
   * @throws org.sosy_lab.cpachecker.exceptions.CPATransferException
   * @throws InterruptedException
   */
  public Pair<ImmutableMap<String, FormulaType<?>>, List<BooleanFormula>> valueDeterminationFormula(
      Map<Location, PolicyAbstractedState> policy,
      final Location focusedLocation,
      final Map<Template, PolicyBound> updated
  ) throws CPATransferException, InterruptedException{
    List<BooleanFormula> constraints = new ArrayList<>();
    Map<String, FormulaType<?>> types = new HashMap<>();

    for (Entry<Location, PolicyAbstractedState> stateLocation : policy.entrySet()) {
      Location toLocation = stateLocation.getKey();
      PolicyAbstractedState state = stateLocation.getValue();
      Set<String> visited = new HashSet<>();

      for (Entry<Template, PolicyBound> incoming : state) {

        Template template = incoming.getKey();
        PolicyBound bound = incoming.getValue();
        PathFormula startPathFormula = bound.startPathFormula;
        PathFormula policyFormula = bound.formula;
        Location fromLocation = bound.predecessor;
        Rational currentBound = bound.bound;
        String prefix = String.format(VISIT_PREFIX,
            bound.serializePolicy(toLocation));

        constraintsFromPolicy(
            template,
            policyFormula,
            fromLocation,
            startPathFormula,
            currentBound,
            prefix,
            toLocation,
            constraints, types, visited, focusedLocation, updated, policy);
      }
    }

    return Pair.of(ImmutableMap.copyOf(types), constraints);
  }

  /**
   * Process and add constraints from a single policy.
   *
   * @param template Associated template
   * @param policyFormula Associated formula
   * @param startPathFormula Starting {@link PathFormula} for the policy
   * @param bound Current bound for the policy
   * @param prefix Unique namespace for the policy
   * @param toLocation Location associated with the abstracted state
   * @param fromLocation Backpointer for the policy
   */
  private void constraintsFromPolicy(
      Template template,
      PathFormula policyFormula,
      Location fromLocation,
      PathFormula startPathFormula,
      Rational bound,
      String prefix,
      Location toLocation,
      List<BooleanFormula> constraints,
      Map<String, FormulaType<?>> types,
      Set<String> visited,
      final Location focusedLocation,
      final Map<Template, PolicyBound> updated,
      final Map<Location, PolicyAbstractedState> policy
  ) {
    String abstractDomainElement = absDomainVarName(toLocation, template);
    Formula policyOutTemplate = fmgr.addPrefixToAll(
        templateManager.toFormula(template, policyFormula), prefix);
    types.put(abstractDomainElement, fmgr.getFormulaType(policyOutTemplate));

    // Shortcut: don't follow the nodes not in the policy, as the value
    // determination does not update them.
    if (toLocation == focusedLocation && !updated.containsKey(template)) {

      // NOTE: can be replaced by less-or-equal.
      BooleanFormula outConstraint = fmgr.makeLessOrEqual(
          fmgr.makeVariable(fmgr.getFormulaType(policyOutTemplate),
              abstractDomainElement),
          fmgr.makeNumber(policyOutTemplate, bound),
          true
      );

      constraints.add(outConstraint);

    } else {
      BooleanFormula namespacedPolicy = (BooleanFormula)
          fmgr.addPrefixToAll(policyFormula.getFormula(), prefix);

      // Optimization.
      if (!(namespacedPolicy.equals(bfmgr.makeBoolean(true))
          || visited.contains(prefix))) {
        constraints.add(namespacedPolicy);
      }

      PolicyAbstractedState incomingState = policy.get(fromLocation);

      // Process incoming constraints on the policy start.
      for (Entry<Template, PolicyBound> incomingConstraint : incomingState) {
        Template incomingTemplate = incomingConstraint.getKey();
        String prevAbstractDomainElement = absDomainVarName(fromLocation,
            incomingTemplate);

        // NOTE: different code for focused location?
        Formula incomingTemplateFormula = fmgr.addPrefixToAll(
            templateManager.toFormula(
                incomingTemplate,
                startPathFormula
            ),
            prefix);

        Formula absDomainElementFormula = fmgr.makeVariable(
            fmgr.getFormulaType(incomingTemplateFormula),
            prevAbstractDomainElement);

        BooleanFormula constraint = fmgr.makeLessOrEqual(
            incomingTemplateFormula, absDomainElementFormula, true);

        constraints.add(constraint);
      }

      // NOTE: experiment with changing to less-or-equal.
      BooleanFormula outConstraint = fmgr.makeGreaterOrEqual(
          policyOutTemplate,
          fmgr.makeVariable(fmgr.getFormulaType(policyOutTemplate),
              abstractDomainElement), true
      );

      logger.log(Level.FINE, "Output constraint = ", outConstraint);
      constraints.add(outConstraint);
    }
    visited.add(prefix);
  }

  String absDomainVarName(Location pLocation, Template template) {
    return String.format(
        BOUND_VAR_NAME,
        pLocation.toID(),
        template.linearExpression.toString(
            new Function<CIdExpression, String>() {
              public String apply(CIdExpression pCIdExpression) {
                return pCIdExpression.getDeclaration().getQualifiedName();
              }
            }));
  }
}
