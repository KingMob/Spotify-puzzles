#include <iostream>

#include "Edge.h"

namespace Bilateral {
	Edge::Edge() 
		: flow(0) {
	}

	Edge::Edge(int newFlow) 
		: flow(newFlow) {
	}

	int Edge::getFlow() const { return flow;}
	void Edge::setFlow(int newFlow) { flow = newFlow;}
	void Edge::addToFlow(int flowInc){ flow += flowInc;}
	void Edge::flipFlow(){ flow = (flow == 1 ? 0 : 1);	}

	int Edge::getCapacity() const { return capacity;}

	std::ostream& operator<<(std::ostream& out, const Edge& e){
		out << "flow: " << e.flow << std::endl;
		return out;
	}
}