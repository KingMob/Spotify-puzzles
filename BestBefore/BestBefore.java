//package Spotify.BestBefore;

import java.io.*;
import java.util.*;

public class BestBefore {
	public static void main(String[] args) {
		try {
			// Parse input
			PotentialDate pd = PotentialDate.dateFromStdin();
			if(!pd.stringFormIsValid()){
				System.out.println(pd.origDate() + " is illegal");
				return;
			}

			// Generate permutations and add to min-heap if valid
			PriorityQueue<String> minDateHeap = new PriorityQueue<String>();
			pd.addValidPermutationsTo(minDateHeap);

			// Output final result
			if(minDateHeap.size() == 0){
				System.out.println(pd.origDate() + " is illegal");
			}
			else {
				System.out.println(minDateHeap.peek());
			}
		}
		catch(IOException e){
			System.err.println("Error reading data from stdin\n");
			e.printStackTrace();
		}
	}
}

class PotentialDate {
	public static final int NUM_FIELDS = 3;
	public static final int NUM_PERMS = 6;

	private String origRepresentation;
	private int[] dateNums = new int[NUM_FIELDS];
	private DateValidator dv = new DateValidator();

	// Constructors
	public PotentialDate(String origRepresentation, int... dateNums) {
		this.origRepresentation = origRepresentation;
		this.dateNums = dateNums;
	}

	public PotentialDate(String origRepresentation, String[] fields) {
		this(origRepresentation, Integer.parseInt(fields[0]), Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
	}

	// Utility methods
	public boolean stringFormIsValid(){
		return dv.isValidDateString(origRepresentation.split("/"));
	}

	public void addValidPermutationsTo(Collection<String> c){
		PotentialDate[] perms = getDatePermutations();
		
		for(PotentialDate perm : perms){
			if(dv.isValid(perm)){
				c.add(perm.outputDate());
			}
		}
	}

	private PotentialDate[] getDatePermutations(){
		// Bit hacky, but since a date only has 3 nums involved, no real need to generalize perm code
		int[][] datePermIdxs = new int[][] {{0,1,2}, {0,2,1}, {1,0,2}, {1,2,0}, {2,0,1}, {2,1,0}};
		int[][] dateFieldPerms = new int[NUM_PERMS][NUM_FIELDS];
		PotentialDate[] datePerms = new PotentialDate[NUM_PERMS];

		for(int i = 0; i < NUM_PERMS; i++){
			for(int j = 0; j < NUM_FIELDS; j++){
				dateFieldPerms[i][datePermIdxs[i][j]] = dateNums[j];
			}
			datePerms[i] = new PotentialDate(origRepresentation, dateFieldPerms[i]);
		}

		return datePerms;
	}

	// Printing/formatting methods
	public void printPermutations() {
		PotentialDate[] datePerms = getDatePermutations();
		for(PotentialDate perm: datePerms){
			perm.origDate();
		}
	}

	public String origDate(){
		return origRepresentation;
	}

	public String outputDate(){
		int year = dateNums[0] < 100 ? dateNums[0] + 2000 : dateNums[0];
		return String.format("%04d-%02d-%02d", year, dateNums[1], dateNums[2]);
	}


	// Factory methods
	public static PotentialDate dateFromStdin() throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String input = in.readLine();
		in.close();

		return dateFromString(input);
	}

	public static PotentialDate dateFromString(String input) {
		String[] fields = input.split("/");
		if(fields.length != NUM_FIELDS) {
			throw(new IllegalArgumentException("Invalid input: " + input));
		}
		return new PotentialDate(input, fields);
	}

	// Accessor methods
	public String getOrigRepresentation(){
		return origRepresentation;
	}

	public int getYear(){
		return dateNums[0];
	}

	public int getMonth(){
		return dateNums[1];
	}

	public int getDay(){
		return dateNums[2];
	}
}

class DateValidator {
	// MAX_DAY is year and month-dependent, so not final
	private static final int MIN_DAY = 1;
	private static final int MIN_MONTH = 1;
	private static final int MAX_MONTH = 12;
	private static final int MIN_YEAR = 2000;
	private static final int MAX_YEAR = 2999;

	private int year;
	private int month;
	private int day;

	// Constructor
	public DateValidator(){}

	public boolean isValid(PotentialDate pd){
		this.year = pd.getYear();
		this.month = pd.getMonth();
		this.day = pd.getDay();

		canonicalizeYear();

		return isValidYear() && isValidMonth() && isValidDay();
	}

	public boolean isValidDateString(String[] dateParts){
		for(String s : dateParts){
			if(s.length() == 4){
				int year = Integer.parseInt(s);
				if(year < MIN_YEAR || year > MAX_YEAR){
					return false;
				}
			}
		}
		return true;
	}

	// Private utility methods
	private void canonicalizeYear(){
		if(year >= 0 && year <= 99)
			year += 2000;
	}

	private boolean isValidYear(){
		if(year >= MIN_YEAR && year <= MAX_YEAR){
			return true;
		}
		else{
			return false;
		}
	}

	private boolean isValidMonth(){
		if(month >= MIN_MONTH && month <= MAX_MONTH){
			return true;
		}
		else {
			return false;
		}
	}

	private boolean isValidDay(){
		int[] numDaysPerMonth;

		if(isLeapYear()){
			numDaysPerMonth = new int[]{31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
		}
		else {
			numDaysPerMonth = new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
		}

		if(isValidMonth()){
			return day >= MIN_DAY && day <= numDaysPerMonth[month - 1];
		}
		else {
			return false;
		}
	}

	private boolean isLeapYear(){
		return (year % 4 == 0) && (year % 100 != 0 || year % 400 == 0);
	}
}
