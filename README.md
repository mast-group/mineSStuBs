# SStuBs-mining
Hosts our tool for mining simple "stupid" bugs (SStuBs) that was used to mine the [ManySStuBs4J dataset](https://doi.org/10.5281/zenodo.3653444).

# Running the tool
A precompiled version of the tool is available in the file miner.jar taht contains all the required dependencies.
It can easily be run via the following command:
```
java -jar miner.jar PROJECTS_DIR DATASET_SAVE_DIR
```
PROJECTS_DIR must point to directory containing the Java repositories for mining.
DATASET_SAVE_DIR must point to the directory in which the dataset will be saved.

# Installation via Maven
The tool can also easily be installed and built from source via maven.\
Maven will download all the required dependencies.\
Many thanks to Martin Monperrus for his help with this.\
**Important: A Maven version >= 3.2.3 is required since 15 January 2020 and onwards**\
Maven will download the dependencies and build the project with the command:
```
mvn compile
```
To run the tool you can use the following command:
```
mvn exec:java -Dexec.mainClass=uk.ac.ed.inf.mpatsis.sstubs.mining.SStuBsMiner -Dexec.args="$PROJECTS_DIR DATASET_SAVE_DIR"
```
PROJECTS_DIR must point to directory containing the Java repositories for mining.
DATASET_SAVE_DIR must point to the directory in which the dataset will be saved.


# About the Dataset
The ManySStuBs4J corpus contains simple statement bugs mined from open-source Java projects hosted in GitHub.\
There are two variations of the dataset. One mined from the 100 Java Maven Projects and one mined from the top 1000 Java Projects.\
A project's popularity is determined by computing the sum of z-scores of its forks and watchers.\
We kept only bug commits that contain only single statement changes and ignore stylistic differences such as spaces or empty as well as differences in comments.\
We also attempted to spot refactorings such as variable, function, and class renamings, function argument renamings or changing the number of arguments in a function.\
The commits are classified as bug fixes or not by checking if the commit message contains any of a set of predetermined keywords such as bug, fix, fault etc.\
We evaluated the accuracy of this method on a random sample of 100 commits that contained SStuBs from the smaller version of the dataset and found it to achieve a satisfactory 94% accuracy.\
This method has also been used before to extract bug commits (Ray et al., 2015; Tufano et al., 2018) where it achieved an accuracy of 96% and 97.6% respectively.\
\
The bugs are stored in a JSON file (each version of the dataset has each own instance of this file).\
Any bugs that fit one of 16 patterns are also annotated by which pattern(s) they fit in a separate JSON file (each version of the dataset has each own instance of this file).\
We refer to bugs that fit any of the 16 patterns as simple stupid bugs (SStuBs).


## Corpus Statistics
Projects | Bug Commits | Buggy Statements | Bug Statements per Commit | SStuBs
---------|-------------|------------------|---------------------------|-------------------------------------------
100 Java Maven  |	  12598		   | 25539	|	          2.03    |  		7824
100 Java	|  	  86771   	   | 153652	|	          1.77     |  		51537


## SStuB Statistics
Pattern Name	|	Instances|	Instances Large     
----------------|----------------|-----------------------
| Change Idenfier Used  	|   3265	|      22668      	
| Change Numeric Literal	|   1137   	|      5447       	
| Change Boolean Literal	|   169	  	|      1842       	
| Change Modifier       	|   1852   	|      5010       	
| Wrong Function Name   	|   1486   	|      10179      	
| Same Function More Args	|   758   	|      5100       	
| Same Function Less Args	|   179   	|      1588       	
| Same Function Change Caller	|   187   	|      1504       	
| Same Function Swap Args	|   127   	|      612       	
| Change Binary Operator	|   275   	|      2241       	
| Change Unary Operator		|   170   	|      1016       	
| Change Operand        	|   120   	|      807       	
| Less Specific If      	|   215   	|      2813       	
| More Specific If      	|   175   	|      2381       	
| Missing Throws Exception	|   68   	|      206       	
| Delete Throws Exception	|   48   	|      508       	


## Use
The Corpus can be downloaded via the [Edinburgh Datashare](https://doi.org/10.7488/ds/2528).\
The ManySStuBs4J Corpus is an automatically mined collection of Java bugs at large-scale.\
We note that the automatic extraction could potentially insert some noise. \
However, the amount of inserted noise is deemed to be almost negligible (see about).\
We also note that the code of the Java projects is not ours but is open-source. \
Please respect the license of each project.

The corpus was collected for the work related to:

@inproceedings{ManySStuBs4JCorpus2019,\
	author={Karampatsis, Rafael-Michael and Sutton, Charles},\
	title={{How Often Do Single-Statement Bugs Occur? The ManySStuBs4J Dataset}},\
	booktitle={},\
	year={2019},\
	pages={},\
	organization={}\
}

## Files
100 Java Maven Project Bugs				bugs.json\
1000 Java Project Bugs					bugsLarge.json\
100 Java Maven Project SStuBs				sstubs.json\
1000 Java Project SStuBs				sstubsLarge.json\
\
Due to a bug zenodo returns an error when uploading json files.\
The .json suffix can be restored by simply renaming the files (e.g. bugs -> bugs.json).


## JSON Fields
Each SStuB entry in the JSON files contains the following fields:

"bugType"		:	The bug type (16 possible values).\
"commitSHA1"		:	The hash of the commit fixing the bug.  \
"fixCommitParentSHA1"	:	The hash of the last commit containing the bug.\
"commitFile"		:	Path of the fixed file.\
"patch"  		:	The diff of the buggy and fixed file containing all the changes applied by the fix commit.\
"projectName"		:	The concatenated repo owner and repo name separated by a '.'.\
"bugLineNum"		:	The line in which the bug exists in the buggy version of the file.\
"bugNodeStartChar"	:	The character index (i.e., the number of characters in the java file that must be read before encountering the first one of the AST node) at which the affected ASTNode starts in the buggy version of the file. \
"bugNodeLength"		:	The length of the affected ASTNode in the buggy version of the file.\
"fixLineNum"		:	The line in which the bug was fixed in the fixed version of the file.\
"fixNodeStartChar"	:	The character index (i.e., the number of characters in the java file that must be read before encountering the first one of the AST node) at which the affected ASTNode starts in the fixed version of the file.\
"fixNodeLength"		:	The length of the affected ASTNode in the fixed version of the file.\
"before"		:	The affected AST's tree (sometimes subtree  e.g. Change Numeric Literal) text before the fix.\
"after"			:	The affected AST's tree (sometimes subtree  e.g. Change Numeric Literal) text after the fix. \

The "before", "after", "patch" fields help humans to understand the change.\
The "bugLineNum", "bugNodeStartChar", "bugNodeLength", "fixLineNum", "fixNodeStartChar", and "fixNodeLength" allow pinpointing of the AST nodes and lines that contained the bug and their equivalent ones in the  fixed version of the file.\

Similarly the bugs in bugs.json contain the above fields except bugType.\
All bugs appearing in sstubs.json have also an entry in bugs.json.



## Examples for Each SStuB Pattern
