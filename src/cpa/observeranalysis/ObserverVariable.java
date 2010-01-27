package cpa.observeranalysis;

/** Represents a local variable of the observer automaton.
 * So far only integer variables are supported.
 * @author rhein
 */
class ObserverVariable {
  private int value;
  private String name;
  public ObserverVariable(String type, String name) {
    if (type.toLowerCase().equals("int") || (type.toLowerCase().equals("integer"))) {
      value = 0;
      this.name = name;
    } else {
      throw new IllegalArgumentException("Only Type int supported");
    }
  }
  
  public String getName() {
    return name;
  }
  
  public int getValue() {
    return value;
  }
  public void setValue(int v) {
    value = v;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
  
  @Override
  public ObserverVariable clone() {
    ObserverVariable ret = new ObserverVariable("int", name);
    ret.setValue(getValue());
    return ret;
  }
  @Override
  public boolean equals(Object pObj) {
    if (super.equals(pObj)) {
      return true;
    }
    if (!(pObj instanceof ObserverVariable)) {
      return false;
    }
    ObserverVariable otherVar = (ObserverVariable) pObj;
    return (this.value == otherVar.value) && this.name.equals(otherVar.name);
  }
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   * 
   * I don't use the hashcode, but it should be redefined every time equals is overwritten.
   */
  @Override
  public int hashCode() {
    return this.value + this.name.hashCode(); 
  }
}
