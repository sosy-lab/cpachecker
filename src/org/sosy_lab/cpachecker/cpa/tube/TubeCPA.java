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
import java.util.logging.Level;
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
import org.sosy_lab.cpachecker.cfa.ast.c.*;
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
  /**
   *
   */

  /**
   * ObjectMapper is a class provided by the Jackson library that allows you to convert
   * between JSON and Java objects.
   */
  ObjectMapper om = new ObjectMapper();
  private final LogManager logger;

  private final FormulaManagerView formulaManagerView;
  /**
   * Represents a file path as a string.
   * This variable stores the path "C:\Users\ozkat\Desktop\jsonT.json".
   */
  String path = "C:\\Users\\ozkat\\Desktop/jsonT.json";
  /**
   * The formulaManager field is an instance of FormulaManagerView, which provides
   * a high-level interface for interacting with formulas.
   *
   * FormulaManagerView is a component of the TubeCPA class, which is used for
   * analyzing programs based on Counterexample Guided Abstraction Refinement (CEGAR).
   *
   * The formulaManager field is declared as private final, meaning it cannot be modified
   * or assigned a new value once initialized. The reference to the FormulaManagerView
   * instance it holds remains constant throughout the execution of the program.
   *
   * FormulaManagerView is used in various methods of the TubeCPA class, such as parsing
   * formulas from a JSON file, converting formula strings to BooleanFormulas, converting
   * CExpression statements and formula strings to a Formula object, and casting a Formula
   * to a BooleanFormula.
   *
   * For more details about the methods and functionality related to this field, refer to the
   * documentation of the TubeCPA class.
   */
  private final FormulaManagerView formulaManager;

  /**
   * The solver used for solving formulas and constraints.
   * This variable represents an instance of a solver class.
   */
  private final Solver solver;
  /**
   * Represents the Control Flow Automaton (CFA).
   */
  CFA cfa;

  /**
   * Represents a dummy CFA edge.
   */
  CFAEdge dummyEdge;
  /**
   * Builder for SSAMaps. Its state starts with an existing SSAMap, but may be changed later. It
   * supports read access, but it is not recommended to use instances of this class except for the
   * short period of time while creating a new SSAMap.
   *
   * <p>This class is not thread-safe.
   */
  SSAMapBuilder ssaMapBuilder = SSAMap.emptySSAMap().builder();
  /**
   * A variable of type PointerTargetSetBuilder.
   *
   * <p>
   * The PointerTargetSetBuilder class is responsible for building and managing a set of pointer targets.
   * </p>
   *
   * <p>
   * <b>Usage:</b>
   * </p>
   *
   * <pre>
   * PointerTargetSetBuilder ptsBuilder = DummyPointerTargetSetBuilder.INSTANCE;
   * </pre>
   */
  PointerTargetSetBuilder ptsBuilder = DummyPointerTargetSetBuilder.INSTANCE;

  /**
   * This variable represents a converter used for converting formulas in CtoFormulaConverter class.
   * It provides methods for parsing JSON formulas, parsing formula strings, converting CExpression statements and
   * formula strings to Formula objects, and casting a Formula to a BooleanFormula.
   *
   * @see TubeCPA#parseJsonFormula(String)
   * @see TubeCPA#parseFormula(Entry)
   * @see TubeCPA#convertToFormula(CExpression, String)
   * @see TubeCPA#castToBooleanFormula(Formula)
   */
  CtoFormulaConverter converter;
  /**
   * A handler for CtoFormulaType.
   *
   * This class is responsible for handling conversions between CtoFormulaType and other formula types.
   * It provides methods for parsing formula strings, converting statements and formula strings to Formula objects,
   * and casting Formula objects to BooleanFormulas.
   *
   * This class is part of the TubeCPA, a component used in the CPAchecker software.
   *
   * @see TubeCPA
   */
  CtoFormulaTypeHandler typeHandler;
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
            solver = Solver.create(config,pLogger,pShutdownNotifier);
            this.logger = pLogger;
            //Solver solver = Solver.create(Configuration.defaultConfiguration(), LogManager.createNullLogManager(),
            //ShutdownNotifier.createDummy());
            formulaManagerView = solver.getFormulaManager();
            formulaManager = solver.getFormulaManager();
            FormulaEncodingOptions options = new FormulaEncodingOptions(config);
            this.typeHandler = new CtoFormulaTypeHandler(pLogger, pCfa.getMachineModel());
            this.converter =
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
    public ImmutableMap<Integer, BooleanFormula> parseJsonFormula(String jsonFilePath) throws IOException {
        Builder<Integer, BooleanFormula> formulaMapBuilder = ImmutableMap.builder();
        try {
            JsonNode rootNode = om.readTree(new File(jsonFilePath));
            Iterator<Entry<String, JsonNode>> fields = rootNode.fields();
            while (fields.hasNext()) {
                Entry<String, JsonNode> fieldEntry = fields.next();
                BooleanFormula booleanFormula = parseFormula(fieldEntry);
                formulaMapBuilder.put(getLineNumber(fieldEntry), booleanFormula);
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
        return formulaMapBuilder.build();
    }

    /**
     * Returns the line number parsed from the given field entry.
     *
     * @param fieldEntry The entry containing the line number as the key.
     * @return The parsed line number as an integer.
     */
    private int getLineNumber(Entry<String, JsonNode> fieldEntry) {
        return Integer.parseInt(fieldEntry.getKey());
    }

    /**
     * Parses the given formula string and converts it to a {@link BooleanFormula} object.
     *
     * @param fieldEntry The entry containing the formula string to parse.
     * @return The parsed BooleanFormula object.
     * @throws InvalidAutomatonException If the automaton is invalid.
     * @throws InterruptedException     If the operation is interrupted.
     * @throws UnrecognizedCodeException If the code is unrecognized.
     */
    private BooleanFormula parseFormula(Entry<String, JsonNode> fieldEntry) throws InvalidAutomatonException, InterruptedException, UnrecognizedCodeException {
        String formulaString = fieldEntry.getValue().asText();
        //CExpression statements = (CExpression) CParserUtils.parseSingleStatement(formulaString, null, null);
        CStatement statement = CParserUtils.parseSingleStatement(formulaString, null, null);
        CExpression expression = getcExpression(statement);

        Formula formula = convertToFormula(expression, formulaString);
        return castToBooleanFormula(formula);
    }

    private static CExpression getcExpression(CStatement statement) {
        CExpression expression = null;
        if (statement instanceof CExpressionAssignmentStatement expressionAssignmentStatement) {
            expression = expressionAssignmentStatement.getLeftHandSide();
        }

        // Handle other statement types if needed...

        if (expression == null) {
            throw new IllegalArgumentException("Statement cannot be converted into CExpression. Invalid statement: " + statement);
        }
        return expression;
    }

    /**
     * Converts the given CExpression statements and formula string to a Formula object.
     *
     * @param statements    The CExpression statements to convert.
     * @param formulaString The formula string to convert.
     * @return The converted Formula object.
     * @throws InvalidAutomatonException   If the automaton is invalid.
     * @throws InterruptedException       If the operation is interrupted.
     * @throws UnrecognizedCodeException   If the code is unrecognized.
     */
    private Formula convertToFormula(CExpression statements, String formulaString) throws InvalidAutomatonException, InterruptedException, UnrecognizedCodeException {
        return (Formula) converter.buildTermForTubes(
                statements, dummyEdge, formulaString, ssaMapBuilder, ptsBuilder, new Constraints(formulaManager.getBooleanFormulaManager()), ErrorConditions.dummyInstance(formulaManager.getBooleanFormulaManager()));
    }

    /**
     * Casts a Formula to a BooleanFormula.
     *
     * @param formula The formula to cast.
     * @return The boolean formula.
     * @throws UnsupportedOperationException if the formula type is not supported.
     */
    private BooleanFormula castToBooleanFormula(Formula formula) {
        FormulaType<?> formulaType = formulaManager.getFormulaType((BitvectorFormula) formula);
        if (!formulaType.isBooleanType()) {
            throw new UnsupportedOperationException("Unsupported formula type: " + formulaType);
        }
        return (BooleanFormula) formula;
    }

  /**
   * This method returns a CPAFactory object for creating instances of TubeCPA.
   * The TubeCPAFactory is initialized with the AnalysisDirection.FORWARD value.
   *
   * @return the CPAFactory object
   */
  public static CPAFactory factory() {
    return new TubeCPAFactory(AnalysisDirection.FORWARD);
  }

  /**
   * Creates a new instance of TubeCPA.
   *
   * @param config             the configuration object
   * @param logger             the logger object
   * @param pShutdownNotifier  the shutdown notifier object
   * @param pCfa               the CFA object
   * @return a new instance of TubeCPA
   */
  public static TubeCPA create(Configuration config, LogManager logger, ShutdownNotifier pShutdownNotifier, CFA pCfa) {
    return new TubeCPA(config, logger, pShutdownNotifier, pCfa);
  }

  /**
   * Returns the initial state for the TubeCPA.
   *
   * <p>This method creates an initial state for the TubeCPA given a CFANode and a StateSpacePartition.
   * It retrieves the leaving edge from the CFANode, parses the JSON formula using the parseJsonFormula method,
   * and creates and returns a new TubeState with the retrieved edge, parsed asserts, null values for other parameters,
   * and the formula manager view.
   *
   * @param node      the CFANode from which the leaving edge is retrieved
   * @param partition the StateSpacePartition for which the initial state is created
   * @return the created initial state as an AbstractState
   * @throws InterruptedException if the operation is interrupted
   */
  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
          throws InterruptedException {
      CFAEdge edge = node.getLeavingEdge(0);
      ImmutableMap<Integer, BooleanFormula> localAsserts;
      try {
          localAsserts = parseJsonFormula(path);
      } catch (IOException e) {
          localAsserts = ImmutableMap.of();
          logger.log(Level.SEVERE, "An error occurred while parsing the JSON file", e);
      }
      return new TubeState(edge, localAsserts, null, false,0, formulaManagerView);
  }
}