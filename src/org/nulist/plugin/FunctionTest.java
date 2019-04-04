package org.nulist.plugin;
import com.grammatech.cs.*;
import org.nulist.plugin.parser.CFGFunctionBuilder;
import org.nulist.plugin.parser.CFGHandleExpression;
import org.nulist.plugin.parser.CFGParser;
import org.nulist.plugin.parser.CFGTypeConverter;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cmdline.CPAMain;
import static org.nulist.plugin.parser.CFGParser.*;
import static org.nulist.plugin.util.FileOperations.*;

/**
 * @ClassName FunctionTest
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/27/19 10:25 PM
 * @Version 1.0
 **/
public class FunctionTest {


    public static void functionTest(CFGFunctionBuilder builder)throws result{
        if(!builder.functionName.equals("rrc_eNB_generate_HO_RRCConnectionReconfiguration"))
            return;
        point_set pointSet = builder.function.points();
        for(point_set_iterator point_it=pointSet.cbegin();!point_it.at_end();point_it.advance()){
            point node = point_it.current();
            if(node.get_kind().equals(point_kind.getEXPRESSION())){
                if(node.toString().contains("ReportConfig_A4->reportConfig.choice.reportConfigEUTRA.triggerType.choice.event.eventId.present = 4")){
                    ast un_ast = node.get_ast(ast_family.getC_UNNORMALIZED());
                    FileLocation fileLocation = getLocation(node);
                    CStatement statement = builder.expressionHandler.getAssignStatementFromUC(un_ast, fileLocation);
                    System.out.println(statement.toString());
                }
            }
        }
    }

    public static void test(project proj){
        try{
            System.out.println("==================TEST CSURF_PLUGIN_BEGIN==================");

            CFGParser parser = new CFGParser();
            CFGTypeConverter typeConverter = new CFGTypeConverter(null);


            for(project_compunits_iterator cu_it = proj.compunits();
                !cu_it.at_end(); cu_it.advance() )
            {
                compunit cu = cu_it.current();
                // only focus on user-defined c files
                if(targetFile(cu.name(),proj.name()))
                {
                    //input_file.add(Paths.get(cu.normalized_name()));
                    System.out.println(cu.name());
                    for (compunit_procedure_iterator proc_it = cu.procedures();
                         !proc_it.at_end(); proc_it.advance()) {
                        procedure proc = proc_it.current();
                        if(!proc.name().equals("rrc_eNB_generate_HO_RRCConnectionReconfiguration"))
                            continue;
                        CFGHandleExpression expressionhandler = new CFGHandleExpression(null,proc.name(),typeConverter);

                        if(proc.get_kind().equals(procedure_kind.getUSER_DEFINED())||
                                proc.get_kind().equals(procedure_kind.getLIBRARY())){
                            point_set pointSet = proc.points();
                            for(point_set_iterator point_it = pointSet.cbegin();
                                !point_it.at_end(); point_it.advance()){
                                point p = point_it.current();
                                if(p.get_kind().equals(point_kind.getEXPRESSION())){
                                    if(p.toString().contains("ReportConfig_A4->reportConfig.choice.reportConfigEUTRA.triggerType.choice.event.eventId.present = 4")){
                                        ast un_ast = p.get_ast(ast_family.getC_UNNORMALIZED());
                                        FileLocation fileLocation = getLocation(p);
                                        CStatement statement = expressionhandler.getAssignStatementFromUC(un_ast, fileLocation);
                                        System.out.println(statement.toString());
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
