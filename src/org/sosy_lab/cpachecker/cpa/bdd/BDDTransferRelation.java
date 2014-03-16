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
package org.sosy_lab.cpachecker.cpa.bdd;

import static org.sosy_lab.cpachecker.util.VariableClassification.FUNCTION_RETURN_VARIABLE;
import static org.sosy_lab.cpachecker.util.VariableClassification.isFunctionReturnVariable;
import static org.sosy_lab.cpachecker.util.VariableClassification.scopeVar;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.VariableClassification.Partition;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/** This Transfer Relation tracks variables and handles them as bitvectors. */
@Options(prefix = "cpa.bdd")
public class BDDTransferRelation extends ForwardingTransferRelation<BDDState, BDDPrecision> {

  @Option(name = "logfile", description = "Dump tracked variables to a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path dumpfile = Paths.get("BDDCPA_tracked_variables.log");

  @Option(description = "max bitsize for values and vars, initial value")
  private int bitsize = 64;

  @Option(description = "use a smaller bitsize for all vars, that have only intEqual values")
  private boolean compressIntEqual = true;

  private final LogManager logger;
  private final VariableClassification varClass;

  /** This map is used for scoping vars. It contains all used vars of a function. */
  private final Multimap<String, Region> functionToVars = LinkedHashMultimap.create();

  private final BitvectorManager bvmgr;
  private final NamedRegionManager rmgr;
  private final PredicateManager predmgr;
  private final MachineModel machineModel;

  /** The Constructor of BDDVectorTransferRelation sets the NamedRegionManager
   * and the BitVectorManager. Both are used to build and manipulate BDDs,
   * that represent the regions. */
  public BDDTransferRelation(NamedRegionManager manager, BitvectorManager bvmgr, LogManager pLogger,
                             Configuration config, CFA cfa, BDDPrecision precision)
          throws InvalidConfigurationException {
    config.inject(this);

    this.logger = pLogger;
    this.machineModel = cfa.getMachineModel();
    this.rmgr = manager;
    this.bvmgr = bvmgr;
    this.predmgr = new PredicateManager(config, manager, precision, cfa, machineModel);

    assert cfa.getVarClassification().isPresent();
    this.varClass = cfa.getVarClassification().get();
  }

  @Override
  protected Collection<BDDState> preCheck() {
    // no variables should be tracked
    if (precision.isDisabled()) {
      return Collections.singleton(state);
    }
    // the path is not fulfilled
    if (state.getRegion().isFalse()) {
      return Collections.emptyList();
    }
    return null;
  }

  /** This function handles statements like "a = 0;" and "b = !a;" and
   * calls of external functions. */
  @Override
  protected BDDState handleStatementEdge(final CStatementEdge cfaEdge, final CStatement statement) {

    BDDState result = state;

    // normal assignment, "a = ..."
    if (statement instanceof CAssignment) {
      result = handleAssignment((CAssignment) statement);

      // call of external function, "scanf(...)" without assignment
      // internal functioncalls are handled as FunctionCallEdges
    } else if (statement instanceof CFunctionCallStatement) {
      result = handleExternalFunctionCall(result,
              ((CFunctionCallStatement) statement).getFunctionCallExpression().getParameterExpressions());
    }

    assert !result.getRegion().isFalse();
    return result;
  }

  /** This function handles statements like "a = 0;" and "b = !a;".
   * A region is build for the right side of the statement.
   * Then this region is assigned to the variable at the left side.
   * This equality is added to the BDDstate to get the next state. */
  private BDDState handleAssignment(CAssignment assignment) {
    CExpression lhs = assignment.getLeftHandSide();

    if (!(lhs instanceof CIdExpression)) {
      return state;
    }

    final CType targetType = lhs.getExpressionType();
    final String varName = ((CIdExpression) lhs).getDeclaration().getQualifiedName();

    // next line is a shortcut, not necessary
    if (!precision.isTracking(varName)) {
      return state;
    }

    BDDState newState = state;
    CRightHandSide rhs = assignment.getRightHandSide();
    if (rhs instanceof CExpression) {
      final CExpression exp = (CExpression) rhs;
      final Partition partition = varClass.getPartitionForEdge(edge);

      if (isUsedInExpression(varName, exp)) {
        // make tmp for assignment,
        // this is done to handle assignments like "a = !a;" as "tmp = !a; a = tmp;"
        String tmpVarName = predmgr.getTmpVariableForVars(partition.getVars());
        final Region[] tmp = createPredicateWithoutPrecisionCheck(tmpVarName, getBitsize(partition, targetType));

        // make region for RIGHT SIDE and build equality of var and region
        final Region[] regRHS = evaluateVectorExpression(partition, exp, targetType);
        newState = newState.addAssignment(tmp, regRHS);

        // delete var, make tmp equal to (new) var, then delete tmp
        final Region[] var = createPredicate(lhs, getBitsize(partition, targetType));
        newState = newState.forget(var);
        newState = newState.addAssignment(var, tmp);
        newState = newState.forget(tmp);

      } else {
        final Region[] var = createPredicate(lhs, getBitsize(partition, targetType));
        newState = newState.forget(var);

        // make region for RIGHT SIDE and build equality of var and region
        final Region[] regRHS = evaluateVectorExpression(partition, (CExpression) rhs, targetType);
        newState = newState.addAssignment(var, regRHS);
      }
      return newState;

    } else if (rhs instanceof CFunctionCallExpression) {
      // handle params of functionCall, maybe there is a sideeffect
      newState = handleExternalFunctionCall(newState,
              ((CFunctionCallExpression) rhs).getParameterExpressions());

      // call of external function: we know nothing, so we delete the value of the var
      // TODO can we assume, that malloc returns something !=0?
      // are there some "save functions"?

      final Region[] var = createPredicate(lhs, bitsize); // is default bitsize enough?
      newState = newState.forget(var);

      return newState;

    } else {
      throw new AssertionError("unhandled assignment: " + edge.getRawStatement());
    }
  }

  /** This function deletes all vars, that could be modified
   * through a side-effect of the (external) functionCall. */
  private BDDState handleExternalFunctionCall(BDDState currentState, final List<CExpression> params) {

    for (final CExpression param : params) {

      /* special case: external functioncall with possible side-effect!
       * this is the only statement, where a pointer-operation is allowed
       * and the var can be boolean, intEqual or intAdd,
       * because we know, the variable can have a random (unknown) value after the functioncall.
       * example: "scanf("%d", &input);" */
      if (param instanceof CUnaryExpression &&
              UnaryOperator.AMPER == ((CUnaryExpression) param).getOperator() &&
              ((CUnaryExpression) param).getOperand() instanceof CIdExpression) {
        final CIdExpression id = (CIdExpression) ((CUnaryExpression) param).getOperand();
        final Region[] var = createPredicate(id, bitsize); // is default bitsize enough?
        currentState = currentState.forget(var);

      } else {
        // "printf("%d", output);" or "assert(exp);"
        // TODO: can we do something here?
      }
    }
    return state;
  }

  /** This function handles declarations like "int a = 0;" and "int b = !a;".
   * Regions are build for all Bits of the right side of the declaration,
   * if it is not null. Then these regions are assigned to the regions of
   * variable (bitvector) at the left side.
   * These equalities are added to the BDDstate to get the next state. */
  @Override
  protected BDDState handleDeclarationEdge(CDeclarationEdge cfaEdge, CDeclaration decl) {

    if (decl instanceof CVariableDeclaration) {
      CVariableDeclaration vdecl = (CVariableDeclaration) decl;
      CInitializer initializer = vdecl.getInitializer();

      CExpression init = null;
      if (initializer instanceof CInitializerExpression) {
        init = ((CInitializerExpression) initializer).getExpression();
      }

      // make variable (predicate) for LEFT SIDE of declaration,
      // delete variable, if it was initialized before i.e. in another block, with an existential operator
      Partition partition = varClass.getPartitionForEdge(cfaEdge);
      Region[] var = createPredicate(vdecl, getBitsize(partition, vdecl.getType()));
      BDDState newState = state.forget(var);

      // initializer on RIGHT SIDE available, make region for it
      if (init != null) {
        final Region[] rhs = evaluateVectorExpression(partition, init, vdecl.getType());
        newState = newState.addAssignment(var, rhs);
        return newState;
      }
    }

    return state; // if we know nothing, we return the old state
  }

  /** This function handles functioncalls like "f(x)", that calls "f(int a)".
   * Therefore each arg ("x") is transformed into a region and assigned
   * to a param ("int a") of the function. The equalities of
   * all arg-param-pairs are added to the BDDstate to get the next state. */
  @Override
  protected BDDState handleFunctionCallEdge(CFunctionCallEdge cfaEdge,
                                            List<CExpression> args, List<CParameterDeclaration> params, String calledFunction) {
    BDDState newState = state;

    // var_args cannot be handled: func(int x, ...) --> we only handle the first n parameters
    assert args.size() >= params.size();

    for (int i = 0; i < params.size(); i++) {

      // make variable (predicate) for param, this variable is not global
      final String varName = params.get(i).getQualifiedName();
      final CType targetType = params.get(i).getType();
      final Partition partition = varClass.getPartitionForEdge(cfaEdge, i);
      final Region[] var = createPredicate(varName, getBitsize(partition, targetType));
      final Region[] arg = evaluateVectorExpression(partition, args.get(i), targetType);
      newState = newState.addAssignment(var, arg);
    }

    return newState;
  }

  /** This function handles functionReturns like "y=f(x)".
   * The equality of the returnValue (FUNCTION_RETURN_VARIABLE) and the
   * left side ("y") is added to the new state.
   * Each variable from inside the function is removed from the BDDstate. */
  @Override
  protected BDDState handleFunctionReturnEdge(CFunctionReturnEdge cfaEdge,
                                              CFunctionSummaryEdge fnkCall, CFunctionCall summaryExpr, String outerFunctionName) {
    BDDState newState = state;

    // delete variables from returning function,
    // this results in a smaller BDD and allows to call a function twice.
    Collection<Region> innerVars = functionToVars.get(functionName);
    if (innerVars.size() > 0) {
      newState = newState.forget(innerVars.toArray(new Region[innerVars.size()]));
    }

    // set result of function equal to variable on left side
    final Partition partition = varClass.getPartitionForEdge(cfaEdge);

    // handle assignments like "y = f(x);"
    if (summaryExpr instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement cAssignment = (CFunctionCallAssignmentStatement) summaryExpr;
      CExpression lhs = cAssignment.getLeftHandSide();
      final int size = getBitsize(partition, lhs.getExpressionType());

      // make variable (predicate) for LEFT SIDE of assignment,
      // delete variable, if it was used before, this is done with an existential operator
      final Region[] var = createPredicate(lhs, size);
      newState = newState.forget(var);

      // make region (predicate) for RIGHT SIDE
      final Region[] retVar = createPredicate(scopeVar(functionName, FUNCTION_RETURN_VARIABLE), size);
      newState = newState.addAssignment(var, retVar);

      // LAST ACTION: delete varname of right side
      newState = newState.forget(retVar);

    } else if (summaryExpr instanceof CFunctionCallStatement) {
      // use default bitsize, there is no assignment
      final Region[] retVar = createPredicate(scopeVar(functionName, FUNCTION_RETURN_VARIABLE), bitsize);
      newState = newState.forget(retVar);

    } else {
      assert false;
    }

    return newState;
  }

  /** This function handles functionStatements like "return (x)".
   * The equality of the returnValue (FUNCTION_RETURN_VARIABLE) and the
   * evaluated right side ("x") is added to the new state. */
  @Override
  protected BDDState handleReturnStatementEdge(CReturnStatementEdge cfaEdge, CExpression rhs) {
    if (rhs != null) {

      BDDState newState = state;
      final Partition partition = varClass.getPartitionForEdge(cfaEdge);
      final CType functionReturnType = ((CFunctionDeclaration) cfaEdge.getSuccessor().getEntryNode()
              .getFunctionDefinition()).getType().getReturnType();

      // make region for RIGHT SIDE, this is the 'x' from 'return (x);
      final Region[] regRHS = evaluateVectorExpression(partition, rhs,
              functionReturnType);

      // make variable (predicate) for returnStatement,
      // delete variable, if it was used before, this is done with an existential operator
      final Region[] retvar = createPredicate(scopeVar(functionName, FUNCTION_RETURN_VARIABLE), getBitsize(partition, functionReturnType));
      newState = newState.forget(retvar);
      newState = newState.addAssignment(retvar, regRHS);

      return newState;
    }
    return state;
  }

  /** This function handles assumptions like "if(a==b)" and "if(a!=0)".
   * A region is build for the assumption.
   * This region is added to the BDDstate to get the next state.
   * If the next state is False, the assumption is not fulfilled.
   * In this case NULL is returned. */
  @Override
  protected BDDState handleAssumption(CAssumeEdge cfaEdge,
                                      CExpression expression, boolean truthAssumption) {

    Partition partition = varClass.getPartitionForEdge(cfaEdge);
    final Region[] operand = evaluateVectorExpression(partition, expression, CNumericTypes.INT);
    if (operand == null) {
      return state;
    } // assumption cannot be evaluated
    Region evaluated = bvmgr.makeOr(operand);

    if (!truthAssumption) { // if false-branch
      evaluated = rmgr.makeNot(evaluated);
    }

    // get information from region into evaluated region
    Region newRegion = rmgr.makeAnd(state.getRegion(), evaluated);
    if (newRegion.isFalse()) { // assumption is not fulfilled / not possible
      return null;
    } else {
      return new BDDState(rmgr, bvmgr, newRegion);
    }
  }

  /** This function returns a bitvector, that represents the expression.
   * The partition chooses the compression of the bitvector. */
  private @Nullable Region[] evaluateVectorExpression(
          final Partition partition, final CExpression exp, final CType targetType) {
    final boolean compress = (partition != null) && compressIntEqual
            && varClass.getIntEqualPartitions().contains(partition);
    if (varClass.getIntBoolPartitions().contains(partition)) {
      Region booleanResult = exp.accept(new BDDBooleanExpressionVisitor(this, rmgr));
      return (booleanResult == null) ? null : new Region[]{booleanResult};
    } else if (compress) {
      return exp.accept(new BDDCompressExpressionVisitor(this, bvmgr, partition));
    } else {
      Region[] value = exp.accept(new BDDVectorCExpressionVisitor(this, bvmgr, machineModel));
      if (value != null) {
        // cast to correct length
        final CType sourceType = exp.getExpressionType().getCanonicalType();
        value = bvmgr.toBitsize(
                machineModel.getSizeof(targetType) * machineModel.getSizeofCharInBits(),
                sourceType instanceof CSimpleType && ((CSimpleType) sourceType).isSigned(),
                value);
      }
      return value;
    }
  }

  /** This function returns, if the variable is used in the Expression. */
  private static boolean isUsedInExpression(String varName, CExpression exp) {
    return exp.accept(new VarCExpressionVisitor(varName));
  }

  /** This function returns regions containing bits of a variable.
   * returns regions for positions of a variable, s --> [s@2, s@1, s@0].
   * If the variable is not tracked by the the precision, Null is returned. */
  private Region[] createPredicateWithoutPrecisionCheck(final String varName, final int size) {
    final Region[] var = predmgr.createPredicates(varName, size);

    // track vars, so we can delete them after returning from a function,
    // see handleFunctionReturnEdge(...) for detail.
    if (!isFunctionReturnVariable(varName)) {
      for (Region v : var) {
        functionToVars.put(functionName, v);
      }
    }
    return var;
  }

  /** This function returns regions containing bits of a variable.
   * returns regions for positions of a variable, s --> [s@2, s@1, s@0].
   * If the variable is not tracked by the the precision, Null is returned. */
  private Region[] createPredicate(final String varName, final int size) {
    if (precision != null && !precision.isTracking(varName)) {
      return null;
    }
    return createPredicateWithoutPrecisionCheck(varName, size);
  }

  /** This function returns regions containing bits of a variable.
   * The name of the variable is build from given functionName and varName.
   * If the precision does not allow to track this variable, NULL is returned. */
  protected Region[] createPredicate(final CExpression exp, final int size) {
    final String var;
    if (exp instanceof CIdExpression) {
      var = ((CIdExpression) exp).getDeclaration().getQualifiedName();
    } else {
      var = exp.toASTString();
    }
    return createPredicate(var, size);
  }

  /** This function returns regions containing bits of a variable.
   * The name of the variable is build from functionName and varName.
   * If the precision does not allow to track this variable, NULL is returned. */
  private Region[] createPredicate(final CVariableDeclaration exp, final int size) {
    final String var = exp.getQualifiedName();
    return createPredicate(var, size);
  }

  /** This function returns the bitsize for vars of a partition.
   * For a boolean var the value is 1.
   *
   * Compression for IntEqual-vars:
   * For N different values (maybe plus 2 for additional Zero and One)
   * of M different variables there are N+M possible values for a variable
   * (one for each value and one for each (maybe uninitialized) variable).
   * For N+M different values we need at least log_2(N+M) bits in the representation.
   *
   * For other variables, the lengthof the CType is used. */
  protected int getBitsize(@Nullable Partition partition, @Nullable CType type) {
    if (partition == null) {
      // we know nothing about the partition, so do not track it with BDDCPA
      return 0;
    } else if (varClass.getIntBoolPartitions().contains(partition)) {
      return 1;
    } else if (compressIntEqual && varClass.getIntEqualPartitions().contains(partition)) {
      final Set<BigInteger> values = partition.getValues();
      int N = values.size();
      if (!values.contains(BigInteger.ZERO)) {
        N++;
      }
      if (!values.contains(BigInteger.ONE)) {
        N++;
      }
      int M = partition.getVars().size();
      return (int) Math.ceil(Math.log(N + M) / Math.log(2));
    } else {
      return machineModel.getSizeof(type) * machineModel.getSizeofCharInBits();
    }
  }

  /** This Visitor evaluates the visited expression and
   * returns iff the given variable is used in it. */
  private static class VarCExpressionVisitor extends DefaultCExpressionVisitor<Boolean, RuntimeException> {

    private String varName;

    VarCExpressionVisitor(String var) {
      this.varName = var;
    }

    private Boolean handle(CExpression exp) {
      return varName.equals(exp.toASTString());
    }

    @Override
    public Boolean visit(CArraySubscriptExpression exp) {
      return handle(exp);
    }

    @Override
    public Boolean visit(CBinaryExpression exp) {
      return exp.getOperand1().accept(this) || exp.getOperand2().accept(this);
    }

    @Override
    public Boolean visit(CCastExpression exp) {
      return exp.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CComplexCastExpression exp) {
      // TODO check if only the part of the operand should be evaluated which the
      // expression casts to
      return exp.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CFieldReference exp) {
      return handle(exp);
    }

    @Override
    public Boolean visit(CIdExpression exp) {
      return varName.equals(exp.getDeclaration().getQualifiedName());
    }

    @Override
    public Boolean visit(CUnaryExpression exp) {
      return exp.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CPointerExpression exp) {
      return exp.getOperand().accept(this);
    }

    @Override
    protected Boolean visitDefault(CExpression pExp) {
      return false;
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
          AbstractState state, List<AbstractState> states, CFAEdge cfaEdge,
          Precision precision) {
    // do nothing
    return null;
  }

  /** THis function writes some information about tracked variables, number of partitions,... */
  void printStatistics(final PrintStream out) {
    final Set<Partition> intBool = varClass.getIntBoolPartitions();
    int numOfBooleans = varClass.getIntBoolVars().size();

    int numOfIntEquals = 0;
    final Set<Partition> intEq = varClass.getIntEqualPartitions();
    for (Partition p : intEq) {
      numOfIntEquals += p.getVars().size();
    }

    int numOfIntAdds = 0;
    final Set<Partition> intAdd = varClass.getIntAddPartitions();
    for (Partition p : intAdd) {
      numOfIntAdds += p.getVars().size();
    }

    Collection<String> trackedIntBool = new TreeSet<>(); // TreeSet for nicer output through ordering
    Collection<String> trackedIntEq = new TreeSet<>();
    Collection<String> trackedIntAdd = new TreeSet<>();
    for (String var : predmgr.getTrackedVars()) {
      if (varClass.getIntBoolVars().contains(var)) {
        trackedIntBool.add(var);
      } else if (varClass.getIntEqualVars().contains(var)) {
        trackedIntEq.add(var);
      } else if (varClass.getIntAddVars().contains(var)) {
        trackedIntAdd.add(var);
      } else {
        // ignore other vars, they are either function_return_vars or tmp_vars
      }
    }

    if (dumpfile != null) { // option -noout
      try (Writer w = Files.openOutputFile(dumpfile)) {
        w.append("Boolean\n\n");
        w.append(trackedIntBool.toString());
        w.append("\n\nIntEq\n\n");
        w.append(trackedIntEq.toString());
        w.append("\n\nIntAdd\n\n");
        w.append(trackedIntAdd.toString());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write tracked variables for BDDCPA to file");
      }
    }

    out.println(String.format("Number of boolean vars:           %d (of %d)", trackedIntBool.size(), numOfBooleans));
    out.println(String.format("Number of intEqual vars:          %d (of %d)", trackedIntEq.size(), numOfIntEquals));
    out.println(String.format("Number of intAdd vars:            %d (of %d)", trackedIntAdd.size(), numOfIntAdds));
    out.println(String.format("Number of all vars:               %d (of %d)",
            trackedIntBool.size() + trackedIntEq.size() + trackedIntAdd.size(), varClass.getAllVars().size()));
    out.println("Number of intBool partitions:     " + intBool.size());
    out.println("Number of intEq partitions:       " + intEq.size());
    out.println("Number of intAdd partitions:      " + intAdd.size());
    out.println("Number of all partitions:         " + varClass.getPartitions().size());
    rmgr.printStatistics(out);
  }
}
