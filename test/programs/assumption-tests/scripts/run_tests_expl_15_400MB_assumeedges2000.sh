BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/explicitAnalysis_15_400MB_assumeedges2000.properties"
OUTPUTM="output/explicit/400MBMemLimit/explicitAnalysis_15_400MB_assumeedges2000.log"
INPUTS="testsets/explicit_BUG.set"

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_explicit.py --config=$CONFIG --output=$OUTPUTM "$@" $INSTANCES
