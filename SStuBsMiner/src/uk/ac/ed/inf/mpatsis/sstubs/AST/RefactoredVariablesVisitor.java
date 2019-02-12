package uk.ac.ed.inf.mpatsis.sstubs.AST;

import java.util.HashSet;
import java.util.Hashtable;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.SimpleName;

import uk.ac.ed.inf.mpatsis.sstubs.mining.RefactoredVariable;

public class RefactoredVariablesVisitor extends ASTVisitor {
	
	private int refactoredVars = 0;
	Hashtable<String, RefactoredVariable> refactoredVariables;
	HashSet<String> refactoredVariablesFound;
	
	public RefactoredVariablesVisitor( Hashtable<String, RefactoredVariable> refactoredVariables ) {
		this.refactoredVariables = refactoredVariables;
		refactoredVariablesFound = new HashSet<String>();
	}

	public RefactoredVariablesVisitor( Hashtable<String, RefactoredVariable> refactoredVariables, 
			boolean visitDocTags) {
		super(visitDocTags);
		this.refactoredVariables = refactoredVariables;
		refactoredVariablesFound = new HashSet<String>();
	}
	
	public boolean visit( SimpleName nameNode ) {
		if ( refactoredVariables.containsKey( nameNode.getIdentifier() ) && 
				nameNode.getParent().getNodeType() != ASTNode.FIELD_ACCESS ) {
			if ( refactoredVariablesFound.contains( nameNode.getIdentifier() ) ) return true;
			
//			System.out.println( nameNode + " " + nameNode.getParent() + 
//					" Refactor line!!! " + nameNode.getParent().getNodeType() );
			refactoredVars++;
			refactoredVariablesFound.add( nameNode.getIdentifier() );
		}
		return true;
	}
	
	public int getRefactoredVars() {
		return refactoredVars;
	}

}
