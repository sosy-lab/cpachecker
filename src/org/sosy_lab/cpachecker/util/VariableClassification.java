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
package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpressionCollectorVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializers;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

@Options(prefix = "cfa.variableClassification")
public class VariableClassification {

  @Option(name = "logfile", description = "Dump variable classification to a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path dumpfile = Paths.get("VariableClassification.log");

  @Option(description = "Dump variable type mapping to a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path typeMapFile = Paths.get("VariableTypeMapping.txt");

  @Option(description = "Dump domain type statistics to a CSV file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path domainTypeStatisticsFile = null;

  @Option(description = "Print some information about the variable classification.")
  private boolean printStatsOnStartup = false;

  /** name for return-variables, it is used for function-returns. */
  public static final String FUNCTION_RETURN_VARIABLE = "__retval__";
  public static final String SCOPE_SEPARATOR = "::";

  /** normally a boolean value would be 0 or 1,
   * however there are cases, where the values are only 0 and 1,
   * but the variable is not boolean at all: "int x; if(x!=0 && x!= 1){}".
   * so we allow only 0 as boolean value, and not 1. */
  private boolean allowOneAsBooleanValue = false;
  private Timer buildTimer = new Timer();

  private Set<String> allVars = null;

  private Set<String> nonIntBoolVars;
  private Set<String> nonIntEqVars;
  private Set<String> nonIntAddVars;

  private Dependencies dependencies;

  private Set<String> intBoolVars;
  private Set<String> intEqualVars;
  private Set<String> intAddVars;

  private Set<String> loopExitConditionVariables;
  private Set<String> loopExitIncDecConditionVariables;

  /** These sets contain all variables even ones of array, pointer or structure types.
   *  Such variables cannot be classified even as Int, so they are only kept in these sets in order
   *  not to break the classification of Int variables.*/
  private Set<String> assignedVariables; // Variables used in the left hand side
  // Initially contains variables used in assumes and assigned to pointer dereferences,
  // then all essential variables (by propagation)
  private Set<String> relevantVariables;
  private Set<String> irrelevantVariables; // leftVariables without rightVariables
  private Set<String> addressedVariables;

  /** Fields information doesn't take any aliasing information into account,
   *  fields are considered per type, not per composite instance */
  private Multimap<CCompositeType, String> assignedFields; // Fields used in the left hand side
  // Initially contains fields used in assumes and assigned to pointer dereferences,
  // then all essential fields (by propagation)
  private Multimap<CCompositeType, String> relevantFields;
  private Multimap<CCompositeType, String> irrelevantFields; // leftFields without rightFields

  private Multimap<VariableOrField, VariableOrField> assignments; // Variables and fields used in the right hand side

  private Set<Partition> intBoolPartitions;
  private Set<Partition> intEqualPartitions;
  private Set<Partition> intAddPartitions;

  private CollectingLHSVisitor collectingLHSVisitor = null;
  private CollectingRHSVisitor collectingRHSVisitor = null;

  private final CFA cfa;
  private final ImmutableMultimap<String, Loop> loopStructure;
  private final LogManager logger;

  public VariableClassification(CFA cfa, Configuration config, LogManager pLogger,
      ImmutableMultimap<String, Loop> pLoopStructure) throws InvalidConfigurationException {
    checkArgument(cfa.getLanguage() == Language.C, "VariableClassification currently only supports C");
    config.inject(this);
    this.cfa = cfa;
    this.loopStructure = pLoopStructure;
    this.logger = pLogger;

    if (printStatsOnStartup) {
      build();
      printStats();
    }

  }

  private void printStats() {
    final Set<Partition> intBool = getIntBoolPartitions();
    int numOfBooleans = getIntBoolVars().size();

    int numOfIntEquals = 0;
    final Set<Partition> intEq = getIntEqualPartitions();
    for (Partition p : intEq) {
      numOfIntEquals += p.getVars().size();
    }

    int numOfIntAdds = 0;
    final Set<Partition> intAdd = getIntAddPartitions();
    for (Partition p : intAdd) {
      numOfIntAdds += p.getVars().size();
    }

    final String prefix = "\nVC ";
    StringBuilder str = new StringBuilder("VariableClassification Statistics\n");
    Joiner.on(prefix).appendTo(str, new String[] {
        "---------------------------------",
        "number of boolean vars:  " + numOfBooleans,
        "number of intEq vars:    " + numOfIntEquals,
        "number of intAdd vars:   " + numOfIntAdds,
        "number of all vars:      " + allVars.size(),
        "number of irrel. vars:   " + irrelevantVariables.size(),
        "number of addr. vars:    " + addressedVariables.size(),
        "number of irrel. fields: " + irrelevantFields.size(),
        "number of intBool partitions:  " + intBool.size(),
        "number of intEq partitions:    " + intEq.size(),
        "number of intAdd partitions:   " + intAdd.size(),
        "number of all partitions:      " + getPartitions().size(),
        "time for building classification: " + buildTimer });
    str.append("\n---------------------------------\n");

    logger.log(Level.INFO, str.toString());
  }

  /** This function does the whole work:
   * creating all maps, collecting vars, solving dependencies.
   * The function runs only once, after that it does nothing. */
  private void build() {
    if (allVars == null) {

      buildTimer.start();

      // init maps
      allVars = new HashSet<>();
      nonIntBoolVars = new HashSet<>();
      nonIntEqVars = new HashSet<>();
      nonIntAddVars = new HashSet<>();

      dependencies = new Dependencies();

      intBoolVars = new HashSet<>();
      intEqualVars = new HashSet<>();
      intAddVars = new HashSet<>();

      loopExitConditionVariables = new HashSet<>();
      loopExitIncDecConditionVariables = new HashSet<>();

      assignedVariables = new HashSet<>();
      relevantVariables = new HashSet<>();
      irrelevantVariables = new HashSet<>();
      addressedVariables = new HashSet<>();

      assignedFields = LinkedHashMultimap.create();
      relevantFields = LinkedHashMultimap.create();
      irrelevantFields = LinkedHashMultimap.create();

      assignments = LinkedHashMultimap.create();

      intBoolPartitions = new HashSet<>();
      intEqualPartitions = new HashSet<>();
      intAddPartitions = new HashSet<>();

      collectingLHSVisitor = new CollectingLHSVisitor();
      collectingRHSVisitor = new CollectingRHSVisitor();

      // fill maps
      collectVars();

      // we have collected the nonBooleanVars, lets build the needed booleanVars.
      buildOpposites();

      // collect loop condition variables
      collectLoopCondVars();

      // add last vars to dependencies,
      // this allows to get partitions for all vars,
      // otherwise only dependent vars are in the partitions
      for (String var : allVars) {
        dependencies.addVar(var);
      }

      buildTimer.stop();

      if (dumpfile != null) { // option -noout
        try (Writer w = Files.openOutputFile(dumpfile)) {
          w.append("IntBool\n\n");
          w.append(intBoolVars.toString());
          w.append("\n\nIntEq\n\n");
          w.append(intEqualVars.toString());
          w.append("\n\nIntAdd\n\n");
          w.append(intAddVars.toString());
          w.append("\n\nALL\n\n");
          w.append(allVars.toString());
          w.append("\n\nIRRELEVANT FIELDS\n\n");
          w.append(irrelevantFields.toString());
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Could not write variable classification to file");
        }
      }

      if (typeMapFile != null) {
        dumpVariableTypeMapping(typeMapFile);
      }

      if (domainTypeStatisticsFile != null) {
        dumpDomainTypeStatistics(domainTypeStatisticsFile);
      }
    }
  }

  private void dumpDomainTypeStatistics(Path pDomainTypeStatisticsFile) {
    try (Writer w = Files.openOutputFile(pDomainTypeStatisticsFile)) {
      try (PrintWriter p = new PrintWriter(w)) {
        Object[][] statMapping = {
              {"intBoolVars",           intBoolVars.size()},
              {"intEqualVars",          intEqualVars.size()},
              {"intAddVars",            intAddVars.size()},
              {"allVars",               allVars.size()},
              {"irrelevantFields",      irrelevantFields.size()},
              {"intBoolVarsRelevant",   countNumberOfRelevantVars(intBoolVars)},
              {"intEqualVarsRelevant",  countNumberOfRelevantVars(intEqualVars)},
              {"intAddVarsRelevant",    countNumberOfRelevantVars(intAddVars)},
              {"allVarsRelevant",       countNumberOfRelevantVars(allVars)}
        };
        // Write header
        for (int col=0; col<statMapping.length; col++) {
          p.print(statMapping[col][0]);
          if (col != statMapping.length-1) {
            p.print("\t");
          }
        }
        p.print("\n");
        // Write data
        for (int col=0; col<statMapping.length; col++) {
          p.print(statMapping[col][1]);
          if (col != statMapping.length-1) {
            p.print("\t");
          }
        }
        p.print("\n");
      }
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write variable classification statistics to file");
    }
  }

  public Set<String> getVariablesOfExpression(CExpression expr) {
    Set<String> result = new HashSet<>();
    CIdExpressionCollectorVisitor collector = new CIdExpressionCollectorVisitor();

    expr.accept(collector);

    for (CIdExpression id : collector.getReferencedIdExpressions()) {
      String assignToVar = scopeVar(id);
      result.add(assignToVar);
    }

    return result;
  }

  private void collectLoopCondVars() {
    for (Collection<Loop> localLoops : loopStructure.asMap().values()) {
      for (Loop l : localLoops) {
        // Get all variables that are used in exit-conditions
        for (CFAEdge e : l.getOutgoingEdges()) {
          if (e instanceof CAssumeEdge) {
            CExpression expr = ((CAssumeEdge) e).getExpression();
            loopExitConditionVariables.addAll(getVariablesOfExpression(expr));
          }
        }

        // Get all variables that are incremented or decrement by literal values
        for (CFAEdge e : l.getInnerLoopEdges()) {
          if (e instanceof CStatementEdge) {
            CStatementEdge stmtEdge = (CStatementEdge) e;
            if (stmtEdge.getStatement() instanceof CAssignment) {
              CAssignment assign = (CAssignment) stmtEdge.getStatement();
              if (assign.getLeftHandSide() instanceof CIdExpression) {
                CIdExpression assignementToId = (CIdExpression) assign.getLeftHandSide();
                String assignToVar = scopeVar(assignementToId);
                if (loopExitConditionVariables.contains(assignToVar)) {
                  if (assign.getRightHandSide() instanceof CBinaryExpression) {
                    CBinaryExpression binExpr = (CBinaryExpression) assign.getRightHandSide();
                    BinaryOperator op = binExpr.getOperator();
                    if (op == BinaryOperator.PLUS || op == BinaryOperator.MINUS) {
                      if (binExpr.getOperand1() instanceof CLiteralExpression
                          || binExpr.getOperand2() instanceof CLiteralExpression) {
                        CIdExpression operandId = null;
                        if (binExpr.getOperand1() instanceof CIdExpression) {
                          operandId = (CIdExpression) binExpr.getOperand1();
                        }
                        if (binExpr.getOperand2() instanceof CIdExpression) {
                          operandId = (CIdExpression) binExpr.getOperand2();
                        }
                        if (operandId != null) {
                          String operandVar = scopeVar(operandId);
                          if (assignToVar.equals(operandVar)) {
                            loopExitIncDecConditionVariables.add(assignToVar);
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  public void dumpVariableTypeMapping(Path target) {
    try (Writer w = Files.openOutputFile(target)) {
        for (String var : getAllVars()) {
          byte type = 0;
          if (getIntBoolVars().contains(var)) {
            type += 1 + 2 + 4; // IntBool is subset of IntEqualBool and IntAddEqBool
          } else if (getIntEqualVars().contains(var)) {
            type += 2 + 4; // IntEqual is subset of IntAddEqBool
          } else if (getIntAddVars().contains(var)) {
            type += 4;
          }
          if (loopExitConditionVariables.contains(var)) {
            type += 8;
          }
          if (loopExitIncDecConditionVariables.contains(var)) {
            type += 16;
          }
          w.append(String.format("%s\t%d%n", var, type));
      }
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write variable type mapping to file");
    }
  }

  /**
   * All variables, that may be assigned, but are not essential for reachability properties.
   * The variables are returned as a collection of scopedNames.
   * <p>
   * <strong>
   * Note: the collection includes all variables, including pointers, arrays and structures, i.e.
   *       non-Int variables.
   * </strong>
   * </p>
   */
  public Set<String> getIrrelevantVariables() {
    build();
    return irrelevantVariables;
  }

  private int countNumberOfRelevantVars(Collection<String> ofVars) {
    build();

    int result = 0;
    for (String var: ofVars) {
      if (relevantVariables.contains(var)) {
        result++;
      }
    }

    return result;
  }


  public boolean hasRelevantNonIntAddVars() {
    build();

    for (String var: nonIntAddVars) {
      if (relevantVariables.contains(var)) {
        return true;
      }
    }
    return false;
  }

  /**
   * All variables that may be essential for reachability properties.
   * The variables are returned as a collection of scopedNames.
   * <p>
   * <strong>
   * Note: the collection includes all variables, including pointers, arrays and structures, i.e.
   *       non-Int variables.
   * </strong>
   * </p>
   */
  public Set<String> getRelevantVariables() {
    build();
    return relevantVariables;
  }

  /**
   * All variables that have their addresses taken somewhere in the source code.
   * The variables are returned as a collection of scopedNames.
   * <p>
   * <strong>
   * Note: the collection includes all variables, including pointers, arrays and structures, i.e.
   *       non-Int variables.
   * </strong>
   * </p>
   */
  public Set<String> getAddressedVariables() {
    build();
    return addressedVariables;
  }

  /**
   * All fields that may be essential for reachability properties
   * (only fields accessed explicitly through either dot (.) or arrow (->) operator count).
   *
   * @return A collection of (CCompositeType, fieldName) mappings.
   */
  public Multimap<CCompositeType, String> getRelevantFields() {
    build();
    return relevantFields;
  }

  /**
   * All fields that are written somewhere
   * (explicitly with dot (.), arrow (->) operators or designated initializers), but
   * are not essential for reachability properties.
   *
   * @return A collection of (CCompositeType, fieldName) mappings.
   */
  public Multimap<CCompositeType, String> getIrrelevantFields() {
    build();
    return irrelevantFields;
  }

  /**
   * Possible loop variables of the program
   * in form of a collection of (functionName, varNames)
   */
  public Set<String> getLoopExitConditionVariables() {
    build();
    return loopExitConditionVariables;
  }

  /** This function returns a collection of scoped names.
   * This collection contains all vars. */
  public Set<String> getAllVars() {
    build();
    return allVars;
  }

  /** This function returns a collection of scoped names.
   * This collection contains all vars, that are boolean,
   * i.e. the value is 0 or 1. */
  public Set<String> getIntBoolVars() {
    build();
    return intBoolVars;
  }

  /** This function returns a collection of partitions.
   * Each partition contains only boolean vars. */
  public Set<Partition> getIntBoolPartitions() {
    build();
    return intBoolPartitions;
  }

  /** This function returns a collection of scoped names.
   * This collection contains all vars, that are only assigned or compared
   * for equality with integer values.
   * There are NO mathematical calculations (add, sub, mult) with these vars.
   * This collection does not contain any variable from "IntBool" or "IntAdd". */
  public Set<String> getIntEqualVars() {
    build();
    return intEqualVars;
  }

  /** This function returns a collection of partitions.
   * Each partition contains only vars,
   * that are only assigned or compared for equality with integer values.
   * This collection does not contains anypartition from "IntBool" or "IntAdd". */
  public Set<Partition> getIntEqualPartitions() {
    build();
    return intEqualPartitions;
  }

  /** This function returns a collection of scoped names.
   * This collection contains all vars, that are only used in simple calculations
   * (+, -, <, >, <=, >=, ==, !=, &, &&, |, ||, ^).
   * This collection does not contain any variable from "IntBool" or "IntEq". */
  public Set<String> getIntAddVars() {
    build();
    return intAddVars;
  }

  /** This function returns a collection of partitions.
   * Each partition contains only vars, that are used in simple calculations.
   * This collection does not contains anypartition from "IntBool" or "IntEq". */
  public Set<Partition> getIntAddPartitions() {
    build();
    return intAddPartitions;
  }

  /** This function returns a collection of partitions.
   * A partition contains all vars, that are dependent from each other. */
  public List<Partition> getPartitions() {
    build();
    return dependencies.getPartitions();
  }

  /** This function returns a partition containing all vars,
   * that are dependent with the given variable. */
  public Partition getPartitionForVar(String var) {
    build();
    return dependencies.getPartitionForVar(var);
  }

  /** This function returns a partition containing all vars,
   * that are dependent from a given CFAedge. */
  public Partition getPartitionForEdge(CFAEdge edge) {
    return getPartitionForEdge(edge, 0);
  }

  /** This function returns a partition containing all vars,
   * that are dependent from a given CFAedge.
   * The index is 0 for all edges, except functionCalls,
   * where it is the position of the param.
   * For the left-hand-side of the assignment of external functionCalls use -1. */
  public Partition getPartitionForEdge(CFAEdge edge, int index) {
    build();
    return dependencies.getPartitionForEdge(edge, index);
  }

  /** This function iterates over all edges of the cfa, collects all variables
   * and orders them into different sets, i.e. nonBoolean and nonIntEuqalNumber. */
  private void collectVars() {
    Collection<CFANode> nodes = cfa.getAllNodes();
    for (CFANode node : nodes) {
      for (CFAEdge edge : leavingEdges(node)) {
        handleEdge(edge);
      }
    }

    // if a value is not boolean, all dependent vars are not boolean and viceversa
    dependencies.solve(nonIntBoolVars);
    dependencies.solve(nonIntEqVars);
    dependencies.solve(nonIntAddVars);
  }

  /** This function builds the opposites of each non-x-vars-collection.
   * This method is responsible for the hierarchy of the variables. */
  private void buildOpposites() {
    for (final String var : allVars) {
        // we have this hierarchy of classes for variables:
        //        IntBool < IntEqBool < IntAddEqBool < AllInt
        // we define and build:
        //        IntBool = IntBool
        //        IntEq   = IntEqBool - IntBool
        //        IntAdd  = IntAddEqBool - IntEqBool
        //        Other   = IntAll - IntAddEqBool

        if (!nonIntBoolVars.contains(var)) {
          intBoolVars.add(var);
          intBoolPartitions.add(getPartitionForVar(var));

        } else if (!nonIntEqVars.contains(var)) {
          intEqualVars.add(var);
          intEqualPartitions.add(getPartitionForVar(var));

        } else if (!nonIntAddVars.contains(var)) {
          intAddVars.add(var);
          intAddPartitions.add(getPartitionForVar(var));
        }
    }

    // Propagate relevant variables from assumes and assignments to pointer dereferences to
    // other variables up to a fix-point (actually as the direction of dependency doesn't matter
    // it's just a BFS)
    Queue<VariableOrField> queue = new ArrayDeque<>(relevantVariables.size() + relevantFields.size());
    for (final String relevantVariable : relevantVariables) {
      queue.add(VariableOrField.newVariable(relevantVariable));
    }
    for (final Map.Entry<CCompositeType, String> relevantField : relevantFields.entries()) {
      queue.add(VariableOrField.newField(relevantField.getKey(), relevantField.getValue()));
    }
    while (!queue.isEmpty()) {
      final VariableOrField relevantVariableOrField = queue.poll();
      for (VariableOrField variableOrField : assignments.get(relevantVariableOrField)) {
        final VariableOrField.Variable variable = variableOrField.asVariable();
        final VariableOrField.Field field = variableOrField.asField();
        assert variable != null || field != null : "Sum type match failure: neither variable nor field!";
        if (variable != null && !relevantVariables.contains(variable.getScopedName())) {
          relevantVariables.add(variable.getScopedName());
          queue.add(variable);
        } else if (field != null && !relevantFields.containsEntry(field.getCompositeType(), field.getName())) {
          relevantFields.put(field.getCompositeType(), field.getName());
          queue.add(field);
        }
      }
    }

    // assignedFields without relevantFields
    for (final CCompositeType t : assignedFields.keySet()) {
      for (final String field : assignedFields.get(t)) {
        if (!relevantFields.containsEntry(t, field)) {
          irrelevantFields.put(t, field);
        }
      }
    }

    // we define: irrelevantVars == assignedVars without relevantVars
    for (final String var : assignedVariables) {
      if (!relevantVariables.contains(var)) {
        irrelevantVariables.add(var);
      }
    }
  }

  private static CCompositeType canonizeFieldOwnerType(CType fieldOwnerType) {
    fieldOwnerType = fieldOwnerType.getCanonicalType();

    if (fieldOwnerType instanceof CPointerType) {
      fieldOwnerType = ((CPointerType) fieldOwnerType).getType();
    }
    assert fieldOwnerType instanceof CCompositeType : "Field owner sould have composite type";
    final CCompositeType compositeType = (CCompositeType) fieldOwnerType;
    // Currently we don't pay attention to possible const and volatile modifiers
    if (compositeType.isConst() || compositeType.isVolatile()) {
      return new CCompositeType(false,
                                false,
                                compositeType.getKind(),
                                compositeType.getMembers(),
                                compositeType.getName());
    } else {
      return compositeType;
    }
  }

  /** switch to edgeType and handle all expressions, that could be part of the edge. */
  private void handleEdge(CFAEdge edge) {
    switch (edge.getEdgeType()) {

    case AssumeEdge: {
      CExpression exp = ((CAssumeEdge) edge).getExpression();
      CFANode pre = edge.getPredecessor();

      VariablesCollectingVisitor dcv = new VariablesCollectingVisitor(pre);
      Set<String> vars = exp.accept(dcv);
      if (vars != null) {
        dependencies.addAll(vars, dcv.getValues(), edge, 0);
      }

      exp.accept(new BoolCollectingVisitor(pre));
      exp.accept(new IntEqualCollectingVisitor(pre));
      exp.accept(new IntAddCollectingVisitor(pre));

      collectingRHSVisitor.setLHS(null);
      exp.accept(collectingRHSVisitor);
      break;
    }

    case DeclarationEdge: {
      handleDeclarationEdge((CDeclarationEdge) edge);
      break;
    }

    case StatementEdge: {
      final CStatement statement = ((CStatementEdge) edge).getStatement();

      // normal assignment of variable, rightHandSide can be expression or (external) functioncall
      if (statement instanceof CAssignment) {
        handleAssignment(edge, (CAssignment) statement);

        // pure external functioncall
      } else if (statement instanceof CFunctionCallStatement) {
        handleExternalFunctionCall(edge, ((CFunctionCallStatement) statement).
            getFunctionCallExpression().getParameterExpressions());
      }

      break;
    }

    case FunctionCallEdge: {
      handleFunctionCallEdge((CFunctionCallEdge) edge);
      break;
    }

    case FunctionReturnEdge: {
      String scopedVarName = edge.getPredecessor().getFunctionName() + SCOPE_SEPARATOR + FUNCTION_RETURN_VARIABLE;
      dependencies.addVar(scopedVarName);
      Partition partition = getPartitionForVar(scopedVarName);
      partition.addEdge(edge, 0);
      break;
    }

    case ReturnStatementEdge: {
      // this is the 'x' from 'return (x);
      // adding a new temporary FUNCTION_RETURN_VARIABLE, that is not global (-> false)
      CReturnStatementEdge returnStatement = (CReturnStatementEdge) edge;
      CExpression rhs = returnStatement.getExpression();
      if (rhs != null) {
        String function = edge.getPredecessor().getFunctionName();
        handleExpression(edge,
                         rhs,
                         scopeVar(function, FUNCTION_RETURN_VARIABLE),
                         VariableOrField.newVariable(scopeVar(function, FUNCTION_RETURN_VARIABLE)));
      }
      break;
    }

    case MultiEdge:
      for (CFAEdge innerEdge : (MultiEdge) edge) {
        handleEdge(innerEdge);
      }
      break;

    case BlankEdge:
    case CallToReturnEdge:
      // other cases are not interesting
      break;

    default:
      throw new AssertionError("Unknoewn edgeType: " + edge.getEdgeType());
    }
  }

  /** This function handles a declaration with an optional initializer.
   * Only simple types are handled. */
  private void handleDeclarationEdge(final CDeclarationEdge edge) {
    CDeclaration declaration = edge.getDeclaration();
    if (!(declaration instanceof CVariableDeclaration)) { return; }

    CVariableDeclaration vdecl = (CVariableDeclaration) declaration;
    String varName = vdecl.getQualifiedName();

    // "connect" the edge with its partition
    Set<String> var = new HashSet<>(1);
    var.add(varName);
    dependencies.addAll(var, new HashSet<BigInteger>(), edge, 0);

    // only simple types (int, long) are allowed for booleans, ...
    if (!(vdecl.getType() instanceof CSimpleType)) {
      nonIntBoolVars.add(varName);
      nonIntEqVars.add(varName);
      nonIntAddVars.add(varName);
    }

    final CInitializer initializer = vdecl.getInitializer();
    List<CExpressionAssignmentStatement> l;

    try {
      l = CInitializers.convertToAssignments(vdecl, edge);
    } catch (UnrecognizedCCodeException should_not_happen) {
      throw new AssertionError(should_not_happen);
    }

    for (CExpressionAssignmentStatement init : l) {
      final CLeftHandSide lhsExpression = init.getLeftHandSide();
      final VariableOrField lhs = lhsExpression.accept(collectingLHSVisitor);

      final CExpression rhs = init.getRightHandSide();
      collectingRHSVisitor.setLHS(lhs);
      rhs.accept(collectingRHSVisitor);
    }

    if ((initializer == null) || !(initializer instanceof CInitializerExpression)) { return; }

    CExpression exp = ((CInitializerExpression) initializer).getExpression();
    if (exp == null) { return; }

    handleExpression(edge, exp, varName, VariableOrField.newVariable(varName));
  }

  /** This function handles normal assignments of vars. */
  private void handleAssignment(final CFAEdge edge, final CAssignment assignment) {
    CRightHandSide rhs = assignment.getRightHandSide();
    CExpression lhs = assignment.getLeftHandSide();
    String function = isGlobal(lhs) ? null : edge.getPredecessor().getFunctionName();
    String varName = scopeVar(function, lhs.toASTString());

    // only simple types (int, long) are allowed for booleans, ...
    if (!(lhs instanceof CIdExpression && lhs.getExpressionType() instanceof CSimpleType)) {
      nonIntBoolVars.add(varName);
      nonIntEqVars.add(varName);
      nonIntAddVars.add(varName);
    }

    dependencies.addVar(varName);

    final VariableOrField lhsVariableOrField = lhs.accept(collectingLHSVisitor);

    if (rhs instanceof CExpression) {
      handleExpression(edge, ((CExpression) rhs), varName, lhsVariableOrField);

    } else if (rhs instanceof CFunctionCallExpression) {
      // use FUNCTION_RETURN_VARIABLE for RIGHT SIDE
      CFunctionCallExpression func = (CFunctionCallExpression) rhs;
      String functionName = func.getFunctionNameExpression().toASTString(); // TODO correct?

      if (cfa.getAllFunctionNames().contains(functionName)) {
        // TODO is this case really appearing or is it always handled as "functionCallEdge"?
        dependencies.add(scopeVar(functionName, FUNCTION_RETURN_VARIABLE), varName);

      } else {
        // external function
        Partition partition = getPartitionForVar(varName);
        partition.addEdge(edge, -1); // negative value, because all positives are used for params
      }

      handleExternalFunctionCall(edge, func.getParameterExpressions());

    } else {
      throw new AssertionError("unhandled assignment: " + edge.getRawStatement());
    }
  }

  /** This function handles the call of an external function
   * without an assignment of the result.
   * example: "printf("%d", output);" or "assert(exp);" */
  private void handleExternalFunctionCall(final CFAEdge edge, final List<CExpression> params) {
    for (int i = 0; i < params.size(); i++) {
      final CExpression param = params.get(i);

      /* special case: external functioncall with possible side-effect!
       * this is the only statement, where a pointer-operation is allowed
       * and the var can be boolean, intEqual or intAdd,
       * because we know, the variable can have a random (unknown) value after the functioncall.
       * example: "scanf("%d", &input);" */
      if (param instanceof CUnaryExpression &&
          UnaryOperator.AMPER == ((CUnaryExpression) param).getOperator() &&
          ((CUnaryExpression) param).getOperand() instanceof CIdExpression) {
        final CIdExpression id = (CIdExpression) ((CUnaryExpression) param).getOperand();
        final String varName = id.getDeclaration().getQualifiedName();

        dependencies.addVar(varName);
        Partition partition = getPartitionForVar(varName);
        partition.addEdge(edge, i);

      } else {
        // "printf("%d", output);" or "assert(exp);"
        // TODO do we need the edge? ignore it?

        CFANode pre = edge.getPredecessor();
        VariablesCollectingVisitor dcv = new VariablesCollectingVisitor(pre);
        Set<String> vars = param.accept(dcv);
        if (vars != null) {
          dependencies.addAll(vars, dcv.getValues(), edge, i);
        }

        param.accept(new BoolCollectingVisitor(pre));
        param.accept(new IntEqualCollectingVisitor(pre));
        param.accept(new IntAddCollectingVisitor(pre));
      }
    }
  }

  /** This function puts each param in same partition than its arg.
   * If there the functionresult is assigned, it is also handled. */
  private void handleFunctionCallEdge(CFunctionCallEdge edge) {

    // overtake arguments from last functioncall into function,
    // get args from functioncall and make them equal with params from functionstart
    final List<CExpression> args = edge.getArguments();
    final List<CParameterDeclaration> params = edge.getSuccessor().getFunctionParameters();
    final String innerFunctionName = edge.getSuccessor().getFunctionName();
    final String scopedRetVal = scopeVar(innerFunctionName, FUNCTION_RETURN_VARIABLE);

    // functions can have more args than params used in the call
    assert args.size() >= params.size();

    for (int i = 0; i < params.size(); i++) {
      CParameterDeclaration param = params.get(i);
      String varName = param.getQualifiedName();

      // only simple types (int, long) are allowed for booleans, ...
      if (!(param.getType() instanceof CSimpleType)) {
        nonIntBoolVars.add(varName);
        nonIntEqVars.add(varName);
        nonIntAddVars.add(varName);
      }

      // build name for param and evaluate it
      // this variable is not global (->false)
      handleExpression(edge, args.get(i), varName, i, VariableOrField.newVariable(varName));
    }

    // create dependency for functionreturn
    CFunctionSummaryEdge func = edge.getSummaryEdge();
    CFunctionCall statement = func.getExpression();

    // a=f();
    if (statement instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement call = (CFunctionCallAssignmentStatement) statement;
      CExpression lhs = call.getLeftHandSide();
      String function = isGlobal(lhs) ? null : edge.getPredecessor().getFunctionName();
      String varName = scopeVar(function, lhs.toASTString());
      dependencies.add(scopedRetVal, varName);

      final VariableOrField lhsVariableOrField = lhs.accept(collectingLHSVisitor);

      assignments.put(lhsVariableOrField, VariableOrField.newVariable(scopedRetVal));

      // f(); without assignment
    } else if (statement instanceof CFunctionCallStatement) {
      // next line is not necessary, but we do it for completeness, TODO correct?
      dependencies.addVar(scopedRetVal);
    }
  }

  /** evaluates an expression and adds containing vars to the sets. */
  private void handleExpression(CFAEdge edge,
                                CExpression exp,
                                String varName,
                                final VariableOrField lhs) {
    handleExpression(edge, exp, varName, 0, lhs);
  }

  /** evaluates an expression and adds containing vars to the sets.
   * the id is the position of the expression in the edge,
   * it is 0 for all edges except a FuntionCallEdge. */
  private void handleExpression(CFAEdge edge,
                                CExpression exp,
                                String varName,
                                int id,
                                final VariableOrField lhs) {
    CFANode pre = edge.getPredecessor();

    VariablesCollectingVisitor dcv = new VariablesCollectingVisitor(pre);
    Set<String> vars = exp.accept(dcv);
    if (vars == null) {
      vars = new HashSet<>(1);
    }

    vars.add(varName);
    dependencies.addAll(vars, dcv.getValues(), edge, id);

    BoolCollectingVisitor bcv = new BoolCollectingVisitor(pre);
    Set<String> possibleBoolean = exp.accept(bcv);
    handleResult(varName, possibleBoolean, nonIntBoolVars);

    IntEqualCollectingVisitor ncv = new IntEqualCollectingVisitor(pre);
    Set<String> possibleIntEqualVars = exp.accept(ncv);
    handleResult(varName, possibleIntEqualVars, nonIntEqVars);

    IntAddCollectingVisitor icv = new IntAddCollectingVisitor(pre);
    Set<String> possibleIntAddVars = exp.accept(icv);
    handleResult(varName, possibleIntAddVars, nonIntAddVars);

    collectingRHSVisitor.setLHS(lhs);
    exp.accept(collectingRHSVisitor);
  }

  /** adds the variable to notPossibleVars, if possibleVars is null.  */
  private void handleResult(String varName, Collection<String> possibleVars, Collection<String> notPossibleVars) {
    if (possibleVars == null) {
      notPossibleVars.add(varName);
    }
  }

  /** Returns a scoped name for a given IdExpression. */
  public static String scopeVar(final CExpression exp) {
    if(exp instanceof CIdExpression) {
      return ((CIdExpression) exp).getDeclaration().getQualifiedName();
    } else {
      return exp.toASTString();
    }
  }

  public static String scopeVar(@Nullable final String function, final String var) {
    return (function == null) ? (var) : (function + SCOPE_SEPARATOR + var);
  }

  public static boolean isGlobal(CExpression exp) {
    if (exp instanceof CIdExpression) {
      CSimpleDeclaration decl = ((CIdExpression) exp).getDeclaration();
      if (decl instanceof CDeclaration) { return ((CDeclaration) decl).isGlobal(); }
    }
    return false;
  }

  public static boolean isFunctionReturnVariable(final String var) {
    return var != null && var.endsWith(FUNCTION_RETURN_VARIABLE);
  }

  /** returns the value of a (nested) IntegerLiteralExpression
   * or null for everything else. */
  public static BigInteger getNumber(CExpression exp) {
    if (exp instanceof CIntegerLiteralExpression) {
      return ((CIntegerLiteralExpression) exp).getValue();

    } else if (exp instanceof CUnaryExpression) {
      CUnaryExpression unExp = (CUnaryExpression) exp;
      BigInteger value = getNumber(unExp.getOperand());
      if (value == null) { return null; }
      switch (unExp.getOperator()) {
      case MINUS:
        return value.negate();
      default:
        return null;
      }

    } else if (exp instanceof CCastExpression) {
      return getNumber(((CCastExpression) exp).getOperand());

    } else {
      return null;
    }
  }

  /** returns true, if the expression contains a casted binaryExpression. */
  private boolean isNestedBinaryExp(CExpression exp) {
    if (exp instanceof CBinaryExpression) {
      return true;

    } else if (exp instanceof CCastExpression) {
      return isNestedBinaryExp(((CCastExpression) exp).getOperand());

    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    build();

    StringBuilder str = new StringBuilder();
    str.append("\nALL  " + allVars.size() + "\n    " + allVars);
    str.append("\nIntBool  " + intBoolVars.size() + "\n    " + intBoolVars);
    str.append("\nIntEq  " + intEqualVars.size() + "\n    " + intEqualVars);
    str.append("\nIntAdd  " + intAddVars.size() + "\n    " + intAddVars);
    return str.toString();
  }

  /** This Visitor evaluates an Expression. It collects all variables.
   * a visit of IdExpression or CFieldReference returns a collection containing the varName,
   * other visits return the inner visit-results.
  * The Visitor also collects all numbers used in the expression. */
  private class VariablesCollectingVisitor implements
      CExpressionVisitor<Set<String>, RuntimeException> {

    private CFANode predecessor;
    private Set<BigInteger> values = new TreeSet<>();

    public VariablesCollectingVisitor(CFANode pre) {
      this.predecessor = pre;
    }

    public Set<BigInteger> getValues() {
      return values;
    }

    @Override
    public Set<String> visit(CArraySubscriptExpression exp) {
      return null;
    }

    @Override
    public Set<String> visit(CBinaryExpression exp) {

      // for numeral values
      BigInteger val1 = getNumber(exp.getOperand1());
      Set<String> operand1;
      if (val1 == null) {
        operand1 = exp.getOperand1().accept(this);
      } else {
        values.add(val1);
        operand1 = null;
      }

      // for numeral values
      BigInteger val2 = getNumber(exp.getOperand2());
      Set<String> operand2;
      if (val2 == null) {
        operand2 = exp.getOperand2().accept(this);
      } else {
        values.add(val2);
        operand2 = null;
      }

      // handle vars from operands
      if (operand1 == null) {
        return operand2;
      } else if (operand2 == null) {
        return operand1;
      } else {
        operand1.addAll(operand2);
        return operand1;
      }
    }

    @Override
    public Set<String> visit(CCastExpression exp) {
      BigInteger val = getNumber(exp.getOperand());
      if (val == null) {
        return exp.getOperand().accept(this);
      } else {
        values.add(val);
        return null;
      }
    }

    @Override
    public Set<String> visit(CComplexCastExpression exp) {
      // TODO complex numbers are not supported for evaluation right now, this
      // way of handling the variables my be wrong

      BigInteger val = getNumber(exp.getOperand());
      if (val == null) {
        return exp.getOperand().accept(this);
      } else {
        values.add(val);
        return null;
      }
    }

    @Override
    public Set<String> visit(CFieldReference exp) {
      String varName = exp.toASTString(); // TODO "(*p).x" vs "p->x"
      String function = isGlobal(exp) ? "" : predecessor.getFunctionName() + SCOPE_SEPARATOR;
      Set<String> ret = new HashSet<>(1);
      ret.add(function + varName);
      return ret;
    }

    @Override
    public Set<String> visit(CIdExpression exp) {
      Set<String> ret = new HashSet<>(1);
      ret.add(exp.getDeclaration().getQualifiedName());
      return ret;
    }

    @Override
    public Set<String> visit(CCharLiteralExpression exp) {
      return null;
    }

    @Override
    public Set<String> visit(CFloatLiteralExpression exp) {
      return null;
    }

    @Override
    public Set<String> visit(CImaginaryLiteralExpression exp) {
      return exp.getValue().accept(this);
    }

    @Override
    public Set<String> visit(CIntegerLiteralExpression exp) {
      values.add(exp.getValue());
      return null;
    }

    @Override
    public Set<String> visit(CStringLiteralExpression exp) {
      return null;
    }

    @Override
    public Set<String> visit(CTypeIdExpression exp) {
      return null;
    }

    @Override
    public Set<String> visit(CTypeIdInitializerExpression exp) {
      return null;
    }

    @Override
    public Set<String> visit(CUnaryExpression exp) {
      BigInteger val = getNumber(exp);
      if (val == null) {
        return exp.getOperand().accept(this);
      } else {
        values.add(val);
        return null;
      }
    }

    @Override
    public Set<String> visit(CPointerExpression exp) {
      BigInteger val = getNumber(exp);
      if (val == null) {
        return exp.getOperand().accept(this);
      } else {
        values.add(val);
        return null;
      }
    }
  }


  /** This Visitor evaluates an Expression. It also collects all variables.
   * Each visit-function returns
   * - null, if the expression is not boolean
   * - a collection, if the expression is boolean.
   * The collection contains all boolean vars. */
  private class BoolCollectingVisitor extends VariablesCollectingVisitor {

    public BoolCollectingVisitor(CFANode pre) {
      super(pre);
    }

    @Override
    public Set<String> visit(CFieldReference exp) {
      nonIntBoolVars.addAll(super.visit(exp));
      return null;
    }

    @Override
    public Set<String> visit(CBinaryExpression exp) {
      Set<String> operand1 = exp.getOperand1().accept(this);
      Set<String> operand2 = exp.getOperand2().accept(this);

      if (operand1 == null || operand2 == null) { // a+123 --> a is not boolean
        if (operand1 != null) {
          nonIntBoolVars.addAll(operand1);
        }
        if (operand2 != null) {
          nonIntBoolVars.addAll(operand2);
        }
        return null;
      }

      switch (exp.getOperator()) {

      case EQUALS:
      case NOT_EQUALS: // ==, != work with boolean operands
        if (operand1.isEmpty() || operand2.isEmpty()) {
          // one operand is Zero (or One, if allowed)
          operand1.addAll(operand2);
          return operand1;
        }
        // We compare 2 variables. There is no guarantee, that they are boolean!
        // Example: (a!=b) && (b!=c) && (c!=a)
        // -> FALSE for boolean, but TRUE for {1,2,3}

        //$FALL-THROUGH$

      default: // +-*/ --> no boolean operators, a+b --> a and b are not boolean
        nonIntBoolVars.addAll(operand1);
        nonIntBoolVars.addAll(operand2);
        return null;
      }
    }

    @Override
    public Set<String> visit(CIntegerLiteralExpression exp) {
      BigInteger value = exp.getValue();
      if (BigInteger.ZERO.equals(value)
          || (allowOneAsBooleanValue && BigInteger.ONE.equals(value))) {
        return new HashSet<>(0);
      } else {
        return null;
      }
    }

    @Override
    public Set<String> visit(CUnaryExpression exp) {
      Set<String> inner = exp.getOperand().accept(this);

      if (inner == null) {
        return null;
      } else { // PLUS, MINUS, etc --> not boolean
        nonIntBoolVars.addAll(inner);
        return null;
      }
    }

    @Override
    public Set<String> visit(CPointerExpression exp) {
      Set<String> inner = exp.getOperand().accept(this);

      if (inner == null) {
        return null;
      } else {
        nonIntBoolVars.addAll(inner);
        return null;
      }
    }
  }


  /** This Visitor evaluates an Expression.
   * Each visit-function returns
   * - null, if the expression contains calculations
   * - a collection, if the expression is a number, unaryExp, == or != */
  private class IntEqualCollectingVisitor extends VariablesCollectingVisitor {

    public IntEqualCollectingVisitor(CFANode pre) {
      super(pre);
    }

    @Override
    public Set<String> visit(CCastExpression exp) {
      BigInteger val = getNumber(exp.getOperand());
      if (val == null) {
        return exp.getOperand().accept(this);
      } else {
        return new HashSet<>(0);
      }
    }

    @Override
    public Set<String> visit(CFieldReference exp) {
      nonIntEqVars.addAll(super.visit(exp));
      return null;
    }

    @Override
    public Set<String> visit(CBinaryExpression exp) {

      // for numeral values
      BigInteger val1 = getNumber(exp.getOperand1());
      Set<String> operand1;
      if (val1 == null) {
        operand1 = exp.getOperand1().accept(this);
      } else {
        operand1 = new HashSet<>(0);
      }

      // for numeral values
      BigInteger val2 = getNumber(exp.getOperand2());
      Set<String> operand2;
      if (val2 == null) {
        operand2 = exp.getOperand2().accept(this);
      } else {
        operand2 = new HashSet<>(0);
      }

      // handle vars from operands
      if (operand1 == null || operand2 == null) { // a+0.2 --> no simple number
        if (operand1 != null) {
          nonIntEqVars.addAll(operand1);
        }
        if (operand2 != null) {
          nonIntEqVars.addAll(operand2);
        }
        return null;
      }

      switch (exp.getOperator()) {

      case EQUALS:
      case NOT_EQUALS: // ==, != work with numbers
        operand1.addAll(operand2);
        return operand1;

      default: // +-*/ --> no simple operators
        nonIntEqVars.addAll(operand1);
        nonIntEqVars.addAll(operand2);
        return null;
      }
    }

    @Override
    public Set<String> visit(CIntegerLiteralExpression exp) {
      return new HashSet<>(0);
    }

    @Override
    public Set<String> visit(CUnaryExpression exp) {

      // if exp is numeral
      BigInteger val = getNumber(exp);
      if (val != null) { return new HashSet<>(0); }

      // if exp is binary expression
      Set<String> inner = exp.getOperand().accept(this);
      if (isNestedBinaryExp(exp)) { return inner; }

      if (inner != null) {
        nonIntEqVars.addAll(inner);
      }
      return null;
    }

    @Override
    public Set<String> visit(CPointerExpression exp) {

      // if exp is numeral
      BigInteger val = getNumber(exp);
      if (val != null) { return new HashSet<>(0); }

      // if exp is binary expression
      Set<String> inner = exp.getOperand().accept(this);
      if (isNestedBinaryExp(exp)) { return inner; }

      // if exp is unknown
      if (inner == null) { return null; }

      nonIntEqVars.addAll(inner);
      return null;
    }
  }


  /** This Visitor evaluates an Expression.
   * Each visit-function returns
   * - a collection, if the expression is a var or a simple mathematical
   *   calculation (add, sub, <, >, <=, >=, ==, !=, !),
   * - else null */
  private class IntAddCollectingVisitor extends VariablesCollectingVisitor {

    public IntAddCollectingVisitor(CFANode pre) {
      super(pre);
    }

    @Override
    public Set<String> visit(CCastExpression exp) {
      return exp.getOperand().accept(this);
    }

    @Override
    public Set<String> visit(CFieldReference exp) {
      nonIntAddVars.addAll(super.visit(exp));
      return null;
    }

    @Override
    public Set<String> visit(CBinaryExpression exp) {
      Set<String> operand1 = exp.getOperand1().accept(this);
      Set<String> operand2 = exp.getOperand2().accept(this);

      if (operand1 == null || operand2 == null) { // a+0.2 --> no simple number
        if (operand1 != null) {
          nonIntAddVars.addAll(operand1);
        }
        if (operand2 != null) {
          nonIntAddVars.addAll(operand2);
        }
        return null;
      }

      switch (exp.getOperator()) {

      case PLUS:
      case MINUS:
      case LESS_THAN:
      case LESS_EQUAL:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case EQUALS:
      case NOT_EQUALS:
      case BINARY_AND:
      case BINARY_XOR:
      case BINARY_OR:
        // this calculations work with all numbers
        operand1.addAll(operand2);
        return operand1;

      default: // *, /, %, shift --> no simple calculations
        nonIntAddVars.addAll(operand1);
        nonIntAddVars.addAll(operand2);
        return null;
      }
    }

    @Override
    public Set<String> visit(CIntegerLiteralExpression exp) {
      return new HashSet<>(0);
    }

    @Override
    public Set<String> visit(CUnaryExpression exp) {
      Set<String> inner = exp.getOperand().accept(this);
      if (inner == null) { return null; }
      if (exp.getOperator() == UnaryOperator.MINUS) { return inner; }

      // *, ~, etc --> not simple
      nonIntAddVars.addAll(inner);
      return null;
    }

    @Override
    public Set<String> visit(CPointerExpression exp) {
      Set<String> inner = exp.getOperand().accept(this);
      if (inner == null) { return null; }

      nonIntAddVars.addAll(inner);
      return null;
    }
  }

  private class CollectingLHSVisitor extends DefaultCExpressionVisitor<VariableOrField, RuntimeException> {

    @Override
    public VariableOrField visit(final CArraySubscriptExpression e) {
      final VariableOrField result = e.getArrayExpression().accept(this);
      collectingRHSVisitor.setLHS(result);
      e.getSubscriptExpression().accept(collectingRHSVisitor);
      return result;
    }

    @Override
    public VariableOrField visit(final CFieldReference e) {
      final CCompositeType compositeType = canonizeFieldOwnerType(e.getFieldOwner().getExpressionType());
      assignedFields.put(compositeType, e.getFieldName());
      final VariableOrField result = VariableOrField.newField(compositeType, e.getFieldName());
      if (e.isPointerDereference()) {
        collectingRHSVisitor.setLHS(result);
        e.getFieldOwner().accept(collectingRHSVisitor);
      } else {
        e.getFieldOwner().accept(this);
      }
      return result;
    }

    @Override
    public VariableOrField visit(final CPointerExpression e) {
      collectingRHSVisitor.setLHS(null);
      e.getOperand().accept(collectingRHSVisitor);
      return null;
    }

    @Override
    public VariableOrField visit(final CComplexCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public VariableOrField visit(final CCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public VariableOrField visit(final CIdExpression e) {
      final VariableOrField.Variable result = VariableOrField.newVariable(e.getDeclaration().getQualifiedName());
      assignedVariables.add(result.getScopedName());
      return result;
    }

    @Override
    protected VariableOrField visitDefault(final CExpression e)  {
      throw new IllegalArgumentException("The expression should not occur in the left hand side");
    }
  }

  private void addVariableOrField(final @Nullable VariableOrField lhs, final VariableOrField rhs) {
    if (lhs != null) {
      assignments.put(lhs, rhs);
    } else {
      final VariableOrField.Variable variable = rhs.asVariable();
      final VariableOrField.Field field = rhs.asField();
      if (variable != null) {
        relevantVariables.add(variable.getScopedName());
      } else {
        relevantFields.put(field.getCompositeType(), field.getName());
      }
    }
  }

  private class CollectingRHSVisitor extends DefaultCExpressionVisitor<Void, RuntimeException> {

    public void setLHS(final VariableOrField lhs) {
      this.lhs = lhs;
    }

    public void setAddressed(final boolean addressed) {
      this.addressed = addressed;
    }

    @Override
    public Void visit(final CArraySubscriptExpression e) {
      e.getArrayExpression().accept(this);
      return e.getSubscriptExpression().accept(this);
    }

    @Override
    public Void visit(final CFieldReference e) {
      final CCompositeType compositeType = canonizeFieldOwnerType(e.getFieldOwner().getExpressionType());
      addVariableOrField(lhs, VariableOrField.newField(compositeType, e.getFieldName()));
      return e.getFieldOwner().accept(this);
    }

    @Override
    public Void visit(final CBinaryExpression e) {
      e.getOperand1().accept(this);
      return e.getOperand2().accept(this);
    }

    @Override
    public Void visit(final CUnaryExpression e) {
      if (e.getOperator() != UnaryOperator.AMPER) {
        return e.getOperand().accept(this);
      } else {
        setAddressed(true);
        e.getOperand().accept(this);
        setAddressed(false);
        return null;
      }
    }

    @Override
    public Void visit(final CPointerExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public Void visit(final CComplexCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public Void visit(final CCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public Void visit(final CIdExpression e) {
      final VariableOrField.Variable variable = VariableOrField.newVariable(e.getDeclaration().getQualifiedName());
      addVariableOrField(lhs, variable);
      if (addressed) {
        addressedVariables.add(variable.getScopedName());
      }
      return null;
    }

    @Override
    protected Void visitDefault(final CExpression e)  {
      return null;
    }

    VariableOrField lhs = null;
    boolean addressed = false;
  }

   /** A Partition is a Wrapper for a Collection of vars, values and edges.
   * The Partitions are disjunct, so no variable and no edge is in 2 Partitions. */
  public class Partition {

    private final Set<String> vars = new HashSet<>();
    private final Set<BigInteger> values = Sets.newTreeSet();
    private final Multimap<CFAEdge, Integer> edges = HashMultimap.create();

    private final Map<String, Partition> varToPartition;
    private final Map<Pair<CFAEdge, Integer>, Partition> edgeToPartition;

    public Partition(Map<String, Partition> varToPartition,
        Map<Pair<CFAEdge, Integer>, Partition> edgeToPartition) {
      this.varToPartition = varToPartition;
      this.edgeToPartition = edgeToPartition;
    }

    public Set<String> getVars() {
      return vars;
    }

    public Set<BigInteger> getValues() {
      return values;
    }

    public Multimap<CFAEdge, Integer> getEdges() {
      return edges;
    }

    /** adds the var to the partition and also to the global set of all vars. */
    public void add(String var) {
      vars.add(var);
      allVars.add(var);
      varToPartition.put(var, this);
    }

    public void addValues(Set<BigInteger> newValues) {
      values.addAll(newValues);
    }

    public void addEdge(CFAEdge edge, int index) {
      edges.put(edge, index);
      edgeToPartition.put(Pair.of(edge, index), this);
    }

    /** copies all data from other to current partition */
    public void merge(Partition other) {
      assert this.varToPartition == other.varToPartition;

      this.vars.addAll(other.vars);
      this.values.addAll(other.values);
      this.edges.putAll(other.edges);

      // update mapping of vars
      for (String var : other.vars) {
        varToPartition.put(var, this);
      }

      // update mapping of edges
      for (Entry<CFAEdge, Integer> edge : other.edges.entries()) {
        edgeToPartition.put(Pair.of(edge.getKey(), edge.getValue()), this);
      }
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof Partition) {
        Partition p = (Partition) other;
        return this.vars == p.vars;
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return vars.hashCode();
    }

    @Override
    public String toString() {
      return vars.toString() + " --> " + Arrays.toString(values.toArray());
    }
  }

  /** This class stores dependencies between variables.
   * It sorts vars into partitions.
   * Dependent vars are in the same partition. Partitions are independent. */
  private class Dependencies {

    /** partitions, each of them contains vars */
    private final List<Partition> partitions = Lists.newArrayList();

    /** map to get partition of a var */
    private final Map<String, Partition> varToPartition = Maps.newHashMap();

    /** table to get a partition for a edge. */
    private final Map<Pair<CFAEdge, Integer>, Partition> edgeToPartition = Maps.newHashMap();

    public List<Partition> getPartitions() {
      return partitions;
    }

    public Partition getPartitionForVar(String var) {
      return varToPartition.get(var);
    }

    public Partition getPartitionForEdge(CFAEdge edge, int index) {
      return edgeToPartition.get(Pair.of(edge, index));
    }

    /** This function creates a dependency between function1::var1 and function2::var2. */
    public void add(String var1, String var2) {

      // if both vars exists in some dependencies,
      // either ignore them or merge their partitions
      Partition partition1 = varToPartition.get(var1);
      Partition partition2 = varToPartition.get(var2);
      if (partition1 != null && partition2 != null) {

        // swap partitions, we create partitions in the order they are used
        if (partitions.lastIndexOf(partition1) > partitions.lastIndexOf(partition2)) {
          Partition tmp = partition2;
          partition2 = partition1;
          partition1 = tmp;
        }

        if (!partition1.equals(partition2)) {
          partition1.merge(partition2);
          partitions.remove(partition2);
        }

        // if only left side of dependency exists, add right side into same partition
      } else if (partition1 != null) {
        partition1.add(var2);

        // if only right side of dependency exists, add left side into same partition
      } else if (partition2 != null) {
        partition2.add(var1);

        // if none side is in any existing partition, create new partition
      } else {
        Partition partition = new Partition(varToPartition, edgeToPartition);
        partition.add(var1);
        partition.add(var2);
        partitions.add(partition);
      }
    }

    /** This function adds a group of vars to exactly one partition.
     * The values are stored in the partition.
     * The partition is "connected" with the expression.
     *
     * @param vars group of variables tobe added
     * @param values numbers, with are used in an expression together with the variables
     * @param edge where is the expression
     * @param index if an edge has several expressions, this index is the position ofthe expression
     *  */
    public void addAll(Collection<String> vars, Set<BigInteger> values,
        CFAEdge edge, int index) {
      if (vars == null || vars.isEmpty()) { return; }

      Iterator<String> iter = vars.iterator();

      // we use same varName for all other vars --> dependency
      String var = iter.next();

      // first add one single var
      addVar(var);

      // then add all other vars, they are dependent from the first var
      while (iter.hasNext()) {
        add(var, iter.next());
      }

      Partition partition = getPartitionForVar(var);
      partition.addValues(values);
      partition.addEdge(edge, index);
    }

    /** This function adds one single variable to the partitions.
     * This is the only method to create a partition with only one element. */
    public void addVar(String var) {

      // if var exists, we can ignore it, otherwise create new partition for var
      if (!varToPartition.containsKey(var)) {
        Partition partition = new Partition(varToPartition, edgeToPartition);
        partition.add(var);
        partitions.add(partition);
      }
    }

    /** This function adds all depending vars to the set, if necessary.
     * If A depends on B and A is part of the set, B is added to the set, and vice versa.
    * Example: If A is not boolean, B is not boolean. */
    public void solve(final Collection<String> vars) {
      for (Partition partition : partitions) {

        // is at least one var from the partition part of vars
        boolean isDependency = false;
        for (String var : partition.getVars()) {
          if (vars.contains(var)) {
            isDependency = true;
            break;
          }
        }

        // add all dependend vars to vars
        if (isDependency) {
          vars.addAll(partition.getVars());
        }
      }
    }

    @Override
    public String toString() {
      StringBuilder str = new StringBuilder("[");
      for (Partition partition : partitions) {
        str.append(partition.toString() + ",\n");
      }
      str.append("]\n\n");

      //      for (Pair<CFAEdge, Integer> edge : edgeToPartition.keySet()) {
      //        str.append(edge.getFirst().getRawStatement() + " :: "
      //            + edge.getSecond() + " --> " + edgeToPartition.get(edge) + "\n");
      //      }
      return str.toString();
    }
  }

  private static class VariableOrField {
    private static class Variable extends VariableOrField {
      private Variable(final @Nonnull String scopedName) {
        this.scopedName = scopedName;
      }

      public @Nonnull String getScopedName() {
        return scopedName;
      }

      @Override
      public String toString() {
        return getScopedName();
      }

      @Override
      public boolean equals(final Object o) {
        if (o == this) {
          return true;
        } else if (!(o instanceof Variable)) {
          return false;
        } else {
          final Variable other = (Variable) o;
          return this.scopedName.equals(other.scopedName);
        }
      }

      @Override
      public int hashCode() {
        return scopedName.hashCode();
      }

      private final @Nonnull String scopedName;
    }

    private static class Field extends VariableOrField {
      private Field(final CCompositeType composite, final String name) {
        this.composite = composite;
        this.name = name;
      }

      public CCompositeType getCompositeType() {
        return composite;
      }

      public String getName() {
        return name;
      }

      @Override
      public String toString() {
        return composite + SCOPE_SEPARATOR + name;
      }

      @Override
      public boolean equals(final Object o) {
        if (o == this) {
          return true;
        } else if (!(o instanceof Field)) {
          return false;
        } else {
          final Field other = (Field) o;
          return this.composite.equals(other.composite) && this.name.equals(other.name);
        }
      }

      @Override
      public int hashCode() {
        final int prime = 67;
        return prime * composite.hashCode() + name.hashCode();
      }

      private @Nonnull CCompositeType composite;
      private @Nonnull String name;
    }

    public static Variable newVariable(final String scopedName) {
      return new Variable(scopedName);
    }

    public static Field newField(final @Nonnull CCompositeType composite, final @Nonnull String name) {
      return new Field(composite, name);
    }

    public @Nullable Variable asVariable() {
      if (this instanceof Variable) {
        return (Variable) this;
      } else {
        return null;
      }
    }

    public @Nullable Field asField() {
      if (this instanceof Field) {
        return (Field) this;
      } else {
        return null;
      }
    }
  }
}
