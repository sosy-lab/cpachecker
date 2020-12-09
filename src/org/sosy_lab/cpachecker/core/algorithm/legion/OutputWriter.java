// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
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
    private int previousSetSize;
    private Instant zero;

    // Stats
    private LegionComponentStatistics stats;
    int successfull_writes = 0;

    /**
     * The output writer can take a pReachedSet on .writeTestCases and traverse it, rendering out a
     * testcase for it.
     * 
     * @param pPath The output path to write files to.
     */
    public OutputWriter(LogManager pLogger, PredicateCPA pPredicateCPA, String pPath) {
        this.logger = pLogger;
        this.predicateCPA = pPredicateCPA;
        this.path = pPath;

        this.testCaseNumber = 0;
        this.previousSetSize = 0;

        this.zero = Instant.now();
        this.stats = new LegionComponentStatistics("output_writer");

        initOutDir(path);
        writeTestMetadata();
    }

    private void initOutDir(String pPath) {
        File outpath = new File(pPath);
        boolean dirs_done = outpath.mkdirs();
        if (!dirs_done) {
            logger.log(Level.WARNING, "Could not make output directory for test cases, maybe alread exists.");
        }
    }

    /**
     * Write the metadata file necessary for testcomp to the output path.
     * 
     * This only needs to be done once and does not contain testcase specific information.
     */
    private void writeTestMetadata() {
        this.stats.start();
        try (Writer metadata =
                Files.newBufferedWriter(
                        Paths.get(this.path + "/metadata.xml"),
                        Charset.defaultCharset())) {
            XMLTestCaseExport.writeXMLMetadata(metadata, predicateCPA.getCfa(), null, "legion");
            metadata.flush();
        } catch (IOException exc) {
            logger.log(Level.SEVERE, "Could not write metadata file", exc);
        } finally {
            this.stats.finish();
            this.successfull_writes += 1;
        }

    }

    /**
     * Handles writing of all testcases necessary for the given reachedSet.
     */
    public void writeTestCases(UnmodifiableReachedSet pReachedSet) {

        this.stats.start();
        int reached_size = pReachedSet.size();
        // Write output only if new states have been reached
        if (previousSetSize == reached_size) {
            return;
        }
        logger.log(
                Level.INFO,
                "Searching through arg(" + Integer.toString(reached_size) + ") for testcases");

        // Get starting point for search
        AbstractState first = pReachedSet.getFirstState();
        ARGState args = AbstractStates.extractStateByType(first, ARGState.class);

        // Search individual testcases
        List<Entry<MemoryLocation, ValueAndType>> values = new ArrayList<>();
        searchTestCase(args, values);

        // Determine file to open
        String violation_str = "";
        if (pReachedSet.hasViolatedProperties()) {
            violation_str = "_error";
        }
        Duration since_zero = Duration.between(this.zero, Instant.now());
        String filename =
                String.format(
                        "/testcase_%016d_%s%s.xml",
                        since_zero.toNanos(),
                        this.testCaseNumber,
                        violation_str);

        // Get content
        String inputs = writeVariablesToTestcase(values);
        if (inputs.length() < 1) {
            this.previousSetSize = reached_size;
            this.stats.finish();
            return;
        }

        logger.log(Level.WARNING, "Writing testcase ", filename);
        try (Writer testcase =
                Files.newBufferedWriter(
                        Paths.get(this.path + filename),
                        Charset.defaultCharset())) {

            // Write header
            testcase.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
            testcase.write(
                    "<!DOCTYPE testcase PUBLIC \"+//IDN sosy-lab.org//DTD test-format testcase 1.0//EN\" \"https://sosy-lab.org/test-format/testcase-1.0.dtd\">\n");
            testcase.write("<testcase>\n");

            // Add input testcases
            testcase.write(inputs);

            // Footer and flush
            testcase.write("</testcase>\n");
            testcase.flush();
            this.successfull_writes += 1;
        } catch (IOException exc) {
            logger.log(Level.SEVERE, "Could not write test output", exc);
        } finally {
            this.testCaseNumber += 1;
            this.previousSetSize = reached_size;
            this.stats.finish();
        }

    }

    /**
     * Extract the variable name from the left hand side.
     * 
     * In case of a regular CIdExpression this is easy, just get the name. But with arrays, this is
     * apparently `different`.
     */
    String getLeftHandName(ALeftHandSide left) {
        if (left instanceof CIdExpression) {
            return ((CIdExpression) left).getName();
        } else if (left instanceof CArraySubscriptExpression) {
            return ((CArraySubscriptExpression) left).getArrayExpression().toString();
        }
        return "";
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
            searchTestCase(ARGState state, List<Entry<MemoryLocation, ValueAndType>> values) {
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
                String identifier = getLeftHandName(assignment.getLeftHandSide());

                ValueAnalysisState vs =
                        AbstractStates.extractStateByType(state, ValueAnalysisState.class);
                Entry<MemoryLocation, ValueAndType> vt =
                        getValueTypeFromState(function_name, identifier, vs);
                if (vt != null) {
                    values.add(vt);
                } else {
                    logger.log(
                            Level.WARNING,
                            "No values to write, propably `MemoryLocation forgotten`");
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
            return;
        }

        // If not, search in largest_child
        try {
            searchTestCase(largest_child, values);
        } catch (StackOverflowError e) {
            return;
        }
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
    private String writeVariablesToTestcase(List<Entry<MemoryLocation, ValueAndType>> values) {

        StringBuilder sb = new StringBuilder();
        for (Entry<MemoryLocation, ValueAndType> v : values) {
            String name = v.getKey().toString();
            String type = v.getValue().getType().toString();
            Value value = v.getValue().getValue();

            String value_str = "";
            if (value instanceof NumericValue) {
                value_str = String.valueOf(((NumericValue) value).longValue());
            } else {
                value_str = value.toString();
            }

            sb.append(
                    String.format(
                            "\t<input variable=\"%s\" type=\"%s\">%s</input>%n",
                            name,
                            type,
                            value_str));
        }
        return sb.toString();
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

            String fn_name = "";
            String ident = "";
            try {
                fn_name = loc.getFunctionName();
            } catch (NullPointerException exc) {
                continue;
            }

            try {
                ident = loc.getIdentifier();
            } catch (NullPointerException exc) {
                continue;
            }

            if (fn_name.equals(function_name) && ident.equals(identifier)) {
                return entry;
            }
        }
        return null;
    }

    public LegionComponentStatistics getStats() {
        this.stats.set_other("successfull_writes", (double)this.successfull_writes);

        return this.stats;
    }
}
