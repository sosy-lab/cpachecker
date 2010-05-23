import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.SpringLayoutAlgorithm;



public class test {

	static Set<Integer> myset= new HashSet<Integer>();
	/**
	 * @param args
	 */
	public static void main(String[] args) {
			Display d = new Display();
			Shell shell = new Shell(d);
			
			/* The DOT input, can be given as a String, File or IFile: */
			/*DotImport importer = new DotImport(new File(
					"D:\\Meine Dateien\\Uni\\Vorlesungen 0910\\Software Analyse\\runtime-EclipseApplication\\.metadata\\.plugins\\org.sosy_lab.cpachecker.plugin.eclipse\\results\\generated\\cfa.dot"));
					*/			
			/* Or create a Zest graph instance in a parent, with a style: */
			/*Graph graph = importer.newGraphInstance(shell, SWT.NONE);*/
			
			
			
			Image image1 = Display.getDefault().getSystemImage(SWT.ICON_INFORMATION);
			Image image2 = Display.getDefault().getSystemImage(SWT.ICON_WARNING);
			Image image3 = Display.getDefault().getSystemImage(SWT.ICON_ERROR);
			
			shell.setLayout(new FillLayout());
			shell.setSize(400, 400);

			Graph g = new Graph(shell, SWT.NONE);
			
			g.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED);
			GraphNode n1 = new GraphNode(g, SWT.NONE, "Information", image1);
			GraphNode n2 = new GraphNode(g, SWT.NONE, "Warning", image2);
			GraphNode n3 = new GraphNode(g, SWT.NONE, "Error", image3);

			new GraphConnection(g, SWT.NONE, n1, n2);
			new GraphConnection(g, SWT.NONE, n2, n3);
			new GraphConnection(g, SWT.NONE, n3, n3);

			g.setLayoutAlgorithm(new SpringLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);
			
			shell.open();
			while (!shell.isDisposed()) {
				while (!d.readAndDispatch()) {
					d.sleep();
				}
			}
			
			image1.dispose();
			image2.dispose();
			image3.dispose();
			
		}
	
	public Graph parseDotFile(String filename, Composite shell) {
		try {
			Graph g = new Graph(shell, SWT.NONE);
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = null;
			while (( line = br.readLine()) != null) {
				
				
			}
		
		
		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}


}
