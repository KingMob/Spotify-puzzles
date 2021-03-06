#include <iostream>

#include "Vertex.h"
#include "ProjectParams.h"
#include "BilateralBFS.h"

namespace Bilateral {
	Vertex::Vertex(int newId) :
		id(newId),
		neighbors(0) {
	}

	// Factory construction method to ensure that when building teams/neighbors of vertices hat the same neighbor is used
	SpVertex Vertex::vertexFromId(int id){
		int idx = id - ProjectParams::ID_IDX_DIFFERENCE;

		if(vertices.find(idx) == vertices.end()){
			SpVertex newVert(new Vertex(id)); // can't use make_shared because Vertex ctor is private
			vertices.insert(std::make_pair(idx, newVert));
		}
		return vertices[idx];
	}


	int Vertex::getIdx() const {
		return id - ProjectParams::ID_IDX_DIFFERENCE;
	}

	int Vertex::getId() const { 
		return id;
	}

	const std::vector<WpVertex>& Vertex::getNeighbors() const { 
		return neighbors;
	}

	int Vertex::getNumNeighbors() const { 
		return neighbors.size();
	}

	void Vertex::addToNeighbors(SpVertex v) { 
		neighbors.push_back(v);
	}


	// Initialize static vertex pool
	unordered_map<int, SpVertex> Vertex::vertices(2 * BilateralBFS::MAX_NUM_VERTICES); // twice the max num vertices should be sufficient to avoid collisions


	// Comparison operator to allow Vertex/SpVertex to be used in sorted maps/sets
	bool operator<(const Vertex& lhs, const Vertex& rhs){
		return lhs.getId() < rhs.getId();
	}

	bool operator<(const SpVertex& lhs, const SpVertex& rhs) {
		return *lhs < *rhs;
	}

	// Overloaded IOStream operators
	std::ostream& operator<<(std::ostream& out, const Vertex& v){
		out << "id: " << v.id << " - # of neighbors: " << v.neighbors.size();
		return out;
	}

	std::ostream& operator<<(std::ostream& out, const std::set<SpVertex>& vertices){
		for(std::set<SpVertex>::const_iterator it = vertices.begin(); it != vertices.end(); ++it){
			out << **it << std::endl;
		}
		return out;
	}

	std::ostream& operator<<(std::ostream& out, const std::vector<SpVertex>& vertices){
		for(std::vector<SpVertex>::const_iterator it = vertices.begin(); it != vertices.end(); ++it){
			out << **it << std::endl;
		}
		return out;
	}
};

namespace std {
	bool less<Bilateral::SpVertex>::operator()(const Bilateral::SpVertex& lhs, const Bilateral::SpVertex& rhs) const {
		return lhs < rhs;
	}
};