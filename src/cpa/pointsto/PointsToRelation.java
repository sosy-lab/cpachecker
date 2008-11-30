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
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class PointsToRelation {

	private final IASTDeclarator variable;
	private Set<String> pointsTo;
	/* TODO this is kind of a TOP element, but we can't use the real TOP here, because
	 * TOP is not sensitive to variable names
	 */
	private boolean pointsToUndef;
	
	public PointsToRelation (IASTDeclarator variable) {
		this.variable = variable;
		pointsTo = new HashSet<String>();
		pointsToUndef = true;
	}
	
	public IASTDeclarator getVariable() {
		return variable;
	}
	
	public void pointsTo (String pointsTo) {
		this.pointsTo.clear();
		pointsToUndef = false;
		addPointsTo(pointsTo);
	}
	
	public void pointsToNull () {
		this.pointsTo.clear();
		pointsToUndef = false;
		addPointsToNull();
	}
	
	public void pointsToUndef () {
		this.pointsTo.clear();
		pointsToUndef = true;
	}
	
	public void addPointsTo (String pointsTo) {
		if (!pointsToUndef) this.pointsTo.add(pointsTo);
	}
	
	public void addPointsToNull () {
		if (!pointsToUndef) this.pointsTo.add("null");
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
	
	public boolean subsetOf (final PointsToRelation other) {
		if (!other.variable.equals(variable)) return false;
		return other.pointsToUndef || other.pointsTo.containsAll(pointsTo);
	}
	
	public void join (final PointsToRelation other) {
		assert (other.variable.equals(variable));
		pointsToUndef |= other.pointsToUndef;
		if (pointsToUndef) {
			pointsTo.clear();
		} else {
			pointsTo.addAll(other.pointsTo);
		}
	}
	
	public PointsToRelation clone() {
		PointsToRelation result = new PointsToRelation(variable);
		result.pointsToUndef = pointsToUndef;
		for (String s : pointsTo) {
			result.pointsTo.add(s);
		}
		return result;
	}
	
	public String toString () {

		String out = "";
		IBinding binding = variable.getName().resolveBinding();
		// out += variable/*.getParent()*/.getRawSignature() + " -> ";
		out += binding.getName() + " -> ";
		if (pointsToUndef) {
			out += "*";
		} else {
			Iterator<String> iter = pointsTo.iterator();
			while (iter.hasNext()) {
				out += iter.next();
				if (iter.hasNext()) out += ",";
			}
		}

		return out;

	}

}
