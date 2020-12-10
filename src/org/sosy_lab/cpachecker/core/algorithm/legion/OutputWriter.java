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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
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
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.testcase.XMLTestCaseExport;

@Options(prefix = "legion")
public class OutputWriter {

  private final LogManager logger;
  private final PredicateCPA predicateCPA;

  private static final String VERIFIER_NONDET = "__VERIFIER_nondet_";

  private int testCaseNumber = 0;
  private int previousSetSize = 0;
  private Instant zero = Instant.now();

  @Option(
      secure = false,
      name = "testcaseOutputDirectory",
      description = "The subdirectory testcases will be written to.")
  @FileOption(FileOption.Type.OUTPUT_DIRECTORY)
  @Nullable
  private Path testcaseOutputDir = Paths.get("testcases");

  // Stats
  private final LegionComponentStatistics stats = new LegionComponentStatistics("output_writer");
  private final StatInt successfullWrites = new StatInt(StatKind.SUM, "successfull_writes");

  /**
   * The output writer can take a pReachedSet on .writeTestCases and traverse it, rendering out a
   * testcase for it.
   */
  public OutputWriter(LogManager pLogger, PredicateCPA pPredicateCPA, Configuration pConfig)
      throws InvalidConfigurationException {
    this.logger = pLogger;
    pConfig.inject(this, OutputWriter.class);
    this.predicateCPA = pPredicateCPA;

    if (testcaseOutputDir != null) {
      try {
        Files.createDirectories(testcaseOutputDir);
      } catch (IOException exc) {
        throw new InvalidConfigurationException("Could not create configured output dir", exc);
      }
      writeTestMetadata();
    }
  }

  /**
   * Write the metadata file necessary for testcomp to the output path.
   *
   * <p>This only needs to be done once and does not contain testcase specific information.
   */
  private void writeTestMetadata() {
    this.stats.start();
    Path metaFilePath = this.testcaseOutputDir.resolve(Paths.get("metadata.xml"));
    try (Writer metadata = IO.openOutputFile(metaFilePath, Charset.defaultCharset())) {
      XMLTestCaseExport.writeXMLMetadata(metadata, predicateCPA.getCfa(), null, "legion");
      metadata.flush();
    } catch (IOException exc) {
      logger.logUserException(Level.SEVERE, exc, "Could not write metadata file");
    } finally {
      this.stats.finish();
      this.successfullWrites.setNextValue(1);
    }
  }

  /** Handles writing of all testcases necessary for the given reachedSet. */
  public void writeTestCases(UnmodifiableReachedSet pReachedSet) {

    if (testcaseOutputDir == null) {
      return;
    }

    this.stats.start();
    int reachedSize = pReachedSet.size();
    // Write output only if new states have been reached
    if (previousSetSize == reachedSize) {
      this.stats.finish();
      return;
    }
    logger.log(Level.INFO, "Searching through arg(" + reachedSize + ") for testcases");

    // Get starting point for search
    AbstractState first = pReachedSet.getFirstState();
    ARGState args = AbstractStates.extractStateByType(first, ARGState.class);

    // Search individual testcases
    List<Map.Entry<MemoryLocation, ValueAndType>> values = new ArrayList<>();
    searchTestCase(args, values);

    // Determine file to open
    String violationStr = "";
    if (pReachedSet.hasViolatedProperties()) {
      violationStr = "_error";
    }
    Duration sinceZero = Duration.between(this.zero, Instant.now());
    String filename =
        String.format(
            "testcase_%016d_%s%s.xml", sinceZero.toNanos(), this.testCaseNumber, violationStr);
    Path testcasePath = this.testcaseOutputDir.resolve(Paths.get(filename));
    

    // Get content
    String inputs = writeVariablesToTestcase(values);
    if (inputs.length() < 1) {
      this.previousSetSize = reachedSize;
      this.stats.finish();
      return;
    }

    logger.log(Level.WARNING, "Writing testcase ", testcasePath);
    try (Writer testcase =
        IO.openOutputFile(testcasePath, Charset.defaultCharset())) {
      // Write header
      testcase.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
      testcase.write(
          "<!DOCTYPE testcase PUBLIC \"+//IDN sosy-lab.org//DTD test-format testcase 1.0//EN\""
              + " \"https://sosy-lab.org/test-format/testcase-1.0.dtd\">\n");
      testcase.write("<testcase>\n");

      // Add input testcases
      testcase.write(inputs);

      // Footer and flush
      testcase.write("</testcase>\n");
      testcase.flush();
      this.successfullWrites.setNextValue(1);
    } catch (IOException exc) {
      logger.log(Level.SEVERE, "Could not write test output", exc);
    } finally {
      this.testCaseNumber += 1;
      this.previousSetSize = reachedSize;
      this.stats.finish();
    }
  }

  /**
   * Extract the variable name from the left hand side.
   *
   * <p>In case of a regular CIdExpression this is easy, just get the name. But with arrays, this is
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
   * <p>This performs walk along the children of state. The child to walk down to is selected by the
   * highest state id (meaning the newest). This results in a list of values starting at the given
   * state and walking the newest path through it's children.
   *
   * @param state The starting state.
   * @param values The list of values to append to.
   */
  private void searchTestCase(ARGState state, List<Map.Entry<MemoryLocation, ValueAndType>> values) {
    // check if is nondet assignment
    LocationState ls = AbstractStates.extractStateByType(state, LocationState.class);

    for (CFAEdge edge : ls.getIngoingEdges()) {
      // Extract assignment
      CFunctionCallAssignmentStatement assignment = extractAssignment(edge);
      if (assignment == null) {
        continue;
      }

      // Check if assignment is for a nondeterministic variable
      if (assignment.getRightHandSide().toString().startsWith(VERIFIER_NONDET)) {
        String functionName = ls.getLocationNode().getFunctionName();
        String identifier = getLeftHandName(assignment.getLeftHandSide());

        ValueAnalysisState vs = AbstractStates.extractStateByType(state, ValueAnalysisState.class);
        Map.Entry<MemoryLocation, ValueAndType> vt =
            getValueTypeFromState(functionName, identifier, vs);
        if (vt != null) {
          values.add(vt);
        } else {
          logger.log(Level.WARNING, "No values to write, propably `MemoryLocation forgotten`");
        }
      }
    }

    // find largest child state
    ARGState largestChild = null;
    for (ARGState child : state.getChildren()) {
      if (largestChild == null || largestChild.getStateId() < child.getStateId()) {
        largestChild = child;
      }
    }

    // If largestChild still null -> at the bottom of the graph
    if (largestChild == null) {
      return;
    }

    // If not, search in largestChild
    searchTestCase(largestChild, values);
  }

  /** Retrieve an assignment statement from a CFAEdge, if this is possible. */
  private CFunctionCallAssignmentStatement extractAssignment(CFAEdge edge) {
    if (edge.getEdgeType() == CFAEdgeType.StatementEdge) {
      CStatement statement = ((CStatementEdge) edge).getStatement();
      if (statement instanceof CFunctionCallAssignmentStatement) {
        return ((CFunctionCallAssignmentStatement) statement);
      }
    }
    return null;
  }

  /** Write variables from values to a testcase file. */
  private String writeVariablesToTestcase(List<Map.Entry<MemoryLocation, ValueAndType>> values) {

    StringBuilder sb = new StringBuilder();
    for (Map.Entry<MemoryLocation, ValueAndType> v : values) {
      MemoryLocation name = v.getKey();
      Type type = v.getValue().getType();
      Value value = v.getValue().getValue();

      String valueStr = "";
      if (value instanceof NumericValue) {
        valueStr = String.valueOf(((NumericValue) value).longValue());
      } else {
        valueStr = value.toString();
      }

      sb.append(
          String.format("\t<input variable=\"%s\" type=\"%s\">%s</input>%n", name, type, valueStr));
    }
    return sb.toString();
  }

  /**
   * Retrieve variables a MemoryLocation and ValueAndType from a ValueAnalysisState by its function
   * name and identifier (=name).
   *
   * @param functionName Name of the function the variable is contained in.
   * @param identifier The name of the function.
   * @return The constants entry for this value or null.
   */
  private static Map.Entry<MemoryLocation, ValueAndType> getValueTypeFromState(
      String functionName, String identifier, ValueAnalysisState state) {
    for (Map.Entry<MemoryLocation, ValueAndType> entry : state.getConstants()) {
      MemoryLocation loc = entry.getKey();

      if (loc == null) {
        continue;
      }

      String fnName = loc.getFunctionName();
      String ident = loc.getIdentifier();

      if (fnName == null || ident == null) {
        continue;
      }

      if (fnName.equals(functionName) && ident.equals(identifier)) {
        return entry;
      }
    }
    return null;
  }

  public LegionComponentStatistics getStats() {
    this.stats.setOther(this.successfullWrites);
    return this.stats;
  }
}
