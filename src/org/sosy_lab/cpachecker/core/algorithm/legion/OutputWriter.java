package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
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

    private LogManager logger;
    private PredicateCPA predicateCPA;
    private String path;

    private int testCaseNumber;

    /**
     * The output writer can take a pReachedSet on .writeTestCases and traverse it, rendering out a
     * testcase for it.
     * 
     * @param pPath The output path to write files to.
     */
    public OutputWriter(LogManager pLogger, PredicateCPA pPredicateCPA, String pPath) {
        testCaseNumber = 0;
        logger = pLogger;
        predicateCPA = pPredicateCPA;
        path = pPath;

        initOutDir(path);
        writeTestMetadata();
    }

    private void initOutDir(String pPath) {
        File outpath = new File(pPath);
        outpath.mkdirs();
    }

    /**
     * Write the metadata file necessary for testcomp to the output path.
     * 
     * This only needs to be done once and does not contain testcase specific information.
     */
    private void writeTestMetadata() {
        try (Writer metadata =
                Files.newBufferedWriter(
                        Paths.get(this.path + "/metadata.xml"),
                        Charset.defaultCharset())) {
            XMLTestCaseExport.writeXMLMetadata(metadata, predicateCPA.getCfa(), null, "legion");
            metadata.flush();
        } catch (IOException exc) {
            logger.log(Level.SEVERE, "Could not write metadata file", exc);
        }
    }

    /**
     * Handles writing of all testcases necessary for the given reachedSet.
     */
    public void writeTestCases(ReachedSet pReachedSet) {
        // Get starting point for search
        AbstractState first = pReachedSet.getFirstState();
        ARGState args = AbstractStates.extractStateByType(first, ARGState.class);

        // Search individual testcases
        ArrayList<Entry<MemoryLocation, ValueAndType>> values = new ArrayList<>();
        searchTestCase(args, values);

        // Determine file to open
        String filename = String.format("/testcase_%s.xml", this.testCaseNumber);
        logger.log(Level.WARNING, "Writing testcase ", filename);

        try (Writer testcase =
                Files.newBufferedWriter(
                        Paths.get(this.path + filename),
                        Charset.defaultCharset())) {
            // Write header
            testcase.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
            testcase.write(
                    "<!DOCTYPE testcase PUBLIC \"+//IDN sosy-lab.org//DTD test-format testcase 1.0//EN\" \"https://sosy-lab.org/test-format/testcase-1.0.dtd\">");
            testcase.write("<testcase>\n");

            // Write testcases
            writeVariablesToTestcase(values, testcase);

            // Footer and flush
            testcase.write("</testcase>\n");
            testcase.flush();
        } catch (IOException exc) {
            logger.log(Level.SEVERE, "Could not write test output", exc);
        }

    }

    /**
     * Search through connected states starting from the state given and return their MemoryLocation
     * and ValueType.
     * 
     * This performs walk along the children of state. The child to walk down to is selected by the
     * highest state id (meaning the newest). This results in a list of values starting at the given
     * state and walking the newest path through it's children.
     * 
     * @param state  The starting state.
     * @param values The list of values to append to.
     */
    private void
            searchTestCase(ARGState state, ArrayList<Entry<MemoryLocation, ValueAndType>> values) {
        // check if is nondet assignment
        LocationState ls = AbstractStates.extractStateByType(state, LocationState.class);

        for (CFAEdge edge : ls.getIngoingEdges()) {
            // Extract assignment
            CFunctionCallAssignmentStatement assignment = extractAssignment(edge);
            if (assignment == null) {
                continue;
            }

            // Check if assignment is for a nondeterministic variable
            if (assignment.getRightHandSide().toString().startsWith("__VERIFIER_nondet_")) {
                String function_name = ls.getLocationNode().getFunctionName();
                String identifier = ((CIdExpression) assignment.getLeftHandSide()).getName();
                @Nullable
                ValueAnalysisState vs =
                        AbstractStates.extractStateByType(state, ValueAnalysisState.class);
                Entry<MemoryLocation, ValueAndType> vt =
                        getValueTypeFromState(function_name, identifier, vs);
                values.add(vt);
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
            return;
        }

        // If not, search in largest_child
        searchTestCase(largest_child, values);
    }

    /**
     * Retrieve an assignment statement from a CFAEdge, if this is possible.
     */
    private CFunctionCallAssignmentStatement extractAssignment(CFAEdge edge) {
        if (edge.getEdgeType() == CFAEdgeType.StatementEdge) {
            CStatement statement = ((CStatementEdge) edge).getStatement();
            if (statement instanceof CFunctionCallAssignmentStatement) {
                return ((CFunctionCallAssignmentStatement) statement);
            }
        }
        return null;
    }

    /**
     * Write variables from values to a testcase file.
     */
    private void writeVariablesToTestcase(
            ArrayList<Entry<MemoryLocation, ValueAndType>> values,
            Writer testcase)
            throws IOException {

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
        this.testCaseNumber += 1;
    }

    /**
     * Retrieve variables a MemoryLocation and ValueAndType from a ValueAnalysisState by its
     * function name and identifier (=name).
     * 
     * @param function_name Name of the function the variable is contained in.
     * @param identifier    The name of the function.
     * @return The constants entry for this value or null.
     */
    private static Entry<MemoryLocation, ValueAndType> getValueTypeFromState(
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
