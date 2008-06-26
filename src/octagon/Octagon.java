package octagon;

public class Octagon {

	private int dimension;	/* number of variables, aka dimension */
	private int ref;		/* reference counting */
	private int state;		/* empty 0, normal 1, closed 2*/
	private Octagon closed;  	/* pointer to the closed version, or NULL */
	private Num[] matrix;

	public Octagon(int dimension, int ref, int state, Octagon closed, Num[] matrix) {
		this.dimension = dimension;
		this.ref = ref;
		this.state = state;
		this.closed = closed;
		this.matrix = matrix;
	}

	public int getDimension() {
		return dimension;
	}

	public void setDimension(int dimension) {
		this.dimension = dimension;
	}

	public int getRef() {
		return ref;
	}

	public void setRef(int ref) {
		this.ref = ref;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public Octagon getClosed() {
		return closed;
	}

	public void setClosed(Octagon closed) {
		this.closed = closed;
	}

	public Num[] getMatrix() {
		return matrix;
	}

	public void setMatrix(Num[] matrix) {
		this.matrix = matrix;
	}

	//@Override
	//public String toString() {,,,}

	public String getContents(){
		String s = "";

		s = s + "Dimension: " + this.dimension + " Ref. Counting: " 
		+ this.ref + " State: " + this.state + "\n";

		if(matrix != null){
			for(int i=0; i<this.matrix.length; i++){
				s = s + this.matrix[i] + " ";
			}
		}
		else s = s + "Array is NULL ";

		if(closed != null){
			s = s + " CLOSED is NOT NULL ";
		}
		else s = s + " // closed is NULL ";

		return s;
	}

	public int matSize(){
		return 2 * dimension * (dimension+1);
	}

	public int matPos(int i, int j){
		return (j+((i+1)*(i+1))/2);
	}

	public int matPos2(int i, int j){
		return ((i)<(j)?matPos((j^1),(i^1)):matPos(i,j));
	}
}
