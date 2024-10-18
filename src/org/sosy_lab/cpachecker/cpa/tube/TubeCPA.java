// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.tube;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

@Options(prefix = "tubeCPA")
public class TubeCPA extends AbstractCPA {
  /** Represents the path to a file containing tubes list. */
  @Option(secure = true, required = true, description = "List of files with tubes")
  @FileOption(Type.REQUIRED_INPUT_FILE)
  private Path initialFile;

  /** Private final variable that holds an instance of ObjectMapper. */
  private final ObjectMapper om = new ObjectMapper();

  /** Represents a LogManager object for handling logging functionalities. */
  private final LogManager logger;

  /** Represents the Control Flow Automaton (CFA) associated with a program. */
  private final CFA cfa;

  /**
   * Represents a type handler for translating C types to formulas. This class provides methods for
   * calculating the size and offset of types, as well as obtaining the pointer type for the given
   * machine model.
   */
  CtoFormulaTypeHandler typeHandler;

  /**
   * Represents a supplier function that accepts a FormulaManagerView and returns a
   * CtoFormulaConverter.
   */
  private final Function<FormulaManagerView, CtoFormulaConverter> supplier;

  /**
   * Constructor for TubeCPA class.
   *
   * @param config the configuration for TubeCPA
   * @param pLogger the logger for TubeCPA
   * @param pShutdownNotifier the shutdown notifier for TubeCPA
   * @param pCfa the CFA for TubeCPA
   */
  public TubeCPA(
      Configuration config, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCfa) {
    super("sep", "sep", new FlatLatticeDomain(), new TubeTransferRelation());
    try {
      config.inject(this);
      this.logger = pLogger;
      FormulaEncodingOptions options = new FormulaEncodingOptions(config);
      this.typeHandler = new CtoFormulaTypeHandler(pLogger, pCfa.getMachineModel());
      supplier =
          formulaManager ->
              new CtoFormulaConverter(
                  options,
                  formulaManager,
                  pCfa.getMachineModel(),
                  pCfa.getVarClassification(),
                  pLogger,
                  pShutdownNotifier,
                  typeHandler,
                  AnalysisDirection.FORWARD);
      this.cfa = pCfa;

    } catch (InvalidConfigurationException pE) {
      throw new RuntimeException(pE);
    }
  }

  /**
   * Parses a JSON file located at the given path and returns an ImmutableMap of integer keys and
   * string values.
   *
   * @param jsonFilePath the path to the JSON file to be parsed
   * @return an ImmutableMap containing integer keys and string values parsed from the JSON file
   * @throws IOException if an error occurs during reading the JSON file
   */
  public ImmutableMap<Integer, String> parseJson(Path jsonFilePath) throws IOException {
    TypeReference<Map<Integer, String>> typeRef = new TypeReference<Map<Integer, String>>() {};
    Map<Integer, String> map = om.readValue(jsonFilePath.toFile(), typeRef);

    return ImmutableMap.copyOf(map);
  }

  /**
   * Provides a factory method for creating instances of TubeCPAFactory. This method creates a new
   * TubeCPAFactory instance with the analysis direction set to FORWARD.
   *
   * @return a new TubeCPAFactory instance with analysis direction set to FORWARD
   */
  public static CPAFactory factory() {
    return new TubeCPAFactory(AnalysisDirection.FORWARD);
  }

  /**
   * Creates a TubeCPA instance with the given configuration, logger, shutdown notifier, and CFA.
   *
   * @param config the configuration for TubeCPA
   * @param logger the logger for TubeCPA
   * @param pShutdownNotifier the shutdown notifier for TubeCPA
   * @param pCfa the CFA for TubeCPA
   * @return a new TubeCPA instance
   */
  public static TubeCPA create(
      Configuration config, LogManager logger, ShutdownNotifier pShutdownNotifier, CFA pCfa) {
    return new TubeCPA(config, logger, pShutdownNotifier, pCfa);
  }

  /**
   * Gets the initial abstract state for a given CFANode and StateSpacePartition.
   *
   * @param node the CFANode to get the leaving edge from
   * @param partition the StateSpacePartition to get the initial state for
   * @return the initial AbstractState based on the given CFANode and StateSpacePartition
   * @throws InterruptedException if the operation is interrupted
   */
  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    CFAEdge edge = node.getLeavingEdge(0);
    ImmutableMap<Integer, String> asserts;
    try {
      asserts = parseJson(initialFile);
    } catch (IOException e) {
      asserts = ImmutableMap.of();
      logger.log(Level.SEVERE, "An error occurred while parsing the JSON file", e);
    }
    return new TubeState(edge, asserts, null, false, 0, supplier, logger, cfa);
  }
}
