package octagon;

import octagon.LibraryAccess;
import octagon.Octagon;

public class OctLibraryTest {

	public static void main(String[] args) {

		Octagon oct = LibraryAccess.universe(2);
		System.out.println(oct);
	}
}
