OBSERVER AUTOMATON qMultiIntervalChecker
INITIAL STATE Init;

STATE USEFIRST Init :
		MATCH EXIT && CHECK(qMultiInterval, "MinEntropyCheck") && CHECK(DependencyTrackerCPA, "noninterference_All") -> ERROR("Error");
	

CHECK(DependencyTrackerCPA, "noninterference_All_Violables") -> ERROR("Error");
END AUTOMATON

