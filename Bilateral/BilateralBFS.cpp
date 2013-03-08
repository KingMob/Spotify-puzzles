#include <iostream>

#include <list>
using std::list;

#include <algorithm>

#include "BilateralBFS.h"
#include "ProjectParams.h"
#include "Vertex.h"
#include "Edge.h"
#include "Edges.h"

namespace Bilateral {

	BilateralBFS::BilateralBFS(const int2dVector& ids, const intPairVector& teams) :
		edges(MAX_NUM_VERTICES),
		source(Vertex::vertexFromId(SOURCE_ID)),
		sink(Vertex::vertexFromId(SINK_ID)),
		friendv(Vertex::vertexFromId(ProjectParams::FRIEND_ID)){

		visited.assign(false); // would be .fill() for C++11
		prev.assign(-1); // would be .fill() for C++11

		initialize(ids, teams);
	}

	void BilateralBFS::solve() {
		while(findAugmentingPath()) {
			processPath();
		}

		int numEdgesInMatching = computeMatchset();
		computeInvitees();

		if(invitees.size() != numEdgesInMatching){
			std::cerr << "Num edges in matching: " << numEdgesInMatching << " does not equal num invitees: " << invitees.size() << "!!!" << std::endl;
		}
	}


	void BilateralBFS::initialize(const int2dVector& ids, const intPairVector& teams){
		const vector<int>& Lset = ids[0];
		const vector<int>& Rset = ids[1];

		setupSourceSide(Lset);
		setupSinkSide(Rset);
		setupNeighbors(teams);

		setupInitialMatching();
		// edges.printMatching();

		prev.assign(-1); // would be .fill() for C++11
	}

	// Form edges from source to all left vertices
	void BilateralBFS::setupSourceSide(const vector<int>& ids){
		for(vector<int>::const_iterator it = ids.begin(); it != ids.end(); ++it){
			int id = *it;

			SpVertex u(Vertex::vertexFromId(id));
			LV.push_back(u);

			source->addToNeighbors(u);
			u->addToNeighbors(source);

			edges.setFlow(source, u, 0);
		}
	}

	// Form edges from all right vertices to sink
	void BilateralBFS::setupSinkSide(const vector<int>& ids){
		for(vector<int>::const_iterator it = ids.begin(); it != ids.end(); ++it){
			int id = *it;

			SpVertex u(Vertex::vertexFromId(id));
			RV.push_back(u);

			sink->addToNeighbors(u);
			u->addToNeighbors(sink);

			edges.setFlow(u, sink, 0);
		}
	}

	// set up neighborhoods for each non-sink/source vertex
	void BilateralBFS::setupNeighbors(const intPairVector& teams){
		for(intPairVector::const_iterator it = teams.begin(); it != teams.end(); ++it){
			SpVertex u(Vertex::vertexFromId(it->first));
			SpVertex v(Vertex::vertexFromId(it->second));

			u->addToNeighbors(v);
			v->addToNeighbors(u);

			edges.setFlow(u, v, 0);
		}
	}

	// Start off with a simple greedy matching
	void BilateralBFS::setupInitialMatching(){
		for(vector<SpVertex>::const_iterator it = LV.begin(); it != LV.end(); ++it){
			const SpVertex& u = *it;

			const vector<WpVertex>& neighbors = u->getNeighbors();
			for(vector<WpVertex>::const_iterator neighbIt = neighbors.begin(); neighbIt != neighbors.end(); ++neighbIt){
				const SpVertex& v = neighbIt->lock();

				if(v != source && !visited[v->getIdx()] && edges.getFlow(u, v) == 0){
					edges.setFlow(source, u, 1);
					edges.setFlow(u, v, 1);
					edges.setFlow(v, sink, 1);
					visited[u->getIdx()] = true;
					visited[v->getIdx()] = true;
					break;
				}
			}
		}

		visited.assign(false); // would be .fill() for C++11
	}

	// Use a BFS search for an augmenting path
	// Theoretically suboptimal, but pragmatically fastest for problems with only a few thousand vertices,
	// according to Setubal 1996
	bool BilateralBFS::findAugmentingPath(){
		list<SpVertex> queue;

		queue.push_back(source);
		visited[source->getIdx()] = true;
		
		while(!queue.empty()){
			const SpVertex& u = queue.front();
			// std::cout << "Considering vertex: " << u->getId() << std::endl;

			const vector<WpVertex>& neighbors = u->getNeighbors();
			for(vector<WpVertex>::const_iterator neighbIt = neighbors.begin(); neighbIt != neighbors.end(); ++neighbIt){
				const SpVertex& v = neighbIt->lock();
				// std::cout << "\tConsidering neighb: " << v->getId() << std::endl;

				if(!visited[v->getIdx()] && edges.getResidualCapacity(u, v) > 0){
					// std::cout << "\tAdding " << v->getId() << " to path/queue" << std::endl;
					prev[v->getIdx()] = u->getIdx();
					queue.push_back(v);

					visited[v->getIdx()] = true;
					if(v == sink){
						return true;
					}
				}
			}

			queue.pop_front();
		}

		return false;
	}

	// Follow path backwards from sink to source, flipping flow along the way
	void BilateralBFS::processPath() {
		// std::cout << "Processing path" << std::endl;
		int v = sink->getIdx();

		while (v != source->getIdx()) {
			int u = prev[v];

			// std::cout << "\tFlipping flow from " << (u + ProjectParams::ID_IDX_DIFFERENCE) << " to " << (v + ProjectParams::ID_IDX_DIFFERENCE) << std::endl;
			
			if(u < v || isSpecialVertexId(u + ProjectParams::ID_IDX_DIFFERENCE)){
				edges.flipFlow(Vertex::vertexFromId(u + ProjectParams::ID_IDX_DIFFERENCE), Vertex::vertexFromId(v + ProjectParams::ID_IDX_DIFFERENCE));
			}
			else {
				edges.flipFlow(Vertex::vertexFromId(v + ProjectParams::ID_IDX_DIFFERENCE), Vertex::vertexFromId(u + ProjectParams::ID_IDX_DIFFERENCE));	
			}
			v = u; // follow reverse path to source 
		}

		 // reset for next iteration 
		visited.assign(false); // would be .fill() for C++11
		prev.assign(-1); // would be .fill() for C++11
	}



	// Konig's theorem methods

	// The algorithm can start with either side of the bipartite graph
	// Compute in both directions as the friend, may or may not appear in a
	// particular vertex cover, depending on graph structure
	void BilateralBFS::computeInvitees(){
		const std::set<SpVertex>* pResult;

		sort(LV.begin(), LV.end());
		sort(RV.begin(), RV.end());

		const std::set<SpVertex> minVertexCovers1(computeMinVertexCover(LV, RV));
		const std::set<SpVertex> minVertexCovers2(computeMinVertexCover(RV, LV));

		if(minVertexCovers1.find(friendv) != minVertexCovers1.end()){
			pResult = &minVertexCovers1;
		}
		else {
			pResult = &minVertexCovers2;
		}


		for(std::set<SpVertex>::const_iterator it = pResult->begin(); it != pResult->end(); ++it){
			const SpVertex& v = *it;
			invitees.insert(v->getId());
		}
	}

	// Uses Konig's theorem to compute min vertex cover starting on one side, given a maximum 
	// matching. Starts with every unmatched vertex, then computes a path from side
	// to side, alternating with edges that are either in, or not in, the matching.
	// Every reachable vertex is added to a unordered_set, T.
	// result is (side1 not in T) union (side2 intersection T)
	// returns by value for move constructor
	const std::set<SpVertex> BilateralBFS::computeMinVertexCover(const vector<SpVertex>& side1, const vector<SpVertex>& side2) const {
		std::set<SpVertex> konigSet;
		std::set<SpVertex> unmatched;
		set_difference(side1.begin(), side1.end(), matches.begin(), matches.end(), std::inserter(unmatched, unmatched.begin()));
		// std::cout << "UNMATCHED\n" << unmatched << std::endl;
		
		for(std::set<SpVertex>::const_iterator it = unmatched.begin(); it != unmatched.end(); ++it){
			konigDFS(konigSet, *it, false);
		}
		// std::cout << "KONIG SET\n" << konigSet << std::endl;

		std::set<SpVertex> result;
		set_intersection(side2.begin(), side2.end(), konigSet.begin(), konigSet.end(), std::inserter(result, result.begin()));
		set_difference(side1.begin(), side1.end(), konigSet.begin(), konigSet.end(), std::inserter(result, result.begin()));
		// std::cout << "RESULT\n" << result << std::endl;

		return result;	// returns by value for move constructor
	}

	// Generates depth-first paths based on Konig's algorithms. Edges used must alternate being in 
	// or out of the matching
	void BilateralBFS::konigDFS(std::set<SpVertex>& konigSet, const SpVertex& v, bool edgesInMatch) const {
		if(konigSet.find(v) == konigSet.end()){
			konigSet.insert(v);
			
			const vector<WpVertex>& neighbors = v->getNeighbors();
			for(vector<WpVertex>::const_iterator neighbIt = neighbors.begin(); neighbIt != neighbors.end(); ++neighbIt){
				const SpVertex& neighb = neighbIt->lock();

				if(neighb->getId() != SOURCE_ID && neighb->getId() != SINK_ID){
					if(edgesInMatch == (edges.getFlow(v, neighb) > 0 || edges.getFlow(neighb, v) > 0)) {
						konigDFS(konigSet, neighb, !edgesInMatch);
					}
				}
			}
		}
	}

	// Parses the flow after finishing to determine which vertices are in the matching
	int BilateralBFS::computeMatchset(){
		int numEdgesInMatching = 0;

		// std::cout << "MATCHES\n" << std::endl;
		for(vector<SpVertex>::const_iterator it = LV.begin(); it != LV.end(); ++it){
			const SpVertex& v = *it;

			const vector<WpVertex>& neighbors = v->getNeighbors();
			for(vector<WpVertex>::const_iterator neighbIt = neighbors.begin(); neighbIt != neighbors.end(); ++neighbIt){
				const SpVertex& neighb = neighbIt->lock();

				if(neighb->getId() != SOURCE_ID && edges.getFlow(v, neighb) > 0){
					// std::cout << "Edge " << v->getId() << " to " << neighb->getId() << ", flow: " << edges.getFlow(v, neighb) << std::endl;

					matches.insert(v);
					matches.insert(neighb);
					numEdgesInMatching++;
				}
			}
		}

		return numEdgesInMatching;
	}

	bool BilateralBFS::isSpecialVertexId(int id) const {
		return id > BilateralBFS::MAX_NORMAL_ID;
	}


	void BilateralBFS::printSolution(){
		std::cout << invitees.size() << std::endl;
		for(unordered_set<int>::const_iterator it = invitees.begin(); it != invitees.end(); ++it){
			std::cout << *it << std::endl;
		}
	}


	std::ostream& operator<<(std::ostream& out, const BilateralBFS& s){
		out << "Source:\t" << *s.source << std::endl;
		out << "Sink:\t" << *s.sink << std::endl;

		out << "L Vertex vector:" << std::endl;
		for(vector<SpVertex>::const_iterator it = s.LV.begin(); it != s.LV.end(); ++it){
			out << **it << std::endl;
		}
		
		out << "R Vertex vector:" << std::endl;
		for(vector<SpVertex>::const_iterator it = s.RV.begin(); it != s.RV.end(); ++it){
			out << **it << std::endl;
		}
		out << std::endl << "EDGES" << std::endl << s.edges << std::endl;

		out << "MATCHES:" << std::endl;
		for(set<SpVertex>::const_iterator it = s.matches.begin(); it != s.matches.end(); ++it){
			out << (*it)->getId() << std::endl;
		}

		return out;
	}

};


int main(int argc, char **argv) {
	using namespace Bilateral; // main itself can't be member of namespace Bilateral

	// return by value, relying on copy elision for efficiency
	ProjectParams pp(ProjectParams::fromStdIn());
	// std::cout << "ProjectParams" << pp << std::endl;

	BilateralBFS solver(pp.getIds(), pp.getTeams());

	// // Solve and print
	solver.solve();
	solver.printSolution();

	
	return 0;
};