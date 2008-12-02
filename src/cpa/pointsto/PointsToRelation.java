/**
 *
 */
package cpa.pointsto;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;

import common.Pair;
import common.Triple;

/**
 * this is what a C pointer is; we may have multiple declarations of the same
 * variable with a different number of deref ops (*) in a PointsToElement, each
 * points to an object of higher deref count
 *
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class PointsToRelation {

  private class Pointer {
    private final Pair<PointsToRelation,Integer> addressObject;
    private final String addressString;
    private Vector<PointsToRelation> children;

    public Pointer (PointsToRelation o, int offset) {
      this.addressObject = new Pair<PointsToRelation,Integer>(o, offset);
      this.addressString = null;
      this.children = null;
    }

    public Pointer (String address) {
      this.addressObject = null;
      this.addressString = new String(address);
      this.children = null;
    }

    @Override
    public Pointer clone() {
      Pointer result = null;
      if (addressObject != null) {
        result = new Pointer(addressObject.getFirst(), addressObject.getSecond());

      } else {
        result = new Pointer(addressString);
      }
      if (children != null) {
        result.children = new Vector<PointsToRelation>();
        result.children.addAll(children);
      }
      return result;
    }

    public Triple<PointsToRelation,Integer,String> getAddress () {
      if (addressObject != null) {
        return new Triple<PointsToRelation,Integer,String>(addressObject.getFirst(),
            addressObject.getSecond(),null);
      } else {
        return new Triple<PointsToRelation,Integer,String>(null, null, addressString);
      }
    }

    public PointsToRelation bracketOp (int index) {
      if (children != null && children.size() > index) {
        return children.get(index);
      }
      return null;
    }

    public PointsToRelation starOp () {
      return bracketOp(0);
    }

    public Iterator<PointsToRelation> iterator() {
      return (children != null ? children.iterator() : null);
    }

    public PointsToRelation shift (PointsToRelation ptr, int shift) {
      // we have no idea about how to modify locations represented by strings,
      // caller should then invalidate itself
      if (addressObject != null) {
        int offset = addressObject.getSecond() + shift;
        if (offset < 0) return null;
        Pointer parent = addressObject.getFirst().find(ptr, addressObject.getSecond());
        if (null != parent) return parent.bracketOp(offset);
      }
      return null;
    }
  }

	private final IASTDeclarator variable;
	private final String name;

	private boolean isTop;
	private final Set<Pointer> data;

	public PointsToRelation (IASTDeclarator variable, String name) {
		this.variable = variable;
		this.name = name;

		this.isTop = true;
		this.data = new HashSet<Pointer>();
	}

	@Override
	public int hashCode () {
		return variable.hashCode() + name.hashCode();
	}

	@Override
	public boolean equals (Object o) {
		if (!(o instanceof PointsToRelation)) {
			return false;
		}
		PointsToRelation other = (PointsToRelation)o;

		if (other.variable != variable || other.isTop != isTop) {
			return false;
		}

		if (!other.data.equals(data)) {
		  return false;
		}

		return true;
	}

	@Override
	public PointsToRelation clone() {
		PointsToRelation result = new PointsToRelation(variable, name);
		result.isTop = isTop;
		for (Pointer p : data) {
		  result.data.add(p.clone());
		}
		assert (result.data.size() == data.size());
		return result;
	}

	@Override
	public String toString () {
		String out = name + " = ";
		if (isTop) {
			out += "##TOP##";
		} else {
		  out += "{";
		  for (Pointer p : data) {
		    Triple<PointsToRelation,Integer,String> a = p.getAddress();
		    if (a.getFirst() != null) {
		      out += a.getFirst().getName() + "[" + a.getSecond() + "]";
		    } else {
		      out += a.getThird();
		    }

		    Iterator<PointsToRelation> iter = p.iterator();
		    if (iter != null) {
		      out += ": ";
		      while (iter.hasNext()) {
		        out += iter.next().toString();
		      }
		    }
		  }
		  out += "}";
		}
		return out;
	}

	public IASTDeclarator getVariable () {
		return variable;
	}

  public String getName() {
    return name;
  }

	public void makeTop () {
		this.isTop = true;
		this.data.clear();
	}

	public void setAddress (PointsToRelation obj, int offset) {
		this.isTop = false;
		this.data.clear();
		this.data.add(new Pointer(obj, offset));
	}

	public void setAddress (String init) {
	  this.isTop = false;
    this.data.clear();
    this.data.add(new Pointer(init));
	}

	public void makeNull () {
		setAddress("null");
	}

	public void addAddress (PointsToRelation obj, int offset) {
		if (!isTop) this.data.add(new Pointer(obj, offset));
	}

	public void addAddress (String init) {
	  if (!isTop) this.data.add(new Pointer(init));
	}

	public void addNull () {
		addAddress("null");
	}

	public void makeAlias (PointsToRelation other) {
	  if (this == other) return;
		isTop = other.isTop;

		data.clear();
		data.addAll(other.data);

		assert (other.data.size() == data.size());
	}

	public Pointer find (PointsToRelation r, int offset) {
	  for (Pointer p : data) {
	    if (p.bracketOp(offset) == r) return p;
	  }

	  return null;
	}

	public void shift (int offset) {
		if (isTop) return;
		PointsToRelation newThis = new PointsToRelation(variable, name);

		for (Pointer p : data) {
		  PointsToRelation r = p.shift(this, offset);
		  if (null == r || r.isTop) {
		    this.isTop = true;
		    break;
		  }
		  newThis.data.addAll(r.data);
		}

		data.clear();
		if (isTop) return;
		data.addAll(newThis.data);
	}

	public boolean subsetOf (final PointsToRelation other) {
		if (!other.variable.equals(variable)) return false;
		return other.isTop || other.data.containsAll(data);
	}

	public void join (final PointsToRelation other) {
		assert (other.variable.equals(variable));
		assert (other.name.equals(name));
		isTop |= other.isTop;
		if (isTop) {
			data.clear();
		} else {
			data.addAll(other.data);
		}
	}
}
