package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision.ConfigurablePrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.simpleformulas.Predicate;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

@Options(prefix="cpa.predicate.mining")
public class PrecisionMiningPrecisionAdjustment implements PrecisionAdjustment {

  private final VariableTrackingPrecision miningPrecision;
  private final PredicateAbstractionManager predMgr;
  private final LogManager logger;
  private final Configuration config;
  private final PathFormulaManager pfMgr;

  public PrecisionMiningPrecisionAdjustment(
      LogManager pLogger, Configuration pConfig,
      PathFormulaManager pPfMgr, PredicateAbstractionManager pPredicateManager, CFA pCfa)
      throws InvalidConfigurationException {

    logger = pLogger;
    config = pConfig;
    pfMgr = pPfMgr;
    predMgr = pPredicateManager;

    miningPrecision = VariableTrackingPrecision.createStaticPrecision(
        Configuration.copyWithNewPrefix(pConfig, "cpa.predicate.mining"), pCfa
            .getVarClassification(), PredicateCPA.class);
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pComponentState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState)
    throws CPAException, InterruptedException {

    PredicatePrecision piPrime = (PredicatePrecision) pPrecision;

    ARGState argState = AbstractStates.extractStateByType(pFullState, ARGState.class);

    if (argState.getParents().size() == 1) {
      ARGState parentState = Iterables.getOnlyElement(argState.getParents());
      List<CFAEdge> edges = parentState.getEdgesToChild(argState);
      for (CFAEdge g: edges) {
        final PredicatePrecision piDelta;
        if (g instanceof AssumeEdge) {
          // Example: F_1 == 1;
          AssumeEdge ag = (AssumeEdge) g;
          piDelta = PredicatePrecisions.edgeToPrecision(pfMgr, predMgr, ag, miningPrecision, true, false);
        } else if (g instanceof AStatementEdge) {
          // Example: MARKER_X = 1;
          AStatementEdge sg = (AStatementEdge) g;
          piDelta = PredicatePrecisions.edgeToPrecision(pfMgr, predMgr, sg, miningPrecision, true, false);
        } else {
          continue;
        }

        if (!piDelta.isEmpty()) {
          logger.log(Level.FINE, "Mined", piDelta);
          PredicatePrecision joined = (PredicatePrecision) piPrime.join(piDelta);
          if (!joined.equals(pPrecision)) {
            piPrime = joined;
          }
        }
      }
    }

    return Optional.of(PrecisionAdjustmentResult.create(
        pComponentState, piPrime, PrecisionAdjustmentResult.Action.CONTINUE));
  }

}
