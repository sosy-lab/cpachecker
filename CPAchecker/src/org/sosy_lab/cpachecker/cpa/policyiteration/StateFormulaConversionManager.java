package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.cpa.policyiteration.congruence.CongruenceManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

/**
 * Class responsible for converting states to formulas.
 */
public class StateFormulaConversionManager {
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final PathFormulaManager pfmgr;
  private final CongruenceManager congruenceManager;
  private final TemplateManager templateManager;
  private final InvariantGenerator invariantGenerator;
  private @Nullable InvariantSupplier invariants = null;

  public StateFormulaConversionManager(FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr, CongruenceManager pCongruenceManager,
      TemplateManager pTemplateManager, InvariantGenerator pInvariantGenerator) {
    fmgr = pFmgr;
    pfmgr = pPfmgr;
    congruenceManager = pCongruenceManager;
    templateManager = pTemplateManager;
    invariantGenerator = pInvariantGenerator;
    bfmgr = pFmgr.getBooleanFormulaManager();
  }

  /**
   * Returns _instantiated_ set of constraints.
   *
   * @param attachExtraInvariant Attach the invariant supplied by other analysis.
   */
  List<BooleanFormula> abstractStateToConstraints(
      FormulaManagerView fmgrv,
      PathFormulaManager pfmgr,
      PolicyAbstractedState abstractState,
      boolean attachExtraInvariant) throws CPAException {

    // Returns the abstract state together with the conjoined extra invariant.
    PathFormula inputPath = getPathFormula(abstractState, fmgrv,
        attachExtraInvariant);
    if (attachExtraInvariant) {
      // todo: this is really hacky, can we think of a more elegant solution?
      inputPath = inputPath.updateFormula(
          bfmgr.and(
              inputPath.getFormula(),
              fmgr.instantiate(
                  getInvariantFor(abstractState.getNode()),
                  inputPath.getSsa()
              )
          ));
    }

    List<BooleanFormula> constraints = new ArrayList<>();
    constraints.add(congruenceManager.toFormula(
        pfmgr, fmgrv,
        abstractState.getCongruence(), inputPath
    ));
    for (Entry<Template, PolicyBound> entry : abstractState) {
      Template template = entry.getKey();
      PolicyBound bound = entry.getValue();

      Formula t = templateManager.toFormula(pfmgr, fmgrv, template, inputPath);

      BooleanFormula constraint = fmgrv.makeLessOrEqual(
          t, fmgrv.makeNumber(t, bound.getBound()), true);
      constraints.add(constraint);
    }
    return constraints;
  }

  BooleanFormula getStartConstraints(
      PolicyIntermediateState state,
      boolean attachExtraInvariant) throws CPAException {
    return bfmgr.and(abstractStateToConstraints(fmgr, pfmgr,
        state.getGeneratingState().getLatestVersion(), attachExtraInvariant));
  }

  /**
   * @return Representation of an {@code abstractState} as a
   * {@link PolicyIntermediateState}.
   */
  PolicyIntermediateState abstractStateToIntermediate(
      PolicyAbstractedState abstractState,
      boolean attachExtraInvariant)
      throws InterruptedException, CPATransferException {
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
      extraPredicate = fmgr.getBooleanFormulaManager().makeBoolean(true);
    }
    return new PathFormula(extraPredicate, abstractState.getSSA(),
        abstractState.getPointerTargetSet(), 1);
  }

  BooleanFormula getInvariantFor(CFANode node) throws CPAException {
    if (invariants == null) {
      try {
        invariants = invariantGenerator.get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    return invariants.getInvariantFor(node, fmgr, pfmgr);
  }
}
