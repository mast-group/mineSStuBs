/**
 * 
 */
package uk.ac.ed.inf.mpatsis.sstubs.core;

import com.google.gson.Gson;

/**
 * @author mpatsis
 *
 */
public class MinedBug {

	protected final String commitSHA1;
	protected final String commitFile;
	protected final String patch;
	protected final String projectName;
	protected final int lineNum, nodeStartChar;
	protected final String before, after;
	
	protected final static String TAB = "\t";
	
	
	/**
	 * 
	 */
	public MinedBug( String commitSHA1, String commitFile, String patch, String projectName, 
			int lineNum, int nodeStartChar, String before, String after ) {
		this.commitSHA1 = commitSHA1;
		this.commitFile = commitFile;
		this.patch = patch;
		this.projectName = projectName;
		
		this.lineNum = lineNum;
		this.nodeStartChar = nodeStartChar;
		this.before = before;
		this.after = after;
	}
	
	
	@Override
	public String toString() {
		return toString( false );
	}
	
	public String toString( boolean getPatch ) {
		StringBuilder builder = new StringBuilder();
		builder.append( commitSHA1 );
		builder.append( TAB );
		builder.append( commitFile );
		builder.append( TAB );
		builder.append( projectName );
		builder.append( TAB );
		builder.append( lineNum );
		builder.append( TAB );
		builder.append( nodeStartChar );
		builder.append( TAB );
		builder.append( before );
		builder.append( TAB );
		builder.append( after );
		builder.append( "\n" );
		if ( getPatch ) {
			builder.append( patch );
			builder.append( "\n-----------------------------------------\n\n" );
		}
		
		return builder.toString();
	}
	
	
	public String toGson() {
		Gson gson = new Gson();
		final String JSON = gson.toJson(this);
		return JSON;
	}
	
	
	public String getPatch() {
		return patch + "\n-----------------------------------------\n\n";
	}
	
	public int getLineNum() {
		return lineNum;
	}
	
	public int getNodeStartChar() {
		return nodeStartChar;
	}


	/**
	 * @return the commitSHA1
	 */
	public String getCommitSHA1() {
		return commitSHA1;
	}


	/**
	 * @return the commitFile
	 */
	public String getCommitFile() {
		return commitFile;
	}


	/**
	 * @return the projectName
	 */
	public String getProjectName() {
		return projectName;
	}


	/**
	 * @return the before
	 */
	public String getBefore() {
		return before;
	}


	/**
	 * @return the after
	 */
	public String getAfter() {
		return after;
	}
	
	
}