package uk.ac.ed.inf.mpatsis.sstubs.core;

import com.google.gson.Gson;

/**
 * @author mpatsis
 *
 */
public class MinedSStuB extends MinedBug {

	private final BugType bugType;
	
	/**
	 * 
	 */
	public MinedSStuB( String commitSHA1, String parentCommitSHA1, String commitFile, 
			BugType bugType, String patch, String projectName, int oldLineNum, int oldNodeStartChar, int oldNodeLength, int newLineNum, 
			int newNodeStartChar, int newNodeLength, 
			String before, String after ) {
		super( commitSHA1, parentCommitSHA1, commitFile, patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
				newLineNum, newNodeStartChar, newNodeLength, before, after );
		this.bugType = bugType;
	}
	
	
	@Override
	public String toString() {
		return toString( false );
	}
	
	@Override
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
		builder.append( TAB );
		builder.append( bugType );
		builder.append( "\n" );
		if ( getPatch ) {
			builder.append( fixPatch );
			builder.append( "\n-----------------------------------------\n\n" );
		}
		
		return builder.toString();
	}
	
	
	@Override
	public String toGson() {
		Gson gson = new Gson();
		final String JSON = gson.toJson(this);
		return JSON;
	}
	
	
	/**
	 * @return the bugType
	 */
	public BugType getBugType() {
		return bugType;
	}
	
}