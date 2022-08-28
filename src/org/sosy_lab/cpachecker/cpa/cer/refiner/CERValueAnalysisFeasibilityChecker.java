package org.sosy_lab.cpachecker.cpa.cer.refiner;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

public class CERValueAnalysisFeasibilityChecker extends ValueAnalysisFeasibilityChecker {

    private LogManager logger;

    public CERValueAnalysisFeasibilityChecker(
            final StrongestPostOperator<ValueAnalysisState> pStrongestPostOp,
            final LogManager pLogger,
            final CFA pCfa,
            final Configuration config)
            throws InvalidConfigurationException {
        super(pStrongestPostOp, pLogger, pCfa, config);
        logger = pLogger;
    }

    @Override
    public boolean isFeasible(
            final ARGPath pPath,
            final ValueAnalysisState pStartingPoint,
            final Deque<ValueAnalysisState> pCallstack)
            throws CPAException, InterruptedException {

        try {
            ValueAnalysisState next = pStartingPoint;

            PathIterator iterator = pPath.fullPathIterator();
            while (iterator.hasNext()) {
                final CFAEdge edge = iterator.getOutgoingEdge();
                Optional<ValueAnalysisState> maybeNext =
                        strongestPostOp.step(next, edge, precision, pCallstack, pPath);

                if (!maybeNext.isPresent()) {
                    logger.log(
                            Level.FINE,
                            "found path to be infeasible: ",
                            edge,
                            " did not yield a successor");
                    return false;
                } else {
                    next = maybeNext.orElseThrow();
                }

                iterator.advance();
            }

            return true;
        } catch (CPATransferException e) {
            throw new CPAException(
                    "Computation of successor failed for checking path: " + e.getMessage(),
                    e);
        }
    }

    // TODO cleanup
    @Override
    public List<Pair<ValueAnalysisState, List<CFAEdge>>> evaluate(final ARGPath path)
            throws CPAException, InterruptedException {

        try {
            List<Pair<ValueAnalysisState, List<CFAEdge>>> reevaluatedPath = new ArrayList<>();
            ValueAnalysisState next = new ValueAnalysisState(machineModel);

            PathIterator iterator = path.fullPathIterator();
            while (iterator.hasNext()) {
                Optional<ValueAnalysisState> successor;
                CFAEdge outgoingEdge;
                List<CFAEdge> allOutgoingEdges = new ArrayList<>();
                do {
                    outgoingEdge = iterator.getOutgoingEdge();
                    allOutgoingEdges.add(outgoingEdge);
                    successor = strongestPostOp.getStrongestPost(next, precision, outgoingEdge);
                    iterator.advance();

                    if (!successor.isPresent()) {
                        return reevaluatedPath;
                    }

                    // extract singleton successor state
                    next = successor.orElseThrow();
                } while (!iterator.isPositionWithState());

                reevaluatedPath.add(Pair.of(next, allOutgoingEdges));
            }

            return reevaluatedPath;
        } catch (CPATransferException e) {
            throw new CPAException(
                    "Computation of successor failed for checking path: " + e.getMessage(),
                    e);
        }
    }
}
