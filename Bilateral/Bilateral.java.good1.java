// ** Algorithm **
// The problem as described is a minimum vertex cover graph problem, i.e., how few vertices (people)
// are needed to cover all teams (edges). Given that all employees are divided into two groups, this is 
// a bipartite graph. And by Konig's theorem, the minimum vertex cover in a bipartite graph is equivalent
// to the maximum matching problem for that graph. By adding a source and a sink, and treating the edges
// as if they had a capacity of 1, the maximum matching can be treated as a maximum flow problem.
//
// The implementation is the relabel-to-front variant of the push-relabel algorithm as found in Cormen's Algorithms, 
// which runs in O(V^3) time. Theoretically, the Hopcroft-Karp method is faster for bipartite graphs, but has been 
// found to be slower in practice (Cherkassky 1998, Setubal 1996). 
//
// The only difference is a bias introduced in favor of the friend. The original algorithm specifies no order to 
// consider the neighbors in when discharging. My variant considers flow direction when setting up the neighbor list.
// When the flow is *toward* the friend, the friend is placed first in consideration for receiving flow; when the
// flow is in reverse (i.e., the friend would potentially lose flow), the friend is placed last. Since the push and 
// relabel operations are unchanged, the algorithm will still converge on a maximum flow, but one biased towards 
// including an edge with the friend, if possible.

// package Spotify.Bilateral;

import java.io.*;
import java.util.*;

public class Bilateral {
	public static void main(String[] args) {
		try {
			// Parse input and set up
			ProjectParams pp = ProjectParams.fromStdIn();
			//System.out.println(pp.getTeams());
			//System.out.println(pp);

			Solver solver = new Solver(pp);
			//System.out.println(solver + "\n");

			// Solve and print
			solver.solve();
			solver.printSolution();
		}
		catch(IOException e){
			System.err.println("Error reading data from stdin\n");
			e.printStackTrace();
		}
	}
}

class Solver {
	private static final int SOURCE_ID = 1;
	private static final int SINK_ID = 3000;

	private ProjectParams pp;

	private LinkedList<Vertex> V = new LinkedList<Vertex>();
	private Set<Vertex> LV = new HashSet<Vertex>();
	private Set<Vertex> RV = new HashSet<Vertex>();
	private Map<Vertex, Map<Vertex, Integer>> capacity;
	private Map<Vertex, Map<Vertex, Integer>> flow;
	private final Vertex source = Vertex.vertexFromEmployee(Employee.getEmployeeFromId(SOURCE_ID));
	private final Vertex sink = Vertex.vertexFromEmployee(Employee.getEmployeeFromId(SINK_ID));
	private final Vertex friend = Vertex.vertexFromEmployee(Employee.getEmployeeFromId(ProjectParams.FRIEND_ID));
	private Set<Vertex> matches = new HashSet<Vertex>();
	private Set<Integer> invitees = new HashSet<Integer>();

	public Solver(ProjectParams pp){
		this.pp = pp;

		initialize();
	}

	public void solve() {
		relabelToFront();
		//System.out.println("\n" + toString());

		computeMatchSet();
		computeInvitees();
	}


	private void initialize(){
		Set<Employee> Lset = pp.getEmployees(0);
		Set<Employee> Rset = pp.getEmployees(1);

		// FIXME!!!
		// flow = new HashMap<Vertex, Map<Vertex, Integer>>(2 * (Lset.size() + Rset.size()));
		// capacity = new HashMap<Vertex, Map<Vertex, Integer>>(2 * (Lset.size() + Rset.size()));
		flow = new TreeMap<Vertex, Map<Vertex, Integer>>();
		capacity = new TreeMap<Vertex, Map<Vertex, Integer>>();

		setupSourceSide(Lset);
		setupSinkSide(Rset);

		setupLRNeighbors();
		biasForFriend();

		source.setHeight(V.size() + 2);
	}

	private void setupSourceSide(Set<Employee> emps){
		for(Employee e : emps){
			Vertex u = Vertex.vertexFromEmployee(e);
			LV.add(u);

			source.addToNeighbors(u);
			u.addToNeighbors(source);

			setCapacity(source, u, 1);
			setFlow(source, u, 1);
			u.setExcess(1);

			// Prioritize friend for initial flow
			if(e.getId() == ProjectParams.FRIEND_ID){
				V.addFirst(u);
			}
			else{
				V.add(u);
			}
		}

		source.setExcess(source.getExcess() - source.getNumNeighbors());
	}

	private void setupSinkSide(Set<Employee> emps){
		for(Employee e : emps){
			Vertex u = Vertex.vertexFromEmployee(e);
			RV.add(u);

			sink.addToNeighbors(u);
			u.addToNeighbors(sink);

			setCapacity(u, sink, 1);
			setFlow(u, sink, 0);
			V.add(u);
		}
	}

	private void setupLRNeighbors(){
		List<Team> teams = pp.getTeams();
		for(Team t : teams){
			//System.out.println(t);
			Vertex u = Vertex.vertexFromEmployee(t.getEmployee(0));
			Vertex v = Vertex.vertexFromEmployee(t.getEmployee(1));

			u.addToNeighbors(v);
			v.addToNeighbors(u);

			setCapacity(u, v, 1);
			setFlow(u, v, 0);
		}
	}

	private void biasForFriend(){
		Vertex f = Vertex.vertexFromEmployee(Employee.getEmployeeFromId(ProjectParams.FRIEND_ID));

		List<Vertex> neighbors = f.getNeighbors();
		for(Vertex v : neighbors){
			if(flowsForward(v, f)){
				v.moveToFrontOfNeighbors(f);
			}
			else{
				v.moveToBackOfNeighbors(f);
			}
		}
	}

	private void relabelToFront(){
		assert(!V.isEmpty()) : "No vertices";
		ListIterator<Vertex> iter = V.listIterator();
		while(iter.hasNext()){
			Vertex v =  iter.next();
			//System.out.println("Considering vertex: " + v);
			int oldHeight = v.getHeight();
			discharge(v);
			if(v.getHeight() > oldHeight){
				iter.remove();
				V.addFirst(v);
				iter = V.listIterator(1);
			}
		}
	}

	// Uses Konig's theorem to compute min vertex cover
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

	private Set<Vertex> computeMinVertexCover(Set<Vertex> side1, Set<Vertex> side2){
		Set<Vertex> konigSet = new HashSet<Vertex>();
		Set<Vertex> unmatched = new TreeSet<Vertex>(side1);
		unmatched.removeAll(matches);
		//System.out.println("Matches: " + matches);
		//System.out.println("side 1 unmatched set: " + unmatched);

		for(Vertex v : unmatched){
			konigDFS(konigSet, v, false);
		}
		
		//System.out.println("Konig set: " + konigSet);

		Set<Vertex> result = new HashSet<Vertex>(side2);
		result.retainAll(konigSet);
		//System.out.println("side 2 intersect konigSet: " + result);

		Set<Vertex> side1notInKonigSet = new HashSet<Vertex>(side1);
		side1notInKonigSet.removeAll(konigSet);
		//System.out.println("side 1 not in Konig set: " + side1notInKonigSet);

		result.addAll(side1notInKonigSet);

		return result;
	}

	private void konigDFS(Set<Vertex> konigSet, Vertex v, boolean edgesInMatch){
		if(!konigSet.contains(v)){
			konigSet.add(v);
			
			for(Vertex neighb : v.getNeighbors()){
				if(neighb.getId() != SOURCE_ID && neighb.getId() != SINK_ID){
					if(edgesInMatch == (getFlow(v, neighb) > 0 || getFlow(neighb, v) > 0)) {
						konigDFS(konigSet, neighb, !edgesInMatch);
					}
				}
			}
		}
	}

	private void computeMatchSet(){
		for(Vertex v : LV){
			for(Vertex neighb : v.getNeighbors()){
				if(neighb.getId() != SOURCE_ID && getFlow(v, neighb) > 0){
					matches.add(v);
					matches.add(neighb);
				}
			}
		}
	}

	// Push-relabel methods
	// Push() pushes flow forwards, or decreases incoming flow
	private void push(Vertex u, Vertex v){
		//System.out.println("Pushing from " + u + " to " + v);
		int excess = u.getExcess(); 
		int residualCapacity = getResidualCapacity(u, v);

		assert(excess > 0) : "Excess <= 0";
		assert(residualCapacity > 0) : "Resid capacity <= 0";
		assert(u.getHeight() == v.getHeight() + 1) : "Height of u != height of v + 1";

		int changeInFlow = excess < residualCapacity ? excess : residualCapacity;

		if(flowsForward(u, v)){
			addToFlow(u, v, changeInFlow);
		}
		else{
			addToFlow(v, u, -changeInFlow);
		}

		u.addToExcess(-changeInFlow);
		v.addToExcess(changeInFlow);
	}

	private void relabel(Vertex u){
		//System.out.println("Relabeling " + u);
		assert(u.getExcess() > 0) : "u not overflowing";

		List<Vertex> neighbors = u.getNeighbors();
		int minHeight = Integer.MAX_VALUE;
		for(Vertex v : neighbors){
			int residCapacity = getResidualCapacity(u, v);
			assert(residCapacity == 0 || u.getHeight() <= v.getHeight());
			if(residCapacity > 0){
				int partnerHeight = v.getHeight();
				minHeight = partnerHeight < minHeight ? partnerHeight : minHeight;
			}
		}
		u.setHeight(1 + minHeight);
	}

	private void discharge(Vertex u){
		//System.out.println("Discharging " + u);
		while(u.getExcess() > 0){
			if(u.getCurrNeighbor() >= u.getNumNeighbors()){
				relabel(u);
				u.setCurrNeighbor(0);
			}
			else {
				Vertex v = u.getNeighbor(u.getCurrNeighbor());

				if(getResidualCapacity(u, v) > 0 && u.getHeight() == v.getHeight() + 1){
					push(u, v);
				}
				else{
					u.incNextNeighbor();
				}
			}
		}
	}

	private int getResidualCapacity(Vertex u, Vertex v){
		int forwardCapacity = getCapacity(u, v);
		int backwardCapacity = getCapacity(v, u);

		// u->v
		if(forwardCapacity > 0){
			return getCapacity(u, v) - getFlow(u, v);
		}
		// v->u
		else if(backwardCapacity > 0){
			return getFlow(v, u);
		}

		return 0;
	}

	private boolean flowsForward(Vertex u, Vertex v){
		return getCapacity(u, v) > 0;
	}

	private int getFlow(Vertex u, Vertex v){
		return getEdgeInfo(u, v, flow, 0);
	}

	private int getCapacity(Vertex u, Vertex v){
		return getEdgeInfo(u, v, capacity, 0);
	}

	private int getEdgeInfo(Vertex u, Vertex v, Map<Vertex, Map<Vertex, Integer>> edgeInfo, int nullResult){
		Map<Vertex, Integer> edges = edgeInfo.get(u);
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


	private void setFlow(Vertex u, Vertex v, int newFlow){
		setEdgeInfo(u, v, flow, newFlow);
	}

	private void setCapacity(Vertex u, Vertex v, int newCapacity){
		setEdgeInfo(u, v, capacity, newCapacity);
	}

	private void setEdgeInfo(Vertex u, Vertex v, Map<Vertex, Map<Vertex, Integer>> edgeInfo, int newVal){
		Map<Vertex, Integer> edges = edgeInfo.get(u);
		if(edges == null){
			edges = new HashMap<Vertex, Integer>();
			edges.put(v, newVal);
			edgeInfo.put(u, edges);
		}
		else{
			edges.put(v, newVal);
		}
	}

	private void addToFlow(Vertex u, Vertex v, int flowInc){
		addToEdgeInfo(u, v, flow, flowInc);
	}

	private void addToCapacity(Vertex u, Vertex v, int capInc){
		addToEdgeInfo(u, v, capacity, capInc);
	}

	private void addToEdgeInfo(Vertex u, Vertex v, Map<Vertex, Map<Vertex, Integer>> edgeInfo, int incVal){
		Map<Vertex, Integer> edges = edgeInfo.get(u);
		assert(edges != null);
		// if(edges == null){
		// 	edges = new HashMap<Vertex, Integer>();
		// 	edges.put(v, incVal);
		// 	edgeInfo.put(u, edges);
		// }
		// else{
			edges.put(v, incVal + edges.get(v));
		// }
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
		sb.append("Capacity:\n").append(capacity).append("\n\n");
		//sb.append("Flow:\n").append(flow).append("\n");
		sb.append("Flow:\n");
		for(Map.Entry<Vertex, Map<Vertex, Integer>> edges : flow.entrySet()){
			Integer id1 = edges.getKey().getId();
			sb.append(id1).append(" -> ");
			for(Map.Entry<Vertex, Integer> flowEdge : edges.getValue().entrySet()){
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

class Vertex implements Comparable {
	private static Map<Employee, Vertex> empToVertex = new HashMap<Employee, Vertex>();

	private Employee e;

	private int height = 0;
	private int excess = 0;
	private int currNeighbor = 0;
	private List<Vertex> neighbors = new ArrayList<Vertex>();


	public static Vertex vertexFromEmployee(Employee e){
		Vertex v = empToVertex.get(e);
		if(v == null){
			v = new Vertex(e);
			empToVertex.put(e, v);
		}
		return v;
	}

	private Vertex(Employee e){
		this.e = e;
	}

	public Employee getEmployee(){
		return e;
	}

	public Integer getId(){ return e.getId();}
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
		sb.append("Vertex ").append(e.getId());
		sb.append(", h: ").append(height);
		sb.append(", e: ").append(excess);
		//sb.append(", currNeighbor: ").append(currNeighbor);
		//sb.append("\tneighbors: ").append(neighbors).append('\n');

		return sb.toString();
	}

	// @Override 
	// public boolean equals(Object o){
	// 	boolean result = false;
	// 	if(o != null && o.getClass() == getClass()){
	// 		Vertex oVert = (Vertex) o;
	// 		result = e.equals(oVert.e) &&
	// 			neighbors.equals(oVert.neighbors);
	// 	}
	// 	return result;
	// }

	// @Override 
	// public int hashCode() {
	// 	int result = 17;
	//     result = 31 * result + e.hashCode();
 //    	result = 31 * result + neighbors.hashCode();
	// 	return result;
	// }

	@Override
	public int compareTo(Object o){
		return getId().compareTo(((Vertex) o).getId());
	}
}

// Class that represents employee
class Employee {
	private static Map<Integer, Employee> idToEmployee = new HashMap<Integer, Employee>();

	private Integer id;
	private Team[] teams;

	public static Employee getEmployeeFromId(Integer id){
		Employee e = idToEmployee.get(id);
		if(e == null){
			e = new Employee(id);
			idToEmployee.put(id, e);
		}
		return e;
	}

	private Employee(Integer id){
		this.id = id;
		teams = new Team[0];
	}

	public Employee(Integer id, Team[] teams){
		this.id = id;
		this.teams = teams;
	}

	public int getNumTeams() { return teams.length;}

	// Accessors
	public Team[] getTeams(){ return teams;}
	public void setTeams(Team[] teams){ this.teams = teams; }

	public Integer getId(){ return id;}

	@Override 
	public boolean equals(Object o){
		boolean result = false;
		if(o != null && o.getClass() == getClass()){
			Employee oEmp = (Employee) o;
			result = (id.equals(oEmp.id) && Arrays.equals(teams, oEmp.teams));
		}
		return result;
	}

	@Override 
	public int hashCode() {
		int result = 17;
	    result = 31 * result + id.hashCode();
    	result = 31 * result + Arrays.hashCode(teams);
		return result;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Employee ").append(id).append("\t# teammates: ").append(teams.length);
		if(teams.length > 0){
			sb.append('\t').append(teams);
		}
		return sb.toString();
	}
}

class Edge {
	private Vertex start;
	private Vertex finish;
	private int flow = 0;
	private int capacity = 1;

	public Edge(Vertex start, Vertex finish){
		this.start = start;
		this.finish = finish;
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

	public int getFlow() { return flow;}
	public void setFlow(int flow) { this.flow = flow;}
	public void addToFlow(int flowInc){ flow += flowInc;}

	public int getCapacity() { return capacity;}
	public void setCapacity(int capacity) { this.capacity = capacity;}
}

// Class that represents teams/edges
class Team {
	private Employee[] teammates = new Employee[2];

	// Constructor
	public Team(Employee e1, Employee e2){
		setEmployees(e1, e2);
	}

	// Accessors
	public Employee[] getEmployees() { return teammates;}
	public void setEmployees(Employee e1, Employee e2) { 
		assert(e1 != null && e2 != null);
		teammates[0] = e1; 
		teammates[1] = e2;
	}

	public Employee getEmployee(int idx) {return teammates[idx];}

	// Object-inherited methods
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(teammates[0]).append(", ").append(teammates[1]);
		return sb.toString();
	}

	@Override 
	public boolean equals(Object o){
		boolean result = false;
		if(o!= null && o.getClass() == getClass()){
			Team oTeam = (Team) o;
			result = Arrays.equals(teammates, oTeam.teammates);
		}
		return result;
	}

	@Override 
	public int hashCode() {
		return Arrays.hashCode(teammates);
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
	private List<Set<Employee>> employees;

	// Constructors and factory methods
	private ProjectParams(List<Team> teams, List<Set<Employee>> employees){
		this.teams = teams;
		this.employees = employees;
	}

	public static ProjectParams fromStdIn() throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		int numTeams = Integer.parseInt(in.readLine());
		//System.out.println("Reading in " + numTeams + " teams...");

		List<Team> teams = new ArrayList<Team>(numTeams);
		List<Set<Employee>> employees = new ArrayList<Set<Employee>>(NUM_LOCATIONS);
		employees.add(new HashSet<Employee>());
		employees.add(new HashSet<Employee>());

		String currLine = in.readLine();

		// Could use while on readLine(), but this handles empty lines at the end better
		for(int currTeam = 0; currTeam < numTeams; currTeam++){
			String[] idStrings = currLine.split(" ");
			assert(idStrings.length == 2);

			Employee e1 = Employee.getEmployeeFromId(Integer.parseInt(idStrings[0]));
			Employee e2 = Employee.getEmployeeFromId(Integer.parseInt(idStrings[1]));
			assert(e1 != e2 && !e1.equals(e2));
			employees.get(0).add(e1);
			employees.get(1).add(e2);

			teams.add(new Team(e1, e2));
			
			currLine = in.readLine();
		}
		in.close();

		assert(teams.size() == uniqueTeams(teams));

		return new ProjectParams(teams, employees);
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
	public int getNumTeams() {
		return teams.size();
	}

	public List<Team> getTeams() {
		return teams;
	}

	public Set<Employee> getEmployees(int employeeIdx){
		return employees.get(employeeIdx);
	}

	// Private methods
	private static int uniqueTeams(List<Team> teams){
		Set<Team> s = new HashSet<Team>();
		for(Team t : teams){
			s.add(t);
		}
		return s.size();
	}
}
