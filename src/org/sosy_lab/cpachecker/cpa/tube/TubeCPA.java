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
import java.nio.file.Paths;
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
  @Option(
      secure = true,
      required = true,
      description =
          "List of files with tubes")
  @FileOption(Type.OUTPUT_FILE)
  private Path initialFile;
  private final ObjectMapper om = new ObjectMapper();
  private final LogManager logger;
  private final CFA cfa;

  CtoFormulaTypeHandler typeHandler;
  private  final Function<FormulaManagerView,CtoFormulaConverter> supplier;


    public TubeCPA(Configuration config, LogManager pLogger,ShutdownNotifier pShutdownNotifier, CFA pCfa) {
        super("sep", "sep", new FlatLatticeDomain(), new TubeTransferRelation());
        try {
          config.inject(this);
            this.logger = pLogger;
            FormulaEncodingOptions options = new FormulaEncodingOptions(config);
            this.typeHandler = new CtoFormulaTypeHandler(pLogger, pCfa.getMachineModel());
            supplier = formulaManager ->
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

    public ImmutableMap<Integer, String> parseJson(Path jsonFilePath) throws IOException {
      TypeReference<Map<Integer, String>> typeRef = new TypeReference<Map<Integer, String>>() {};
      Map<Integer, String> map = om.readValue(jsonFilePath.toFile(), typeRef);

      return ImmutableMap.copyOf(map);
    }

  public static CPAFactory factory() {
    return new TubeCPAFactory(AnalysisDirection.FORWARD);
  }

  public static TubeCPA create(Configuration config, LogManager logger, ShutdownNotifier pShutdownNotifier, CFA pCfa) {
    return new TubeCPA(config, logger, pShutdownNotifier, pCfa);
  }

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
      return new TubeState(edge, asserts, null, false,0,supplier, logger, cfa);
  }
}