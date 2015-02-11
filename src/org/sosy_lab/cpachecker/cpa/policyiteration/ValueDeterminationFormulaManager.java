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
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;


public class ValueDeterminationFormulaManager {

  /** Dependencies */
  private final FormulaManager formulaManager;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final LogManager logger;
  private final TemplateManager templateManager;

  /** Private variables. */
  private final int threshold;

  /** Constants */
  private static final String BOUND_VAR_NAME = "BOUND_[%s]_[%s]";
  private static final String VISIT_PREFIX = "[%d]_";

  public ValueDeterminationFormulaManager(
      FormulaManagerView fmgr,
      LogManager logger,
      CFA cfa,
      FormulaManager rfmgr,
      TemplateManager pTemplateManager) throws InvalidConfigurationException{

    this.fmgr = fmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.logger = logger;
    this.formulaManager = rfmgr;
    templateManager = pTemplateManager;

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
  public Pair<ImmutableMap<String, FormulaType<?>>, BooleanFormula> valueDeterminationFormula(
      Map<Location, PolicyAbstractedState> policy,
      final Location focusedLocation,
      final Map<Template, PolicyBound> updated
  ) throws CPATransferException, InterruptedException{
    List<BooleanFormula> constraints = new ArrayList<>();
    Map<String, FormulaType<?>> types = new HashMap<>();

    for (Entry<Location, PolicyAbstractedState> entry : policy.entrySet()) {
      Location toLocation = entry.getKey();
      PolicyState state = entry.getValue();
      Preconditions.checkState(state.isAbstract());
      Set<String> visited = new HashSet<>();

      for (Entry<Template, PolicyBound> incoming : state.asAbstracted()) {
        Template template = incoming.getKey();
        PolicyBound bound = incoming.getValue();

        String prefix = String.format(VISIT_PREFIX,
            bound.serializePath(toLocation));

        if (toLocation == focusedLocation && !updated.containsKey(template)) {

          // Insert the invariant from the previous constraint.
          Formula templateFormula = templateManager.toFormula(
              template,
              new PathFormula(
                  bfmgr.makeBoolean(true),
                  SSAMap.emptySSAMap(),
                  PointerTargetSet.emptyPointerTargetSet(),
                  0
              ),
              prefix);
          BooleanFormula constraint = fmgr.makeLessOrEqual(
              templateFormula,
              fmgr.makeNumber(templateFormula, bound.bound), true
          );

          constraints.add(constraint);
        } else {
          PathFormula formula = incoming.getValue().formula;

          Location fromLocation = incoming.getValue().predecessor;
          int toLocationNo = toLocation.toID();
          int toLocationPrimeNo = toPrime(toLocationNo);

          PathFormula prefixedPathFormula = pathFormulaWithCustomIdxAndPrefix(
              formula,
              toLocationPrimeNo,
              prefix
          );
          BooleanFormula edgeFormula = prefixedPathFormula.getFormula();

          PolicyAbstractedState incomingState = policy.get(fromLocation);
          for (Entry<Template, PolicyBound> incomingConstraint : incomingState) {
            Template incomingTemplate = incomingConstraint.getKey();
            Formula templateFormula = templateWithInitialMap(
                incomingTemplate, formula,  prefix);
            String prevAbstractDomainElement = absDomainVarName(fromLocation,
                incomingTemplate);
            Formula absDomainElementFormula = fmgr.makeVariable(
                fmgr.getFormulaType(templateFormula), prevAbstractDomainElement
            );

            BooleanFormula constraint = fmgr.makeLessOrEqual(
                templateFormula, absDomainElementFormula, true
            );
            constraints.add(constraint);
          }

          // Optimization.
          if (!(edgeFormula.equals(bfmgr.makeBoolean(true))
                || visited.contains(prefix))) {

            // Check for visited.
            constraints.add(edgeFormula);
          }
          if (policy.get(fromLocation) == null) {
            // NOTE: nodes with no templates aren't in the policy.
            continue;
          }

          Formula outExpr = templateManager.toFormula(
              template, prefixedPathFormula, prefix);
          final String abstractDomainElement = absDomainVarName(toLocation, template);
          BooleanFormula outConstraint;
          types.put(abstractDomainElement, fmgr.getFormulaType(outExpr));

          outConstraint = fmgr.makeEqual(
              outExpr,
              fmgr.makeVariable(fmgr.getFormulaType(outExpr), abstractDomainElement)
          );

          logger.log(Level.FINE, "Output constraint = ", outConstraint);
          constraints.add(outConstraint);
        }
        visited.add(prefix);
      }
    }
    return Pair.of(ImmutableMap.copyOf(types), bfmgr.and(constraints));
  }

  /**
   * Prefix all variables and change the {@code stopIdx}.
   * NOTE: Changing the {@code stopIdx} probably does not do anything,
   * the variable is prefixed anyway.
   */
  private PathFormula pathFormulaWithCustomIdxAndPrefix(
      PathFormula p,
      int stopIdx,
      String customPrefix) throws CPATransferException, InterruptedException {

    SSAMap ssa = p.getSsa();

    SSAMap.SSAMapBuilder newMapBuilder = ssa.builder();

    final BooleanFormula policyConstraint = p.getFormula();

    List<Formula> fromVars = new ArrayList<>();
    List<Formula> toVars = new ArrayList<>();

    Set<Triple<Formula, String, Integer>> allVars = fmgr.extractFreeVariables(policyConstraint);
    for (Triple<Formula, String, Integer> e : allVars) {

      Formula formula = e.getFirst();
      Integer oldIdx = e.getThird();
      if (oldIdx == null) {
        oldIdx = 0;
      }
      String varName = e.getSecond();

      CType type = newMapBuilder.getType(varName);
      if (type == null) {
        // A hack. I'm not using types inside the SSAMap, but SSAMap complaints
        // if it gets null.
        type = CNumericTypes.DOUBLE;
      }

      int newIdx;
      if (oldIdx == ssa.getIndex(varName)) {

        newIdx = stopIdx;
        newMapBuilder = newMapBuilder.setIndex(varName, type, newIdx);
      } else {
        newIdx = oldIdx;
      }

      fromVars.add(formula);
      toVars.add(makeVariable(formula, varName, newIdx, customPrefix));
    }

    BooleanFormula innerFormula = formulaManager.getUnsafeFormulaManager().substitute(
        policyConstraint, fromVars, toVars
    );

    return new PathFormula(
        innerFormula,
        newMapBuilder.build(),
        p.getPointerTargetSet(),
        p.getLength());
  }

  private Formula makeVariable(
        Formula pFormula, String variable, int idx, String namespace) {
    return fmgr.makeVariable(fmgr.getFormulaType(pFormula), namespace + variable, idx);
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
        10000,
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

  String absDomainVarName(Location pLocation, Template template) {
    return String.format(BOUND_VAR_NAME, pLocation.toID(), template);
  }

  private Formula templateWithInitialMap(Template template,
      PathFormula p, String prefix) {
    SSAMap initialMap = deriveInitialSSAMap(p);
    PathFormula initialFormula = new PathFormula(
        p.getFormula(), initialMap, p.getPointerTargetSet(),
        p.getLength()
    );
    return templateManager.toFormula(
        template, initialFormula, prefix
    );
  }

  private SSAMap deriveInitialSSAMap(PathFormula pFormula) {
    // TODO (problem 1): does not work when assignment is a first test.
    // TODO (problem 2): this is a hack. might not work for makeOR.
    // Problem: might not work for makeOR (multiple input paths possible =/)
    Set<Triple<Formula, String, Integer>> allVars =
        fmgr.extractFreeVariables(pFormula.getFormula());
    Map<String, Integer> initialSSA = new HashMap<>();
    for (Triple<Formula, String, Integer> e : allVars) {
      String varName = e.getSecond();
      Integer newValue = e.getThird();
      if (newValue != null) {
        Integer oldValue = initialSSA.get(varName);
        if (oldValue != null) {
          initialSSA.put(varName, Math.min(newValue, oldValue));
        } else {
          initialSSA.put(varName, newValue);
        }
      }
    }

    SSAMap.SSAMapBuilder b = SSAMap.emptySSAMap().builder();
    for (Entry<String, Integer> e : initialSSA.entrySet()) {
      String varName = e.getKey();
      int newIdx = e.getValue();
      CType type = pFormula.getSsa().getType(varName);
      if (type == null) {
        type = CNumericTypes.DOUBLE;
      }
      b = b.setIndex(varName, type , newIdx);
    }

    return b.build();
  }
}
