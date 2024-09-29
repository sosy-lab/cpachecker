// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.tube_cpa;

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

import static com.google.common.base.Preconditions.checkState;

import ap.terfor.Formula;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.annotation.*;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import javax.swing.text.html.parser.Parser;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CParser.Dialect;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAEdgeUtils;
import org.sosy_lab.cpachecker.util.CParserUtils;
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
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;


public class TubeCPA extends AbstractCPA {
    ObjectMapper om = new ObjectMapper();
  String path = "C:\\Users\\ozkat\\Desktop/jsonT.json";
  private final FormulaManagerView formulaManager;

  private final Solver solver;
  CFA cfa;

  CFAEdge dummyEdge;
  SSAMapBuilder ssaMapBuilder = SSAMap.emptySSAMap().builder();
  PointerTargetSetBuilder ptsBuilder = DummyPointerTargetSetBuilder.INSTANCE;

  CtoFormulaConverter converter;
  CtoFormulaTypeHandler typeHandler;



  public ImmutableMap<Integer, BooleanFormula> parse(String jsonFilePath) throws IOException {
    Builder<Integer, BooleanFormula> formulaMapBuilder = ImmutableMap.builder();
    BooleanFormula pBooleanFormula;

    try {
      JsonNode rootNode = om.readTree(new File(jsonFilePath));
      Iterator<Entry<String, JsonNode>> fields = rootNode.fields();
      //MachineModel.ARM, Optional.empty(),LogManager.createNullLogManager(),ShutdownNotifier.createDummy(),new CtoFormulaTypeHandler(LogManager.createNullLogManager(),MachineModel.ARM),AnalysisDirection.FORWARD);
      while (fields.hasNext()) {
        Entry<String, JsonNode> field = fields.next();
        int lineNumber = Integer.parseInt(field.getKey());
        String formulaString = field.getValue().asText();
        CExpression statements =
                (CExpression) CParserUtils.parseSingleStatement(formulaString, null,null); //should get CExpression and i call converter.covert on it and it should work

        Formula formula = (Formula) converter.buildTermForTubes(
                statements, dummyEdge, formulaString, ssaMapBuilder, ptsBuilder, new Constraints(formulaManager.getBooleanFormulaManager()), ErrorConditions.dummyInstance(formulaManager.getBooleanFormulaManager()));
        //BooleanFormula pBooleanFormula = formulaManagerView.parse(formulaString);

        FormulaType<?> formulaType = formulaManager.getFormulaType((BitvectorFormula) formula);
        if (formulaManager.getFormulaType((BitvectorFormula) formula).isBooleanType()) {
          pBooleanFormula = (BooleanFormula) formula;
        } else if (formulaType.isBooleanType()){
          pBooleanFormula = (BooleanFormula) formula;
        } else {
          throw new UnsupportedOperationException("Unsupported formula type: " + formulaType);
        }
        formulaMapBuilder.put(lineNumber, pBooleanFormula);
      }

    } catch (InvalidAutomatonException pE) {
      throw new RuntimeException(pE);
    } catch (UnrecognizedCodeException pE) {
      throw new RuntimeException(pE);
    } catch (InterruptedException pE) {
      throw new RuntimeException(pE);
    }

    return formulaMapBuilder.build();
  }


  private final FormulaManagerView formulaManagerView;
  public TubeCPA(Configuration config, LogManager logger,ShutdownNotifier pShutdownNotifier, CFA pCfa) {
    super("sep", "sep", new FlatLatticeDomain(), new TubeTransferRelation());
    try {
      solver = Solver.create(config,logger,pShutdownNotifier);
      //Solver solver = Solver.create(Configuration.defaultConfiguration(), LogManager.createNullLogManager(),
      //ShutdownNotifier.createDummy());
      formulaManagerView = solver.getFormulaManager();
      formulaManager = solver.getFormulaManager();
      FormulaEncodingOptions options = new FormulaEncodingOptions(config);
      this.typeHandler = new CtoFormulaTypeHandler(logger, pCfa.getMachineModel());
      this.converter =
              new CtoFormulaConverter(
                      options,
                      formulaManager,
                      pCfa.getMachineModel(),
                      pCfa.getVarClassification(),
                      logger,
                      pShutdownNotifier,
                      typeHandler,
                      AnalysisDirection.FORWARD);

      this.cfa = pCfa;
    } catch (InvalidConfigurationException pE) {
      throw new RuntimeException(pE);
    }
  }


  public static CPAFactory factory() {
    return new TubeCPAFactory(AnalysisDirection.FORWARD);
  }

  public static TubeCPA create() {
    return new TubeCPA();
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
          throws InterruptedException {
    CFAEdge edge = node.getLeavingEdge(0);
      ImmutableMap<Integer, BooleanFormula> asserts = null;
      try {
          asserts = parse(path);
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
      return new TubeState(edge, asserts,null,0,formulaManagerView);
  }
}

