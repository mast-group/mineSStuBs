/**
 * 
 */
package uk.ac.ed.inf.mpatsis.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Hashtable;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

import uk.ac.ed.inf.mpatsis.sstubs.mining.SStuBsMiner;


/**
 * @author mpatsis
 *
 */
public class ExtractRepoHeads {

	/**
	 * 
	 */
	public ExtractRepoHeads() {
		
	}
	
	public static String getHeadName(Repository repo) {
		String result = null;
		try {
			ObjectId id = repo.resolve(Constants.HEAD);
			result = id.getName();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Runtime rt = Runtime.getRuntime();
		Hashtable<String, String> repoHeads = new Hashtable<String, String>();
		
//		final File repositoriesDirectory = new File( "/disk/scratch1/mpatsis/topJavaProjects/" );
		final File repositoriesDirectory = new File( "/media/mpatsis/SeagateExternal/PhD/rafaelository/data/GHCorpora/MAST/testRepos/" );
		
		File [] reposList = repositoriesDirectory.listFiles();
		Arrays.sort( reposList );
		int p = 0;
		for ( File repoDir : reposList ) {
			if ( !repoDir.isDirectory() ) continue;
			System.out.println( repoDir.getAbsolutePath() );
			
			try {
				Git git = SStuBsMiner.getGitRepository( repoDir.getAbsolutePath() );
				Repository repo = git.getRepository();
				System.out.println( repo );
				System.out.println( repo.readOrigHead() );
				System.out.println( getHeadName( repo ) );
				
				final String headId =  getHeadName( repo );
//				final String headId =  repo.readOrigHead().name();
//				Process pr = rt.exec( new String[]{"/bin/sh", "-c", 
//						"cd " + repoDir.getAbsolutePath() + "/ ; "
//								+ "git checkout" + headId} );
//				pr.waitFor();
				repoHeads.put( repoDir.getAbsolutePath(), headId );
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
//			break;
		}
		
		try {
			FileOutputStream f = new FileOutputStream( new File( repositoriesDirectory.getAbsolutePath() + "/repoHeads.ser" ) );
			ObjectOutputStream o = new ObjectOutputStream( f );
			o.writeObject( repoHeads );
			o.close();
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
		} catch (IOException e) {
			System.out.println("Error initializing stream");
		}
	}

}
