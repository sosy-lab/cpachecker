package org.sosy_lab.cpachecker.cpa.einterpreter.memory;


//TODO: CIL optimiert signed weg waehrend AST  variablen definiton ohne signed/unsigned als unsigned behandelt

import org.sosy_lab.cpachecker.cpa.einterpreter.InterpreterElement;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.MemoryBlock.CellType;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.Primitive;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.PrimitiveType;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.TypeClass;
public class PrimitiveVariable implements Variable{
 Address memorylocation;
  String name;
  PrimitiveType type;
  InterpreterElement tmp;


  boolean isSigned;
  boolean isConst;
  boolean isPntCalc;//wenn CIL basic datentyp fuer pointer berechnung verwendet; Versions gebraucht


  /*public PrimitiveVariable(String pname, Address pmemorylocation, Scope pscope, Primitive ptype, boolean pisSigned,boolean pisConst){
    name = pname;
    memorylocation = pmemorylocation;
    type = new PrimitiveType(ptype, false, false);
    scope = pscope;
    isSigned = pisSigned;
    isConst = pisConst;


  }*/

  public PrimitiveVariable(String pname, Address pmemorylocation,  PrimitiveType ptype, boolean pisSigned, boolean pisConst){
    name = new String(pname);

    memorylocation = pmemorylocation;
    type = ptype;
    isSigned = pisSigned;
    isConst = pisConst;
    isPntCalc = false;
  }

  private PrimitiveVariable(String pname, Address pmemorylocation,  PrimitiveType ptype, boolean pisSigned, boolean pisConst,boolean pisPntCalc){
    name = new String(pname);

    memorylocation = pmemorylocation;
    type = ptype;
    isSigned = pisSigned;
    isConst = pisConst;
    isPntCalc = pisPntCalc;
  }




  public  Primitive getPrimitiveType(){
    return type.getPrimitive();
  }

  @Override
  public  Type getType(){
    return type;
  }


  @Override
  public Address getAddress(){
    return memorylocation;
  }

  @Override
  public int getSize(){
    return type.sizeOf();
  }

  @Override
  public String getName() {
    return name;
  }







  @Override
  public TypeClass getTypeClass() {
    if(memorylocation.getMemoryBlock().getCellType(memorylocation.getOffset()) == CellType.ADDR){
      return TypeClass.PRIMITIVEPNT;
    }else{
      return TypeClass.PRIMITIVE;
    }
    /*if(isPntCalc==false){
    return TypeClass.PRIMITIVE;
    }else{
      return TypeClass.PRIMITIVEPNT;
    }*/




  }

  @Override
  public boolean isConst() {
    // TODO Auto-generated method stub
    return isConst;
  }
  public boolean isSigned(){
    return isSigned;
  }


  public void setPnt(){
    isPntCalc = true;
  }

  @Override
  public void copyVar(String pname,InterpreterElement el) throws Exception {
    // TODO Auto-generated method stub

    MemoryBlock b =el.getFactory().allocateMemoryBlock(this.getSize());
    Address addr = new Address(b, 0);
    PrimitiveVariable nvar = new PrimitiveVariable(pname, addr,type , isSigned, isConst, isPntCalc);
    MemoryBlock oldb=memorylocation.getMemoryBlock();
    int of= memorylocation.getOffset();

    for(int x=0; x<nvar.getSize();x++){
      MemoryCell data = oldb.getMemoryCell(of+x);
      if(data != null){
        data = data.copy();
      }
      b.setMemoryCell(data,x);

    }
    el.getCurrentScope().addVariable(nvar);


  }

  @Override
  public void setcurInterpreterElement(InterpreterElement pTmp) {
    tmp = pTmp;

  }



}
