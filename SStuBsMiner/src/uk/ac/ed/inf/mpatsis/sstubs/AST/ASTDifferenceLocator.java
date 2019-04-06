package uk.ac.ed.inf.mpatsis.sstubs.AST;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.internal.preferences.ImmutableMap;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

//import edu.stanford.nlp.io.EncodingPrintWriter.out;

public class ASTDifferenceLocator {

	public ASTDifferenceLocator() {
		// TODO Auto-generated constructor stub
	}
	

	@SuppressWarnings("unchecked")
	/**
	 * 
	 * @param left
	 * @param right
	 * @return
	 */
	public static boolean equals( ASTNode left, ASTNode right ) {
		// if both are null, they are equal, but if only one, they aren't
		if (left == null && right == null) {
			return true;
		} else if (left == null || right == null) {
			return false;
		}
		
		if ( left.getNodeType() == ASTNode.NULL_LITERAL && right.getNodeType() == ASTNode.NULL_LITERAL  )
			return true;
		
		// if node types are the same we can assume that they will have the same properties
		if (left.getNodeType() != right.getNodeType()) {
			return false;
		}
		
		List<StructuralPropertyDescriptor> props = left.structuralPropertiesForType();
		for (StructuralPropertyDescriptor property : props) {
			Object leftVal = left.getStructuralProperty(property);
			Object rightVal = right.getStructuralProperty(property);
			if (leftVal == null && rightVal == null) {
				continue;
			} else if (leftVal == null || rightVal == null) {
				return false;
			}
			
			if (property.isSimpleProperty()) {
				// check for simple properties (primitive types, Strings, ...)
				// with normal equality
				if (!leftVal.equals(rightVal)) {
					return false;
				}
			} else if (property.isChildProperty()) {
				// recursively call this function on child nodes
				if (!equals((ASTNode) leftVal, (ASTNode) rightVal)) {
					return false;
				}
			} else if (property.isChildListProperty()) {
				Iterator<ASTNode> leftValIt = ((Iterable<ASTNode>) leftVal)
						.iterator();
				Iterator<ASTNode> rightValIt = ((Iterable<ASTNode>) rightVal)
						.iterator();
				while (leftValIt.hasNext() && rightValIt.hasNext()) {
					// recursively call this function on child nodes
					if (!equals(leftValIt.next(), rightValIt.next())) {
						return false;
					}
				}
				// one of the value lists have additional elements
				if (leftValIt.hasNext() || rightValIt.hasNext()) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	
	/**
	 * 
	 * @param left
	 * @param right
	 * @return
	 * @throws NullPointerException
	 */
	public static SimpleImmutableEntry<ASTNode, ASTNode> getFirstDifferentNode2( ASTNode left, ASTNode right ) throws NullPointerException {
		// if any of the nodes is null throw a NullPointerException
		if (left == null || right == null) {
			throw new NullPointerException();
		}
		
		// if node types are different then the nodes are different.
		if (left.getNodeType() != right.getNodeType()) {
			return new SimpleImmutableEntry<ASTNode, ASTNode>( left, right );
		}
		List<StructuralPropertyDescriptor> props = left.structuralPropertiesForType();
		List<StructuralPropertyDescriptor> rightProps = right.structuralPropertiesForType();
		if ( props.size() != rightProps.size() ) 
			return new SimpleImmutableEntry<ASTNode, ASTNode>( left, right );
		for ( StructuralPropertyDescriptor property : props ) {
			Object leftVal = left.getStructuralProperty( property );
			Object rightVal = right.getStructuralProperty( property );
			
			if ( leftVal == null && rightVal == null ) {
				continue;
			}
			if ( leftVal == null || rightVal == null ) {
				return new SimpleImmutableEntry<ASTNode, ASTNode>( left, right );
			}
			if ( property.isSimpleProperty() ) {
				// check for simple properties (primitive types, Strings, ...)
				// with normal equality
				if ( !leftVal.equals( rightVal ) ) {
					if ( left.getNodeType() == 42 ) { // SIMPLE_NAME
						return new SimpleImmutableEntry<ASTNode, ASTNode>( 
								left.getParent(), right.getParent() );
					}
					else return new SimpleImmutableEntry<ASTNode, ASTNode>( left, right );
				}
			} else if ( property.isChildProperty() ) {
				// recursively call this function on child nodes
				SimpleImmutableEntry<ASTNode, ASTNode> childrenDifference = 
						getFirstDifferentNode( (ASTNode) leftVal, (ASTNode) rightVal );
				if ( childrenDifference != null ) return childrenDifference;
			} else if ( property.isChildListProperty() ) {
				Iterator<ASTNode> leftValIt = ((Iterable<ASTNode>) leftVal).iterator();
				Iterator<ASTNode> rightValIt = ((Iterable<ASTNode>) rightVal).iterator();
				SimpleImmutableEntry<ASTNode, ASTNode> firstChildrenDifference = null;
				int differences = 0;
				while ( leftValIt.hasNext() && rightValIt.hasNext() ) {
					// recursively call this function on child nodes
					SimpleImmutableEntry<ASTNode, ASTNode> childrenDifference = 
							getFirstDifferentNode( leftValIt.next(), rightValIt.next() );
					if ( childrenDifference != null ) {
						differences++;
						if ( firstChildrenDifference == null ) {
							firstChildrenDifference = childrenDifference;
						}
					}
				}
				
				// one of the value lists have additional elements
				if ( leftValIt.hasNext() || rightValIt.hasNext() ) {
					return new SimpleImmutableEntry<ASTNode, ASTNode>( left, right );
				}
				else if ( differences > 1 ) {
					return new SimpleImmutableEntry<ASTNode, ASTNode>( left, right );
				}
				else if ( firstChildrenDifference != null ) {
					return firstChildrenDifference;
				}
			}
		}
		return null;
	}
	
	
	/**
	 * 
	 * @param left
	 * @param right
	 * @return
	 * @throws NullPointerException
	 */
	public static SimpleImmutableEntry<ASTNode, ASTNode> getFirstDifferentNode( ASTNode left, ASTNode right ) throws NullPointerException {
		// if any of the nodes is null throw a NullPointerException
		if (left == null || right == null) {
			throw new NullPointerException();
		}
		
		// if node types are different then the nodes are different.
		if (left.getNodeType() != right.getNodeType()) {
			return new SimpleImmutableEntry<ASTNode, ASTNode>( left, right );
		}
		List<StructuralPropertyDescriptor> props = left.structuralPropertiesForType();
		List<StructuralPropertyDescriptor> rightProps = right.structuralPropertiesForType();
		if ( props.size() != rightProps.size() ) 
			return new SimpleImmutableEntry<ASTNode, ASTNode>( left, right );
		
		int propertyDiffs = 0;
		SimpleImmutableEntry<ASTNode, ASTNode> firstPropertyDifference = null;
		// Go through simple properties first
		for ( StructuralPropertyDescriptor property : props ) {
			Object leftVal = left.getStructuralProperty( property );
			Object rightVal = right.getStructuralProperty( property );
			
			if ( leftVal == null && rightVal == null ) {
				continue;
			}
			if ( leftVal == null || rightVal == null ) {
				return new SimpleImmutableEntry<ASTNode, ASTNode>( left, right );
			}
			if ( property.isSimpleProperty() ) {
				// check for simple properties (primitive types, Strings, ...)
				// with normal equality
				if ( !leftVal.equals( rightVal ) ) {
					if ( left.getNodeType() == 42 ) { // SIMPLE_NAME
						return new SimpleImmutableEntry<ASTNode, ASTNode>( left.getParent(), right.getParent() );
					}
					else return new SimpleImmutableEntry<ASTNode, ASTNode>( left, right );
				}
			}
		}
		// Now go through child properties and child list properties
		for ( StructuralPropertyDescriptor property : props ) {
			Object leftVal = left.getStructuralProperty( property );
			Object rightVal = right.getStructuralProperty( property );
			
			if ( leftVal == null && rightVal == null ) {
				continue;
			}
			if ( leftVal == null || rightVal == null ) {
				return new SimpleImmutableEntry<ASTNode, ASTNode>( left, right );
			}
			
			if ( property.isChildProperty() ) {
				// recursively call this function on child nodes
				SimpleImmutableEntry<ASTNode, ASTNode> childrenDifference = 
						getFirstDifferentNode( (ASTNode) leftVal, (ASTNode) rightVal );
				
				if ( childrenDifference != null ) {
					propertyDiffs++;
					if ( firstPropertyDifference == null )
						firstPropertyDifference = childrenDifference;
				}
			}
			else if ( property.isChildListProperty() ) {
				Iterator<ASTNode> leftValIt = ((Iterable<ASTNode>) leftVal).iterator();
				Iterator<ASTNode> rightValIt = ((Iterable<ASTNode>) rightVal).iterator();
				SimpleImmutableEntry<ASTNode, ASTNode> firstChildrenDifference = null;
				int differences = 0;
				while ( leftValIt.hasNext() && rightValIt.hasNext() ) {
					// recursively call this function on child nodes
					SimpleImmutableEntry<ASTNode, ASTNode> childrenDifference = 
							getFirstDifferentNode( leftValIt.next(), rightValIt.next() );
					
					if ( childrenDifference != null ) {
						differences++;
						if ( firstChildrenDifference == null ) {
							firstChildrenDifference = childrenDifference;
						}
					}
				}
				
				// one of the value lists have additional elements
				if ( leftValIt.hasNext() || rightValIt.hasNext() ) {
//					System.out.println("Apo has additional");
					return new SimpleImmutableEntry<ASTNode, ASTNode>( left, right );
				}
				else if ( differences > 1 ) {
					return new SimpleImmutableEntry<ASTNode, ASTNode>( left, right );
				}
				else if ( firstPropertyDifference != null ) {
					return firstPropertyDifference;
				}
				else if ( firstChildrenDifference != null ) {
					return firstChildrenDifference;
				}
			}
		}
		if ( propertyDiffs > 1 ) {
			return new SimpleImmutableEntry<ASTNode, ASTNode>( left, right );
		}
		else if ( firstPropertyDifference != null ) return firstPropertyDifference;
		return null;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isDifferentMethodSameArgs( MethodInvocation newNode, MethodInvocation oldNode ) {
		if ( equals( newNode.getExpression(), oldNode.getExpression() ) ) {
			if ( !equals( newNode.getName(), oldNode.getName() ) ) {
				List<ASTNode> newNodeArgs = newNode.arguments();
				List<ASTNode> oldNodeArgs = oldNode.arguments();
				if ( newNodeArgs.size() != oldNodeArgs.size() ) return false;
				for ( int i = 0; i < newNodeArgs.size(); i++ ) {
					if ( !equals( newNodeArgs.get(i), oldNodeArgs.get(i) ) ) 
						return false;
				}
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isSwapArguments( MethodInvocation newNode, MethodInvocation oldNode ) {
		if ( equals( newNode.getExpression(), oldNode.getExpression() ) &&
				equals( newNode.getName(), oldNode.getName() ) ) {
			
			List<ASTNode> newNodeArgs = newNode.arguments();
			List<ASTNode> oldNodeArgs = oldNode.arguments();
			if ( newNodeArgs.size() != oldNodeArgs.size() ) return false;
			
			ArrayList<Integer> leftDifferentPositions = new ArrayList<Integer>();
			for ( int i = 0; i < newNodeArgs.size(); i++ ) {
				if ( !equals( newNodeArgs.get(i), oldNodeArgs.get(i) ) )
						leftDifferentPositions.add(i);
			}
			if ( leftDifferentPositions.size() != 2 ) return false;
			
			// Try to match the first leftDifferentPosition
			int firstLeftPos = leftDifferentPositions.get(0);
			for ( int i = 0; i < oldNodeArgs.size(); i++ ) {
				if ( equals( oldNodeArgs.get(i), 
						newNodeArgs.get( firstLeftPos ) ) ) {
					// Matched first leftDifferentPosition.
					// Now check if the arguments were swapped.
					if ( equals( newNodeArgs.get(i), 
							oldNodeArgs.get(firstLeftPos) ) ) return true;
				}
			}
		}
		return false;
	}
	
	
	public static boolean isSwapArguments( ClassInstanceCreation newNode, ClassInstanceCreation oldNode ) {
		if ( equals( newNode.getType(), oldNode.getType() )  ) {
			List<ASTNode> newNodeArgs = newNode.arguments();
			List<ASTNode> oldNodeArgs = oldNode.arguments();
			if ( newNodeArgs.size() != oldNodeArgs.size() ) return false;
			ArrayList<Integer> leftDifferentPositions = new ArrayList<Integer>();
			for ( int i = 0; i < newNodeArgs.size(); i++ ) {
				if ( !equals( newNodeArgs.get(i), oldNodeArgs.get(i) ) ) 
					leftDifferentPositions.add(i);
			}
			if ( leftDifferentPositions.size() != 2 ) return false;
			
			// Try to match the first leftDifferentPosition
			int firstLeftPos = leftDifferentPositions.get(0);
			for ( int i = 0; i < oldNodeArgs.size(); i++ ) {
				if ( equals( oldNodeArgs.get(i), newNodeArgs.get( firstLeftPos ) ) ) {
					// Matched first leftDifferentPosition.
					// Now check if the arguments were swapped.
					if ( equals( newNodeArgs.get(i), oldNodeArgs.get(firstLeftPos) ) ) 
						return true;
				}
			}
		}
		return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isCallOverloadedMethodDeletedArgs( MethodInvocation newNode, MethodInvocation oldNode ) {
		if ( equals( newNode.getExpression(), oldNode.getExpression() ) ) {
			if ( equals( newNode.getName(), oldNode.getName() ) ) {
				List<ASTNode> newNodeArgs = newNode.arguments();
				List<ASTNode> oldNodeArgs = oldNode.arguments();
				if ( newNodeArgs.size() >= oldNodeArgs.size() ) return false;

				for ( int i = 0; i < newNodeArgs.size(); i++ ) {
					boolean matchedArg = false;
					for ( int j = 0; j < oldNodeArgs.size(); j++  ) {
						if ( equals( newNodeArgs.get(i), oldNodeArgs.get(j) ) ) 
							matchedArg = true;
					}
					if ( !matchedArg ) return false;
				}
				return true;
			}
		}
		return false;
	}
	
	
	public static boolean isCallOverloadedMethodMoreArgs( MethodInvocation newNode, MethodInvocation oldNode ) {
		if ( equals( newNode.getExpression(), oldNode.getExpression() ) ) {
			if ( equals( newNode.getName(), oldNode.getName() ) ) {
				List<ASTNode> newNodeArgs = newNode.arguments();
				List<ASTNode> oldNodeArgs = oldNode.arguments();
				if ( newNodeArgs.size() <= oldNodeArgs.size() ) return false;

				for ( int i = 0; i < oldNodeArgs.size(); i++ ) {
					boolean matchedArg = false;
					for ( int j = 0; j < newNodeArgs.size(); j++  ) {
						if ( equals( oldNodeArgs.get(i), newNodeArgs.get(j) ) ) 
							matchedArg = true;
					}
					if ( !matchedArg ) return false;
				}
				return true;
			}
		}
		return false;
	}
	
	public static boolean isCallOverloadedMethodDeletedArgs( ClassInstanceCreation newNode, 
			ClassInstanceCreation oldNode ) {
		if ( equals( newNode.getType(), oldNode.getType() ) ) {
			List<ASTNode> newNodeArgs = newNode.arguments();
			List<ASTNode> oldNodeArgs = oldNode.arguments();
			if ( newNodeArgs.size() >= oldNodeArgs.size() ) return false;
			
			for ( int i = 0; i < newNodeArgs.size(); i++ ) {
				boolean matchedArg = false;
				for ( int j = 0; j < oldNodeArgs.size(); j++  ) {
					if ( equals( newNodeArgs.get(i), oldNodeArgs.get(j) ) ) 
						matchedArg = true;
				}
				if ( !matchedArg ) return false;
			}
			return true;
		}
		return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isCallOverloadedMethodMoreArgs( ClassInstanceCreation newNode, 
			ClassInstanceCreation oldNode ) {
		if ( equals( newNode.getType(), oldNode.getType() ) ) {
			List<ASTNode> newNodeArgs = newNode.arguments();
			List<ASTNode> oldNodeArgs = oldNode.arguments();
			if ( newNodeArgs.size() <= oldNodeArgs.size() ) return false;
			
			for ( int i = 0; i < oldNodeArgs.size(); i++ ) {
				boolean matchedArg = false;
				for ( int j = 0; j < newNodeArgs.size(); j++  ) {
					if ( equals( oldNodeArgs.get(i), newNodeArgs.get(j) ) ) 
						matchedArg = true;
				}
				if ( !matchedArg ) return false;
			}
			return true;
		}
		return false;
	}
	
	
	public static boolean isChangeOperator( InfixExpression newNode, InfixExpression oldNode ) {
		if ( equals( newNode.getLeftOperand(), oldNode.getLeftOperand() ) &&
				equals( newNode.getRightOperand(), oldNode.getRightOperand() ) &&
				newNode.hasExtendedOperands() == oldNode.hasExtendedOperands() &&
				! newNode.getOperator().equals( oldNode.getOperator() ) ) {
			if ( newNode.hasExtendedOperands() ) {
				List<Expression> newNodeExtendedOperands = newNode.extendedOperands();
				List<Expression> oldNodeExtendedOperands = oldNode.extendedOperands();
				if ( newNodeExtendedOperands.size() != oldNodeExtendedOperands.size() ) return false;
				for ( int i = 0; i < newNodeExtendedOperands.size(); i++ ) {
					if ( !equals( newNodeExtendedOperands.get(i), 
							oldNodeExtendedOperands.get(i) ) ) return false;
				}
				return true;
			}
			return true;
		}
		
		return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isChangeUnaryOperator( Expression newNode, Expression oldNode ) {
		if ( newNode instanceof PrefixExpression && !(oldNode instanceof PrefixExpression) ) {
			if ( equals( ((PrefixExpression) newNode).getOperand(), oldNode ) ) return true;
		}
		else if ( !(newNode instanceof PrefixExpression) && oldNode instanceof PrefixExpression ) {
			if ( equals( newNode, ((PrefixExpression) oldNode).getOperand() ) ) return true;
		}
		else if ( newNode instanceof PrefixExpression && oldNode instanceof PrefixExpression ) {
			if ( equals( ((PrefixExpression) newNode).getOperand(), ((PrefixExpression) oldNode).getOperand()) &&
					((PrefixExpression) newNode).getOperator() != ((PrefixExpression) oldNode).getOperator() )
				return true;
		}
		return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isChangeOperand( InfixExpression newNode, InfixExpression oldNode ) {
		boolean leftOperandDifferent = false;
		boolean rightOperandDifferent = false;
		
		if ( equals( newNode.getLeftOperand(), oldNode.getLeftOperand() ) ) leftOperandDifferent = true;
		if ( equals( newNode.getRightOperand(), oldNode.getRightOperand() ) ) rightOperandDifferent = true;
		if ( leftOperandDifferent == rightOperandDifferent ) return false;
		
		if ( newNode.hasExtendedOperands() == oldNode.hasExtendedOperands() &&
				newNode.getOperator().equals( oldNode.getOperator() ) ) {
			if ( newNode.hasExtendedOperands() ) {
				List<Expression> newNodeExtendedOperands = newNode.extendedOperands();
				List<Expression> oldNodeExtendedOperands = oldNode.extendedOperands();
				if ( newNodeExtendedOperands.size() != oldNodeExtendedOperands.size() ) return false;
				for ( int i = 0; i < newNodeExtendedOperands.size(); i++ ) {
					if ( !equals( newNodeExtendedOperands.get(i), 
							oldNodeExtendedOperands.get(i) ) ) return false;
				}
				return true;
			}
			return true;
		}
		
		return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isChangeModifier( VariableDeclarationStatement newNode, VariableDeclarationStatement oldNode ) {
		if ( newNode.getModifiers() == oldNode.getModifiers() ) return false;
		if ( !newNode.getType().toString().equals( oldNode.getType().toString() ) ) return false;
		
		List<VariableDeclarationFragment> newFragments = newNode.fragments();
		List<VariableDeclarationFragment> oldFragments = oldNode.fragments();
		if ( newFragments.size() != oldFragments.size() ) return false;
		for ( int i = 0; i < newFragments.size(); i++ ) {
			if ( !equals( newFragments.get(i), oldFragments.get(i) ) )
				return false;
		}
		return true;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isChangeModifier( FieldDeclaration newNode, FieldDeclaration oldNode ) {
		if ( newNode.getModifiers() == oldNode.getModifiers() ) return false;
		if ( !newNode.getType().toString().equals( oldNode.getType().toString() ) ) return false;
		
		List<VariableDeclarationFragment> newFragments = newNode.fragments();
		List<VariableDeclarationFragment> oldFragments = oldNode.fragments();
		if ( newFragments.size() != oldFragments.size() ) return false;
		for ( int i = 0; i < newFragments.size(); i++ ) {
			if ( !equals( newFragments.get(i), oldFragments.get(i) ) )
				return false;
		}
		return true;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isChangeModifier( MethodDeclaration newNode, MethodDeclaration oldNode ) {
		if ( newNode.getModifiers() == oldNode.getModifiers() ) return false;
		if ( !newNode.getName().toString().equals( oldNode.getName().toString() ) ) return false;
		if ( newNode.getReturnType2() != oldNode.getReturnType2() && 
				(newNode.getReturnType2() == null || oldNode.getReturnType2() == null ) ) return false;
		else if ( newNode.getReturnType2() != null && oldNode.getReturnType2() != null && 
				!newNode.getReturnType2().toString().equals( oldNode.getReturnType2().toString() ) ) return false;
		if ( newNode.isConstructor() != oldNode.isConstructor() ) return false;
		
		
		List<ASTNode> newParameters = newNode.parameters();
		List<ASTNode> oldParameters = oldNode.parameters();
		if ( newParameters.size() != oldParameters.size() ) return false;
		for ( int i = 0; i < newParameters.size(); i++ ) {
			if ( !equals( newParameters.get(i), oldParameters.get(i) ) )
				return false;
		}
		return true;
	}
	
	
	public static boolean isChangeModifier( TypeDeclaration newNode, TypeDeclaration oldNode ) {
		if ( newNode.getModifiers() == oldNode.getModifiers() ) return false;
		if ( !newNode.getName().toString().equals( oldNode.getName().toString() ) ) return false;
		if ( newNode.isInterface() != oldNode.isInterface() ) return false;
		if ( newNode.isLocalTypeDeclaration() != oldNode.isLocalTypeDeclaration() ) return false;
		if ( newNode.isMemberTypeDeclaration() != oldNode.isMemberTypeDeclaration() ) return false;
		if ( newNode.isPackageMemberTypeDeclaration() != oldNode.isPackageMemberTypeDeclaration() ) return false;

		return true;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isSwapBooleanLiteral( BooleanLiteral newNode, BooleanLiteral oldNode ) {
		if ( newNode.booleanValue() != oldNode.booleanValue() )	return true;
		else return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isSwapBooleanLiteral( MethodInvocation newNode, MethodInvocation oldNode ) {
		if ( equals( newNode.getExpression(), oldNode.getExpression() ) ) {
			if ( equals( newNode.getName(), oldNode.getName() ) ) {
				List<ASTNode> newNodeArgs = newNode.arguments();
				List<ASTNode> oldNodeArgs = oldNode.arguments();
				if ( newNodeArgs.size() != oldNodeArgs.size() ) return false;
				boolean foundDifference = false;
				boolean foundSwapped = false;
				for ( int i = 0; i < newNodeArgs.size(); i++ ) {
					if ( !equals( newNodeArgs.get(i), oldNodeArgs.get(i) ) ) {
						if ( foundDifference ) return false;
						foundDifference = true;
						if ( newNodeArgs.get(i) instanceof BooleanLiteral && 
								oldNodeArgs.get(i) instanceof BooleanLiteral ) {
							if ( ((BooleanLiteral) newNodeArgs.get(i)).booleanValue() != 
									((BooleanLiteral) newNodeArgs.get(i)).booleanValue() ) foundSwapped = true;
						}
					}
				}
				if ( foundSwapped ) return true;
			}
		}
		return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isSwapBooleanLiteral( ClassInstanceCreation newNode, ClassInstanceCreation oldNode ) {
		if ( equals( newNode.getExpression(), oldNode.getExpression() ) ) {
			if ( equals( newNode.getType(), oldNode.getType() ) ) {
				List<ASTNode> newNodeArgs = newNode.arguments();
				List<ASTNode> oldNodeArgs = oldNode.arguments();
				if ( newNodeArgs.size() != oldNodeArgs.size() ) return false;
				boolean foundDifference = false;
				boolean foundSwapped = false;
				for ( int i = 0; i < newNodeArgs.size(); i++ ) {
					if ( !equals( newNodeArgs.get(i), oldNodeArgs.get(i) ) ) {
						if ( foundDifference ) return false;
						foundDifference = true;
						if ( newNodeArgs.get(i) instanceof BooleanLiteral && 
								oldNodeArgs.get(i) instanceof BooleanLiteral ) {
							if ( ((BooleanLiteral) newNodeArgs.get(i)).booleanValue() != 
									((BooleanLiteral) newNodeArgs.get(i)).booleanValue() ) foundSwapped = true;
						}
					}
				}
				if ( foundSwapped ) return true;
			}
		}
		return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isChangeCallerInFunctionCall( MethodInvocation newNode, MethodInvocation oldNode ) {
		if ( !equals( newNode.getName(), oldNode.getName() ) ) return false;
		else {
			List<ASTNode> newNodeArgs = newNode.arguments();
			List<ASTNode> oldNodeArgs = oldNode.arguments();
			if ( newNodeArgs.size() != oldNodeArgs.size() ) return false;
			for ( int i = 0; i < newNodeArgs.size(); i++ ) {
				if ( !equals( newNodeArgs.get(i), oldNodeArgs.get(i) ) ) 
					return false;
			}
		}
		if ( equals( newNode.getExpression(), oldNode.getExpression() ) ) return false;
		if ( newNode.getExpression() == null || oldNode.getExpression() == null ) return false;
		
		if ( !newNode.getExpression().toString().contains(".") && 
				!oldNode.getExpression().toString().contains(".") ) {
			if ( !newNode.getExpression().equals( oldNode.getExpression() ) ) return true;
		}
		
		return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isAddThrowsException( MethodDeclaration newNode, MethodDeclaration oldNode ) {
		if ( newNode.getModifiers() != oldNode.getModifiers() ) return false;
		if ( !newNode.getName().toString().equals( oldNode.getName().toString() ) ) return false;
		if ( newNode.getReturnType2() != oldNode.getReturnType2() && 
				(newNode.getReturnType2() == null || oldNode.getReturnType2() == null ) ) return false;
		else if ( newNode.getReturnType2() != null && oldNode.getReturnType2() != null && 
				!newNode.getReturnType2().toString().equals( oldNode.getReturnType2().toString() ) ) return false;
		if ( newNode.isConstructor() != oldNode.isConstructor() ) return false;
		
		
		List<ASTNode> newParameters = newNode.parameters();
		List<ASTNode> oldParameters = oldNode.parameters();
		if ( newParameters.size() != oldParameters.size() ) return false;
		for ( int i = 0; i < newParameters.size(); i++ ) {
			if ( !equals( newParameters.get(i), oldParameters.get(i) ) )
				return false;
		}
		if ( newNode.thrownExceptionTypes().size() > 0 && oldNode.thrownExceptionTypes().size() == 0 ) return true;
		
		return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isDeleteThrowsException( MethodDeclaration newNode, MethodDeclaration oldNode ) {
		if ( newNode.getModifiers() != oldNode.getModifiers() ) return false;
		if ( !newNode.getName().toString().equals( oldNode.getName().toString() ) ) return false;
		if ( newNode.getReturnType2() != oldNode.getReturnType2() && 
				(newNode.getReturnType2() == null || oldNode.getReturnType2() == null ) ) return false;
		else if ( newNode.getReturnType2() != null && oldNode.getReturnType2() != null && 
				!newNode.getReturnType2().toString().equals( oldNode.getReturnType2().toString() ) ) return false;
		if ( newNode.isConstructor() != oldNode.isConstructor() ) return false;
		
		
		List<ASTNode> newParameters = newNode.parameters();
		List<ASTNode> oldParameters = oldNode.parameters();
		if ( newParameters.size() != oldParameters.size() ) return false;
		for ( int i = 0; i < newParameters.size(); i++ ) {
			if ( !equals( newParameters.get(i), oldParameters.get(i) ) )
				return false;
		}
		if ( oldNode.thrownExceptionTypes().size() > 0 && newNode.thrownExceptionTypes().size() == 0 ) return true;
		
		return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isModifiedThrownExceptions( MethodDeclaration newNode, MethodDeclaration oldNode ) {
		if ( newNode.getModifiers() != oldNode.getModifiers() ) return false;
		if ( !newNode.getName().toString().equals( oldNode.getName().toString() ) ) return false;
		if ( newNode.getReturnType2() != oldNode.getReturnType2() && 
				(newNode.getReturnType2() == null || oldNode.getReturnType2() == null ) ) return false;
		else if ( newNode.getReturnType2() != null && oldNode.getReturnType2() != null && 
				!newNode.getReturnType2().toString().equals( oldNode.getReturnType2().toString() ) ) return false;
		if ( newNode.isConstructor() != oldNode.isConstructor() ) return false;
		
		
		List<ASTNode> newParameters = newNode.parameters();
		List<ASTNode> oldParameters = oldNode.parameters();
		if ( newParameters.size() != oldParameters.size() ) return false;
		for ( int i = 0; i < newParameters.size(); i++ ) {
			if ( !equals( newParameters.get(i), oldParameters.get(i) ) )
				return false;
		}
		if ( oldNode.thrownExceptionTypes().size() != newNode.thrownExceptionTypes().size() ) return true;
		
		return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isChangeIdentifier( ASTNode newNode, ASTNode oldNode ) {
		boolean foundNameDifference = false;
		
		int newNodeType = newNode.getNodeType();
		int oldNodeType = oldNode.getNodeType();
		if (newNodeType != oldNodeType) {
			return false;
		}
		
		// Ignore MethodDeclaration, ClassDeclaration, VariableDeclaration
		if ( newNodeType == ASTNode.METHOD_DECLARATION || oldNodeType == ASTNode.METHOD_DECLARATION || 
				newNodeType == ASTNode.TYPE_DECLARATION || oldNodeType == ASTNode.TYPE_DECLARATION || 
				newNodeType == ASTNode.TYPE_DECLARATION_STATEMENT || 
				oldNodeType == ASTNode.TYPE_DECLARATION_STATEMENT || 
				newNodeType == ASTNode.VARIABLE_DECLARATION_STATEMENT || 
				oldNodeType == ASTNode.VARIABLE_DECLARATION_STATEMENT || 
				newNodeType == ASTNode.VARIABLE_DECLARATION_FRAGMENT || 
				oldNodeType == ASTNode.VARIABLE_DECLARATION_FRAGMENT ||
				newNodeType == ASTNode.VARIABLE_DECLARATION_EXPRESSION || 
				oldNodeType == ASTNode.VARIABLE_DECLARATION_EXPRESSION ) {
			return false;
		}
		
		List<StructuralPropertyDescriptor> props = newNode.structuralPropertiesForType();
		List<StructuralPropertyDescriptor> rightProps = oldNode.structuralPropertiesForType();
		if ( props.size() != rightProps.size() ) 
			return false;
		int diffs = 0;
		
		for ( StructuralPropertyDescriptor property : props ) {
			Object leftVal = newNode.getStructuralProperty( property );
			Object rightVal = oldNode.getStructuralProperty( property );
			
			if ( leftVal == null && rightVal == null ) {
				continue;
			}
			if ( leftVal == null || rightVal == null ) {
				return false;
			}
			
			if ( property.isChildProperty() ) {
				SimpleImmutableEntry<ASTNode, ASTNode> childrenDifference = 
						getFirstDifferentNode( (ASTNode) leftVal, (ASTNode) rightVal );
				if ( childrenDifference != null ) {
					diffs++;
					if ( ((ASTNode) leftVal).getNodeType() == ((ASTNode) rightVal).getNodeType() && 
							((ASTNode) leftVal).getNodeType() == ASTNode.SIMPLE_NAME ) {
						if ( !((SimpleName) leftVal).getIdentifier().equals( ((SimpleName) rightVal).getIdentifier() ) )
							foundNameDifference = true;
					}
				}
			}
			else if ( property.isChildListProperty() ) {
				Iterator<ASTNode> leftValIt = ((Iterable<ASTNode>) leftVal).iterator();
				Iterator<ASTNode> rightValIt = ((Iterable<ASTNode>) rightVal).iterator();
				SimpleImmutableEntry<ASTNode, ASTNode> firstChildrenDifference = null;
				int differences = 0;
				while ( leftValIt.hasNext() && rightValIt.hasNext() ) {
					ASTNode l = leftValIt.next();
					ASTNode r = rightValIt.next();
					// recursively call this function on child nodes
					SimpleImmutableEntry<ASTNode, ASTNode> childrenDifference = 
							getFirstDifferentNode( l, r );
					if ( childrenDifference != null ) {
						diffs++;
						if ( l.getNodeType() == r.getNodeType() && l.getNodeType() == ASTNode.SIMPLE_NAME ) {
							if ( !((SimpleName) l).getIdentifier().equals( ((SimpleName) r).getIdentifier() ) )
								foundNameDifference = true;
						}
					}
				}
			}
		}
//		System.out.println( diffs );
		if ( diffs == 1 ) return foundNameDifference;
		return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isChangeNumeral( NumberLiteral newNode, NumberLiteral oldNode ) {
		return !newNode.getToken().equals( oldNode.getToken() );
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isMoreSpecificIf( InfixExpression newNode, InfixExpression oldNode ) {
		if ( oldNode.getParent().getNodeType() == ASTNode.IF_STATEMENT &&
				newNode.getParent().getNodeType() == ASTNode.IF_STATEMENT ||
				oldNode.getParent().getNodeType() == ASTNode.WHILE_STATEMENT &&
				newNode.getParent().getNodeType() == ASTNode.WHILE_STATEMENT ) {
			
			if ( newNode.getOperator() == Operator.CONDITIONAL_AND && (oldNode.toString().equals( 
					newNode.getLeftOperand().toString() ) || 
					oldNode.toString().equals(newNode.getRightOperand().toString()) ) ) {
				return true;
			}
			else if ( isConditionalAndOperand( newNode, oldNode ) ) {
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isLessSpecificIf( InfixExpression newNode, InfixExpression oldNode ) {
		if ( oldNode.getParent().getNodeType() == ASTNode.IF_STATEMENT &&
				newNode.getParent().getNodeType() == ASTNode.IF_STATEMENT ||
				oldNode.getParent().getNodeType() == ASTNode.WHILE_STATEMENT &&
				newNode.getParent().getNodeType() == ASTNode.WHILE_STATEMENT ) {
			
			if ( newNode.getOperator() == Operator.CONDITIONAL_OR && (oldNode.toString().equals( 
					newNode.getLeftOperand().toString() ) || 
					oldNode.toString().equals(newNode.getRightOperand().toString()) ) ) {
				return true;
			}
			else if ( isConditionalOrOperand( newNode, oldNode ) ) {
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isMoreSpecificIf( InfixExpression newNode, Expression oldNode ) {
		if ( oldNode.getParent().getNodeType() == ASTNode.IF_STATEMENT &&
				newNode.getParent().getNodeType() == ASTNode.IF_STATEMENT ||
				oldNode.getParent().getNodeType() == ASTNode.WHILE_STATEMENT &&
				newNode.getParent().getNodeType() == ASTNode.WHILE_STATEMENT ) {
			
			if ( newNode.getOperator() == Operator.CONDITIONAL_AND && (oldNode.toString().equals( 
					newNode.getLeftOperand().toString() ) || 
					oldNode.toString().equals(newNode.getRightOperand().toString()) ) ) {
				return true;
			}
			else if ( newNode.getOperator() == Operator.CONDITIONAL_AND ) {
				return isConditionalAndOperand( newNode, oldNode );
			}
		}
		return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isLessSpecificIf( InfixExpression newNode, Expression oldNode ) {
		if ( oldNode.getParent().getNodeType() == ASTNode.IF_STATEMENT &&
				newNode.getParent().getNodeType() == ASTNode.IF_STATEMENT ||
				oldNode.getParent().getNodeType() == ASTNode.WHILE_STATEMENT &&
				newNode.getParent().getNodeType() == ASTNode.WHILE_STATEMENT ) {
			
			if ( newNode.getOperator() == Operator.CONDITIONAL_OR && (oldNode.toString().equals( 
					newNode.getLeftOperand().toString() ) || 
					oldNode.toString().equals(newNode.getRightOperand().toString()) ) ) {
				return true;
			}
			else if ( newNode.getOperator() == Operator.CONDITIONAL_OR ) {
				return isConditionalOrOperand( newNode, oldNode );
			}
		}
		return false;
	}
	
	
	/**
	 * Checks if the newNode contains all the oldNode (all its subexpression) plus conditional some other expression.
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isConditionalAndOperand( InfixExpression newNode, InfixExpression oldNode ) {
		if ( newNode.getOperator() != Operator.CONDITIONAL_AND || 
				( oldNode.getOperator() != Operator.CONDITIONAL_AND && oldNode.extendedOperands().size() > 0 ) ) return false;
		
		if ( oldNode.getOperator() != Operator.CONDITIONAL_AND ) {
			if ( equals(oldNode, newNode.getLeftOperand() ) || ( newNode.getLeftOperand() instanceof InfixExpression && 
					isConditionalAndOperand( (InfixExpression) newNode.getLeftOperand(), oldNode ) ) ) return true;
			if ( equals(oldNode, newNode.getRightOperand() ) || ( newNode.getRightOperand() instanceof InfixExpression && 
					isConditionalAndOperand( (InfixExpression) newNode.getRightOperand(), oldNode ) ) ) return true;
			for ( Expression extendedOperand : (List<Expression>) newNode.extendedOperands() ) {
				if ( equals(oldNode, extendedOperand) || ( extendedOperand instanceof InfixExpression && 
						isConditionalAndOperand( (InfixExpression) extendedOperand, oldNode ) ) ) return true;
			}
			return false;
		}
		
		Expression oldLeftOperand = oldNode.getLeftOperand();
		boolean foundLeft = false;
		
		if ( equals(oldLeftOperand, newNode.getLeftOperand()) ) foundLeft = true;
		if ( equals(oldLeftOperand, newNode.getRightOperand()) ) foundLeft = true;
		for ( Expression extendedOperand : (List<Expression>) newNode.extendedOperands() ) {
			if ( equals(oldLeftOperand, extendedOperand) ) foundLeft = true;
		}
		if ( !foundLeft ) return false;
		
		Expression oldRightOperand = oldNode.getRightOperand();
		boolean foundRight = false;
		if ( equals(oldRightOperand, newNode.getLeftOperand()) ) foundRight = true;
		if ( equals(oldRightOperand, newNode.getRightOperand()) ) foundRight = true;
		for ( Expression extendedOperand : (List<Expression>) newNode.extendedOperands() ) {
			if ( equals(oldRightOperand, extendedOperand) ) foundRight = true;
		}
		if ( !foundRight ) return false;
		
		for ( Expression extendedOperand : (List<Expression>) oldNode.extendedOperands() ) {
			boolean foundExt = false;
			if ( equals(extendedOperand, newNode.getLeftOperand()) ) foundExt = true;
			if ( equals(extendedOperand, newNode.getRightOperand()) ) foundExt = true;
			for ( Expression extendedOldOperand : (List<Expression>) newNode.extendedOperands() ) {
				if ( equals(extendedOperand, extendedOldOperand) ) foundExt = true;
			}
			if ( !foundExt ) return false;
		}
		
//		if ( newNode.getOperator() == Operator.CONDITIONAL_AND ) {
//			if ( equals( newNode.getLeftOperand(), oldNode ) ) return true;
//			if ( equals( newNode.getRightOperand(), oldNode ) ) return true;
//		}
		return true;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isConditionalAndOperand(  InfixExpression newNode, Expression oldNode  ) {
		if ( newNode.getOperator() != Operator.CONDITIONAL_AND ) return false;
		if ( equals( newNode.getLeftOperand(), oldNode ) ) return true;
		if ( equals( newNode.getRightOperand(), oldNode ) ) return true;
		for ( Expression extendedOperand : (List<Expression>) newNode.extendedOperands() ) {
			if ( equals( extendedOperand, oldNode ) ) return true;
		}
		
		if ( newNode.getLeftOperand() instanceof InfixExpression && 
				isConditionalAndOperand( (InfixExpression) newNode.getLeftOperand(), oldNode ) ) return true;
		if ( newNode.getRightOperand() instanceof InfixExpression && 
				isConditionalAndOperand( (InfixExpression) newNode.getRightOperand(), oldNode ) ) return true;
		for ( Expression extendedOperand : (List<Expression>) newNode.extendedOperands() ) {
			if ( extendedOperand instanceof InfixExpression && 
					isConditionalAndOperand( (InfixExpression) extendedOperand, oldNode ) ) return true;
		}
		
		return false;
	}
	
	
	/**
	 * Checks if the newNode contains all the oldNode (all its subexpression) plus conditional some other expression.
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isConditionalOrOperand( InfixExpression newNode, InfixExpression oldNode ) {
		if ( newNode.getOperator() != Operator.CONDITIONAL_OR || 
				( oldNode.getOperator() != Operator.CONDITIONAL_OR && oldNode.extendedOperands().size() > 0 ) ) return false;
		
		if ( oldNode.getOperator() != Operator.CONDITIONAL_OR ) {
			if ( equals(oldNode, newNode.getLeftOperand() ) || ( newNode.getLeftOperand() instanceof InfixExpression && 
					isConditionalOrOperand( (InfixExpression) newNode.getLeftOperand(), oldNode ) ) ) return true;
			if ( equals(oldNode, newNode.getRightOperand() ) || ( newNode.getRightOperand() instanceof InfixExpression && 
					isConditionalOrOperand( (InfixExpression) newNode.getRightOperand(), oldNode ) ) ) return true;
			for ( Expression extendedOperand : (List<Expression>) newNode.extendedOperands() ) {
				if ( equals(oldNode, extendedOperand) || ( extendedOperand instanceof InfixExpression && 
						isConditionalOrOperand( (InfixExpression) extendedOperand, oldNode ) ) ) return true;
			}
			return false;
		}
		
		Expression oldLeftOperand = oldNode.getLeftOperand();
		boolean foundLeft = false;
		
		if ( equals(oldLeftOperand, newNode.getLeftOperand()) ) foundLeft = true;
		if ( equals(oldLeftOperand, newNode.getRightOperand()) ) foundLeft = true;
		for ( Expression extendedOperand : (List<Expression>) newNode.extendedOperands() ) {
			if ( equals(oldLeftOperand, extendedOperand) ) foundLeft = true;
		}
		if ( !foundLeft ) return false;
		
		Expression oldRightOperand = oldNode.getRightOperand();
		boolean foundRight = false;
		if ( equals(oldRightOperand, newNode.getLeftOperand()) ) foundRight = true;
		if ( equals(oldRightOperand, newNode.getRightOperand()) ) foundRight = true;
		for ( Expression extendedOperand : (List<Expression>) newNode.extendedOperands() ) {
			if ( equals(oldRightOperand, extendedOperand) ) foundRight = true;
		}
		if ( !foundRight ) return false;
		
		for ( Expression extendedOperand : (List<Expression>) oldNode.extendedOperands() ) {
			boolean foundExt = false;
			if ( equals(extendedOperand, newNode.getLeftOperand()) ) foundExt = true;
			if ( equals(extendedOperand, newNode.getRightOperand()) ) foundExt = true;
			for ( Expression extendedOldOperand : (List<Expression>) newNode.extendedOperands() ) {
				if ( equals(extendedOperand, extendedOldOperand) ) foundExt = true;
			}
			if ( !foundExt ) return false;
		}
		
//		if ( newNode.getOperator() == Operator.CONDITIONAL_AND ) {
//			if ( equals( newNode.getLeftOperand(), oldNode ) ) return true;
//			if ( equals( newNode.getRightOperand(), oldNode ) ) return true;
//		}
		return true;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isConditionalOrOperand( InfixExpression newNode, Expression oldNode  ) {
		if ( newNode.getOperator() != Operator.CONDITIONAL_OR ) return false;
		if ( equals( newNode.getLeftOperand(), oldNode ) ) return true;
		if ( equals( newNode.getRightOperand(), oldNode ) ) return true;
		for ( Expression extendedOperand : (List<Expression>) newNode.extendedOperands() ) {
			if ( equals( extendedOperand, oldNode ) ) return true;
		}
		
		if ( newNode.getLeftOperand() instanceof InfixExpression && 
				isConditionalOrOperand( (InfixExpression) newNode.getLeftOperand(), oldNode ) ) return true;
		if ( newNode.getRightOperand() instanceof InfixExpression && 
				isConditionalOrOperand( (InfixExpression) newNode.getRightOperand(), oldNode ) ) return true;
		for ( Expression extendedOperand : (List<Expression>) newNode.extendedOperands() ) {
			if ( extendedOperand instanceof InfixExpression && 
					isConditionalOrOperand( (InfixExpression) extendedOperand, oldNode ) ) return true;
		}
		
		return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isChangeField( FieldAccess newNode, FieldAccess oldNode ) {
		return newNode.toString().equals( oldNode.toString() );
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isChangeField( Assignment newNode, Assignment oldNode ) {
		if ( !equals( newNode.getLeftHandSide(), oldNode.getLeftHandSide() ) ) {
			if ( !equals(newNode.getRightHandSide(), oldNode.getRightHandSide() ) ) {
				return false;
			}
			return true;
		}
		else if ( !equals( newNode.getRightHandSide(), oldNode.getRightHandSide() ) ) {
			return true;
		}
		return false;
	}
	
	
	/**
	 * 
	 * @param newNode
	 * @param oldNode
	 * @return
	 */
	public static boolean isNonMatchingParameter( MethodDeclaration newNode, MethodDeclaration oldNode ) {
		if ( newNode.parameters().size() != oldNode.parameters().size() ) return false;
		List<ASTNode> newParameters = newNode.parameters();
		List<ASTNode> oldParameters = oldNode.parameters();
		for ( int i = 0; i < newParameters.size(); i++ ) {
			if ( !equals( newParameters.get(i), oldParameters.get(i) ) && 
					((SingleVariableDeclaration) newParameters.get(i)).getType().toString() !=
					((SingleVariableDeclaration) oldParameters.get(i)).getType().toString()) return true;
		}
		return false;
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
