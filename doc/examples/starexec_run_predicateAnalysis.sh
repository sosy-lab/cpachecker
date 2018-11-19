#!/bin/bash
#Starexec solver configuration filenames need to start with "starexec_run_"
#Call cpa.sh script to run CPAchecker and add configurations according to
#your requirements. Only make a single cpa.sh call per file. Use "$1" 
#instead of a specific filename as those will be passed by the starexec job.
./cpa.sh -predicateAnalysis "$1"
