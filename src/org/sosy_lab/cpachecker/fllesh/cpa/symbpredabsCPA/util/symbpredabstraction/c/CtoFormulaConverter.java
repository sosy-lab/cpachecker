/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.c;

import java.util.List;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionList;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTInitializerExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.GlobalDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.PathFormula;

/**
 * Class containing all the code that converts C code into a formula.
 */
public class CtoFormulaConverter {

  private final org.sosy_lab.cpachecker.util.symbpredabstraction.CtoFormulaConverter mInternalFC;

  //names for special variables needed to deal with functions
  public static final String NONDET_VARIABLE = org.sosy_lab.cpachecker.util.symbpredabstraction.CtoFormulaConverter.NONDET_VARIABLE;
  public static final String NONDET_FLAG_VARIABLE = NONDET_VARIABLE + "flag__";
  private static final String FUNCTION_PARAM_NAME = "__param__";
  
  private final SymbolicFormula mOne;
  
  public CtoFormulaConverter(Configuration config, SymbolicFormulaManager smgr, LogManager logger) throws InvalidConfigurationException {
    mInternalFC = new org.sosy_lab.cpachecker.util.symbpredabstraction.CtoFormulaConverter(config, smgr, logger);
    mOne = mInternalFC.getFormulaManager().makeNumber(1);
  }
  
  private SymbolicFormula makeNondetVariable(SSAMapBuilder pSSAMap) {
    int lIndex = pSSAMap.getIndex(NONDET_VARIABLE);
    
    if (lIndex < 0) {
      lIndex = 1;
    }
    
    lIndex++;
    
    // We have to keep nondet variable and its flag in sync.
    pSSAMap.setIndex(NONDET_VARIABLE, lIndex);
    pSSAMap.setIndex(NONDET_FLAG_VARIABLE, lIndex);
    
    return mInternalFC.getFormulaManager().makeVariable(NONDET_VARIABLE, lIndex);
  }
  
  /*
   * This method has to be called after makeNondetVariable
   */
  private SymbolicFormula makeNondetFlagVariable(SSAMapBuilder pSSAMap) {
    int lIndex = pSSAMap.getIndex(NONDET_FLAG_VARIABLE);
    
    if (lIndex < 0) {
      throw new RuntimeException();
    }
    
    return mInternalFC.getFormulaManager().makeVariable(NONDET_FLAG_VARIABLE, lIndex);
  }
  
  private SymbolicFormula makeVariable(String var, String function, SSAMapBuilder ssa) {
    if (mInternalFC.isNondetVariable(var)) {
      throw new RuntimeException();
    }
    
    var = mInternalFC.scoped(var, function);
    int idx = mInternalFC.getIndex(var, ssa);
    
    return mInternalFC.getFormulaManager().makeVariable(var, idx);
  }
  
  public Pair<SymbolicFormula, SymbolicFormula> readVariable(String pVariable, String pFunction, SSAMapBuilder pSSAMapBuilder) {
    if (mInternalFC.isNondetVariable(pVariable)) {
      SymbolicFormula lNondetVariable = makeNondetVariable(pSSAMapBuilder);
      SymbolicFormula lFlagVariable = makeNondetFlagVariable(pSSAMapBuilder);
      SymbolicFormula lAssignment = mInternalFC.getFormulaManager().makeAssignment(lFlagVariable, mOne);
      
      return new Pair<SymbolicFormula, SymbolicFormula>(lNondetVariable, lAssignment);
    }
    else {
      SymbolicFormula lVariableFormula = mInternalFC.getFormulaManager().makeVariable(mInternalFC.scoped(pVariable, pFunction), mInternalFC.getIndex(pVariable, pSSAMapBuilder));
      
      return new Pair<SymbolicFormula, SymbolicFormula>(lVariableFormula, mInternalFC.getFormulaManager().makeTrue());
    }
  }

//  @Override
  public PathFormula makeAnd(PathFormula pCurrentPathFormula, CFAEdge edge)
      throws CPATransferException {
    // this is where the "meat" is... We have to parse the statement
    // attached to the edge, and convert it to the appropriate formula

    if (!(edge.getPredecessor() instanceof FunctionDefinitionNode)
        && (edge.getEdgeType() == CFAEdgeType.BlankEdge)) {

      // in this case there's absolutely nothing to do, so take a shortcut
      return pCurrentPathFormula;
    }
    
    SymbolicFormula m = pCurrentPathFormula.getSymbolicFormula();

    String function = (edge.getPredecessor() != null) 
                          ? edge.getPredecessor().getFunctionName() : null;

    // copy SSAMap in all cases to ensure we never modify the old SSAMap accidentally
    SSAMapBuilder lSSAMapBuilder = pCurrentPathFormula.getSsa().builder();
    
    if (edge.getPredecessor() instanceof FunctionDefinitionNode) {
      // function start
      m = makeAndEnterFunction(m, (FunctionDefinitionNode)edge.getPredecessor(), function, lSSAMapBuilder);
    }

    SymbolicFormula f;
    switch (edge.getEdgeType()) {
    case StatementEdge: {
      StatementEdge statementEdge = (StatementEdge)edge;

      if (statementEdge.isJumpEdge()) {
        if (statementEdge.getSuccessor().getFunctionName().equals(
            "main")) {
          mInternalFC.getLogManager().log(Level.ALL, "DEBUG_3",
              "CtoFormulaConverter, IGNORING return ",
              "from main: ", edge.getRawStatement());
          f = m;
        } else {
          f = makeAndReturn(m, statementEdge, function, lSSAMapBuilder);
        }
      } else {
        f = makeAndStatement(m, statementEdge, function, lSSAMapBuilder);
      }
      break;
    }

    case DeclarationEdge: {
      f = makeAndDeclaration(m, (DeclarationEdge)edge, function, lSSAMapBuilder);
      break;
    }

    case AssumeEdge: {
      f = makeAndAssume(m, (AssumeEdge)edge, function, lSSAMapBuilder);
      break;
    }

    case BlankEdge: {
      f = m;
      break;
    }

    case FunctionCallEdge: {
      f = makeAndFunctionCall(m, (FunctionCallEdge)edge, function, lSSAMapBuilder);
      break;
    }

    case ReturnEdge: {
      // get the expression from the summary edge
      CFANode succ = edge.getSuccessor();
      CallToReturnEdge ce = succ.getEnteringSummaryEdge();
      f = makeAndExitFunction(m, ce, function, lSSAMapBuilder);
      break;
    }

    default:
      throw new UnrecognizedCFAEdgeException(edge);
    }

    // TODO replace with more meaningful initialization
    return new PathFormula(f, lSSAMapBuilder.build(), -1, mInternalFC.getFormulaManager().makeTrue(), -1);
  }

  private SymbolicFormula makeAndDeclaration(SymbolicFormula m1,
      DeclarationEdge declarationEdge, String function, SSAMapBuilder ssa)
      throws CPATransferException {

    IASTDeclarator[] decls = declarationEdge.getDeclarators();
    IASTDeclSpecifier spec = declarationEdge.getDeclSpecifier();

    boolean isGlobal = declarationEdge instanceof GlobalDeclarationEdge;

    if (spec instanceof IASTEnumerationSpecifier) {
      // extract the fields, and add them as global variables
      assert(isGlobal);
      IASTEnumerationSpecifier.IASTEnumerator[] enums =
        ((IASTEnumerationSpecifier)spec).getEnumerators();
      for (IASTEnumerationSpecifier.IASTEnumerator e : enums) {
        String var = e.getName().getRawSignature();
        mInternalFC.addToGlobalVars(var);
        IASTExpression exp = e.getValue();
        assert(exp != null);

        int idx = 1;
        ssa.setIndex(var, 1);

        mInternalFC.getLogManager().log(Level.ALL, "DEBUG_3",
            "Declared enum field: ", var, ", index: ", idx);

        SymbolicFormula minit = buildTerm(exp, function, ssa);
        SymbolicFormula mvar = mInternalFC.getFormulaManager().makeVariable(var, idx);
        SymbolicFormula t = mInternalFC.getFormulaManager().makeAssignment(mvar, minit);
        m1 = mInternalFC.getFormulaManager().makeAnd(m1, t);
      }
      return m1;
    }


    if (!(spec instanceof IASTSimpleDeclSpecifier ||
        spec instanceof IASTElaboratedTypeSpecifier ||
        spec instanceof IASTNamedTypeSpecifier)) {

      if (spec instanceof IASTCompositeTypeSpecifier) {
        // this is the declaration of a struct, just ignore it...
        mInternalFC.log(Level.ALL, "Ignoring declaration", spec);
        return m1;
      } else {
        throw new UnrecognizedCFAEdgeException(
            "UNSUPPORTED SPECIFIER FOR DECLARATION: " +
            declarationEdge.getRawStatement());
      }
    }

    if (spec.getStorageClass() == IASTDeclSpecifier.sc_typedef) {
      mInternalFC.log(Level.ALL, "Ignoring typedef", spec);
      return m1;
    }

    for (IASTDeclarator d : decls) {
      String var = d.getName().getRawSignature();
      if (isGlobal) {
        mInternalFC.addToGlobalVars(var);
      }
      var = mInternalFC.scoped(var, function);
      // assign new index to variable
      // (a declaration contains an implicit assignment, even without initializer)
      int idx = mInternalFC.makeLvalIndex(var, ssa);

      mInternalFC.getLogManager().log(Level.ALL, "DEBUG_3",
          "Declared variable: ", var, ", index: ", idx);
      // TODO get the type of the variable, and act accordingly

      // if the var is unsigned, add the constraint that it should
      // be > 0
//    if (((IASTSimpleDeclSpecifier)spec).isUnsigned()) {
//    long z = mathsat.api.msat_make_number(msatEnv, "0");
//    long mvar = buildMsatVariable(var, idx);
//    long t = mathsat.api.msat_make_gt(msatEnv, mvar, z);
//    t = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), t);
//    m1 = new MathsatSymbolicFormula(t);
//    }

      // if there is an initializer associated to this variable,
      // take it into account
      if (d.getInitializer() != null) {
        IASTInitializer init = d.getInitializer();
        if (!(init instanceof IASTInitializerExpression)) {
//        throw new UnrecognizedCFAEdgeException(
//        "BAD INITIALIZER: " + edge.getRawStatement());
          mInternalFC.log(Level.ALL, "Ingoring unsupported initializer", init);
          continue;
        }
        if (mInternalFC.isNondetVariable(var)) {
          mInternalFC.getLogManager().log(Level.WARNING, "Assignment to special non-determinism variable",
              var, "will be ignored.");
        }
        IASTExpression exp =
          ((IASTInitializerExpression)init).getExpression();
        SymbolicFormula minit = buildTerm(exp, function, ssa);
        SymbolicFormula mvar = mInternalFC.getFormulaManager().makeVariable(var, idx);
        SymbolicFormula t = mInternalFC.getFormulaManager().makeAssignment(mvar, minit);
        m1 = mInternalFC.getFormulaManager().makeAnd(m1, t);
      } else if (spec.getStorageClass() ==
        IASTDeclSpecifier.sc_extern) {
        mInternalFC.log(Level.ALL, "Ignoring initializer of extern declaration", d);
      } else if (isGlobal || mInternalFC.initializeAllVariables()) {
        // auto-initialize variables to zero, unless they match
        // the noAutoInitPrefix pattern
        if (!mInternalFC.isNondetVariable(d.getName().getRawSignature())) {  
          SymbolicFormula mvar = mInternalFC.getFormulaManager().makeVariable(var, idx);
          SymbolicFormula z = mInternalFC.getFormulaManager().makeNumber(0);
          SymbolicFormula t = mInternalFC.getFormulaManager().makeAssignment(mvar, z);
          m1 = mInternalFC.getFormulaManager().makeAnd(m1, t);
          mInternalFC.getLogManager().log(Level.ALL, "DEBUG_3", "AUTO-INITIALIZING",
              (isGlobal ? "GLOBAL" : ""), "VAR: ",
              var, " (", d.getName().getRawSignature(), ")");
        } else {
          mInternalFC.getLogManager().log(Level.ALL, "DEBUG_3",
              "NOT AUTO-INITIALIZING VAR:", var);
        }
      }
    }
    return m1;
  }

  private SymbolicFormula makeAndEnterFunction(SymbolicFormula m1,
      FunctionDefinitionNode fn, String function, SSAMapBuilder pSSAMapBuilder)
      throws UnrecognizedCFAEdgeException {
    List<IASTParameterDeclaration> params = fn.getFunctionParameters();
    if (params.isEmpty()) {
      return m1;
    }

    SymbolicFormula term = mInternalFC.getFormulaManager().makeTrue();
    int i = 0;
    for (IASTParameterDeclaration param : params) {
      String paramName = mInternalFC.scoped(FUNCTION_PARAM_NAME + (i++), function);
      int idx = mInternalFC.getIndex(paramName, pSSAMapBuilder);
      SymbolicFormula paramFormula = mInternalFC.getFormulaManager().makeVariable(paramName, idx);
      if (param.getDeclarator().getPointerOperators().length != 0) {
        mInternalFC.log(Level.WARNING, "Ignoring the semantics of pointer for parameter "
            + param.getDeclarator().getName(), fn.getFunctionDefinition().getDeclarator());
      }
      String pn = param.getDeclarator().getName().toString();
      if (pn.isEmpty()) {
        assert(param.getDeclarator().getNestedDeclarator() != null);
        pn = param.getDeclarator().getNestedDeclarator().getName().toString();
      }
      assert(!pn.isEmpty());
      String formalParamName = mInternalFC.scoped(pn, function);
      idx = mInternalFC.makeLvalIndex(formalParamName, pSSAMapBuilder);
      SymbolicFormula formalParam = mInternalFC.getFormulaManager().makeVariable(formalParamName, idx);
      SymbolicFormula eq = mInternalFC.getFormulaManager().makeAssignment(formalParam, paramFormula);
      term = mInternalFC.getFormulaManager().makeAnd(term, eq);
    }
    return mInternalFC.getFormulaManager().makeAnd(m1, term);
  }

  private SymbolicFormula makeAndExitFunction(SymbolicFormula m1,
      CallToReturnEdge ce, String function, SSAMapBuilder ssa)
      throws CPATransferException {
    IASTExpression retExp = ce.getExpression();
    if (retExp instanceof IASTFunctionCallExpression) {
      // this should be a void return, just do nothing...
      return m1;
    } else if (retExp instanceof IASTBinaryExpression) {
      IASTBinaryExpression exp = (IASTBinaryExpression)retExp;
      assert(exp.getOperator() == IASTBinaryExpression.op_assign);
      String retvar = mInternalFC.scoped(org.sosy_lab.cpachecker.util.symbpredabstraction.CtoFormulaConverter.VAR_RETURN_NAME, function);

      int retidx = mInternalFC.getIndex(retvar, ssa);
      SymbolicFormula retvarFormula = mInternalFC.getFormulaManager().makeVariable(retvar, retidx);
      IASTExpression e = exp.getOperand1();
      
      function = ce.getSuccessor().getFunctionName();
      SymbolicFormula outvarFormula = buildsatLvalueTerm(e, function, ssa);
      SymbolicFormula term = mInternalFC.getFormulaManager().makeAssignment(outvarFormula, retvarFormula);
      return mInternalFC.getFormulaManager().makeAnd(m1, term);
    } else {
      throw new UnrecognizedCFAEdgeException(
          "UNKNOWN FUNCTION EXIT EXPRESSION: " +
          ce.getRawStatement());
    }
  }

  private SymbolicFormula makeAndFunctionCall(SymbolicFormula m1,
      FunctionCallEdge edge, String function, SSAMapBuilder ssa)
      throws CPATransferException {
    if (edge.isExternalCall()) {
      throw new UnrecognizedCFAEdgeException(
          "EXTERNAL CALL UNSUPPORTED: " + edge.getRawStatement());
    } else {

      // build the actual parameters in the caller's context
      SymbolicFormula[] actualParamsFormulas;
      if (edge.getArguments() == null) {
        actualParamsFormulas = new SymbolicFormula[0];
      } else {
        actualParamsFormulas = new SymbolicFormula[edge.getArguments().length];
        IASTExpression[] actualParams = edge.getArguments();
        for (int i = 0; i < actualParamsFormulas.length; ++i) {
          actualParamsFormulas[i] = buildTerm(actualParams[i], function, ssa);
        }
      }
      // now switch to the context of the function
      FunctionDefinitionNode fn =
        (FunctionDefinitionNode)edge.getSuccessor();
      function = fn.getFunctionName();
      
      // create the symbolic vars for the formal parameters
      List<IASTParameterDeclaration> formalParams =
        fn.getFunctionParameters();
      assert(formalParams.size() == actualParamsFormulas.length);

      int i = 0;
      SymbolicFormula term = mInternalFC.getFormulaManager().makeTrue();
      for (IASTParameterDeclaration param : formalParams) {
        SymbolicFormula arg = actualParamsFormulas[i++];
        if (param.getDeclarator().getPointerOperators().length != 0) {
          mInternalFC.log(Level.WARNING, "Ignoring the semantics of pointer for parameter "
              + param.getDeclarator().getName(), fn.getFunctionDefinition().getDeclarator());
        }
        String paramName = mInternalFC.scoped(FUNCTION_PARAM_NAME + (i-1), function);
        int idx = mInternalFC.makeLvalIndex(paramName, ssa);
        SymbolicFormula paramFormula = mInternalFC.getFormulaManager().makeVariable(paramName, idx);
        SymbolicFormula eq = mInternalFC.getFormulaManager().makeAssignment(paramFormula, arg);
        term = mInternalFC.getFormulaManager().makeAnd(term, eq);
      }
      return mInternalFC.getFormulaManager().makeAnd(m1, term);
    }
  }

  private SymbolicFormula makeAndReturn(SymbolicFormula m1, StatementEdge edge,
      String function, SSAMapBuilder ssa)
      throws CPATransferException {
    IASTExpression exp = edge.getExpression();
    if (exp == null) {
      // this is a return from a void function, do nothing
      return m1;
    } else if (exp instanceof IASTUnaryExpression) {
      
      // we have to save the information about the return value,
      // so that we can use it later on, if it is assigned to
      // a variable. We create a function::__retval__ variable
      // that will hold the return value
      String retvalname = mInternalFC.scoped(org.sosy_lab.cpachecker.util.symbpredabstraction.CtoFormulaConverter.VAR_RETURN_NAME, function);
      int idx = mInternalFC.makeLvalIndex(retvalname, ssa);

      SymbolicFormula retvar = mInternalFC.getFormulaManager().makeVariable(retvalname, idx);
      SymbolicFormula retval = buildTerm(exp, function, ssa);
      SymbolicFormula term = mInternalFC.getFormulaManager().makeAssignment(retvar, retval);
      return mInternalFC.getFormulaManager().makeAnd(m1, term);
    }
    // if we are here, we can't handle the return properly...
    throw new UnrecognizedCFAEdgeException(edge);
  }

  private SymbolicFormula makeAndStatement(SymbolicFormula f1, StatementEdge stmt,
      String function, SSAMapBuilder ssa) throws CPATransferException {
    IASTExpression expr = stmt.getExpression();

    SymbolicFormula f2 = null;
    
    // check for nondet special case
    if (expr instanceof IASTBinaryExpression) {
      IASTBinaryExpression lBinaryExpression = (IASTBinaryExpression)expr;
      
      if (lBinaryExpression.getOperator() == IASTBinaryExpression.op_assign) {
        IASTExpression lOperand1Expression = ((IASTBinaryExpression)expr).getOperand1();
        IASTExpression lOperand2Expression = ((IASTBinaryExpression)expr).getOperand2();
        
        if (lOperand1Expression instanceof IASTIdExpression 
            && lOperand2Expression instanceof IASTIdExpression) {
          IASTIdExpression lRVariable = (IASTIdExpression)lOperand2Expression;
          String lRVariableName = lRVariable.getName().getRawSignature();
          
          if (mInternalFC.isNondetVariable(lRVariableName)) {
            // special case handling
            
            SymbolicFormula lNondetVariable = makeNondetVariable(ssa);
            SymbolicFormula lLValueVariable = buildsatLvalueTerm(lOperand1Expression, function, ssa);
            
            f2 = mInternalFC.getFormulaManager().makeAssignment(lLValueVariable, lNondetVariable);
            
            SymbolicFormula lNondetFlagVariable = makeNondetFlagVariable(ssa);
            SymbolicFormula lAssignment = mInternalFC.getFormulaManager().makeAssignment(lNondetFlagVariable, mOne);
            
            f2 = mInternalFC.getFormulaManager().makeAnd(f2, lAssignment);
          }
        }
      }
    }
    
    if (f2 == null) {
      f2 = buildTerm(expr, function, ssa);
    }

    //SymbolicFormula d = mathsat.api.msat_term_get_decl(f2);
    //if (mathsat.api.msat_decl_get_return_type(d) != mathsat.api.MSAT_BOOL) {
    if (!mInternalFC.getFormulaManager().isBoolean(f2)) {
      // in this case, we have something like:
        // f(x);
      // i.e. an expression that gets assigned to nothing. Since
      // we don't handle side-effects, this means that the
      // expression has no effect, and we can just drop it
      mInternalFC.log(Level.INFO, "Statement is assumed to be side-effect free, but its return value is not used",
          stmt.getExpression());
      return f1;
    }
    return mInternalFC.getFormulaManager().makeAnd(f1, f2);
  }

  private SymbolicFormula makeAndAssume(SymbolicFormula f1,
      AssumeEdge assume, String function, SSAMapBuilder ssa) throws CPATransferException {
    SymbolicFormula f2;
    if (assume.getTruthAssumption()) {
      f2 = makePredicate(assume.getExpression(), function, ssa);
    }
    else {
      f2 = makeNegativePredicate(assume.getExpression(), function, ssa);
    }

    return mInternalFC.getFormulaManager().makeAnd(f1, f2);
  }
  
  private SymbolicFormula buildIdExpression(IASTIdExpression pExpression, String pFunction, SSAMapBuilder pSSAMapBuilder) {
    // this is a variable: get the right index for the SSA
    String lVariable = pExpression.getName().getRawSignature();
    return makeVariable(lVariable, pFunction, pSSAMapBuilder);
  }
  
  private SymbolicFormula buildTerm(IASTExpression exp, String function, SSAMapBuilder ssa)
        throws UnrecognizedCCodeException {
    if (exp instanceof IASTIdExpression) {
      return buildIdExpression((IASTIdExpression)exp, function, ssa);
    } else if (exp instanceof IASTLiteralExpression) {
      return mInternalFC.buildLiteralExpression((IASTLiteralExpression)exp);
    } else if (exp instanceof IASTCastExpression) {
      // we completely ignore type casts
      mInternalFC.getLogManager().log(Level.ALL, "DEBUG_3", "IGNORING TYPE CAST:",
          exp.getRawSignature());
      return buildTerm(((IASTCastExpression)exp).getOperand(), function, ssa);
    } else if (exp instanceof IASTUnaryExpression) {
      IASTExpression operand = ((IASTUnaryExpression)exp).getOperand();
      int op = ((IASTUnaryExpression)exp).getOperator();
      switch (op) {
      case IASTUnaryExpression.op_bracketedPrimary:
        return buildTerm(operand, function, ssa);
      case IASTUnaryExpression.op_postFixIncr:
      case IASTUnaryExpression.op_prefixIncr:
      case IASTUnaryExpression.op_postFixDecr:
      case IASTUnaryExpression.op_prefixDecr: {
        SymbolicFormula mvar = buildTerm(operand, function, ssa);
        SymbolicFormula newvar = buildsatLvalueTerm(operand, function, ssa);
        SymbolicFormula me;
        SymbolicFormula one = mInternalFC.getFormulaManager().makeNumber(1);
        if (op == IASTUnaryExpression.op_postFixIncr ||
            op == IASTUnaryExpression.op_prefixIncr) {
          me = mInternalFC.getFormulaManager().makePlus(mvar, one);
        } else {
          me = mInternalFC.getFormulaManager().makeMinus(mvar, one);
        }
        return mInternalFC.getFormulaManager().makeAssignment(newvar, me);
      }

      case IASTUnaryExpression.op_minus: {
        SymbolicFormula mop = buildTerm(operand, function, ssa);
        return mInternalFC.getFormulaManager().makeNegate(mop);
      }

      case IASTUnaryExpression.op_amper:
      case IASTUnaryExpression.op_star:
        if (mInternalFC.handleLValuesAsUIF()) {
          String opname;
          if (op == IASTUnaryExpression.op_amper) {
            opname = org.sosy_lab.cpachecker.util.symbpredabstraction.CtoFormulaConverter.OP_ADDRESSOF_NAME;
          } else {
            opname = org.sosy_lab.cpachecker.util.symbpredabstraction.CtoFormulaConverter.OP_STAR_NAME;
          }
          SymbolicFormula term = buildTerm(operand, function, ssa);

          // PW make SSA index of * independent from argument
          int idx = mInternalFC.getIndex(opname, ssa);
          //int idx = getIndex(
          //    opname, term, ssa, absoluteSSAIndices);

          // build the  function corresponding to this operation.
          return mInternalFC.getFormulaManager().makeUIF(opname, mInternalFC.getFormulaManager().makeList(term), idx);

        } else {
          mInternalFC.warnUnsafeVar(exp);
          return makeVariable(org.sosy_lab.cpachecker.util.symbpredabstraction.CtoFormulaConverter.exprToVarName(exp), function, ssa);
        }

      case IASTUnaryExpression.op_tilde: {
        SymbolicFormula term = buildTerm(operand, function, ssa);
        return mInternalFC.getFormulaManager().makeBitwiseNot(term);
      }

      /* !operand cannot be handled directly in case operand is a variable
       * we would need to know if operand is of type boolean or something else
       * currently ! is handled by the default branch
      case IASTUnaryExpression.op_not: {
        long operandMsat = buildMsatTerm(operand, ssa);
        return mathsat.api.msat_make_not(msatEnv, operandMsat);
      }*/

      case IASTUnaryExpression.op_sizeof: {
        // TODO
        mInternalFC.warnUnsafeVar(exp);
        return makeVariable(org.sosy_lab.cpachecker.util.symbpredabstraction.CtoFormulaConverter.exprToVarName(exp), function, ssa);
      }

      default: {
        // this might be a predicate implicitly cast to an int. Let's
        // see if this is indeed the case...
        SymbolicFormula ftmp = makePredicate(exp, function, ssa);
        return mInternalFC.getFormulaManager().makeIfThenElse(ftmp, mInternalFC.getFormulaManager().makeNumber(1), mInternalFC.getFormulaManager().makeNumber(0));
      }
      }
    } else if (exp instanceof IASTBinaryExpression) {
      int op = ((IASTBinaryExpression)exp).getOperator();
      IASTExpression e1 = ((IASTBinaryExpression)exp).getOperand1();
      IASTExpression e2 = ((IASTBinaryExpression)exp).getOperand2();

      switch (op) {
      case IASTBinaryExpression.op_assign:
      case IASTBinaryExpression.op_plusAssign:
      case IASTBinaryExpression.op_minusAssign:
      case IASTBinaryExpression.op_multiplyAssign:
      case IASTBinaryExpression.op_divideAssign:
      case IASTBinaryExpression.op_moduloAssign:
      case IASTBinaryExpression.op_binaryAndAssign:
      case IASTBinaryExpression.op_binaryOrAssign:
      case IASTBinaryExpression.op_binaryXorAssign:
      case IASTBinaryExpression.op_shiftLeftAssign:
      case IASTBinaryExpression.op_shiftRightAssign: {
        SymbolicFormula me2 = buildTerm(e2, function, ssa);
        if (op != IASTBinaryExpression.op_assign) {
          // in this case, we have to get the old SSA instance for
          // reading the value of the variable, and build the
          // corresponding expression
          SymbolicFormula oldvar = buildTerm(e1, function, ssa);
          switch (op) {
          case IASTBinaryExpression.op_plusAssign:
            me2 = mInternalFC.getFormulaManager().makePlus(oldvar, me2);
            break;
          case IASTBinaryExpression.op_minusAssign:
            me2 = mInternalFC.getFormulaManager().makeMinus(oldvar, me2);
            break;
          case IASTBinaryExpression.op_multiplyAssign:
            me2 = mInternalFC.getFormulaManager().makeMultiply(oldvar, me2);
            break;
          case IASTBinaryExpression.op_divideAssign:
            me2 = mInternalFC.getFormulaManager().makeDivide(oldvar, me2);
            break;
          case IASTBinaryExpression.op_moduloAssign:
            me2 = mInternalFC.getFormulaManager().makeModulo(oldvar, me2);
            break;
          case IASTBinaryExpression.op_binaryAndAssign:
            me2 = mInternalFC.getFormulaManager().makeBitwiseAnd(oldvar, me2);
            break;
          case IASTBinaryExpression.op_binaryOrAssign:
            me2 = mInternalFC.getFormulaManager().makeBitwiseOr(oldvar, me2);
            break;
          case IASTBinaryExpression.op_binaryXorAssign:
            me2 = mInternalFC.getFormulaManager().makeBitwiseXor(oldvar, me2);
            break;
          case IASTBinaryExpression.op_shiftLeftAssign:
            me2 = mInternalFC.getFormulaManager().makeShiftLeft(oldvar, me2);
            break;
          case IASTBinaryExpression.op_shiftRightAssign:
            me2 = mInternalFC.getFormulaManager().makeShiftRight(oldvar, me2);
            break;
          default:
            throw new UnrecognizedCCodeException("Unknown binary operator", null, exp);
          }
        }
        SymbolicFormula mvar = buildsatLvalueTerm(e1, function, ssa);
        return mInternalFC.getFormulaManager().makeAssignment(mvar, me2);
      }

      case IASTBinaryExpression.op_plus:
      case IASTBinaryExpression.op_minus:
      case IASTBinaryExpression.op_multiply:
      case IASTBinaryExpression.op_divide:
      case IASTBinaryExpression.op_modulo:
      case IASTBinaryExpression.op_binaryAnd:
      case IASTBinaryExpression.op_binaryOr:
      case IASTBinaryExpression.op_binaryXor:
      case IASTBinaryExpression.op_shiftLeft:
      case IASTBinaryExpression.op_shiftRight: {
        SymbolicFormula me1 = buildTerm(e1, function, ssa);
        SymbolicFormula me2 = buildTerm(e2, function, ssa);

        switch (op) {
        case IASTBinaryExpression.op_plus:
          return mInternalFC.getFormulaManager().makePlus(me1, me2);
        case IASTBinaryExpression.op_minus:
          return mInternalFC.getFormulaManager().makeMinus(me1, me2);
        case IASTBinaryExpression.op_multiply:
          return mInternalFC.getFormulaManager().makeMultiply(me1, me2);
        case IASTBinaryExpression.op_divide:
          return mInternalFC.getFormulaManager().makeDivide(me1, me2);
        case IASTBinaryExpression.op_modulo:
          return mInternalFC.getFormulaManager().makeModulo(me1, me2);
        case IASTBinaryExpression.op_binaryAnd:
          return mInternalFC.getFormulaManager().makeBitwiseAnd(me1, me2);
        case IASTBinaryExpression.op_binaryOr:
          return mInternalFC.getFormulaManager().makeBitwiseOr(me1, me2);
        case IASTBinaryExpression.op_binaryXor:
          return mInternalFC.getFormulaManager().makeBitwiseXor(me1, me2);
        case IASTBinaryExpression.op_shiftLeft:
          return mInternalFC.getFormulaManager().makeShiftLeft(me1, me2);
        case IASTBinaryExpression.op_shiftRight:
          return mInternalFC.getFormulaManager().makeShiftRight(me1, me2);
        }
        break;
      }

      default:
        // this might be a predicate implicitly cast to an int, like this:
        // int tmp = (a == b)
        // Let's see if this is indeed the case...
        SymbolicFormula ftmp = makePredicate(exp, function, ssa);
        return mInternalFC.getFormulaManager().makeIfThenElse(ftmp, mInternalFC.getFormulaManager().makeNumber(1), mInternalFC.getFormulaManager().makeNumber(0));
      }
    } else if (exp instanceof IASTFieldReference) {
      if (mInternalFC.handleLValuesAsUIF()) {
        IASTFieldReference fexp = (IASTFieldReference)exp;
        String field = fexp.getFieldName().getRawSignature();
        IASTExpression owner = fexp.getFieldOwner();
        SymbolicFormula term = buildTerm(owner, function, ssa);

        String tpname = mInternalFC.getTypeName(owner.getExpressionType());
        String ufname =
          (fexp.isPointerDereference() ? "->{" : ".{") +
          tpname + "," + field + "}";
        
        return mInternalFC.makeUIF(ufname, mInternalFC.getFormulaManager().makeList(term), ssa);
      } else {
        mInternalFC.warnUnsafeVar(exp);
        return makeVariable(org.sosy_lab.cpachecker.util.symbpredabstraction.CtoFormulaConverter.exprToVarName(exp), function, ssa);

      }
    } else if (exp instanceof IASTArraySubscriptExpression) {
      if (mInternalFC.handleLValuesAsUIF()) {
        IASTArraySubscriptExpression aexp =
          (IASTArraySubscriptExpression)exp;
        IASTExpression arrexp = aexp.getArrayExpression();
        IASTExpression subexp = aexp.getSubscriptExpression();
        SymbolicFormula aterm = buildTerm(arrexp, function, ssa);
        SymbolicFormula sterm = buildTerm(subexp, function, ssa);

        String ufname = org.sosy_lab.cpachecker.util.symbpredabstraction.CtoFormulaConverter.OP_ARRAY_SUBSCRIPT;
        
        return mInternalFC.makeUIF(ufname, mInternalFC.getFormulaManager().makeList(aterm, sterm), ssa);

      } else {
        mInternalFC.warnUnsafeVar(exp);
        return makeVariable(org.sosy_lab.cpachecker.util.symbpredabstraction.CtoFormulaConverter.exprToVarName(exp), function, ssa);
      }
    } else if (exp instanceof IASTFunctionCallExpression) {
      // this is an external call. We have to create an UIF.
      IASTFunctionCallExpression fexp = (IASTFunctionCallExpression)exp;
      return makeExternalFunctionCall(fexp, function, ssa);
    } else if (exp instanceof IASTTypeIdExpression) {
      assert(((IASTTypeIdExpression)exp).getOperator() ==
        IASTTypeIdExpression.op_sizeof);
      mInternalFC.warnUnsafeVar(exp);
      return makeVariable(org.sosy_lab.cpachecker.util.symbpredabstraction.CtoFormulaConverter.exprToVarName(exp), function, ssa);
    }
    
    throw new UnrecognizedCCodeException("Unknown expression", null, exp);
  }

  private SymbolicFormula buildsatLvalueTerm(IASTExpression pExpression, String pCurrentFunction, SSAMapBuilder pSSAMap) {
    if (pExpression instanceof IASTIdExpression) {
      String lVariable = ((IASTIdExpression)pExpression).getName().getRawSignature();
      
      if (mInternalFC.isNondetVariable(lVariable)) {
        throw new RuntimeException("Do not assign values to nondeterministic variables! (Line " + pExpression.getFileLocation().getStartingLineNumber() + ")");
      }
      
      String lScopedVariable = mInternalFC.scoped(lVariable, pCurrentFunction);
      int lIndex = mInternalFC.makeLvalIndex(lScopedVariable, pSSAMap);
      
      return mInternalFC.getFormulaManager().makeVariable(lScopedVariable, lIndex);
    }
    else {
      throw new RuntimeException("We only support test generation for assignments to variables!");
    }
  }

  private SymbolicFormula makeExternalFunctionCall(IASTFunctionCallExpression fexp,
        String function, SSAMapBuilder ssa) throws UnrecognizedCCodeException {
    IASTExpression fn = fexp.getFunctionNameExpression();
    String func;
    if (fn instanceof IASTIdExpression) {
      mInternalFC.log(Level.INFO, "Assuming external function to be a pure function", fn);
      func = ((IASTIdExpression)fn).getName().getRawSignature();
    } else {
      mInternalFC.log(Level.WARNING, "Ignoring function call through function pointer", fexp);
      func = "<func>{" + fn.getRawSignature() + "}";
    }

    IASTExpression pexp = fexp.getParameterExpression();
    if (pexp == null) {
      // this is a function of arity 0. We create a fresh global variable
      // for it (instantiated at 1 because we need an index but it never
      // increases)
      mInternalFC.addToGlobalVars(func);
      return mInternalFC.getFormulaManager().makeVariable(func, 1);
    } else {
      IASTExpression[] args;
      if (pexp instanceof IASTExpressionList) {
        args = ((IASTExpressionList)pexp).getExpressions();
      } else {
        args = new IASTExpression[]{pexp};
      }
      func += "{" + args.length + "}"; // add #arguments to function name to cope with varargs functions
      SymbolicFormula[] mArgs = new SymbolicFormula[args.length];
      for (int i = 0; i < args.length; ++i) {
        mArgs[i] = buildTerm(args[i], function, ssa);
      }

      return mInternalFC.getFormulaManager().makeUIF(func, mInternalFC.getFormulaManager().makeList(mArgs));
    }
  }

  private SymbolicFormula createBinaryPredicate(int pOperator, SymbolicFormula pOperand1, SymbolicFormula pOperand2) {
    switch (pOperator) {
    case IASTBinaryExpression.op_greaterThan:
      return mInternalFC.getFormulaManager().makeGt(pOperand1, pOperand2);

    case IASTBinaryExpression.op_greaterEqual:
      return mInternalFC.getFormulaManager().makeGeq(pOperand1, pOperand2);

    case IASTBinaryExpression.op_lessThan:
      return mInternalFC.getFormulaManager().makeLt(pOperand1, pOperand2);

    case IASTBinaryExpression.op_lessEqual:
      return mInternalFC.getFormulaManager().makeLeq(pOperand1, pOperand2);

    case IASTBinaryExpression.op_equals:
      return mInternalFC.getFormulaManager().makeEqual(pOperand1, pOperand2);

    case IASTBinaryExpression.op_notequals:
      return mInternalFC.getFormulaManager().makeNot(mInternalFC.getFormulaManager().makeEqual(pOperand1, pOperand2));
      
    default:
      return null;
    } 
  }
  
  private SymbolicFormula createBinaryPredicate(IASTBinaryExpression pExpression, String pFunction, SSAMapBuilder pSSAMapBuilder) throws UnrecognizedCCodeException {
    IASTExpression op1 = pExpression.getOperand1();
    IASTExpression op2 = pExpression.getOperand2();

    SymbolicFormula t1 = buildTerm(op1, pFunction, pSSAMapBuilder);
    SymbolicFormula t2 = buildTerm(op2, pFunction, pSSAMapBuilder);
    
    return createBinaryPredicate(pExpression.getOperator(), t1, t2);
  }
  
  private SymbolicFormula makeNegativePredicate(IASTExpression exp, String function, SSAMapBuilder ssa) throws UnrecognizedCCodeException {
    SymbolicFormula result = makePredicate(exp, function, ssa);
    
    return mInternalFC.getFormulaManager().makeNot(result);
  }
  
  protected SymbolicFormula makePredicate(IASTExpression exp, String function, SSAMapBuilder ssa) throws UnrecognizedCCodeException {
    SymbolicFormula result = null;
    
    if (exp instanceof IASTBinaryExpression) {
      result = createBinaryPredicate((IASTBinaryExpression)exp, function, ssa);
    }
    else if (exp instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExp = ((IASTUnaryExpression)exp);
      if (unaryExp.getOperator() == IASTUnaryExpression.op_not) {
        // ! exp
        return makeNegativePredicate(unaryExp.getOperand(), function, ssa);      
      } 
      else if (unaryExp.getOperator() == IASTUnaryExpression.op_bracketedPrimary) {
        // (exp)
        return makePredicate(unaryExp.getOperand(), function, ssa);
      }
    }
    
    if (result == null) {
      // not handled above, check whether this is an implict cast to bool
      // build the term. If this is not a predicate, make
      // it a predicate by adding a "!= 0"
      result = buildTerm(exp, function, ssa);

      if (!mInternalFC.getFormulaManager().isBoolean(result)) {
        SymbolicFormula z = mInternalFC.getFormulaManager().makeNumber(0);
        result = mInternalFC.getFormulaManager().makeNot(mInternalFC.getFormulaManager().makeEqual(result, z));
      }
    }
      
    return result;
  }
}