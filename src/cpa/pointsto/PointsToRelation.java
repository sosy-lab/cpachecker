/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
/**
 *
 */
package cpa.pointsto;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;

import cpa.pointsto.PointsToElement.InMemoryObject;

/**
 * this is what a C pointer is; we may have multiple declarations of the same
 * variable with a different number of deref ops (*) in a PointsToElement, each
 * points to an object of higher deref count
 *
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class PointsToRelation extends InMemoryObject {

  public interface Address {
    public Address shift (int offset);
    public Address clone ();
  }

  public static class NullPointer implements Address {
    @Override
    public int hashCode () {
      return 0;
    }

    @Override
    public boolean equals (Object o) {
      return (o instanceof NullPointer);
    }

    @Override
    public NullPointer clone () {
      return this;
    }

    @Override
    public String toString () {
      return "NULL";
    }

    public Address shift (int offset) {
      return new InvalidPointer();
    }
  }

  public static class InvalidPointer implements Address {
    @Override
    public int hashCode () {
      return -1;
    }

    @Override
    public boolean equals (Object o) {
      return (o instanceof InvalidPointer);
    }

    @Override
    public InvalidPointer clone () {
      return this;
    }

    @Override
    public String toString () {
      return "##INVALID##";
    }

    public Address shift (int offset) {
      return this;
    }
  }

  public static class Malloc implements Address /*extends NullPointer*/ {
    private class BaseAddress {}

    private int offset;
    private int moreBytes;
    private BaseAddress base;

    public Malloc (int numBytes) {
      offset = 0;
      moreBytes = numBytes;
      base = new BaseAddress();
    }

    private Malloc () {}

    @Override
    public int hashCode () {
      return base.hashCode();
    }

    @Override
    public boolean equals (Object o) {
      if (!(o instanceof Malloc)) return false;
      Malloc m = (Malloc)o;
      if (m.base != base) return false;
      assert (m.offset + m.moreBytes == offset + moreBytes);
      return (m.offset == offset);
    }

    @Override
    public Malloc clone () {
      Malloc result = new Malloc();
      result.offset = offset;
      result.moreBytes = moreBytes;
      result.base = base;
      return result;
    }

    @Override
    public String toString () {
      return new String("##MALLOC+") + offset + "##";
    }

    public Address shift (int offset) {
      if (this.offset + offset < 0 || offset > moreBytes) return new InvalidPointer();
      this.offset += offset;
      this.moreBytes -= offset;
      return this;
    }
  }

  public static class AddressOfObject implements Address {
    private final Object obj;
    public AddressOfObject (Object obj) {
      this.obj = obj;
    }

    @Override
    public int hashCode () {
      return obj.hashCode() + 13;
    }

    @Override
    public boolean equals (Object o) {
      if (!(o instanceof AddressOfObject)) {
        return false;
      }

      return ((AddressOfObject)o).obj.equals(this.obj);
    }

    @Override
    public AddressOfObject clone () {
      // safe as long as shift is a no-op
      return this;
    }

    @Override
    public String toString () {
      return new String("&") + obj.toString();
    }

    public Address shift (int offset) {
      // safe, at least
      return new InvalidPointer ();
    }
  }

	private final Set<Address> values;

	public PointsToRelation (IASTDeclarator variable, String name) {
		super(variable, name);
		this.values = new HashSet<Address>();
		this.values.add(new InvalidPointer());
	}

	@Override
	public int hashCode () {
		return super.hashCode() + values.size();
	}

	@Override
	public boolean equals (Object o) {
		if (!(o instanceof PointsToRelation)) {
			return false;
		}
		PointsToRelation other = (PointsToRelation)o;

    if (!super.equals(other) || !other.values.equals(values)) {
      return false;
    }

		return true;
	}

	@Override
	public PointsToRelation clone() {
		PointsToRelation result = new PointsToRelation(variable, name);
		for (Address a : values) {
		  result.values.add(a.clone());
		}
		return result;
	}

	@Override
	public String toString () {
		String out = name + " = ";
		if (isInvalid()) {
			out += "##TOP##";
		} else {
		  out += "{";
		  Iterator<Address> iter = values.iterator();
		  while (iter.hasNext()) {
		    out += iter.next();
		  }
		  out += "}";
		}
		return out;
	}

  public Set<Address> getValues () {
    return values;
  }

  public boolean isInvalid () {
    return (!values.isEmpty() && values.iterator().next() instanceof InvalidPointer);
  }

  public void makeInvalid () {
    setAddress(new InvalidPointer());
  }

  public void makeNull () {
    setAddress(new NullPointer());
  }

  public void setAddress (Address address) {
    this.values.clear();
    this.values.add(address);
  }

	public void addAddress (Address address) {
		if (!isInvalid()) {
		  if (address instanceof InvalidPointer) {
		    makeInvalid();
		  } else {
		    this.values.add(address);
		  }
		}
	}

	public void addNull () {
		addAddress(new NullPointer());
	}

	public void makeAlias (PointsToRelation other) {
	  if (this == other) return;

		values.clear();
		values.addAll(other.values);
	}

	/*
	public Pointer find (PointsToRelation r, int offset) {
	  for (Pointer p : data) {
	    if (p.bracketOp(offset) == r) return p;
	  }

	  return null;
	}
	*/

	public void shift (int offset) {
	  Set<Address> backup = new HashSet<Address>(values);
	  values.clear();
	  for (Address a : backup) {
	    values.add(a.shift(offset));
	  }
	}

	public boolean subsetOf (final PointsToRelation other) {
		if (!other.variable.equals(variable)) return false;
		return other.isInvalid() || other.values.containsAll(values);
	}

	public void join (final PointsToRelation other) {
		assert (other.variable.equals(variable));
		assert (other.name.equals(name));
		if (other.isInvalid()) {
			makeInvalid();
		} else {
			values.addAll(other.values);
		}
	}
}
