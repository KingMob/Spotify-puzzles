// Vertex represents a single node/person in the problem, once transformed into graph form.
// All vertices are placed into a static pool of shared_ptrs to ensure that when reading the teams
// to build the neighbors, duplicate Vertex objs aren't created. Weak ptrs are used to build the list
// of connected vertices, to avoid the problem of circular references.

#ifndef VERTEX_H
#define VERTEX_H

#include <iosfwd>

#include <vector>
using std::vector;

#include <set>
using std::set;

#include <tr1/unordered_map>
using std::tr1::unordered_map;

#include <tr1/memory>
using std::tr1::shared_ptr;
using std::tr1::weak_ptr;

namespace Bilateral {

	class Vertex {
	private:
		static unordered_map<int, shared_ptr<Vertex> > vertices;

		const int id;
		vector<weak_ptr<Vertex> > neighbors; // weak to avoid circular reference problem

		Vertex(int newId);


	public:
		static shared_ptr<Vertex> vertexFromId(int id);

		int getIdx() const;
		int getId() const;

		const vector<weak_ptr<Vertex> >& getNeighbors() const;
		int getNumNeighbors() const;
		void addToNeighbors(shared_ptr<Vertex> v);


		friend std::ostream& operator<< (std::ostream& out, const Vertex& v);
	};



	typedef shared_ptr<Vertex> SpVertex;
	typedef weak_ptr<Vertex> WpVertex;

	bool operator<(const Vertex& lhs, const Vertex& rhs);
	bool operator<(const SpVertex& lhs, const SpVertex& rhs);

	std::ostream& operator<<(std::ostream& out, const vector<SpVertex>& vertices);
	std::ostream& operator<<(std::ostream& out, const set<SpVertex>& vertices);

};

// Implementation because set wasn't defaulting to operator<
// GCC bug/detail?
namespace std {
	template <>
	struct less<Bilateral::SpVertex> {
		bool operator()(const Bilateral::SpVertex& lhs, const Bilateral::SpVertex& rhs) const;
	};
};


#endif