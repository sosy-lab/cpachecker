package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.templates.Template;
import org.sosy_lab.cpachecker.util.templates.TemplateToFormulaConversionManager;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class responsible for converting states to formulas.
 */
@Options(prefix="cpa.lpi")
public class StateFormulaConversionManager {

  @Option(description="Remove redundant items when abstract values.")
  private boolean simplifyDotOutput = false;

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final TemplateToFormulaConversionManager
      templateToFormulaConversionManager;
  private final Configuration configuration;
  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final PolicyDotWriter dotWriter;
  private final PathFormulaManager pfmgr;
  private final Solver solver;

  public StateFormulaConversionManager(
      FormulaManagerView pFmgr,
      TemplateToFormulaConversionManager pTemplateToFormulaConversionManager,
      Configuration pConfiguration,
      CFA pCfa,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      PathFormulaManager pPfmgr,
      Solver pSolver) throws InvalidConfigurationException {
    pConfiguration.inject(this);
    fmgr = pFmgr;
    bfmgr = pFmgr.getBooleanFormulaManager();
    templateToFormulaConversionManager = pTemplateToFormulaConversionManager;
    configuration = pConfiguration;
    cfa = pCfa;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    pfmgr = pPfmgr;
    solver = pSolver;
    dotWriter = new PolicyDotWriter();
  }

  /**
   * Returns _instantiated_ set of constraints.
   *
   * @param attachExtraInvariant Attach the invariant supplied by other analysis.
   */
  List<BooleanFormula> abstractStateToConstraints(
      FormulaManagerView fmgrv,
      PolicyAbstractedState abstractState,
      boolean attachExtraInvariant) {

    // Returns the abstract state together with the conjoined extra invariant.
    List<BooleanFormula> constraints = new ArrayList<>();

    PathFormulaManager pfmgrv;
    try {
      pfmgrv = new PathFormulaManagerImpl(
          fmgrv, configuration, logger,
          shutdownNotifier,
          cfa,
          AnalysisDirection.FORWARD
      );
    } catch (InvalidConfigurationException pE) {
      throw new UnsupportedOperationException("Could not construct path "
          + "formula manager", pE);
    }

    PathFormula inputPath = getPathFormula(abstractState, fmgrv, attachExtraInvariant);
    if (!fmgrv.getBooleanFormulaManager().isTrue(inputPath.getFormula())) {
      constraints.add(inputPath.getFormula());
    }

    if (attachExtraInvariant) {

      // Extra invariant.
      constraints.add(fmgrv.instantiate(
          abstractState.getExtraInvariant(), inputPath.getSsa()));
    }

    for (Entry<Template, PolicyBound> entry : abstractState) {
      Template template = entry.getKey();
      PolicyBound bound = entry.getValue();

      constraints.add(
          templateToConstraint(template, bound, pfmgrv, fmgrv, inputPath));
    }
    return constraints;
  }

  BooleanFormula templateToConstraint(
      Template template,
      PolicyBound bound,
      PathFormulaManager pfmgrv,
      FormulaManagerView fmgrv,
      PathFormula inputPath
      ) {
    Formula t = templateToFormulaConversionManager.toFormula(
        pfmgrv, fmgrv, template, inputPath);
    return fmgrv.makeLessOrEqual(
        t, fmgrv.makeNumber(t, bound.getBound()), true);

  }

  public BooleanFormula getStartConstraintsWithExtraInvariant(
      PolicyIntermediateState state) {
    return bfmgr.and(abstractStateToConstraints(
        fmgr, state.getBackpointerState(), true));
  }

  /**
   * @return Representation of an {@code abstractState} as a
   * {@link PolicyIntermediateState}.
   */
  PolicyIntermediateState abstractStateToIntermediate(
      PolicyAbstractedState abstractState,
      boolean attachExtraInvariant) {
    CFANode node = abstractState.getNode();
    PathFormula generatingFormula = getPathFormula(abstractState,
        fmgr, attachExtraInvariant
    );

    return PolicyIntermediateState.of(node, generatingFormula, abstractState);
  }

  /**
   * @param attachExtraInvariant Whether the extra invariant should be attached.
   * @return Starting {@code PathFormula} associated with {@code abstractState}.
   * Does not include the constraints.
   */
  PathFormula getPathFormula(
      PolicyAbstractedState abstractState,
      FormulaManagerView fmgr,
      boolean attachExtraInvariant
  ) {
    BooleanFormula extraPredicate;
    if (attachExtraInvariant) {
      extraPredicate = fmgr.instantiate(abstractState.getExtraInvariant(),
          abstractState.getSSA());
    } else {
      extraPredicate = fmgr.getBooleanFormulaManager().makeTrue();
    }
    return new PathFormula(extraPredicate, abstractState.getSSA(),
        abstractState.getPointerTargetSet(), 1);
  }

  public String toDOTLabel(Map<Template, PolicyBound> pAbstraction) {
    if (!simplifyDotOutput) {
      return dotWriter.toDOTLabel(pAbstraction);
    }

    PathFormula inputPath = new PathFormula(
        bfmgr.makeTrue(), SSAMap.emptySSAMap(), PointerTargetSet
        .emptyPointerTargetSet(), 0
    );

    Map<Template, BooleanFormula> templatesToConstraints = pAbstraction
        .entrySet()
        .stream()
        .collect(Collectors.toMap(
            entry -> entry.getKey(),
            entry -> templateToConstraint(
                entry.getKey(),
                entry.getValue(),
                pfmgr,
                fmgr,
                inputPath
            )
        ));
    List<Template> templates = new ArrayList<>(pAbstraction.keySet());
    Set<Template> nonRedundant = new HashSet<>(templates);
    for (Template t : templates) {
      // mark redundant templates as such
      BooleanFormula constraint = templatesToConstraints.get(t);

      Set<Template> others = Sets.filter(nonRedundant, t2 -> t2 != t);

      // if others imply the constraint, remove it.
      BooleanFormula othersConstraint =
          bfmgr.and(Collections2.transform(others, tb -> templatesToConstraints.get(tb)));

      try {
        if (solver.implies(othersConstraint, constraint)) {
          nonRedundant.remove(t);
        }
      } catch (SolverException|InterruptedException pE) {
        logger.logException(Level.WARNING, pE, "Failed simplifying the "
            + "abstraction before rendering, converting as it is.");
        simplifyDotOutput = false;
        return dotWriter.toDOTLabel(pAbstraction);
      }
    }

    Map<Template, PolicyBound> filteredAbstraction = Maps.filterKeys(
        pAbstraction, t -> nonRedundant.contains(t));
    return dotWriter.toDOTLabel(filteredAbstraction);
  }
}
