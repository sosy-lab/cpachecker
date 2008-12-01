/**
 * 
 */
package cpa.pointsto;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IBinding;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * this is what a C pointer is; we may have multiple declarations of the same
 * variable with a different number of deref ops (*) in a PointsToElement, each
 * points to an object of higher deref count
 * 
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 * 
 */
public class PointsToRelation {
	
	private final IASTDeclarator variable;
	private final int derefCount;
	private boolean isTop;
	private Set<PointsToRelation> pointsToObject;
	private Set<String> pointsToString;
	
	public PointsToRelation (IASTDeclarator variable, int derefCount) {
		this.variable = variable;
		this.derefCount = derefCount;
		this.isTop = true;
		this.pointsToObject = new HashSet<PointsToRelation>();
		this.pointsToString = new HashSet<String>();
	}
	
	@Override
	public int hashCode () {
		return variable.hashCode() + derefCount;
	}
	
	@Override
	public boolean equals (Object o) {
		if (!(o instanceof PointsToRelation)) {
			return false;
		}
		PointsToRelation other = (PointsToRelation)o;
		
		if (other.variable != variable || other.derefCount != derefCount ||
				other.isTop != isTop) {
			return false;
		}
		
		if (!other.pointsToObject.equals(pointsToObject) ||
				!other.pointsToString.equals(pointsToString)) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public PointsToRelation clone() {
		PointsToRelation result = new PointsToRelation(variable, derefCount);
		result.isTop = isTop;
		result.pointsToObject.addAll(pointsToObject);
		for (String s : pointsToString) {
			result.pointsToString.add(new String(s));
		}
		return result;
	}
	
	public String getVariableString () {
		String out = "";
		IBinding binding = variable.getName().resolveBinding();
		for (int i = derefCount; i > 0; --i) {
			out += "*";
		}
		// out += variable/*.getParent()*/.getRawSignature() + " -> ";
		out += binding.getName();
		return out;
	}
	
	@Override
	public String toString () {
		String out = getVariableString();
		if (isTop) {
			out += "##TOP##";
		} else {
			Iterator<PointsToRelation> iter1 = pointsToObject.iterator();
			while (iter1.hasNext()) {
				out += iter1.next().getVariableString();
				if (iter1.hasNext() || !pointsToString.isEmpty()) out += ",";
			}
			Iterator<String> iter2 = pointsToString.iterator();
			while (iter2.hasNext()) {
				out += iter2.next();
				if (iter2.hasNext()) out += ",";
			}
		}
		return out;
	}
	
	public IASTDeclarator getVariable () {
		return variable;
	}
	
	public int getDerefCount () {
		return derefCount;
	}
	
	public void makeTop () {
		this.isTop = true;
		this.pointsToObject.clear();
		this.pointsToString.clear();
	}
	
	public void pointsTo (PointsToRelation obj) {
		this.isTop = false;
		this.pointsToObject.clear();
		this.pointsToObject.add(obj);
		this.pointsToString.clear();
	}
	
	public void pointsTo (String init) {
		this.isTop = false;
		this.pointsToObject.clear();
		this.pointsToString.clear();
		this.pointsToString.add(new String(init));
	}
	
	public void pointsToNull () {
		pointsTo("null");
	}
	
	public void addPointsTo (PointsToRelation obj) {
		if (!isTop) this.pointsToObject.add(obj);
	}
	
	public void addPointsTo (String init) {
		if (!isTop) this.pointsToString.add(init);
	}
	
	public void addPointsToNull () {
		addPointsTo("null");
	}
	
	/*
	public void setPointsTo (PointsToRelation other) {
		pointsToUndef = other.pointsToUndef;
		pointsTo.clear();
		for (String s : other.pointsTo) {
			pointsTo.add(new String(s));
		}
	}
	
	public void updateAll (String update) {
		if (pointsToUndef) return;
		Iterator<String> iter = pointsTo.iterator();
		while (iter.hasNext()) {
			String s = iter.next();
			if (s.equals("null")) {
				s = update;
			} else {
				s = "(" + s + ") " + update;
			}
		}
	}
	*/
	
	public boolean subsetOf (final PointsToRelation other) {
		if (!other.variable.equals(variable) || other.derefCount != derefCount) return false;
		return other.isTop || (other.pointsToObject.containsAll(pointsToObject) &&
				other.pointsToString.containsAll(pointsToString));
	}
	
	public void join (final PointsToRelation other) {
		assert (other.variable.equals(variable));
		assert (other.derefCount == derefCount);
		isTop |= other.isTop;
		if (isTop) {
			pointsToObject.clear();
			pointsToString.clear();
		} else {
			pointsToObject.addAll(other.pointsToObject);
			pointsToString.addAll(other.pointsToString);
		}
	}
}
