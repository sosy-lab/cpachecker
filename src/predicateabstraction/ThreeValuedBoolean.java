package predicateabstraction;

public enum ThreeValuedBoolean {
	TRUE,
	FALSE,
	DONTKNOW;
	
	@Override
	public String toString() {
		switch(this) {
		case TRUE:   return "TRUE";
		case FALSE:  return "FALSE";
		case DONTKNOW:  return "DONTKNOW";
		}
		return "";
	}
}
