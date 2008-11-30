/**
 * 
 */
package cpa.pointsto;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

import cpa.common.interfaces.AbstractElement;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IBinding;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class PointsToElement implements AbstractElement {
	
	private Set<PointsToRelation> references;
	private HashMap<IASTDeclarator,PointsToRelation> variables;
    
	public PointsToElement () {
		references = new HashSet<PointsToRelation>();
		variables = new HashMap<IASTDeclarator,PointsToRelation>();
	}
	
	public PointsToElement clone () {
		PointsToElement result = new PointsToElement();
		for (PointsToRelation p : references) {
			PointsToRelation r = p.clone();
			result.variables.put(p.getVariable(), r);
			result.references.add(r);
		}
    	assert (result.references.size() == result.variables.size());
		return result;
	}
	
    public Iterator<PointsToRelation> getIterator ()
    {
        return references.iterator ();
    }
    
    public PointsToRelation addVariable (IASTDeclarator variable) {
    	PointsToRelation entry = variables.get(variable);
    	if (null == entry) {
    		entry = new PointsToRelation(variable);
    		variables.put(variable, entry);
    		references.add(entry);
    	}
    	assert (references.size() == variables.size());
    	return entry;
    }
    
    public PointsToRelation lookup (IASTName name) {
    	IBinding binding = name.resolveBinding();
    	for (IASTDeclarator decl : variables.keySet()) {
    		if (decl.getNestedDeclarator() != null &&
    				decl.getNestedDeclarator().getName().resolveBinding() == binding) 
    			return variables.get(decl);
    		if (decl.getName().resolveBinding() == binding) return variables.get(decl);
    	}
    	
    	return null;
    }
    
    public void join (final PointsToElement other) {
    	for (PointsToRelation p : other.references) {
    		addVariable(p.getVariable()).join(p);
    	}
    	assert (references.size() == variables.size());
    }
    
    public boolean containsRecursive (final PointsToRelation pointsTo) {
    	PointsToRelation candidate = variables.get(pointsTo.getVariable());
    	if (candidate != null && pointsTo.subsetOf(candidate)) return true;
    	return false;
    }
    
    public String toString () {
    	assert (references.size() == variables.size());
    	String out = "{";
    	Iterator<PointsToRelation> iter = references.iterator();
    	while (iter.hasNext()) {
    		out += "(" + iter.next() + ")";
    		if (iter.hasNext()) out += ", ";
    	}
    	out += "}";
    	return out;
    }
}
