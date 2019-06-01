# SStuBs-mining
Hosts our tool for mining simple "stupid" bugs (SStuBs) that was used to mine the [ManySStuBs4J dataset](https://doi.org/10.7488/ds/2528).

# Running the tool
java -jar miner.jar PROJECTS_DIR DATASET_SAVE_DIR

# About the Dataset
=====
The ManySStuBs4J corpus contains simple statement bugs mined from open-source Java projects hosted in GitHub.
There are two variations of the dataset. One mined from the 100 Java Maven Projects and one mined from the top 1000 Java Projects.
A project's popularity is determined by computing the sum of z-scores of its forks and watchers.
We kept only bug commits that contain only single statement changes and ignore stylistic differences such as spaces or empty as well as differences in comments.
We also attempted to spot refactorings such as variable, function, and class renamings, function argument renamings or changing the number of arguments in a function.
The commits are classified as bug fixes or not by checking if the commit message contains any of a set of predetermined keywords such as bug, fix, fault etc.
We evaluated the accuracy of this method on a random sample of 100 commits that contained SStuBs from the smaller version of the dataset and found it to achieve a satisfactory 94% accuracy.
This method has also been used before to extract bug commits (Ray et al., 2015; Tufano et al., 2018) where it achieved an accuracy of 96% and 97.6% respectively.

The bugs are stored in a JSON file (each version of the dataset has each own instance of this file).
Any bugs that fit one of 16 patterns are also annotated by which pattern(s) they fit in a separate JSON file (each version of the dataset has each own instance of this file).
We refer to bugs that fit any of the 16 patterns as simple stupid bugs (SStuBs).

For more information on extracting the dataset and a detailed documentation of the software visit our GitHub repo: https://github.com/mast-group/SStuBs-mining


## Corpus Statistics
-----------------------------------------------------------------------------------------------------------------
|	Projects	Bug Commits	Buggy Statements	Bug Statements per Commit	SStuBs     	|
+---------------------------------------------------------------------------------------------------------------+
| 100 Java Maven  	  13000		    24412		          1.88      		7824		|
| 100 Java	   	  87000   	    5447		          1.77       		51537		|
+---------------------------------------------------------------------------------------------------------------+


##SStuB Statistics
-------------------------------------------------------------------------
|	Pattern Name		Instances	Instances Large     	|
+-----------------------------------------------------------------------+
| Change Idenfier Used  	   3290		      22773      	|
| Change Numeric Literal	   1178   	      5447       	|
| Change Boolean Literal	   166	  	      1841       	|
| Change Modifier       	   1028   	      5010       	|
| Wrong Function Name   	   1491   	      10179      	|
| Same Function More Args	   807   	      5100       	|
| Same Function Less Args	   185   	      1588       	|
| Same Function Change Caller	   196   	      1504       	|
| Same Function Swap Args	   131   	      612       	|
| Change Binary Operator	   327   	      2241       	|
| Change Unary Operator		   174   	      1016       	|
| Change Operand        	   127   	      807       	|
| Less Specific If      	   220   	      2813       	|
| More Specific If      	   203   	      2381       	|
| Missing Throws Exception	   69   	      206       	|
| Delete Throws Exception	   47   	      508       	|
+-----------------------------------------------------------------------+


##Use
The Corpus can be downloaded via the [Edinburgh Datashare](https://doi.org/10.7488/ds/2528).
The ManySStuBs4J Corpus is an automatically mined collection of Java bugs at large-scale.
We note that the automatic extraction could potentially insert some noise. 
However, the amount of inserted noise is deemed to be almost negligible (see about).
We also note that the code of the Java projects is not ours but is open-source. 
Please respect the license of each project.

The corpus was collected for the work related to:

@inproceedings{ManySStuBs4JCorpus2019,
	author={Karampatsis, Rafael-Michael and Sutton, Charles},
	title={{How Often Do Single-Statement Bugs Occur?\\ The ManySStuBs4J Dataset}},
	booktitle={},	
	year={2019},
	pages={},
	organization={}
}

##Files
100 Java Maven Project Bugs				bugs.json
1000 Java Project Bugs					bugsLarge.json
100 Java Maven Project SStuBs				sstubs.json
1000 Java Project SStuBs				sstubsLarge.json

All files can be loaded via any JSON library.


##JSON Fields
The SStuBs contain the following fields:

"bugType"	:	The bug type (16 possible values)
"commitSHA1"	:	The hash of the commit fixing the bug.
"commitFile"	:	Path of the fixed file.
"patch"  	:	The diff of the change.
"projectName"	:	The concatenated repo owner and repo name separated by a '.'.
"lineNum"	:	The line in which the bug exists.
"nodeStartChar"	:	The character position at which the affected ASTNode starts.
"before"	:	The affected AST node text before the fix. (This field does not appear in some SStuB types for which it is not useful, e.g. Change Numeric Literal)
"after"		:	The affected AST node text after the fix. (This field does not appear in some SStuB types for which it is not useful, e.g. Change Numeric Literal)

Similarly the bugs in bugs.json contain the above fields except bugType.
All bugs appearing in sstubs.json have also an entry in bugs.json.

