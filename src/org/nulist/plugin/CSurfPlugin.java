package org.nulist.plugin;

import com.grammatech.cs.*;
import org.sosy_lab.cpachecker.cmdline.CPAMain;

import java.lang.*;

import static org.nulist.plugin.util.CFGDumping.dumpCFG2Dot;

//Combine CPAChecker as a plugin of CodeSurfer

public class CSurfPlugin {

    /**
     * @Description
     * @Param [args]
     * @return void
     **/
    public static void main(String args[]){

        //Arguments to CPAChecker
        String[] arguments = null;
        String cpacheckPath ="";
        String programPath = "";
        if(args.length>=3){
            cpacheckPath = args[0];
            programPath = args[1];
            arguments = args[2].split(" ");
        }

        //perform parser execution
        try{
            System.out.println("==================CSURF_PLUGIN_BEGIN==================");
            project proj = project.current();
            CPAMain.executionTesting(arguments, cpacheckPath, programPath, proj);
            //CPAMain.executeParser(arguments, cpacheckPath, programPath, proj);

            System.out.println("==================CSURF_PLUGIN_END==================");
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
