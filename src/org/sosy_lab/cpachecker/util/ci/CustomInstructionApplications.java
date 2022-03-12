// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ci;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class CustomInstructionApplications {

  private final Map<CFANode, AppliedCustomInstruction> cis;
  private final CustomInstruction ci;

  /**
   * Constructor of CustomInstructionApplications
   *
   * @param pCis ImmutableMap
   */
  protected CustomInstructionApplications(
      final Map<CFANode, AppliedCustomInstruction> pCis, final CustomInstruction pCi) {
    cis = pCis;
    ci = pCi;
  }

  /**
   * Checks if the ImmutableMap cis contains the given CFANode (after it is extracted out of the
   * AbstractState)
   *
   * @param pState AbstractState
   * @return true if cis contains the given node
   * @throws CPAException if the given node can't be extracted
   */
  public boolean isStartState(final AbstractState pState) throws CPAException {
    CFANode locState = AbstractStates.extractLocation(pState);
    if (locState == null) {
      throw new CPAException("The state " + pState + " has to contain a location state!");
    }
    return cis.containsKey(locState);
  }

  /**
   * Checks if the given AbstractState pIsEnd is an endNode of the given AbsractState pCISart
   *
   * @param pIsEnd AbstractState
   * @param pCIStart AbstractState
   * @return true if pIsEnd is an endNode of pCISart
   */
  public boolean isEndState(final AbstractState pIsEnd, final AbstractState pCIStart)
      throws CPAException {
    return isEndState(pIsEnd, AbstractStates.extractLocation(pCIStart));
  }

  /**
   * Checks if the given AbstractState pIsEnd is an endNode of the given CFANode pCISart
   *
   * @param pIsEnd AbstractState
   * @param pCIStart CFANode
   * @return true if pIsEnd is an endNode of pCISart
   */
  public boolean isEndState(final AbstractState pIsEnd, final CFANode pCIStart)
      throws CPAException {
    assert cis.containsKey(pCIStart);
    return cis.get(pCIStart).isEndState(pIsEnd);
  }

  public AppliedCustomInstruction getAppliedCustomInstructionFor(final ARGState pState)
      throws CPAException {
    CFANode locState = AbstractStates.extractLocation(pState);

    if (locState == null) {
      throw new CPAException("The state " + pState + " has to contain a location state!");
    }

    if (!isStartState(pState)) {
      throw new CPAException("The state does not represent start of known custom instruction");
    }

    return cis.get(locState);
  }

  public CustomInstruction getCustomInstruction() {
    return ci;
  }

  public Map<CFANode, AppliedCustomInstruction> getMapping() {
    return cis;
  }

  public ImmutableSet<CFANode> getStartAndEndLocationsOfCIApplications() {
    ImmutableSet.Builder<CFANode> result = ImmutableSet.builder();

    for (AppliedCustomInstruction aci : cis.values()) {
      result.addAll(aci.getStartAndEndNodes());
    }
    return result.build();
  }

  public int getNumApplications() {
    return cis.size();
  }

  @Options(prefix = "custominstructions")
  public abstract static class CustomInstructionApplicationBuilder {

    public enum CIDescriptionType {
      MANUAL,
      OPERATOR,
      AUTOMATIC
    }

    @Option(
        secure = true,
        name = "ciSignature",
        description =
            "Signature for custom instruction, describes names and order of input and output"
                + " variables of a custom instruction")
    @FileOption(FileOption.Type.OUTPUT_FILE)
    protected Path ciSpec = Path.of("ci_spec.txt");

    protected final LogManager logger;
    protected final ShutdownNotifier shutdownNotifier;
    protected final CFA cfa;

    protected CustomInstructionApplicationBuilder(
        final Configuration config,
        final LogManager pLogger,
        final ShutdownNotifier sdNotifier,
        final CFA pCfa)
        throws InvalidConfigurationException {
      config.inject(this, CustomInstructionApplicationBuilder.class);
      logger = pLogger;
      shutdownNotifier = sdNotifier;
      cfa = pCfa;
    }

    public abstract CustomInstructionApplications identifyCIApplications()
        throws AppliedCustomInstructionParsingFailedException, IOException, InterruptedException,
            UnrecognizedCodeException;

    public static CustomInstructionApplicationBuilder getBuilder(
        CIDescriptionType type,
        Configuration pConfig,
        LogManager pLogger,
        ShutdownNotifier pSdNotifier,
        CFA pCfa)
        throws InvalidConfigurationException {
      switch (type) {
        case AUTOMATIC:
          return new CustomInstructionApplicationsAutomatic(pConfig, pCfa, pLogger, pSdNotifier);
        case MANUAL:
          return new CustomInstructionApplicationsFromFile(pConfig, pCfa, pLogger, pSdNotifier);
        case OPERATOR:
          return new CustomInstructionsForBinaryOperator(pConfig, pLogger, pSdNotifier, pCfa);
        default:
          throw new IllegalArgumentException(
              "Unknown type of custom instruction applications identifier");
      }
    }
  }

  @Options(prefix = "custominstructions")
  private static class CustomInstructionApplicationsFromFile
      extends CustomInstructionApplicationBuilder {

    @Option(
        secure = true,
        name = "definitionFile",
        description = "File specifying start locations of custom instruction applications")
    @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
    private Path appliedCustomInstructionsDefinition = Path.of("ci_def.txt");

    public CustomInstructionApplicationsFromFile(
        Configuration pConfig, final CFA pCfa, LogManager pLogger, ShutdownNotifier pSdNotifier)
        throws InvalidConfigurationException {
      super(pConfig, pLogger, pSdNotifier, pCfa);

      pConfig.inject(this, CustomInstructionApplicationsFromFile.class);

      try {
        IO.checkReadableFile(appliedCustomInstructionsDefinition);
      } catch (FileNotFoundException e) {
        throw new InvalidConfigurationException(
            "Definition file for custom instruction application does not exist", e);
      }
    }

    @Override
    public CustomInstructionApplications identifyCIApplications()
        throws AppliedCustomInstructionParsingFailedException, IOException, InterruptedException {
      return new AppliedCustomInstructionParser(shutdownNotifier, logger, cfa)
          .parse(appliedCustomInstructionsDefinition, ciSpec);
    }
  }

  @Options(prefix = "custominstructions")
  private static class CustomInstructionApplicationsAutomatic
      extends CustomInstructionApplicationBuilder {

    @Option(
        secure = true,
        name = "ciFun",
        description = "Name of function containing the custom instruction definition")
    private String ciFunction;

    @Option(
        secure = true,
        name = "definitionFile",
        description = "File specifying start locations of custom instruction applications")
    @FileOption(FileOption.Type.OUTPUT_FILE)
    private Path appliedCustomInstructionsDefinition = Path.of("ci_def.txt");

    public CustomInstructionApplicationsAutomatic(
        Configuration pConfig, final CFA pCfa, LogManager pLogger, ShutdownNotifier pSdNotifier)
        throws InvalidConfigurationException {
      super(pConfig, pLogger, pSdNotifier, pCfa);
      pConfig.inject(this);
      Preconditions.checkNotNull(ciFunction);
    }

    @Override
    public CustomInstructionApplications identifyCIApplications()
        throws AppliedCustomInstructionParsingFailedException, IOException, InterruptedException {
      AppliedCustomInstructionParser parser =
          new AppliedCustomInstructionParser(shutdownNotifier, logger, cfa);

      return parser.parse(findCIApplications(parser), appliedCustomInstructionsDefinition);
    }

    private CustomInstruction findCIApplications(final AppliedCustomInstructionParser pParser)
        throws AppliedCustomInstructionParsingFailedException, InterruptedException, IOException {
      CustomInstruction ci = pParser.readCustomInstruction(ciFunction);

      try (Writer out =
          IO.openOutputFile(appliedCustomInstructionsDefinition, Charset.defaultCharset())) {
        for (CFANode node : cfa.getAllNodes()) {
          if (!Objects.equals(node, ci.getStartNode()) && pParser.isAppliedCI(ci, node)) {
            shutdownNotifier.shutdownIfNecessary();
            out.append(node.getNodeNumber() + "\n");
          }
        }
      }

      return ci;
    }
  }

  @Options(prefix = "custominstructions")
  private static class CustomInstructionsForBinaryOperator
      extends CustomInstructionApplicationBuilder {

    @Option(
        secure = true,
        description =
            "Specify simple custom instruction by specifying the binary operator op. All simple cis"
                + " are of the form r = x op y. Leave empty (default) if you specify a more complex"
                + " custom instruction within code.")
    private BinaryOperator binaryOperatorForSimpleCustomInstruction = BinaryOperator.PLUS;

    @Option(
        secure = true,
        name = "definitionFile",
        description = "File to dump start location of identified custom instruction applications")
    @FileOption(FileOption.Type.OUTPUT_FILE)
    private Path foundCustomInstructionsDefinition = Path.of("ci_def.txt");

    public CustomInstructionsForBinaryOperator(
        Configuration pConfig, LogManager pLogger, ShutdownNotifier pSdNotifier, CFA pCfa)
        throws InvalidConfigurationException {
      super(pConfig, pLogger, pSdNotifier, pCfa);
      pConfig.inject(this);

      logger.log(
          Level.FINE, "Using a simple custom instruction. Find out the applications ourselves");
    }

    private CustomInstructionApplications findSimpleCustomInstructionApplications()
        throws AppliedCustomInstructionParsingFailedException, IOException, InterruptedException,
            UnrecognizedCodeException {
      // build simple custom instruction, is of the form r= x pOp y;
      // create variable expressions
      CType type = CNumericTypes.INT;
      CIdExpression r, x, y;
      r =
          new CIdExpression(
              FileLocation.DUMMY,
              new CVariableDeclaration(
                  FileLocation.DUMMY, true, CStorageClass.AUTO, type, "r", "r", "r", null));
      x =
          new CIdExpression(
              FileLocation.DUMMY,
              new CVariableDeclaration(
                  FileLocation.DUMMY, true, CStorageClass.AUTO, type, "x", "x", "x", null));
      y =
          new CIdExpression(
              FileLocation.DUMMY,
              new CVariableDeclaration(
                  FileLocation.DUMMY, true, CStorageClass.AUTO, type, "y", "y", "y", null));
      // create statement
      CExpressionAssignmentStatement stmt =
          new CExpressionAssignmentStatement(
              FileLocation.DUMMY,
              r,
              new CBinaryExpressionBuilder(MachineModel.LINUX64, logger)
                  .buildBinaryExpression(x, y, binaryOperatorForSimpleCustomInstruction));
      // create edge
      CFunctionDeclaration ciDef =
          new CFunctionDeclaration(
              FileLocation.DUMMY,
              CFunctionType.NO_ARGS_VOID_FUNCTION,
              "ci",
              ImmutableList.of(),
              ImmutableSet.of());
      CFANode start = new CFANode(ciDef);
      CFANode end = new CFANode(ciDef);
      CFAEdge ciEdge =
          new CStatementEdge(
              "r=x" + binaryOperatorForSimpleCustomInstruction + "y;",
              stmt,
              FileLocation.DUMMY,
              start,
              end);
      start.addLeavingEdge(ciEdge);
      end.addEnteringEdge(ciEdge);
      // build custom instruction
      ImmutableList<String> input = ImmutableList.of("x", "y");
      CustomInstruction ci =
          new CustomInstruction(
              start, ImmutableSet.of(end), input, ImmutableList.of("r"), shutdownNotifier);

      // find applied custom instructions in program
      try (Writer aciDef =
          IO.openOutputFile(foundCustomInstructionsDefinition, Charset.defaultCharset())) {

        // inspect all CFA edges potential candidates
        for (CFANode node : cfa.getAllNodes()) {
          for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
            if (edge instanceof CStatementEdge
                && ((CStatementEdge) edge).getStatement()
                    instanceof CExpressionAssignmentStatement) {
              stmt = (CExpressionAssignmentStatement) ((CStatementEdge) edge).getStatement();
              if (stmt.getRightHandSide() instanceof CBinaryExpression
                  && ((CBinaryExpression) stmt.getRightHandSide())
                      .getOperator()
                      .equals(binaryOperatorForSimpleCustomInstruction)
                  && stmt.getLeftHandSide().getExpressionType().equals(type)) {
                // application of custom instruction found, add to definition file
                aciDef.write(node.getNodeNumber() + "\n");
              }
            }
          }
        }
      }

      try (Writer br = IO.openOutputFile(ciSpec, Charset.defaultCharset())) {
        // write signature
        br.write(ci.getSignature() + "\n");
        String ciString = ci.getFakeSMTDescription().getSecond();
        br.write(ciString.substring(ciString.indexOf("a") - 1, ciString.length() - 1) + ";");
      }

      return new AppliedCustomInstructionParser(shutdownNotifier, logger, cfa)
          .parse(ci, foundCustomInstructionsDefinition);
    }

    @Override
    public CustomInstructionApplications identifyCIApplications()
        throws UnrecognizedCodeException, AppliedCustomInstructionParsingFailedException,
            IOException, InterruptedException {
      CustomInstructionApplications cia = findSimpleCustomInstructionApplications();
      logger.log(
          Level.INFO,
          "Found ",
          cia.getMapping().size(),
          " applications of binary operator",
          binaryOperatorForSimpleCustomInstruction,
          " in code.");
      return cia;
    }
  }
}
