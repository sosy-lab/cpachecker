// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.tube;

import static com.google.common.base.Preconditions.checkState;

import ap.terfor.Formula;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CParser.Factory;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder.DummyPointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaType;


public class TubeCPA extends AbstractCPA {

  ObjectMapper om = new ObjectMapper();
  private final LogManager logger;

  String path = "C:\\Users\\ozkat\\Desktop/jsonT.json";

  private final CFA cfa;


  CtoFormulaTypeHandler typeHandler;
  private  final Function<FormulaManagerView,CtoFormulaConverter> supplier;


    /**
     * Initialize a new instance of TubeCPA.
     *
     * @param config              the configuration object
     * @param pLogger              the pLogger object
     * @param pShutdownNotifier   the shutdown notifier object
     * @param pCfa                the CFA object
     */
    public TubeCPA(Configuration config, LogManager pLogger,ShutdownNotifier pShutdownNotifier, CFA pCfa) {
        super("sep", "sep", new FlatLatticeDomain(), new TubeTransferRelation());
        try {
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

    /**
     * Parses the given JSON file containing formulas and converts them to a map of line numbers to BooleanFormulas.
     *
     * @param jsonFilePath The path to the JSON file containing the formulas.
     * @return An ImmutableMap containing the parsed formulas, with line numbers as keys and BooleanFormulas as values.
     * @throws IOException If an I/O error occurs while reading the JSON file.
     */
    public ImmutableMap<Integer, String> parseJson(String jsonFilePath) throws IOException {
      TypeReference<Map<Integer, String>> typeRef = new TypeReference<Map<Integer, String>>() {};
      Map<Integer, String> map = om.readValue(Paths.get(jsonFilePath).toFile(), typeRef);

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
          asserts = parseJson(path);
      } catch (IOException e) {
          asserts = ImmutableMap.of();
          logger.log(Level.SEVERE, "An error occurred while parsing the JSON file", e);
      }
      return new TubeState(edge, asserts, null, false,0,supplier, logger, cfa);
  }
}
