package org.sosy_lab.cpachecker.cpa.einterpreter.memory;


import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.einterpreter.InterpreterElement;

public class Scope {
  private Map<InterpreterElement,HashMap<String,Variable>> variables; //versions
  private  String   name;
  private Scope parent;


  private DynamicTypes types;
  private EnumTypes etypes;

  private TDef definition;

  private  IASTExpression returnexpr;
  private CFANode returnnod;



  public Scope(String pname,InterpreterElement el){
    HashMap<String, Variable> variable = new HashMap<String,Variable>();
    variables = new HashMap<InterpreterElement, HashMap<String,Variable>>();
    variables.put(el, variable);
    name = pname;
    types = new DynamicTypes();
    etypes = new EnumTypes();
    definition = new TDef();

  }
  public Scope(String pname,Scope pparent,InterpreterElement el){
    HashMap<String, Variable> variable = new HashMap<String,Variable>();
    variables = new HashMap<InterpreterElement, HashMap<String,Variable>>();
    variables.put(el, variable);
    name = pname;
    parent = pparent;
    if(parent!=null){
      types = parent.types;
      definition = parent.definition;
      etypes = parent.etypes;
    }else{
      types = new DynamicTypes();
      definition = new TDef();
      etypes = new EnumTypes();
    }

  }





  public String getID(){
    return name;
  }
  @SuppressWarnings("unchecked")
  public Variable getVariable(String pname , InterpreterElement tmp){
      //TODO: globale Variablen richtig handeln
      HashMap<String, Variable> vtmp = variables.get(tmp);
      InterpreterElement itmp= tmp;
      while(vtmp == null && itmp != null){
        itmp = itmp.getprev();
        vtmp = variables.get(itmp);
      }
      Variable k = vtmp.get(pname);
      Scope s = this;
      if(k==null&& !s.name.equals("main")){
        while(!s.name.equals("main")&& s.parent!=null){
          s = s.parent;
        }

        k= s.getVariable(pname,tmp);
        if(k!= null)
          return k;

      }
      if(k==null&& !s.name.equals("global")){
        while(!s.name.equals("global")&& s.parent!=null){
          s = s.parent;
        }

        k= s.getVariable(pname,tmp);
        if(k!= null)
          return k;

      }

      //k = vtmp.get(pname);
      if(k==null){

      }
      return k;

  }

  @SuppressWarnings("unchecked")
  public void addVariable(Variable pvar,InterpreterElement el){
    InterpreterElement tmp=el;
    HashMap<String, Variable> variable = null;
    while(variable == null){

      variable = variables.get(tmp);
      tmp = tmp.getprev();

    }
    variable = (HashMap<String, Variable>) variable.clone();
    variable.put(pvar.getName(), pvar);
    variables.put(el, variable);
  }





  public Scope getParentScope(){
    return parent;
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
  public EnumTypes getCurrentEnums(){
    return etypes;
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
