// ProjectParams is an interface class for representing most of the problem invariants, 
// as well as parsing the input

#ifndef PROJECTPARAMS_H
#define PROJECTPARAMS_H

#include <vector>
using std::vector;

#include <iosfwd>


namespace Bilateral {

	typedef vector<vector<int> > int2dVector;
	typedef vector<std::pair<int, int> > intPairVector;

	class ProjectParams {
	private:
		const int2dVector ids;
		const intPairVector teams;

		ProjectParams(int2dVector newIds, intPairVector newTeams);


	public:
		const static int MAX_NUM_IDS = 2000;
		const static int MAX_NY_ID = 1999;
		const static int MAX_NUM_TEAMS = 10000;
		const static int FRIEND_ID = 1009;
		const static int NUM_LOCATIONS = 2;
		const static int ID_IDX_DIFFERENCE = 1000; // difference between min ID and and min index

		// Constructors and factory methods	
		static ProjectParams fromStdIn();

		// Accessors
		const intPairVector& getTeams() const;

		const int2dVector& getIds() const;

		int getNumIds() const;

		friend std::ostream& operator<<(std::ostream& out, const ProjectParams& pp);
	};
}

#endif