package cpaplugin.cpa.cpas.octagon;

import octagon.FunctionMap;
import octagon.LibraryAccess;
import octagon.Octagon;
import octagon.VariableMap;
import cpaplugin.cpa.common.interfaces.AbstractElement;

/**
 * An element of octagon abstract domain. This element contains an {@link Octagon} which 
 * is the concrete representation of the octagon and a {@link FunctionMap} which
 * provides a mapping from each function to its variables and variable IDs.
 * 
 * @author erkan
 *
 */
public class OctElement implements AbstractElement{
	
	private Octagon oct;
	private FunctionMap variables;
	
	/**
	 * Class constructor.
	 */
	public OctElement(){
		oct = LibraryAccess.empty(1);
		this.variables = new FunctionMap();
	}
	
	public OctElement(Octagon oct, VariableMap varMap){
		this.oct = oct;
		this.variables = varMap; 
	}
	
	public Octagon getOctagon(){
		return this.oct;
	}
	
	public OctElement clone(){
		VariableMap vp = new VariableMap(variables.getSize());
		vp = variables.clone();
		return new OctElement(oct, vp);
	}
	
	public void update(OctElement ent){
		this.oct = ent.oct;
		this.variables = ent.variables;
	}
	
	@Override
	public boolean equals (Object other)
    {
        if (this == other)
            return true;
        
        if (!(other instanceof OctElement))
            return false;

        OctElement otherOctagon = (OctElement) other;
        return LibraryAccess.isEqual(this, otherOctagon);
    }
	
	@Override
	public String toString() {

		String s = "";

		if (oct.getState() == 0){
			s = s + "[ empty ] \n";
			return s;
		}

		if (oct.getState() == 2){
			s = s + " [ closed ]";
		}

		for (int i=0; i<oct.getDimension(); i++) {
			String varName = variables.getVarName(i);
			if (oct.getMatrix()[oct.matPos(2*i,2*i)].f > 0) {
				s = s + "\n  " + varName + "-" + varName + " <= " + oct.getMatrix()[oct.matPos(2*i, 2*i)].f;
			}
			if (oct.getMatrix()[oct.matPos(2*i+1,2*i+1)].f > 0) {
				s = s + "\n  " + "-"+ varName + "+" + varName + " <= " + oct.getMatrix()[oct.matPos(2*i+1,2*i+1)].f;
			} 
			if ((oct.getMatrix()[oct.matPos(2*i+1,2*i)].f != Double.NEGATIVE_INFINITY) &&
					(oct.getMatrix()[oct.matPos(2*i+1,2*i)].f != Double.POSITIVE_INFINITY)) {
				s = s + "\n  " + varName + " <= " + (oct.getMatrix()[oct.matPos(2*i+1,2*i)].f)/2;
			}
			if ((oct.getMatrix()[oct.matPos(2*i,2*i+1)].f != Double.NEGATIVE_INFINITY) &&
					(oct.getMatrix()[oct.matPos(2*i,2*i+1)].f != Double.POSITIVE_INFINITY)) {
				s = s + "\n  " + "-" + varName + " <= " + (oct.getMatrix()[oct.matPos(2*i,2*i+1)].f)/2;
			}
		}

		for (int i=0; i<oct.getDimension(); i++){
			for (int j=i+1; j<oct.getDimension(); j++) {
				String iVarName = variables.getVarName(i);
				String jVarName = variables.getVarName(j);
				if((oct.getMatrix()[oct.matPos(2*j,2*i)].f != Double.NEGATIVE_INFINITY) &&
						(oct.getMatrix()[oct.matPos(2*j,2*i)].f != Double.POSITIVE_INFINITY)){
					s = s + "\n  " + iVarName + "-" + jVarName +" <= " + (oct.getMatrix()[oct.matPos(2*j,2*i)].f);
				}
				// 2*j,2*i+1
				if ((oct.getMatrix()[oct.matPos(2*j,2*i+1)].f != Double.NEGATIVE_INFINITY) &&
						(oct.getMatrix()[oct.matPos(2*j,2*i+1)].f != Double.POSITIVE_INFINITY)) {
					s = s + "\n  " + "-" + iVarName + "-" + jVarName +" <= " + (oct.getMatrix()[oct.matPos(2*j,2*i+1)].f);
				}
				// 2*j+1,2*i
				if ((oct.getMatrix()[oct.matPos(2*j+1,2*i)].f != Double.NEGATIVE_INFINITY) &&
						(oct.getMatrix()[oct.matPos(2*j+1,2*i)].f != Double.POSITIVE_INFINITY)) {
					s = s + "\n  " + iVarName + "+" + jVarName +" <= " + (oct.getMatrix()[oct.matPos(2*j+1,2*i)]);
				}
				// 2*j+1,2*i+1
				if ((oct.getMatrix()[oct.matPos(2*j+1,2*i+1)].f != Double.NEGATIVE_INFINITY) &&
						(oct.getMatrix()[oct.matPos(2*j+1,2*i+1)].f != Double.POSITIVE_INFINITY)){
					s = s + "\n  " + jVarName + "-" + iVarName +" <= " + (oct.getMatrix()[oct.matPos(2*j+1,2*i+1)]);
				}
			}
		}
		s = s + "\n";
		return s;
	
	}

	public void addVar(String varName, String funcName) {
		variables.addVar(varName, funcName);
	}
	
	public void removeVar(String varName, String functionName) {
		variables.removeVar(varName, functionName);
	}

	public int getNumberOfVars() {
		return variables.getSize();
	}

	public int getVarId(String varName, String functionName) {
		return variables.getVarId(varName, functionName);
	}

	public VariableMap getVarMap() {
		return this.variables;
	}

	public boolean contains(String varName, String functionName) {
		return variables.contains(varName, functionName);
	}

	public int removeVariablesOfFunction(String fname) {
		return variables.removeVariablesOfFunction(fname);
	}
	
	public boolean isEmpty(){
		return LibraryAccess.isEmpty(this);
	}

//	public void addVariablesFrom(OctElement octEl1) {
//		VariableMap oct1Map = octEl1.getVarMap();
//		for(int i=0; i<oct1Map.getSize(); i++){
//			String s = oct1Map.getVarName(i);
//			if(!this.variables.contains(s)){
//				this.update(LibraryAccess.addDimension(this, 1));
//				this.addVar(s);
//			}
//		}
//	}
	
}
