package uk.ac.ed.inf.mpatsis.sstubs.core;

/**
 * @author mpatsis
 *
 */
public class MinedSStuB extends MinedBug{

	private final BugType bugType;
	
	/**
	 * 
	 */
	public MinedSStuB( String commitSHA1, String commitFile, 
			BugType bugType, String patch, String projectName, int lineNum, int nodeStartChar,
			String before, String after ) {
		super( commitSHA1, commitFile, patch, projectName, lineNum, nodeStartChar, before, after );
		this.bugType = bugType;
	}
	
	
	@Override
	public String toString() {
		return toString( false );
	}
	
	@Override
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
		builder.append( TAB );
		builder.append( bugType );
		builder.append( "\n" );
		if ( getPatch ) {
			builder.append( patch );
			builder.append( "\n-----------------------------------------\n\n" );
		}
		
		return builder.toString();
	}
	
	
	/**
	 * @return the bugType
	 */
	public BugType getBugType() {
		return bugType;
	}
	
}