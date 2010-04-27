package org.sosy_lab.cpachecker.plugin.eclipse;

public interface ITestListener {
	void testsStarted(int testCount);
	void testsFinished();
	void testStarted(String id);
	void testFailed(String id);
}
