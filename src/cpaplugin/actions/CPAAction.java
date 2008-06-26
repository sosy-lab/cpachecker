package cpaplugin.actions;

import java.util.Collection;
import java.util.List;

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

import cpaplugin.CPAConfig;
import cpaplugin.cfa.CFABuilder;
import cpaplugin.cfa.CFASimplifier;
import cpaplugin.cfa.DOTBuilder;
import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cpa.common.CPAAlgorithm;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.demo.DemoCPA;

public class CPAAction implements IWorkbenchWindowActionDelegate
{
    private IWorkbenchWindow window;

    /**
     * The constructor.
     */
    public CPAAction ()
    {
    }

    private ConfigurableProblemAnalysis getCPA ()
    {
        return DemoCPA.getDemoCPA ();
    }
    
    private AbstractElement getInitialElement (CFAFunctionDefinitionNode cfa)
    {
        return DemoCPA.getInitialElement (cfa);
    }
    
    /**
     * The action has been activated. The argument of the method represents the
     * 'real' action sitting in the workbench UI.
     * 
     * @see IWorkbenchWindowActionDelegate#run
     */
    public void run (IAction action)
    {
        MessageDialog.openInformation (window.getShell (), "CPAPlugin Plug-in", "Launching Geoff's CPA Plugin");

        try
        {
            // Get the current document
            IWorkbench workbench = PlatformUI.getWorkbench ();
            if (workbench == null)
            {
                System.out.println ("Workbench cannot be found");
                return;
            }

            IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow ();
            if (workbenchWindow == null)
            {
                System.out.println ("No active workbench window");
                return;
            }

            IWorkbenchPage workbenchPage = workbenchWindow.getActivePage ();
            if (workbenchPage == null)
            {
                System.out.println ("No active page in active workbench window");
                return;
            }

            IEditorPart editorPart = workbenchPage.getActiveEditor ();
            if (editorPart == null)
            {
                System.out.println ("No active editor in the active workbench");
                return;
            }

            IEditorInput editorInput = editorPart.getEditorInput ();
            if (editorInput == null)
            {
                System.out.println ("No active editor input in active editor part");
                return;
            }

            IFile currentFile = (IFile) editorInput.getAdapter (IFile.class);
            if (currentFile == null)
            {
                System.out.println ("No file associated with current editor input");
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
                
                System.out.println ("Eclipse had trouble parsing C");
                return;
            }

            CFABuilder builder = new CFABuilder ();
            ast.accept (builder);
            List<CFAFunctionDefinitionNode> cfas = builder.getCFAs ();
            int numFunctions = cfas.size ();

            System.out.println (numFunctions + " functions parsed");

            // Iterate over all functions and perform CPA on each
            DOTBuilder dotBuilder = new DOTBuilder (); 
            for (int funcIdx = 0; funcIdx < numFunctions; funcIdx++)
            {
                CFAFunctionDefinitionNode cfa = cfas.get (funcIdx);

                if (CPAConfig.exportDOTfiles)
                    dotBuilder.generateDOT (cfa, CPAConfig.DOTOutputPath + "dot" + Integer.toString (funcIdx) + ".dot");

                if (CPAConfig.simplifyCFA)
                {
                    CFASimplifier simplifier = new CFASimplifier (true);
                    simplifier.simplify (cfa);
                    
                    if (CPAConfig.exportDOTfiles) // If we've simplified the CFA, also export to DOT the simplified version
                        dotBuilder.generateDOT (cfa, CPAConfig.DOTOutputPath + "dot" + Integer.toString (funcIdx) + "simple.dot");
                }

                ConfigurableProblemAnalysis cpa = getCPA ();
                CPAAlgorithm algo = new CPAAlgorithm ();

                AbstractElement initialElement = getInitialElement (cfa);
                Collection<AbstractElement> reached = algo.CPA (cpa, initialElement);

                System.out.println ("Reached CPA Size: " + reached.size () + " for function: " + cfa.getFunctionName ());

                for (AbstractElement element : reached)
                {
                    System.out.println (element.toString ());
                }
            }
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