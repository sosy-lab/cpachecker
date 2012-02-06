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
package org.sosy_lab.cpachecker.cpa.einterpreter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.cfa.ast.DummyType;
import org.sosy_lab.cpachecker.cfa.ast.IASTArrayTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTCompositeTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTElaboratedTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTEnumerationSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTEnumerationSpecifier.IASTEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNamedTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTPointerTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IComplexType;
import org.sosy_lab.cpachecker.cfa.ast.IType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionPntCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionPntReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.assume.ConstrainedAssumeElement;
import org.sosy_lab.cpachecker.cpa.einterpreter.ExprResult.RType;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.AddrMemoryCell;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Address;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.ArrayVariable;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.DataMemoryCell;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.DynVariable;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.DynVariable.dyntypes;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.EnumVariable;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.FuncMemoryCell;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.FuncPointerVariable;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.MemoryBlock;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.MemoryBlock.CellType;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.PersMemory;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.PointerVariable;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.PrimitiveVariable;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Scope;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.ArrayType;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.CompositeType;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.EnumType;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.FunctionType;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.PointerType;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.Primitive;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.PrimitiveType;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.StructType;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.TypeClass;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.UnionType;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Variable;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonPredicateElement;
import org.sosy_lab.cpachecker.cpa.interpreter.exceptions.AccessToUninitializedVariableException;
import org.sosy_lab.cpachecker.cpa.interpreter.exceptions.MissingInputException;
import org.sosy_lab.cpachecker.cpa.interpreter.exceptions.ReadingFromNondetVariableException;
import org.sosy_lab.cpachecker.cpa.pointer.Memory;
import org.sosy_lab.cpachecker.cpa.pointer.Pointer;
import org.sosy_lab.cpachecker.cpa.pointer.PointerElement;
import org.sosy_lab.cpachecker.efshell.Main;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.fshell.fql2.translators.cfa.ToFlleShAssumeEdgeTranslator;
import org.sosy_lab.cpachecker.util.ecp.ECPPredicate;


@SuppressWarnings("unused")
public class InterpreterTransferRelation implements TransferRelation {
  public static int TRCOUNT=0;
  public final static ArrayList<String> TRLIST = new ArrayList<String>();
  public final static ArrayList<Long> TRLISTTIME = new ArrayList<Long>();
  public final static ArrayList<Integer> PMSlist = new ArrayList<Integer>();
  public final static ArrayList<Integer> SVlist = new ArrayList<Integer>();
  public final static ArrayList<Integer> MBClist = new ArrayList<Integer>();
  public final static ArrayList<Integer> AMClist = new ArrayList<Integer>();
  public final static ArrayList<Integer> DMClist = new ArrayList<Integer>();
  public final static ArrayList<Integer> FMClist = new ArrayList<Integer>();
  private long t1,t2=0;
  private final Set<String> globalVars = new HashSet<String>();

  private String missingInformationLeftVariable = null;
  private String missingInformationRightPointer = null;
  private String missingInformationLeftPointer  = null;
  private IASTExpression missingInformationRightExpression = null;
  private Map<String, CFAFunctionDefinitionNode>  map= null;
  Timer k = new Timer();

  InterpreterTransferRelation(Map<String, CFAFunctionDefinitionNode> pmap){
    map = pmap;
  }

/*
 * NumOP enum is used during evaluation of an lexpr or rexpr in handleBinaryOP
 * currently contains method for perfoming binary numeric ops with data types integer and BigInteger
 */
  enum NumOP{
    PLUS,
    MINUS,
    MULTIPLY,
    DIVIDE;
    BigInteger performCalc(BigInteger a, BigInteger b){
      switch(this){
      case PLUS:
        return a.add(b);
      case MINUS:
        return a.subtract(b);
      case MULTIPLY:
        return a.multiply(b);
      case DIVIDE:
        return a.divide(b);
      default:

        return null;
      }

    }
   int performCalc(int a, int b){
      switch(this){
      case PLUS:
        return a+b;
      case MINUS:
        return a-b;
      case MULTIPLY:
        return a*b;
      case DIVIDE:
        return a/b;
      default:

        return 0;
      }

    }

  }


  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement element, Precision precision, CFAEdge cfaEdge) throws CPATransferException {


    TRCOUNT++;
    if(Main.CMPLXA==1){
    TRLIST.add(String.valueOf(cfaEdge.getLineNumber()));

    PersMemory.PMScnt=0;
    Scope.SVcnt=0;
    MemoryBlock.MBCcnt=0;
    AddrMemoryCell.AMCcnt=0;
    DataMemoryCell.DMCcnt=0;
    FuncMemoryCell.FMCcnt=0;
    }

    t1 = System.nanoTime();
    InterpreterElement successor ;
    AbstractElement check = null;
    InterpreterElement explicitElement = (InterpreterElement)element;

    //successor = explicitElement;


    // check the type of the edge
    //System.out.println(cfaEdge.getLineNumber()+": "+ cfaEdge.getRawStatement());
    /*System.out.println(cfaEdge.getEdgeType());
    System.out.println(cfaEdge.getPredecessor());
    System.out.println(cfaEdge.getPredecessor().getNumLeavingEdges());
    System.out.println(cfaEdge.getSuccessor().getLeavingEdge(0));*/
  /*  if(cfaEdge.getLineNumber()==3){
      System.out.println("here");
    }*/
    switch (cfaEdge.getEdgeType ()) {

    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge: {
      successor = explicitElement.copy();
      StatementEdge statementEdge = (StatementEdge) cfaEdge;
      try {
        handleStatement(statementEdge,successor);
      } catch (Exception e) {
       e.printStackTrace();
       System.exit(1);
      }




      check = successor;
      break;
    }
    // here the return value of a function is passed to the left hand side variable if there is one
    // e.g. a= test(b);
    case ReturnStatementEdge: {
      successor = explicitElement.copy();
      ReturnStatementEdge returnEdge = (ReturnStatementEdge)cfaEdge;
      IASTExpression exp = successor.getCurrentScope().getReturnExpression();
      if(exp!=null){
        try {
          ExprResult res = handleLeftSide(exp, successor.getCurrentScope().getParentScope(),successor);
          ExprResult res2  = handleRightSide(returnEdge.getExpression(),successor.getCurrentScope(),successor);
          handleAssignment(res,res2,successor);

        } catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
        }
      }
      // this statement is a function return, e.g. return (a);
      // note that this is different from return edge
      // this is a statement edge which leads the function to the
      // last node of its CFA, where return edge is from that last node
      // to the return site of the caller function
      check = successor;
      break;
    }

    // edge is a declaration edge, e.g. int a;
    case DeclarationEdge: {
      successor = explicitElement.copy();
      DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;
      try {
        handleDeclaration(declarationEdge,successor);
      } catch (Exception e) {
       e.printStackTrace();
       System.exit(1);
      }
      check = successor;
      break;
    }

    // this is an assumption, e.g. if(a == b)
    case AssumeEdge: {
      successor = explicitElement;
      AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;

      try {
        check = handleAssume(assumeEdge, successor);
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        System.exit(1);
      }

      break;
    }

    case BlankEdge: {
      successor = explicitElement;
      break;
    }
    // handles FunctionCalls like a=test(b);
    case FunctionCallEdge: {
      successor = explicitElement.copy();
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) cfaEdge;
     try {
      handleFunctionCall(functionCallEdge.getRawAST(),functionCallEdge.getSuccessor(),functionCallEdge.getArguments(),successor);
    } catch (Exception e) {
     e.printStackTrace();
     System.exit(1);
    }
    check = successor;


      break;
    }

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case FunctionReturnEdge: {
      successor = explicitElement.copy();
      FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge) cfaEdge;

      successor.redScope();
      check = successor;
      break;
    }
    //handles the return
    case FunctionPntReturnEdge:
      successor = explicitElement.copy();
      check = InterpreterBottomElement.INSTANCE;
      FunctionPntReturnEdge fpredge = (FunctionPntReturnEdge) cfaEdge;
      CallToReturnEdge abbr = fpredge.getSuccessor().getEnteringSummaryEdge();
      CFANode nod = abbr.getPredecessor();
      if(nod.equals(successor.getCurrentScope().getReturnNode())){
        check = successor;
        successor.redScope();
      }
      break;
    // handles function calls thru a function pointer like a=(*test)(b);
    case FunctionPntCallEdge:
      successor = explicitElement.copy();
      check = InterpreterBottomElement.INSTANCE;
      ExprResult res;
      FunctionPntCallEdge fpcedge = (FunctionPntCallEdge)cfaEdge;
      IASTStatement statement = fpcedge.getRawAST();
      IASTFunctionCallExpression rside;
      if(statement instanceof IASTFunctionCallAssignmentStatement ){
         rside = ((IASTFunctionCallAssignmentStatement) statement).getRightHandSide();

      }
      else if(statement instanceof IASTFunctionCallStatement){
        rside = ((IASTFunctionCallStatement) statement).getFunctionCallExpression();

      }else{
        break;
      }


      try {
        res = handleRightSide(rside.getFunctionNameExpression(),successor.getCurrentScope(),successor);

        if(res.getFunctionPnt().equals(fpcedge.getSuccessor())){
          handleFunctionCall(fpcedge.getRawAST(),fpcedge.getSuccessor(),fpcedge.getArguments(), successor);
          successor.getCurrentScope().setReturnNode(cfaEdge.getPredecessor());
          check = successor;

        }

       } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
       }



      break;
    default:
      throw new UnrecognizedCFAEdgeException(cfaEdge);
    }

   /*try {
      System.out.println(cfaEdge);
      System.in.read();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }*/

    /*System.out.println("INTERPRETER: ");
    System.out.println("(" + element.toString() + ", " + precision.toString() + ")");
    System.out.println("--[" + cfaEdge.toString() + "]->");
    System.out.println(successor.toString());
    System.out.println("--------------");*/


    if(Main.CMPLXA==1){

    PMSlist.add(PersMemory.PMScnt);
    SVlist.add(Scope.SVcnt);
    MBClist.add(MemoryBlock.MBCcnt);
    AMClist.add(AddrMemoryCell.AMCcnt);
    DMClist.add(DataMemoryCell.DMCcnt);
    FMClist.add(FuncMemoryCell.FMCcnt);
    }

    if (InterpreterBottomElement.INSTANCE.equals(check)) {
      TRLISTTIME.add(System.nanoTime()-t1);
      return Collections.emptySet();
    } else {
      TRLISTTIME.add(System.nanoTime()-t1);
      return Collections.singleton(successor);
    }

  }


/*
 * function which evaluates an expr and checks if the result of the evaluation
 * corresponds to the TruthAssumption or not.
 * Returns the interpreterElement if they coincide and the BottomElement if not.
 *
 */
private AbstractElement handleAssume(AssumeEdge pAssumeEdge,
      InterpreterElement pel) throws Exception{

      //System.out.println("TRUTH "+ pAssumeEdge.getTruthAssumption());
      //System.out.println("EXPRESSION "+ pAssumeEdge.getExpression().getRawSignature());
      ExprResult exp = handleRightSide(pAssumeEdge.getExpression(),pel.getCurrentScope(),pel);
      switch(exp.getResultType()){
      case Num:
        if((exp.getnumber().compareTo(BigInteger.ZERO)==0) ^ !pAssumeEdge.getTruthAssumption()){
          return InterpreterBottomElement.INSTANCE;
        }else{
          return pel;
        }
      case Var:
        Variable v = exp.getVariable();
        if(v.getTypeClass(pel) == TypeClass.PRIMITIVE){ //if the variable is a primitive variable
          PrimitiveVariable pvar = (PrimitiveVariable)v;
          BigInteger big1 = decodeVar(pvar,pel);
          //System.out.println(big1);
          if((big1.compareTo(BigInteger.ZERO)==0) ^ !pAssumeEdge.getTruthAssumption()){
            return InterpreterBottomElement.INSTANCE;
          }else{
            return pel;
          }
        }else if (v.getTypeClass(pel) == TypeClass.POINTER || v.getTypeClass(pel) == TypeClass.PRIMITIVEPNT){ // NULL Pointer Check
          MemoryBlock block = v.getAddress().getMemoryBlock();
          int offset = v.getAddress().getOffset();
          if(block.getAddress(offset,pel).getMemoryBlock()==null ^ !pAssumeEdge.getTruthAssumption()){
            return InterpreterBottomElement.INSTANCE;
          }else{
            return pel;
          }
        }else{
          throw new Exception("not yet supported");
        }


      default:
        throw new Exception("NOt supported assume result");
      }


  }



/*
 * this function handles function calls with the following steps:
 * 1. get the lefthand expr
 * 2. a new scope with the name of the function is set up
 * 3. pass the lefthandexpr to the scope so that the return value can be parsed to it
 * 4. create the parameter variables and pass the value of the arguments to them
 */
private void handleFunctionCall(IASTStatement f ,  FunctionDefinitionNode functiondef ,List<IASTExpression> args,InterpreterElement pelement) throws Exception {

  //IASTStatement f;
 // f = pFunctionCallEdge.getRawAST();
  IASTExpression left=null;
  if(f instanceof IASTFunctionCallAssignmentStatement ){
    //res= func();
    left = ((IASTFunctionCallAssignmentStatement) f).getLeftHandSide();

  }
  //FunctionDefinitionNode functiondef = pFunctionCallEdge.getSuccessor();

  List<IASTParameterDeclaration> parlist = functiondef.getFunctionParameters();
  //List<IASTExpression> args = pFunctionCallEdge.getArguments();
  String name = functiondef.getFunctionName();
  List<String> list = functiondef.getFunctionParameterNames();
  pelement.setCurrentScope(name);
  pelement.getCurrentScope().setReturnExpression(left);
  copyVars(pelement,list,args,parlist);

}


/*
 * handles AssignStatements and function calls which are unknown like CPAmalloc and CPAfree
 */
private void handleStatement(StatementEdge pStatementEdge,
    InterpreterElement pelement) throws Exception {

  IASTStatement statement =pStatementEdge.getStatement();

  if(statement instanceof IASTExpressionAssignmentStatement){

    handleAssignStatement((IASTExpressionAssignmentStatement)statement,pelement);

  }else if(statement instanceof IASTFunctionCallAssignmentStatement ){

    handleUnknownFunctionCall((IASTFunctionCallAssignmentStatement) statement,pelement);
  }else if(statement instanceof IASTFunctionCallStatement){
    handleFree((IASTFunctionCallStatement)statement,pelement);
  }


}

/*
 * handles CPAmalloc defines a empty memoryblock with size defined by CPAmalloc
 *
 */

private void handleUnknownFunctionCall(IASTFunctionCallAssignmentStatement pstatement, InterpreterElement pelement) throws Exception {
  //handle CPAmalloc
  IASTFunctionCallAssignmentStatement mcall = pstatement;
  IASTExpression nameexpr = mcall.getFunctionCallExpression().getFunctionNameExpression();
  if(nameexpr instanceof IASTIdExpression){
    String funcname = ((IASTIdExpression) nameexpr).getName();
    if(funcname.compareTo("CPAmalloc")==0){
      if(mcall.getFunctionCallExpression().getParameterExpressions().size()!=1)
        throw new Exception("CPAmalloc not properly used: arguments do not match");
      IASTExpression sizeexp = mcall.getFunctionCallExpression().getParameterExpressions().get(0);
      ExprResult expr= handleRightSide(sizeexp, pelement.getCurrentScope(),pelement);
      int size = expr.getnumber().intValue();
      MemoryBlock block = pelement.getFactory().allocateMemoryBlock(size,pelement);
      Address addr = new Address(block,0);
      ExprResult res1 = new ExprResult(addr,null,-1);
      ExprResult res2  = handleLeftSide(mcall.getLeftHandSide(),pelement.getCurrentScope(),pelement);
      handleAssignment(res2,res1,pelement);


    }else{ //unknown function
      throw new Exception("Unknown function");
    }
  }else{//unknown function pointer??
    throw new Exception("Unknown function type");
  }

}

/*
 * creates Variable during a function call according to the parameter list
 * and copies the values of the arguments passes during the function call
 * to the newly created Variables
 */
private void copyVars(InterpreterElement pelement, List<String> pList,
    List<IASTExpression> pArgs, List<IASTParameterDeclaration> parlist) throws Exception {
  for(int x=0;x<pArgs.size();x++){
    IASTExpression exp;
    String varname;
    exp = pArgs.get(x);
    varname = pList.get(x);
    IASTParameterDeclaration tmp = parlist.get(x);
    if(exp instanceof IASTLiteralExpression){ // number in argument
      if(tmp.getDeclSpecifier() instanceof IASTNamedTypeSpecifier){//typedef is handled here
        Type res = getNamedType(tmp.getDeclSpecifier(),  pelement.getCurrentScope(),pelement);
        allocSimpleVar(tmp.getName(), (PrimitiveType) res, pelement);
      }
      else{//primitve variable
      handleSimpleDecl((IASTSimpleDeclSpecifier)tmp.getDeclSpecifier(),tmp.getName(),pelement);
      }
      ExprResult res2 =handleRightSide(exp, pelement.getCurrentScope(), pelement);
      ExprResult res1 = new ExprResult(pelement.getCurrentScope().getVariable(tmp.getName(),pelement));
      handleAssignment(res1, res2,pelement);
    }else if(exp instanceof IASTIdExpression){ //variable in argument - look up value und pass to new variable
      String cname = ((IASTIdExpression) exp).getName();
      Variable cvar = pelement.getCurrentScope().getParentScope().getVariable(cname,pelement);
      cvar.copyVar(varname, pelement);
    }else if (exp instanceof IASTUnaryExpression){ //handling deref of variable

       ExprResult expr = handleRightSide(exp, pelement.getCurrentScope().getParentScope(),pelement);
       IASTParameterDeclaration pardef = parlist.get(x);
       String cpname = pardef.getName();
       IType  typ = pardef.getDeclSpecifier();

       if(typ instanceof IASTPointerTypeSpecifier){ //parameter Pointer
         try {
           handlePointerDecl((IASTPointerTypeSpecifier)typ,cpname ,pelement);
           ExprResult expr2 = new ExprResult(pelement.getCurrentScope().getVariable(cpname,pelement));
           handleAssignment(expr2,expr,pelement);
         } catch (Exception e) {
          e.printStackTrace();
         }
       }else if (typ instanceof IASTNamedTypeSpecifier){ //parameter typedef variable
         Type res = getNamedType(typ, pelement.getCurrentScope(), pelement);
         if(res instanceof PointerType){
           allocatePointerVar(cpname, (PointerType)res, pelement);
           ExprResult expr2 = new ExprResult(pelement.getCurrentScope().getVariable(cpname,pelement));
           handleAssignment(expr2,expr,pelement);

         }else{
           throw new Exception("Only address assignment in function call supported or typedefs");
         }

       }
       else{
         throw new Exception("Only address assignment in function call supported or typedefs");
       }

    }else{

      throw new Exception("Not supported");
    }


  }

}


/*
 * free memoryblocks by setting flags in the memory thus that a memory exception
 * is thrown if the memoryblock is being accessed again
 */
private void handleFree(IASTFunctionCallStatement pStatement,
    InterpreterElement pel) throws Exception {
  IASTFunctionCallExpression mcall = pStatement.getFunctionCallExpression();
  IASTExpression nameexpr =mcall.getFunctionNameExpression();
    if(nameexpr instanceof IASTIdExpression){
      String funcname = ((IASTIdExpression)nameexpr).getName();
      if(funcname.compareTo("CPAfree")==0){
        ExprResult x = handleRightSide(mcall.getParameterExpressions().get(0),pel.getCurrentScope(),pel);
        switch(x.getResultType()){
        case Var:
          Variable v = x.getVariable();
          if(v instanceof PointerVariable){
            MemoryBlock b = v.getAddress().getMemoryBlock();
            int offset = v.getAddress().getOffset();
            Address addr = b.getAddress(offset,pel);
            if(addr.getOffset()!=0){
              throw new Exception("need address with offset 0 for memoryblock");
            }
            addr.getMemoryBlock().free(pel);
            return;
          }

        default:
          throw new Exception("not supported free");
        }
      }
    }

}


/*
 *  handles assigment of the form lexpr = rexpr;
 *  first lexpr and rexpr are evaluated then the assigment is done
 */
private void handleAssignStatement(
    IASTExpressionAssignmentStatement pStatement, InterpreterElement pelement) throws Exception {
     IASTExpression left = pStatement.getLeftHandSide();
     IASTExpression right = pStatement.getRightHandSide();
     ExprResult res1;
     ExprResult res2;
     res2 = handleRightSide(right,pelement.getCurrentScope(),pelement);
     res1 = handleLeftSide(left, pelement.getCurrentScope(),pelement);



     handleAssignment(res1,res2,pelement);




}


/*
 * handles the assignment
 * Takes the address of ExprResult res1 and writes the values
 * of ExprResult res2 into it
 */
private void handleAssignment(ExprResult res1, ExprResult res2, InterpreterElement pelement) throws Exception{



  Type typ;
  Address addr;
  int level;
  switch(res1.getResultType()){ //get data typ address and level
  case Var:
    typ = res1.getVariable().getType();
    addr = res1.getVariable().getAddress();
    level = 0;
    if(res1.getVariable() instanceof PointerVariable){

    }
    break;
  case Addr:
    addr = res1.getAddress();
    Type basetyp = res1.getDataType();
    if(res1.getLevelofIndirection()>=1)
      typ = new PointerType(basetyp,false,res1.getLevelofIndirection());
    else
      typ = basetyp;
    level = res1.getLevelofIndirection();
    break;
  default:
    throw new Exception("Not supported");
  }

  switch(res2.getResultType()){
  case FuncPnt:
    MemoryBlock bl = addr.getMemoryBlock();
    int offst = addr.getOffset();
    bl.setFunctionPointer(offst,res2.getFunctionPnt(),pelement);
    return;

  case Addr: //data to be written into memblock is address
    if(res2.getLevelofIndirection()==-1){ //malloc
      int offset =  addr.getOffset();
      MemoryBlock block = addr.getMemoryBlock();
      block.setAddress(offset, res2.getAddress(),pelement);
      return;
    }
    if(res1.getResultType()== RType.Var){
        Variable v = res1.getVariable();
        if(v instanceof PointerVariable){
          PointerVariable pvar = (PointerVariable)v;

          //if base types and level coincide write address to memory otherwise throw exception
          if(checkType(pvar.getBaseType(),res2.getDataType())&& (pvar.getlevel() == res2.getLevelofIndirection())){
            int offset = pvar.getAddress().getOffset();
            MemoryBlock block = pvar.getAddress().getMemoryBlock();
            block.setAddress(offset, res2.getAddress(),pelement);

          } else{
            throw new Exception("TYPES do not coincide");
          }



        }else if(v instanceof PrimitiveVariable){ //primitive variable used in pointercalc
           PrimitiveVariable pvar = (PrimitiveVariable)v;
           pvar.setPnt();
           int offset = pvar.getAddress().getOffset();
           MemoryBlock block = pvar.getAddress().getMemoryBlock();
           block.setAddress(offset, res2.getAddress(),pelement);

        }else{

          throw new Exception("Cannot assign Address to non pointer variable");
        }
    }else if(res1.getResultType() == RType.Addr){
      if(checkType(res1.getDataType(),res2.getDataType()) && (res1.getLevelofIndirection()== res2.getLevelofIndirection())){
      MemoryBlock b =addr.getMemoryBlock();
      int offset = addr.getOffset();
      b.setAddress(offset, res2.getAddress(),pelement);
      }else{
        throw new Exception("TYPES don't coincide");
      }

    }
    break;
  case Num: //rexpr result is a number
    if(typ instanceof EnumType  && level ==0)
      writeEnumVar(addr,res2.getnumber(),pelement);


    else if(typ instanceof PrimitiveType  && level ==0)
      writePrimVar((PrimitiveType)typ,addr,res2.getnumber(),pelement);
    else if(typ instanceof PointerType &&res2.getnumber().compareTo(BigInteger.valueOf(0)) ==0){ //set NULL POINTER

      if(res1.getResultType()==RType.Var){
        if(((PointerType)typ).getTargetType()instanceof FunctionType)
          ((FuncPointerVariable)res1.getVariable()).setNullPointer(true, pelement);
        else
          ((PointerVariable)res1.getVariable()).setNullPointer(pelement);
        return;
      }
      MemoryBlock block;
      int offset=0;
      block = addr.getMemoryBlock();
      offset = addr.getOffset();
      block.setAddress(offset, new Address(null,0),pelement);

    }
    else if(level >0 && res2.getnumber().compareTo(BigInteger.ZERO)==0){ //set NULL POINTER with address result for res1
      addr.getMemoryBlock().setAddress(addr.getOffset(), new Address(null,0),pelement);
    }else{
      //System.out.println(typ.getTypeClass());
      //System.out.println("NUMBER" +res2.getnumber());
      throw new Exception("Cannot asign number to not primitive type");
    }

    break;
  case Var: //result is a variable

    if( res2.getVariable().getName().equals("__BLAST_NONDET") ){
      if(!(typ instanceof PrimitiveType)){
        throw new Exception("Can blast nondet only assign to primitive variables");
      }
      writePrimVar((PrimitiveType) typ, addr, pelement.getNonDetNumber(), pelement);
      return;
    }

    Type typ2 = res2.getVariable().getType();
    Address addr2 = res2.getVariable().getAddress();

    if(checkType(typ,typ2)){ //if types coincide
      if(res1.getResultType()== RType.Var){
        copyVar(res1.getVariable().getAddress(),res2.getVariable().getAddress(),res2.getVariable().getSize(),pelement);

      }else if(res1.getResultType()== RType.Addr){
        copyVar(res1.getAddress(),res2.getVariable().getAddress(),res2.getVariable().getSize(),pelement);
      }
    }else if(typ instanceof PrimitiveType && typ2 instanceof PointerType){ //using primvariable for Pointercalc
       if (res1.getResultType()==RType.Var){
         PrimitiveVariable  pvar = (PrimitiveVariable) res1.getVariable();
         if(pvar.getPrimitiveType()== Primitive.LONG){
           pvar.setPnt();



         }else  if(pvar.getPrimitiveType()== Primitive.LONGLONG){
           pvar.setPnt();



         }
         else{
           throw new Exception("only int can be used in pointer calcs");
         }
       }

       copyVar(addr,res2.getVariable().getAddress(), res2.getVariable().getSize(),pelement );

    }
    /*else if(typ instanceof PointerType && typ2 instanceof PrimitiveType){


      copyVar(res1.getVariable().getAddress(),res2.getVariable().getAddress(),res2.getVariable().getSize(),pelement);
    }*/

    else{

      throw new Exception("Types of the 2 variables differ");
    }
    break;
  default:
    throw new Exception("Not supported");
  }




}
/*
 * evaluates the right hand side of an assignment recursively
 */
private ExprResult handleRightSide(IASTExpression pRight,
    Scope currentScope,InterpreterElement pel ) throws Exception {

  if(pRight instanceof IASTLiteralExpression){ //if right side is a number
    switch(((IASTLiteralExpression) pRight).getKind()){
    case IASTLiteralExpression.lk_float_constant:
      throw new RuntimeException("floating point operations not supported");
    case IASTLiteralExpression.lk_char_constant:
      IASTCharLiteralExpression charexp=((IASTCharLiteralExpression)pRight);
      byte data =(byte)charexp.getCharacter();
      /*byte x[] = new byte[1];
      x[0]=data;*/
      new ExprResult(BigInteger.valueOf(data));
      break;
    case IASTLiteralExpression.lk_integer_constant:
      IASTIntegerLiteralExpression intexp =((IASTIntegerLiteralExpression)pRight);
      BigInteger value =intexp.getValue();
      return new ExprResult(value);

    default:
      break;
    }
  }
  if(pRight instanceof IASTIdExpression){// if right side is a variable or funcpointer
    String var =((IASTIdExpression) pRight).getName();
    if(var.compareTo("__BLAST_NONDET")==0){
      return new ExprResult(pel.getNonDetNumber());
    }
    CFAFunctionDefinitionNode func = map.get(var);
    if(func != null){
      return new ExprResult(func);
    }else{
    return new ExprResult(currentScope.getVariable(var,pel));
    }

  }
  if(pRight instanceof IASTUnaryExpression){
    IASTUnaryExpression unaryexp= (IASTUnaryExpression) pRight;
    //recursive call for rexpr of form rexpr => op rexpr;
    ExprResult res =handleRightSide(unaryexp.getOperand(),currentScope,pel);
    UnaryOperator op = unaryexp.getOperator();
    switch(op){
    case  MINUS:
      switch(res.getResultType()){
      case Addr:
        throw new Exception("not supported");
      case Var:
        Variable v =res.getVariable();
        if(v.getTypeClass(pel)==TypeClass.PRIMITIVE){
          PrimitiveVariable var = (PrimitiveVariable)v;
          BigInteger bigint = decodeVar(var,pel);
          bigint = bigint.multiply(BigInteger.valueOf(-1));

          return new ExprResult(bigint);
        }else throw new Exception("cannot negate non primitive variable");

      case Num:
        return new ExprResult(res.getnumber().multiply(BigInteger.valueOf(-1)));

      default:
        throw new Exception("unhandled case");
      }

    case PLUS:
      return res;
    case STAR:
      res = handleRightSide(unaryexp.getOperand(),currentScope,pel);
      switch(res.getResultType()){
      case Var:
        if(res.getVariable().getTypeClass(pel)== TypeClass.POINTER){
          PointerVariable var = (PointerVariable)res.getVariable();
          MemoryBlock b =  var.getAddress().getMemoryBlock();


          Address addr = b.getAddress(0,pel);
          if(var.getlevel()>1 ){ //read address in memorycell and decrease lvl by 1
            if(addr.getMemoryBlock().getCellType(addr.getOffset(),pel)==CellType.ADDR){
              Address addr2 = addr.getMemoryBlock().getAddress(addr.getOffset(),pel);

              return new ExprResult(addr2,var.getBaseType(),var.getlevel()-1);
            }else if(addr.getMemoryBlock().getCellType(addr.getOffset(),pel)==CellType.FUNC){

              if(var.getlevel()!=2) //funcpointer has at least level 1 so pointer must have level 2
                throw new Exception("Error with dereference");
              return new ExprResult(addr.getMemoryBlock().getFunctionPointer(addr.getOffset(),pel));
            }else if (addr.getMemoryBlock().getCellType(addr.getOffset(),pel)== CellType.EMPTY){
                throw new RuntimeException("Can not deref empty Cell");
            }
          }else{
            switch(var.getBaseType().getTypeClass()){
            case PRIMITIVE:
              PrimitiveVariable tmp = new PrimitiveVariable("tmp", addr,  (PrimitiveType)var.getBaseType(), false,false);//TODO: signed unsigned handeln
              return new ExprResult(decodeVar(tmp,pel));
            case ENUM:
              EnumVariable evar = new EnumVariable("tmp",addr,(EnumType)var.getBaseType(),false);
              return new ExprResult(evar);
            default:
              throw new Exception("REF OF DATA TYPE NOT YET SUPPORTED");
            }
          }
        }else if(res.getVariable().getTypeClass(pel) == TypeClass.FUNCPOINTER){
          FuncPointerVariable var = (FuncPointerVariable)res.getVariable();
          MemoryBlock b =  var.getAddress().getMemoryBlock();
          int offset = var.getAddress().getOffset();

          if(var.getlevel()>1){
            Address tmp = b.getAddress(offset,pel);
            PointerType k  = (PointerType)var.getType();
            PointerType x = new PointerType(var.getBaseType(),false,var.getlevel()-1);

            FuncPointerVariable fptmp = new FuncPointerVariable("tmp",tmp,var.getBaseType(),x,var.getlevel()-1);
            return new ExprResult(fptmp);
          }else{
            return new ExprResult(b.getFunctionPointer(offset,pel));
          }




        }

      default:
        throw new Exception("STAR only on var and addr applicable");
      }

    case AMPER: // deref of a var e.g a = &x;
      switch(res.getResultType()){
      case Addr:
        //struct data->a might return address
        break;
      case Var:
        switch(res.getVariable().getTypeClass(pel)){
        case PRIMITIVE:
          PrimitiveVariable v = (PrimitiveVariable) res.getVariable();
          return new ExprResult(v.getAddress(), v.getType(),1);
        case POINTER:
          PointerVariable pvar = (PointerVariable) res.getVariable();
          return new ExprResult(pvar.getAddress(),pvar.getBaseType(),pvar.getlevel()+1);
        case STRUCT:
          DynVariable svar = (DynVariable)res.getVariable();
          return new ExprResult(svar.getAddress(),svar.getType(),1);
        case ENUM:
         EnumVariable evar = (EnumVariable)res.getVariable();
         return new ExprResult(evar.getAddress(),evar.getType(),1);
        default:
          throw new Exception("DEREF NOT YET SUPPORTED");
        }

      case Num:
        break;
      case FuncPnt:
        return res;


      default:
        break;
      }
      break;
    case TILDE: //binary negate e.g. 00000000 =>11111111
      switch(res.getResultType()){
      case Addr:
        throw new Exception("inversion of addresses not allowed");

      case Var:
        Variable v = res.getVariable();
        switch(v.getTypeClass(pel)){
        case PRIMITIVE:
          PrimitiveVariable pvar = (PrimitiveVariable)v;
          BigInteger big1= decodeVar(pvar,pel);
          return new ExprResult(big1.not());

        default:
          throw new Exception("inversion of complex data types not allowed");
        }

      case Num:
        BigInteger big1 = res.getnumber();
        return new ExprResult(big1.not());





      default:
        break;
      }

    case NOT: //handle logical not e.g 33=>0 0=>1
      ExprResult expr =handleNot(res,pel);
      return expr;
    case SIZEOF:
      switch(res.getResultType()){
      case Addr:

        break;
      case Var:

        return new ExprResult(BigInteger.valueOf(res.getVariable().getSize()));

      case Num:
        break;

      default:
        throw new Exception("SIZEOF NOT YET SUPPORTED");
      }
      break;
    default:
      break;
    }
  }
  if(pRight instanceof IASTBinaryExpression){
    ExprResult res1 = handleRightSide(((IASTBinaryExpression) pRight).getOperand1(), currentScope,pel);
    ExprResult res2 = handleRightSide(((IASTBinaryExpression) pRight).getOperand2(), currentScope,pel);
    ExprResult res;
    switch(((IASTBinaryExpression) pRight).getOperator()){
    case LOGICAL_OR:
      res = handleOr(res1,res2,currentScope,pel);
      return res;
    case LOGICAL_AND:
      res1 = handleNot(res1,pel);
      res2 = handleNot(res2,pel);
      res = handleOr(res1,res2,currentScope,pel);
      return res;
    case LESS_THAN:
      res = handleLessThan(res1,res2,currentScope,pel);
      return res;


    case GREATER_THAN:
      res = handleLessThan(res2,res1,currentScope,pel);
      return res;
    case LESS_EQUAL:
      ExprResult tmp;
      tmp = res1;
      res1 = res2;
      res2 = tmp;
    case GREATER_EQUAL:
      res = handleLessThan(res1,res2,currentScope,pel);
      if(res.getnumber().compareTo(BigInteger.ZERO)==0){
        return new ExprResult(BigInteger.ONE);
      }else{
        return new ExprResult(BigInteger.ZERO);
      }

    case BINARY_AND:
      res = handleBinaryOP(res1,res2,currentScope,pel,BOP.AND);
      return res;

    case BINARY_OR:
      res = handleBinaryOP(res1,res2,currentScope,pel,BOP.OR);
      return res;


    case PLUS:
       res = handleBinaryOP(res1,res2,currentScope,NumOP.PLUS,pel);
      return res;
    case MINUS:
      res = handleBinaryOP(res1,res2,currentScope,NumOP.MINUS,pel);
      return res;
    case MULTIPLY:
      res = handleBinaryOP(res1,res2,currentScope,NumOP.MULTIPLY,pel);
      return res;
    case DIVIDE:
      res = handleBinaryOP(res1,res2,currentScope,NumOP.DIVIDE,pel);
      return res;
    case EQUALS:
      res = handleEquals(res1,res2,currentScope,pel);
      return res;
    case NOT_EQUALS:
      res = handleEquals(res1,res2,currentScope,pel);
      if(res.getnumber().compareTo(BigInteger.ZERO)==0){
        return new ExprResult(BigInteger.ONE);
      }else{
        return new ExprResult(BigInteger.ZERO);
      }
    default:
      throw new Exception("BINARY OPERATOR NOT YET SUPPORTED");




    }
  }

  if(pRight instanceof IASTCastExpression){ //conversions are handled here
    return handleRCast(pRight,currentScope,pel);
     }




  //System.out.println(pRight.getRawSignature());
  throw new Exception("handleRightSide case unhandled");

}

private ExprResult handleRCast(IASTExpression pRight, Scope cur,
    InterpreterElement pel) throws Exception {
  Type z= getTypeCast(pRight.getExpressionType(),cur,pel); //get conversion type
  ExprResult res;
  switch(z.getTypeClass()){
  case PRIMITIVE:
    PrimitiveType k=(PrimitiveType)z;
    res = handleRightSide(((IASTCastExpression)pRight).getOperand(), cur,pel);
    switch(res.getResultType()){
    case Num:
      //TODO: add conversion?? (not really needed assign typechecks done by cil)
      return res;
    case Var:
      switch(res.getVariable().getTypeClass(pel)){
      case ENUM:
      case PRIMITIVE:
        if(res.getVariable().getName().compareTo("__BLAST_NONDET")==0){
          return new ExprResult(pel.getNonDetNumber());
        }
        //create tmp variable with new size and copy value of variable res to tmp
        MemoryBlock block = pel.getFactory().allocateMemoryBlock(k.sizeOf(),pel);
        Address naddr = new Address(block,0);
        if(k.sizeOf()<res.getVariable().getSize()){
          copyVar(naddr, res.getVariable().getAddress(), k.sizeOf(),pel);
        }else{
          copyVar(naddr, res.getVariable().getAddress(), res.getVariable().getSize(),pel);
          for(int x= res.getVariable().getSize();x<k.sizeOf();x++){
            naddr.getMemoryBlock().setData(x,(byte)0,pel);
          }
        }
        PrimitiveVariable tmp = new PrimitiveVariable("tmpx", naddr, k, k.isSigned(), k.isConst());

        return new ExprResult(tmp);

      case POINTER:
        //(int) pnt;
        return res;

      case FUNCPOINTER: //changed
        FuncPointerVariable v = (FuncPointerVariable) res.getVariable();
        if(v.isNullPointer(pel)){
          return new ExprResult(BigInteger.ZERO);
        }else{

        }
      case ARRAY:
        //write the address of the array or funcpointer in a new block and create an temporary primitivevariable used for pntcalc
        tmp = null;
        block = pel.getFactory().allocateMemoryBlock(k.getPrimitive().sizeOf(),pel);
        block.setAddress(0, res.getVariable().getAddress(),pel);
        naddr = new Address(block,0);
        tmp = new PrimitiveVariable("tmpy",naddr,k,k.isSigned(),k.isConst());
        tmp.setPnt();
        return new ExprResult(tmp);


      default:
        throw new Exception("Conversion not yet considered");
      }
    default:
      return res;
      //TODO: type conversion constraints
    }
  case POINTER:
    PointerType pt = (PointerType)z;
    res = handleRightSide(((IASTCastExpression)pRight).getOperand(), cur,pel);
    switch(res.getResultType()){
    case Addr:
      //TODO add pointer conversion logic
      PointerType ptyp = (PointerType)z;
      return new ExprResult(res.getAddress(),ptyp.getTargetType(),ptyp.getLevelOfIndirection());


    case Num:
      if(res.getnumber().compareTo(BigInteger.ZERO)==0){
        return res;
      }
      throw new Exception("Can not cast number to pointer");
    case Var:
      Variable v = res.getVariable();
      switch(v.getTypeClass(pel)){
        case PRIMITIVE:
          ((PrimitiveVariable) res.getVariable()).setPnt();


        case PRIMITIVEPNT:

          PrimitiveVariable var = (PrimitiveVariable)res.getVariable();
          MemoryBlock mblock = var.getAddress().getMemoryBlock();
          CellType ct = mblock.getCellType(var.getAddress().getOffset(), pel);
          if(ct==CellType.DATA){ //must contain value 0 otherwise conversion not feasible
            BigInteger val = decodeVar(var,pel);
            //System.out.println(val);
            if(val.longValue()==0){
              return new ExprResult(val);
            }else{
              throw new Exception("cannot convert bytes in addresses");
            }
          }


          PointerVariable pvar = new PointerVariable("tmp_convz",v.getAddress(),pt.getTargetType(),pt,pt.getLevelOfIndirection());
          return new ExprResult(pvar);
        case STRUCT:
          pvar = new PointerVariable("tmp_convz",v.getAddress(),pt.getTargetType(),pt,pt.getLevelOfIndirection());
          return new ExprResult(pvar);
        case FUNCPOINTER:
        case POINTER:
          if(pt.getTargetType()instanceof FunctionType){
            FuncPointerVariable kk;
            kk = new FuncPointerVariable("tmp_conpnt",v.getAddress(),pt.getTargetType(),pt,pt.getLevelOfIndirection());
            return new ExprResult(kk);
          }else{
            pvar = new PointerVariable("tmp_convz",v.getAddress(),pt.getTargetType(),pt,pt.getLevelOfIndirection());
          }
          return new ExprResult(pvar);


        default:
          throw new Exception("can not convert into pointer");
      }


    }
    break;
  case ENUM:
    res = handleRightSide(((IASTCastExpression)pRight).getOperand(), cur,pel);
    if(res.getResultType()== RType.Num){
      return res;
    }else{
      throw new Exception("not yet considered");
    }
  default:
    throw new Exception("NOT SUPPORTED CONVERSION");
  }

  return null;
}

/*
 * handle logical or
 */

private ExprResult handleOr(ExprResult pRes1, ExprResult pRes2, Scope pCur,InterpreterElement pel) throws Exception{
  //if first operand >1 return 1
  switch(pRes1.getResultType()){
  case Num:
    if(pRes1.getnumber().compareTo(BigInteger.ZERO)>0){
      return new ExprResult(BigInteger.ONE);
    }
   break;
  case Var:
    Variable v = pRes1.getVariable();
    switch(v.getTypeClass(pel)){
    case PRIMITIVE:
      PrimitiveVariable var = (PrimitiveVariable)v;
      BigInteger big1 = decodeVar(var,pel);
      if(big1.compareTo(BigInteger.ZERO)>0){
        return new ExprResult(BigInteger.ONE);
      }
      break;
    case POINTER:
      PointerVariable pvar = (PointerVariable)v;
      if(!pvar.isNullPointer(pel)){
        return new ExprResult(BigInteger.ONE);
      }
      break;
    case PRIMITIVEPNT:
      var = (PrimitiveVariable)v;
      MemoryBlock b = var.getAddress().getMemoryBlock();
      int offset = var.getAddress().getOffset();
      if(b.getAddress(offset,pel).getMemoryBlock()!=null){
        return new ExprResult(BigInteger.ONE);
      }
      break;
    default:
      throw new Exception("logical op not supported for var type");
    }
  default:
    throw new Exception("Unsupported Result Type");


  }




  switch(pRes2.getResultType()){
  case Num:
    if(pRes2.getnumber().compareTo(BigInteger.ZERO)>0){
      return new ExprResult(BigInteger.ONE);
    }else{
      return new ExprResult(BigInteger.ZERO);
    }

  case Var:
    Variable v = pRes2.getVariable();
    switch(v.getTypeClass(pel)){
    case PRIMITIVE:
      PrimitiveVariable var = (PrimitiveVariable)v;
      BigInteger big1 = decodeVar(var,pel);
      if(big1.compareTo(BigInteger.ZERO)>0){
        return new ExprResult(BigInteger.ONE);
      }else{
        return new ExprResult(BigInteger.ZERO);
      }

    case POINTER:
      PointerVariable pvar = (PointerVariable)v;
      if(!pvar.isNullPointer(pel)){
        return new ExprResult(BigInteger.ONE);
      }else{
        return new ExprResult(BigInteger.ZERO);
      }

    case PRIMITIVEPNT:
      var = (PrimitiveVariable)v;
      MemoryBlock b = var.getAddress().getMemoryBlock();
      int offset = var.getAddress().getOffset();
      if(b.getAddress(offset,pel).getMemoryBlock()!=null){
        return new ExprResult(BigInteger.ONE);
      }else{
        return new ExprResult(BigInteger.ZERO);
      }

    default:
      throw new Exception("logical op not supported for var type");
    }
  default:
    throw new Exception("Unsupported Result Type");


  }







}

/*
 * handles logical not return 1 if the value of res is 0 and 1 if the value of res is greater than 0
 */

private ExprResult  handleNot(ExprResult res, InterpreterElement pel) throws Exception {
  switch(res.getResultType()){
  case Addr:
    throw new Exception("inversion of addresses not allowed");

  case Var:
    Variable v = res.getVariable();
    switch(v.getTypeClass(pel)){
    case PRIMITIVE:
      PrimitiveVariable pvar = (PrimitiveVariable)v;
      BigInteger big1= decodeVar(pvar,pel);
      if(big1.compareTo(BigInteger.ZERO)==0){
        return new ExprResult(BigInteger.ONE);
      }else{
        return new ExprResult(BigInteger.ZERO);
      }

    default:
      throw new Exception("inversion of complex data types not allowed");
    }

  case Num:
    BigInteger big1 = res.getnumber();
    if(big1.compareTo(BigInteger.ZERO)==0){
      return new ExprResult(BigInteger.ONE);
    }else{
      return new ExprResult(BigInteger.ZERO);
    }





  default:
    break;
  }
  throw new Exception("NOT YET SUPPORTED");

}

/*
 * handles arithmetical "<"
 */

private ExprResult handleLessThan(ExprResult pRes1, ExprResult pRes2, Scope pCur, InterpreterElement pel) throws Exception {
  BigInteger big1;
  BigInteger big2;
  switch(pRes1.getResultType()){
  case Num:
    big1 = pRes1.getnumber();
    break;
  case Var:
    Variable v = pRes1.getVariable();
    if(v.getTypeClass(pel)== TypeClass.PRIMITIVE){
      big1 = decodeVar((PrimitiveVariable)v,pel);
    }else{
      throw new Exception("less than does not support pointers currently");
    }
    break;
  default:
    throw new Exception("addresses not supported in less");
  }

  switch(pRes2.getResultType()){
  case Num:
    big2 = pRes2.getnumber();
    break;
  case Var:
    Variable v = pRes2.getVariable();
    if(v.getTypeClass(pel)== TypeClass.PRIMITIVE){
      big2 = decodeVar((PrimitiveVariable)v,pel);
    }else{
      throw new Exception("less than does not support pointers");
    }
    break;
  default:
    throw new Exception("addresses not supported in less");
  }
  //System.out.println(big1);
  //System.out.println(big2);
  if(big1.compareTo(big2)<0){
    return new ExprResult(BigInteger.ONE);
  }else{
    return new ExprResult(BigInteger.ZERO);
  }



}



private ExprResult handleEquals(ExprResult pRes1, ExprResult pRes2, Scope pCur,InterpreterElement pel) throws Exception {




  switch(pRes1.getResultType()){
  case Num:
    switch(pRes2.getResultType()){
    case Num:
        if(pRes1.getnumber().compareTo(pRes2.getnumber())==0){
          return new ExprResult(BigInteger.valueOf(1));

        }else{
          return new ExprResult(BigInteger.valueOf(0));

        }

    case Var:
      return handleEquals(pRes2, pRes1, pCur,pel); //recursive call in order to decode variable without duplication of code


      /*Variable v = pRes2.getVariable();
      if(v.getTypeClass()==TypeClass.PRIMITIVE){
        BigInteger big2 = decodeVar((PrimitiveVariable)v);
        ExprResult expr2 = new ExprResult(big2);
        return handleEquals(pRes1, expr2, pCur);

      }else if(v.getTypeClass() == TypeClass.POINTER && pRes1.getnumber().compareTo(BigInteger.ZERO)==0) {
          PointerVariable pvar = (PointerVariable) v;
          if(pvar.isNullPointer() == true){
            return new ExprResult(BigInteger.ONE);
          }else{
            return new ExprResult(BigInteger.ZERO);
          }
      }
      else{
        //vorlauefiges throw;
        throw new Exception("Can not compare number with pointer");
      }*/

    case Addr:
      throw new Exception("Not supported yet");
    default:
      throw new Exception("Error in equality");
    }

  case Var: //decoding of variables regarding of argument position occur here (recursive call if position does not match)
    Variable v = pRes1.getVariable();

    if(v.getTypeClass(pel)==TypeClass.PRIMITIVE){
      PrimitiveVariable pvar =(PrimitiveVariable)v;
      BigInteger big1 = decodeVar(pvar,pel);
      ExprResult res1 = new ExprResult(big1);
      return handleEquals(pRes2, res1,  pCur,pel);
    }else if(v.getTypeClass(pel) == TypeClass.POINTER){
      PointerVariable pvar =(PointerVariable)v;
     ExprResult res1 = new ExprResult(pvar.getAddress(), pvar.getBaseType(), pvar.getlevel());
      return handleEquals(pRes2,res1,pCur,pel);
    }else if(v.getTypeClass(pel) == TypeClass.PRIMITIVEPNT){
      PrimitiveVariable pvar =(PrimitiveVariable)v;
      MemoryBlock b = pvar.getAddress().getMemoryBlock();
      int offset = pvar.getAddress().getOffset();
      ExprResult res1 = new ExprResult(b.getAddress(offset,pel), pvar.getType(), 1);
       return handleEquals(pRes2,res1,pCur,pel);
     }


    else{
      throw new Exception("not supported type of variable for comparsion");
    }

  case Addr:
    switch(pRes2.getResultType()){
    case Addr:
      if(pRes2.getAddress().getMemoryBlock() == pRes1.getAddress().getMemoryBlock()
          && pRes2.getAddress().getOffset()==pRes1.getAddress().getOffset()
          ){
        return new ExprResult(BigInteger.ONE);

      }else{
        return new ExprResult(BigInteger.ZERO);
      }

    case Var:
      return handleEquals(pRes2, pRes1, pCur,pel);
    case Num:
      MemoryBlock block = pRes1.getAddress().getMemoryBlock();
      int offset = pRes1.getAddress().getOffset();
      if(block.getCellType(offset,pel)==CellType.ADDR){
        if(block.getAddress(offset,pel).getMemoryBlock()== null){
          if(pRes2.getnumber().compareTo(BigInteger.ZERO)==0){
            return new ExprResult(BigInteger.ONE);

          }else{
            return new ExprResult(BigInteger.ZERO);

          }
        }else{
          return new ExprResult(BigInteger.ZERO);
        }
      }else{
        throw new Exception("Can only compare addresses not data cells");
      }
    }
    break;


    default:
      throw new Exception("Unsupported equality result");
  }
  return null;
}
/*
 * used to distinguish BINARY OPERATIONS
 */
enum BOP{
  AND,
  OR
}

/*
 * function for binary "or" and binary "and" using BOP
 */
private ExprResult handleBinaryOP(ExprResult pRes1, ExprResult pRes2,
    Scope pCur, InterpreterElement pel,BOP op) throws Exception {
  BigInteger big1;
  BigInteger big2;

  //get Result1 as BIGInteger
  switch(pRes1.getResultType()){
  case Num:
    big1 = pRes1.getnumber();
    break;
  case Var:
    Variable v = pRes1.getVariable();
    switch(v.getTypeClass(pel)){
    case PRIMITIVE:
      PrimitiveVariable pvar = (PrimitiveVariable)v;
      big1 = decodeVar(pvar,pel);
      break;
    default:
      throw new Exception("Binaryand not for complex data types");
    }
    break;
    default:
      throw new Exception("Binaryand not for addresses");

    }

  //get Result2 as BigInteger
  switch(pRes2.getResultType()){
  case Num:
    big2 = pRes2.getnumber();
    break;
  case Var:
    Variable v = pRes2.getVariable();
    switch(v.getTypeClass(pel)){
    case PRIMITIVE:
      PrimitiveVariable pvar = (PrimitiveVariable)v;
      big2 = decodeVar(pvar,pel);
      break;
    default:
      throw new Exception("Binaryand not for complex data types");
    }
    break;
    default:
      throw new Exception("Binaryand not for addresses");

    }
  //depending on opcode
  if(op ==BOP.AND)
    big1 = big1.and(big2);
  else
    big1  =big1.or(big2);
  return new ExprResult(big1);
}

/*
 * used to evaluate the lefthande side expr of an assignment
 * should either return a variable or a address but not data
 */
private ExprResult handleLeftSide(IASTExpression pLeft,
    Scope cur,InterpreterElement pel) throws Exception {
    ExprResult res;
    if(pLeft instanceof IASTIdExpression){
      Address z = cur.getVariable(((IASTIdExpression) pLeft).getName(),pel).getAddress();
      return new ExprResult(cur.getVariable(((IASTIdExpression) pLeft).getName(),pel));
    }
    if(pLeft instanceof IASTUnaryExpression){
      IASTUnaryExpression uexp = (IASTUnaryExpression) pLeft;
      switch(uexp.getOperator()){
      case STAR:
          res = handleLeftSide(uexp.getOperand(),cur,pel);
          switch(res.getResultType()){
          case Var:

            PointerVariable var = (PointerVariable)res.getVariable();
            MemoryBlock b =  var.getAddress().getMemoryBlock();
            int offset = var.getAddress().getOffset();
            return new ExprResult(b.getAddress(offset,pel),var.getBaseType(),var.getlevel()-1);
          case Addr:
            b= res.getAddress().getMemoryBlock();
            return new ExprResult(b.getAddress(res.getAddress().getOffset(),pel),res.getDataType(),res.getLevelofIndirection()-1);


          default:
            throw new Exception("STAR only on var and addr applicable");
          }

      default:
        throw new Exception("Operation not supported");
      }
    }

    //conversion happen here
    if(pLeft instanceof IASTCastExpression){
      IASTCastExpression expr = (IASTCastExpression)pLeft;
      IType type = expr.getExpressionType();
      Type p = getTypeCast(type,cur,pel); //used to retrieve the target type of the conversion
      ExprResult res1 = handleLeftSide(expr.getOperand(),cur,pel);
      switch(p.getTypeClass()){
      case POINTER: //only conversions in pointer interesting
        PointerType pnt= (PointerType)p;
        switch(res1.getResultType()){
        case Var:
          Variable v = res1.getVariable();
          Address a = v.getAddress();
          return new ExprResult(a,pnt.getTargetType(),pnt.getLevelOfIndirection());


        default:
          throw new Exception("not supported operand in conversion");
        }

      default:
        throw new Exception("not possible conversation");
      }

    }

  return null;
}
/*
 * used to retrieve Type information for a cast
 */
private Type getTypeCast(IType pExpressionType, Scope pCur,InterpreterElement pel) throws Exception {
  if(pExpressionType instanceof IASTSimpleDeclSpecifier){
      return getPrimitiveType((IASTSimpleDeclSpecifier)pExpressionType);
  }
  if(pExpressionType instanceof IASTPointerTypeSpecifier){
    return getPointerType((IASTPointerTypeSpecifier)pExpressionType,pCur,pel);
  }
  if(pExpressionType instanceof IComplexType){
    return pel.getCurrentScope().getCurrentEnums().getEnum(((IComplexType) pExpressionType).getName());
  }
  //System.out.println(pExpressionType);
  throw new Exception("Not supported");
}


/*
 * function is used for binary arithemtik operations like +,-,*,/
 * provides functionality for simple arithmetic operations or pointer calc
 * depending on Input
 */
private ExprResult handleBinaryOP(ExprResult pRes1, ExprResult pRes2,
    Scope pCur,NumOP op, InterpreterElement pel) throws Exception {
  BigInteger big1,big2;
  switch(pRes1.getResultType()){
  case Num:
    big1 = pRes1.getnumber();
    break;
  case Var:
    Variable v = pRes1.getVariable();
    switch(v.getTypeClass(pel)){
    case PRIMITIVE:
      PrimitiveVariable var = (PrimitiveVariable) v;
      big1 = decodeVar(var,pel);
      break;
    case POINTER:
    case PRIMITIVEPNT:
      if(op== NumOP.PLUS || op== NumOP.MINUS)
        return handlePntOP(pRes1.getVariable(),pRes2,op,pel);
      else
        throw new Exception("Pnt arithmetik only supported for +/-");



    default:
      throw new Exception("Currently not supported");
    }
    break;
  case Addr:
    if(op== NumOP.PLUS || op== NumOP.MINUS)
      return handleAddrPntOP(pRes1.getAddress(),pRes1.getDataType(),pRes2,op,pRes1.getLevelofIndirection(),pel);
    else
      throw new Exception("Pnt arithmetik only supported for +/-");

  default:
    throw new Exception("Unsupported");
  }


  switch(pRes2.getResultType()){
  case Num:
    big2 = pRes2.getnumber();
    break;
  case Var:
    Variable v = pRes2.getVariable();
    switch(v.getTypeClass(pel)){
    case PRIMITIVE:
      PrimitiveVariable var = (PrimitiveVariable) v;
      big2 = decodeVar(var,pel);
      break;
    case POINTER:
    case PRIMITIVEPNT:
      if(op== NumOP.PLUS || op== NumOP.MINUS)
        return handlePntOP(pRes2.getVariable(),pRes1,op,pel);
      else
        throw new Exception("Pnt arithmetik only supported for +/-");

    default:
      throw new Exception("Currently not supported");
    }

    break;
  case Addr:
    if(op== NumOP.PLUS || op== NumOP.MINUS)
      return handleAddrPntOP(pRes2.getAddress(),pRes2.getDataType(),pRes1,op,pRes2.getLevelofIndirection(),pel);
    else
      throw new Exception("Pnt arithmetik only supported for +/-");

  default:
    throw new Exception("Unsupported");
  }

  //System.out.println(op.toString() +": "+ big1 + " and " + big2);
  return new ExprResult( op.performCalc(big1, big2));
}
/*
 * performs point calc on Address input
 */
private ExprResult handleAddrPntOP(Address pAddress, Type pType,
    ExprResult pRes1, NumOP op, int level, InterpreterElement pel) throws Exception {



  switch(pRes1.getResultType()){
  case Var:
    PrimitiveVariable var = (PrimitiveVariable)pRes1.getVariable();
    pRes1= new ExprResult(decodeVar(var,pel));
    //return handleAddrPntOP(pAddress,pType, expr,op,level,pel);
  case Num:
    Address v = new Address(pAddress.getMemoryBlock(),op.performCalc(pAddress.getOffset(), pRes1.getnumber().intValue()));
    return new ExprResult(v,pType,level);
  default:
    throw new Exception("Not supported pointercalc");
  }

}

/*
 * performs pointercalc on variable as input
 */
private ExprResult handlePntOP(Variable pV, ExprResult pRes2, NumOP op, InterpreterElement pel) throws Exception {

  Address addr = pV.getAddress();
  switch(pRes2.getResultType()){
  case Var:
    PrimitiveVariable var = (PrimitiveVariable)pRes2.getVariable();
    ExprResult expr = new ExprResult(decodeVar(var,pel));
    return handlePntOP(pV, expr,op,pel);
  case Num:
    Address naddr;
    int big1 =pRes2.getnumber().intValue();
    int pnt=big1;
    //depending on data type perform offset calc in bytes
    if (pV.getTypeClass(pel) == TypeClass.POINTER){ //pointer has size 4
      PointerVariable pvar = (PointerVariable)pV;
      //PointerVariable pvar = (PointerVariable)pV;
      if(pvar.getlevel()>1){
        pnt= big1*4;
      }else{ //pointing to non-pointer var get size of var and perform offest calc
        pnt = big1*pvar.getBaseType().sizeOf();
      }
    }
    int base =addr.getMemoryBlock().getAddress(addr.getOffset(),pel).getOffset();
    naddr = new Address(addr.getMemoryBlock().getAddress(addr.getOffset(),pel).getMemoryBlock(),op.performCalc(base, pnt));
    if(pV.getTypeClass(pel)==TypeClass.PRIMITIVEPNT){
      return new ExprResult(naddr,pV.getType(),1);
    }else if(pV.getTypeClass(pel) == TypeClass.POINTER){
      return new ExprResult(naddr,((PointerVariable)pV).getBaseType(),((PointerVariable)pV).getlevel());
    }
  default:
    throw new Exception("Not supported pointercalc");
  }
  //TODO: ADD integrity check

}
/*
 * used to copy the first pSize bytes of var1 to var2
 */
private void copyVar(Address pAddress, Address pAddress2, int pSize,InterpreterElement pel) throws Exception {
    int offset, offset2;
    MemoryBlock block, block2;

    offset = pAddress.getOffset();
    offset2 = pAddress2.getOffset();

    block = pAddress.getMemoryBlock();
    block2 = pAddress2.getMemoryBlock();

    for(int x=0;x<pSize;x++){
      switch(block2.getCellType(offset2+x,pel)){
      case EMPTY:
        //TODO: analyze behavior of composite variables
        break;
      case DATA:
        block.setData(offset+x, block2.getData(offset2+x,pel),pel);

        break;
      case ADDR:
        block.setAddress(offset+x, block2.getAddress(offset2+x,pel),pel);
        break;
      case FUNC:
        block.setFunctionPointer(offset+x, block2.getFunctionPointer(offset2+x,pel),pel);
        break;
      default:
      throw new Exception("not supported yet");
    }
    }


}
/*
 * checks if 2 types do coincide by comparing their designated string
 */
private boolean checkType(Type pTyp, Type pTyp2) {




  if(pTyp.getDefinition().compareTo(pTyp2.getDefinition())==0)
    return true;
  return false;


}

/*
 * used to write a integervalue into a enum memblock
 */
private void writeEnumVar(Address pAddr, BigInteger pGetnumber,
    InterpreterElement pel) {
  // TODO Value Check
  MemoryBlock block = pAddr.getMemoryBlock();
  int offset = pAddr.getOffset();
  byte []data = new byte[4];
  for(int x =0; x<4;x++){
    BigInteger tmp = pGetnumber.and(BigInteger.valueOf(255));
    try {
      block.setData(offset+x,  tmp.byteValue(),pel);
    } catch (RuntimeException e) {
      e.printStackTrace();
    }
    pGetnumber= pGetnumber.shiftRight(8);

  }



}


/*
 * writes a number into a primitive memblock location
 */
private void writePrimVar(PrimitiveType pTyp, Address pAddr, BigInteger pGetnumber,InterpreterElement pel) throws Exception {
  int exp;
  exp = pTyp.sizeOf();
  BigInteger numb = BigInteger.valueOf(2);
  numb =numb.pow(exp*8);

 //TODO: verify  signed unsigned behavior
  if(pTyp.isSigned()){
    pGetnumber = pGetnumber.remainder(numb);
  }else{
    pGetnumber = pGetnumber.mod(numb);

  }
  MemoryBlock block = pAddr.getMemoryBlock();
  int offset = pAddr.getOffset();
  byte []data = new byte[exp];
  for(int x =0; x<exp;x++){
    BigInteger tmp = pGetnumber.and(BigInteger.valueOf(255));

      block.setData(offset+x,  tmp.byteValue(),pel);

    pGetnumber= pGetnumber.shiftRight(8);

  }


}

/*
 * used to read a integer value of a primitive var location
 */
private BigInteger decodeVar(PrimitiveVariable pVar, InterpreterElement pel) throws Exception{
  if(pVar.getName().equals("__BLAST_NONDET")){
    return pel.getNonDetNumber();
  }
  byte data[] = new byte[pVar.getSize()];
  MemoryBlock mem = pVar.getAddress().getMemoryBlock();
  int offset = pVar.getAddress().getOffset();
  for(int x=0;x<data.length;x++){

      data[x]= mem.getData(x+offset,pel);

  }
  BigInteger var;
  if(pVar.isSigned()){
    if((data[data.length-1]&-128)==-128){
      var = BigInteger.valueOf(0);
      for(int x = data.length-1; x>=0;x--){
        var= var.or(BigInteger.valueOf(data[x]).not().mod(BigInteger.valueOf(256)));

        if(x!=0){
          var = var.shiftLeft(8);
        }
      }
      var =var.not();
    }else{
      var = BigInteger.valueOf(0);
      for(int x = data.length-1; x>=0;x--){
        var= var.or(BigInteger.valueOf(data[x]).mod(BigInteger.valueOf(255)));
        if(x!=0)
          var = var.shiftLeft(8);
      }
    }

  }else{
    var = BigInteger.valueOf(0);
    for(int x = data.length-1; x>=0;x--){
      var= var.or(BigInteger.valueOf(data[x]).and(BigInteger.valueOf(255)));

      if(x!=0)
        var = var.shiftLeft(8);
    }
  }


  return var;
}

/*
 * main entry point for variable declaration
 */
public void handleDeclaration(DeclarationEdge pDeclarationEdge,
    InterpreterElement pElement) throws Exception{
 if(pDeclarationEdge.getName().startsWith("__BLAST_NONDET")){
    return;

  }
  //typedef declaration is handled here
  if(pDeclarationEdge.getStorageClass().name().compareTo("TYPEDEF")==0){
    String tmp = pDeclarationEdge.getName();
    IType spec = pDeclarationEdge.getDeclSpecifier();
    pElement.getCurrentScope().getCurrentDefinitions().addDefinition(tmp,spec );
    return;
  }

  IType typ = pDeclarationEdge.getDeclSpecifier();
 String name = pDeclarationEdge.getName();
 handleTypeDecl(name,pElement,typ);






}
/*
 * handles:
 * 1. SimpleDecl (PrimitiveVariables)
 * 2. ArrayDecl
 * 3. StructUnionDecl
 * 4. StructUnionVariable Declaration
 * 5. Enum
 * 6. EnumVariable declaration
 * 7. PointerDeclarations
 */
private void handleTypeDecl(String name, InterpreterElement pElement,
    IType typ) throws Exception {
  if(typ instanceof  IASTSimpleDeclSpecifier){
    handleSimpleDecl((IASTSimpleDeclSpecifier)typ,name, pElement);
  }
  if(typ instanceof IASTPointerTypeSpecifier){
    try {
      IASTPointerTypeSpecifier ptype = (IASTPointerTypeSpecifier)typ;

      handlePointerDecl(ptype, name,pElement);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  if(typ instanceof IASTArrayTypeSpecifier){
    handleArrayDecl((IASTArrayTypeSpecifier)typ,name,pElement);
  }
  if(typ instanceof IASTCompositeTypeSpecifier){//struct und co definition
     IASTCompositeTypeSpecifier comptyp = (IASTCompositeTypeSpecifier)typ;
         allocateDynType(comptyp,pElement);



     handleDynVarDef(name,comptyp.getName(),pElement,comptyp.isConst());

  }

  if(typ instanceof IASTElaboratedTypeSpecifier){
    IASTElaboratedTypeSpecifier def = (IASTElaboratedTypeSpecifier)typ;
    if(((IASTElaboratedTypeSpecifier) typ).getKind()>0){//defintion von struct variablen
      CompositeType comp = pElement.getCurrentScope().getCurrentTypeLibrary().getType(((IASTElaboratedTypeSpecifier) typ).getName());
      if(comp == null){
        //Forward declaration are handled here, empty Struct Types are added to the TypeLibary which holds structs and unions
        if(def.getKind()==1){
          StructType k = new StructType(((IASTElaboratedTypeSpecifier) typ).getName(),false);
          pElement.getCurrentScope().getCurrentTypeLibrary().addType(k);
        }else if(def.getKind()==2){
          UnionType k = new UnionType(((IASTElaboratedTypeSpecifier) typ).getName(),false);
          pElement.getCurrentScope().getCurrentTypeLibrary().addType(k);
        }
        return;
      }
      if(comp.getMembers().size()>0)
        handleDynVarDef(name,((IASTElaboratedTypeSpecifier) typ).getName(),pElement,typ.isConst());
    }else{//enumvar defintion
      handleEnumVarDef(name,((IASTElaboratedTypeSpecifier) typ).getName(),pElement,typ.isConst());
    }
  }

  if(typ instanceof IASTNamedTypeSpecifier){ //used for Typedefs
    IASTNamedTypeSpecifier ntyp = (IASTNamedTypeSpecifier)typ;
    IType def =pElement.getCurrentScope().getCurrentDefinitions().getDefinition(ntyp.getName());
    handleTypeDecl(name, pElement, def);

  }

  if(typ instanceof IASTEnumerationSpecifier ){ //Enum definition
    IASTEnumerationSpecifier etyp = (IASTEnumerationSpecifier)typ;
    EnumType ntyp = new EnumType(etyp.getName(),false);
    IASTEnumerator[] members = etyp.getEnumerators();
    int x=0;
    for(x=0;x<members.length;x++){
      ntyp.addEnumerator(members[x].getName(), members[x].getValue());
    }
    pElement.getCurrentScope().getCurrentEnums().addEnum(etyp.getName(), ntyp);

  }

}



private void handleEnumVarDef(String name, String label,
    InterpreterElement pElement, boolean pConst) {
  MemoryBlock data = pElement.getFactory().allocateMemoryBlock(4, pElement);
  Address addr = new Address(data,0);

  EnumType type = pElement.getCurrentScope().getCurrentEnums().getEnum(label); //lookup enum type
  EnumVariable variable = new EnumVariable(name, addr, type,pConst);
  pElement.getCurrentScope().addVariable(variable, pElement);
}


/*
 * used to define the variable for struct and unions
 */
private void handleDynVarDef(String name, String label, InterpreterElement pel,boolean isConst) throws Exception {
  CompositeType comp = pel.getCurrentScope().getCurrentTypeLibrary().getType(label);//Type lookingup in currenttypelibary

  if(name == null){
    int size = comp.sizeOf();
    return;
  }
  int size = comp.sizeOf();
  MemoryBlock data = pel.getFactory().allocateMemoryBlock(size,pel);
  Address addr = new Address(data,0);
  switch(comp.getTypeClass()){
  case STRUCT:
    DynVariable var = new DynVariable(name,addr,comp,isConst,dyntypes.STRUCT);
    pel.getCurrentScope().addVariable(var,pel);
    break;
  case UNION:
    var = new DynVariable(name,addr,comp,isConst,dyntypes.UNION);
    pel.getCurrentScope().addVariable(var,pel);
    break;
   default:
     throw new Exception("Not supported dynamic variable type");
  }

}


/*
 * used to fully register struct and union types (opposion to forward decl)
 */
private CompositeType allocateDynType(IASTCompositeTypeSpecifier pComptyp,
    InterpreterElement pElement) throws Exception {
  CompositeType tmp;
  tmp = pElement.getCurrentScope().getCurrentTypeLibrary().getType(pComptyp.getName());
  if(tmp == null){ //if not forward decl create type
    if( pComptyp.getKey() == IASTCompositeTypeSpecifier.k_struct){

      tmp= new StructType(pComptyp.getName(),false);
    }else {
      tmp = new UnionType(pComptyp.getName(),false);
    }
    pElement.getCurrentScope().getCurrentTypeLibrary().addType(tmp);
  }
  //add members types to dyn data structure
  Iterator<IASTCompositeTypeMemberDeclaration> it = pComptyp.getMembers().iterator();
  while(it.hasNext()){
    Type cur;
    IASTCompositeTypeMemberDeclaration el = it.next();
   IType memb = el.getDeclSpecifier();
   if(memb instanceof IASTSimpleDeclSpecifier){
     cur = getPrimitiveType((IASTSimpleDeclSpecifier) memb);
   }
   else if(memb instanceof IASTPointerTypeSpecifier){
     cur = getPointerType((IASTPointerTypeSpecifier) memb,pElement.getCurrentScope(),pElement);
   }
   else if(memb instanceof IASTArrayTypeSpecifier){
     cur = getArrayType((IASTArrayTypeSpecifier) memb, pElement.getCurrentScope(),pElement);
   }else if(memb instanceof IASTElaboratedTypeSpecifier){
     IASTElaboratedTypeSpecifier ememb = (IASTElaboratedTypeSpecifier)memb;
     if(ememb.getKind()>0){ //>0 struct and unions
     cur = pElement.getCurrentScope().getCurrentTypeLibrary().getType(ememb.getName());
     }else{ //=0 enums
       cur = pElement.getCurrentScope().getCurrentEnums().getEnum(ememb.getName());
     }
   }else if (memb instanceof IASTNamedTypeSpecifier){

     IASTNamedTypeSpecifier ntmp = (IASTNamedTypeSpecifier)memb;
     cur = getNamedType(pElement.getCurrentScope().getCurrentDefinitions().getDefinition(ntmp.getName()), pElement.getCurrentScope(), pElement);

   }else{
     throw new Exception("NOT YET SUPPORTED");
   }
   tmp.addMember(el.getName(), cur);
  }

  return tmp;
}

/*
 * used to handle the declaration of an array
 */

private void handleArrayDecl(IASTArrayTypeSpecifier pTyp, String pName,
    InterpreterElement pel) throws Exception {
    ArrayType typ=getArrayType(pTyp,pel.getCurrentScope(),pel);
    int length = typ.length();
    MemoryBlock b = pel.getFactory().allocateMemoryBlock(typ.sizeOf(),pel);
    Address addr = new Address(b,0);
    ArrayVariable var = new ArrayVariable(pName, addr, length, typ, typ.getType());

    pel.getCurrentScope().addVariable(var,pel);

}

/*
 * used to handle the declaration of a pointer
 */

private void handlePointerDecl(IASTPointerTypeSpecifier pTyp,
    String pName, InterpreterElement pel) throws Exception{

    PointerType typ = getPointerType(pTyp,pel.getCurrentScope(),pel);
    allocatePointerVar(pName,typ,pel);


}

/*
 * used to declare a pointer variable (functionpointer and datapointer) given a certain pointer type
 * also used during copying of variables
 */
private void allocatePointerVar( String pName,PointerType typ,InterpreterElement pel){
  MemoryBlock block = pel.getFactory().allocateMemoryBlock(4,pel);
  Address addr = new Address(block, 0);
  if(typ.getTargetType() instanceof FunctionType){
    FuncPointerVariable var = new FuncPointerVariable(pName, addr, typ.getTargetType(), typ, typ.getLevelOfIndirection());
    pel.getCurrentScope().addVariable(var,pel);
  }else{
    PointerVariable var = new PointerVariable(pName, addr, typ.getTargetType(), typ, typ.getLevelOfIndirection());
    pel.getCurrentScope().addVariable(var,pel);
  }
  return;
}

/*
 * used to declare primitive variables like int or long
 */
public void handleSimpleDecl(IASTSimpleDeclSpecifier pTyp,
    String name, InterpreterElement pel) {



  PrimitiveType ptyp = getPrimitiveType(pTyp);
  allocSimpleVar(name,ptyp,pel);


}


private void allocSimpleVar(String name,PrimitiveType ptyp,InterpreterElement pel){
  Address addr;
  MemoryBlock block;
  PrimitiveVariable pVar;
  if(ptyp.getPrimitive() != null){
    block = pel.getFactory().allocateMemoryBlock(ptyp.getPrimitive().sizeOf(),pel);
    addr = new Address(block,0);

    pVar = new PrimitiveVariable(name, addr,  ptyp, ptyp.isSigned(),ptyp.isConst());
    pel.getCurrentScope().addVariable(pVar,pel);
  }else{
    try {
      throw new Exception("Unsupported primitive Data Type");
    } catch (Exception e) {

      e.printStackTrace();
    }
  }
}



private ArrayType getArrayType(IASTArrayTypeSpecifier pTyp,
    Scope scope,InterpreterElement pel) throws Exception {
  IType typ=pTyp;
  IASTArrayTypeSpecifier tmp=null;
  int length = 1;
  int dim=0;
  int x=1;
  ExprResult res = null;
  //used to determine the size and dimension of the memory block
  while(typ instanceof IASTArrayTypeSpecifier){
    tmp = (IASTArrayTypeSpecifier)typ;

    res = handleRightSide(tmp.getLength(),scope, pel);
    switch(res.getResultType()){
    case Num:
      x= res.getnumber().intValue();
      break;
    case Var:
      Variable v = res.getVariable();
      switch(v.getTypeClass(pel)){
        case PRIMITIVE:
            x= decodeVar((PrimitiveVariable)v,pel).intValue();
            break;
        default:
          throw new Exception("unknown index field: variable type not allowed");
      }
      break;
    case Addr:
    default:
      throw new Exception("not supported yet");
    }
    length *=x;
    dim++;
    typ = tmp.getType();


  }

  Type basetype;
  //here the base type of the array is determined
  if(typ instanceof IASTSimpleDeclSpecifier){
    basetype = getPrimitiveType((IASTSimpleDeclSpecifier) typ);
  }else if(typ instanceof IASTPointerTypeSpecifier)
    basetype = getPointerType((IASTPointerTypeSpecifier) typ,scope,pel);
  else if (typ instanceof IASTNamedTypeSpecifier ){

    String name = ((IASTNamedTypeSpecifier)typ).getName();
    IType t  = scope.getCurrentDefinitions().getDefinition(name);
    basetype = getNamedType(t,scope,pel);
    if(basetype instanceof ArrayType){
      length *= ((ArrayType)basetype).length();
    }

  }else if(typ instanceof IASTElaboratedTypeSpecifier){ //TODO:Enums
    basetype = scope.getCurrentTypeLibrary().getType(((IASTElaboratedTypeSpecifier) typ).getName());

  }else{

    throw new Exception("not yet supported");
  }

  //System.out.println("LENGTH " +length);
  return new ArrayType(basetype,length);
}


/*
 * used for typedefs makes use of all getType functions
 */
private Type getNamedType(IType t,Scope scope,InterpreterElement pel) throws Exception {

  if(t instanceof IASTArrayTypeSpecifier){
    return getArrayType((IASTArrayTypeSpecifier) t, scope, pel);
  }
  if(t instanceof IASTSimpleDeclSpecifier){
    return getPrimitiveType((IASTSimpleDeclSpecifier) t);
  }
  if(t instanceof IASTPointerTypeSpecifier){
    return getPointerType((IASTPointerTypeSpecifier) t, scope,pel);
  }
  if(t instanceof IASTCompositeTypeSpecifier){ //struct and unions
    return scope.getCurrentTypeLibrary().getType(((IASTCompositeTypeSpecifier) t).getName());
  }
  if(t instanceof IASTElaboratedTypeSpecifier){
    IASTElaboratedTypeSpecifier tt = (IASTElaboratedTypeSpecifier)t;
    if(tt.getKind()>0) //struct and unions
      return scope.getCurrentTypeLibrary().getType(tt.getName());
    else //enum
      return scope.getCurrentEnums().getEnum(tt.getName());
  }
  if(t instanceof IASTNamedTypeSpecifier){ //recursive typedef
    String f =((IASTNamedTypeSpecifier) t).getName();
    IType def = scope.getCurrentDefinitions().getDefinition(f);
    return getNamedType(def, scope,pel);
  }
  if (t instanceof IASTFunctionTypeSpecifier) { //function
    IASTFunctionTypeSpecifier ft = (IASTFunctionTypeSpecifier) t;
    return getFunctionType(ft,scope,pel);

  }

 throw new Exception("could not handle definition "+t.toASTString() );
}



private FunctionType getFunctionType(IASTFunctionTypeSpecifier ft, Scope pscope,
    InterpreterElement pel) throws Exception {
  String funcname = ft.getName();
  IType rettype = ft.getReturnType();
  Type res;

  //get the return type
  if(rettype instanceof IASTSimpleDeclSpecifier){
    res = getPrimitiveType((IASTSimpleDeclSpecifier) rettype);
  }else if(rettype instanceof IASTPointerTypeSpecifier){
    res = getPointerType((IASTPointerTypeSpecifier) rettype, pscope, pel);
  }else if(rettype instanceof IASTArrayTypeSpecifier){
    res = getArrayType((IASTArrayTypeSpecifier) rettype, pscope, pel);
  }else  if(rettype instanceof IASTElaboratedTypeSpecifier){
    IASTElaboratedTypeSpecifier tt = (IASTElaboratedTypeSpecifier)rettype;
    if(tt.getKind()>0)
      res= pscope.getCurrentTypeLibrary().getType(tt.getName());
    else
      res= pscope.getCurrentEnums().getEnum(tt.getName());
  }else if(rettype instanceof IASTCompositeTypeSpecifier){
    res =pscope.getCurrentTypeLibrary().getType(((IASTCompositeTypeSpecifier) rettype).getName());
  }else{
    throw new Exception("Not considered return type for a function");
  }

  FunctionType functype = new FunctionType(funcname,res,false);
  return functype;
}



public PointerType getPointerType(IASTPointerTypeSpecifier pTyp, Scope scop,InterpreterElement pel) throws Exception{
  boolean isConst;
  isConst = pTyp.isConst();
  int level=1;
  IType tmp = (pTyp).getType();
  while(tmp instanceof IASTPointerTypeSpecifier){
    level++;
    tmp = ((IASTPointerTypeSpecifier) tmp).getType();
  }


  if(tmp instanceof IASTSimpleDeclSpecifier){
    Primitive prim=null;
    IASTSimpleDeclSpecifier ptmp = (IASTSimpleDeclSpecifier)tmp;

    /*switch(ptmp.getType()){
    case INT:
      prim = Primitive.LONG;
      break;
    case BOOL:
      break;
    case CHAR:
      prim = Primitive.CHAR;
      break;
    case FLOAT:
      prim = Primitive.FLOAT;
      break;
    case DOUBLE:
      prim = Primitive.DOUBLE;
      break;
    default:
    break;

    }
    if(ptmp.isLong()){
      prim = Primitive.LONGLONG;
    }else if(ptmp.isShort()){
      prim = Primitive.SHORT;

    }
    if(prim == null){
      throw new Exception("Unsupported primitive Base Type");
    }
    PrimitiveType basetype = new PrimitiveType(prim,ptmp.isSigned() ,isConst);*/
    PrimitiveType  basetype =getPrimitiveType(ptmp);
   return new PointerType(basetype, isConst, level);

  }else if (tmp instanceof IASTElaboratedTypeSpecifier){
    Type basetype;
    IASTElaboratedTypeSpecifier ctmp = (IASTElaboratedTypeSpecifier)tmp;
    if(ctmp.getKind()>0){ //structs and unions

       basetype = scop.getCurrentTypeLibrary().getType(ctmp.getName());
      if(basetype == null){
        //System.out.println("WHAT");
      }

    }else{ //enum
      basetype= scop.getCurrentEnums().getEnum(ctmp.getName());
    }
    return new PointerType(basetype, isConst, level);

  }else if (tmp instanceof IASTNamedTypeSpecifier){ //typedef
    IASTNamedTypeSpecifier ntmp = (IASTNamedTypeSpecifier)tmp;
    Type basetype = getNamedType(scop.getCurrentDefinitions().getDefinition(ntmp.getName()), scop, pel);
    while(basetype instanceof PointerType){ //if basetype is pointer recalc level and get new basetype
      level +=((PointerType) basetype).getLevelOfIndirection();
      basetype = ((PointerType) basetype).getTargetType();

    }
    return new PointerType(basetype, isConst,level);

  }
  else if (tmp instanceof IComplexType){ //used for unions struct and typedefs

    IComplexType ctmp = (IComplexType)tmp;
    Type basetype = scop.getCurrentTypeLibrary().getType(ctmp.getName());
    if(basetype == null){
       IType xx = scop.getCurrentDefinitions().getDefinition(ctmp.getName());

       basetype = getNamedType(xx, scop, pel);
       while(basetype instanceof PointerType){//if basetype is pointer recalc level and get new basetype
         level +=((PointerType) basetype).getLevelOfIndirection();
         basetype = ((PointerType) basetype).getTargetType();

       }
    }
    return new PointerType(basetype, isConst, level);

  }else if(tmp instanceof IASTFunctionTypeSpecifier){//Function Pointer
    IASTFunctionTypeSpecifier ftmp = (IASTFunctionTypeSpecifier)tmp;


   Type basetype = new FunctionType("func",null,false);
  return new PointerType(basetype, isConst, level);
  }else if (tmp instanceof DummyType) {

    Type basetype = new FunctionType("func",null,false);
    return new PointerType(basetype, isConst, level);
  }
  else{
    //System.out.println(tmp);
    throw new Exception("BASE TYpe not yet supported");
  }

}



public PrimitiveType getPrimitiveType(IASTSimpleDeclSpecifier pTyp){
  boolean isSigned;
  boolean isConst;
  isSigned = pTyp.isSigned();
  pTyp.isUnsigned();


  isConst = pTyp.isConst();
  Primitive typ = null;

  switch(pTyp.getType()){

  case INT:
    //TODO:LONG and LONGLONG what is what?
    typ = Primitive.LONG;

    break;
  case BOOL:
    break;
  case CHAR:
    typ = Primitive.CHAR;

    break;
  case VOID:
    typ = Primitive.VOID;
    break;
  case FLOAT:
    typ = Primitive.FLOAT;

    break;
  case DOUBLE:
    typ = Primitive.DOUBLE;

    break;
  case UNSPECIFIED:
  default:
    break;
  }

  if(pTyp.isLong()||pTyp.isLongLong()){
    typ = Primitive.LONGLONG;

  }else if(pTyp.isShort()){

    typ = Primitive.SHORT;

  }else{


  }
  PrimitiveType k;
  k= new PrimitiveType(typ,isSigned,isConst);
  return k;
}




  private Long parseLiteral(IASTExpression expression) throws UnrecognizedCCodeException {
    if (expression instanceof IASTLiteralExpression) {

      int typeOfLiteral = ((IASTLiteralExpression)expression).getKind();
      if (typeOfLiteral == IASTLiteralExpression.lk_integer_constant) {

        String s = expression.getRawSignature();
        if(s.endsWith("L") || s.endsWith("U") || s.endsWith("UL")){
          s = s.replace("L", "");
          s = s.replace("U", "");
          s = s.replace("UL", "");
        }
        try {
          return Long.valueOf(s);
        } catch (NumberFormatException e) {
          throw new UnrecognizedCCodeException("invalid integer literal", null, expression);
        }
      }
      if (typeOfLiteral == IASTLiteralExpression.lk_string_literal) {
        return (long) expression.hashCode();
      }
    }
    return null;
  }

  public String getvarName(String variableName, String functionName){
    if(globalVars.contains(variableName)){
      return variableName;
    }
    return functionName + "::" + variableName;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
                                    List<AbstractElement> elements,
                                    CFAEdge cfaEdge, Precision precision) throws UnrecognizedCCodeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    assert element instanceof InterpreterElement;
    InterpreterElement explicitElement = (InterpreterElement)element;

    for (AbstractElement ae : elements) {
      if (ae instanceof PointerElement) {
        return strengthen(explicitElement, (PointerElement)ae, cfaEdge, precision);
      }
      else if (ae instanceof ConstrainedAssumeElement) {
        return strengthen(cfaEdge.getSuccessor(), explicitElement, (ConstrainedAssumeElement)ae, precision);
      }
      else if (ae instanceof GuardedEdgeAutomatonPredicateElement) {
        return strengthen(cfaEdge.getSuccessor(), explicitElement, (GuardedEdgeAutomatonPredicateElement)ae, precision);
      }
    }
    return null;
  }

  private Collection<? extends AbstractElement> strengthen(InterpreterElement explicitElement,
      PointerElement pointerElement, CFAEdge cfaEdge, Precision precision) throws UnrecognizedCCodeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    if (true)
      throw new RuntimeException("The computer says nice!");

    return null;
  }

  private String derefPointerToVariable(PointerElement pointerElement,
                                        String pointer) {
    Pointer p = pointerElement.lookupPointer(pointer);
    if (p != null && p.getNumberOfTargets() == 1) {
      Memory.PointerTarget target = p.getFirstTarget();
      if (target instanceof Memory.Variable) {
        return ((Memory.Variable)target).getVarName();
      } else if (target instanceof Memory.StackArrayCell) {
        return ((Memory.StackArrayCell)target).getVarName();
      }
    }
    return null;
  }

  public Collection<? extends AbstractElement> strengthen(CFANode pNode, InterpreterElement pElement, GuardedEdgeAutomatonPredicateElement pAutomatonElement, Precision pPrecision) {
    AbstractElement lResultElement = pElement;

    for (ECPPredicate lPredicate : pAutomatonElement) {
      AssumeEdge lEdge = ToFlleShAssumeEdgeTranslator.translate(pNode, lPredicate);

      try {
        Collection<? extends AbstractElement> lResult = getAbstractSuccessors(lResultElement, pPrecision, lEdge);

        if (true)
          throw new RuntimeException("The computer says f...ine!");

        if (lResult.size() == 0) {
          return Collections.emptySet();
        }
        else if (lResult.size() == 1) {
          lResultElement = lResult.iterator().next();
        }
        else {
          throw new RuntimeException();
        }

        return lResult;
      } catch (CPATransferException e) {
        throw new RuntimeException(e);
      }
    }

    return Collections.singleton(lResultElement);
  }

  public Collection<? extends AbstractElement> strengthen(CFANode pNode, InterpreterElement pElement, ConstrainedAssumeElement pAssumeElement, Precision pPrecision) {
    AssumeEdge lEdge = new AssumeEdge(pAssumeElement.getExpression().getRawSignature(), pNode.getLineNumber(), pNode, pNode, pAssumeElement.getExpression(), true);

    try {
      Collection<? extends AbstractElement> lResult = getAbstractSuccessors(pElement, pPrecision, lEdge);

      if (true)
        throw new RuntimeException("The computer says damn!");

      return lResult;
    } catch (CPATransferException e) {
      throw new RuntimeException(e);
    }
  }

}
