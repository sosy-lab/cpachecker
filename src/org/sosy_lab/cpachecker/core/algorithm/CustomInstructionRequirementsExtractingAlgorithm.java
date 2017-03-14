/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.ci.AppliedCustomInstruction;
import org.sosy_lab.cpachecker.util.ci.AppliedCustomInstructionParser;
import org.sosy_lab.cpachecker.util.ci.AppliedCustomInstructionParsingFailedException;
import org.sosy_lab.cpachecker.util.ci.CustomInstruction;
import org.sosy_lab.cpachecker.util.ci.CustomInstructionApplications;
import org.sosy_lab.cpachecker.util.ci.CustomInstructionRequirementsExtractor;


@Options(prefix="custominstructions")
public class CustomInstructionRequirementsExtractingAlgorithm implements Algorithm, StatisticsProvider {

  private final Algorithm analysis;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  @Option(secure=true, name="definitionFile", description = "File to be parsed")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path appliedCustomInstructionsDefinition;

  @Option(secure=true, name="ciSignature", description = "Signature for custom instruction, describes names and order of input and output variables of a custom instruction")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path ciSpec = Paths.get("ci_spec.txt");

  @Option(secure = true,
      description = "Specify simple custom instruction by specifying the binary operator op. All simple cis are of the form r = x op y. Leave empty (default) if you specify a more complex custom instruction within code.",
      values = { "MULTIPLY", "DIVIDE", "MODULO", "PLUS", "MINUS", "SHIFT_LEFT", "SHIFT_RIGHT", "LESS_THAN",
          "GREATER_THAN", "LESS_EQUAL", "GREATER_EQUAL", "BINARY_AND", "BINARY_XOR", "BINARY_OR", "EQUALS",
          "NOT_EQUALS", ""})
  private String binaryOperatorForSimpleCustomInstruction = "";

  private CFA cfa;
  private final ConfigurableProgramAnalysis cpa;
  private final CustomInstructionRequirementsExtractor ciExtractor;

  /**
   * Constructor of CustomInstructionRequirementsExtractingAlgorithm
   * @param analysisAlgorithm Algorithm
   * @param cpa ConfigurableProgramAnalysis
   * @param config Configuration
   * @param logger LogManager
   * @param sdNotifier ShutdownNotifier
   * @throws InvalidConfigurationException if the given Path not exists
   */
  public CustomInstructionRequirementsExtractingAlgorithm(final Algorithm analysisAlgorithm,
      final ConfigurableProgramAnalysis cpa, final Configuration config, final LogManager logger,
      final ShutdownNotifier sdNotifier, final CFA cfa) throws InvalidConfigurationException {

    config.inject(this);

    analysis = analysisAlgorithm;
    this.logger = logger;
    this.shutdownNotifier = sdNotifier;
    this.cpa = cpa;

    if (!(cpa instanceof ARGCPA)) {
      throw new InvalidConfigurationException("The given cpa " + cpa + "is not an instance of ARGCPA");
    }

    if (appliedCustomInstructionsDefinition == null) {
      throw new InvalidConfigurationException(
        "Need to specify at least a path where to save the applied custom instruction definition.");
    }

    if (!appliedCustomInstructionsDefinition.toFile().exists() && binaryOperatorForSimpleCustomInstruction.isEmpty()) {
      throw new InvalidConfigurationException("The given path '" + appliedCustomInstructionsDefinition + "' is not a valid path to a file.");
    }

    ciExtractor = new CustomInstructionRequirementsExtractor(config, logger, sdNotifier, cpa);

    Class<? extends AbstractState> requirementsStateClass = ciExtractor.getRequirementsStateClass();
    try {
      if (AbstractStates.extractStateByType(cpa.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition()),
                                            requirementsStateClass) == null) {
        throw new InvalidConfigurationException(requirementsStateClass + "is not an abstract state.");
      }
    } catch (InterruptedException e) {
      throw new InvalidConfigurationException(requirementsStateClass + "initial state computation did not finish in time");
    }

    this.cfa = cfa;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException,
      CPAEnabledAnalysisPropertyViolationException {

    logger.log(Level.INFO, "Get custom instruction applications in program.");

    CustomInstructionApplications cia = null;
    try {
      if (binaryOperatorForSimpleCustomInstruction.isEmpty()) {
        cia = new AppliedCustomInstructionParser(shutdownNotifier, logger, cfa)
            .parse(appliedCustomInstructionsDefinition, ciSpec);
      } else {
        logger.log(Level.FINE, "Using a simple custom instruction. Find out the applications ourselves");
        cia = findSimpleCustomInstructionApplications(BinaryOperator.valueOf(binaryOperatorForSimpleCustomInstruction));
        logger.log(Level.INFO, "Found ", cia.getMapping().size(), " applications of binary operatior",
            binaryOperatorForSimpleCustomInstruction, " in code.");
      }
    } catch (IllegalArgumentException ie) {
      logger.log(Level.SEVERE, "Unknown binary operator ", binaryOperatorForSimpleCustomInstruction,
          ". Abort requirement extraction.", ie);
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
    } catch (FileNotFoundException ex) {
      logger.log(Level.SEVERE, "The file '" + appliedCustomInstructionsDefinition + "' was not found", ex);
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Parsing the file '" + appliedCustomInstructionsDefinition + "' failed.", e);
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
    }

    if (ciExtractor.getRequirementsStateClass().equals(PredicateAbstractState.class)) {
      PredicateCPA predCPA = CPAs.retrieveCPA(cpa, PredicateCPA.class);
      if (predCPA == null) {
        logger.log(Level.SEVERE,
            "Cannot find PredicateCPA in CPA configuration but it is required to set abstraction nodes");
        return AlgorithmStatus.UNSOUND_AND_PRECISE;
      }
      predCPA.changeExplicitAbstractionNodes(extractAdditionalAbstractionLocations(cia));
    }

    shutdownNotifier.shutdownIfNecessary();
    logger.log(Level.INFO, "Start analysing to compute requirements.");

    AlgorithmStatus status = analysis.run(pReachedSet);

    // analysis was unsound
    if (!status.isSound()) {
      logger.log(Level.SEVERE, "Do not extract requirements since analysis failed.");
      return status;
    }

    shutdownNotifier.shutdownIfNecessary();
    logger.log(Level.INFO, "Start extracting requirements for applied custom instructions");

    ciExtractor.extractRequirements((ARGState)pReachedSet.getFirstState(), cia);
    return status;
  }

  private ImmutableSet<CFANode> extractAdditionalAbstractionLocations(final CustomInstructionApplications pCia) {
    Builder<CFANode> result = ImmutableSet.builder();

    for (AppliedCustomInstruction aci : pCia.getMapping().values()) {
      result.addAll(aci.getStartAndEndNodes());
    }
    return result.build();
  }

  private CustomInstructionApplications findSimpleCustomInstructionApplications(final BinaryOperator pOp)
      throws AppliedCustomInstructionParsingFailedException, IOException, InterruptedException, UnrecognizedCCodeException {
    // build simple custom instruction, is of the form r= x pOp y;
   // create variable expressions
    CType type = new CSimpleType(false, false, CBasicType.INT, false, false, false, false, false, false, false);
    CIdExpression r, x, y;
    r = new CIdExpression(FileLocation.DUMMY, new CVariableDeclaration(FileLocation.DUMMY, true, CStorageClass.AUTO,
            type, "r", "r", "r", null));
    x = new CIdExpression(FileLocation.DUMMY, new CVariableDeclaration(FileLocation.DUMMY, true, CStorageClass.AUTO,
            type, "x", "x", "x", null));
    y = new CIdExpression(FileLocation.DUMMY, new CVariableDeclaration(FileLocation.DUMMY, true, CStorageClass.AUTO,
            type, "y", "y", "y", null));
    // create statement
    CExpressionAssignmentStatement stmt =
        new CExpressionAssignmentStatement(FileLocation.DUMMY, r, new CBinaryExpressionBuilder(MachineModel.LINUX64,
            logger).buildBinaryExpression(x, y, pOp));
    // create edge
    CFANode start, end;
    start = new CFANode("ci");
    end = new CFANode("ci");
    CFAEdge ciEdge = new CStatementEdge("r=x" + pOp + "y;", stmt, FileLocation.DUMMY, start, end);
    start.addLeavingEdge(ciEdge);
    end.addEnteringEdge(ciEdge);
    // build custom instruction
    List<String> input = new ArrayList<>(2);
    input.add("x");
    input.add("y");
    CustomInstruction ci = new CustomInstruction(start, Collections.singleton(end),
        input, Collections.singletonList("r"), shutdownNotifier);

    // find applied custom instructions in program
    try (Writer aciDef =
        MoreFiles.openOutputFile(appliedCustomInstructionsDefinition, Charset.defaultCharset())) {

      // inspect all CFA edges potential candidates
      for (CFANode node : cfa.getAllNodes()) {
        for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
          if (edge instanceof CStatementEdge
              && ((CStatementEdge) edge).getStatement() instanceof CExpressionAssignmentStatement) {
            stmt = (CExpressionAssignmentStatement) ((CStatementEdge) edge).getStatement();
            if (stmt.getRightHandSide() instanceof CBinaryExpression
                && ((CBinaryExpression) stmt.getRightHandSide()).getOperator().equals(pOp) &&
                stmt.getLeftHandSide().getExpressionType().equals(type)) {
              // application of custom instruction found, add to definition file
              aciDef.write(node.getNodeNumber()+"\n");
            }
          }
        }
      }
    }

    try (Writer br = MoreFiles.openOutputFile(ciSpec, Charset.defaultCharset())) {
      // write signature
      br.write(ci.getSignature() + "\n");
      String ciString = ci.getFakeSMTDescription().getSecond();
      br.write(ciString.substring(ciString.indexOf("a")-1,ciString.length()-1) + ";");
    }

    return new AppliedCustomInstructionParser(shutdownNotifier, logger, cfa).
        parse(ci, appliedCustomInstructionsDefinition);
  }


  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (analysis instanceof StatisticsProvider) {
      ((StatisticsProvider) analysis).collectStatistics(pStatsCollection);
    }
  }
}