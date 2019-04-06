package uk.ac.ed.inf.mpatsis.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import edu.stanford.nlp.io.EncodingPrintWriter.out;
import uk.ac.ed.inf.mpatsis.sstubs.mining.SStuBsMiner;

public class ModifiedLinesCounter implements Runnable {
	
	final static Pattern LAST_INT_PATTERN = Pattern.compile("[^0-9]+([0-9]+)$");
	final static Pattern PATCH_LINES_PATTERN = Pattern.compile(
			"\\s*@@\\s+\\-[0-9]+,[0-9]+\\s+\\+[0-9]+,[0-9]+\\s+@@\\s*");
	final static String ENDL = "\n";
	
	private String repositoryDir;
	private String linesFile;
	private String [] cloc_command = {"/bin/bash", "-c", 
			"source $HOME/.cargo/env && loc " + System.getProperty( "user.dir" ) + "/" + linesFile};
	private Thread clocThread;
	
	private final static int MAX_THREADS = 50;
	private volatile static int runningThreads = 0;
	
	
	public ModifiedLinesCounter( File repositoryDir ) {
		this.repositoryDir = repositoryDir.getAbsolutePath();
		linesFile = repositoryDir.getName() + ".java";
		final String [] CLOC_COMMAND = {"/bin/bash", "-c", 
				"source $HOME/.cargo/env && loc " + System.getProperty( "user.dir" ) + "/" + linesFile};
		cloc_command = CLOC_COMMAND;
	}
	
	public long countModifiedLines() {
		
		long linesOfCode = 0;
		
		try{
			Git git = SStuBsMiner.getGitRepository( repositoryDir );
			Repository repo = git.getRepository();
			RevWalk walk = new RevWalk( repo );
		
			final Iterable<RevCommit> logs = git.log().call();
			final Iterator<RevCommit> i = logs.iterator();

			final List<RevCommit> commitSet = new ArrayList<RevCommit>();
			while (i.hasNext()) {
				final RevCommit commit = walk.parseCommit( i.next() );
				commitSet.add( commit );
			}
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DiffFormatter df = new DiffFormatter( out ); //DisabledOutputStream.INSTANCE );
			df.setRepository( repo );
			df.setDiffComparator( RawTextComparator.WS_IGNORE_ALL );
			df.setDetectRenames( true );
			
			for ( RevCommit commit : commitSet ) {
				if ( commit.getParentCount() == 0 ) continue; // There is no diff if there is no parent
				RevCommit parent = commit.getParent(0);
				List<DiffEntry> diffs = df.scan( parent.getTree(), commit.getTree() );
				
				for ( DiffEntry diff : diffs ) {
					out.reset();
					df.format( diff );
					if ( diff.getChangeType().compareTo( ChangeType.ADD ) == 0 && 
							diff.getNewPath().endsWith( ".java" ) ) {
						
						ObjectId newFileId = diff.getNewId().toObjectId();
						ObjectLoader newLoader = repo.open( newFileId );
						final String newFileContent = new String( newLoader.getBytes(), "UTF-8" );
						
						linesOfCode += runCloc( newFileContent );
//						System.exit(1);
						
					}
					else if ( diff.getChangeType().compareTo( ChangeType.DELETE ) == 0 && 
							diff.getOldPath().endsWith( ".java" )) {
						ObjectId oldFileId = diff.getOldId().toObjectId();
						ObjectLoader oldLoader = repo.open( oldFileId );
						final String oldFileContent = new String( oldLoader.getBytes(), "UTF-8" );
						
						linesOfCode += runCloc( oldFileContent );
					}
					else if ( diff.getChangeType().compareTo( ChangeType.MODIFY ) == 0 && 
							diff.getOldPath().endsWith( ".java" )) {
						int lines = 0;
						StringBuilder patchBuilder = new StringBuilder();
						for ( String line : out.toString().split("\\r?\\n") ) {
							if ( lines++ < 4 ) continue;
							
							Matcher matcher = PATCH_LINES_PATTERN.matcher( line );
							//if ( matcher.matches()) continue;
							if ( line.charAt(0) == '-' || line.charAt(0) == '+'  ) {
								patchBuilder.append( line.substring(1) );
								patchBuilder.append( ENDL );
							}
						}
						linesOfCode += runCloc( patchBuilder.toString() );
					}
					
				}
			}
			
		}
		catch (IOException ioe) {
			// TODO Auto-generated catch block
			ioe.printStackTrace();
		} catch (NoHeadException nhe) {
			// TODO Auto-generated catch block
			nhe.printStackTrace();
		} catch (GitAPIException gae) {
			// TODO Auto-generated catch block
			gae.printStackTrace();
		}
		
		System.out.println( repositoryDir + ":" + linesOfCode );
		return linesOfCode;
	}
	
	
	private long runCloc( final String code ) {
		long codeLines = 0;
		try {
			PrintStream out = new PrintStream( new FileOutputStream( linesFile ) );
		    out.print( code );
		    
		    Runtime console = Runtime.getRuntime();
		    Process lineCounterProcess = console.exec( cloc_command );
			BufferedReader br = new BufferedReader( new InputStreamReader( lineCounterProcess.getInputStream() ) );
			
			String line = null;
			int lines = 0;
			while ( (line = br.readLine()) != null) {
				if ( lines++ == 3 ) {
//					System.out.println( "line: " + line);
					codeLines = parseLOC( line );
					break;
				}
			}
			br.close();
			out.close();
			
			int exitVal = lineCounterProcess.waitFor();
//			System.out.println( "Process exitValue: " + exitVal );
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		} 
		catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		catch (Exception e) {
			// TODO: handle exception
		}
		
		return codeLines;
	}
	
	
	private long parseLOC( final String clocOutput ) {
		int codeLines = 0;
		Matcher matcher = LAST_INT_PATTERN.matcher( clocOutput );
		if ( matcher.find() ) {
		    String someNumberStr = matcher.group(1);
		    codeLines = Integer.parseInt( someNumberStr );
		}
		return codeLines;
	}
	
	
	@Override
	public void run() {
		System.out.println( "Running:" + repositoryDir );
		countModifiedLines();
		runningThreads--;
	}
	
	
	public void start() {
		if ( clocThread == null ) {
			clocThread = new Thread( this );
			clocThread.start();
			runningThreads++;
		}
	}
	
	public static void main( String [] args ) {
//		final String topJProjectsPath = "/home/mpatsis/src/programRepair/GithubRepos/topJProjects/";
		final String topJProjectsPath = args[0];
		final File repositoriesDirectory = new File( topJProjectsPath );
		File [] reposList = repositoriesDirectory.listFiles();
		Arrays.sort( reposList );
		for ( File repoDir : reposList ) {
			if ( !repoDir.isDirectory() ) continue;
			ModifiedLinesCounter modifiedLinesCounter = new ModifiedLinesCounter( repoDir );
			modifiedLinesCounter.start();
			while ( runningThreads >= MAX_THREADS );
//			break;
		}
	}

}