package uk.ac.ed.inf.mpatsis.sstubs.mining;

public class RefactoredVariable {

	private String variableName;
	private final int start, end;
	
	public String getVariableName() {
		return variableName;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public RefactoredVariable( String variableName, int start, int end ) {
		this.variableName = variableName;
		this.start = start;
		this.end = end;
	}

}
