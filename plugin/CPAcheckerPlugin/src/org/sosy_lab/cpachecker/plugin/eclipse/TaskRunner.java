package org.sosy_lab.cpachecker.plugin.eclipse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.StreamHandler;

import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.LogManager.ConsoleLogFormatter;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.plugin.eclipse.preferences.PreferenceConstants;

public class TaskRunner {

	public static void run(final List<Task> tasks) {
		CPAcheckerPlugin.getPlugin().fireTasksStarted(tasks.size());

		CPAcheckerPlugin.getPlugin().addTestListener(
				new ITestListener.DefaultImplementation() {
					int currentTaskIndex = 1;

					@Override
					public void taskHasPreRunError(Task t, String errorMessage) {
						if (currentTaskIndex >= tasks.size()) {
							CPAcheckerPlugin.getPlugin().fireTasksFinished();
						} else {
							startNext();
						}
					}

					@Override
					public void taskFinished(Task id, CPAcheckerResult results) {
						if (currentTaskIndex >= tasks.size()) {
							CPAcheckerPlugin.getPlugin().fireTasksFinished();
						} else {
							startNext();
						}
					}

					private void startNext() {
						startSingleTask(tasks.get(currentTaskIndex++));
					}
				});
		startSingleTask(tasks.get(0));
	}

	public static void startSingleTask(Task t) {
		CPAcheckerPlugin.getPlugin().fireTaskStarted(t);
		if (t.hasPreRunError()) {
			CPAcheckerPlugin.getPlugin()
					.firePreRunError(t, t.getErrorMessage());
		} else {
			MessageConsole con = createMessageConsole("CPAchecker : "
					+ t.getName());
			Configuration config;
			try {
				config = t.createConfig();
				MessageConsoleStream outStream = con.newMessageStream();
				LogManager logger = new LogManager(config, new StreamHandler(
						outStream, new ConsoleLogFormatter()));
				Thread run = new Thread(new TaskRun(t.getTranslationUnit(),
						config, outStream, logger, t));
				run.start();
			} catch (IOException e) {
				// cannot happen because this would have been a preRunError
				assert false;
			} catch (CoreException e) {
				// cannot happen because this would have been a preRunError
				assert false;
			} catch (InvalidConfigurationException e) {
				// cannot happen because this would have been a preRunError
				assert false;
			}
		}
	}

	public static void runParallel(List<Task> tasks) {
		CPAcheckerPlugin.getPlugin().fireTasksStarted(tasks.size());
		for (Task t : tasks) {
			CPAcheckerPlugin.getPlugin().fireTaskStarted(t);
			if (t.hasPreRunError()) {
				CPAcheckerPlugin.getPlugin().firePreRunError(t,
						t.getErrorMessage());
			} else {
				MessageConsole con = createMessageConsole("CPAchecker : "
						+ t.getName());
				Configuration config;
				try {
					config = t.createConfig();
					MessageConsoleStream outStream = con.newMessageStream();
					LogManager logger = new LogManager(config,
							new StreamHandler(outStream,
									new ConsoleLogFormatter()));
					Thread run = new Thread(new TaskRun(t.getTranslationUnit(),
							config, outStream, logger, t));
					run.start();
				} catch (IOException e) {
					// cannot happen because this would have been a preRunError
					assert false;
				} catch (CoreException e) {
					// cannot happen because this would have been a preRunError
					assert false;
				} catch (InvalidConfigurationException e) {
					// cannot happen because this would have been a preRunError
					assert false;
				}
			}
		}
		// wont work
		// CPAcheckerPlugin.getPlugin().fireTasksFinished();
	}

	private static MessageConsole createMessageConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		myConsole.activate();
		return myConsole;
	}

	private static class TaskRun implements Runnable {
		private ITranslationUnit source;
		private Configuration config;
		private MessageConsoleStream consoleStream;
		private LogManager logger;
		private Task task;

		TaskRun(ITranslationUnit source, Configuration config,
				MessageConsoleStream outStream, LogManager logger, Task t) {
			super();
			this.task = t;
			this.source = source;
			this.config = config;
			this.consoleStream = outStream;
			this.logger = logger;
		}

		@Override
		public void run() {
			try {
				CPAchecker cpachecker = new CPAchecker(config, logger);
				final CPAcheckerResult results = cpachecker.run(source.getLocation().toOSString());
				logger.flush();
				if (CPAcheckerPlugin.getPlugin().getPreferenceStore().getBoolean(PreferenceConstants.P_STATS)) {
					results.printStatistics(new PrintWriter(consoleStream));					
				} else {
					switch (results.getResult()) {
					case SAFE:
						//color: green, doesnt work, threading issues
						//consoleStream.setColor(new Color(CPAcheckerPlugin.getPlugin().getWorkbench().getDisplay(), 0, 255,0));
						consoleStream.println("\nCPA run was safe. No Error locations found.");
						break;
					case UNKNOWN:
						//color: blue, doesnt work, threading issues
						//consoleStream.setColor(new Color(CPAcheckerPlugin.getPlugin().getWorkbench().getDisplay(), 0, 0,255));
						consoleStream.println("\nThe CPA run could not be terminated correctly. The result is unknown.");
						break;
					case UNSAFE:
						// color: red, doesnt work, threading issues
						//consoleStream.setColor(new Color(CPAcheckerPlugin.getPlugin().getWorkbench().getDisplay(), 255, 0,0));
						consoleStream.println("\nCPA found a reachable error location. The program is UNSAFE!");
						break;
					}
				}
				consoleStream.close();
				task.setLastResult(results.getResult());
				// finshedAnnouncement must be fired in Eclipse UI thread
				CPAcheckerPlugin.getPlugin().getWorkbench().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						CPAcheckerPlugin.getPlugin().fireTaskFinished(task, results);	
					}
				});
			} catch (Exception e) {
				System.err.println("Task \"" + task.getName() + "\" run has thrown an exception:");
				e.printStackTrace();
			}			
		}
	}
}
