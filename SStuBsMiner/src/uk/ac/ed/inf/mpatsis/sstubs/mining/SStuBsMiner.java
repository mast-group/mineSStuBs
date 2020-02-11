/**
 * 
 */
package uk.ac.ed.inf.mpatsis.sstubs.mining;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleImmutableEntry;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
//import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.stanford.nlp.io.EncodingPrintWriter.out;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Stemmer;

import uk.ac.ed.inf.mpatsis.sstubs.AST.ASTDifferenceLocator;
import uk.ac.ed.inf.mpatsis.sstubs.AST.RefactoredFunctionsVisitor;
import uk.ac.ed.inf.mpatsis.sstubs.AST.RefactoredVariablesVisitor;
import uk.ac.ed.inf.mpatsis.sstubs.core.BugType;
import uk.ac.ed.inf.mpatsis.sstubs.core.MinedBug;
import uk.ac.ed.inf.mpatsis.sstubs.core.MinedSStuB;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.parser.Scanner;


enum EditType {
	COMMENT_ONLY, SINGLE_STATEMENT_MOD, OTHER;
}


/**
 * @author mpatsis
 *
 */
public class SStuBsMiner {
	
	private DiffFormatter df;
	private final static String [] errorKeywordsArray = 
		{	"error", 
			"bug", 
			"fix", 
			"issue", 
			"mistake",
			"incorrect",
			"fault",
			"defect",
			"flaw",
			"type"
		};
	Set<String> errorKeywords = new HashSet<String>( Arrays.asList( errorKeywordsArray ) );
	
	private final ASTParser parser;
	private final Stemmer stemmer = new Stemmer();
	private Git git;
	private Repository repo;
	private RevWalk walk;
	private Map<String, String> options;
	
	private Hashtable<String, RefactoredVariable> refactoredVariables;
	private Hashtable<String, RefactoredFunction> refactoredFunctions;
	private HashSet<String> refactoredClasses;
	private HashSet<String> refactoringLines;
	
	private final String endl = "\n";
	
	private BufferedWriter sstubsWriter;
	private BufferedWriter bugsWriter;
	private BufferedWriter patchesSStuBWriter;
	private BufferedWriter patchesWriter;
	private final Gson objGson = new GsonBuilder().setPrettyPrinting().create();
	
	private int commits;
	private int bugFixCommits;
	private int onlyOneLiners;
	private int onlyOneLinersFitTemplate;
	private int onlyOneLinersAtleastOneFitsTemplate;
	private int filesOnlyOneLinersFitTemplate;
	private int oneLiners;
	private int refactoredLines;
	private int oneLinersFitTemplate;
	private int sstubs = 0;
	
	private ArrayList<String> nonOnlyOneLiner;
	private ArrayList<MinedSStuB> minedSstubs;
	private ArrayList<MinedBug> minedBugs;
	
	
	/**
	 * @param repositoryDir
	 * @return
	 * @throws IOException
	 */
	public static Git getGitRepository(final String repositoryDir)
			throws IOException {
		// Open a single repository
		final FileRepositoryBuilder builder = new FileRepositoryBuilder();
		final Repository repository = builder
				.setGitDir(new File(repositoryDir + "/.git")).readEnvironment()
				.findGitDir().build();
		final Git git = new Git(repository);
		return git;
	}
	

	/**
	 * 
	 */
	public SStuBsMiner( final File DATASET_EXPORT_DIR ) {
		commits = 0;
		bugFixCommits = 0;
		onlyOneLiners = 0;
		onlyOneLinersFitTemplate = 0;
		onlyOneLinersAtleastOneFitsTemplate = 0;
		filesOnlyOneLinersFitTemplate = 0;
		oneLiners = 0;
		refactoredLines = 0;
		oneLinersFitTemplate = 0;
		
		nonOnlyOneLiner = new ArrayList<String>();
		minedSstubs = new ArrayList<MinedSStuB>();
		minedBugs = new ArrayList<MinedBug>();
		
		parser = ASTParser.newParser( AST.JLS8 );
		options = new HashMap<String, String>( JavaCore.getOptions() );
		options.put( JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8 );
		
		
		try {
			patchesSStuBWriter = new BufferedWriter(new OutputStreamWriter( ( new FileOutputStream( 
					DATASET_EXPORT_DIR.getAbsolutePath() + "/patchesSStuBs.json" ) ) ) );
		} catch ( FileNotFoundException fnfe ) {
			fnfe.printStackTrace();
			System.exit(2);
		}
		try {
			sstubsWriter = new BufferedWriter(new OutputStreamWriter( ( new FileOutputStream( 
					DATASET_EXPORT_DIR.getAbsolutePath() + "/sstubs.json" ) ) ) );
		}
		catch ( FileNotFoundException  fnfe ) {
			fnfe.printStackTrace();
			System.exit(3);
		}
		try {
			bugsWriter = new BufferedWriter(new OutputStreamWriter( ( new FileOutputStream( 
					DATASET_EXPORT_DIR.getAbsolutePath() + "/bugs.json" ) ) ) );
		}
		catch ( FileNotFoundException  fnfe ) {
			fnfe.printStackTrace();
			System.exit(4);
		}
		try {
			patchesWriter = new BufferedWriter(new OutputStreamWriter( ( new FileOutputStream( 
					DATASET_EXPORT_DIR.getAbsolutePath() + "/patches.json" ) ) ) );
		} catch ( FileNotFoundException fnfe ) {
			fnfe.printStackTrace();
			System.exit(5);
		}
	}
	
	
	/**
	 * 
	 * @param repositoryDir
	 * @throws Exception 
	 */
	public void mineSStuBs( String repositoryDir ) throws Exception {
		this.git = getGitRepository( repositoryDir );
		this.repo = git.getRepository();
		this.walk = new RevWalk( repo );
		
		try {
			final Iterable<RevCommit> logs = git.log().call();
			final Iterator<RevCommit> i = logs.iterator();

			final List<RevCommit> commitSet = new ArrayList<RevCommit>();
			while (i.hasNext()) {
				final RevCommit commit = walk.parseCommit( i.next() );
				commitSet.add( commit );
			}
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			df = new DiffFormatter( out ); //DisabledOutputStream.INSTANCE );
			df.setRepository( repo );
			df.setDiffComparator( RawTextComparator.WS_IGNORE_ALL );
			df.setDetectRenames( true );
			
			for ( RevCommit commit : commitSet ) {
				out.reset();
				
				String commitPatches = null;
				commits++;
				List<String> stems = getStemList( commit.getFullMessage() );
				boolean isBugFix = false;
				for ( String stem : stems ) {
					if ( errorKeywords.contains( stem ) ) {
						isBugFix = true;
						bugFixCommits++;
						break;
					}
				}
				if ( !isBugFix ) continue;
				if ( commit.getParentCount() == 0 ) break;
								
//				boolean oneLineOnly = true;
				boolean fitsTemplate = false;
				boolean allOneLinerFilesFitTemplate = true;
				int modifiedJavaFiles = 0;
				
				boolean nonOneLiners = false;
				boolean nonCommentChanges = false;
				int refactoredChanges = 0;
				
				final String commitSHA1 = ObjectId.toString( commit.getId() );
				final String parentCommitSHA1 = ObjectId.toString( commit.getParent(0).getId() );
				final String[] split = repositoryDir.split( "/" );
				final String projectName = split[split.length - 1];
				
				refactoredVariables = new Hashtable<String, RefactoredVariable>();
				refactoredFunctions = new Hashtable<String, RefactoredFunction>();
				refactoredClasses = new HashSet<String>();
				refactoringLines = new HashSet<String>();
				
				
				RevCommit parent = commit.getParent(0);
				List<DiffEntry> diffs = df.scan( parent.getTree(), commit.getTree() );
				ArrayList<MinedSStuB> currentMinedStubs = new ArrayList<MinedSStuB>();
				ArrayList<MinedBug> currentMinedBugs = new ArrayList<MinedBug>();
				ArrayList<String> currentSStuBPatches = new ArrayList<String>();
				for ( DiffEntry diff : diffs ) {
					if ( diff.getChangeType().compareTo( ChangeType.ADD ) == 0 && diff.getNewPath().endsWith( ".java" ) ) {
						nonOneLiners = true;
						out.reset();
						break;
					}
					// If a Java file was deleted in this commit that might have affected the bug fix. Is this an issue though???
					else if ( diff.getChangeType().compareTo( ChangeType.DELETE ) == 0 && diff.getOldPath().endsWith( ".java" )) {
						nonOneLiners = true;
						out.reset();
						break;
					}
					else if ( df.toFileHeader(diff).toEditList().size() > 100 ) {
						nonOneLiners = true;
						out.reset();
						break;
					}
					// Modification on a Java file.
					else if ( diff.getChangeType().compareTo( ChangeType.MODIFY ) == 0
							&& diff.getNewPath().endsWith( ".java" ) ) {
						out.reset();
						df.format( diff );
						
						if ( out.toString().length() > 5000000 ) break;
						List<EditType> editTypes = processParseEdits( diff, out );
						boolean noMultistatenentChanges = noMultistatenentChanges( editTypes );
						
						ArrayList<CompilationUnit> ASTs = (ArrayList<CompilationUnit>) getASTs( diff, editTypes );
						if ( noMultistatenentChanges( editTypes ) && !onlyCommentEdits( editTypes ) &&
								hasSingleStatementEdit( editTypes ) ) {
							nonCommentChanges = true;
							refactoredChanges += spotRefactoringsFirstPass( ASTs, diff, out, repo );
						}
					}
				}
				
				for ( DiffEntry diff : diffs ) {
					boolean countedJavaFile = false;
					// If a new Java file was added in this commit that might have affected the bug fix.
					// We do not care about non-Java files.
					if ( diff.getChangeType().compareTo( ChangeType.ADD ) == 0 && diff.getNewPath().endsWith( ".java" ) ) {
						nonOneLiners = true;
						out.reset();
						break;
					}
					// If a Java file was deleted in this commit that might have affected the bug fix. Is this an issue though???
					else if ( diff.getChangeType().compareTo( ChangeType.DELETE ) == 0 && diff.getOldPath().endsWith( ".java" )) {
						nonOneLiners = true;
						out.reset();
						break;
					}
					else if ( df.toFileHeader(diff).toEditList().size() > 100 ) {
						nonOneLiners = true;
						out.reset();
						break;
					}
					// Modification on a Java file.
					else if ( diff.getChangeType().compareTo( ChangeType.MODIFY ) == 0
							&& diff.getNewPath().endsWith( ".java" ) ) {
						out.reset();
						df.format( diff );
						
						if ( out.toString().length() > 5000000 ) break;
						List<EditType> editTypes = processParseEdits( diff, out );
						boolean noMultistatenentChanges = noMultistatenentChanges( editTypes );
						
						ArrayList<CompilationUnit> ASTs = (ArrayList<CompilationUnit>) getASTs( diff, editTypes );
						
						if ( noMultistatenentChanges( editTypes ) && !onlyCommentEdits( editTypes ) &&
								hasSingleStatementEdit( editTypes ) ) {
							nonCommentChanges = true;
							refactoredChanges += spotRefactoringsSecondPass( ASTs, diff, out, repo );
							
							ASTNode originalAST = ASTs.get(0);
							for ( int t = 1; t < ASTs.size(); t++ ) {
								// Skip if the ASTs are equal 
								if ( ASTDifferenceLocator.equals( originalAST, ASTs.get(t) ) ) {
									nonOneLiners = true; // check me
									out.reset();
									break;
								}
								else { // Check if the AST pair fits any of the SStuB patterns.
									// The ast's are different. Now find the first node that they differ by dfs.
									SimpleImmutableEntry<ASTNode, ASTNode> nodesDiff = 
											ASTDifferenceLocator.getFirstDifferentNode( ASTs.get(t), originalAST );
									
									final String REFACTORING_LINE = diff.getNewPath() + ":" + ASTs.get(t).getLineNumber( nodesDiff.getKey().getStartPosition() );
									if ( refactoringLines.contains( REFACTORING_LINE ) ) {
//										System.out.println( "This is a refactoring line. Skipping it" );
										continue;
									}
									
									final String commitFile = diff.getNewPath();
									final String patch = out.toString( "UTF-8" );
									
									ASTNode newASTNode = nodesDiff.getKey();
									ASTNode oldASTNode = nodesDiff.getValue();
									MinedBug minedBug = new MinedBug(commitSHA1, parentCommitSHA1, commitFile, patch, projectName, 
											 ((CompilationUnit) oldASTNode.getRoot()).getLineNumber( oldASTNode.getStartPosition() ), 
											 oldASTNode.getStartPosition(), oldASTNode.getLength(), 
											 ((CompilationUnit) newASTNode.getRoot()).getLineNumber( newASTNode.getStartPosition() ), 
											 newASTNode.getStartPosition(), newASTNode.getLength(), oldASTNode.toString().replaceAll( "\n", " " ), 
											newASTNode.toString().replaceAll( "\n", " " ));
									currentMinedBugs.add( minedBug );
									
									if ( nodesDiff.getKey() instanceof Expression && 
											nodesDiff.getValue() instanceof Expression ) {
										Expression newNode = (Expression) nodesDiff.getKey();
										Expression oldNode = (Expression) nodesDiff.getValue();
										
										// Does the Expression pair fit the ChangeUnaryOperator template?
										if ( ASTDifferenceLocator.isChangeUnaryOperator( newNode, oldNode) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.toString().replaceAll( "\n", " " );
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.CHANGE_UNARY_OPERATOR, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength,
													nodeBeforeString, nodeAfterString );
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
											continue;
										}
									}
									
									// Change identifier instance.
									if ( (nodesDiff.getKey() instanceof SimpleName && 
											nodesDiff.getValue() instanceof ThisExpression) || 
											(nodesDiff.getKey() instanceof ThisExpression && 
											nodesDiff.getValue() instanceof SimpleName) ) {
										Expression newNode = (Expression) nodesDiff.getKey();
										Expression oldNode = (Expression) nodesDiff.getValue();
										
										final int oldNodeStartChar = oldNode.getStartPosition();
										final int oldNodeLength = oldNode.getLength();
										final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
										final int newNodeStartChar = newNode.getStartPosition();
										final int newNodeLength = newNode.getLength();
										final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
										final String nodeTypeString = oldNode.getClass().getSimpleName();
										final String nodeBeforeString = oldNode.getParent().toString().replaceAll( "\n", " " );
										final String nodeAfterString = newNode.getParent().toString().replaceAll( "\n", " " );
										
										MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
												commitFile, BugType.CHANGE_IDENTIFIER, 
												patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
												newLineNum, newNodeStartChar, newNodeLength,
												nodeBeforeString, nodeAfterString );
										currentMinedStubs.add( minedSStuB );
										
										if ( !countedJavaFile ) {
											modifiedJavaFiles++;
											countedJavaFile = true;
										}
										oneLinersFitTemplate++;
										currentSStuBPatches.add( out.toString() );
										fitsTemplate = true;
										continue;
									}
									
									// AST pair of Constructor calls.
									if ( nodesDiff.getKey().getNodeType() == ASTNode.CLASS_INSTANCE_CREATION &&
											nodesDiff.getValue().getNodeType() == ASTNode.CLASS_INSTANCE_CREATION ) {
										ClassInstanceCreation newNode = ( (ClassInstanceCreation) nodesDiff.getKey() );
										ClassInstanceCreation oldNode = ( (ClassInstanceCreation) nodesDiff.getValue() );
										// Is constructor call pair that fits same function swap arguments?
										if ( ASTDifferenceLocator.isSwapArguments( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.toString().replaceAll( "\n", " " );
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.SWAP_ARGUMENTS, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, 
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
										// Is constructor call pair that fits same function deleted arguments?
										else if ( ASTDifferenceLocator.isCallOverloadedMethodDeletedArgs( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.toString().replaceAll( "\n", " " );
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1,
													commitFile, BugType.OVERLOAD_METHOD_DELETED_ARGS, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, 
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
										// Is constructor call pair that fits same function more arguments?
										else if ( ASTDifferenceLocator.isCallOverloadedMethodMoreArgs( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.toString().replaceAll( "\n", " " );
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.OVERLOAD_METHOD_MORE_ARGS, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, 
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
										// Spots instances of swap boolean literal inside a constructor call.
										else if ( ASTDifferenceLocator.isSwapBooleanLiteral( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.toString().replaceAll( "\n", " " );
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.SWAP_BOOLEAN_LITERAL, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, 
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
										}
										else {
											allOneLinerFilesFitTemplate = false;
										}
									}
									// AST pair of Method Invocations.
									else if ( nodesDiff.getKey().getNodeType() == ASTNode.METHOD_INVOCATION &&
											nodesDiff.getValue().getNodeType() == ASTNode.METHOD_INVOCATION ) {
										MethodInvocation newNode = ( (MethodInvocation) nodesDiff.getKey() );
										MethodInvocation oldNode = ( (MethodInvocation) nodesDiff.getValue() );
										
										// Is method invocation pair that fits same function swap arguments?
										if ( ASTDifferenceLocator.isSwapArguments( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.toString().replaceAll( "\n", " " );
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.SWAP_ARGUMENTS, patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
										// Is method invocation pair that fits wrong function name?
										else if ( ASTDifferenceLocator.isDifferentMethodSameArgs( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.toString().replaceAll( "\n", " " );
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.DIFFERENT_METHOD_SAME_ARGS, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength,  
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
										// Is method invocation pair that fits same function change caller?
										else if ( ASTDifferenceLocator.isChangeCallerInFunctionCall( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.toString().replaceAll( "\n", " " );
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.CHANGE_CALLER_IN_FUNCTION_CALL, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength,  
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
										// Is method invocation pair that fits same function less arguments?
										else if ( ASTDifferenceLocator.isCallOverloadedMethodDeletedArgs( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.toString().replaceAll( "\n", " " );
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.OVERLOAD_METHOD_DELETED_ARGS, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength,  
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
										// Is method invocation pair that fits same function more arguments?
										else if ( ASTDifferenceLocator.isCallOverloadedMethodMoreArgs( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.toString().replaceAll( "\n", " " );
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.OVERLOAD_METHOD_MORE_ARGS, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength,  
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
										// Spots instances of swap boolean literal inside a method invocation.
										else if ( ASTDifferenceLocator.isSwapBooleanLiteral( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.toString().replaceAll( "\n", " " );
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.SWAP_BOOLEAN_LITERAL, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength,  
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
										}
										else {
											allOneLinerFilesFitTemplate = false;
										}
									}
									// Spots instances of swap boolean literal.
									else if ( nodesDiff.getKey().getNodeType() == ASTNode.BOOLEAN_LITERAL &&
											nodesDiff.getValue().getNodeType() == ASTNode.BOOLEAN_LITERAL ){
										BooleanLiteral newNode = ( (BooleanLiteral) nodesDiff.getKey() );
										BooleanLiteral oldNode = ( (BooleanLiteral) nodesDiff.getValue() );
										
										if ( ASTDifferenceLocator.isSwapBooleanLiteral( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getParent().getStartPosition();
											final int oldNodeLength = oldNode.getParent().getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getParent().getStartPosition();
											final int newNodeLength = newNode.getParent().getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getParent().getClass().getSimpleName();
											final String nodeBeforeString = oldNode.getParent().toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.getParent().toString().replaceAll( "\n", " " );
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.SWAP_BOOLEAN_LITERAL, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, 
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
									}
									// Pair of InfixExpressions
									else if ( nodesDiff.getKey().getNodeType() == ASTNode.INFIX_EXPRESSION &&
											nodesDiff.getValue().getNodeType() == ASTNode.INFIX_EXPRESSION ) {
										InfixExpression newNode = ( (InfixExpression) nodesDiff.getKey() );
										InfixExpression oldNode = ( (InfixExpression) nodesDiff.getValue() );
										
										// Spots instances of change binary operator for InfixExpression pairs.
										if ( ASTDifferenceLocator.isChangeOperator( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.toString().replaceAll( "\n", " " );
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.CHANGE_OPERATOR, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, 
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
										// Spots instances of change operand for InfixExpression pairs.
										else if ( ASTDifferenceLocator.isChangeOperand( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.toString().replaceAll( "\n", " " );
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.CHANGE_OPERAND, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, 
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
										// Checks if it is an instance of more specific if/while
										else if ( ASTDifferenceLocator.isMoreSpecificIf( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.toString().replaceAll( "\n", " " );
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.MORE_SPECIFIC_IF, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, 
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
										// Checks if it is an instance of less specific if/while
										else if ( ASTDifferenceLocator.isLessSpecificIf( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.toString().replaceAll( "\n", " " );
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.LESS_SPECIFIC_IF, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, 
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
										else {
											allOneLinerFilesFitTemplate = false;
										}
									}
									// Pair of variable declaration statements.
									else if ( nodesDiff.getKey().getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT &&
											nodesDiff.getValue().getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT ) {
										VariableDeclarationStatement newNode = ( (VariableDeclarationStatement) nodesDiff.getKey() );
										VariableDeclarationStatement oldNode = ( (VariableDeclarationStatement) nodesDiff.getValue() );
										
										// Checks if it is an instance of change modifier.
										if ( ASTDifferenceLocator.isChangeModifier( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.getModifiers() + "";
											final String nodeAfterString = newNode.getModifiers() + "";
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.CHANGE_MODIFIER, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, 
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
										else {
											allOneLinerFilesFitTemplate = false;
										}
									}
									// Pair of field declarations.
									else if ( nodesDiff.getKey().getNodeType() == ASTNode.FIELD_DECLARATION &&
											nodesDiff.getValue().getNodeType() == ASTNode.FIELD_DECLARATION ) {
										FieldDeclaration newNode = ( (FieldDeclaration) nodesDiff.getKey() );
										FieldDeclaration oldNode = ( (FieldDeclaration) nodesDiff.getValue() );
										
										// Checks if it is an instance of change modifier.
										if ( ASTDifferenceLocator.isChangeModifier( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.getModifiers() + "";
											final String nodeAfterString = newNode.getModifiers() + "";
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.CHANGE_MODIFIER, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, 
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
										else {
											allOneLinerFilesFitTemplate = false;
										}
									}
									// Pair of Method Declarations.
									else if ( nodesDiff.getKey().getNodeType() == ASTNode.METHOD_DECLARATION &&
											nodesDiff.getValue().getNodeType() == ASTNode.METHOD_DECLARATION ) {
										MethodDeclaration newNode = ( (MethodDeclaration) nodesDiff.getKey() );
										MethodDeclaration oldNode = ( (MethodDeclaration) nodesDiff.getValue() );
										// Checks if it is an instance of change modifier.
										if ( ASTDifferenceLocator.isChangeModifier( newNode, oldNode ) ) {
											
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.getModifiers() + "";
											final String nodeAfterString = newNode.getModifiers() + "";
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.CHANGE_MODIFIER, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, 
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );

											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
										// Checks if the method declaration pair fits the missing throws exception pattern.
										else if ( ASTDifferenceLocator.isAddThrowsException( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.getModifiers() + "";
											final String nodeAfterString = newNode.getModifiers() + "";
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.ADD_THROWS_EXCEPTION, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, 
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
																						
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
										// Checks if the method declaration pair fits the delete throws exception pattern.
										else if ( ASTDifferenceLocator.isDeleteThrowsException( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.getModifiers() + "";
											final String nodeAfterString = newNode.getModifiers() + "";
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.DELETE_THROWS_EXCEPTION, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, 
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
										else {
											allOneLinerFilesFitTemplate = false;
										}
									}
									// Pair of Class Declaration nodes.
									else if ( nodesDiff.getKey().getNodeType() == ASTNode.TYPE_DECLARATION &&
											nodesDiff.getValue().getNodeType() == ASTNode.TYPE_DECLARATION ) {
										TypeDeclaration newNode = ( (TypeDeclaration) nodesDiff.getKey() );
										TypeDeclaration oldNode = ( (TypeDeclaration) nodesDiff.getValue() );
										// Checks if it is an instance of change modifier
										if ( ASTDifferenceLocator.isChangeModifier( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.getModifiers() + "";
											final String nodeAfterString = newNode.getModifiers() + "";
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.CHANGE_MODIFIER, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, 
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
										else {
											allOneLinerFilesFitTemplate = false;
										}
									}
									// Pair of number literal nodes.
									else if ( nodesDiff.getKey().getNodeType() == ASTNode.NUMBER_LITERAL &&
											nodesDiff.getValue().getNodeType() == ASTNode.NUMBER_LITERAL ) {
										NumberLiteral newNode = (NumberLiteral) nodesDiff.getKey();
										NumberLiteral oldNode = (NumberLiteral) nodesDiff.getValue();
										// Check whether the pair fits the change numeric literal pattern. 
										if ( ASTDifferenceLocator.isChangeNumeral( newNode, oldNode ) ) {
											final int oldNodeStartChar = oldNode.getParent().getStartPosition();
											final int oldNodeLength = oldNode.getParent().getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getParent().getStartPosition();
											final int newNodeLength = newNode.getParent().getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getParent().getClass().getSimpleName();
											final String nodeBeforeString = oldNode.getParent().toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.getParent().toString().replaceAll( "\n", " " );
											
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.CHANGE_NUMERAL, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, 
													nodeBeforeString, nodeAfterString );
											
											currentMinedStubs.add( minedSStuB );
											
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
										}
									}
									else {
										allOneLinerFilesFitTemplate = false;
									}
									
									// Changed identifier instance
									if ( ASTDifferenceLocator.isChangeIdentifier( nodesDiff.getKey(), nodesDiff.getValue() ) ) {
										ASTNode newNode = nodesDiff.getKey();
										ASTNode oldNode = nodesDiff.getValue();
										
										if ( oldNode.getClass().getSimpleName().equals( "SimpleType" ) ) {
											oldNode = oldNode.getParent();
											newNode = newNode.getParent();
										}
										
										final int oldNodeStartChar = oldNode.getStartPosition();
										final int oldNodeLength = oldNode.getLength();
										final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
										final int newNodeStartChar = newNode.getStartPosition();
										final int newNodeLength = newNode.getLength();
										final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
										final String nodeTypeString = oldNode.getClass().getSimpleName();
										final String nodeBeforeString = oldNode.toString().replaceAll( "\n", " " );
										final String nodeAfterString = newNode.toString().replaceAll( "\n", " " );

										MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
												commitFile, BugType.CHANGE_IDENTIFIER, 
												patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
												newLineNum, newNodeStartChar, newNodeLength, 
												nodeBeforeString, nodeAfterString );
										currentMinedStubs.add( minedSStuB );

										if ( !countedJavaFile ) {
											modifiedJavaFiles++;
											countedJavaFile = true;
										}
										oneLinersFitTemplate++;
										currentSStuBPatches.add( out.toString() );
										fitsTemplate = true;
										continue;
									}
									
									
									if ( nodesDiff.getValue() instanceof Expression && 
											nodesDiff.getKey() instanceof InfixExpression ) {
										InfixExpression newNode = (InfixExpression) nodesDiff.getKey();
										Expression oldNode = (Expression) nodesDiff.getValue();
										
										// Checks if it is an instance of more specific if/while
										if ( ASTDifferenceLocator.isMoreSpecificIf( newNode, oldNode ) && 
												nodesDiff.getKey().getNodeType() != ASTNode.INFIX_EXPRESSION ) {										
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.toString().replaceAll( "\n", " " );
	
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.MORE_SPECIFIC_IF, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, 
													nodeBeforeString, nodeAfterString );
											currentMinedStubs.add( minedSStuB );
	
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
											continue;
										}
										// Checks if it is an instance of less specific if/while
										else if ( ASTDifferenceLocator.isLessSpecificIf( newNode, oldNode ) ) {										
											final int oldNodeStartChar = oldNode.getStartPosition();
											final int oldNodeLength = oldNode.getLength();
											final int oldLineNum = ((CompilationUnit) oldNode.getRoot()).getLineNumber( oldNodeStartChar );
											final int newNodeStartChar = newNode.getStartPosition();
											final int newNodeLength = newNode.getLength();
											final int newLineNum = ((CompilationUnit) newNode.getRoot()).getLineNumber( newNodeStartChar );
											final String nodeTypeString = oldNode.getClass().getSimpleName();
											final String nodeBeforeString = oldNode.toString().replaceAll( "\n", " " );
											final String nodeAfterString = newNode.toString().replaceAll( "\n", " " );
	
											MinedSStuB minedSStuB = new MinedSStuB( commitSHA1, parentCommitSHA1, 
													commitFile, BugType.LESS_SPECIFIC_IF, 
													patch, projectName, oldLineNum, oldNodeStartChar, oldNodeLength, 
													newLineNum, newNodeStartChar, newNodeLength, 
													nodeBeforeString, nodeAfterString );
											currentMinedStubs.add( minedSStuB );
	
											if ( !countedJavaFile ) {
												modifiedJavaFiles++;
												countedJavaFile = true;
											}
											oneLinersFitTemplate++;
											currentSStuBPatches.add( out.toString() );
											fitsTemplate = true;
											continue;
										}
									}
								}
							}
							
						}
						else if ( !noMultistatenentChanges( editTypes ) ) {
							nonOneLiners = true;
							out.reset();
							break;
						}
					}
					commitPatches = out.toString();
					out.reset();
				}
				
				if ( !nonOneLiners && nonCommentChanges ) {
					onlyOneLiners++;
					oneLiners += currentMinedBugs.size();
					refactoredLines += refactoredChanges;
					
					minedBugs.addAll( currentMinedBugs );
//					for ( MinedBug bug : currentMinedBugs ) {
//						bugsWriter.write( bug.toString() );
//						patchesWriter.write( bug.getPatch() );
//					}
				}
				// If only one liner bugs and there are stubs 
				if ( !nonOneLiners && currentMinedStubs.size() > 0 ) {
					sstubs += currentMinedStubs.size();
					
					for ( String patch : currentSStuBPatches ) {
						patchesSStuBWriter.write( patch );
					}
					
					minedSstubs.addAll( currentMinedStubs );
//					for ( MinedSStuB sstub : currentMinedStubs ) {
//						sstubsWriter.write( sstub.toString() );
//					}
					
				}
			}
			System.out.println( "Bug fix commits: " + bugFixCommits );
			System.out.println( "Only one liner changes commits: " + onlyOneLiners );
			System.out.println( "One liner bug changes in valid commits: " + oneLiners );
			System.out.println( "One liner bug changes are refactorings in valid commits: " + refactoredLines );
			System.out.println( "oneLinersFitTemplate: " + oneLinersFitTemplate );
			System.out.println( "SStuBs: " + sstubs );
		}
		catch( GitAPIException gApiE ) {
			gApiE.printStackTrace();
		}
		finally {
			walk.close();
			repo.close();
			git.close();
		}
	}
	
	
	public int spotRefactoringsFirstPass( ArrayList<CompilationUnit> ASTs, DiffEntry diff, 
			ByteArrayOutputStream out, Repository repo ) {
		int refactoredChanges = 0;
		if ( out.toString().length() > 5000000 ) {
			return 0;
		}
		final String FILENAME = diff.getNewPath();
		// First pass. Spots refactored functions and variables.
		
		// Get old and new file contents
		ObjectId newFileId = diff.getNewId().toObjectId();
		ObjectLoader newLoader;
		String newFileContent = null;
		ObjectId oldFileId = diff.getOldId().toObjectId();
		ObjectLoader oldLoader;
		String oldFileContent = null;
		try {
			newLoader = repo.open( newFileId );
			newFileContent = new String( newLoader.getBytes(), "UTF-8" );
			
			oldLoader = repo.open( oldFileId );
			oldFileContent = new String( oldLoader.getBytes(), "UTF-8" );
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		CompilationUnit originalAST = ASTs.get(0);
		// First pass. This spots refactored Variable and Function definitions and Class.
		for ( int t = 1; t < ASTs.size(); t++ ) {
			if ( ASTDifferenceLocator.equals( originalAST, ASTs.get(t) ) ) {
				continue;
			}
			else {
				SimpleImmutableEntry<ASTNode, ASTNode> nodesDiff = 
						ASTDifferenceLocator.getFirstDifferentNode( ASTs.get(t), originalAST );
				ASTNode newNode = nodesDiff.getKey();
				ASTNode oldNode = nodesDiff.getValue();
				
				if ( newNode.getNodeType() != oldNode.getNodeType() && 
						!( newNode.getNodeType() == ASTNode.INFIX_EXPRESSION || 
						newNode.getNodeType() == ASTNode.STRING_LITERAL || 
						newNode.getNodeType() == ASTNode.PRIMITIVE_TYPE || 
						newNode.getNodeType() == ASTNode.SIMPLE_TYPE ) ) {
					continue;
				}
				else {
					int refactoringLine = ASTs.get(t).getLineNumber( newNode.getStartPosition() );
					
					switch ( newNode.getNodeType() ) {
					case ASTNode.TYPE_DECLARATION:
						if ( !((TypeDeclaration) newNode).getName().toString().equals( 
								((TypeDeclaration) oldNode).getName().toString() ) ) {
							refactoredChanges++;
							refactoredClasses.add( ((TypeDeclaration) oldNode).getName().toString() );
							refactoringLines.add( FILENAME + ":" + refactoringLine );
						}
						else if ( ASTDifferenceLocator.isChangeModifier( 
								(TypeDeclaration) newNode, (TypeDeclaration) oldNode ) ) {
//							System.out.println( "Changed modifiers. Not refactoring" );
						}
						
						break;
					case ASTNode.VARIABLE_DECLARATION_STATEMENT:
//						System.out.println( "old var declaration " + oldNode );
//						System.out.println( "new var declaration " + newNode );
//						if ( ((VariableDeclarationStatement) oldNode).getProperty("SimpleName").equals(
//								) )
						List<VariableDeclarationFragment> oldFragments = ((VariableDeclarationStatement) oldNode).fragments();
						List<VariableDeclarationFragment> newFragments = ((VariableDeclarationStatement) newNode).fragments();
						if ( oldFragments.size() != newFragments.size() ) continue;
						else {
							for ( int i = 0; i < oldFragments.size(); i++ ) {
								if ( !oldFragments.get(i).getName().toString().equals( 
										newFragments.get(i).getName().toString() ) ) {
									final int START = oldNode.getParent().getStartPosition();
									final int END = START + oldNode.getParent().getLength() + 1;
									refactoredVariables.put( newFragments.get(i).getName().toString(), new 
											RefactoredVariable( newFragments.get(i).getName().toString(), START, END ) );
									refactoredChanges++;
									refactoringLines.add( FILENAME + ":" + refactoringLine );
								}
							}
						}
						break;
					case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
						if ( newNode.getParent().getNodeType() == ASTNode.FIELD_DECLARATION || 
								 newNode.getParent().getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT ) {
							if ( newNode.getParent().getNodeType() == ASTNode.FIELD_DECLARATION && 
									ASTDifferenceLocator.isChangeModifier( (FieldDeclaration) 
											((VariableDeclarationFragment) newNode).getParent(), 
									(FieldDeclaration) ((VariableDeclarationFragment) oldNode).getParent() ) )
								break;
							if ( newNode.getParent().getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT && 
									ASTDifferenceLocator.isChangeModifier( (VariableDeclarationStatement) 
											((VariableDeclarationFragment) newNode).getParent(),
									(VariableDeclarationStatement) ((VariableDeclarationFragment) oldNode).getParent() ) )
								break;
							
							if ( !((VariableDeclarationFragment) newNode).getName().equals( 
									((VariableDeclarationFragment) oldNode).getName() ) ) {
								final int START = oldNode.getParent().getParent().getStartPosition();
								final int END = START + oldNode.getParent().getParent().getLength() + 1;
								final String varName = ((VariableDeclarationFragment) newNode).getName().toString();
								refactoredVariables.put( varName, new RefactoredVariable( varName, START, END ) );
								refactoredChanges++;
								refactoringLines.add( FILENAME + ":" + refactoringLine );
							}
							break;
						}
						else {
							break;
						}
					case ASTNode.FIELD_DECLARATION:
						final int START_F = oldNode.getParent().getParent().getStartPosition();
						final int END_F = START_F + oldNode.getParent().getParent().getLength() + 1;
//						System.out.println( ((FieldDeclaration) newNode).fragments() );
						// FIX ME RAFA
						if ( ((FieldDeclaration) newNode).fragments().size() == 
								((FieldDeclaration) oldNode).fragments().size() ) {
							boolean refactored = false;
							for ( int i = 0; i < ((FieldDeclaration) newNode).fragments().size(); i++ ) {
								final Object newFragment = ((FieldDeclaration) newNode).fragments().get(i);
								final String newName = 
										((VariableDeclarationFragment) newFragment).getName().toString();
								
								boolean found = false;
								for ( int j = 0; j < ((FieldDeclaration) oldNode).fragments().size(); j++ ) {
									final Object oldFragment = ((FieldDeclaration) oldNode).fragments().get(j);
									final String oldName = 
											((VariableDeclarationFragment) oldFragment).getName().toString();
									
									if ( newName.equals( oldName ) ) {
										found = true;
										break;
									}
								}
								if (!found) {
									refactoredVariables.put( newName, 
											new RefactoredVariable( newName, START_F, END_F ) );
									refactoringLines.add( FILENAME + ":" + refactoringLine );
									refactored = true;
								}
							}
							if (refactored) refactoredChanges++;
						}
						else {
							refactoredChanges++;
							refactoringLines.add( FILENAME + ":" + refactoringLine );
							for ( int j = 0; j < ((FieldDeclaration) newNode).fragments().size(); j++ ) {
								Object newFragment = ((FieldDeclaration) newNode).fragments().get(j);
								final String newName = 
										((VariableDeclarationFragment) newFragment).getName().toString();
								
								boolean found = false;
								for ( int i = 0; i < ((FieldDeclaration) oldNode).fragments().size(); i++ ) {
									final Object oldFragment = ((FieldDeclaration) oldNode).fragments().get(i);
									final String oldName = 
											((VariableDeclarationFragment) oldFragment).getName().toString();
									
									if ( newName.equals( oldName ) ) {
										found = true;
										break;
									}	
								}
								if (!found) {
									refactoredVariables.put( newName, 
											new RefactoredVariable( newName, START_F, END_F ) );
								}
							}
						}
						break;
					case ASTNode.SINGLE_VARIABLE_DECLARATION:
						int START = oldNode.getParent().getStartPosition();
						int END = START + oldNode.getParent().getLength() + 1;
						
						final int START_LINE = ((CompilationUnit) oldNode.getRoot()).getLineNumber( START );
						final int END_LINE = ((CompilationUnit) oldNode.getRoot()).getLineNumber( END );
						
						if ( !((SingleVariableDeclaration) oldNode).getName().equals( 
								((SingleVariableDeclaration) newNode).getName() ) ) {
							final String varName = ((SingleVariableDeclaration) newNode).getName().toString();
							refactoredVariables.put( varName, 
									new RefactoredVariable( varName, START, END ) );
							refactoredChanges++;
							refactoringLines.add( FILENAME + ":" + refactoringLine );
						}
						break;
					case ASTNode.METHOD_DECLARATION:
						START = oldNode.getStartPosition();
						END = START + oldNode.getLength() + 1;
						
						RefactoredFunction refactoredFunc = new RefactoredFunction();
						String signature = ((MethodDeclaration) newNode).getName() + ":" + 
								((MethodDeclaration) newNode).parameters().size();
						
						if ( !((MethodDeclaration) oldNode).getName().toString().equals( 
								((MethodDeclaration) newNode).getName().toString() ) ) {
							refactoredChanges++;
							refactoredFunctions.put( signature, refactoredFunc );
							refactoringLines.add( FILENAME + ":" + refactoringLine );
						}
						else if ( ((MethodDeclaration) oldNode).getReturnType2() != 
								((MethodDeclaration) newNode).getReturnType2() && 
								(((MethodDeclaration) oldNode).getReturnType2() == null || 
										((MethodDeclaration) newNode).getReturnType2() == null ) ) {
							refactoredChanges++;
							refactoredFunctions.put( signature, refactoredFunc );
							refactoringLines.add( FILENAME + ":" + refactoringLine );
						}
						else if ( (((MethodDeclaration) oldNode).getReturnType2() != null && 
								((MethodDeclaration) oldNode).getReturnType2() != null) && 
								!((MethodDeclaration) oldNode).getReturnType2().toString().equals( 
										((MethodDeclaration) newNode).getReturnType2().toString() ) ) {
							refactoredChanges++;
							refactoredFunctions.put( signature, refactoredFunc );
							refactoringLines.add( FILENAME + ":" + refactoringLine );
						}
						else if ( ((MethodDeclaration) oldNode).parameters().size() != 
								((MethodDeclaration) newNode).parameters().size() ) {
							refactoredChanges++;
							refactoredFunctions.put( signature, refactoredFunc );
							refactoringLines.add( FILENAME + ":" + refactoringLine );
						}
						else if ( ASTDifferenceLocator.isChangeModifier( 
								(MethodDeclaration) newNode, (MethodDeclaration) oldNode ) ) {
						}
						else if ( ASTDifferenceLocator.isNonMatchingParameter( ((MethodDeclaration) newNode), 
								((MethodDeclaration) oldNode) ) ) {
							refactoredChanges++;
							refactoredFunctions.put( signature, refactoredFunc );
							refactoringLines.add( FILENAME + ":" + refactoringLine );
						}
						else if ( ((MethodDeclaration) newNode).parameters().size() == 
								((MethodDeclaration) oldNode).parameters().size() ) {
							List<ASTNode> newParameters = ((MethodDeclaration) newNode).parameters();
							List<ASTNode> oldParameters = ((MethodDeclaration) oldNode).parameters();
							for ( int i = 0; i < newParameters.size(); i++ ) {
								if ( !ASTDifferenceLocator.equals( newParameters.get(i), oldParameters.get(i) ) && 
										!((SingleVariableDeclaration) newParameters.get(i)).getName().toString().equals( 
										((SingleVariableDeclaration) oldParameters.get(i)).getName().toString()) ) {
									refactoredChanges++;
									final String varName = ((SingleVariableDeclaration) newParameters.get(i)).getName().toString();
									refactoredVariables.put( varName, 
											new RefactoredVariable( varName, START, END ) );
									refactoringLines.add( FILENAME + ":" + refactoringLine );
								}
							}
						}
						break;
					case ASTNode.QUALIFIED_NAME:
						refactoredChanges++;
						refactoringLines.add( FILENAME + ":" + refactoringLine );
						break;
					case ASTNode.SIMPLE_TYPE:
						break;
					case ASTNode.STRING_LITERAL:
						break;
					case ASTNode.INFIX_EXPRESSION:
						break;
					case ASTNode.PRIMITIVE_TYPE: // changed primitive type
						refactoredChanges++;
						break;
					case ASTNode.ENUM_DECLARATION:
						refactoredChanges++;
						break;
					case ASTNode.NUMBER_LITERAL:
						break;
					case ASTNode.SWITCH_CASE:
						refactoredChanges++;
						break;
					case ASTNode.NORMAL_ANNOTATION:
					case ASTNode.MARKER_ANNOTATION:
					case ASTNode.ARRAY_INITIALIZER:
						refactoredChanges++;
						break;
					default:
						break;
					}
				}
			}
		}
		
		return refactoredChanges;
	}
	
	public int spotRefactoringsSecondPass( ArrayList<CompilationUnit> ASTs, DiffEntry diff, 
			ByteArrayOutputStream out, Repository repo ) {
		int refactoredChanges = 0;
		if ( out.toString().length() > 5000000 ) return 0;
		
		final String FILENAME = diff.getNewPath();
		
		// Second pass. Spots refactored functions, variables, and classes uses across files.
		
		// Get old and new file contents
		ObjectId newFileId = diff.getNewId().toObjectId();
		ObjectLoader newLoader;
		String newFileContent = null;
		ObjectId oldFileId = diff.getOldId().toObjectId();
		ObjectLoader oldLoader;
		String oldFileContent = null;
		try {
			newLoader = repo.open( newFileId );
			newFileContent = new String( newLoader.getBytes(), "UTF-8" );
			
			oldLoader = repo.open( oldFileId );
			oldFileContent = new String( oldLoader.getBytes(), "UTF-8" );
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		CompilationUnit originalAST = ASTs.get(0);
		
		// Second pass. This spots lines affected by the refactored definitions of the first pass.
		for ( int t = 1; t < ASTs.size(); t++ ) {
			if ( ASTDifferenceLocator.equals( originalAST, ASTs.get(t) ) ) {
				continue;
			}
			else {
				SimpleImmutableEntry<ASTNode, ASTNode> nodesDiff = 
						ASTDifferenceLocator.getFirstDifferentNode( ASTs.get(t), originalAST );
				ASTNode newNode = nodesDiff.getKey();
				ASTNode oldNode = nodesDiff.getValue();
				if ( newNode.getNodeType() != oldNode.getNodeType() ) continue;
				else {
					RefactoredVariablesVisitor varVisitor = new RefactoredVariablesVisitor( refactoredVariables );
					RefactoredFunctionsVisitor funcVisitor = new RefactoredFunctionsVisitor( refactoredFunctions );
					final int CURRENT_REFACTORED_CHANGES = refactoredChanges;
						
					switch ( newNode.getNodeType() ) {
					case ASTNode.ASSIGNMENT:
						newNode.accept( varVisitor );
						refactoredChanges += varVisitor.getRefactoredVars();
						
						newNode.accept( funcVisitor );
						refactoredChanges += funcVisitor.getRefactoredFuncs();
						break;
					case ASTNode.INFIX_EXPRESSION:
						newNode.accept( varVisitor );
						refactoredChanges += varVisitor.getRefactoredVars();
						
						newNode.accept( funcVisitor );
						refactoredChanges += funcVisitor.getRefactoredFuncs();
						break;
					case ASTNode.METHOD_INVOCATION:
						newNode.accept( varVisitor );
						refactoredChanges += varVisitor.getRefactoredVars();
						
						newNode.accept( funcVisitor );
						refactoredChanges += funcVisitor.getRefactoredFuncs();
						break;
					case ASTNode.CLASS_INSTANCE_CREATION:
						// Probably unnecessary
						if ( refactoredClasses.contains( 
								((ClassInstanceCreation) oldNode).getType().toString() ) ) {
							refactoredChanges++;
						}
						break;
					case ASTNode.SIMPLE_TYPE:
						if ( refactoredClasses.contains( 
								((SimpleType) oldNode).getName().toString() ) ) {
							refactoredChanges++;
						}
						break;
					case ASTNode.FIELD_ACCESS:
						if ( ((FieldAccess) newNode).getExpression().toString().equals("this") ) {
							if ( refactoredVariables.containsKey( 
									((FieldAccess) newNode).getName().getIdentifier() ) ) refactoredChanges++;
						}
						break;
					default:
						newNode.accept( varVisitor );
						refactoredChanges += varVisitor.getRefactoredVars();
						
						newNode.accept( funcVisitor );
						refactoredChanges += funcVisitor.getRefactoredFuncs();
						int refactoringLine = ASTs.get(t).getLineNumber( newNode.getStartPosition() );
						if ( refactoringLines.contains( FILENAME + ":" + refactoringLine ) ) refactoredChanges = 0;
						break;
					}
					
					// Refactoring spotted
					if ( refactoredChanges != CURRENT_REFACTORED_CHANGES ) {
						int refactoringLine = ASTs.get(t).getLineNumber( newNode.getStartPosition() );
						refactoringLines.add( FILENAME + ":" + refactoringLine );
					}
				}
			}
		}
		
		return refactoredChanges;
	}
	
	
	private boolean noMultistatenentChanges( List<EditType> editTypes ) {
		for ( EditType editType : editTypes ) {
			if ( editType == EditType.OTHER ) return false;
		}
		return true;
	}
	
	
	private boolean onlyCommentEdits( List<EditType> editTypes ) {
		for ( EditType editType : editTypes ) {
			if ( editType != EditType.COMMENT_ONLY ) return false;
		}
		return true;
	}
	
	
	private boolean hasSingleStatementEdit( List<EditType> editTypes ) {
		for ( EditType editType : editTypes ) {
			if ( editType == EditType.SINGLE_STATEMENT_MOD ) return true;
		}
		return false;
	}
	
	
	public HashMap<Integer, Boolean> getCommentLinesMap( String fileContent ) {
		HashMap<Integer, Boolean> commentLinesMap = new HashMap<Integer, Boolean>();
		String [] fileLines = fileContent.split( "\n" );
		boolean inComment = false;
		for ( int i = 0; i < fileLines.length; i++ ) {
			String line = fileLines[i];
			if ( line.endsWith( "\r" ) ) {
				line = line.substring(0, line.length() - 1);
			}
			if ( !inComment ) {
				// Single line comment
				if ( line.matches( "\\s*//.*" ) ) {
					commentLinesMap.put( i, true );
				}
				// Multiline comment but ends at the same line
				else if ( line.matches( "\\s*/\\*.*\\*/.*" )) {
					commentLinesMap.put( i, true );
				}
				// Starts multiline comment
				else if ( line.matches( "\\s*/\\*.*" )) {
					inComment = true;
					commentLinesMap.put( i, inComment );
				}
				// No comment
				else {
					commentLinesMap.put( i, inComment );
				}
			}
			else {
				if ( line.matches( "\\s*\\*/\\s*.+" ) ) {
					inComment = false;
					commentLinesMap.put( i, inComment );
				}
				else if ( line.matches( "\\s*.*\\*/\\s*.+" ) ) {
					commentLinesMap.put( i, inComment );
					inComment = false;
				}
				else if ( line.matches( "\\s*.*\\*/\\s*" ) ) {
					commentLinesMap.put( i, inComment );
					inComment = false;
				}
				else if ( line.matches( "\\s*\\*.*" ) ) {
					commentLinesMap.put( i, inComment );
				}
				else {
					commentLinesMap.put( i, inComment );
				}
			}
		}
		
		return commentLinesMap;
	}
	
	/**
	 * Checks if all the edits contained in a patch are single statement changes.
	 * Import statements are an exception in this and are considered refactorings and are not accepted single statement changes.
	 * If an import statement was changed then false is returned.
	 * @param diff
	 * @param out
	 * @return
	 */
	private List<EditType> processParseEdits( DiffEntry diff, ByteArrayOutputStream out ) {
		boolean isOneLineChange = true;
		boolean notCommentChange = false;
		boolean foundYaas = false;
		
		List<EditType> editTypes = new ArrayList<EditType>();
		
		try {
			final String patch = out.toString( "UTF-8" );
			final String cleanedPatch = patch.substring( patch.lastIndexOf("@@") + 3 );
			
			// Get old and new file contents
			ObjectId newFileId = diff.getNewId().toObjectId();
			ObjectLoader newLoader = repo.open( newFileId );
			ObjectId oldFileId = diff.getOldId().toObjectId();
			ObjectLoader oldLoader = repo.open( oldFileId );
			final String newFileContent = new String( newLoader.getBytes(), "UTF-8" );
			final String[] newLines = newFileContent.split( "\n" );
			final String oldFileContent = new String( oldLoader.getBytes(), "UTF-8" );
			final String[] oldLines = oldFileContent.split( "\n" );
			
			HashMap<Integer, Boolean> oldCommentLines = getCommentLinesMap( oldFileContent );
			HashMap<Integer, Boolean> newCommentLines = getCommentLinesMap( newFileContent );
			
			Scanner lexer = new Scanner(false, false, false, ClassFileConstants.JDK1_7, null, null, true );
			
			EDIT_LOOP:for ( Edit edit : df.toFileHeader(diff).toEditList() ) {
				int deletions = 0;
				int additions = 0;
				for ( int l = edit.getBeginA(); l < edit.getEndA(); l++ ) {
					if ( oldCommentLines.size() <= l ) {
						editTypes.add( EditType.OTHER );
						continue EDIT_LOOP;
					}
					
					if ( !oldCommentLines.get(l) ) deletions++;
				}
				for ( int l = edit.getBeginB(); l < edit.getEndB(); l++ ) {
					if ( newCommentLines.size() <= l ) {
						editTypes.add( EditType.OTHER );
						continue EDIT_LOOP;
					}
					if ( !newCommentLines.get( l ) ) additions++;
				}
				
				if ( deletions == 0 && additions == 0 ) {
					editTypes.add( EditType.COMMENT_ONLY );
					continue EDIT_LOOP;
				}
				if ( deletions == 0 || additions == 0 ) {
					editTypes.add( EditType.OTHER );
					continue EDIT_LOOP;
				}
				else if ( deletions > 0 && additions > 0 ) {
					notCommentChange = true;
					// We now have to match statements
					StringBuilder oldPatchBuilder = new StringBuilder();
					
					int beforeL;
					for ( beforeL = edit.getBeginA(); beforeL < edit.getEndA(); beforeL++ ) {
						if ( !oldCommentLines.get( beforeL )  ) {
							oldPatchBuilder.append( oldLines[beforeL] );
							oldPatchBuilder.append( '\n' );
						}
					}
					
//					lexer.skipComments = true;
					lexer.setSource( oldPatchBuilder.toString().toCharArray() );
					ArrayList<String> beforeTokens = new ArrayList<String>();
					ArrayList<String> currentStatement = new ArrayList<String>();
					ArrayList<ArrayList<String>> beforeStatements = new ArrayList<ArrayList<String>>();
					try {
						boolean forOrIf = false;
						int lPars = 0;
						while( lexer.getNextToken() != lexer.TokenNameEOF ) {
							String token = lexer.getCurrentTokenString();
							beforeTokens.add( token );
							currentStatement.add( token );
							if ( (token.equals( ";" ) && !forOrIf ) || token.equals( "}" ) || token.equals( "{" ) ) {
								beforeStatements.add( currentStatement );
								currentStatement = new ArrayList<String>();
							}
							else if ( token.equals( "for" ) || token.equals( "if" ) ) {
								forOrIf = true;
							}

							if ( forOrIf ) {
								if ( token.equals( "(" ) ) lPars++;
								else if ( token.equals( ")" ) ) {
									lPars--;
									if ( lPars == 0 ) {
										forOrIf = false;
										beforeStatements.add( currentStatement );
										currentStatement = new ArrayList<String>();
									}
								}
							}
						}
						if ( !currentStatement.isEmpty() ) {
							beforeStatements.add( currentStatement );
						}
					} catch ( InvalidInputException iie ) {
						editTypes.add( EditType.OTHER );
						continue EDIT_LOOP;
					}
					
					StringBuilder newPatchBuilder = new StringBuilder();
					int afterL;
					for ( afterL = edit.getBeginB(); afterL < edit.getEndB(); afterL++ ) {
						if ( !newCommentLines.get( afterL )  ) {
							newPatchBuilder.append( newLines[afterL] );
							newPatchBuilder.append( '\n' );
						}
					}

//					lexer.skipComments = true;
					lexer.setSource( newPatchBuilder.toString().toCharArray() );
					ArrayList<String> afterTokens = new ArrayList<String>();
					currentStatement = new ArrayList<String>();
					ArrayList<ArrayList<String>> afterStatements = new ArrayList<ArrayList<String>>();
					try {
						boolean forOrIf = false;
						int lPars = 0;
						while( lexer.getNextToken() != lexer.TokenNameEOF ) {
							String token = lexer.getCurrentTokenString();
							afterTokens.add( token );
							currentStatement.add( token );
							if ( (token.equals( ";" ) && !forOrIf ) || token.equals( "}" ) || token.equals( "{" ) ) {
								afterStatements.add( currentStatement );
								currentStatement = new ArrayList<String>();
							}
							else if ( token.equals( "for" ) || token.equals( "if" ) ) {
								forOrIf = true;
							}
							if ( forOrIf ) {
								if ( token.equals( "(" ) ) lPars++;
								else if ( token.equals( ")" ) ) {
									lPars--;
									if ( lPars == 0 ) {
										forOrIf = false;
										afterStatements.add( currentStatement );
										currentStatement = new ArrayList<String>();
									}
								}
							}
						}
						if ( !currentStatement.isEmpty() ) {
							afterStatements.add( currentStatement );
						}
					} catch ( InvalidInputException iie ) {
						editTypes.add( EditType.OTHER );
						continue EDIT_LOOP;
					}
					
					// Match statements for before and after patch
					if ( beforeStatements.size() != afterStatements.size() || beforeStatements.size() == 0 ) {
						editTypes.add( EditType.OTHER );
						continue EDIT_LOOP;
					}
					int differentStatements = 0;
					// import statements are not accepted changes.
					if ( beforeStatements.get(0).get(0).equals( "import" ) || afterStatements.get(0).get(0).equals( "import" ) ) {
						editTypes.add( EditType.OTHER );
						continue EDIT_LOOP;
					}
					for ( int s = 0; s < beforeStatements.size(); s++ ) {
						if ( beforeStatements.get(s).size() != afterStatements.get(s).size() ) differentStatements++;
						else {
							for ( int t = 0; t < beforeStatements.get(s).size(); t++ ) {
								if ( !beforeStatements.get(s).get(t).equals( afterStatements.get(s).get(t) ) ) {
									differentStatements++;
									break;
								}
							}
						}
					}
					if ( differentStatements != 1 ) {
						editTypes.add( EditType.OTHER );
						continue EDIT_LOOP;
					}
					
					// Everything is okay!!!
					editTypes.add( EditType.SINGLE_STATEMENT_MOD );
				}
			}
		} 
		catch ( IOException e ) {
			e.printStackTrace();
		}
		
		return editTypes;
	}
	
	private List<CompilationUnit> getASTs( DiffEntry diff, List<EditType> editTypes ) {
		try {
			List<CompilationUnit> ASTs = new ArrayList<CompilationUnit>();
			
			// Get old and new file contents
			ObjectId newFileId = diff.getNewId().toObjectId();
			ObjectLoader newLoader = repo.open( newFileId );
			final String newFileContent = new String( newLoader.getBytes(), "UTF-8" );

			ObjectId oldFileId = diff.getOldId().toObjectId();
			ObjectLoader oldLoader = repo.open( oldFileId );
			final String oldFileContent = new String( oldLoader.getBytes(), "UTF-8" );
			
			// Get the original file's AST
			ASTParser parser = ASTParser.newParser( AST.JLS8 );
			HashMap<String, String> options = new HashMap<String, String>( JavaCore.getOptions() );
			options.put( JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8 );
			options.put( JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8 );
			options.put( JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8 );
			parser.setSource( oldFileContent.toCharArray() );
			parser.setCompilerOptions( options );
			parser.setResolveBindings( true );
			parser.setKind( ASTParser.K_COMPILATION_UNIT );
			
			CompilationUnit beforeCU = (CompilationUnit) parser.createAST( null );			 
			ASTs.add( beforeCU );
			
			final String[] oldLines = oldFileContent.split( "\n" );
			final String[] newLines = newFileContent.split( "\n" );
			
			int editIndex = 0;
			for ( Edit edit : df.toFileHeader(diff).toEditList() ) {
				if ( editTypes.get( editIndex++ ) != EditType.SINGLE_STATEMENT_MOD ) continue;
				// First calculate the file's content after applying this edit
				final StringBuilder editBuilder = new StringBuilder();
				for ( int l = 0 ; l < edit.getBeginA(); l++ ) {
					if ( oldLines[l].endsWith( "\r" ) ) 
						editBuilder.append( oldLines[l].substring(0, oldLines[l].length() - 1) );
					else editBuilder.append( oldLines[l] );
					editBuilder.append( '\n' );
				}
				
				for ( int l = edit.getBeginB(); l < edit.getEndB(); l++ ) {
					if ( newLines[l].endsWith( "\r" ) ) 
						editBuilder.append( newLines[l].substring(0, newLines[l].length() - 1) );
					else editBuilder.append( newLines[l] );
					editBuilder.append( '\n' );
				}
				for ( int l = edit.getEndA(); l < oldLines.length; l++ ) {
					if ( oldLines[l].endsWith( "\r" ) ) 
						editBuilder.append( oldLines[l].substring(0, oldLines[l].length() - 1) );
					else editBuilder.append( oldLines[l] );
					editBuilder.append( '\n' );
				}
				
				options.put( JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8 );
				options.put( JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8 );
				options.put( JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8 );
				parser.setCompilerOptions( options );
				parser.setResolveBindings( true );
				parser.setKind( ASTParser.K_COMPILATION_UNIT );
				parser.setSource( editBuilder.toString().toCharArray() );

				final CompilationUnit editCU = (CompilationUnit) parser.createAST( null );
				ASTs.add( editCU );
				
			}
			return ASTs;
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	private String createPatch( String patch, boolean before ) {
		StringBuilder patchBuilder = new StringBuilder();
		String [] patchLines = patch.split( "\n" );
		for ( String line : patchLines ) {
			if ( line.charAt(0) != '+' && before ) {
				patchBuilder.append( line.substring(1) );
				patchBuilder.append( '\n' );
			}
			else if ( line.charAt(0) == '+' && !before ) {
				patchBuilder.append( line.substring(1) );
				patchBuilder.append( '\n' );
			}
			else if ( line.charAt(0) != '+' && line.charAt(0) != '-' ) {
				patchBuilder.append( line );
				patchBuilder.append( '\n' );
			}
		}
		
		return patchBuilder.toString();
	}
	
	public boolean definitelyNonOneLiner( String patch ) {
		boolean nonOneLinerChange = false;
		int deletedStatements = 0;
		int addedStatements = 0;
		for ( String patchLine : patch.split("\n") ) {
			if ( patchLine.matches( "+\\s+" ) ) {
				
			}
		}
		
		return false;
	}
	
	public boolean isPossibleCommentLine( String line ) {
		boolean possibleCommentLine = false;
		if ( line.matches( "" ) ) {
			
		}
		
		return possibleCommentLine;
	}
	
//	private int countDifferentStatements( String before, String after ) {
//		Scanner lexer = new Scanner(false, false, false, ClassFileConstants.JDK1_7, null, null, true );
//		lexer.skipComments = true;
//		
//		lexer.setSource( before.trim().toCharArray() );
//		ArrayList<String> beforeTokens = new ArrayList<String>();
//		try {
//			while( !lexer.atEnd() ) {
//				lexer.getNextToken();
//				String token = lexer.getCurrentTokenString();
//				beforeTokens.add( token );
////				System.out.println( token );
//			}
//		} catch ( InvalidInputException e ) {
//			e.printStackTrace();
//		}
//		
//		lexer.setSource( after.trim().toCharArray() );
//		ArrayList<String> afterTokens = new ArrayList<String>();
//		try {
//			while( !lexer.atEnd() ) {
//				lexer.getNextToken();
//				String token = lexer.getCurrentTokenString();
//				afterTokens.add( token );
//				System.out.println( token );
//			}
//		} catch ( InvalidInputException e ) {
//			e.printStackTrace();
//		}
//		
//		boolean matchingStatement = true;
//		Iterator<String> beforeIt = beforeTokens.iterator();
//		Iterator<String> afterIt = afterTokens.iterator();
//		while ( beforeIt.hasNext() && afterIt.hasNext() ) {
//			String tokenBefore = beforeIt.next();
//		}
//		
//		return 0;
//	}
	
	
	private boolean patchIsOneLinerBad(DiffEntry diff) {
		int deletions = 0;
		int additions = 0;
		try {
			for (Edit edit : df.toFileHeader(diff).toEditList()) {
				deletions += edit.getEndA() - edit.getBeginA();
				additions += edit.getEndB() - edit.getBeginB();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		boolean isOneLineChange = ( deletions == 1 && additions == 1 );
		return isOneLineChange;
	}

	/**
	 * 
	 * @param message
	 * @return
	 */
	public List<String> getStemList( String message ) {
		List<Word> tokens_words = PTBTokenizer.factory().getTokenizer( new StringReader( message ) ).tokenize();
		ArrayList<String> stems = new ArrayList<String>();
		for ( Word w : tokens_words ) {
			stems.add( stemmer.stem( w ).toString().toLowerCase() );
		}
		return stems;
	}
	
	
	public void saveToJSON() throws IOException {
		sstubsWriter.write(  objGson.toJson( minedSstubs ) );
		bugsWriter.write(  objGson.toJson( minedBugs ) );
	}
	
	public void completeMining() {
		try {
			sstubsWriter.close();
			bugsWriter.close();
			patchesSStuBWriter.close();
			patchesWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println( "Error: The dataset was not succesfully saved. Crucial data might be missing..." );
			System.exit( 4 );
		}
		
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// Read the repository heads
		final String topJProjectsPath = args[0];
		final File DATASET_EXPORT_DIR = new File(args[1]);

		try {
			if ( !DATASET_EXPORT_DIR.exists() ) DATASET_EXPORT_DIR.mkdir();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		SStuBsMiner miner = new SStuBsMiner( DATASET_EXPORT_DIR );
		final File repositoriesDirectory = new File( topJProjectsPath );
		File [] reposList = repositoriesDirectory.listFiles();
		Arrays.sort( reposList );
		int p = 0;
		for ( File repoDir : reposList ) {
			if ( !repoDir.isDirectory() ) continue;
			System.out.println( "Mining repository: " + repoDir.getAbsolutePath() );
			miner.mineSStuBs( repoDir.getAbsolutePath() );			
			System.out.println( "Projects Mined: " + ++p );
			System.gc();
		}
		
		miner.saveToJSON();
		miner.completeMining();
		
		int differentMethodSameArgs = 0;
		int swapArguments = 0;
		int swapBooleanLiteral = 0;
		int overloadMethodDeletedArgs = 0; 
		int overloadMethodMoreArgs = 0;
		int changeOperator = 0;
		int changeUnaryOperator = 0;
		int changeOperand = 0;
		int changeModifier = 0;
		int changeCallerInFunctionCall = 0;
		int changeIdentifier = 0;
		int changeNumeral = 0;
		int addThrowsException = 0;
		int deleteThrowsException = 0;
		int moreSpecificIf = 0;
		int lessSpecificIf = 0;
		
		for ( MinedSStuB sstub : miner.minedSstubs ) {
			switch ( sstub.getBugType() ) {
			case DIFFERENT_METHOD_SAME_ARGS:
				differentMethodSameArgs++;
				break;
			case SWAP_ARGUMENTS:
				swapArguments++;
				break;
			case SWAP_BOOLEAN_LITERAL:
				swapBooleanLiteral++;
				break;
			case OVERLOAD_METHOD_DELETED_ARGS:
				overloadMethodDeletedArgs++;
				break;
			case OVERLOAD_METHOD_MORE_ARGS:
				overloadMethodMoreArgs++;
				break;
			case CHANGE_OPERATOR:
				changeOperator++;
				break;
			case CHANGE_UNARY_OPERATOR:
				changeUnaryOperator++;
				break;
			case CHANGE_OPERAND:
				changeOperand++;
				break;
			case CHANGE_MODIFIER:
				changeModifier++;
				break;
			case CHANGE_CALLER_IN_FUNCTION_CALL:
				changeCallerInFunctionCall++;
				break;
			case CHANGE_IDENTIFIER:
				changeIdentifier++;
				break;
			case CHANGE_NUMERAL:
				changeNumeral++;
				break;
			case ADD_THROWS_EXCEPTION:
				addThrowsException++;
				break;
			case DELETE_THROWS_EXCEPTION:
				deleteThrowsException++;
				break;
			case MORE_SPECIFIC_IF:
				moreSpecificIf++;
				break;
			case LESS_SPECIFIC_IF:
				lessSpecificIf++;
				break;
			}
		}
		
		System.out.println( "DifferentMethodSameArgs:" + differentMethodSameArgs );
		System.out.println( "SwapArguments:" + swapArguments );
		System.out.println( "SwapBooleanLiteral:" + swapBooleanLiteral );
		System.out.println( "OverloadMethodDeletedArgs:" + overloadMethodDeletedArgs );
		System.out.println( "OverloadMethodMoreArgs:" + overloadMethodMoreArgs );
		System.out.println( "ChangeOperator:" + changeOperator );
		System.out.println( "ChangeUnaryOperator:" + changeUnaryOperator );
		System.out.println( "ChangeOperand:" + changeOperand );
		System.out.println( "ChangeModifier:" + changeModifier );
		System.out.println( "ChangeCallerInFunctionCall:" + changeCallerInFunctionCall );
		System.out.println( "ChangeIdentifier:" + changeIdentifier );
		System.out.println( "ChangeNumeral:" + changeNumeral );
		System.out.println( "AddThrowsExceptionCounter:" + addThrowsException );
		System.out.println( "DeleteThrowsExceptionCounter:" + deleteThrowsException );
		System.out.println( "MoreSpecificIfCounter:" + moreSpecificIf );
		System.out.println( "LessSpecificIfCounter:" + lessSpecificIf );
		
	}

}
