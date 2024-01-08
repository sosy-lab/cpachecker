// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.directexport;

import static java.util.logging.Level.WARNING;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.invariantwitness.directexport.DataTypes.ExpressionType;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.InvariantWitnessWriter.GraphTraverser;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.InvariantWitnessWriter.YamlWitnessExportException;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.SetEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.EnsuresRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.ExportableRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.FunctionContractRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.InvariantRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.InvariantRecord.InvariantRecordType;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.LocationRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.RequiresRecord;

public class ARGToWitness extends DirectWitnessExporter {

  public ARGToWitness(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  private static class CollectRelevantARGStates
      extends GraphTraverser<ARGState, YamlWitnessExportException> {

    protected Multimap<CFANode, ARGState> loopInvariants = HashMultimap.create();
    protected Multimap<CFANode, ARGState> functionCallInvariants = HashMultimap.create();
    protected Multimap<FunctionEntryNode, ARGState> functionContractRequires =
        HashMultimap.create();
    protected Multimap<FunctionExitNode, ARGState> functionContractEnsures = HashMultimap.create();

    protected CollectRelevantARGStates(ARGState startNode) {
      super(startNode);
    }

    @Override
    protected ARGState visit(ARGState pSuccessor) throws YamlWitnessExportException {
      for (LocationState state :
          AbstractStates.asIterable(pSuccessor).filter(LocationState.class)) {
        CFANode node = state.getLocationNode();
        FluentIterable<CFAEdge> leavingEdges = CFAUtils.leavingEdges(node);
        if (node.isLoopStart()) {
          loopInvariants.put(node, pSuccessor);
        } else if (leavingEdges.size() == 1
            && leavingEdges.anyMatch(e -> e instanceof FunctionCallEdge)) {
          functionCallInvariants.put(node, pSuccessor);
        } else if (node instanceof FunctionEntryNode functionEntryNode) {
          functionContractRequires.put(functionEntryNode, pSuccessor);
        } else if (node instanceof FunctionExitNode functionExitNode) {
          functionContractEnsures.put(functionExitNode, pSuccessor);
        }
      }

      return pSuccessor;
    }

    @Override
    protected Iterable<ARGState> getSuccessors(ARGState pCurrent)
        throws YamlWitnessExportException {
      return pCurrent.getChildren();
    }
  }

  private ExpressionTree<Object> getOverapproximationOfStates(
      Collection<ARGState> argStates, CFANode node) throws InterruptedException {
    FunctionEntryNode entryNode = cfa.getFunctionHead(node.getFunctionName());

    FluentIterable<ExpressionTreeReportingState> reportingStates =
        FluentIterable.from(argStates)
            .transformAndConcat(AbstractStates::asIterable)
            .filter(ExpressionTreeReportingState.class);
    List<List<ExpressionTree<Object>>> expressionsPerClass = new ArrayList<>();
    for (Class<?> stateClass : reportingStates.transform(AbstractState::getClass).toSet()) {
      List<ExpressionTree<Object>> expressionsMatchingClass = new ArrayList<>();
      for (ExpressionTreeReportingState state : reportingStates) {
        if (stateClass.isAssignableFrom(state.getClass())) {
          expressionsMatchingClass.add(state.getFormulaApproximation(entryNode, node));
        }
      }
      expressionsPerClass.add(expressionsMatchingClass);
    }
    return And.of(FluentIterable.from(expressionsPerClass).transform(Or::of));
  }

  private InvariantRecord createLoopInvariant(Collection<ARGState> argStates, CFANode node)
      throws InterruptedException, IOException {
    ListMultimap<String, Integer> lineOffsetByLine = getlineOffsetsByFile();

    // We now conjunct all the overapproximations of the states and export them as loop invariants
    // TODO: Determine location from the ASTStructure
    FileLocation fileLocation = null;
    ExpressionTree<Object> invariant = getOverapproximationOfStates(argStates, node);
    LocationRecord locationRecord =
        Utils.createLocationRecordAtStart(fileLocation, lineOffsetByLine, node.getFunctionName());

    InvariantRecord invariantRecord =
        new InvariantRecord(
            invariant.toString(),
            InvariantRecordType.LOOP_INVARIANT.getKeyword(),
            ExpressionType.C.toString(),
            locationRecord);

    return invariantRecord;
  }

  private void exportEntries(AbstractEntry entry, Path outFile) {
    try (Writer writer = IO.openOutputFile(outFile, Charset.defaultCharset())) {
      String entryYaml = mapper.writeValueAsString(ImmutableList.of(entry));
      writer.write(entryYaml);
    } catch (IOException e) {
      logger.logfException(WARNING, e, "Invariant witness export to %s failed.", outFile);
    }
  }

  private List<FunctionContractRecord> handleFunctionContract(
      Multimap<FunctionEntryNode, ARGState> functionContractRequires,
      Multimap<FunctionExitNode, ARGState> functionContractEnsures)
      throws InterruptedException, IOException {
    List<FunctionContractRecord> functionContractRecords = new ArrayList<>();
    for (FunctionEntryNode node : functionContractRequires.keySet()) {
      Collection<ARGState> requiresArgStates = functionContractRequires.get(node);

      FileLocation location = node.getFileLocation();
      String requiresClause = getOverapproximationOfStates(requiresArgStates, node).toString();
      String ensuresClause = "1";
      if (node.getExitNode().isPresent()
          && functionContractEnsures.containsKey(node.getExitNode().orElseThrow())) {
        Collection<ARGState> ensuresArgStates =
            functionContractEnsures.get(node.getExitNode().orElseThrow());
        ensuresClause = getOverapproximationOfStates(ensuresArgStates, node).toString();
      }
      functionContractRecords.add(
          new FunctionContractRecord(
              new EnsuresRecord(ImmutableList.of(ensuresClause)),
              new RequiresRecord(ImmutableList.of(requiresClause)),
              ExpressionType.C,
              Utils.createLocationRecordAtStart(
                  location, getlineOffsetsByFile(), node.getFunctionName())));
    }

    return functionContractRecords;
  }

  public void export(ARGState pRootState, Path outFile)
      throws YamlWitnessExportException, InterruptedException, IOException {
    // Collect the information about the states which contain the information about the invariants
    CollectRelevantARGStates statesCollector = new CollectRelevantARGStates(pRootState);
    statesCollector.traverse();

    Multimap<CFANode, ARGState> loopInvariants = statesCollector.loopInvariants;
    @SuppressWarnings("unused")
    Multimap<CFANode, ARGState> functionCallInvariants = statesCollector.functionCallInvariants;

    // Use the collected states to generate invariants
    List<ExportableRecord> entries = new ArrayList<>();

    // First handle the loop invariants
    for (CFANode node : loopInvariants.keySet()) {
      Collection<ARGState> argStates = loopInvariants.get(node);
      entries.add(createLoopInvariant(argStates, node));
    }

    // If we are exporting to witness version 3.0 then we want to include function contracts
    entries.addAll(
        handleFunctionContract(
            statesCollector.functionContractRequires, statesCollector.functionContractEnsures));

    exportEntries(new SetEntry(getMetadata(), entries), outFile);
  }
}
