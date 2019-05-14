package org.nulist.plugin;

import com.grammatech.cs.*;
import org.sosy_lab.cpachecker.cmdline.CPAMain;

import java.lang.*;

import static org.nulist.plugin.util.CFGDumping.dumpCFG2Dot;
import static org.nulist.plugin.util.ClassTool.*;
//Combine CPAChecker as a plugin of CodeSurfer

public class CSurfPlugin {
    private final static String ENBProjectPath = "/OAI-ENB/OAI-ENB.prj_files/OAI-ENB.sdg";
    private final static String MMEProjectPath = "/OAI-MME/OAI-MME.prj_files/OAI-MME.sdg";
    private final static String UEProjectPath = "/OAI-UE/OAI-UE.prj_files/OAI-UE.sdg";

    /**
     * @Description
     * @Param [args]
     * @return void
     **/
    public static void main(String args[]){

        //Arguments to CPAChecker
        String[] arguments = null;
        String cpacheckPath ="";
        String projectPath = "";
        if(args.length>=3){
            cpacheckPath = args[0];
            projectPath = args[1];
            arguments = args[2].split(" ");
        }

        //perform parser execution
        try{
            printINFO("==================CSURF_PLUGIN_BEGIN==================");
//            printINFO("==================Parsing UE==================");
//            project.load(projectPath+UEProjectPath,true);
//            project proj = project.current();
//            CPAMain.executionTesting(arguments, cpacheckPath, projectPath+UEProjectPath, proj);
            //project.unload();
//            printINFO("==================Finish==================");
            printINFO("==================Parsing ENB==================");
            project.load(projectPath+ENBProjectPath,true);
            project proj = project.current();
            CPAMain.executionTesting(arguments, cpacheckPath, projectPath+ENBProjectPath, proj);
            //project.unload();
            printINFO("==================Finish==================");
//            printINFO("==================Parsing MME==================");
//            project.load(projectPath+MMEProjectPath,true);
//            proj = project.current();
//            CPAMain.executionTesting(arguments, cpacheckPath, projectPath+MMEProjectPath, proj);
//            //project.unload();
//            printINFO("==================Finish==================");

            //CPAMain.executeParser(arguments, cpacheckPath, programPath, proj);
            project.unload();
            printINFO("==================CSURF_PLUGIN_END==================");
        }catch(result r){
            System.out.println("Uncaught exception: " + r);
        }
    }

    private static void dumpCFG(project target, String path) throws result{

        for( project_compunits_iterator cu_it = target.compunits();
             !cu_it.at_end();
             cu_it.advance() )
        {
            compunit cu = cu_it.current();//each shall be a C file

            if(!cu.is_user())
                continue;
            // Iterate over all procedures in the compilation unit
            // procedure = function
            for( compunit_procedure_iterator proc_it = cu.procedures();
                 !proc_it.at_end();
                 proc_it.advance() )
            {
                procedure proc = proc_it.current();

                if(proc.get_kind().equals(procedure_kind.getUSER_DEFINED())){
                    dumpCFG2Dot(proc, path);
                }
            }
        }
    }

}
