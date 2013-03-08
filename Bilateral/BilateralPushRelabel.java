// ** Algorithm **
// The problem as described is a minimum vertex cover graph problem, i.e., how few vertices (people)
// are needed to cover all teams (edges). Given that all employees are divided into two groups, this is 
// a bipartite graph. And by Konig's theorem, the minimum vertex cover in a bipartite graph is equivalent
// to the maximum matching problem for that graph. By adding a source and a sink, and treating the edges
// as if they had a capacity of 1, the maximum matching can be treated as a maximum flow problem.
//
// The implementation is the relabel-to-front variant of the push-relabel algorithm as found in Cormen's Algorithms, 
// which runs in O(V^3) time. Theoretically, the Hopcroft-Karp method is faster for bipartite graphs, but has been 
// found to be slower in practice (Cherkassky 1998, Setubal 1996), using appropriate heuristics. 
//
// The only difference is a bias introduced in favor of the friend. The original algorithm specifies no order to 
// consider the neighbors in when discharging. My variant considers flow direction when setting up the neighbor list.
// When the flow is *toward* the friend, the friend is placed first in consideration for receiving flow; when the
// flow is in reverse (i.e., the friend would potentially lose flow), the friend is placed last. Since the push and 
// relabel operations are unchanged, the algorithm will still converge on a maximum flow, but one biased towards 
// including an edge with the friend, if possible. 
//
// Likewise, when computing the minimum vertex cover from the maximum matching, Konig's algorithm can be applied 
// in either direction. Knowing the direction of the algorithm in theory allows you to bias towards the friend, but
// is not guaranteed, since it depends on the particular structure of the graph. Instead the algorithm is computed 
// in both directions, and the one with the friend is chosen (if any).
//
// Further improvement: to actually take advantage of the practical speed improvement, add gap heuristic and global 
// relabeling code, and switch to a faster (but more complicated ordering), like highest-labeling or excess scaling.

// package Spotify.Bilateral;

import java.io.*;
import java.util.*;

public class BilateralPushRelabel {
	public static void main(String[] args) {
		long startTime = System.nanoTime();
		try {
			//Thread.sleep(15000);
			// Parse input and set up
			ProjectParams pp = ProjectParams.fromStdIn();

			Solver solver = new Solver(pp);

			// Solve and print
			solver.solve();
			solver.printSolution();
			// while(true){
			// 	Thread.sleep(200);
			// }
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		long elapsedTime = System.nanoTime() - startTime;
		System.out.println("Elapsed time (ms): " + elapsedTime/1000000.0);
	}
}

class Solver {
	private static final int SOURCE_ID = 3000;
	private static final int SINK_ID = 3001;
	private static final int MAX_NUM_VERTICES = ProjectParams.MAX_NUM_IDS + 2;

	private ProjectParams pp;

	private LinkedList<Vertex> V = new LinkedList<Vertex>();
	private Set<Vertex> LV = new HashSet<Vertex>();
	private Set<Vertex> RV = new HashSet<Vertex>();
	private Edges edges;

	private final Vertex source = Vertex.vertexFromId(SOURCE_ID);
	private final Vertex sink = Vertex.vertexFromId(SINK_ID);
	private final Vertex friend = Vertex.vertexFromId(ProjectParams.FRIEND_ID);

	private Set<Vertex> matches = new HashSet<Vertex>();
	private Set<Integer> invitees = new HashSet<Integer>();

	public Solver(ProjectParams pp){
		this.pp = pp;

		initialize();
	}

	public void solve() {
		relabelToFront();

		computeMatchSet();
		computeInvitees();
	}


	private void initialize(){
		List<Integer> Lset = pp.getIds(0);
		List<Integer> Rset = pp.getIds(1);

		// edges = new Edges(Lset.size() + Rset.size());
		edges = new Edges(MAX_NUM_VERTICES);

		setupSourceSide(Lset);
		setupSinkSide(Rset);

		setupNeighbors();
		biasForFriend();

		source.setHeight(V.size() + 2);
	}

	// Form edges from source to all left vertices, with flow from source
	// Prioritize neighbor order for friend and set height = BFS distance
	// from sink (i.e., 2)
	private void setupSourceSide(List<Integer> ids){
		for(Integer id : ids){
			Vertex u = Vertex.vertexFromId(id);
			LV.add(u);

			source.addToNeighbors(u);
			u.addToNeighbors(source);

			edges.setCapacity(source, u, 1);
			edges.setFlow(source, u, 1);
			u.setExcess(1);
			u.setHeight(2);

			// Prioritize friend for initial flow
			if(id.intValue() == ProjectParams.FRIEND_ID){
				V.addFirst(u);
			}
			else{
				V.add(u);
			}
		}

		source.setExcess(source.getExcess() - source.getNumNeighbors());
	}

	// Form edges from sink to all right vertices, and set height = BFS distance
	// from sink (i.e., 1)
	private void setupSinkSide(List<Integer> ids){
		for(Integer id : ids){
			Vertex u = Vertex.vertexFromId(id);
			RV.add(u);

			sink.addToNeighbors(u);
			u.addToNeighbors(sink);
			u.setHeight(1);

			edges.setCapacity(u, sink, 1);
			edges.setFlow(u, sink, 0);
			V.add(u);
		}
	}

	// Set up neighborhoods for each non-sink/source vertex
	private void setupNeighbors(){
		List<Team> teams = pp.getTeams();
		for(Team t : teams){
			Vertex u = Vertex.vertexFromId(t.getId(0));
			Vertex v = Vertex.vertexFromId(t.getId(1));

			u.addToNeighbors(v);
			v.addToNeighbors(u);

			edges.setCapacity(u, v, 1);
			edges.setFlow(u, v, 0);
		}
	}

	private void biasForFriend(){
		Vertex f = Vertex.vertexFromId(ProjectParams.FRIEND_ID);

		List<Vertex> neighbors = f.getNeighbors();
		for(Vertex v : neighbors){
			if(edges.flowsForward(v, f)){
				v.moveToFrontOfNeighbors(f);
			}
			else{
				v.moveToBackOfNeighbors(f);
			}
		}
	}



	// Push-relabel methods

	// Maintains a topological sorting relative to the which vertices can still have edges
	// that flow could be pushed along. All vertices prior to the current vertex in the list
	// have no remaining admissible edges. When the end of the list is reached, the flow is 
	// at its maximum.
	private void relabelToFront(){
		assert(!V.isEmpty()) : "No vertices";

		ListIterator<Vertex> iter = V.listIterator();
		while(iter.hasNext()){
			Vertex v =  iter.next();

			int oldHeight = v.getHeight();
			discharge(v);
			if(v.getHeight() > oldHeight){
				iter.remove();
				V.addFirst(v);
				iter = V.listIterator(1);
			}
		}
	}

	// Push pushes excess flow forwards, or decreases incoming flow
	private void push(Vertex u, Vertex v){
		//System.out.println("Pushing from " + u + " to " + v);
		int excess = u.getExcess(); 
		int residualCapacity = edges.getResidualCapacity(u, v);

		assert(excess > 0) : "Excess <= 0";
		assert(residualCapacity > 0) : "Resid capacity <= 0";
		assert(u.getHeight() == v.getHeight() + 1) : "Height of u != height of v + 1";

		int changeInFlow = excess < residualCapacity ? excess : residualCapacity;

		if(edges.flowsForward(u, v)){
			edges.addToFlow(u, v, changeInFlow);
		}
		else{
			edges.addToFlow(v, u, -changeInFlow);
		}

		u.addToExcess(-changeInFlow);
		v.addToExcess(changeInFlow);
	}

	// Relabel increases vertex's height until it can push its excess to a neighboring
	// vertex
	private void relabel(Vertex u){
		//System.out.println("Relabeling " + u);
		assert(u.getExcess() > 0) : "u not overflowing";

		List<Vertex> neighbors = u.getNeighbors();
		int minHeight = Integer.MAX_VALUE;
		for(Vertex v : neighbors){
			int residCapacity = edges.getResidualCapacity(u, v);
			assert(residCapacity == 0 || u.getHeight() <= v.getHeight());

			if(residCapacity > 0){
				int partnerHeight = v.getHeight();
				minHeight = partnerHeight < minHeight ? partnerHeight : minHeight;
			}
		}
		u.setHeight(1 + minHeight);
	}

	// Scans through a vertex's neighbors until it finds one it can push excess to.
	// If no candidates are found, it increases height until it exceeds at least one 
	// neighbor and then tries again.
	private void discharge(Vertex u){
		//System.out.println("Discharging " + u);
		while(u.getExcess() > 0){
			if(u.getCurrNeighbor() >= u.getNumNeighbors()){
				relabel(u);
				u.setCurrNeighbor(0);
			}
			else {
				Vertex v = u.getNeighbor(u.getCurrNeighbor());

				if(u.getHeight() == v.getHeight() + 1 && edges.getResidualCapacity(u, v) > 0){
					push(u, v);
				}
				else{
					u.incNextNeighbor();
				}
			}
		}
	}


	// Konig's theorem methods

	// The algorithm can start with either side of the bipartite graph
	// Compute in both directions as the friend, may or may not appear in a
	// particular vertex cover, depending on graph structure
	private void computeInvitees(){
		Set<Vertex> result;
		Set<Vertex> minVertexCovers1 = computeMinVertexCover(LV, RV);
		Set<Vertex> minVertexCovers2 = computeMinVertexCover(RV, LV);

		if(minVertexCovers1.contains(friend)){
			result = minVertexCovers1;
		}
		else {
			result = minVertexCovers2;
		}

		for(Vertex v : result){
			invitees.add(v.getId());
		}
	}

	// Uses Konig's theorem to compute min vertex cover for one side, given a maximum 
	// matching. Starts with every unmatched vertex, then computes a path from side
	// to side, alternating with edges that are either in, or not in, the matching.
	// Every reachable vertex is added to a set, T.
	// Final vertex set is (side1 not in T) union (side2 intersection T)
	private Set<Vertex> computeMinVertexCover(Set<Vertex> side1, Set<Vertex> side2){
		Set<Vertex> konigSet = new HashSet<Vertex>();
		Set<Vertex> unmatched = new HashSet<Vertex>(side1);
		unmatched.removeAll(matches);

		for(Vertex v : unmatched){
			konigDFS(konigSet, v, false);
		}
		
		Set<Vertex> result = new HashSet<Vertex>(side2);
		result.retainAll(konigSet);

		Set<Vertex> side1notInKonigSet = new HashSet<Vertex>(side1);
		side1notInKonigSet.removeAll(konigSet);

		result.addAll(side1notInKonigSet);

		return result;
	}

	// Generates depth-first paths based on Konig's algorithms. Edges used must alternate being in 
	// or out of the matching
	private void konigDFS(Set<Vertex> konigSet, Vertex v, boolean edgesInMatch){
		if(!konigSet.contains(v)){
			konigSet.add(v);
			
			for(Vertex neighb : v.getNeighbors()){
				if(neighb.getId() != SOURCE_ID && neighb.getId() != SINK_ID){
					if(edgesInMatch == (edges.getFlow(v, neighb) > 0 || edges.getFlow(neighb, v) > 0)) {
						konigDFS(konigSet, neighb, !edgesInMatch);
					}
				}
			}
		}
	}

	// Parses the flow after push-relabel to determine which vertices are in the matching
	private void computeMatchSet(){
		for(Vertex v : LV){
			for(Vertex neighb : v.getNeighbors()){
				if(neighb.getId() != SOURCE_ID && edges.getFlow(v, neighb) > 0){
					matches.add(v);
					matches.add(neighb);
				}
			}
		}
	}


	public void printSolution(){
		System.out.println(invitees.size());
		for(Integer inviteeId : invitees){
			System.out.println(inviteeId);
		}
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Source:\t").append(source).append("\n");
		sb.append("Sink:\t").append(sink).append("\n");
		sb.append("Vertex list:\n").append(V).append("\n");
		sb.append(edges).append("\n");
		return sb.toString();
	}
}

// Class to present interface to all edge-related information
// class Edges {
// 	private Map<Vertex, Map<Vertex, Integer>> capacity;
// 	private Map<Vertex, Map<Vertex, Integer>> flow;
// 	// private int[][] capacity = new int[ProjectParams.MAX_NUM_IDS + 2][ProjectParams.MAX_NUM_IDS + 2];
// 	// private int[][] flow = new int[ProjectParams.MAX_NUM_IDS + 2][ProjectParams.MAX_NUM_IDS + 2];

// 	public Edges(int numVertices){
// 		flow = new HashMap<Vertex, Map<Vertex, Integer>>(2 * numVertices);
// 		capacity = new HashMap<Vertex, Map<Vertex, Integer>>(2 * numVertices);
// 	}

// 	public int getResidualCapacity(Vertex u, Vertex v){
// 		int forwardCapacity = getCapacity(u, v);
// 		//int backwardCapacity = getCapacity(v, u);

// 		// u->v
// 		if(forwardCapacity > 0){
// 			return forwardCapacity - getFlow(u, v);
// 		}
// 		// v->u
// 		//else if(backwardCapacity > 0){
// 		else{
// 			return getFlow(v, u);
// 		}

// 		//return 0;
// 	}

// 	// Flows from u -> v ?
// 	public boolean flowsForward(Vertex u, Vertex v){
// 		return getCapacity(u, v) > 0;
// 	}


// 	public int getFlow(Vertex u, Vertex v){
// 		return getEdgeInfo(u, v, flow, 0);
// 	}

// 	public int getCapacity(Vertex u, Vertex v){
// 		return getEdgeInfo(u, v, capacity, 0);
// 	}

// 	private int getEdgeInfo(Vertex u, Vertex v, Map<Vertex, Map<Vertex, Integer>> edgeInfo, int nullResult){
// 		Map<Vertex, Integer> edges = edgeInfo.get(u);
// 		if(edges == null){
// 			return nullResult;
// 		}
// 		else{
// 			Integer result = edges.get(v);
// 			if(result == null){
// 				return nullResult;
// 			}
// 			return result;
// 		}
// 	}

// 	// private int getEdgeInfo(Vertex u, Vertex v, int[][] edgeInfo, int nullResult){
// 	// 	return edgeInfo[u.getId() - 1000][v.getId() - 1000];
// 	// }

// 	public void setFlow(Vertex u, Vertex v, int newFlow){
// 		setEdgeInfo(u, v, flow, newFlow);
// 	}

// 	// public void setCapacity(Vertex u, Vertex v, int newCapacity){
// 	// 	setEdgeInfo(u, v, capacity, newCapacity);
// 	// }

// 	private void setEdgeInfo(Vertex u, Vertex v, Map<Vertex, Map<Vertex, Integer>> edgeInfo, int newVal){
// 		Map<Vertex, Integer> edges = edgeInfo.get(u);
// 		if(edges == null){
// 			edges = new HashMap<Vertex, Integer>();
// 			edges.put(v, newVal);
// 			edgeInfo.put(u, edges);
// 		}
// 		else{
// 			edges.put(v, newVal);
// 		}
// 	}

// 	// private void setEdgeInfo(Vertex u, Vertex v, int[][] edgeInfo, int newVal){
// 	// 	edgeInfo[u.getId() - 1000][v.getId() - 1000] = newVal;
// 	// }

// 	public void addToFlow(Vertex u, Vertex v, int flowInc){
// 		addToEdgeInfo(u, v, flow, flowInc);
// 	}

// 	// public void addToCapacity(Vertex u, Vertex v, int capInc){
// 	// 	addToEdgeInfo(u, v, capacity, capInc);
// 	// }

// 	private void addToEdgeInfo(Vertex u, Vertex v, Map<Vertex, Map<Vertex, Integer>> edgeInfo, int incVal){
// 		Map<Vertex, Integer> edges = edgeInfo.get(u);
// 		assert(edges != null);
// 		edges.put(v, incVal + edges.get(v));
// 	}

// 	// private void addToEdgeInfo(Vertex u, Vertex v, int[][] edgeInfo, int incVal){
// 	// 	edgeInfo[u.getId() - 1000][v.getId() - 1000] += incVal;
// 	// }

// 	@Override
// 	public String toString(){
// 		StringBuilder sb = new StringBuilder();
// 		sb.append("Capacity:\n").append(capacity).append("\n\n");
// 		sb.append("Flow:\n");
// 		for(Map.Entry<Vertex, Map<Vertex, Integer>> flowEdges : flow.entrySet()){
// 			Integer id1 = flowEdges.getKey().getId();
// 			sb.append(id1).append(" -> ");
// 			for(Map.Entry<Vertex, Integer> flowEdge : flowEdges.getValue().entrySet()){
// 				if(flowEdge.getValue() > 0){
// 					Integer id2 = flowEdge.getKey().getId();
// 					sb.append(id2).append(": ").append(flowEdge.getValue()).append("\t");
// 				}
// 			}
// 			sb.append("\n");
// 		}

// 		return sb.toString();
// 	}
// }

class Edges {
	// private Map<Vertex, Map<Vertex, Integer>> capacity;
	// private Map<Vertex, Map<Vertex, Integer>> flow;
	private List<Map<Vertex, Integer>> capacity;
	private List<Map<Vertex, Integer>> flow;

	public Edges(int numVertices){
		// flow = new HashMap<Vertex, Map<Vertex, Integer>>(2 * numVertices);
		// capacity = new HashMap<Vertex, Map<Vertex, Integer>>(2 * numVertices);

		flow = new ArrayList<Map<Vertex, Integer>>(numVertices);
		capacity = new ArrayList<Map<Vertex, Integer>>(numVertices);

		for(int i = 0; i < numVertices; i++){
			flow.add(null);
			capacity.add(null);
		}
	}

	public int getResidualCapacity(Vertex u, Vertex v){
		int forwardCapacity = getCapacity(u, v);
		//int backwardCapacity = getCapacity(v, u);

		// u->v
		if(forwardCapacity > 0){
			return forwardCapacity - getFlow(u, v);
		}
		// v->u
		else if(getCapacity(v, u) > 0){
		//else{
			return getFlow(v, u);
		}

		return 0;
	}

	public void flipFlow(Vertex u, Vertex v){
		// Map<Vertex, Integer> edges = flow.get(u);
		Map<Vertex, Integer> edges = flow.get(u.getIdx());
		assert(edges != null);
		
		int flow = edges.get(v);
		int newFlow = flow == 1 ? 0 : 1;
		edges.put(v, newFlow);
	}

	// Flows from u -> v ?
	public boolean flowsForward(Vertex u, Vertex v){
		return getCapacity(u, v) > 0;
	}

	public int getFlow(Vertex u, Vertex v){
		return getEdgeInfo(u, v, flow, 0);
	}

	public int getCapacity(Vertex u, Vertex v){
		return getEdgeInfo(u, v, capacity, 0);
	}

	// private int getEdgeInfo(Vertex u, Vertex v, Map<Vertex, Map<Vertex, Integer>> edgeInfo, int nullResult){
		// Map<Vertex, Integer> edges = edgeInfo.get(u);
	private int getEdgeInfo(Vertex u, Vertex v, List<Map<Vertex, Integer>> edgeInfo, int nullResult){
		Map<Vertex, Integer> edges = edgeInfo.get(u.getIdx());
		if(edges == null){
			return nullResult;
		}
		else{
			Integer result = edges.get(v);
			if(result == null){
				return nullResult;
			}
			return result;
		}
	}


	public void setFlow(Vertex u, Vertex v, int newFlow){
		setEdgeInfo(u, v, flow, newFlow);
	}

	public void setCapacity(Vertex u, Vertex v, int newCapacity){
		setEdgeInfo(u, v, capacity, newCapacity);
	}

	// private void setEdgeInfo(Vertex u, Vertex v, Map<Vertex, Map<Vertex, Integer>> edgeInfo, int newVal){
	 	// Map<Vertex, Integer> edges = edgeInfo.get(u);
	private void setEdgeInfo(Vertex u, Vertex v, List<Map<Vertex, Integer>> edgeInfo, int newVal){
		Map<Vertex, Integer> edges = edgeInfo.get(u.getIdx());
		if(edges == null){
			edges = new HashMap<Vertex, Integer>();
			edges.put(v, newVal);
			// edgeInfo.put(u, edges);
			edgeInfo.set(u.getIdx(), edges);
		}
		else{
			edges.put(v, newVal);
		}
	}

	public void addToFlow(Vertex u, Vertex v, int flowInc){
		addToEdgeInfo(u, v, flow, flowInc);
	}

	public void addToCapacity(Vertex u, Vertex v, int capInc){
		addToEdgeInfo(u, v, capacity, capInc);
	}

	// private void addToEdgeInfo(Vertex u, Vertex v, Map<Vertex, Map<Vertex, Integer>> edgeInfo, int incVal){
		// Map<Vertex, Integer> edges = edgeInfo.get(u);
	private void addToEdgeInfo(Vertex u, Vertex v, List<Map<Vertex, Integer>> edgeInfo, int incVal){
		Map<Vertex, Integer> edges = edgeInfo.get(u.getIdx());
		assert(edges != null);
		edges.put(v, incVal + edges.get(v));
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Capacity:\n").append(capacity).append("\n\n");
		sb.append("Flow:\n");
		// for(Map.Entry<Vertex, Map<Vertex, Integer>> flowEdges : flow.entrySet()){
		// 	Integer id1 = flowEdges.getKey().getId();
		// 	sb.append(id1).append(" -> ");
		// 	for(Map.Entry<Vertex, Integer> flowEdge : flowEdges.getValue().entrySet()){
		// 		if(flowEdge.getValue() > 0){
		// 			Integer id2 = flowEdge.getKey().getId();
		// 			sb.append(id2).append(": ").append(flowEdge.getValue()).append("\t");
		// 		}
		// 	}
		// 	sb.append("\n");
		// }
		for(int i = 0; i < flow.size(); i++){
			sb.append(i + 1000).append(" -> ");
			for(Map.Entry<Vertex, Integer> flowEdge : flow.get(i).entrySet()){
				if(flowEdge.getValue() > 0){
					Integer id2 = flowEdge.getKey().getId();
					sb.append(id2).append(": ").append(flowEdge.getValue()).append("\t");
				}
			}
			sb.append("\n");
		}

		return sb.toString();
	}
}

// class Edge {
// 	private Vertex start;
// 	private Vertex finish;
// 	private int flow = 0;
// 	private final int capacity = 1;

// 	public Edge(Vertex start, Vertex finish, int flow){
// 		this.start = start;
// 		this.finish = finish;
// 		this.flow = flow;
// 	}

// 	public boolean flowsForward(Vertex u, Vertex v){
// 		assert(u == start || u == finish);
// 		assert(v == start || v == finish);
// 		return u == start && v == finish;
// 	}

// 	public boolean flowsBackward(Vertex u, Vertex v){
// 		assert(u == start || u == finish);
// 		assert(v == start || v == finish);
// 		return v == start && u == finish;
// 	}

// 	public int getFlow() { return flow;}
// 	public void setFlow(int flow) { this.flow = flow;}
// 	public void addToFlow(int flowInc){ flow += flowInc;}

// 	public int getCapacity() { return capacity;}
// 	//public void setCapacity(int capacity) { this.capacity = capacity;}
// }


class Vertex {
	//private static Map<Integer, Vertex> idToVertex = new HashMap<Integer, Vertex>();
	private static Vertex[] vertices = new Vertex[ProjectParams.MAX_NUM_IDS + 2];

	private int id;

	private int height = 0;
	private int excess = 0;
	private int currNeighbor = 0;
	private List<Vertex> neighbors = new ArrayList<Vertex>();


	//public static Vertex vertexFromId(Integer id){
	public static Vertex vertexFromId(int id){
		int idx = id - 1000;
		if(vertices[idx] == null){
			vertices[idx] = new Vertex(id);
		}
		return vertices[idx];
	}

	private Vertex(Integer id){
		this.id = id;
	}

	private Vertex(int id){
		this.id = id;
	}

	public int getIdx(){
		return id - 1000;
	}

	public int getId(){ return id;}
	public int getHeight(){ return height;}
	public void setHeight(int height){this.height = height;}

	public int getExcess(){ return excess;}
	public void setExcess(int excess){this.excess = excess;}
	public void addToExcess(int excessInc) {excess += excessInc;}

	public Vertex getNeighbor(int i){ return neighbors.get(i);}

	public int getCurrNeighbor(){ return currNeighbor;}
	public void setCurrNeighbor(int currNeighbor){this.currNeighbor = currNeighbor;}
	public void incNextNeighbor() { currNeighbor++; }

	public List<Vertex> getNeighbors(){ return neighbors;}
	public int getNumNeighbors(){ return neighbors.size();}
	public void setNeighbors(List<Vertex> neighbors){ this.neighbors = neighbors;}
	public void addToNeighbors(Vertex v){ neighbors.add(v);}

	public void moveToFrontOfNeighbors(Vertex v){ 
		neighbors.remove(v);
		neighbors.add(0, v);
	}
	public void moveToBackOfNeighbors(Vertex v){
		neighbors.remove(v);
		neighbors.add(v);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Vertex ").append(id);
		sb.append(", h: ").append(height);
		sb.append(", e: ").append(excess);
		//sb.append(", currNeighbor: ").append(currNeighbor);
		//sb.append("\tneighbors: ").append(neighbors).append('\n');

		return sb.toString();
	}
}


// Storage class that represents teams
class Team {
	private int[] teammates = new int[2];

	// Constructor
	public Team(int id1, int id2){
		teammates[0] = id1; 
		teammates[1] = id2;
	}

	// Accessors
	public int[] getIds() { return teammates;}
	public int getId(int idx) {return teammates[idx];}

	// Object-inherited methods
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(teammates[0]).append(", ").append(teammates[1]);
		return sb.toString();
	}
}


// Represents problem invariants and the initial state of the problem
class ProjectParams {
	public static final int MAX_NUM_IDS = 2000;
	public static final int MAX_NY_ID = 1999;
	public static final int MAX_NUM_TEAMS = 10000;
	public static final int FRIEND_ID = 1009;
	public static final int NUM_LOCATIONS = 2;

	private List<Team> teams;
	private List<List<Integer>> ids;
	
	// Constructors and factory methods
	private ProjectParams(List<Team> teams, List<List<Integer>> ids){
		this.teams = teams;
		this.ids = ids;
	}

	public static ProjectParams fromStdIn() throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		// InputStreamReader in = new InputStreamReader(System.in);
		int numTeams = Integer.parseInt(in.readLine());
		//System.out.println("Reading in " + numTeams + " teams...");

		List<Team> teams = new ArrayList<Team>(numTeams);
		boolean[][] idPresent = new boolean[NUM_LOCATIONS][MAX_NUM_IDS / NUM_LOCATIONS];
		// ids.add(new HashSet<Integer>());
		// ids.add(new HashSet<Integer>());

		String currLine = in.readLine();

		// Could use while on readLine(), but this handles empty lines at the end better
		// Java overhead dominates the overall runtime, not the push-relabel algorithm itself,
		// which is why I bypass parseInt
		for(int currTeam = 0; currTeam < numTeams; currTeam++){
			int id1 = parse4DigitId(currLine, 0);
			int id2 = parse4DigitId(currLine, 5);
			// assert(id1 != id2);

			idPresent[0][id1 - 1000] = true;
			idPresent[1][id2 - 2000] = true;

			teams.add(new Team(id1, id2));
			
			currLine = in.readLine();
		}
		in.close();

		List<List<Integer>> uniqueIds = new ArrayList<List<Integer>>(2);
		uniqueIds.add(new ArrayList<Integer>());
		uniqueIds.add(new ArrayList<Integer>());
		for(int i = 0; i < idPresent[0].length; i++){
			if(idPresent[0][i]){
				uniqueIds.get(0).add(i + 1000);
			}
			if(idPresent[1][i]){
				uniqueIds.get(1).add(i + 2000);
			}
		}

		return new ProjectParams(teams, uniqueIds);
	}

	private static int parse4DigitId(String currLine, int startIdx){
		int num = 0;
		for(int i = 0; i < 4; i++){
			num = 10 * num + (currLine.charAt(startIdx + i) - '0');
		}
		return num;
	}

	// Print output
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("# of teams: ").append(teams.size()).append("\n");
		for(Team t : teams){
			sb.append("\t").append(t).append("\n");
		}
		return sb.toString();
	}

	// Accessors
	public List<Team> getTeams() {
		return teams;
	}

	public List<Integer> getIds(int idx){
		return ids.get(idx);
	}
}
