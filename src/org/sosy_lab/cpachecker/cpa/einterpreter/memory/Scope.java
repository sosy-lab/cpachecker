package org.sosy_lab.cpachecker.cpa.einterpreter.memory;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

public class Scope {
  private Map<String,Variable> variables; //versions
  private  String   name;
  private Scope parent;

  private Scope clone=null;
  private DynamicTypes types;
  private TDef definition;

  private  IASTExpression returnexpr;
  private CFANode returnnod;


  public Scope(String pname){
    variables = new HashMap<String,Variable>();
    name = pname;
    types = new DynamicTypes();
    definition = new TDef();

  }
  public Scope(String pname,Scope pparent){
    variables = new HashMap<String,Variable>();
    name = pname;
    parent = pparent;
    if(parent!=null){
      types = parent.types;
      definition = parent.definition;

    }else{
      types = new DynamicTypes();
      definition = new TDef();
    }

  }
  private Scope(String pname, Scope pparent,  IASTExpression preturnexpr,DynamicTypes ptypes,TDef pdefinition,CFANode preturnnode){
   variables = new HashMap<String,Variable>();
   returnexpr = preturnexpr;
    name = new String(pname);
    types = ptypes;
    definition = pdefinition;
    returnnod = preturnnode;

  }


  private  void setVariables(Map<String,Variable> pvariables){
    Iterator<Entry<String, Variable>> i = pvariables.entrySet().iterator();
    while(i.hasNext()){
         Entry<String,Variable> p = i.next();
         String nname = new String(p.getKey());
         Variable nvar = p.getValue().clone();
         variables.put(nname, nvar);
    }
  }

  public String getID(){
    return name;
  }
  public Variable getVariable(String pname){
      Variable vtmp;
      vtmp = variables.get(pname);
      if(vtmp == null){
        if(parent!=null){
          return parent.getVar(pname);

        }else{
          return null;
        }
      }else{
        return vtmp;
      }
  }
  private Variable getVar(String pname) {
    if(name.compareTo("global")==0){
      return variables.get(pname);
    } else{
      return parent.getVar(pname);
    }

  }
  public void addVariable(Variable pvar){
    variables.put(pvar.getName(), pvar);
  }


  private void setParent(Scope pparent){
    parent = pparent;
  }


  public Scope getParentScope(){
    return parent;
  }

  @Override
  public Scope clone(){
    if(clone !=null){
      return clone;
    }else{
     clone = new Scope(name,parent,returnexpr,types,definition,returnnod);
     clone.setVariables(variables);

     if(parent!=null)
       clone.setParent(parent.clone());
     return clone;
    }
  }
  public void setReturnExpression(IASTExpression pLeft) {
    returnexpr= pLeft;

  }

  public IASTExpression getReturnExpression(){
    return returnexpr;
  }

  public DynamicTypes getCurrentTypeLibrary(){
    return types;
}

  public TDef getCurrentDefinitions(){
    return definition;
  }

  public void setReturnNode(CFANode n){
    returnnod = n;
  }

  public CFANode getReturnNode(){
    return returnnod;
  }


}
