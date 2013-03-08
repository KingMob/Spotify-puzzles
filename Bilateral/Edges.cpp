#include <iostream>
#include <cstdlib>

#include <tr1/functional_hash.h>
using std::tr1::hash;

#include "Edges.h"
#include "Vertex.h"
#include "BilateralBFS.h"

namespace Bilateral {

	Edges::Edges(int numVertices) 
		: edgeInfo(2*numVertices){
	}

	const EdgeInfoKey Edges::keyVal(const SpVertex& u, const SpVertex& v) {
		return std::make_pair(u->getId(), v->getId());
	}

	int Edges::getResidualCapacity(const SpVertex& u, const SpVertex& v) const{
		int forwardCapacity = getCapacity(u, v);

		// u->v
		if(forwardCapacity > 0){
			return forwardCapacity - getFlow(u, v);
		}
		// v->u
		else if(getCapacity(v, u) > 0){
			return getFlow(v, u);
		}

		return 0;
	}

	int Edges::getFlow(const SpVertex& u, const SpVertex& v) const{
		EdgeInfo_t::const_iterator it = edgeInfo.find(Edges::keyVal(u, v));
		if(it == edgeInfo.end()){
			return EMPTY_FLOW;
			// std::cerr << "Unknown edge in getFlow() from " << u->getId() << " to " << v->getId() << std::endl;
		}
		else {
			it->second.getFlow();
		}
	}

	void Edges::setFlow(const SpVertex& u, const SpVertex& v, int newFlow){
		edgeInfo[Edges::keyVal(u, v)].setFlow(newFlow);
	}

	void Edges::addToFlow(const SpVertex& u, const SpVertex& v, int flowInc){
		edgeInfo[Edges::keyVal(u, v)].addToFlow(flowInc);
	}

	void Edges::flipFlow(const SpVertex& u, const SpVertex& v){
		// edgeInfo[Edges::keyVal(u, v)].flipFlow();

		EdgeInfo_t::iterator it = edgeInfo.find(Edges::keyVal(u, v));
		if(it == edgeInfo.end()){
			// return EMPTY_FLOW;
			std::cerr << "Unknown edge in flipFlow() from " << *u << " to " << *v << std::endl;
		}
		else {
			it->second.flipFlow();
		}
	}


	int Edges::getCapacity(const SpVertex& u, const SpVertex& v) const{
		EdgeInfo_t::const_iterator it = edgeInfo.find(Edges::keyVal(u, v));
		if(it == edgeInfo.end()){
			return EMPTY_CAPACITY;
		}
		else {
			it->second.getCapacity();
		}
	}



	std::ostream& operator<<(std::ostream& out, const Edges& edges){
		out << "Flow:" << std::endl;

		for(EdgeInfo_t::const_iterator it = edges.edgeInfo.begin(); it != edges.edgeInfo.end(); ++it){
			const std::pair<int, int>& ids = it->first;
			const Edge& e = it->second;

			out << ids.first << " -> " << ids.second << "\tflow: " << e.getFlow() << std::endl;
		}
		return out;
	}

	void Edges::printMatching() const {
		std::cout << "Matching:" << std::endl;

		for(EdgeInfo_t::const_iterator it = edgeInfo.begin(); it != edgeInfo.end(); ++it){
			const std::pair<int, int>& ids = it->first;
			const Edge& e = it->second;

			if(e.getFlow() > 0 && ids.first != BilateralBFS::SOURCE_ID && ids.second != BilateralBFS::SINK_ID){
				std::cout << ids.first << " -> " << ids.second << "\tflow: " << e.getFlow() << std::endl;
			}
		}
	}

	size_t VertexPairHash::operator()(const EdgeInfoKey& vv) const {
		return hash<int>()(vv.first) ^ hash<int>()(vv.second);
	}

}