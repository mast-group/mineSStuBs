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

	protected final String fixCommitSHA1, fixCommitParentSHA1;
	protected final String bugFilePath;
	protected final String fixPatch;
	protected final String projectName;
	protected final int bugLineNum, bugNodeStartChar, bugNodeLength;
	protected final int fixLineNum, fixNodeStartChar, fixNodeLength;
	protected final String sourceBeforeFix, sourceAfterFix;
	
	protected final static String TAB = "\t";
	
	
	/**
	 * 
	 */
	public MinedBug( String commitSHA1, String parentCommitSHA1, String commitFile, String patch, String projectName, 
			int oldLineNum, int oldNodeStartChar, int oldNodeLength, int newLineNum, 
			int newNodeStartChar, int newNodeLength, String before, String after ) {
		this.fixCommitSHA1 = commitSHA1;
		this.fixCommitParentSHA1 = parentCommitSHA1;
		this.bugFilePath = commitFile;
		this.fixPatch = patch;
		this.projectName = projectName;
		
		this.bugLineNum = oldLineNum;
		this.bugNodeStartChar = oldNodeStartChar;
		this.bugNodeLength = oldNodeLength;
		
		this.fixLineNum = newLineNum;
		this.fixNodeStartChar = newNodeStartChar;
		this.fixNodeLength = newNodeLength;
		
		this.sourceBeforeFix = before;
		this.sourceAfterFix = after;
	}
	
	
	@Override
	public String toString() {
		return toString( false );
	}
	
	
	/**
	 * 
	 * @param getPatch
	 * @return
	 */
	public String toString( boolean getPatch ) {
		StringBuilder builder = new StringBuilder();
		builder.append( fixCommitSHA1 );
		builder.append( TAB );
		builder.append( fixCommitParentSHA1 );
		builder.append( TAB );
		builder.append( bugFilePath );
		builder.append( TAB );
		builder.append( projectName );
		builder.append( TAB );
		builder.append( bugLineNum );
		builder.append( TAB );
		builder.append( bugNodeStartChar );
		builder.append( TAB );
		builder.append( bugNodeLength );
		builder.append( TAB );
		builder.append( fixLineNum );
		builder.append( TAB );
		builder.append( fixNodeStartChar );
		builder.append( TAB );
		builder.append( fixNodeLength );
		builder.append( TAB );
		builder.append( sourceBeforeFix );
		builder.append( TAB );
		builder.append( sourceAfterFix );
		builder.append( "\n" );
		if ( getPatch ) {
			builder.append( fixPatch );
			builder.append( "\n-----------------------------------------\n\n" );
		}
		
		return builder.toString();
	}
	
	
	/**
	 * 
	 * @return
	 */
	public String toGson() {
		Gson gson = new Gson();
		final String JSON = gson.toJson(this);
		return JSON;
	}
	
	
	/**
	 * 
	 * @return
	 */
	public String getFixPatch() {
		return fixPatch + "\n-----------------------------------------\n\n";
	}
	
	
	/**
	 * 
	 * @return
	 */
	public int getBugLineNum() {
		return bugLineNum;
	}

	
	/**
	 * 
	 * @return
	 */
	public int getBugNodeStartChar() {
		return bugNodeStartChar;
	}

	
	/**
	 * 
	 * @return
	 */
	public int getBugNodeLength() {
		return bugNodeLength;
	}

	
	/**
	 * 
	 * @return
	 */
	public int getFixLineNum() {
		return fixLineNum;
	}

	
	/**
	 * 
	 * @return
	 */
	public int getFixNodeStartChar() {
		return fixNodeStartChar;
	}


	/**
	 * 
	 * @return
	 */
	public int getFixNodeLength() {
		return fixNodeLength;
	}


	/**
	 * @return the commitSHA1
	 */
	public String getFixCommitSHA1() {
		return fixCommitSHA1;
	}
	
	
	/**
	 * @return the commitSHA1
	 */
	public String getFixCommitParentSHA1() {
		return fixCommitParentSHA1;
	}


	/**
	 * @return the commitFile
	 */
	public String getBugFilePath() {
		return bugFilePath;
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
	public String getSourceBeforeFix() {
		return sourceBeforeFix;
	}


	/**
	 * @return the after
	 */
	public String getSourceAfterFix() {
		return sourceAfterFix;
	}
	
	
}