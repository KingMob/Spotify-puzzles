// Edge represents a single edge in the graph. Primarily a holder for the current flow between the two vertices
// and the capacity. Since the capacity is always the same, it's defined to be 1 if the edge exists. Note, the
// Edge class knows nothing about the vertices it's connected to. The Edges class contains all Edge objects and
// handles their lookup, given Vertex objs

#ifndef EDGE_H
#define EDGE_H

#include <iosfwd>

namespace Bilateral {
	class Edge {
	private:
		int flow;
		const static int capacity = 1; // constant here, but the class is designed to make it easy to switch to variable-capacity edges

	public:
		Edge();
		Edge(int newFlow);

		int getFlow() const;
		void setFlow(int newFlow);
		void addToFlow(int flowInc);
		void flipFlow();

		int getCapacity() const;

		friend std::ostream& operator<<(std::ostream& out, const Edge& e);
	};
}

#endif