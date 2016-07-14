package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

@Options(prefix="cpa.predicate.mining")
public class PrecisionMiningPrecisionAdjustment implements PrecisionAdjustment {

  @Option(secure = true, name="variableWhitelist",
      description="whitelist regex for variables that will always be tracked by the CPA using this precision")
  private Pattern variableWhitelist = Pattern.compile("");

  private final PredicateAbstractionManager predMgr;
  private final LogManager logger;
  private final Configuration config;
  private final PathFormulaManager pfMgr;

  public PrecisionMiningPrecisionAdjustment(
      LogManager pLogger, Configuration pConfig,
      PathFormulaManager pPfMgr, PredicateAbstractionManager pPredicateManager) {

    logger = pLogger;
    config = pConfig;
    pfMgr = pPfMgr;
    predMgr = pPredicateManager;
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
        if (g instanceof AssumeEdge) {
          // Example: F_1 == 1;
          AssumeEdge ag = (AssumeEdge) g;
          PredicatePrecision assumePi =
              PredicatePrecisions.assumeEdgeToPrecision(pfMgr, predMgr, ag, true, false);
          logger.log(Level.INFO, "Mined", assumePi);
          PredicatePrecision joined = (PredicatePrecision) piPrime.join(assumePi);

          if (!joined.equals(pPrecision)) {
            piPrime = joined;
          }
        } else if (g instanceof AStatementEdge) {
          // Example: MARKER_X = 1;
        }
      }
    }

    return Optional.of(PrecisionAdjustmentResult.create(
        pComponentState, piPrime, PrecisionAdjustmentResult.Action.CONTINUE));
  }

}
