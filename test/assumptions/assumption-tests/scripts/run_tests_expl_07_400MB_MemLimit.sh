BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/explicitAnalysis_07_400MB_MemLimit.properties"
OUTPUTM="output/explicit/400MBMemLimit/explicitAnalysis_07_400MB_MemLimit.log"
INPUTS="testsets/explicit.safe_400MB.set"

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_explicit.py --config=$CONFIG --output=$OUTPUTM "$@" $INSTANCES
