// BilateralBFS is the primary class. It's initialized with a list of the ids and teams.
// 
// The problem, as specified, is a minimum vertex cover problem for a bipartite graph.
// According to Konig's theorem, this can be reformulated as a minimum matching problem.
// The particular solution starts with an initial greedy matching between the left and right 
// sides. Then, it finds augmenting paths via a simple breadth-first search, and flips the flow 
// of each edge in the path. After a maximal matching has been found, it uses Konig's algorithm
// to determine the minimum vertex cover from the matching. Since the minimum vertex cover can be 
// done starting from either side, it computes both, and, if the friend ID of 1009 is in one of 
// the sets, it chooses that one.
//
// Theoretically, the Hopcroft-Karp method is faster for bipartite graphs, but has been 
// found to be slower in practice (Cherkassky 1998, Setubal 1996), using appropriate heuristics. 
// While push-relabel variants are generally considered among the fastest practical algorithms
// for maximum matching/flow problems, Setubal's work shows that simple BFS is just as fast for
// problems up to a few thousand vertices, while being simpler to implement.


#ifndef BILATERALBFS_H
#define BILATERALBFS_H

#include <iosfwd>

#include <set>
using std::set;

#include <tr1/unordered_set>
using std::tr1::unordered_set;

#include <vector>
using std::vector;

#include <tr1/memory>
using std::tr1::shared_ptr;
using std::tr1::weak_ptr;

#include <tr1/array>
using std::tr1::array;

#include "ProjectParams.h"
#include "Vertex.h"
#include "Edges.h"


namespace Bilateral {
	class BilateralBFS {
	public:
		const static int SOURCE_ID = 3000;
		const static int SINK_ID = 3001;
		const static int MAX_NORMAL_ID = 2999;
		const static int MAX_NUM_VERTICES = ProjectParams::MAX_NUM_IDS + 2;

	private:
		vector<SpVertex> LV; // left side
		vector<SpVertex> RV; // right side
		Edges edges;

		SpVertex source;
		SpVertex sink;
		SpVertex friendv;

	 	array<bool, MAX_NUM_VERTICES> visited;
	 	array<int, MAX_NUM_VERTICES> prev;

		set<SpVertex> matches; // sorted, to make it easier to use set intersection/difference
		unordered_set<int> invitees;

		// Initialization methods
		void initialize(const int2dVector& ids, const intPairVector& teams);
		void setupSourceSide(const vector<int>& ids);
		void setupSinkSide(const vector<int>& ids);
		void setupNeighbors(const intPairVector& teams);
		void setupInitialMatching();

		// Graph-related methods
		bool findAugmentingPath();
		void processPath();


		// Konig's theorem methods
		const set<SpVertex> computeMinVertexCover(const vector<SpVertex>& side1, const vector<SpVertex>& side2) const;
		void konigDFS(set<SpVertex>& konigSet, const SpVertex& v, bool edgesInMatch) const;

		int computeMatchset();
		void computeInvitees();

		bool isSpecialVertexId(int id) const;

	public:
		BilateralBFS(const int2dVector& ids, const intPairVector& teams);

		void solve();
		void printSolution();

		friend std::ostream& operator<<(std::ostream& out, const BilateralBFS& s);
	};
}

#endif