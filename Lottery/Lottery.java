//package Spotify.Lottery;

import java.io.*;
import java.math.*;

public class Lottery {
	public static void main(String[] args) {
		try {
			// Parse input
			ProblemParams pp = ProblemParams.fromStdin();

			// Compute prob
			ProbabilityComputer pc = new ProbabilityComputer(pp);
			double prob = pc.computeProb();

			// Display results
			System.out.printf("%.10f\n", prob);
		}
		catch(IOException e){
			System.err.println("Error reading data from stdin\n");
			e.printStackTrace();
		}
	}
}

// Class representing structure of the problem
class ProblemParams {
	public static final int NUM_FIELDS = 4;

	private int numContestants;
	private int numLottoWinners;
	private int maxTixPerWinner;
	private int numInGroup;

	// Constructors and factory methods
	public ProblemParams(int numContestants, int numLottoWinners, int maxTixPerWinner, int numInGroup){
		this.numContestants = numContestants;
		this.numLottoWinners = numLottoWinners;
		this.maxTixPerWinner = maxTixPerWinner;
		this.numInGroup = numInGroup;
	}

	public static ProblemParams fromStdin() throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String input = in.readLine();
		in.close();

		return fromString(input);
	}

	public static ProblemParams fromString(String input) {
		String[] fields = input.split(" ");
		if(fields.length != NUM_FIELDS) {
			throw(new IllegalArgumentException("Invalid input: " + input));
		}

		return new ProblemParams(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]), Integer.parseInt(fields[2]), Integer.parseInt(fields[3]));
	}

	@Override
	public String toString(){
		return "# of contestants: " + numContestants + "\n# of winners: " + numLottoWinners + "\nMax # tix per winner: " + maxTixPerWinner + "\n# in group: " + numInGroup;
	}

	// Accessors
	public int getNumContestants() {return numContestants;}
	public int getNumLottoWinners() {return numLottoWinners;}
	public int getMaxTixPerWinner() {return maxTixPerWinner;}
	public int getNumInGroup() {return numInGroup;}
}



// If k tickets must be won by the group, this algorithm computes the probabilities of only 0..k-1 winners occurring in the group (i.e., failure), 
// and subtracts that from 1 to find the probability of the group winning enough tickets. Could also sum up all the probabilities of
// winning exactly k..p tickets. Ideally, the method chosen would depend on the ratio between the number of winners the group needs and
// the group size, but that's kind of overkill.
//
// E.g., for 4 winners out of 100 contestants, 2 winners required to get enough tickets, 4 people in group, sampling wout replacement:
// p(0 winners in group) = (96/100 * 95/99 * 94/98 * 93/97)
// p(1 winners in group) = (4/100 * 96/99 * 95/98 * 94/97) * comb(4,1)
//		NB: the order of the fractions for p(1 winner) above indicates the order of the winners (group vs non-group), 
//		but is irrelevant to their product. All probabilities are multiplied by the the number of combinations of having X winners
//		from the group win. E.g., p(2 winners in group of 4) * comb(4, 2)
//
// ************************************************************************************************
// This could be done with doubles, but BigDecimal handles certain fractions (like tenths) much more nicely :)
// ************************************************************************************************
class ProbabilityComputer {
	public static final MathContext mc = MathContext.DECIMAL128;
	private ProblemParams pp;

	// Constructors
	public ProbabilityComputer(ProblemParams pp) {
		this.pp = pp;
	}

	public double computeProb(){
		int numThatMustWin = numWinnersGroupNeeds();

		// Check for shortcuts, otherwise compute full prob
		if(numThatMustWin > pp.getNumLottoWinners()){
			return 0.0;
		}
		else {
			int guaranteedWinners = pp.getNumInGroup() - (pp.getNumContestants() - pp.getNumLottoWinners());
			if(numThatMustWin <= guaranteedWinners) {
				return 1.0;
			}

			return computeProbNotWinningMethod(numThatMustWin, guaranteedWinners);
		}
	}

	private double computeProbNotWinningMethod(int numThatMustWin, int guaranteedWinners){
		// Compute the numerator  for rounds where there are i winners in the group,
		// and then subtract from the probability so far
		// Superior when the number of people that must win is small
		// Inferior when the number of people that must win is large

		BigDecimal p = BigDecimal.ONE;
		BigDecimal denom = computeDenominator();

		for(int i = 0; i < numThatMustWin; i++){
			if(i >= guaranteedWinners){
				// Compute the numerator factors for the probability of exactly i winners, 
				// multiply by the number of ways of taking i people from the group
				BigDecimal numerator = computeNumerator(i);
				numerator = numerator.multiply(Combinatorics.comb(pp.getNumLottoWinners(), i), mc);
				BigDecimal pOnlyIWinners = numerator.divide(denom, mc);
				p = p.subtract(pOnlyIWinners, mc);
			}
		}

		return p.doubleValue();	
	}

	private BigDecimal computeNumerator(int winnersInGroup) {
		BigDecimal numerator = BigDecimal.ONE;

		// Compute the numerator factors for winning rounds
		for(int i = pp.getNumInGroup(); i > pp.getNumInGroup() - winnersInGroup; i--){
			//System.out.println("Num factor winning: " + i);
			numerator = numerator.multiply(new BigDecimal(i, mc));
		}

		// Compute the numerator factors for non-winning rounds
		for(int i = pp.getNumContestants(); i > pp.getNumContestants() - (pp.getNumLottoWinners() - winnersInGroup); i--){
			//System.out.println("Num factor non-winning: " + (i - pp.getNumInGroup()));
			numerator = numerator.multiply(new BigDecimal(i - pp.getNumInGroup(), mc));
		}

		//System.out.println("Num final(" + winnersInGroup + " winners in group): " + numerator);
		return numerator;
	}

	private BigDecimal computeDenominator(){
		BigDecimal denom = BigDecimal.ONE;

		// Compute the denominator components, which are the same regardless of who wins a ticket that round
		for(int i = pp.getNumContestants(); i > pp.getNumContestants() - pp.getNumLottoWinners(); i--){
			denom = denom.multiply(new BigDecimal(i, mc));
		}

		return denom;
	}

	private int numWinnersGroupNeeds(){
		return (int) Math.ceil((double) pp.getNumInGroup() / (double) pp.getMaxTixPerWinner());
	}
}

// Class containing static methods for basic combinatorics like permutations and combinations
class Combinatorics {
	private Combinatorics(){};

	// Not called, but hey, why not be complete?
	public static BigDecimal perm(int n, int k) {
		BigDecimal res = BigDecimal.ONE;

		for(int i = n - k + 1; i <= n; i++){
			res = res.multiply(new BigDecimal(i), ProbabilityComputer.mc);
		}

		return res;
	}

	public static BigDecimal comb(int n, int k) {
		BigDecimal res = BigDecimal.ONE;

		for(int i = 1; i <= k; i++){
			res = res.multiply(new BigDecimal((n - i + 1.0) / i), ProbabilityComputer.mc);
		}

		return res;
	}
}