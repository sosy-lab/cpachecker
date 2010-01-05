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
}
