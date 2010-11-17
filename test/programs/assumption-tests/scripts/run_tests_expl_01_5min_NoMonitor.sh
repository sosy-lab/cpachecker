BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/explicitAnalysis_01_5min_NoMonitor.properties"
OUTPUTM="output/explicit/5minTimeLimit/explicitAnalysis_01_5min_NoMonitor.log"
INPUTS="testsets/explicit.safe_5min.set"
TIMELIMIT=300 #in seconds

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_explicit.py -t $TIMELIMIT --config=$CONFIG --output=$OUTPUTM "$@" $INSTANCES
