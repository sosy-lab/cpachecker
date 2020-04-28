./scripts/cpa.sh -timelimit 10s -setprop log.level=$1 -preprocess -bmc-interpolation -spec sv-comp-reachability -stats $2
#./scripts/cpa.sh -timelimit 10s -preprocess -bmc-interpolation -setprop cpa.loopbound.maxLoopIterationsUpperBound=3 -spec sv-comp-reachability $1
if [[ $? -eq 0 ]];then
   firefox output/Report.html &
else
   echo "The execution was terminated with error code $? and produced no report."
fi
