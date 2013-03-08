// The Edges class represents the overall structure of the edges in the graph, and presents
// the interface used by the main class to access the flow and capacity of edges
// Most methods take two Vertex objects, and locate the appropriate Edge between them

#ifndef EDGES_H
#define EDGES_H

#include <iosfwd>
#include <tr1/unordered_map>
using std::tr1::unordered_map;

#include "Vertex.h"
#include "Edge.h"

namespace Bilateral {
	typedef std::pair<int, int> EdgeInfoKey;

	struct VertexPairHash {
		size_t operator()(const EdgeInfoKey& vv) const;
	};

	typedef unordered_map<EdgeInfoKey, Edge, VertexPairHash> EdgeInfo_t;

	// Class to present interface to all edge-related information
	class Edges {
	private:
		// flow and capacity values for nonexistent edges
		const static int EMPTY_FLOW = 0;
		const static int EMPTY_CAPACITY = 0;

		EdgeInfo_t edgeInfo;

		static const EdgeInfoKey keyVal(const SpVertex& u, const SpVertex& v);

	public:
		Edges(int numVertices);

		int getResidualCapacity(const SpVertex& u, const SpVertex& v) const;

		int getFlow(const SpVertex& u, const SpVertex& v) const;
		void setFlow(const SpVertex& u, const SpVertex& v, int newFlow);
		void addToFlow(const SpVertex& u, const SpVertex& v, int flowInc);
		void flipFlow(const SpVertex& u, const SpVertex& v);

		int getCapacity(const SpVertex& u, const SpVertex& v) const;

		void printMatching() const;
		friend std::ostream& operator<<(std::ostream& out, const Edges& edges);
	};

}

#endif