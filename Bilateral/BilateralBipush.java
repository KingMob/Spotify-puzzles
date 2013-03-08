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

public class BilateralBipush {
	public static void main(String[] args) {
		long startTime = System.nanoTime();
		try {
			// Parse input and set up
			ProjectParams pp = ProjectParams.fromStdIn();

			Solver solver = new Solver(pp);

			// Solve and print
			solver.solve();
			solver.printSolution();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		long elapsedTime = System.nanoTime() - startTime;
		System.out.println("Elapsed time (ms): " + elapsedTime/1000000.0);
	}
}

class Solver {
	private static final int SOURCE_ID = 1;
	private static final int SINK_ID = 3000;

	private ProjectParams pp;

	private List<Vertex> V = new LinkedList<Vertex>();
	private List<Edge> E;

	private List<Vertex> LV;
	private List<Vertex> RV;

	private Vertex source = Vertex.vertexFromId(SOURCE_ID);
	private Vertex sink = Vertex.vertexFromId(SINK_ID);
	private Vertex friend = Vertex.vertexFromId(ProjectParams.FRIEND_ID);

	private List<Vertex> matches = new ArrayList<Vertex>();
	private List<Integer> invitees = new ArrayList<Integer>();

	public Solver(ProjectParams pp){
		this.pp = pp;

		initialize();
	}

	public void solve() {
		System.out.println(toString());
		relabelToFront();
		
		computeMatchSet();
		computeInvitees();
	}


	private void initialize(){
		Set<Integer> minSet;
		Set<Integer> maxSet;
		boolean flippedSides = false;

		if(pp.getIds(0).size() <= pp.getIds(1).size()){
			minSet = pp.getIds(0);
			maxSet = pp.getIds(1);
		}
		else{
			minSet = pp.getIds(1);
			maxSet = pp.getIds(0);
			flippedSides = true;
		}

		LV = new ArrayList<Vertex>(minSet.size());
		RV = new ArrayList<Vertex>(maxSet.size());
		E = new ArrayList<Edge>(pp.getTeams().size());

		setupSourceSide(minSet);
		setupSinkSide(maxSet);

		setupNeighbors(flippedSides);
		biasForFriend();

		//source.setHeight(V.size() + 2);
		source.setHeight(2 * LV.size());

		//setupInitialHeights();
		//setupInitialMatching();
	}

	// Form edges from source to all left vertices, with flow from source
	// Prioritize neighbor order for friend and set height = BFS distance
	// from sink (i.e., 2)
	private void setupSourceSide(Set<Integer> ids){
		for(Integer id : ids){
			Vertex vert = Vertex.vertexFromId(id);
			LV.add(vert);

			Edge e = new Edge(source, vert, 1);
			source.addToEdges(e);
			vert.addToEdges(e);

			vert.setExcess(1);

			// Prioritize friend for initial flow
			if(id.intValue() == ProjectParams.FRIEND_ID){
				V.add(0, vert);
			}
			else{
				V.add(vert);
			}
		}

		source.setExcess(source.getExcess() - source.getNumNeighbors());
	}

	// Form edges from sink to all right vertices, and set height = BFS distance
	// from sink (i.e., 1)
	private void setupSinkSide(Set<Integer> ids){
		for(Integer id : ids){
			Vertex vert = Vertex.vertexFromId(id);
			RV.add(vert);

			Edge e = new Edge(vert, sink, 0);
			source.addToEdges(e);
			vert.addToEdges(e);

			//V.add(vert);
		}
	}

	// Set up neighborhoods for each non-sink/source vertex
	private void setupNeighbors(boolean flippedSides){
		Vertex u;
		Vertex v;

		List<Team> teams = pp.getTeams();
		for(Team t : teams){
			if(flippedSides){
				u = Vertex.vertexFromId(t.getId(1));
				v = Vertex.vertexFromId(t.getId(0));
			}
			else{
				u = Vertex.vertexFromId(t.getId(0));
				v = Vertex.vertexFromId(t.getId(1));
			}

			Edge e = new Edge(u, v, 0);
			E.add(e);
			u.addToEdges(e);
			v.addToEdges(e);
		}
	}

	private void biasForFriend(){
		List<Edge> edges = friend.getEdges();
		for(Edge e : edges){
			if(e.flowsInto(friend)){
				Vertex v = e.getNeighbor(friend);
				v.moveToFrontOfEdges(e);
			}
			else{
				Vertex v = e.getNeighbor(friend);
				v.moveToBackOfEdges(e);
			}
		}
	}

	private void setupInitialMatching(){
		for(Edge e : E){
			if(!e.getStart().isInInitialMatch() && !e.getFinish().isInInitialMatch()){
				e.setFlow(1);

				e.getStart().setInInitialMatch(true);
				e.getFinish().setInInitialMatch(true);
				e.getStart().setExcess(0);
				e.getFinish().setExcess(1);
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
			Vertex v = iter.next();

			int oldHeight = v.getHeight();
			//discharge(v);
			bipushDischarge(v);

			if(v.getHeight() > oldHeight){
				iter.remove();
				V.add(0, v);
				iter = V.listIterator(1);
			}
		}
	}

	// Bipush-relabel
	// Optimization for bipartite graphs
	private void bipushRelabel(Vertex u){
		System.out.println("Bipush-relabel on " + u);

		boolean eligibleUVEdgeFound = false;
		for(Edge uv : u.getEdges()){
			if(uv.isEligibleFrom(u)){
				eligibleUVEdgeFound = true;
				Vertex v = uv.getNeighbor(u);
				System.out.println("Found edge from u:" + u + " to v: " + v);

				for(Edge vw : v.getEdges()){
					if(u != vw.getNeighbor(v) && vw.isEligibleFrom(v)){
						Vertex w = vw.getNeighbor(v);
						System.out.println("Found edge from v:" + v + " to w: " + w);
						bipush(u, v, w, uv, vw);
						return;
					}
				}

				relabel(v, u);
				continue;
			}
		}

		if(!eligibleUVEdgeFound){
			relabel(u);
		}
	}

	// Push pushes excess flow forwards through two nodes, from L -> R -> L
	private void bipush(Vertex u, Vertex v, Vertex w, Edge uv, Edge vw){
		System.out.println("Pushing from " + u + " to " + v + " to " + w);

		int residualCapacity = Math.min(uv.getResidualCapacity(u, v), vw.getResidualCapacity(v, w));
		int excess = u.getExcess();
		int changeInFlow = excess < residualCapacity ? excess : residualCapacity;

		assert(excess > 0) : "Excess <= 0";
		assert(residualCapacity > 0) : "Resid capacity <= 0";
		assert(u.getHeight() == v.getHeight() + 1) : "Height of u != height of v + 1";
		assert(v.getHeight() == w.getHeight() + 1) : "Height of v != height of w + 1";

		// Excess unchanged at v
		u.addToExcess(-changeInFlow);
		w.addToExcess(changeInFlow);

		if(uv.flowsForward(u, v))
			uv.addToFlow(changeInFlow);
		else
			uv.addToFlow(-changeInFlow);

		if(vw.flowsForward(v, w))
			vw.addToFlow(changeInFlow);
		else
			vw.addToFlow(-changeInFlow);
	}

	// Scans through a vertex's neighbors until it finds one it can push excess to.
	// If no candidates are found, it increases height until it exceeds at least one 
	// neighbor and then tries again.
	private void bipushDischarge(Vertex u){
		System.out.println("Discharging " + u);
		while(u.getExcess() > 0){
			bipushRelabel(u);
		}
	}

	// Pushes excess flow forwards, or decreases incoming flow
	private void push(Vertex u, Vertex v, Edge e){
		System.out.println("Pushing from " + u + " to " + v);
		int excess = u.getExcess(); 
		int residualCapacity = e.getResidualCapacity(u, v);

		assert(excess > 0) : "Excess <= 0";
		assert(residualCapacity > 0) : "Resid capacity <= 0";
		assert(u.getHeight() == v.getHeight() + 1) : "Height of u != height of v + 1";

		int changeInFlow = excess < residualCapacity ? excess : residualCapacity;

		if(e.flowsForward(u, v)){
			e.addToFlow(changeInFlow);
		}
		else{
			e.addToFlow(-changeInFlow);
		}

		u.addToExcess(-changeInFlow);
		v.addToExcess(changeInFlow);
	}

	// Relabel increases vertex's height until it can push its excess to a neighboring
	// vertex
	private void relabel(Vertex u){ relabel(u, null);}
	private void relabel(Vertex u, Vertex exclude){
		System.out.println("Relabeling " + u);
		
		// Not true in bipush 	relabel!
		//assert(u.getExcess() > 0) : "u not overflowing";

		List<Edge> edges = u.getEdges();
		int minHeight = Integer.MAX_VALUE;
		for(Edge e : edges){
			Vertex v = e.getNeighbor(u);
			if(exclude == null || exclude != v){
				int residCapacity = e.getResidualCapacity(u, v);
				assert(residCapacity == 0 || u.getHeight() <= v.getHeight());

				if(residCapacity > 0){
					int partnerHeight = v.getHeight();
					minHeight = partnerHeight < minHeight ? partnerHeight : minHeight;
				}
			}
		}
		u.setHeight(1 + minHeight);
	}



	// Parses the flow after push-relabel to determine which vertices are in the matching
	private void computeMatchSet(){
		// for(Vertex v : LV){
		// 	for(Edge e : v.getEdges()){
		// 		if(e.getFlow() > 0) {
		// 			Vertex neighb = e.getNeighbor(v);
		// 			if(neighb.getId() != SOURCE_ID){
		// 				matches.add(v);
		// 				matches.add(neighb);
		// 			}
		// 		}
		// 	}
		// }
		for(Edge e : E){
			if(e.getFlow() > 0){
				matches.add(e.getStart());
				matches.add(e.getFinish());
			}
		}
	}



	// Konig's theorem methods

	// The algorithm can start with either side of the bipartite graph
	// Compute in both directions as the friend, may or may not appear in a
	// particular vertex cover, depending on graph structure
	private void computeInvitees(){
		List<Vertex> result;
		List<Vertex> minVertexCovers1 = computeMinVertexCover(LV, RV);
		List<Vertex> minVertexCovers2 = computeMinVertexCover(RV, LV);

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
	private List<Vertex> computeMinVertexCover(List<Vertex> side1, List<Vertex> side2){
		Set<Vertex> konigSet = new HashSet<Vertex>(side1.size() + side2.size());
		List<Vertex> unmatched = new ArrayList<Vertex>(side1);
		unmatched.removeAll(matches);

		for(Vertex v : unmatched){
			konigDFS(konigSet, v, false);
		}
		
		List<Vertex> result = new ArrayList<Vertex>(side2);
		result.retainAll(konigSet);

		List<Vertex> side1notInKonigSet = new ArrayList<Vertex>(side1);
		side1notInKonigSet.removeAll(konigSet);

		result.addAll(side1notInKonigSet);

		return result;
	}

	// Generates depth-first paths based on Konig's algorithms. Edges used must alternate being in 
	// or out of the matching
	private void konigDFS(Set<Vertex> konigSet, Vertex v, boolean edgesInMatch){
		if(!konigSet.contains(v)){
			konigSet.add(v);
			
			for(Edge e : v.getEdges()){
				Vertex neighb = e.getNeighbor(v);
				if(neighb.getId() != SOURCE_ID && neighb.getId() != SINK_ID){
					if(edgesInMatch == (e.getFlow() > 0)) {
						konigDFS(konigSet, neighb, !edgesInMatch);
					}
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
		sb.append("Edge list:\n").append(E).append("\n");
		return sb.toString();
	}
}


class Edge {
	private Vertex start;
	private Vertex finish;
	private int flow = 0;
	private boolean inMatch = false;
	private final int capacity = 1;

	public Edge(Vertex start, Vertex finish, int flow){
		this.start = start;
		this.finish = finish;
		this.flow = flow;
		updateMatchStatus();
	}

	public Vertex getNeighbor(Vertex v){
		if(v != start && v != finish){
			System.out.println("v: " + v);
			System.out.println("start: " + start);
			System.out.println("finish: " + finish);
		}

		assert(v == start || v == finish);
		if(v == start){
			return finish;
		}
		else{
			return start;
		}
	}

	public int getResidualCapacity(Vertex u, Vertex v){
		// u->v
		if(flowsForward(u, v)){
			return capacity - flow;
		}
		// v->u
		else {
			return flow;
		}
	}

	public boolean isEligibleFrom(Vertex v){
		Vertex neighb = getNeighbor(v);
		return((v.getHeight() == (neighb.getHeight() + 1)) && getResidualCapacity(v, neighb) > 0);
	}

	public boolean flowsInto(Vertex v){
		return v == finish;
	}

	public boolean flowsOutFrom(Vertex v){
		return v == start;
	}

	public boolean flowsForward(Vertex u, Vertex v){
		assert(u == start || u == finish);
		assert(v == start || v == finish);
		return u == start && v == finish;
	}

	public boolean flowsBackward(Vertex u, Vertex v){
		assert(u == start || u == finish);
		assert(v == start || v == finish);
		return v == start && u == finish;
	}

	public Vertex getStart() { return start;}
	public Vertex getFinish() { return finish;}


	public int getFlow() { return flow;}
	public void setFlow(int flow) { 
		this.flow = flow;
		updateMatchStatus();
	}
	public void addToFlow(int flowInc){ 
		flow += flowInc;
		updateMatchStatus();
	}

	public int getCapacity() { return capacity;}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Edge ").append(start.getId()).append(" -> ").append(finish.getId());
		sb.append(": ").append(flow);
		return sb.toString();
	}

	private void updateMatchStatus(){
		inMatch = flow > 0;
	}

}


class Vertex {
	private static Map<Integer, Vertex> idToVertex = new HashMap<Integer, Vertex>();

	private int id;

	private boolean inInitialMatch = false;
	private int height = 0;
	private int excess = 0;
	private int currNeighbor = 0;
	private List<Edge> edges = new ArrayList<Edge>();


	public static Vertex vertexFromId(Integer id){
		Vertex v = idToVertex.get(id);
		if(v == null){
			v = new Vertex(id);
			idToVertex.put(id, v);
		}
		return v;
	}

	private Vertex(int id){
		this.id = id;
	}

	public int getId(){ return id;}

	public boolean isInInitialMatch(){ return inInitialMatch;}
	public void setInInitialMatch(boolean inInitialMatch){this.inInitialMatch = inInitialMatch;}

	public int getHeight(){ return height;}
	public void setHeight(int height){this.height = height;}

	public int getExcess(){ return excess;}
	public void setExcess(int excess){this.excess = excess;}
	public void addToExcess(int excessInc) {excess += excessInc;}

	public int getCurrNeighbor(){ return currNeighbor;}
	public void setCurrNeighbor(int currNeighbor){this.currNeighbor = currNeighbor;}
	public void incNextNeighbor() { currNeighbor++; }

	public List<Edge> getEdges(){ return edges;}
	public Edge getEdge(int i){ return edges.get(i);}
	public int getNumNeighbors(){ return edges.size();}
	public void setEdges(List<Edge> edges){ this.edges = edges;}
	public void addToEdges(Edge e){ edges.add(e);}

	public void moveToFrontOfEdges(Edge e){ 
		edges.remove(e);
		edges.add(0, e);
	}
	public void moveToBackOfEdges(Edge e){
		edges.remove(e);
		edges.add(e);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Vertex ").append(id);
		sb.append(", h: ").append(height);
		sb.append(", e: ").append(excess);

		return sb.toString();
	}
}


// Storage class to represents teams
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
	private List<Set<Integer>> ids;

	// Constructors and factory methods
	private ProjectParams(List<Team> teams, List<Set<Integer>> ids){
		this.teams = teams;
		this.ids = ids;
	}

	public static ProjectParams fromStdIn() throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		int numTeams = Integer.parseInt(in.readLine());
		//System.out.println("Reading in " + numTeams + " teams...");

		List<Team> teams = new ArrayList<Team>(numTeams);
		List<Set<Integer>> ids = new ArrayList<Set<Integer>>(NUM_LOCATIONS);
		ids.add(new HashSet<Integer>(numTeams));
		ids.add(new HashSet<Integer>(numTeams));

		String currLine = in.readLine();

		// Could use while on readLine(), but this handles empty lines at the end better
		// Java overhead dominates the overall runtime, not the push-relabel algorithm itself,
		// which is why I bypass parseInt
		for(int currTeam = 0; currTeam < numTeams; currTeam++){
			Integer id1 = new Integer(parse4DigitId(currLine, 0));
			Integer id2 = new Integer(parse4DigitId(currLine, 5));
			assert(id1 != id2);
			ids.get(0).add(id1);
			ids.get(1).add(id2);

			teams.add(new Team(id1, id2));
			
			currLine = in.readLine();
		}
		in.close();

		return new ProjectParams(teams, ids);
	}

	// Java overhead dominates running time, so I bypassed parseInt in favor of 
	// my own method
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

	public Set<Integer> getIds(int idx){
		return ids.get(idx);
	}
}
