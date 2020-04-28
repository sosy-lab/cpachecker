#scripts/cpa.sh -heap 2000M -preprocess -kInduction -timelimit 90s -stats -spec sv-comp-reachability $1
scripts/cpa.sh -noout -heap 10000M -setprop cpa.predicate.memoryAllocationsAlwaysSucceed=true -config config/components/kInduction/kInduction.properties -timelimit 60s -stats -spec sv-comp-reachability $1
