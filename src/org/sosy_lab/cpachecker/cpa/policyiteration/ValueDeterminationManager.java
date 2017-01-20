package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
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
      PolicyAbstractedState stateWithUpdates,
      Set<Template> updated
  ) {
    return valueDeterminationFormula(newState, stateWithUpdates, updated, false);
  }

  /**
   * Sound value determination procedure.
   */
  ValueDeterminationConstraints valueDeterminationFormula(
      PolicyAbstractedState newState,
      PolicyAbstractedState stateWithUpdates,
      Set<Template> updated
  ) {
    return valueDeterminationFormula(newState, stateWithUpdates, updated, true);
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
      PolicyAbstractedState mergedState,
      Set<Template> updated,
      boolean useUniquePrefix
  ) {
    Set<BooleanFormula> outConstraints = new HashSet<>();

    Map<Integer, PolicyAbstractedState> stronglyConnectedComponent = findScc2(newState);

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
   * Find an SCC of dependencies.
   */
  private Map<Integer, PolicyAbstractedState> findScc2(
      PolicyAbstractedState newState
  ) {

    int startLocId = newState.getLocationID();

    // Generate the graph.
    // NB: all locations in "stateMap" are forward-reachable by following the dependencies
    // of {@code startLocId}.
    Map<Integer, PolicyAbstractedState> stateMap = new HashMap<>();
    SetMultimap<Integer, Integer> backwDepsEdges = HashMultimap.create();
    populateGraph(newState, stateMap, backwDepsEdges);

    // Now intersect the key set of stateMap with a set of locations
    // backwards reachable from {@code startLocId} to get the SCC.
    Set<Integer> backwardsReachable = backwardsDfs(startLocId, backwDepsEdges);
    return Maps.filterKeys(stateMap, locId -> backwardsReachable.contains(locId));
  }

  /**
   * Perform DFS on a given adjacency list and a starting point.
   * @return set of reachable locations.
   */
  private Set<Integer> backwardsDfs(
    int startLocId,
    Multimap<Integer, Integer> backwEdges
  ) {
    LinkedHashSet<Integer> queue = new LinkedHashSet<>();
    queue.add(startLocId);
    Set<Integer> out = new HashSet<>();
    while (!queue.isEmpty()) {
      Iterator<Integer> it = queue.iterator();
      int locId = it.next();
      it.remove();
      out.add(locId);
      for (int b : backwEdges.get(locId)) {
        if (!out.contains(b)) {
          queue.add(b);
        }
      }
    }
    return out;
  }

  /**
   * Construct a graph representation for dependencies.
   * Store the latest instance per each location ID.
   *
   * @param newState state to start the exploration from.
   * @param stateMap write-into param, mapping from location IDs to corresponding states.
   * @param backwDepsEdges backwards edges specifying backwards dependencies,
   *                  transpose of the information given by backpointers
   *                  contained in the abstraction map.
   *                  Adjacency list graph representation.
   */
  private void populateGraph(
      PolicyAbstractedState newState,
      Map<Integer, PolicyAbstractedState> stateMap,
      SetMultimap<Integer, Integer> backwDepsEdges
  ) {
    LinkedHashSet<PolicyAbstractedState> queue = new LinkedHashSet<>();
    queue.add(newState);
    while (!queue.isEmpty()) {
      Iterator<PolicyAbstractedState> it = queue.iterator();
      PolicyAbstractedState toProcess = it.next();

      // Drop the state from the queue.
      it.remove();

      int locId = toProcess.getLocationID();
      PolicyAbstractedState prev = stateMap.get(locId);
      if (prev == null || prev.getStateId() < toProcess.getStateId()) {
        stateMap.put(locId, toProcess);
        toProcess.getAbstraction().values().stream()
            .filter(b -> !b.getDependencies().isEmpty())
            .forEach(b -> {
              PolicyAbstractedState pred = b.getPredecessor();
              queue.add(pred);
              backwDepsEdges.put(pred.getLocationID(), locId);
            });
      }
    }
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
