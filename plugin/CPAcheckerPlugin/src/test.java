import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.sosy_lab.cpachecker.plugin.eclipse.ITestListener;


public class test {

	static Set<Integer> myset= new HashSet<Integer>();
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		myset.add(1);
		myset.add(2);
		myset.add(3);
		myset.add(4);
		myset.add(5);
		
		
		for (final Iterator<Integer> iter = myset.iterator(); iter.hasNext();) {
			final Integer current = iter.next();
			ISafeRunnable runnable = new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					if (current == 3) {
						throw new IllegalArgumentException();
					}
				
				}
				
				@Override
				public void handleException(Throwable exception) {
					// TODO: remove listener from list?
					//We would need a real iterator for that! (cant modify the set while running the iterator)
					
					iter.remove();
				}
			};
			SafeRunner.run(runnable);
		}
		for (final Integer listener : myset) {
			System.out.println(listener);
		}
	}

}
