package org.sosy_lab.cpachecker.plugin.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.StreamHandler;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.progress.IProgressService;
import org.sosy_lab.common.DuplicateOutputStream;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.LogManager.ConsoleLogFormatter;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.plugin.eclipse.preferences.PreferenceConstants;


public class TaskRunner {

	public static class CloseCurrentConsoleAction extends Action {
		static final String ID = "org.sosy_lab.cpachecker.plugin.eclipse.TaskView.CloseConsoleAction";
		private IConsoleView consoleView;
		public CloseCurrentConsoleAction(IConsoleView view) {
			this.consoleView = view;
		}
		@Override
		public String getId() {
			return ID;
		}
		@Override
		public ImageDescriptor getImageDescriptor() {
			return CPAclipse.getImageDescriptor("icons/rem_co.gif");
		}
		
		@Override
		public String getText() {
			return "close Console";
		}
		@Override
		public void run() {
			ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[] {consoleView.getConsole()});
			super.run();
		}
	}

	public static void run(final List<Task> tasks) {
		//CPAcheckerPlugin.getPlugin().getWorkbench().getProgressService()
		CPAclipse.getPlugin().fireTasksStarted(tasks.size());
		IProgressService service = CPAclipse.getPlugin().getWorkbench().getProgressService();
		for (Task t : tasks) {
			try {
				service.run(false, true, new TaskRun(t));
			} catch (InvocationTargetException e) {
				CPAclipse.logError(e);
			} catch (InterruptedException e) {
				CPAclipse.logError(e);
			}
		}
		
		CPAclipse.getPlugin().fireTasksFinished();
		CPAclipse.getPlugin().fireTasksChanged(tasks);
		
	}

	/*public static void startSingleTask(Task t) {
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
	}*/
/*
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
	}*/

	private static class TaskRun implements IRunnableWithProgress {
		private PrintStream consoleStream;
		private LogManager logger;
		private Task task;

		TaskRun(Task t) {
			super();
			this.task = t;
			// Sync Exec !
			CPAclipse.getPlugin().getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					ConsolePlugin plugin = ConsolePlugin.getDefault();
					final IConsoleManager conMan = plugin.getConsoleManager();
					final IOConsole console = new IOConsole(task.getName(), null) {
						@Override
						public IPageBookViewPage createPage(IConsoleView view) {
							IToolBarManager toolBarManager = view.getViewSite().getActionBars().getToolBarManager();
							if (toolBarManager.find(CloseCurrentConsoleAction.ID) == null) {
								IContributionItem firstItem = toolBarManager.getItems()[0];
								if (firstItem == null) {
									toolBarManager.add(new CloseCurrentConsoleAction(view));
								} else {
									toolBarManager.insertBefore(firstItem.getId(), new CloseCurrentConsoleAction(view));
								}
								
							}
							return super.createPage(view);							
						}
						
					};
					conMan.addConsoles(new IConsole[] { console });
					//myConsole.activate();
					consoleStream = new PrintStream(console.newOutputStream());
				}
			});
		}
		
		@Override
		public void run(IProgressMonitor monitor) {
			fireTaskStarted();
			/*try {
				task.getOutputDirectory(false).delete(IResource.FORCE, monitor);
			} catch (CoreException e2) {
				CPAclipse.logError("Could not delete output directory prior to running task " + task.getName(), e2);
			}*/
			monitor.subTask("Running: " + task.getName());
			if (monitor.isCanceled()) {
				fireTaskFinished(null, monitor);
				return;
			} else if (task.hasPreRunError()) {
				firePreRunError();
				return;
			} else {
				try {
					Configuration config = task.createConfig();
					
					{
				        // We want to be able to use options of type "File" with some additional
				        // logic provided by FileTypeConverter, so we create such a converter,
				        // add it to our Configuration object and to the the map of default converters.
				        // The latter will ensure that it is used whenever a Configuration object
				        // is created.
				        FileTypeConverter fileTypeConverter = new FileTypeConverter(config);
	
				        config = Configuration.builder()
				                            .copyFrom(config)
				                            .addConverter(FileOption.class, fileTypeConverter)
				                            .build();
	
				        Configuration.getDefaultConverters()
				                     .put(FileOption.class, fileTypeConverter);
				    }
					
					logger = new LogManager(config, new StreamHandler(
							consoleStream, new ConsoleLogFormatter(config)));
					CPAchecker cpachecker = new CPAchecker(config, logger);
					final CPAcheckerResult results = cpachecker.run(task.getTranslationUnit().getLocation().toOSString());
					logger.flush();
					
					OutputStream console = null;
					if (CPAclipse.getPlugin().getPreferenceStore().getBoolean(PreferenceConstants.P_STATS)) {
						console = consoleStream;
					}
					
					OutputStream fileStream = null;
					IFolder outDir = task.getOutputDirectory(true);
					if (outDir.exists()) {
						IFile result = outDir.getFile("VerificationResult.txt");						
						File f = new File(result.getLocation().toPortableString());
						f.createNewFile();
						fileStream = new FileOutputStream(f);
						
						IFile prevConfig = outDir.getFile("UsedConfiguration.properties");
						if (prevConfig.exists()) {
							prevConfig.delete(true, true, null);
						}
						task.getConfigFile().copy(prevConfig.getFullPath(), true, null);
						
						IFile prevSpec = outDir.getFile("UsedSpecification.spc");
						if (prevSpec.exists()) {
							prevSpec.delete(true, true, null);
						}
						task.getSpecFile().copy(prevSpec.getFullPath(), true, null);
					}
					
					PrintStream outputStream = makePrintStream(DuplicateOutputStream.mergeStreams(console, fileStream));
					results.printStatistics(outputStream);	
					outputStream.println("");

					switch (results.getResult()) {
					case SAFE:
						//color: green, doesnt work, threading issues
						//consoleStream.setColor(new Color(CPAcheckerPlugin.getPlugin().getWorkbench().getDisplay(), 0, 255,0));
						break;
					case UNKNOWN:
						//color: blue, doesnt work, threading issues
						//consoleStream.setColor(new Color(CPAcheckerPlugin.getPlugin().getWorkbench().getDisplay(), 0, 0,255));
						break;
					case UNSAFE:
						// color: red, doesnt work, threading issues
						//consoleStream.setColor(new Color(CPAcheckerPlugin.getPlugin().getWorkbench().getDisplay(), 255, 0,0));
						break;
					}
					
					if (console == null) {
						outputStream = makePrintStream(DuplicateOutputStream.mergeStreams(consoleStream, fileStream));
					}
					results.printResult(outputStream);
					outputStream.flush();
					
					if (fileStream != null) {
						fileStream.close();
					}
					if (outDir.exists()) {
						outDir.refreshLocal(IResource.DEPTH_ONE, null);
					}
					
					task.setLastResult(results.getResult());
					
					// finshedAnnouncement must be fired in Eclipse UI thread
					fireTaskFinished(results, monitor);
				} catch (Exception e) {
					if (consoleStream!= null) {
						consoleStream.println("Evaluation of Task "+ task.getName() + " has thrown an exception");
						e.printStackTrace(consoleStream);
						/*try {
							consoleStream.close();
						} catch (IOException e1) {
							CPAclipse.logError("OutputStream of the console could not be closed", e);
						}*/
					}
					CPAclipse.logError("Evaluation of Task "+ task.getName() + " has thrown an exception", e);
				} finally {
					if (logger != null)
						logger.close();
					fireTaskFinished(null, monitor);
					monitor.worked(1);
				}
			}
		}
		private void firePreRunError() {
			CPAclipse.getPlugin().getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					CPAclipse.getPlugin().firePreRunError(task, task.getErrorMessage());	
				}
			});
		}

		void fireTaskStarted() {
			CPAclipse.getPlugin().getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					CPAclipse.getPlugin().fireTaskStarted(task);	
				}
			});
		}
		void fireTaskFinished(final CPAcheckerResult results, final IProgressMonitor monitor) {
			CPAclipse.getPlugin().getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					try {
						task.getOutputDirectory(false).refreshLocal(IResource.DEPTH_INFINITE, monitor);
					} catch (CoreException e) {
						CPAclipse.logError("Could not refresh the output directory of Task " + task.getName(), e);
					}
					CPAclipse.getPlugin().fireTaskFinished(task, results);	
				}
			});
		}

	    private static PrintStream makePrintStream(OutputStream stream) {
	      if (stream instanceof PrintStream) {
	        return (PrintStream)stream;
	      } else {
	        return new PrintStream(stream);
	      }
	    }
	}
}
