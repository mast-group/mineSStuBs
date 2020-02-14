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
The ManySStuBs4J corpus is a collection of simple fixes to Java bugs, designed for evaluating program repair techniques.\
We collect all bug-fixing changes using the SZZ heuristic, and then filter these to obtain a data set of small bug fix changes.\
These are single statement fixes, classified where possible into one of 16 syntactic templates which we call SStuBs.\
The dataset contains simple statement bugs mined from open-source Java projects hosted in GitHub.\
There are two variants of the dataset. One mined from the 100 Java Maven Projects and one mined from the top 1000 Java Projects.\
A project's popularity is determined by computing the sum of z-scores of its forks and watchers.\
We kept only bug commits that contain only single statement changes and ignore stylistic differences such as spaces or empty as well as differences in comments.\
Some single statement changes can be caused by refactorings, like changing a variable name rather than bug fixes.\
We attempted to detect and exclude refactorings such as variable, function, and class renamings, function argument renamings or changing the number of arguments in a function.\
The commits are classified as bug fixes or not by checking if the commit message contains any of a set of predetermined keywords such as bug, fix, fault etc.\
We evaluated the accuracy of this method on a random sample of 100 commits that contained SStuBs from the smaller version of the dataset and found it to achieve a satisfactory 94% accuracy.\
This method has also been used before to extract bug datasets (Ray et al., 2015; Tufano et al., 2018) where it achieved an accuracy of 96% and 97.6% respectively.\
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
The Corpus can be downloaded via [Zenodo](https://doi.org/10.5281/zenodo.3653444).\
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
The files sstubs.json and sstubsLarge.json contain the following fields:

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
"sourceBeforeFix"	:	The affected AST's tree (sometimes subtree  e.g. Change Numeric Literal) text before the fix.\
"sourceAfterFix"	:	The affected AST's tree (sometimes subtree  e.g. Change Numeric Literal) text after the fix. 

The "sourceBeforeFix", "sourceAfterFix", "patch" fields help humans to understand the change.\
The "sourceBeforeFix", "sourceAfterFix", "patch" fields are currently not available for the Missing Throws Exception and Delete Throws Exception patterns due to a bug.\
We have fixed this and we will provide an updated version.\
The "bugLineNum", "bugNodeStartChar", "bugNodeLength", "fixLineNum", "fixNodeStartChar", and "fixNodeLength" allow pinpointing of the AST nodes and lines that contained the bug and their equivalent ones in the  fixed version of the file.

Similarly the bugs in bugs.json contain the above fields except bugType.\
All bugs appearing in sstubs.json have also an entry in bugs.json.


# Examples for Each SStuB Pattern
A quick overview of each SStuB pattern follows along with an example from the dataset 
## Change Identifier Used
This pattern checks whether an identifier appearing in some expression in the statement was replaced with an other one.
It is easy for developers to by accident utilize a different
identifier than the intended one that has the same type.
Copy pasting code is a potential source of such errors.
Identifiers with similar names may further contribute to the occurrence of such errors.\
**Change Identifier Used** example patch:
```diff
diff --git a/common/src/main/java/com/google/auto/common/MoreTypes.java b/common/src/main/java/com/google/auto/common/MoreTypes.java
index d0f40a9..1319092 100644
--- a/common/src/main/java/com/google/auto/common/MoreTypes.java
+++ b/common/src/main/java/com/google/auto/common/MoreTypes.java
@@ -738,7 +738,7 @@
    * Returns a {@link WildcardType} if the {@link TypeMirror} represents a wildcard type or throws
    * an {@link IllegalArgumentException}.
    */
-  public static WildcardType asWildcard(WildcardType maybeWildcardType) {
+  public static WildcardType asWildcard(TypeMirror maybeWildcardType) {
     return maybeWildcardType.accept(WildcardTypeVisitor.INSTANCE, null);
   }
```

## Change Numeric Literal
This pattern Checks whether a numeric literal was replaced with another one. 
It is easy for developers to mix two numeric values in their program.\
**Change Numeric Literal** example patch:
```diff
diff --git a/components/camel-pulsar/src/test/java/org/apache/camel/component/pulsar/PulsarConcurrentConsumerInTest.java b/components/camel-pulsar/src/test/java/org/apache/camel/component/pulsar/PulsarConcurrentConsumerInTest.java
index dcd3a02..1d25f25 100644
--- a/components/camel-pulsar/src/test/java/org/apache/camel/component/pulsar/PulsarConcurrentConsumerInTest.java
+++ b/components/camel-pulsar/src/test/java/org/apache/camel/component/pulsar/PulsarConcurrentConsumerInTest.java
@@ -90,12 +90,12 @@
     }
 
     private PulsarClient concurrentPulsarClient() throws PulsarClientException {
-        return new ClientBuilderImpl().serviceUrl(getPulsarBrokerUrl()).ioThreads(2).listenerThreads(5).build();
+        return new ClientBuilderImpl().serviceUrl(getPulsarBrokerUrl()).ioThreads(5).listenerThreads(5).build();
     }
```

## Change Boolean Literal
This pattern checks whether a Boolean literal was replaced.
True is replaced with False and vice-versa. 
In many cases developers use the opposite Boolean value than the intended one.\
**Change Boolean Literal** example patch:
```diff
diff --git a/components/camel-sjms/src/main/java/org/apache/camel/component/sjms/jms/JmsObjectFactory.java b/components/camel-sjms/src/main/java/org/apache/camel/component/sjms/jms/JmsObjectFactory.java
index 3ed3a24..382ed68 100644
--- a/components/camel-sjms/src/main/java/org/apache/camel/component/sjms/jms/JmsObjectFactory.java
+++ b/components/camel-sjms/src/main/java/org/apache/camel/component/sjms/jms/JmsObjectFactory.java
@@ -88,7 +88,8 @@
             String messageSelector, 
             boolean topic, 
             String durableSubscriptionId) throws Exception {
-        return createMessageConsumer(session, destinationName, messageSelector, topic, durableSubscriptionId, true);
+        // noLocal is default false accordingly to JMS spec
+        return createMessageConsumer(session, destinationName, messageSelector, topic, durableSubscriptionId, false);
     }
```

## Change Modifier
This pattern checks whether a variable, function, or class was declared with the wrong modifiers. 
For example a developer can forget to declare one of the modifiers.\
**Change Modifier** example patch:
```diff
diff --git a/src/test/java/com/puppycrawl/tools/checkstyle/BaseCheckTestSupport.java b/src/test/java/com/puppycrawl/tools/checkstyle/BaseCheckTestSupport.java
index 67f89b8..c3b3ebf 100644
--- a/src/test/java/com/puppycrawl/tools/checkstyle/BaseCheckTestSupport.java
+++ b/src/test/java/com/puppycrawl/tools/checkstyle/BaseCheckTestSupport.java
@@ -100,7 +100,7 @@
                 + filename).getCanonicalPath();
     }
 
-    protected void verifyAst(String expectedTextPrintFileName, String actualJavaFileName)
+    protected static void verifyAst(String expectedTextPrintFileName, String actualJavaFileName)
             throws Exception {
         verifyAst(expectedTextPrintFileName, actualJavaFileName, false);
     }
```

## Wrong Function Name
This pattern checks whether the wrong function was called. 
Functions with similar names and the same signature are usual pitfall for developers.\
**Wrong Function Name** example patch:
```diff
diff --git a/modules/DesktopDataLaboratory/src/main/java/org/gephi/desktop/datalab/ConfigurationPanel.java b/modules/DesktopDataLaboratory/src/main/java/org/gephi/desktop/datalab/ConfigurationPanel.java
index f28c614..f295bd3 100644
--- a/modules/DesktopDataLaboratory/src/main/java/org/gephi/desktop/datalab/ConfigurationPanel.java
+++ b/modules/DesktopDataLaboratory/src/main/java/org/gephi/desktop/datalab/ConfigurationPanel.java
@@ -130,7 +130,7 @@
     }
 
     private boolean canChangeTimeRepresentation(GraphModel graphModel) {
-        if (graphModel.getGraph().getEdgeCount() > 0) {
+        if (graphModel.getGraph().getNodeCount() > 0) {
             return false;//Graph has to be empty
         }
```

## Same Function More Args
This pattern checks whether an overloaded version of the function with more arguments was called. 
Functions with multiple overload can often confuse developers.\
**Same Function More Args** example patch:
```diff
diff --git a/ee/src/main/java/org/jboss/as/ee/component/ComponentDescription.java b/ee/src/main/java/org/jboss/as/ee/component/ComponentDescription.java
index f9b99d2..76f83cc 100644
--- a/ee/src/main/java/org/jboss/as/ee/component/ComponentDescription.java
+++ b/ee/src/main/java/org/jboss/as/ee/component/ComponentDescription.java
@@ -543,7 +543,7 @@
                     configuration.getModuleName(),
                     configuration.getApplicationName()
             );
-            injectionConfiguration.getSource().getResourceValue(serviceBuilder, context, managedReferenceFactoryValue);
+            injectionConfiguration.getSource().getResourceValue(resolutionContext, serviceBuilder, context, managedReferenceFactoryValue);
         }
     }
 }
```

## Same Function Less Args
This pattern checks whether an overloaded version of the function with less arguments was called. 
For instance, a developer can forget to specify one of the arguments and not realize it if the code still compiles due to function overloading.\
**Same Function Less Args** example patch:
```diff
diff --git a/src/main/java/com/zaxxer/hikari/pool/HikariPool.java b/src/main/java/com/zaxxer/hikari/pool/HikariPool.java
index 19b49e7..3a87b7f 100644
--- a/src/main/java/com/zaxxer/hikari/pool/HikariPool.java
+++ b/src/main/java/com/zaxxer/hikari/pool/HikariPool.java
@@ -167,7 +167,7 @@
             final long now = clockSource.currentTime();
             if (poolEntry.evict || (clockSource.elapsedMillis(poolEntry.lastAccessed, now) > ALIVE_BYPASS_WINDOW_MS && !isConnectionAlive(poolEntry.connection))) {
                closeConnection(poolEntry, \"(connection evicted or dead)\"); // Throw away the dead connection and try again
-               timeout = hardTimeout - clockSource.elapsedMillis(startTime, now);
+               timeout = hardTimeout - clockSource.elapsedMillis(startTime);
             }
             else {
                metricsTracker.recordBorrowStats(poolEntry, startTime);
```

## Same Function Change Caller
This pattern checks whether in a function call expression the caller object for it was replaced with another one.
When there are multiple variables with the same type a developer can accidentally perform an operation.
Copy pasting code is a potential source of such errors.
Variables with similar names can also further contribute to the occurrence of such errors.\
**Same Function Change Caller** example patch:
```diff
diff --git a/metrics-servlet/src/test/java/com/yammer/metrics/reporting/tests/AdminServletTest.java b/metrics-servlet/src/test/java/com/yammer/metrics/reporting/tests/AdminServletTest.java
index e9d1b4e..4f8601e
--- a/metrics-servlet/src/test/java/com/yammer/metrics/reporting/tests/AdminServletTest.java
+++ b/metrics-servlet/src/test/java/com/yammer/metrics/reporting/tests/AdminServletTest.java
@@ -42,7 +42,7 @@
 
     @Before
     public void setUp() throws Exception {
-        when(context.getContextPath()).thenReturn(\"/context\");
+        when(request.getContextPath()).thenReturn(\"/context\");
 
         when(config.getServletContext()).thenReturn(context);
```

## Same Function Swap Args
This pattern checks whether a function was called with two of its arguments swapped. 
When multiple arguments of a function are of the same type, if developers do not accurately remember what each argument represents then they can easily swap two such arguments without realizing it.\
**Same Function Swap Args** example patch:
```diff
diff --git a/servers/src/main/java/tachyon/master/BlockInfo.java b/servers/src/main/java/tachyon/master/BlockInfo.java
index 10f3b21..ec659db 100644
--- a/servers/src/main/java/tachyon/master/BlockInfo.java
+++ b/servers/src/main/java/tachyon/master/BlockInfo.java
@@ -187,7 +187,8 @@
           } catch (NumberFormatException nfe) {
             continue;
           }
-          ret.add(new NetAddress(resolvedHost, resolvedPort, -1));
+          // The resolved port is the data transfer port not the rpc port
+          ret.add(new NetAddress(resolvedHost, -1, resolvedPort));
         }
       }
     }
```

## Change Binary Operator
This pattern checks whether a binary operand was accidentally replaced with another one of the same type.
For example, developers very often mix comparison operators in expressions.\
**Change Binary Operator** example patch:
```diff
diff --git a/core/server/worker/src/main/java/alluxio/worker/netty/DataServerReadHandler.java b/core/server/worker/src/main/java/alluxio/worker/netty/DataServerReadHandler.java
index 97a07fa..195d89a 100644
--- a/core/server/worker/src/main/java/alluxio/worker/netty/DataServerReadHandler.java
+++ b/core/server/worker/src/main/java/alluxio/worker/netty/DataServerReadHandler.java
@@ -393,7 +393,7 @@
     @GuardedBy(\"mLock\")
     private boolean shouldRestartPacketReader() {
       return !mPacketReaderActive && !tooManyPendingPackets() && mPosToQueue < mRequest.mEnd
-          && mError != null && !mCancel && !mEof;
+          && mError == null && !mCancel && !mEof;
     }
   }
```


## Change Unary Operator
This pattern checks whether a unary operand was accidentally replaced with another one of the same type.
For example, developers very often may forget the ! operator in a boolean expression.\
**Change Unary Operator** example patch:
```diff
diff --git a/core/client/src/main/java/alluxio/client/file/FileInStream.java b/core/client/src/main/java/alluxio/client/file/FileInStream.java
index b263009..5592db2 100644
--- a/core/client/src/main/java/alluxio/client/file/FileInStream.java
+++ b/core/client/src/main/java/alluxio/client/file/FileInStream.java
@@ -454,7 +454,7 @@
 
     // If this block is read from a remote worker but we don't have a local worker, don't cache
     if (mCurrentBlockInStream instanceof RemoteBlockInStream
-        && BlockStoreContext.INSTANCE.hasLocalWorker()) {
+        && !BlockStoreContext.INSTANCE.hasLocalWorker()) {
       return;
     }
```

## Change Operand
This pattern checks whether one of the operands in a binary operation was wrong.\
**Change Operand** example patch:
```diff
diff --git a/modules/VisualizationImpl/src/main/java/org/gephi/visualization/swing/StandardGraphIO.java b/modules/VisualizationImpl/src/main/java/org/gephi/visualization/swing/StandardGraphIO.java
index fd49c8a..67b74a9 100644
--- a/modules/VisualizationImpl/src/main/java/org/gephi/visualization/swing/StandardGraphIO.java
+++ b/modules/VisualizationImpl/src/main/java/org/gephi/visualization/swing/StandardGraphIO.java
@@ -470,7 +470,7 @@
         float newCameraLocation = Math.max(newCameraLocationX, newCameraLocationY);
 
         graphDrawable.cameraLocation[0] = limits.getMinXoctree() + graphWidth / 2;
-        graphDrawable.cameraLocation[1] = limits.getMinYoctree() + graphWidth / 2;
+        graphDrawable.cameraLocation[1] = limits.getMinYoctree() + graphHeight / 2;
         graphDrawable.cameraLocation[2] = newCameraLocation;
 
         graphDrawable.cameraTarget[0] = graphDrawable.cameraLocation[0];
```

## More Specific If
This pattern checks whether an extra condition (&& operand) was added in an if statement’s condition.\
**More Specific If** example patch:
```diff
diff --git a/hazelcast/src/main/java/com/hazelcast/impl/ConcurrentMapManager.java b/hazelcast/src/main/java/com/hazelcast/impl/ConcurrentMapManager.java
index b01c711..85eb787 100644
--- a/hazelcast/src/main/java/com/hazelcast/impl/ConcurrentMapManager.java
+++ b/hazelcast/src/main/java/com/hazelcast/impl/ConcurrentMapManager.java
@@ -546,7 +546,7 @@
         }
         for (Future\u003cPairs\u003e future : lsFutures) {
             Pairs pairs = future.get();
-            if (pairs != null) {
+            if (pairs != null && pairs.getKeyValues()!=null) {
                 for (KeyValue keyValue : pairs.getKeyValues()) {
                     results.addKeyValue(keyValue);
                 }
```

## Less Specific If
This pattern checks whether an extra condition which either this or the original one needs to hold (∥ operand) was added in
an if statement’s condition.\
**Less Specific If** example patch:
```diff
diff --git a/modules/swagger-core/src/main/java/io/swagger/v3/core/jackson/ModelResolver.java b/modules/swagger-core/src/main/java/io/swagger/v3/core/jackson/ModelResolver.java
index baea6e8..aeca799 100644
--- a/modules/swagger-core/src/main/java/io/swagger/v3/core/jackson/ModelResolver.java
+++ b/modules/swagger-core/src/main/java/io/swagger/v3/core/jackson/ModelResolver.java
@@ -999,7 +999,7 @@
                 }
             }
         }
-        if (subtypeProps.isEmpty()) {
+        if (subtypeProps == null || subtypeProps.isEmpty()) {
             child.setProperties(null);
         }
     }
```

## Missing Throws Exception
This pattern checks whether the fix added a throws clause in a function declaration.\
**Missing Throws Exception** example patch:
```diff
diff --git a/example/src/main/java/io/netty/example/securechat/SecureChatServer.java b/example/src/main/java/io/netty/example/securechat/SecureChatServer.java
index 6dad108..19a9dac 100644
--- a/example/src/main/java/io/netty/example/securechat/SecureChatServer.java
+++ b/example/src/main/java/io/netty/example/securechat/SecureChatServer.java
@@ -31,7 +31,7 @@
         this.port \u003d port;
     }
 
-    public void run() {
+    public void run() throws InterruptedException {
         ServerBootstrap b \u003d new ServerBootstrap();
         try {
             b.eventLoop(new NioEventLoop(), new NioEventLoop())
```

## Delete Throws Exception
This pattern checks whether the fix deleted an throws clause in a function declaration.\
**Delete Throws Exception** example patch:
```diff
diff --git a/core/server/src/main/java/tachyon/web/WebInterfaceAbstractMetricsServlet.java b/core/server/src/main/java/tachyon/web/WebInterfaceAbstractMetricsServlet.java
index 23ae5cd..2773882 100644
--- a/core/server/src/main/java/tachyon/web/WebInterfaceAbstractMetricsServlet.java
+++ b/core/server/src/main/java/tachyon/web/WebInterfaceAbstractMetricsServlet.java
@@ -43,13 +43,12 @@
   }
 
   /**
-   * Populates key, value pairs for UI display.
+   * Populates operation metrics for displaying in the UI
    *
    * @param request The {@link HttpServletRequest} object
-   * @throws IOException if an I/O error occurs
    */
   protected void populateCountersValues(Map<String, Metric> operations,
-      Map<String, Counter> rpcInvocations, HttpServletRequest request) throws IOException {
+      Map<String, Counter> rpcInvocations, HttpServletRequest request){
 
     for (Map.Entry<String, Metric> entry : operations.entrySet()) {
       if (entry.getValue() instanceof Gauge) {
```
