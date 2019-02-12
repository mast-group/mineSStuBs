package uk.ac.ed.inf.mpatsis.sstubs.AST;

import java.util.HashSet;
import java.util.Hashtable;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

import uk.ac.ed.inf.mpatsis.sstubs.mining.RefactoredFunction;
import uk.ac.ed.inf.mpatsis.sstubs.mining.RefactoredVariable;

public class RefactoredFunctionsVisitor extends ASTVisitor {
	
	private int refactoredFuncs = 0;
	Hashtable<String, RefactoredFunction> refactoredFunctions;
	HashSet<String> refactoredFunctionsFound;
	
	public RefactoredFunctionsVisitor( Hashtable<String, RefactoredFunction> refactoredFunctions ) {
		this.refactoredFunctions = refactoredFunctions;
		refactoredFunctionsFound = new HashSet<String>();
	}

	public RefactoredFunctionsVisitor(  Hashtable<String, RefactoredFunction> refactoredFunctions, 
			boolean visitDocTags) {
		super(visitDocTags);
		this.refactoredFunctions = refactoredFunctions;
		refactoredFunctionsFound = new HashSet<String>();
	}
	
	public boolean visit( MethodInvocation functionCall ) {
		final String signature = functionCall.getName().toString() + ":" + functionCall.arguments().size();
		if ( refactoredFunctions.containsKey( signature ) ) {
			if ( refactoredFunctionsFound.contains( signature ) ) return true;
			
			refactoredFuncs++;
			refactoredFunctionsFound.add( signature );
		}
		return true;
	}
	
	public int getRefactoredFuncs() {
		return refactoredFuncs;
	}

}
