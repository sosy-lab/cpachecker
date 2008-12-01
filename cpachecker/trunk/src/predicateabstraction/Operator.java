package predicateabstraction;

public enum Operator {

	equals,
	notEquals,
	smaller,
	larger,
	smallarOrEqual,
	largerOrEqual;

	@Override
	public String toString() {
		switch(this) {
		case equals:   return "=";
		case notEquals:  return "~ =";
		case smaller:  return "<";
		case larger: return ">";
		case smallarOrEqual: return "<=";
		case largerOrEqual: return ">=";
		}
		return "";
	}

	public static Operator convertToOperator(String s){

		if(s.compareTo("=") == 0){
			return equals;
		}
		else if(s.compareTo("!=") == 0){
			return notEquals;
		}
		else if(s.compareTo("<") == 0){
			return smaller;
		}
		else if(s.compareTo(">") == 0){
			return larger;
		}
		else if(s.compareTo("<=") == 0){
			return smallarOrEqual;
		}
		else if(s.compareTo(">=") == 0){
			return largerOrEqual;
		}
		else{
			System.out.println("Invalid Input " + s);
			System.out.println("Remove the new line on predicated file");
			return null;
		}
	}
}
