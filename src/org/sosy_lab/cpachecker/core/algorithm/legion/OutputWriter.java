package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.testcase.XMLTestCaseExport;

public class OutputWriter {

    private int testCaseNumber;
    private LogManager logger;
    private PredicateCPA PredicateCPA;
    private String path;

    public OutputWriter(LogManager pLogger, PredicateCPA pPredicateCPA, String pPath) {
        testCaseNumber = 0;
        logger = pLogger;
        PredicateCPA = pPredicateCPA;
        path = pPath;

        initOutDir(path);
        writeTestMetadata();
    }

    private void initOutDir(String pPath){
        File outpath = new File(pPath);
        outpath.mkdirs();
    }

    /**
     * Write the metadata file necessary for testcomp.
     */
    private void writeTestMetadata() {

        try (FileWriter metadata = new FileWriter("./output/testcases/metadata.xml")) {
            XMLTestCaseExport.writeXMLMetadata(metadata, PredicateCPA.getCfa(), null, "legion");
            metadata.flush();
        } catch (IOException exc) {
            logger.log(Level.WARNING, "Could not write test output", exc);
        }
    }

    public void writeTestCases(ReachedSet reachedSet) {
        AbstractState first = reachedSet.getFirstState();
        ARGState args = AbstractStates.extractStateByType(first, ARGState.class);

        ArrayList<Entry<MemoryLocation, ValueAndType>> values = new ArrayList<>();
        searchTestCase(args, values);
        try {
            writeTestCase(values);
        } catch (IOException exc) {
            logger.log(Level.WARNING, "Could not write test output", exc);
        }

    }

    private void
            searchTestCase(ARGState state, ArrayList<Entry<MemoryLocation, ValueAndType>> values) {
        // check if is nondet assignment
        LocationState ls = AbstractStates.extractStateByType(state, LocationState.class);
        Iterable<CFAEdge> incoming = ls.getIngoingEdges();
        for (CFAEdge edge : incoming) {
            if (edge.getEdgeType() == CFAEdgeType.StatementEdge) {
                CStatement statement = ((CStatementEdge) edge).getStatement();
                if (statement instanceof CFunctionCallAssignmentStatement) {
                    CFunctionCallAssignmentStatement assignment =
                            ((CFunctionCallAssignmentStatement) statement);
                    CFunctionCallExpression right_hand = assignment.getRightHandSide();
                    if (right_hand.toString().startsWith("__VERIFIER_nondet_")) {
                        // CHECK!
                        String function_name = ls.getLocationNode().getFunctionName();
                        String identifier =
                                ((CIdExpression) assignment.getLeftHandSide()).getName();
                        @Nullable
                        ValueAnalysisState vs =
                                AbstractStates.extractStateByType(state, ValueAnalysisState.class);
                        Entry<MemoryLocation, ValueAndType> vt =
                                getValueTypeFromState(function_name, identifier, vs);
                        values.add(vt);
                    }
                }
            }
        }

        // find largest child state
        ARGState largest_child = null;
        for (ARGState child : state.getChildren()) {
            if (largest_child == null || largest_child.getStateId() < child.getStateId()) {
                largest_child = child;
            }
        }

        // If largest_child still null -> at the bottom of the graph
        if (largest_child == null) {
            // writeTestCase
            return;
        }

        // If not, search in largest_child
        searchTestCase(largest_child, values);
    }

    /**
     * Write the individual test files.
     */
    private void writeTestCase(ArrayList<Entry<MemoryLocation, ValueAndType>> values)
            throws IOException {
        // Setup directory
        File outpath = new File("./output/testcases");
        outpath.mkdirs();

        // Setup file
        String filename = String.format("testcase_%s.xml", this.testCaseNumber);
        logger.log(Level.WARNING, "Writing testcase ", filename);
        try (FileWriter testcase = new FileWriter("./output/testcases/" + filename)) {
            testcase.write("<testcase>\n");
            for (Entry<MemoryLocation, ValueAndType> v : values) {
                String name = v.getKey().toString();
                String type = v.getValue().getType().toString();
                Value value = v.getValue().getValue();

                String value_str = "";
                if (type.equals("int")) {
                    value_str = String.valueOf(((NumericValue) value).longValue());
                }

                testcase.write(
                        String.format(
                                "\t<input variable=\"%s\" type=\"%s\">%s</input>\n",
                                name,
                                type,
                                value_str));
            }

            testcase.write("</testcase>\n");
            testcase.flush();
        }
        this.testCaseNumber += 1;
    }

    private Entry<MemoryLocation, ValueAndType> getValueTypeFromState(
            String function_name,
            String identifier,
            ValueAnalysisState state) {
        for (Entry<MemoryLocation, ValueAndType> entry : state.getConstants()) {
            MemoryLocation loc = entry.getKey();
            if (loc.getFunctionName().equals(function_name)
                    && loc.getIdentifier().equals(identifier)) {
                return entry;
            }
        }
        return null;
    }
}
