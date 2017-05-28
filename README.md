# Neo4JProcedures
Java code for updating the database



####Install as a user only:

 Download binary from https://github.com/WikiLogic/Neo4JProcedures/releases
 
 Add to ...Documents\Neo4j\default.graphdb\plugins  Note: Windows has two plugin folders. Do NOT use the plugin folder at the installation   directory.
 
 Restart Neo4
 
 ####Helpful functions for checking it worked:
 
 CALL dbms.procedures()
 
 CALL dbms.functions()
 
 ####Run:
 
 call WL.CreateArgumentGroup([1243, 1254])

 call WL.AttachArgumentGroup(1210, 1215, "SUPPORTS")


####Build binary:

Navigate to the root folder with the pom.xml file in your command window

run: mvn clean package

target folder contains original-WL-1.0.0


####Setup development:

Coming soon
