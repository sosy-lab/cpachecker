/**
 * 
 */
package cpa.pointsto;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

import common.Pair;

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
	private HashMap<Pair<IASTDeclarator,Integer>,PointsToRelation> variables;
    
	public PointsToElement () {
		references = new HashSet<PointsToRelation>();
		variables = new HashMap<Pair<IASTDeclarator,Integer>,PointsToRelation>();
	}
	
	@Override
	public PointsToElement clone () {
		PointsToElement result = new PointsToElement();
		for (PointsToRelation p : references) {
			PointsToRelation r = p.clone();
			result.variables.put(new Pair<IASTDeclarator,Integer>(p.getVariable(), p.getDerefCount()), r);
			result.references.add(r);
		}
    	assert (result.references.size() == result.variables.size());
		return result;
	}
	
	@Override
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
	
    public Iterator<PointsToRelation> getIterator ()
    {
        return references.iterator ();
    }
    
    public PointsToRelation addVariable (IASTDeclarator variable, int derefCount) {
    	PointsToRelation entry = variables.get(new Pair<IASTDeclarator,Integer>(variable,derefCount));
    	if (null == entry) {
    		entry = new PointsToRelation(variable, derefCount);
    		variables.put(new Pair<IASTDeclarator,Integer>(variable, derefCount), entry);
    		references.add(entry);
    	}
    	assert (references.size() == variables.size());
    	return entry;
    }
    
    public PointsToRelation lookup (IASTName name, int derefCount) {
    	IBinding binding = name.resolveBinding();
    	for (Pair<IASTDeclarator,Integer> decl : variables.keySet()) {
    		if (decl.getFirst().getNestedDeclarator() != null &&
    				decl.getFirst().getNestedDeclarator().getName().resolveBinding() == binding &&
    				decl.getSecond().equals(derefCount))
    			return variables.get(decl);
    		if (decl.getFirst().getName().resolveBinding() == binding &&
    				decl.getSecond().equals(derefCount)) return variables.get(decl);
    	}
    	
    	return null;
    }
    
    public void join (final PointsToElement other) {
    	for (PointsToRelation p : other.references) {
    		addVariable(p.getVariable(), p.getDerefCount()).join(p);
    	}
    	assert (references.size() == variables.size());
    }
    
    public boolean containsRecursive (final PointsToRelation pointsTo) {
    	PointsToRelation candidate = variables.get(new Pair<IASTDeclarator,Integer>(
    			pointsTo.getVariable(), pointsTo.getDerefCount()));
    	if (candidate != null && pointsTo.subsetOf(candidate)) return true;
    	return false;
    }
}
