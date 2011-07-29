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
  private TDef definition;

  private  IASTExpression returnexpr;
  private CFANode returnnod;

  InterpreterElement tmp = null;

  public Scope(String pname,InterpreterElement el){
    HashMap<String, Variable> variable = new HashMap<String,Variable>();
    variables = new HashMap<InterpreterElement, HashMap<String,Variable>>();
    variables.put(el, variable);
    name = pname;
    types = new DynamicTypes();
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

    }else{
      types = new DynamicTypes();
      definition = new TDef();
    }

  }
  void setcurInterpreterElement(InterpreterElement el){
    tmp = el;
  }




  public String getID(){
    return name;
  }
  @SuppressWarnings("unchecked")
  public Variable getVariable(String pname){

      HashMap<String, Variable> vtmp = variables.get(tmp);
      InterpreterElement itmp= tmp;
      while(vtmp == null && itmp != null){
        itmp = itmp.getprev();
        vtmp = variables.get(itmp);
      }
      Variable k = vtmp.get(pname);
      Scope s = this;
      if(k==null){
        while(s.parent != null){
          s = s.parent;
        }
        s.setcurInterpreterElement(tmp);
        return s.getVariable(pname);

      }

      k = vtmp.get(pname);
      if(k!=null){
        k.setcurInterpreterElement(tmp);
      }
      return k;

  }

  @SuppressWarnings("unchecked")
  public void addVariable(Variable pvar){
    pvar.setcurInterpreterElement(tmp);
    HashMap<String, Variable> variable = variables.get(tmp);
    variable = (HashMap<String, Variable>) variable.clone();
    variable.put(pvar.getName(), pvar);
    variables.put(tmp, variable);
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
