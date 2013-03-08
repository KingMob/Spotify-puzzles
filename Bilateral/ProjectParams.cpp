#include <iostream>

#include "ProjectParams.h"

namespace Bilateral {

	// Constructors and factory methods
	ProjectParams::ProjectParams(int2dVector newIds, intPairVector newTeams) :
		ids(newIds), 
		teams(newTeams)  {}

	ProjectParams ProjectParams::fromStdIn() {
		int numTeams;
		std::cin >> numTeams;

		intPairVector teams = intPairVector(numTeams);

		bool idPresent[MAX_NUM_IDS] = {false};

		for(int i = 0; i < numTeams; i++){
			int id1;
			int id2;

			std::cin >> id1 >> id2;

			idPresent[id1 - ProjectParams::ID_IDX_DIFFERENCE] = true;
			idPresent[id2 - ProjectParams::ID_IDX_DIFFERENCE] = true;

			teams[i].first = id1;
			teams[i].second = id2;
		}

		int2dVector uniqueIds = int2dVector(2, std::vector<int>(0, 0));
		for(int i = 0; i < MAX_NUM_IDS; i++){
			if(idPresent[i]) { 
				if(i <= MAX_NY_ID - ProjectParams::ID_IDX_DIFFERENCE){
					uniqueIds[0].push_back(i + ProjectParams::ID_IDX_DIFFERENCE);
				}
				else{
					uniqueIds[1].push_back(i + ProjectParams::ID_IDX_DIFFERENCE);
				}
			}
		}

		// return by value, relying on copy elision for efficiency
		return ProjectParams(uniqueIds, teams);
	}


	// Accessors
	const intPairVector& ProjectParams::getTeams() const {
		return teams;
	}

	const int2dVector& ProjectParams::getIds() const {
		return ids;
	}

	int ProjectParams::getNumIds() const {
		return ids[0].size() + ids[1].size();
	}


	std::ostream& operator<<(std::ostream& out, const ProjectParams& pp){
		const intPairVector& teams = pp.getTeams();
		out << "TEAMS" << std::endl;
		for(intPairVector::const_iterator it = teams.begin(); it != teams.end(); ++it){
			out << it->first << " - " << it->second << std::endl;
		}

		out << "IDS" << std::endl;
		const int2dVector ids = pp.getIds();
		for(int i = 0; i < ProjectParams::NUM_LOCATIONS; i++){
			for(vector<int>::const_iterator it = ids[i].begin(); it != ids[i].end(); ++it){
				out << *it << std::endl;
			}
		}

		return out;
	}
}