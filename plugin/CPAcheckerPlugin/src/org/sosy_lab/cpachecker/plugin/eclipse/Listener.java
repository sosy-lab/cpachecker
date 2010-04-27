package org.sosy_lab.cpachecker.plugin.eclipse;

public class Listener implements ITestListener {

	@Override
	public void testFailed(String id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testStarted(String id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testsFinished() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testsStarted(int testCount) {
		// TODO Auto-generated method stub
		System.out.println(testCount + " Tests started");
		
	}

}
