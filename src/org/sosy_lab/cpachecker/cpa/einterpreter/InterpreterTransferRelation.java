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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.DummyType;
import org.sosy_lab.cpachecker.cfa.ast.IASTArrayTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTCompositeTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTElaboratedTypeSpecifier;
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
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Address;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.ArrayVariable;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.DynVariable;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.DynVariable.dyntypes;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.FuncPointerVariable;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.MemoryBlock;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.MemoryBlock.CellType;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.MemoryException;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.MemoryFactory;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.PointerVariable;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.PrimitiveVariable;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Scope;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.ArrayType;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.CompositeType;
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
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.fshell.fql2.translators.cfa.ToFlleShAssumeEdgeTranslator;
import org.sosy_lab.cpachecker.util.ecp.ECPPredicate;


@SuppressWarnings("unused")
public class InterpreterTransferRelation implements TransferRelation {

  private final Set<String> globalVars = new HashSet<String>();

  private String missingInformationLeftVariable = null;
  private String missingInformationRightPointer = null;
  private String missingInformationLeftPointer  = null;
  private IASTExpression missingInformationRightExpression = null;
  private Map<String, CFAFunctionDefinitionNode>  map= null;

  InterpreterTransferRelation(Map<String, CFAFunctionDefinitionNode> pmap){
    map = pmap;
  }


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
    InterpreterElement successor ;
    AbstractElement check = null;
    InterpreterElement explicitElement = (InterpreterElement)element;

    //successor = explicitElement;
    successor = explicitElement.copy();

    // check the type of the edge
     System.out.println(cfaEdge.getRawStatement());
    /*System.out.println(cfaEdge.getEdgeType());
    System.out.println(cfaEdge.getPredecessor());
    System.out.println(cfaEdge.getPredecessor().getNumLeavingEdges());
    System.out.println(cfaEdge.getSuccessor().getLeavingEdge(0));*/
    switch (cfaEdge.getEdgeType ()) {

    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge: {

      StatementEdge statementEdge = (StatementEdge) cfaEdge;
      try {
        handleStatement(statementEdge,successor);
      } catch (Exception e) {
       e.printStackTrace();
      }




      check = successor;
      break;
    }

    case ReturnStatementEdge: {
      ReturnStatementEdge returnEdge = (ReturnStatementEdge)cfaEdge;
      IASTExpression exp = successor.getCurrentScope().getReturnExpression();
      if(exp!=null){
        try {
          ExprResult res = handleLeftSide(exp, successor.getCurrentScope().getParentScope(),successor.getFactory());
          ExprResult res2  = handleRightSide(returnEdge.getExpression(),successor.getCurrentScope(),successor.getFactory());
          handleAssignment(res,res2);

        } catch (Exception e) {
         e.printStackTrace();
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

      DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;
      try {
        handleDeclaration(declarationEdge,successor);
      } catch (Exception e) {
       e.printStackTrace();
      }
      check = successor;
      break;
    }

    // this is an assumption, e.g. if(a == b)
    case AssumeEdge: {
      AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
      check = handleAssume(assumeEdge, successor);

      break;
    }

    case BlankEdge: {

      break;
    }

    case FunctionCallEdge: {
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) cfaEdge;
     try {
      handleFunctionCall(functionCallEdge.getRawAST(),functionCallEdge.getSuccessor(),functionCallEdge.getArguments(),successor);
    } catch (Exception e) {
     e.printStackTrace();
    }
    check = successor;


      break;
    }

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case FunctionReturnEdge: {
      FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge) cfaEdge;

      successor.redScope();
      check = successor;
      break;
    }
    case FunctionPntReturnEdge:
      check = InterpreterBottomElement.INSTANCE;
      FunctionPntReturnEdge fpredge = (FunctionPntReturnEdge) cfaEdge;
      CallToReturnEdge abbr = fpredge.getSuccessor().getEnteringSummaryEdge();
      CFANode nod = abbr.getPredecessor();
      if(nod.equals(successor.getCurrentScope().getReturnNode())){
        check = successor;
        successor.redScope();
      }
      break;
    case FunctionPntCallEdge:
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
        res = handleRightSide(rside.getFunctionNameExpression(),successor.getCurrentScope(),successor.getFactory());

        if(res.getFunctionPnt().equals(fpcedge.getSuccessor())){
          handleFunctionCall(fpcedge.getRawAST(),fpcedge.getSuccessor(),fpcedge.getArguments(), successor);
          successor.getCurrentScope().setReturnNode(cfaEdge.getPredecessor());
          check = successor;

        }

       } catch (Exception e) {
        e.printStackTrace();
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

    if (InterpreterBottomElement.INSTANCE.equals(check)) {
      return Collections.emptySet();
    } else {
      return Collections.singleton(successor);
    }
  }



private AbstractElement handleAssume(AssumeEdge pAssumeEdge,
      InterpreterElement pelement) {
    try {
      //System.out.println("TRUTH "+ pAssumeEdge.getTruthAssumption());
      //System.out.println("EXPRESSION "+ pAssumeEdge.getExpression().getRawSignature());
      ExprResult exp = handleRightSide(pAssumeEdge.getExpression(),pelement.getCurrentScope(),pelement.getFactory());
      switch(exp.getResultType()){
      case Num:
        if((exp.getnumber().compareTo(BigInteger.ZERO)==0) ^ !pAssumeEdge.getTruthAssumption()){
          return InterpreterBottomElement.INSTANCE;
        }else{
          return pelement;
        }
      case Var:
        Variable v = exp.getVariable();
        if(v.getTypeClass() == TypeClass.PRIMITIVE){
          PrimitiveVariable pvar = (PrimitiveVariable)v;
          BigInteger big1 = decodeVar(pvar);
          System.out.println(big1);
          if((big1.compareTo(BigInteger.ZERO)==0) ^ !pAssumeEdge.getTruthAssumption()){
            return InterpreterBottomElement.INSTANCE;
          }else{
            return pelement;
          }
        }else if (v.getTypeClass() == TypeClass.POINTER || v.getTypeClass() == TypeClass.PRIMITIVEPNT){
          MemoryBlock block = v.getAddress().getMemoryBlock();
          int offset = v.getAddress().getOffset();
          if(block.getAddress(offset).getMemoryBlock()==null ^ !pAssumeEdge.getTruthAssumption()){
            return InterpreterBottomElement.INSTANCE;
          }else{
            return pelement;
          }
        }else{
          throw new Exception("not yet supported");
        }


      default:
        throw new Exception("NOt supported assume result");
      }

    } catch (Exception e) {
     e.printStackTrace();
    }
    return null;
  }





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



private void handleUnknownFunctionCall(IASTFunctionCallAssignmentStatement pstatement, InterpreterElement pelement) throws Exception {
  //handle CPAmalloc
  IASTFunctionCallAssignmentStatement mcall = pstatement;
  IASTExpression nameexpr = mcall.getFunctionCallExpression().getFunctionNameExpression();
  if(nameexpr instanceof IASTIdExpression){
    String funcname = ((IASTIdExpression) nameexpr).getName();
    if(funcname.compareTo("CPAmalloc")==0){
      IASTExpression sizeexp = mcall.getFunctionCallExpression().getParameterExpressions().get(0);
      ExprResult expr= handleRightSide(sizeexp, pelement.getCurrentScope(),pelement.getFactory());
      int size = expr.getnumber().intValue();
      MemoryBlock block = pelement.getFactory().allocateMemoryBlock(size);
      Address addr = new Address(block,0);
      ExprResult res1 = new ExprResult(addr,null,-1);
      ExprResult res2  = handleLeftSide(mcall.getLeftHandSide(),pelement.getCurrentScope(),pelement.getFactory());
      handleAssignment(res2,res1);


    }else{
      throw new Exception("Unknown function");
    }
  }else{
    throw new Exception("Unknown function type");
  }

}



private void copyVars(InterpreterElement pelement, List<String> pList,
    List<IASTExpression> pArgs, List<IASTParameterDeclaration> parlist) throws Exception {
  for(int x=0;x<pArgs.size();x++){
    IASTExpression exp;
    String varname;
    exp = pArgs.get(x);
    varname = pList.get(x);
    IASTParameterDeclaration tmp = parlist.get(x);
    if(exp instanceof IASTLiteralExpression){
      handleSimpleDecl((IASTSimpleDeclSpecifier)tmp.getDeclSpecifier(),tmp.getName(),pelement);
      ExprResult res2 =handleRightSide(exp, pelement.getCurrentScope(), pelement.getFactory());
      ExprResult res1 = new ExprResult(pelement.getCurrentScope().getVariable(tmp.getName()));
      handleAssignment(res1, res2);
    }else if(exp instanceof IASTIdExpression){
      String cname = ((IASTIdExpression) exp).getName();
      Variable cvar = pelement.getCurrentScope().getParentScope().getVariable(cname);
      cvar.copyVar(varname, pelement);
    }else if (exp instanceof IASTUnaryExpression){

       ExprResult expr = handleRightSide(exp, pelement.getCurrentScope().getParentScope(),pelement.getFactory());
       IASTParameterDeclaration pardef = parlist.get(x);
       String cpname = pardef.getName();
       IType  typ = pardef.getDeclSpecifier();

       if(typ instanceof IASTPointerTypeSpecifier){
         try {
           handlePointerDecl((IASTPointerTypeSpecifier)typ,cpname ,pelement);
           ExprResult expr2 = new ExprResult(pelement.getCurrentScope().getVariable(cpname));
           handleAssignment(expr2,expr);
         } catch (Exception e) {
          e.printStackTrace();
         }
       }else{
         throw new Exception("Only address assignment in function call supported");
       }

    }else{

      throw new Exception("Not supported");
    }


  }

}



private void handleFree(IASTFunctionCallStatement pStatement,
    InterpreterElement pelement) throws Exception {
  IASTFunctionCallExpression mcall = pStatement.getFunctionCallExpression();
  IASTExpression nameexpr =mcall.getFunctionNameExpression();
    if(nameexpr instanceof IASTIdExpression){
      String funcname = ((IASTIdExpression)nameexpr).getName();
      if(funcname.compareTo("CPAfree")==0){
        ExprResult x = handleRightSide(mcall.getParameterExpressions().get(0),pelement.getCurrentScope(),pelement.getFactory());
        switch(x.getResultType()){
        case Var:
          Variable v = x.getVariable();
          if(v instanceof PointerVariable){
            MemoryBlock b = v.getAddress().getMemoryBlock();
            int offset = v.getAddress().getOffset();
            Address addr = b.getAddress(offset);
            if(addr.getOffset()!=0){
              throw new Exception("need address with offset 0 for memoryblock");
            }
            addr.getMemoryBlock().free();
            return;
          }

        default:
          throw new Exception("not supported free");
        }
      }
    }

}



private void handleAssignStatement(
    IASTExpressionAssignmentStatement pStatement, InterpreterElement pelement) throws Exception {
     IASTExpression left = pStatement.getLeftHandSide();
     IASTExpression right = pStatement.getRightHandSide();
     ExprResult res1;
     ExprResult res2;
     res2 = handleRightSide(right,pelement.getCurrentScope(),pelement.getFactory());
     res1 = handleLeftSide(left, pelement.getCurrentScope(),pelement.getFactory());



     handleAssignment(res1,res2);




}



private void handleAssignment(ExprResult res1, ExprResult res2) throws Exception{



  Type typ;
  Address addr;
  int level;
  switch(res1.getResultType()){
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
    bl.setFunctionPointer(offst,res2.getFunctionPnt());
    return;

  case Addr:
    if(res2.getLevelofIndirection()==-1){ //malloc
      int offset =  addr.getOffset();
      MemoryBlock block = addr.getMemoryBlock();
      block.setAddress(offset, res2.getAddress());
      return;
    }
    if(res1.getResultType()== RType.Var){
        Variable v = res1.getVariable();
        if(v instanceof PointerVariable){
          PointerVariable pvar = (PointerVariable)v;

          if(checkType(pvar.getBaseType(),res2.getDataType())&& (pvar.getlevel() == res2.getLevelofIndirection())){
            int offset = pvar.getAddress().getOffset();
            MemoryBlock block = pvar.getAddress().getMemoryBlock();
            block.setAddress(offset, res2.getAddress());

          } else{
            throw new Exception("TYPES do not coincide");
          }



        }else if(v instanceof PrimitiveVariable){
           PrimitiveVariable pvar = (PrimitiveVariable)v;
           pvar.setPnt();
           int offset = pvar.getAddress().getOffset();
           MemoryBlock block = pvar.getAddress().getMemoryBlock();
           block.setAddress(offset, res2.getAddress());

        }else{

          throw new Exception("Cannot assign Address to non pointer variable");
        }
    }else if(res1.getResultType() == RType.Addr){
      if(checkType(res1.getDataType(),res2.getDataType()) && (res1.getLevelofIndirection()== res2.getLevelofIndirection())){
      MemoryBlock b =addr.getMemoryBlock();
      int offset = addr.getOffset();
      b.setAddress(offset, res2.getAddress());
      }else{
        throw new Exception("TYPES don't coincide");
      }

    }
    break;
  case Num:

    if(typ instanceof PrimitiveType  && level ==0)
      writePrimVar((PrimitiveType)typ,addr,res2.getnumber());
    else if(typ instanceof PointerType && res2.getnumber().compareTo(BigInteger.valueOf(0)) ==0){
      if(res1.getResultType()==RType.Var){
        ((PointerVariable)res1.getVariable()).setNullPointer();
        return;
      }
      MemoryBlock block;
      int offset=0;
      block = addr.getMemoryBlock();
      offset = addr.getOffset();
      block.setAddress(offset, new Address(null,0));

    }
    else if(level >0 && res2.getnumber().compareTo(BigInteger.ZERO)==0){
      addr.getMemoryBlock().setAddress(addr.getOffset(), new Address(null,0));
    }else{
      //System.out.println(typ.getTypeClass());
      //System.out.println("NUMBER" +res2.getnumber());
      throw new Exception("Cannot asign number to not primitive type");
    }

    break;
  case Var:
    Type typ2 = res2.getVariable().getType();
    Address addr2 = res2.getVariable().getAddress();

    if(checkType(typ,typ2)){
      if(res1.getResultType()== RType.Var){
        copyVar(res1.getVariable().getAddress(),res2.getVariable().getAddress(),res2.getVariable().getSize());

      }else if(res1.getResultType()== RType.Addr){
        copyVar(res1.getAddress(),res2.getVariable().getAddress(),res2.getVariable().getSize());
      }
    }else if(typ instanceof PrimitiveType && typ2 instanceof PointerType){
       if (res1.getResultType()==RType.Var){
         PrimitiveVariable  pvar = (PrimitiveVariable) res1.getVariable();
         if(pvar.getPrimitiveType()== Primitive.LONG){
           pvar.setPnt();



         }else{
           throw new Exception("only int can be used in pointer calcs");
         }
       }
       copyVar(addr,res2.getVariable().getAddress(), res2.getVariable().getSize() );

    }
    else if(typ instanceof PointerType && typ2 instanceof PrimitiveType){


      copyVar(res1.getVariable().getAddress(),res2.getVariable().getAddress(),res2.getVariable().getSize());
    }

    else{

      throw new Exception("Types of the 2 variables differ");
    }
    break;
  default:
    throw new Exception("Not supported");
  }




}

private ExprResult handleRightSide(IASTExpression pRight,
    Scope currentScope,MemoryFactory factory ) throws Exception {

  if(pRight instanceof IASTLiteralExpression){
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
  if(pRight instanceof IASTIdExpression){
    String var =((IASTIdExpression) pRight).getName();
    CFAFunctionDefinitionNode func = map.get(var);
    if(func != null){
      return new ExprResult(func);
    }else{
    return new ExprResult(currentScope.getVariable(var));
    }

  }
  if(pRight instanceof IASTUnaryExpression){
    IASTUnaryExpression unaryexp= (IASTUnaryExpression) pRight;
    ExprResult res =handleRightSide(unaryexp.getOperand(),currentScope,factory);
    UnaryOperator op = unaryexp.getOperator();
    switch(op){
    case  MINUS:
      switch(res.getResultType()){
      case Addr:
        throw new Exception("not supported");
      case Var:
        Variable v =res.getVariable();
        if(v.getTypeClass()==TypeClass.PRIMITIVE){
          PrimitiveVariable var = (PrimitiveVariable)v;
          BigInteger bigint = decodeVar(var);
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
      res = handleRightSide(unaryexp.getOperand(),currentScope,factory);
      switch(res.getResultType()){
      case Var:
        if(res.getVariable().getTypeClass()== TypeClass.POINTER){
          PointerVariable var = (PointerVariable)res.getVariable();
          MemoryBlock b =  var.getAddress().getMemoryBlock();


          Address addr = b.getAddress(0);
          if(var.getlevel()>1 ){
            if(addr.getMemoryBlock().getCellType(addr.getOffset())==CellType.ADDR){
              Address addr2 = addr.getMemoryBlock().getAddress(addr.getOffset());

              return new ExprResult(addr2,var.getBaseType(),var.getlevel()-1);
            }else if(addr.getMemoryBlock().getCellType(addr.getOffset())==CellType.FUNC){ //TODO: PointerVariable used in Cast should be FuncPointerVariable (either extend cast or merge VariableTypes)
              if(var.getlevel()!=2)
                throw new Exception("Error with dereference");
              return new ExprResult(addr.getMemoryBlock().getFunctionPointer(addr.getOffset()));
            }
          }else{
            switch(var.getBaseType().getTypeClass()){
            case PRIMITIVE:
              PrimitiveVariable tmp = new PrimitiveVariable("tmp", addr,  (PrimitiveType)var.getBaseType(), false,false);//TODO: signed unsigned handeln
              return new ExprResult(decodeVar(tmp));
            default:
              throw new Exception("REF OF DATA TYPE NOT YET SUPPORTED");
            }
          }
        }else if(res.getVariable().getTypeClass() == TypeClass.FUNCPOINTER){
          FuncPointerVariable var = (FuncPointerVariable)res.getVariable();
          MemoryBlock b =  var.getAddress().getMemoryBlock();
          int offset = var.getAddress().getOffset();

          if(var.getlevel()>1){
            Address tmp = b.getAddress(offset);
            PointerType k  = (PointerType)var.getType();
            PointerType x = new PointerType(var.getBaseType(),false,var.getlevel()-1);

            FuncPointerVariable fptmp = new FuncPointerVariable("tmp",tmp,var.getBaseType(),x,var.getlevel()-1);
            return new ExprResult(fptmp);
          }else{
            return new ExprResult(b.getFunctionPointer(offset));
          }




        }

      default:
        throw new Exception("STAR only on var and addr applicable");
      }

    case AMPER:
      switch(res.getResultType()){
      case Addr:
        //struct data->a might return address
        break;
      case Var:
        switch(res.getVariable().getTypeClass()){
        case PRIMITIVE:
          PrimitiveVariable v = (PrimitiveVariable) res.getVariable();
          return new ExprResult(v.getAddress(), v.getType(),1);
        case POINTER:
          PointerVariable pvar = (PointerVariable) res.getVariable();
          return new ExprResult(pvar.getAddress(),pvar.getBaseType(),pvar.getlevel()+1);
        case STRUCT:
          DynVariable svar = (DynVariable)res.getVariable();
          return new ExprResult(svar.getAddress(),svar.getType(),1);
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
    case TILDE:
      switch(res.getResultType()){
      case Addr:
        throw new Exception("inversion of addresses not allowed");

      case Var:
        Variable v = res.getVariable();
        switch(v.getTypeClass()){
        case PRIMITIVE:
          PrimitiveVariable pvar = (PrimitiveVariable)v;
          BigInteger big1= decodeVar(pvar);
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

    case NOT:
      ExprResult expr =handleNot(res);
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
    ExprResult res1 = handleRightSide(((IASTBinaryExpression) pRight).getOperand1(), currentScope,factory);
    ExprResult res2 = handleRightSide(((IASTBinaryExpression) pRight).getOperand2(), currentScope,factory);
    ExprResult res;
    switch(((IASTBinaryExpression) pRight).getOperator()){
    case LOGICAL_OR:
      res = handleOr(res1,res2,currentScope);
      return res;
    case LOGICAL_AND:
      res1 = handleNot(res1);
      res2 = handleNot(res2);
      res = handleOr(res1,res2,currentScope);
      return res;
    case LESS_THAN:
      res = handleLessThan(res1,res2,currentScope);
      return res;


    case GREATER_THAN:
      res = handleLessThan(res2,res1,currentScope);
      return res;
    case LESS_EQUAL:
      ExprResult tmp;
      tmp = res1;
      res1 = res2;
      res2 = tmp;
    case GREATER_EQUAL:
      res = handleLessThan(res1,res2,currentScope);
      if(res.getnumber().compareTo(BigInteger.ZERO)==0){
        return new ExprResult(BigInteger.ONE);
      }else{
        return new ExprResult(BigInteger.ZERO);
      }

    case BINARY_AND:
      res = handleBinaryAND(res1,res2,currentScope);
      return res;
    case PLUS:
       res = handleBinaryOP(res1,res2,currentScope,NumOP.PLUS);
      return res;
    case MINUS:
      res = handleBinaryOP(res1,res2,currentScope,NumOP.MINUS);
      return res;
    case MULTIPLY:
      res = handleBinaryOP(res1,res2,currentScope,NumOP.MULTIPLY);
      return res;
    case DIVIDE:
      res = handleBinaryOP(res1,res2,currentScope,NumOP.DIVIDE);
      return res;
    case EQUALS:
      res = handleEquals(res1,res2,currentScope);
      return res;
    case NOT_EQUALS:
      res = handleEquals(res1,res2,currentScope);
      if(res.getnumber().compareTo(BigInteger.ZERO)==0){
        return new ExprResult(BigInteger.ONE);
      }else{
        return new ExprResult(BigInteger.ZERO);
      }
    default:
      throw new Exception("BINARY OPERATOR NOT YET SUPPORTED");




    }
  }

  if(pRight instanceof IASTCastExpression){
    return handleRCast(pRight,currentScope,factory);
     }




  //System.out.println(pRight.getRawSignature());
  throw new Exception("handleRightSide case unhandled");

}

private ExprResult handleRCast(IASTExpression pRight, Scope cur,
    MemoryFactory factory) throws Exception {
  Type z= handleRightCast(pRight.getExpressionType(),cur,factory);
  ExprResult res;
  switch(z.getTypeClass()){
  case PRIMITIVE:
    PrimitiveType k=(PrimitiveType)z;
    res = handleRightSide(((IASTCastExpression)pRight).getOperand(), cur,factory);
    switch(res.getResultType()){
    case Num:
      //TODO: add conversion?? (not really needed assign typechecks done by cil)
      return res;
    case Var:
      switch(res.getVariable().getTypeClass()){
      case PRIMITIVE:

        MemoryBlock block = factory.allocateMemoryBlock(k.sizeOf());
        Address naddr = new Address(block,0);
        if(k.sizeOf()<res.getVariable().getSize()){
          copyVar(naddr, res.getVariable().getAddress(), k.sizeOf());
        }else{
          copyVar(naddr, res.getVariable().getAddress(), res.getVariable().getSize());
          for(int x= res.getVariable().getSize();x<k.sizeOf();x++){
            naddr.getMemoryBlock().setData(x,(byte)0);
          }
        }
        PrimitiveVariable tmp = new PrimitiveVariable("tmpx", naddr, k, k.isSigned(), k.isConst());

        return new ExprResult(tmp);
      case POINTER:
        //(int) pnt;
        return res;
      case ARRAY:
        tmp = null;
        block = factory.allocateMemoryBlock(4);
        block.setAddress(0, res.getVariable().getAddress());
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
    res = handleRightSide(((IASTCastExpression)pRight).getOperand(), cur,factory);
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
      switch(v.getTypeClass()){
        case PRIMITIVE:
          ((PrimitiveVariable) res.getVariable()).setPnt();

        case PRIMITIVEPNT:
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

  default:
    throw new Exception("NOT SUPPORTED CONVERSION");
  }

  return null;
}



private ExprResult handleOr(ExprResult pRes1, ExprResult pRes2, Scope pCur) throws Exception{
  // TODO Auto-generated method stub
  switch(pRes1.getResultType()){
  case Num:
    if(pRes1.getnumber().compareTo(BigInteger.ZERO)>0){
      return new ExprResult(BigInteger.ONE);
    }
   break;
  case Var:
    Variable v = pRes1.getVariable();
    switch(v.getTypeClass()){
    case PRIMITIVE:
      PrimitiveVariable var = (PrimitiveVariable)v;
      BigInteger big1 = decodeVar(var);
      if(big1.compareTo(BigInteger.ZERO)>0){
        return new ExprResult(BigInteger.ONE);
      }
      break;
    case POINTER:
      PointerVariable pvar = (PointerVariable)v;
      if(!pvar.isNullPointer()){
        return new ExprResult(BigInteger.ONE);
      }
      break;
    case PRIMITIVEPNT:
      var = (PrimitiveVariable)v;
      MemoryBlock b = var.getAddress().getMemoryBlock();
      int offset = var.getAddress().getOffset();
      if(b.getAddress(offset).getMemoryBlock()!=null){
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
    switch(v.getTypeClass()){
    case PRIMITIVE:
      PrimitiveVariable var = (PrimitiveVariable)v;
      BigInteger big1 = decodeVar(var);
      if(big1.compareTo(BigInteger.ZERO)>0){
        return new ExprResult(BigInteger.ONE);
      }else{
        return new ExprResult(BigInteger.ZERO);
      }

    case POINTER:
      PointerVariable pvar = (PointerVariable)v;
      if(!pvar.isNullPointer()){
        return new ExprResult(BigInteger.ONE);
      }else{
        return new ExprResult(BigInteger.ZERO);
      }

    case PRIMITIVEPNT:
      var = (PrimitiveVariable)v;
      MemoryBlock b = var.getAddress().getMemoryBlock();
      int offset = var.getAddress().getOffset();
      if(b.getAddress(offset).getMemoryBlock()!=null){
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



private ExprResult  handleNot(ExprResult res) throws Exception {
  switch(res.getResultType()){
  case Addr:
    throw new Exception("inversion of addresses not allowed");

  case Var:
    Variable v = res.getVariable();
    switch(v.getTypeClass()){
    case PRIMITIVE:
      PrimitiveVariable pvar = (PrimitiveVariable)v;
      BigInteger big1= decodeVar(pvar);
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



private ExprResult handleLessThan(ExprResult pRes1, ExprResult pRes2, Scope pCur) throws Exception {
  BigInteger big1;
  BigInteger big2;
  switch(pRes1.getResultType()){
  case Num:
    big1 = pRes1.getnumber();
    break;
  case Var:
    Variable v = pRes1.getVariable();
    if(v.getTypeClass()== TypeClass.PRIMITIVE){
      big1 = decodeVar((PrimitiveVariable)v);
    }else{
      throw new Exception("less than does not support pointers");
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
    if(v.getTypeClass()== TypeClass.PRIMITIVE){
      big2 = decodeVar((PrimitiveVariable)v);
    }else{
      throw new Exception("less than does not support pointers");
    }
    break;
  default:
    throw new Exception("addresses not supported in less");
  }
  System.out.println(big1);
  System.out.println(big2);
  if(big1.compareTo(big2)<0){
    return new ExprResult(BigInteger.ONE);
  }else{
    return new ExprResult(BigInteger.ZERO);
  }



}



private ExprResult handleEquals(ExprResult pRes1, ExprResult pRes2, Scope pCur) throws Exception {




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
      return handleEquals(pRes2, pRes1, pCur);
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

  case Var:
    Variable v = pRes1.getVariable();

    if(v.getTypeClass()==TypeClass.PRIMITIVE){
      PrimitiveVariable pvar =(PrimitiveVariable)v;
      BigInteger big1 = decodeVar(pvar);
      ExprResult res1 = new ExprResult(big1);
      return handleEquals(pRes2, res1,  pCur);
    }else if(v.getTypeClass() == TypeClass.POINTER){
      PointerVariable pvar =(PointerVariable)v;
     ExprResult res1 = new ExprResult(pvar.getAddress(), pvar.getBaseType(), pvar.getlevel());
      return handleEquals(pRes2,res1,pCur);
    }else if(v.getTypeClass() == TypeClass.PRIMITIVEPNT){
      PrimitiveVariable pvar =(PrimitiveVariable)v;
      MemoryBlock b = pvar.getAddress().getMemoryBlock();
      int offset = pvar.getAddress().getOffset();
      ExprResult res1 = new ExprResult(b.getAddress(offset), pvar.getType(), 1);
       return handleEquals(pRes2,res1,pCur);
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
      return handleEquals(pRes2, pRes1, pCur);
    case Num:
      MemoryBlock block = pRes1.getAddress().getMemoryBlock();
      int offset = pRes1.getAddress().getOffset();
      if(block.getCellType(offset)==CellType.ADDR){
        if(block.getAddress(offset).getMemoryBlock()== null){
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

private ExprResult handleBinaryAND(ExprResult pRes1, ExprResult pRes2,
    Scope pCur) throws Exception {
  BigInteger big1;
  BigInteger big2;
  switch(pRes1.getResultType()){
  case Num:
    big1 = pRes1.getnumber();
    break;
  case Var:
    Variable v = pRes1.getVariable();
    switch(v.getTypeClass()){
    case PRIMITIVE:
      PrimitiveVariable pvar = (PrimitiveVariable)v;
      big1 = decodeVar(pvar);
      break;
    default:
      throw new Exception("Binaryand not for complex data types");
    }
    break;
    default:
      throw new Exception("Binaryand not for addresses");

    }

  switch(pRes2.getResultType()){
  case Num:
    big2 = pRes2.getnumber();
    break;
  case Var:
    Variable v = pRes2.getVariable();
    switch(v.getTypeClass()){
    case PRIMITIVE:
      PrimitiveVariable pvar = (PrimitiveVariable)v;
      big2 = decodeVar(pvar);
      break;
    default:
      throw new Exception("Binaryand not for complex data types");
    }
    break;
    default:
      throw new Exception("Binaryand not for addresses");

    }

  big1 = big1.and(big2);
  return new ExprResult(big1);
}

private ExprResult handleLeftSide(IASTExpression pLeft,
    Scope cur,MemoryFactory fact) throws Exception {
    ExprResult res;
    if(pLeft instanceof IASTIdExpression){
      Address z = cur.getVariable(((IASTIdExpression) pLeft).getName()).getAddress();
      return new ExprResult(cur.getVariable(((IASTIdExpression) pLeft).getName()));
    }
    if(pLeft instanceof IASTUnaryExpression){
      IASTUnaryExpression uexp = (IASTUnaryExpression) pLeft;
      switch(uexp.getOperator()){
      case STAR:
          res = handleLeftSide(uexp.getOperand(),cur,fact);
          switch(res.getResultType()){
          case Var:

            PointerVariable var = (PointerVariable)res.getVariable();
            MemoryBlock b =  var.getAddress().getMemoryBlock();
            int offset = var.getAddress().getOffset();
            return new ExprResult(b.getAddress(offset),var.getBaseType(),var.getlevel()-1);
          case Addr:
            b= res.getAddress().getMemoryBlock();
            return new ExprResult(b.getAddress(res.getAddress().getOffset()),res.getDataType(),res.getLevelofIndirection()-1);


          default:
            throw new Exception("STAR only on var and addr applicable");
          }

      default:
        throw new Exception("Operation not supported");
      }
    }
    if(pLeft instanceof IASTCastExpression){
      IASTCastExpression expr = (IASTCastExpression)pLeft;
      IType type = expr.getExpressionType();
      Type p = handleRightCast(type,cur,fact);
      ExprResult res1 = handleLeftSide(expr.getOperand(),cur,fact);
      switch(p.getTypeClass()){
      case POINTER:
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

private Type handleRightCast(IType pExpressionType, Scope pCur,MemoryFactory fact) throws Exception {
  if(pExpressionType instanceof IASTSimpleDeclSpecifier){
      return getPrimitiveType((IASTSimpleDeclSpecifier)pExpressionType);
  }
  if(pExpressionType instanceof IASTPointerTypeSpecifier){
    return getPointerType((IASTPointerTypeSpecifier)pExpressionType,pCur,fact);
  }
  throw new Exception("Not supported");
}

private ExprResult handleBinaryOP(ExprResult pRes1, ExprResult pRes2,
    Scope pCur,NumOP op) throws Exception {
  BigInteger big1,big2;
  switch(pRes1.getResultType()){
  case Num:
    big1 = pRes1.getnumber();
    break;
  case Var:
    Variable v = pRes1.getVariable();
    switch(v.getTypeClass()){
    case PRIMITIVE:
      PrimitiveVariable var = (PrimitiveVariable) v;
      big1 = decodeVar(var);
      break;
    case POINTER:
    case PRIMITIVEPNT:
      if(op== NumOP.PLUS || op== NumOP.MINUS)
        return handlePntOP(pRes1.getVariable(),pRes2,op);
      else
        throw new Exception("Pnt arithmetik only supported for +/-");



    default:
      throw new Exception("Currently not supported");
    }
    break;
  case Addr:
    if(op== NumOP.PLUS || op== NumOP.MINUS)
      return handleAddrPntOP(pRes1.getAddress(),pRes1.getDataType(),pRes2,op,pRes1.getLevelofIndirection());
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
    switch(v.getTypeClass()){
    case PRIMITIVE:
      PrimitiveVariable var = (PrimitiveVariable) v;
      big2 = decodeVar(var);
      break;
    case POINTER:
    case PRIMITIVEPNT:
      if(op== NumOP.PLUS || op== NumOP.MINUS)
        return handlePntOP(pRes2.getVariable(),pRes1,op);
      else
        throw new Exception("Pnt arithmetik only supported for +/-");

    default:
      throw new Exception("Currently not supported");
    }

    break;
  case Addr:
    if(op== NumOP.PLUS || op== NumOP.MINUS)
      return handleAddrPntOP(pRes2.getAddress(),pRes2.getDataType(),pRes1,op,pRes2.getLevelofIndirection());
    else
      throw new Exception("Pnt arithmetik only supported for +/-");

  default:
    throw new Exception("Unsupported");
  }

  System.out.println(op.toString() +": "+ big1 + " and " + big2);
  return new ExprResult( op.performCalc(big1, big2));
}

private ExprResult handleAddrPntOP(Address pAddress, Type pType,
    ExprResult pRes1, NumOP op, int level) throws Exception {



  switch(pRes1.getResultType()){
  case Var:
    PrimitiveVariable var = (PrimitiveVariable)pRes1.getVariable();
    ExprResult expr = new ExprResult(decodeVar(var));
    return handleAddrPntOP(pAddress,pType, expr,op,level);
  case Num:
    Address v = new Address(pAddress.getMemoryBlock(),op.performCalc(pAddress.getOffset(), pRes1.getnumber().intValue()));
    return new ExprResult(v,pType,level);
  default:
    throw new Exception("Not supported pointercalc");
  }

}



private ExprResult handlePntOP(Variable pV, ExprResult pRes2, NumOP op) throws Exception {

  Address addr = pV.getAddress();
  switch(pRes2.getResultType()){
  case Var:
    PrimitiveVariable var = (PrimitiveVariable)pRes2.getVariable();
    ExprResult expr = new ExprResult(decodeVar(var));
    return handlePntOP(pV, expr,op);
  case Num:
    Address naddr;
    int big1 =pRes2.getnumber().intValue();
    int pnt=big1;
    if (pV.getTypeClass() == TypeClass.POINTER){
      PointerVariable pvar = (PointerVariable)pV;
      //PointerVariable pvar = (PointerVariable)pV;
      if(pvar.getlevel()>1){
        pnt= big1*4;
      }else{
        pnt = big1*pvar.getBaseType().sizeOf();
      }
    }
    int base =addr.getMemoryBlock().getAddress(addr.getOffset()).getOffset();
    naddr = new Address(addr.getMemoryBlock().getAddress(addr.getOffset()).getMemoryBlock(),op.performCalc(base, pnt));
    if(pV.getTypeClass()==TypeClass.PRIMITIVEPNT){
      return new ExprResult(naddr,pV.getType(),1);
    }else if(pV.getTypeClass() == TypeClass.POINTER){
      return new ExprResult(naddr,((PointerVariable)pV).getBaseType(),((PointerVariable)pV).getlevel());
    }
  default:
    throw new Exception("Not supported pointercalc");
  }
  //TODO: ADD integrity check

}

private void copyVar(Address pAddress, Address pAddress2, int pSize) throws Exception {
    int offset, offset2;
    MemoryBlock block, block2;

    offset = pAddress.getOffset();
    offset2 = pAddress2.getOffset();

    block = pAddress.getMemoryBlock();
    block2 = pAddress2.getMemoryBlock();
    for(int x=0;x<pSize;x++){
      switch(block2.getCellType(offset2+x)){
      case EMPTY:
        //TODO: analyze behavior of composite variables
        break;
      case DATA:
        block.setData(offset+x, block2.getData(offset2+x));

        break;
      case ADDR:
        block.setAddress(offset+x, block2.getAddress(offset2+x));
        break;
      case FUNC:
        block.setFunctionPointer(offset+x, block2.getFunctionPointer(offset2+x));
        break;
      default:
      throw new Exception("not supported yet");
    }
    }


}

private boolean checkType(Type pTyp, Type pTyp2) {



  if(pTyp.getDefinition().compareTo(pTyp2.getDefinition())==0)
    return true;
  return false;


}

private void writePrimVar(PrimitiveType pTyp, Address pAddr, BigInteger pGetnumber) {
  int exp;
  exp = pTyp.sizeOf();
  BigInteger numb = BigInteger.valueOf(2);
  numb =numb.pow(exp*8);

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
    try {
      block.setData(offset+x,  tmp.byteValue());
    } catch (MemoryException e) {
      e.printStackTrace();
    }
    pGetnumber= pGetnumber.shiftRight(8);

  }


}

//TODO:test method
private BigInteger decodeVar(PrimitiveVariable pVar) {
  byte data[] = new byte[pVar.getSize()];
  MemoryBlock mem = pVar.getAddress().getMemoryBlock();
  int offset = pVar.getAddress().getOffset();
  for(int x=0;x<data.length;x++){
    try {
      data[x]= mem.getData(x+offset);
    } catch (MemoryException e) {

      e.printStackTrace();
    }
  }
  BigInteger var;
  if(pVar.isSigned()){
    if((data[data.length-1]&-128)==-128){
      var = BigInteger.valueOf(-1);
      for(int x = data.length-1; x>=0;x--){
        var= var.and(BigInteger.valueOf(data[x]).mod(BigInteger.valueOf(255)));
        if(x!=0)
          var = var.shiftLeft(8);
      }
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


public void handleDeclaration(DeclarationEdge pDeclarationEdge,
    InterpreterElement pElement) throws Exception{
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

  if(typ instanceof IASTElaboratedTypeSpecifier){//defintion von struct variablen
    IASTElaboratedTypeSpecifier def = (IASTElaboratedTypeSpecifier)typ;
    handleDynVarDef(name,((IASTElaboratedTypeSpecifier) typ).getName(),pElement,typ.isConst());

  }

  if(typ instanceof IASTNamedTypeSpecifier){
    IASTNamedTypeSpecifier ntyp = (IASTNamedTypeSpecifier)typ;
    IType def =pElement.getCurrentScope().getCurrentDefinitions().getDefinition(ntyp.getName());
    handleTypeDecl(name, pElement, def);
  }


}



private void handleDynVarDef(String name, String label, InterpreterElement pElement,boolean isConst) throws Exception {

  CompositeType comp = pElement.getCurrentScope().getCurrentTypeLibrary().getType(label);
  if(comp == null){
    System.out.println("Forward declaration");
    return;
  }
  int size = comp.sizeOf();
  MemoryBlock data = pElement.getFactory().allocateMemoryBlock(size);
  Address addr = new Address(data,0);
  switch(comp.getTypeClass()){
  case STRUCT:
    DynVariable var = new DynVariable(name,addr,comp,isConst,dyntypes.STRUCT);
    pElement.getCurrentScope().addVariable(var);
    break;
  case UNION:
    var = new DynVariable(name,addr,comp,isConst,dyntypes.UNION);
    pElement.getCurrentScope().addVariable(var);
    break;
   default:
     throw new Exception("Not supported dynamic variable type");
  }

}



private CompositeType allocateDynType(IASTCompositeTypeSpecifier pComptyp,
    InterpreterElement pElement) throws Exception {
  CompositeType tmp;
  if( pComptyp.getKey() == IASTCompositeTypeSpecifier.k_struct){

    tmp= new StructType(pComptyp.getName(),false);
  }else {
    tmp = new UnionType(pComptyp.getName(),false);
  }
  pElement.getCurrentScope().getCurrentTypeLibrary().addType(tmp);
  Iterator<IASTCompositeTypeMemberDeclaration> it = pComptyp.getMembers().iterator();
  while(it.hasNext()){
    Type cur;
    IASTCompositeTypeMemberDeclaration el = it.next();
   IType memb = el.getDeclSpecifier();
   if(memb instanceof IASTSimpleDeclSpecifier){
     cur = getPrimitiveType((IASTSimpleDeclSpecifier) memb);
   }
   else if(memb instanceof IASTPointerTypeSpecifier){
     cur = getPointerType((IASTPointerTypeSpecifier) memb,pElement.getCurrentScope(),pElement.getFactory());
   }
   else if(memb instanceof IASTArrayTypeSpecifier){
     cur = getArrayType((IASTArrayTypeSpecifier) memb, pElement.getCurrentScope(),pElement.getFactory());
   }else if(memb instanceof IASTElaboratedTypeSpecifier){
     cur = pElement.getCurrentScope().getCurrentTypeLibrary().getType(((IASTElaboratedTypeSpecifier) memb).getName());
   }else if (memb instanceof IASTNamedTypeSpecifier){

     IASTNamedTypeSpecifier ntmp = (IASTNamedTypeSpecifier)memb;
     cur = getNamedType(pElement.getCurrentScope().getCurrentDefinitions().getDefinition(ntmp.getName()), pElement.getCurrentScope(), pElement.getFactory());

   }else{
     throw new Exception("NOT YET SUPPORTED");
   }
   tmp.addMember(el.getName(), cur);
  }

  return tmp;
}



private void handleArrayDecl(IASTArrayTypeSpecifier pTyp, String pName,
    InterpreterElement pElement) throws Exception {
    ArrayType typ=getArrayType(pTyp,pElement.getCurrentScope(),pElement.getFactory());
    int length = typ.length();
    MemoryBlock b = pElement.getFactory().allocateMemoryBlock(typ.sizeOf());
    Address addr = new Address(b,0);
    ArrayVariable var = new ArrayVariable(pName, addr, length, typ, typ.getType());

    pElement.getCurrentScope().addVariable(var);

}



private void handlePointerDecl(IASTPointerTypeSpecifier pTyp,
    String pName, InterpreterElement pElement) throws Exception{




    PointerType typ = getPointerType(pTyp,pElement.getCurrentScope(),pElement.getFactory());
    MemoryBlock block = pElement.getFactory().allocateMemoryBlock(4);
    Address addr = new Address(block, 0);
    if(typ.getTargetType() instanceof FunctionType){
      FuncPointerVariable var = new FuncPointerVariable(pName, addr, typ.getTargetType(), typ, typ.getLevelOfIndirection());
      pElement.getCurrentScope().addVariable(var);
    }else{
      PointerVariable var = new PointerVariable(pName, addr, typ.getTargetType(), typ, typ.getLevelOfIndirection());
      pElement.getCurrentScope().addVariable(var);
    }
    return;


}

public void handleSimpleDecl(IASTSimpleDeclSpecifier pTyp,
    String name, InterpreterElement pElement) {
  Address addr;
  MemoryBlock block;
  PrimitiveVariable pVar;


  PrimitiveType ptyp = getPrimitiveType(pTyp);
  if(ptyp.getPrimitive() != null){
    block = pElement.getFactory().allocateMemoryBlock(ptyp.getPrimitive().sizeOf());
    addr = new Address(block,0);

    pVar = new PrimitiveVariable(name, addr,  ptyp, ptyp.isSigned(),ptyp.isConst());
    pElement.getCurrentScope().addVariable(pVar);
  }else{
    try {
      throw new Exception("Unsupported primitive Data Type");
    } catch (Exception e) {

      e.printStackTrace();
    }
  }


}



private ArrayType getArrayType(IASTArrayTypeSpecifier pTyp,
    Scope scope,MemoryFactory fact) throws Exception {
  IType typ=pTyp;
  IASTArrayTypeSpecifier tmp=null;
  int length = 1;
  int dim=0;
  int x=1;
  ExprResult res = null;
  while(typ instanceof IASTArrayTypeSpecifier){
    tmp = (IASTArrayTypeSpecifier)typ;

    res = handleRightSide(tmp.getLength(),scope, fact);
    switch(res.getResultType()){
    case Num:
      x= res.getnumber().intValue();
      break;
    case Var:
      Variable v = res.getVariable();
      switch(v.getTypeClass()){
        case PRIMITIVE:
            x= decodeVar((PrimitiveVariable)v).intValue();
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

  if(typ instanceof IASTSimpleDeclSpecifier){
    basetype = getPrimitiveType((IASTSimpleDeclSpecifier) typ);
  }else if(typ instanceof IASTPointerTypeSpecifier)
    basetype = getPointerType((IASTPointerTypeSpecifier) typ,scope,fact);
  else if (typ instanceof IASTNamedTypeSpecifier ){
    String name = ((IASTNamedTypeSpecifier)typ).getName();
    IType t  = scope.getCurrentDefinitions().getDefinition(name);
    basetype = getNamedType(t,scope,fact);
    if(basetype instanceof ArrayType){
      length *= ((ArrayType)basetype).length();
    }

  }else if(typ instanceof IASTElaboratedTypeSpecifier){
    basetype = scope.getCurrentTypeLibrary().getType(((IASTElaboratedTypeSpecifier) typ).getName());

  }else{

    throw new Exception("not yet supported");
  }
  System.out.println("LENGTH " +length);
  return new ArrayType(basetype,length);
}



private Type getNamedType(IType t,Scope scope,MemoryFactory fact) throws Exception {

  if(t instanceof IASTArrayTypeSpecifier){
    return getArrayType((IASTArrayTypeSpecifier) t, scope, fact);
  }
  if(t instanceof IASTSimpleDeclSpecifier){
    return getPrimitiveType((IASTSimpleDeclSpecifier) t);
  }
  if(t instanceof IASTPointerTypeSpecifier){
    return getPointerType((IASTPointerTypeSpecifier) t, scope,fact);
  }
  if(t instanceof IASTCompositeTypeSpecifier){
    return scope.getCurrentTypeLibrary().getType(((IASTCompositeTypeSpecifier) t).getName());
  }
  if(t instanceof IASTElaboratedTypeSpecifier){
    return scope.getCurrentTypeLibrary().getType(((IASTElaboratedTypeSpecifier) t).getName());
  }
  if(t instanceof IASTNamedTypeSpecifier){
    String f =((IASTNamedTypeSpecifier) t).getName();
    IType def = scope.getCurrentDefinitions().getDefinition(f);
    return getNamedType(def, scope,fact);
  }

 throw new Exception("could not handle definition");
}



public PointerType getPointerType(IASTPointerTypeSpecifier pTyp, Scope scop,MemoryFactory fact) throws Exception{
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
    IASTElaboratedTypeSpecifier ctmp = (IASTElaboratedTypeSpecifier)tmp;
    Type basetype = scop.getCurrentTypeLibrary().getType(ctmp.getName());
    if(basetype == null){
      System.out.println("WHAT");
    }
    return new PointerType(basetype, isConst, level);


  }else if (tmp instanceof IASTNamedTypeSpecifier){
    IASTNamedTypeSpecifier ntmp = (IASTNamedTypeSpecifier)tmp;
    Type basetype = getNamedType(scop.getCurrentDefinitions().getDefinition(ntmp.getName()), scop, fact);
    while(basetype instanceof PointerType){
      level +=((PointerType) basetype).getLevelOfIndirection();
      basetype = ((PointerType) basetype).getTargetType();

    }
    return new PointerType(basetype, isConst,level);

  }
  else if (tmp instanceof IComplexType){
    IComplexType ctmp = (IComplexType)tmp;
    Type basetype = scop.getCurrentTypeLibrary().getType(ctmp.getName());
    if(basetype == null){
       IType xx = scop.getCurrentDefinitions().getDefinition(ctmp.getName());

       basetype = getNamedType(xx, scop, fact);
       while(basetype instanceof PointerType){
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
    System.out.println(tmp);
    throw new Exception("BASE TYpe not yet supported");
  }

}



public PrimitiveType getPrimitiveType(IASTSimpleDeclSpecifier pTyp){
  boolean isSigned;
  boolean isConst;
  isSigned = pTyp.isSigned();
  isConst = pTyp.isConst();
  Primitive typ = null;

  switch(pTyp.getType()){

  case INT:

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

  if(pTyp.isLong()){
    typ = Primitive.LONGLONG;

  }else if(pTyp.isShort()){

    typ = Primitive.SHORT;

  }else{


  }
  return new PrimitiveType(typ,isSigned,isConst);
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
