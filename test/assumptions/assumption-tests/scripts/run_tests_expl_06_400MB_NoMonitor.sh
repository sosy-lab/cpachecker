BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/explicitAnalysis_06_400MB_NoMonitor.properties"
OUTPUTM="output/explicit/400MBMemLimit/explicitAnalysis_06_400MB_NoMonitor.log"
INPUTS="testsets/explicit.safe_400MB.set"
MEMLIMIT=570000 #in kb

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_explicit400MB.py -m $MEMLIMIT --config=$CONFIG --output=$OUTPUTM "$@" $INSTANCES
