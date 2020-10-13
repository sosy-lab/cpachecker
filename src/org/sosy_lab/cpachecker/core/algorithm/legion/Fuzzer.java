package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;


public class Fuzzer {


    private final LogManager logger;
    private final ValueAnalysisCPA valueCpa;
    private OutputWriter outputWriter;


    public Fuzzer(final LogManager pLogger, ValueAnalysisCPA pValueCPA, OutputWriter pOutputWriter) {
        this.logger = pLogger;
        this.valueCpa = pValueCPA;
        this.outputWriter = pOutputWriter;
    }

    /**
     * Run the fuzzing phase using pAlgorithm pPasses times on the states in pReachedSet.
     * 
     * To be discussed: Design decision --------------------------------
     * 
     */
    public ReachedSet fuzz(
            ReachedSet pReachedSet,
            int pPasses,
            Algorithm pAlgorithm,
            ArrayList<ArrayList<ValueAssignment>> pPreLoadedValues
            )
            throws CPAEnabledAnalysisPropertyViolationException, CPAException, InterruptedException,
            PropertyViolationException {

        for (int i = 0; i < pPasses; i++) {
            logger.log(Level.INFO, "Fuzzing pass", i + 1);

            int previousSetSize = pReachedSet.size();

            // Preload values if they exist
            int size = pPreLoadedValues.size();
            if (size > 0) {
                int j = i % size;
                logger.log(Level.FINE, "pPreLoadedValues at", j, "/", size);
                // valCpa.getTransferRelation().setKnownValues(pPreLoadedValues.get(j));
                preloadValues(pPreLoadedValues.get(j));
            }
            // Run algorithm and collect result
            pAlgorithm.run(pReachedSet);

            if (previousSetSize < pReachedSet.size()){
                outputWriter.writeTestCases(pReachedSet);
            }
            
            // If an error was found, stop execution
            Collection<Property> violatedProperties = pReachedSet.getViolatedProperties();
            if (!violatedProperties.isEmpty()) {
                throw new PropertyViolationException(violatedProperties);
            }

            // Otherwise, start from the beginning again
            pReachedSet.reAddToWaitlist(pReachedSet.getFirstState());

        }
        return pReachedSet;
    }

    private ArrayList<Value> preloadValues(
        ArrayList<ValueAssignment> assignments
    ) {
        ArrayList<Value> values = new ArrayList<>();
        for (ValueAssignment a : assignments) {
            values.add(utils.toValue(a.getValue()));
        }

        valueCpa.getTransferRelation().setKnownValues(values);

        return values;
    }
}
