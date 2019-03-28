package org.nulist.plugin;
import com.grammatech.cs.*;
import org.nulist.plugin.parser.CFGParser;
import org.sosy_lab.cpachecker.cmdline.CPAMain;
import static org.nulist.plugin.parser.CFGParser.*;

/**
 * @ClassName FunctionTest
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/27/19 10:25 PM
 * @Version 1.0
 **/
public class FunctionTest {

    public static void test(project proj){
        try{
            System.out.println("==================TEST CSURF_PLUGIN_BEGIN==================");

            CFGParser parser = new CFGParser();

            for(project_compunits_iterator cu_it = proj.compunits();
                !cu_it.at_end(); cu_it.advance() )
            {
                compunit cu = cu_it.current();
                // only focus on user-defined c files
                if(targetFile(cu.name()))
                {
                    //input_file.add(Paths.get(cu.normalized_name()));
                    System.out.println(cu.name());
                    for (compunit_procedure_iterator proc_it = cu.procedures();
                         !proc_it.at_end(); proc_it.advance()) {
                        procedure proc = proc_it.current();
                        if(proc.get_kind().equals(procedure_kind.getUSER_DEFINED())||
                                proc.get_kind().equals(procedure_kind.getLIBRARY())){
                            point_set pointSet = proc.points();
                            for(point_set_iterator point_it = pointSet.cbegin();
                                !point_it.at_end(); point_it.advance()){
                                point p = point_it.current();
                                if(p.get_kind().equals(point_kind.getEXPRESSION())){
                                    ast un_ast = p.get_ast(ast_family.getC_UNNORMALIZED());
                                    ast no_ast = p.get_ast(ast_family.getC_NORMALIZED());
                                    if(no_ast.pretty_print().contains("$temp")){

                                    }

                                }
                            }
                                //
                        }
                    }
                }
            }

            System.out.println("==================TEST CSURF_PLUGIN_END==================");
        }catch(result r){
            System.out.println("Uncaught exception: " + r);
        }
    }


}
