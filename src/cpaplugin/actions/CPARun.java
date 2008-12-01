package cpaplugin.actions;

import java.io.PrintStream;

import logging.CPACheckerLogger;
import logging.CustomLogLevel;

import org.eclipse.cdt.core.dom.CDOM;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.swt.widgets.Display;

import cmdline.CPAMain;

import cpaplugin.CPAConfiguration;

public class CPARun implements IWorkbenchWindowActionDelegate
{
	private boolean init = false;
	private int numberOfRuns = 0;
	private IWorkbenchWindow window;

	/**
	 * The constructor.
	 */
	public CPARun ()
	{

	}
	private void init()
	{
		if(init == false)
		{
			MessageConsole myConsole = findConsole("CPACHECKER");
			IOConsoleOutputStream outStream = myConsole.newOutputStream();
			IOConsoleOutputStream errStream = myConsole.newOutputStream();
			errStream.setColor(new org.eclipse.swt.graphics.Color(Display.getDefault(), 255,0,0));
			System.setOut(new PrintStream(outStream));
			System.setErr(new PrintStream(errStream));
			init = true;
		}
		CPACheckerLogger.clear();
		CPACheckerLogger.init();
		return;
	}
	private MessageConsole findConsole(String name) {
	      ConsolePlugin plugin = ConsolePlugin.getDefault();
	      IConsoleManager conMan = plugin.getConsoleManager();
	      IConsole[] existing = conMan.getConsoles();
	      for (int i = 0; i < existing.length; i++)
	         if (name.equals(existing[i].getName()))
	            return (MessageConsole) existing[i];
	      //no console found, so create a new one
	      MessageConsole myConsole = new MessageConsole(name, null);
	      conMan.addConsoles(new IConsole[]{myConsole});
	      return myConsole;
	   }
	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 *
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run (IAction action)
	{
		numberOfRuns++;
	    String s[] = {};
	    CPAMain.cpaConfig = new CPAConfiguration(s);
	    if(!CPAMain.cpaConfig.validConfig)
	    {
	    	return;
	    }
	    //Lets set up a console to write to
	    init();
		MessageConsole myConsole = findConsole("CPACHECKER");
		CPACheckerLogger.log(CustomLogLevel.INFO, "Run #: " + numberOfRuns);
		CPACheckerLogger.log(CustomLogLevel.INFO, "Program Started");

		MessageDialog.openInformation (window.getShell (), "CPAPlugin Plug-in", "Launching CPAChecker Eclipse Plugin");

		try
		{
			// Get the current document
			IWorkbench workbench = PlatformUI.getWorkbench ();
			if (workbench == null)
			{
				MessageDialog.openInformation (window.getShell (), "CPAPlugin Plug-in", "Workbench cannot be found");
				return;
			}

			IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow ();
			if (workbenchWindow == null)
			{
				MessageDialog.openInformation (window.getShell (), "CPAPlugin Plug-in", "No active workbench window");
				return;
			}

			IWorkbenchPage workbenchPage = workbenchWindow.getActivePage ();
			if (workbenchPage == null)
			{
				MessageDialog.openInformation (window.getShell (), "CPAPlugin Plug-in", "No active page in active workbench window");
				return;
			}

			IEditorPart editorPart = workbenchPage.getActiveEditor ();
			if (editorPart == null)
			{
				MessageDialog.openInformation (window.getShell (), "CPAPlugin Plug-in", "No active editor in the active workbench");
				return;
			}

			IEditorInput editorInput = editorPart.getEditorInput ();
			if (editorInput == null)
			{
				MessageDialog.openInformation (window.getShell (), "CPAPlugin Plug-in", "No active editor input in active editor part");
				return;
			}

			IFile currentFile = (IFile) editorInput.getAdapter (IFile.class);
			if (currentFile == null)
			{
				MessageDialog.openInformation (window.getShell (), "CPAPlugin Plug-in", "No file associated with current editor input");
				return;
			}
			String extension = currentFile.getFileExtension();
			if(extension.compareTo("c") != 0)
			{
				MessageDialog.openInformation (window.getShell (), "CPAPlugin Plug-in", "Cannot parse non-c file");
				return;
			}
			// Get Eclipse to parse the C in the current file
			IASTTranslationUnit ast = null;
			try
			{
				ast = CDOM.getInstance ().getTranslationUnit (currentFile);
			}
			catch (Exception e)
			{
				e.printStackTrace ();
				e.getMessage ();

				MessageDialog.openInformation (window.getShell (), "CPAPlugin Plug-in", "Eclipse had trouble parsing C");
				return;
			}

			//Now grab its attention and display
		    String id = IConsoleConstants.ID_CONSOLE_VIEW;
		    IConsoleView view = (IConsoleView) workbenchPage.showView(id);
		    view.display(myConsole);

		    //Now run analysis
			CPAMain.doRunAnalysis(ast);
		}
		catch (Exception e)
		{
			System.out.println (e.getMessage ());
			e.printStackTrace ();
		}
	}

	/**
	 * Selection in the workbench has been changed. We can change the state of
	 * the 'real' action here if we want, but this can only happen after the
	 * delegate has been created.
	 *
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged (IAction action, ISelection selection)
	{
	}

	/**
	 * We can use this method to dispose of any system resources we previously
	 * allocated.
	 *
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose ()
	{
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 *
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init (IWorkbenchWindow window)
	{
		this.window = window;
	}
}