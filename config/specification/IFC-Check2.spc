OBSERVER AUTOMATON NoninterferenceChecker

INITIAL STATE Init;

STATE USEFIRST Init :
	MATCH EXIT && CHECK(DependencyTrackerCPA, "noninterference_All") -> ERROR("Error");
	CHECK(DependencyTrackerCPA, "noninterference_All_Violables") -> ERROR("Error");
END AUTOMATON

