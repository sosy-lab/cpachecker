package org.sosy_lab.cpachecker.cpa.policyiteration.congruence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.policyiteration.PolicyIterationStatistics;
import org.sosy_lab.cpachecker.cpa.policyiteration.Template;
import org.sosy_lab.cpachecker.cpa.policyiteration.Template.Kind;
import org.sosy_lab.cpachecker.cpa.policyiteration.TemplateManager;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

@Options(prefix="cpa.stator.congruence")
public class CongruenceManager {

  @Option(secure=true,
  description="Generate congruences for sums of variables "
      + "(<=> x and y have same/different evenness)")
  private boolean trackCongruenceSum = false;

  private final Solver solver;
  private final TemplateManager templateManager;
  private final BitvectorFormulaManager bvfmgr;
  private final FormulaManagerView fmgr;
  private final PolicyIterationStatistics statistics;
  private final PathFormulaManager pfmgr;

  public CongruenceManager(Configuration config,
      Solver pSolver, TemplateManager pTemplateManager,
      FormulaManagerView pFmgr, PolicyIterationStatistics pStatistics,
      PathFormulaManager pPfmgr)
      throws InvalidConfigurationException {
    config.inject(this);
    solver = pSolver;
    templateManager = pTemplateManager;
    fmgr = pFmgr;
    statistics = pStatistics;
    bvfmgr = fmgr.getBitvectorFormulaManager();
    pfmgr = pPfmgr;
  }

  public CongruenceState join(
      CongruenceState a,
      CongruenceState b
  ) {
    Map<Template, Congruence> abstraction = new HashMap<>();
    for (Template t : Sets.intersection(
        a.getAbstraction().keySet(), b.getAbstraction().keySet()
    )) {
      if (a.getAbstraction().get(t) == b.getAbstraction().get(t)) {
        abstraction.put(t, a.getAbstraction().get(t));
      }
    }
    return new CongruenceState(abstraction);
  }

  public boolean isLessOrEqual(CongruenceState a, CongruenceState b) {
    for (Entry<Template, Congruence> e : b) {
      Template template = e.getKey();
      Congruence congruence = e.getValue();
      Optional<Congruence> smallerCongruence = a.get(template);
      if (!smallerCongruence.isPresent()
          || smallerCongruence.get() != congruence) {
        return false;
      }
    }
    return true;
  }

  public CongruenceState performAbstraction(
      CFANode node,
      PathFormula p,
      BooleanFormula startConstraints
  ) throws CPATransferException, InterruptedException {

    Map<Template, Congruence> abstraction = new HashMap<>();

    statistics.startCongruenceTimer();
    try (ProverEnvironment env = solver.newProverEnvironment()) {
      //noinspection ResultOfMethodCallIgnored
      env.push(p.getFormula());
      //noinspection ResultOfMethodCallIgnored
      env.push(startConstraints);

      for (Template template : templateManager.templatesForNode(node)) {
        if (!shouldUseTemplate(template)) {
          continue;
        }

        Formula formula = templateManager.toFormula(pfmgr, fmgr, template, p);

        // Test odd <=> isEven is UNSAT.
        try {
          //noinspection ResultOfMethodCallIgnored
          env.push(fmgr.makeModularCongruence(formula, makeBv(bvfmgr, formula, 0), 2));
          if (env.isUnsat()) {
            abstraction.put(template, Congruence.ODD);
            continue;
          }
        } finally {
          env.pop();
        }

        // Test even <=> isOdd is UNSAT.
        try {
          //noinspection ResultOfMethodCallIgnored
          env.push(
              fmgr.makeModularCongruence(formula, makeBv(bvfmgr, formula, 1), 2));
          if (env.isUnsat()) {
            abstraction.put(template, Congruence.EVEN);
            continue;
          }
        } finally {
          env.pop();
        }
      }
    } catch (SolverException ex) {
      throw new CPATransferException("Solver exception: ", ex);
    } finally {
      statistics.stopCongruenceTimer();
    }

    return new CongruenceState(abstraction);
  }

  public BooleanFormula toFormula(
      PathFormulaManager pfmgr,
      FormulaManagerView fmgr,
      CongruenceState state,
      PathFormula ref) {
    Map<Template, Congruence> abstraction = state.getAbstraction();

    List<BooleanFormula> constraints = new ArrayList<>(abstraction.size());

    for (Entry<Template, Congruence> entry : abstraction.entrySet()) {
      Template template = entry.getKey();
      Congruence congr = entry.getValue();

      Formula formula = templateManager.toFormula(pfmgr, fmgr, template, ref);
      Formula remainder;
      switch (congr) {
        case ODD:
          remainder = makeBv(fmgr.getBitvectorFormulaManager(), formula, 1);
          break;
        case EVEN:
          remainder = makeBv(fmgr.getBitvectorFormulaManager(), formula, 0);
          break;
        default:
          remainder = null;
          assert false : "Unexpected case";
      }

      constraints.add(fmgr.makeModularCongruence(formula, remainder, 2));
    }
    return fmgr.getBooleanFormulaManager().and(constraints);
  }

  private boolean shouldUseTemplate(Template template) {
    return template.isIntegral() && (
        (template.getKind() == Kind.UPPER_BOUND)
        || (trackCongruenceSum && template.getKind() == Kind.SUM)
    );
  }

  private Formula makeBv(BitvectorFormulaManager bvfmgr, Formula other, int value) {
    return bvfmgr.makeBitvector(
        bvfmgr.getLength((BitvectorFormula) other),
        value);
  }
}
