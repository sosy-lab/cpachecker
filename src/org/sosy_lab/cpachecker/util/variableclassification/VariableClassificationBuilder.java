// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.variableclassification;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.variableclassification.VariableAndFieldRelevancyComputer.VarFieldDependencies;

@Options(prefix = "cfa.variableClassification")
public class VariableClassificationBuilder implements StatisticsProvider {

  @Option(secure = true, name = "logfile", description = "Dump variable classification to a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path dumpfile = Path.of("VariableClassification.log");

  @Option(secure = true, description = "Dump variable type mapping to a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path typeMapFile = Path.of("VariableTypeMapping.txt");

  @Option(secure = true, description = "Dump domain type statistics to a CSV file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path domainTypeStatisticsFile = null;

  @Option(secure = true, description = "Print some information about the variable classification.")
  private boolean printStatsOnStartup = false;

  /**
   * Use {@link FunctionEntryNode#getReturnVariable()} and {@link AReturnStatement#asAssignment()}
   * instead.
   */
  @Deprecated public static final String FUNCTION_RETURN_VARIABLE = "__retval__";

  private static final String SCOPE_SEPARATOR = "::";

  private final Set<String> allVars = new HashSet<>();

  private final Set<String> nonIntBoolVars = new HashSet<>();
  private final Set<String> nonIntEqVars = new HashSet<>();
  private final Set<String> nonIntAddVars = new HashSet<>();
  private final Set<String> intOverflowVars = new HashSet<>();

  private final Dependencies dependencies = new Dependencies();

  private @Nullable ImmutableSet<String> relevantVariables;
  private @Nullable ImmutableMultimap<CCompositeType, String> relevantFields;
  private @Nullable ImmutableMultimap<CCompositeType, String> addressedFields;
  private @Nullable ImmutableSet<String> addressedVariables;

  private final LogManager logger;
  private final VariableClassificationStatistics stats = new VariableClassificationStatistics();

  public static class VariableClassificationStatistics implements Statistics {

    private final StatTimer variableClassificationTimer =
        new StatTimer("Time for classifying variables");
    private final StatTimer collectTimer = new StatTimer("Time for collecting variables");
    private final StatTimer dependencyTimer = new StatTimer("Time for solving dependencies");
    private final StatTimer hierarchyTimer = new StatTimer("Time for building hierarchy");
    private final StatTimer buildTimer = new StatTimer("Time for building classification");
    private final StatTimer exportTimer = new StatTimer("Time for exporting data");

    @Override
    public String getName() {
      return "";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
      if (variableClassificationTimer.getUpdateCount() > 0) {
        put(out, 3, variableClassificationTimer);
        put(out, 4, collectTimer);
        put(out, 4, dependencyTimer);
        put(out, 4, hierarchyTimer);
        put(out, 4, buildTimer);
        put(out, 4, exportTimer);
      }
    }
  }

  public VariableClassificationBuilder(Configuration config, LogManager pLogger)
      throws InvalidConfigurationException {
    logger = checkNotNull(pLogger);
    config.inject(this);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  /**
   * This function does the whole work: creating all maps, collecting vars, solving dependencies.
   * The function runs only once, after that it does nothing.
   */
  public VariableClassification build(CFA cfa) throws UnrecognizedCodeException {
    checkArgument(
        cfa.getLanguage() == Language.C, "VariableClassification currently only supports C");

    stats.variableClassificationTimer.start();
    // fill maps
    stats.collectTimer.start();
    collectVars(cfa);
    stats.collectTimer.stop();

    // if a value is not boolean, all dependent vars are not boolean and viceversa
    stats.dependencyTimer.start();
    dependencies.solve(nonIntBoolVars);
    dependencies.solve(nonIntEqVars);
    dependencies.solve(nonIntAddVars);
    dependencies.solve(intOverflowVars);
    stats.dependencyTimer.stop();

    // Now build the opposites of each non-x-vars-collection.
    // This is responsible for the hierarchy of the variables.
    final Set<String> intBoolVars = new HashSet<>();
    final Set<String> intEqualVars = new HashSet<>();
    final Set<String> intAddVars = new HashSet<>();
    final Set<Partition> intBoolPartitions = new HashSet<>();
    final Set<Partition> intEqualPartitions = new HashSet<>();
    final Set<Partition> intAddPartitions = new HashSet<>();

    stats.hierarchyTimer.start();
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
        intBoolPartitions.add(dependencies.getPartitionForVar(var));

      } else if (!nonIntEqVars.contains(var)) {
        intEqualVars.add(var);
        intEqualPartitions.add(dependencies.getPartitionForVar(var));

      } else if (!nonIntAddVars.contains(var)) {
        intAddVars.add(var);
        intAddPartitions.add(dependencies.getPartitionForVar(var));
      }
    }
    stats.hierarchyTimer.stop();

    // add last vars to dependencies,
    // this allows to get partitions for all vars,
    // otherwise only dependent vars are in the partitions
    for (String var : allVars) {
      dependencies.addVar(var);
    }

    boolean hasRelevantNonIntAddVars =
        !Sets.intersection(relevantVariables, nonIntAddVars).isEmpty();

    stats.buildTimer.start();
    VariableClassification result =
        new VariableClassification(
            hasRelevantNonIntAddVars,
            intBoolVars,
            intEqualVars,
            intAddVars,
            intOverflowVars,
            relevantVariables,
            addressedVariables,
            relevantFields,
            addressedFields,
            dependencies.partitions,
            intBoolPartitions,
            intEqualPartitions,
            intAddPartitions,
            dependencies.edgeToPartition,
            extractAssumedVariables(cfa.getAllNodes()),
            extractAssignedVariables(cfa.getAllNodes()));
    stats.buildTimer.stop();

    stats.exportTimer.start();
    if (printStatsOnStartup) {
      printStats(result);
    }

    if (dumpfile != null) { // option -noout
      try (Writer w = IO.openOutputFile(dumpfile, Charset.defaultCharset())) {
        w.append("IntBool\n\n");
        w.append(intBoolVars.toString());
        w.append("\n\nIntEq\n\n");
        w.append(intEqualVars.toString());
        w.append("\n\nIntAdd\n\n");
        w.append(intAddVars.toString());
        w.append("\n\nIntOverflow\n\n");
        w.append(intOverflowVars.toString());
        w.append("\n\nALL\n\n");
        w.append(allVars.toString());
        w.append("\n\nDEPENDENCIES\n\n");
        w.append(dependencies.toString());
        w.append("\n\nRELEVANT VARS\n\n");
        w.append(relevantVariables.toString());
        w.append("\n\nRELEVANT FIELDS\n\n");
        w.append(relevantFields.toString());
        w.append("\n");
      } catch (IOException e) {
        logger.logUserException(
            Level.WARNING, e, "Could not write variable classification to file");
      }
    }

    if (typeMapFile != null) {
      dumpVariableTypeMapping(typeMapFile, result);
    }

    if (domainTypeStatisticsFile != null) {
      dumpDomainTypeStatistics(domainTypeStatisticsFile, result);
    }
    stats.exportTimer.stop();
    stats.variableClassificationTimer.stop();

    return result;
  }

  private void dumpDomainTypeStatistics(Path pDomainTypeStatisticsFile, VariableClassification vc) {
    try (Writer w = IO.openOutputFile(pDomainTypeStatisticsFile, Charset.defaultCharset())) {
      Object[][] statMapping = {
        {"intBoolVars", vc.getIntBoolVars().size()},
        {"intEqualVars", vc.getIntEqualVars().size()},
        {"intAddVars", vc.getIntAddVars().size()},
        {"allVars", allVars.size()},
        {"intBoolVarsRelevant", countNumberOfRelevantVars(vc.getIntBoolVars())},
        {"intEqualVarsRelevant", countNumberOfRelevantVars(vc.getIntEqualVars())},
        {"intAddVarsRelevant", countNumberOfRelevantVars(vc.getIntAddVars())},
        {"allVarsRelevant", countNumberOfRelevantVars(allVars)}
      };
      // Write header
      for (int col = 0; col < statMapping.length; col++) {
        w.write(String.valueOf(statMapping[col][0]));
        if (col != statMapping.length - 1) {
          w.write("\t");
        }
      }
      w.write("\n");
      // Write data
      for (int col = 0; col < statMapping.length; col++) {
        w.write(String.valueOf(statMapping[col][1]));
        if (col != statMapping.length - 1) {
          w.write("\t");
        }
      }
      w.write("\n");
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING, e, "Could not write variable classification statistics to file");
    }
  }

  private void dumpVariableTypeMapping(Path target, VariableClassification vc) {
    try (Writer w = IO.openOutputFile(target, Charset.defaultCharset())) {
      for (String var : allVars) {
        int type = 0;
        if (vc.getIntBoolVars().contains(var)) {
          type += 1 + 2 + 4; // IntBool is subset of IntEqualBool and IntAddEqBool
        } else if (vc.getIntEqualVars().contains(var)) {
          type += 2 + 4; // IntEqual is subset of IntAddEqBool
        } else if (vc.getIntAddVars().contains(var)) {
          type += 4;
        }
        w.append(String.format("%s\t%d%n", var, type));
      }
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write variable type mapping to file");
    }
  }

  private void printStats(VariableClassification vc) {
    int numOfBooleans = 0;
    for (Partition p : vc.getIntEqualPartitions()) {
      numOfBooleans += p.getVars().size();
    }
    assert numOfBooleans == vc.getIntBoolVars().size();

    int numOfIntEquals = 0;
    for (Partition p : vc.getIntEqualPartitions()) {
      numOfIntEquals += p.getVars().size();
    }
    assert numOfIntEquals == vc.getIntEqualVars().size();

    int numOfIntAdds = 0;
    for (Partition p : vc.getIntAddPartitions()) {
      numOfIntAdds += p.getVars().size();
    }
    assert numOfIntAdds == vc.getIntAddVars().size();

    final String prefix = "\nVC ";
    StringBuilder str = new StringBuilder("VariableClassification Statistics\n");
    Joiner.on(prefix)
        .appendTo(
            str,
            new String[] {
              "---------------------------------",
              "number of boolean vars:  " + numOfBooleans,
              "number of intEq vars:    " + numOfIntEquals,
              "number of intAdd vars:   " + numOfIntAdds,
              "number of all vars:      " + allVars.size(),
              "number of rel. vars:     " + relevantVariables.size(),
              "number of addr. vars:    " + addressedVariables.size(),
              "number of rel. fields:   " + relevantFields.size(),
              "number of addr. fields:  " + addressedFields.size(),
              "number of intBool partitions:  " + vc.getIntBoolPartitions().size(),
              "number of intEq partitions:    " + vc.getIntEqualPartitions().size(),
              "number of intAdd partitions:   " + vc.getIntAddPartitions().size(),
              "number of all partitions:      " + dependencies.partitions.size(),
            });
    str.append("\n---------------------------------\n");

    logger.log(Level.INFO, str.toString());
  }

  private int countNumberOfRelevantVars(Set<String> ofVars) {
    return Sets.intersection(ofVars, relevantVariables).size();
  }

  /**
   * This function iterates over all edges of the cfa, collects all variables and orders them into
   * different sets, i.e. nonBoolean and nonIntEuqalNumber.
   */
  private void collectVars(CFA cfa) throws UnrecognizedCodeException {
    Collection<CFANode> nodes = cfa.getAllNodes();
    VarFieldDependencies varFieldDependencies = VarFieldDependencies.emptyDependencies();
    for (CFANode node : nodes) {
      for (CFAEdge edge : leavingEdges(node)) {
        handleEdge(edge, cfa);
        varFieldDependencies =
            varFieldDependencies.withDependencies(
                VariableAndFieldRelevancyComputer.handleEdge(cfa, edge));
      }
    }
    addressedVariables = varFieldDependencies.computeAddressedVariables();
    addressedFields = varFieldDependencies.computeAddressedFields();
    final Pair<ImmutableSet<String>, ImmutableMultimap<CCompositeType, String>> relevant =
        varFieldDependencies.computeRelevantVariablesAndFields();
    relevantVariables = relevant.getFirst();
    relevantFields = relevant.getSecond();
  }

  /**
   * This method extracts all variables (i.e., their qualified name), that occur in an assumption.
   */
  private Multiset<String> extractAssumedVariables(Collection<CFANode> nodes) {
    Multiset<String> assumeVariables = HashMultiset.create();

    for (CFANode node : nodes) {
      for (CAssumeEdge edge : Iterables.filter(leavingEdges(node), CAssumeEdge.class)) {
        assumeVariables.addAll(
            CFAUtils.getIdExpressionsOfExpression(edge.getExpression())
                .transform(id -> id.getDeclaration().getQualifiedName())
                .toSet());
      }
    }

    return assumeVariables;
  }

  /**
   * This method extracts all variables (i.e., their qualified name), that occur as left-hand side
   * in an assignment.
   */
  private Multiset<String> extractAssignedVariables(Collection<CFANode> nodes) {
    Multiset<String> assignedVariables = HashMultiset.create();

    for (CFANode node : nodes) {
      for (CFAEdge leavingEdge : leavingEdges(node)) {
        if (leavingEdge instanceof AStatementEdge) {
          AStatementEdge edge = (AStatementEdge) leavingEdge;
          if (!(edge.getStatement() instanceof CAssignment)) {
            continue;
          }

          CAssignment assignment = (CAssignment) edge.getStatement();
          assignedVariables.addAll(
              CFAUtils.getIdExpressionsOfExpression(assignment.getLeftHandSide())
                  .transform(id -> id.getDeclaration().getQualifiedName())
                  .toSet());
        }
      }
    }

    return assignedVariables;
  }

  /** switch to edgeType and handle all expressions, that could be part of the edge. */
  private void handleEdge(CFAEdge edge, CFA cfa) throws UnrecognizedCodeException {
    switch (edge.getEdgeType()) {
      case AssumeEdge:
        {
          CExpression exp = ((CAssumeEdge) edge).getExpression();
          CFANode pre = edge.getPredecessor();

          VariablesCollectingVisitor dcv = new VariablesCollectingVisitor(pre);
          Set<String> vars = exp.accept(dcv);
          if (vars != null) {
            allVars.addAll(vars);
            dependencies.addAll(vars, dcv.getValues(), edge, 0);
          }

          exp.accept(new BoolCollectingVisitor(pre, nonIntBoolVars));
          exp.accept(new IntEqualCollectingVisitor(pre, nonIntEqVars));
          exp.accept(new IntAddCollectingVisitor(pre, nonIntAddVars));
          exp.accept(new IntOverflowCollectingVisitor(pre, intOverflowVars));

          break;
        }

      case DeclarationEdge:
        {
          handleDeclarationEdge((CDeclarationEdge) edge);
          break;
        }

      case StatementEdge:
        {
          final CStatement statement = ((CStatementEdge) edge).getStatement();

          // normal assignment of variable, rightHandSide can be expression or (external)
          // functioncall
          if (statement instanceof CAssignment) {
            handleAssignment(edge, (CAssignment) statement, cfa);

            // pure external functioncall
          } else if (statement instanceof CFunctionCallStatement) {
            handleExternalFunctionCall(
                edge,
                ((CFunctionCallStatement) statement)
                    .getFunctionCallExpression()
                    .getParameterExpressions());
          }

          break;
        }

      case FunctionCallEdge:
        {
          handleFunctionCallEdge((CFunctionCallEdge) edge);
          break;
        }

      case FunctionReturnEdge:
        {
          Optional<CVariableDeclaration> returnVar =
              ((CFunctionReturnEdge) edge).getFunctionEntry().getReturnVariable();
          if (returnVar.isPresent()) {
            String scopedVarName = returnVar.orElseThrow().getQualifiedName();
            dependencies.addVar(scopedVarName);
            Partition partition = dependencies.getPartitionForVar(scopedVarName);
            partition.addEdge(edge, 0);
          }
          break;
        }

      case ReturnStatementEdge:
        {
          // this is the 'x' from 'return (x);
          // adding a new temporary FUNCTION_RETURN_VARIABLE, that is not global (-> false)
          CReturnStatementEdge returnStatement = (CReturnStatementEdge) edge;
          if (returnStatement.asAssignment().isPresent()) {
            handleAssignment(edge, returnStatement.asAssignment().orElseThrow(), cfa);
          }
          break;
        }

      case BlankEdge:
      case CallToReturnEdge:
        // other cases are not interesting
        break;

      default:
        throw new UnrecognizedCodeException("Unknown edgeType: " + edge.getEdgeType(), edge);
    }
  }

  /**
   * This function handles a declaration with an optional initializer. Only simple types are
   * handled.
   */
  private void handleDeclarationEdge(final CDeclarationEdge edge) {
    CDeclaration declaration = edge.getDeclaration();
    if (!(declaration instanceof CVariableDeclaration)) {
      return;
    }

    CVariableDeclaration vdecl = (CVariableDeclaration) declaration;
    String varName = vdecl.getQualifiedName();
    allVars.add(varName);

    // "connect" the edge with its partition
    Set<String> var = Sets.newHashSetWithExpectedSize(1);
    var.add(varName);
    dependencies.addAll(var, new HashSet<BigInteger>(), edge, 0);

    // only simple types (int, long) are allowed for booleans, ...
    if (!(vdecl.getType() instanceof CSimpleType)) {
      nonIntBoolVars.add(varName);
      nonIntEqVars.add(varName);
      nonIntAddVars.add(varName);
    }

    final CInitializer initializer = vdecl.getInitializer();

    if (!(initializer instanceof CInitializerExpression)) {
      return;
    }

    CExpression exp = ((CInitializerExpression) initializer).getExpression();
    if (exp == null) {
      return;
    }

    handleExpression(edge, exp, varName);
  }

  /** This function handles normal assignments of vars. */
  private void handleAssignment(final CFAEdge edge, final CAssignment assignment, final CFA cfa)
      throws UnrecognizedCodeException {
    CRightHandSide rhs = assignment.getRightHandSide();
    CExpression lhs = assignment.getLeftHandSide();
    String function = isGlobal(lhs) ? null : edge.getPredecessor().getFunctionName();

    // If we have a simple pointer, we handle it like a simple variable.
    // This allows us to track dependencies between simple references.
    String varName = scopeVar(function, lhs.toASTString());
    if (lhs instanceof CPointerExpression && lhs.getExpressionType() instanceof CSimpleType) {
      CExpression operand = ((CPointerExpression) lhs).getOperand();
      if (operand instanceof CIdExpression) {
        varName = scopeVar(function, operand.toASTString());
      }
    }

    // only simple types (int, long) are allowed for booleans, ...
    if (!(lhs instanceof CIdExpression && lhs.getExpressionType() instanceof CSimpleType)) {
      nonIntBoolVars.add(varName);
      nonIntEqVars.add(varName);
      nonIntAddVars.add(varName);
    }

    dependencies.addVar(varName);

    if (rhs instanceof CExpression) {
      handleExpression(edge, ((CExpression) rhs), varName);

    } else if (rhs instanceof CFunctionCallExpression) {
      // use FUNCTION_RETURN_VARIABLE for RIGHT SIDE
      CFunctionCallExpression func = (CFunctionCallExpression) rhs;
      String functionName = func.getFunctionNameExpression().toASTString(); // TODO correct?

      if (cfa.getAllFunctionNames().contains(functionName)) {
        Optional<? extends AVariableDeclaration> returnVariable =
            cfa.getFunctionHead(functionName).getReturnVariable();
        if (!returnVariable.isPresent()) {
          throw new UnrecognizedCodeException(
              "Void function " + functionName + " used in assignment", edge, assignment);
        }
        String returnVar = returnVariable.get().getQualifiedName();
        allVars.add(returnVar);
        allVars.add(varName);
        dependencies.add(returnVar, varName);

      } else {
        // external function
        Partition partition = dependencies.getPartitionForVar(varName);
        partition.addEdge(edge, -1); // negative value, because all positives are used for params
      }

      handleExternalFunctionCall(edge, func.getParameterExpressions());

    } else {
      throw new UnrecognizedCodeException("unhandled assignment", edge, assignment);
    }
  }

  /**
   * This function handles the call of an external function without an assignment of the result.
   * example: "printf("%d", output);" or "assert(exp);"
   */
  private void handleExternalFunctionCall(final CFAEdge edge, final List<CExpression> params) {
    for (int i = 0; i < params.size(); i++) {
      final CExpression param = params.get(i);

      /* special case: external functioncall with possible side-effect!
       * this is the only statement, where a pointer-operation is allowed
       * and the var can be boolean, intEqual or intAdd,
       * because we know, the variable can have a random (unknown) value after the functioncall.
       * example: "scanf("%d", &input);" */
      if (param instanceof CUnaryExpression
          && UnaryOperator.AMPER == ((CUnaryExpression) param).getOperator()
          && ((CUnaryExpression) param).getOperand() instanceof CIdExpression) {
        final CIdExpression id = (CIdExpression) ((CUnaryExpression) param).getOperand();
        final String varName = id.getDeclaration().getQualifiedName();

        dependencies.addVar(varName);
        Partition partition = dependencies.getPartitionForVar(varName);
        partition.addEdge(edge, i);

      } else {
        // "printf("%d", output);" or "assert(exp);"
        // TODO do we need the edge? ignore it?

        CFANode pre = edge.getPredecessor();
        VariablesCollectingVisitor dcv = new VariablesCollectingVisitor(pre);
        Set<String> vars = param.accept(dcv);
        if (vars != null) {
          allVars.addAll(vars);
          dependencies.addAll(vars, dcv.getValues(), edge, i);
        }

        param.accept(new BoolCollectingVisitor(pre, nonIntBoolVars));
        param.accept(new IntEqualCollectingVisitor(pre, nonIntEqVars));
        param.accept(new IntAddCollectingVisitor(pre, nonIntAddVars));
        param.accept(new IntOverflowCollectingVisitor(pre, intOverflowVars));
      }
    }
  }

  /**
   * This function puts each param in same partition than its arg. If there the functionresult is
   * assigned, it is also handled.
   */
  private void handleFunctionCallEdge(CFunctionCallEdge edge) {

    // overtake arguments from last functioncall into function,
    // get args from functioncall and make them equal with params from functionstart
    final List<CExpression> args = edge.getArguments();
    final List<CParameterDeclaration> params = edge.getSuccessor().getFunctionParameters();

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
      handleExpression(edge, args.get(i), varName, i);
    }

    // create dependency for functionreturn
    CFunctionSummaryEdge func = edge.getSummaryEdge();
    CFunctionCall statement = func.getExpression();
    Optional<CVariableDeclaration> returnVar = edge.getSuccessor().getReturnVariable();
    if (returnVar.isPresent()) {
      String scopedRetVal = returnVar.orElseThrow().getQualifiedName();
      if (statement instanceof CFunctionCallAssignmentStatement) {
        // a=f();
        CFunctionCallAssignmentStatement call = (CFunctionCallAssignmentStatement) statement;
        CExpression lhs = call.getLeftHandSide();
        String function = isGlobal(lhs) ? null : edge.getPredecessor().getFunctionName();
        String varName = scopeVar(function, lhs.toASTString());
        allVars.add(scopedRetVal);
        allVars.add(varName);
        dependencies.add(scopedRetVal, varName);
      } else if (statement instanceof CFunctionCallStatement) {
        // f(); without assignment
        // next line is not necessary, but we do it for completeness, TODO correct?
        dependencies.addVar(scopedRetVal);
      }
    }
  }

  /** evaluates an expression and adds containing vars to the sets. */
  private void handleExpression(CFAEdge edge, CExpression exp, String varName) {
    handleExpression(edge, exp, varName, 0);
  }

  /**
   * evaluates an expression and adds containing vars to the sets. the id is the position of the
   * expression in the edge, it is 0 for all edges except a FuntionCallEdge.
   */
  private void handleExpression(CFAEdge edge, CExpression exp, String varName, int id) {
    CFANode pre = edge.getPredecessor();

    VariablesCollectingVisitor dcv = new VariablesCollectingVisitor(pre);
    Set<String> vars = exp.accept(dcv);
    if (vars == null) {
      vars = Sets.newHashSetWithExpectedSize(1);
    }

    vars.add(varName);
    allVars.addAll(vars);
    dependencies.addAll(vars, dcv.getValues(), edge, id);

    BoolCollectingVisitor bcv = new BoolCollectingVisitor(pre, nonIntBoolVars);
    Set<String> possibleBoolean = exp.accept(bcv);
    handleResult(varName, possibleBoolean, nonIntBoolVars);

    IntEqualCollectingVisitor ncv = new IntEqualCollectingVisitor(pre, nonIntEqVars);
    Set<String> possibleIntEqualVars = exp.accept(ncv);
    handleResult(varName, possibleIntEqualVars, nonIntEqVars);

    IntAddCollectingVisitor icv = new IntAddCollectingVisitor(pre, nonIntAddVars);
    Set<String> possibleIntAddVars = exp.accept(icv);
    handleResult(varName, possibleIntAddVars, nonIntAddVars);

    IntOverflowCollectingVisitor iov = new IntOverflowCollectingVisitor(pre, intOverflowVars);
    Set<String> possibleIntOverflowVars = exp.accept(iov);
    handleResult(varName, possibleIntOverflowVars, intOverflowVars);
  }

  /** adds the variable to notPossibleVars, if possibleVars is null. */
  private void handleResult(
      String varName, Collection<String> possibleVars, Collection<String> notPossibleVars) {
    if (possibleVars == null) {
      notPossibleVars.add(varName);
    }
  }

  static String scopeVar(@Nullable final String function, final String var) {
    checkNotNull(var);
    return (function == null) ? var : (function + SCOPE_SEPARATOR + var);
  }

  static boolean isGlobal(CExpression exp) {
    if (checkNotNull(exp) instanceof CIdExpression) {
      CSimpleDeclaration decl = ((CIdExpression) exp).getDeclaration();
      if (decl instanceof CDeclaration) {
        return ((CDeclaration) decl).isGlobal();
      }
    }
    return false;
  }

  /** returns the value of a (nested) IntegerLiteralExpression or null for everything else. */
  public static BigInteger getNumber(CExpression exp) {
    checkNotNull(exp);
    if (exp instanceof CIntegerLiteralExpression) {
      return ((CIntegerLiteralExpression) exp).getValue();

    } else if (exp instanceof CUnaryExpression) {
      CUnaryExpression unExp = (CUnaryExpression) exp;
      BigInteger value = getNumber(unExp.getOperand());
      if (value == null) {
        return null;
      }
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
}
