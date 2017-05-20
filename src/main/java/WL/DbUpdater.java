package WL;

import java.util.List;
import java.util.stream.Stream;

import java.util.Iterator;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.procedure.UserFunction;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.procedure.*;
import org.neo4j.logging.Log;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.util.Map;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.graphdb.Result;

/**
 * This is an example how you can create a simple user-defined function for Neo4j.
 */
public class DbUpdater {
    // This field declares that we need a GraphDatabaseService
    // as context when any procedure in this class is invoked
    @Context
    public GraphDatabaseService db;

    // This gives us a log instance that outputs messages to the
    // standard log, normally found under `data/log/console.log`
    @Context
    public Log log;

    @UserFunction
    @Description("Who to mention?")
    public String Hello(@Name("name") String name) {
        return "Hello " + name;
    }

    /**
     * Arguments multiply all parts together as its the group is only valid if all the parts happen to be correct 
     * This means if one premise is > 50, it will make it too small to count
     */
    @Procedure(value = "WL.CreateArgumentGroup", mode = Mode.SCHEMA)
    @Description("Takes node id's and create a new arg group with those nodes as premises")
    public Stream<LongResult> CreateArgumentGroup(@Name("nodeIds") List<Long> nodeIds) {

        Label[] labels = { Label.label("ArgGroup") };
        Node argGroup = db.createNode(labels);

        double lastValue = 0;
        for (Iterator<Long> i = nodeIds.iterator(); i.hasNext();) {
            Long item = i.next();
            Node node = db.getNodeById(item);
            RelationshipType rType = new RelationshipTypeImpl("USED_IN");
            node.createRelationshipTo(argGroup, rType);

            Double thisProb = new Double(node.getProperty("probability").toString()).doubleValue();
            if (lastValue == 0) {
                lastValue = thisProb;
            } else {
                lastValue = lastValue * thisProb;
            }
        }

        argGroup.setProperty("probability", lastValue);

        log.info("HELLO THERE!");

        //return Stream.of(argGroup.getId());
        return Stream.of(new LongResult(argGroup.getId()));
    }

    @Procedure(value = "WL.AttachArgumentGroup", mode = Mode.SCHEMA)
    @Description("Link the argGroupId to the claimId with a connection of the type passed in")
    //http://stackoverflow.com/questions/33163622/neo4j-update-properties-on-10-million-nodes
    public Stream<ClaimResult> AttachArgumentGroup(@Name("argGroupId") Long argGroupId, @Name("claimId") Long claimId,
            @Name("type") String connectionType) {

        Node argGroup = db.getNodeById(argGroupId);
        Node claim = db.getNodeById(claimId);

        //create the relationship
        RelationshipType rType = new RelationshipTypeImpl(connectionType);
        argGroup.createRelationshipTo(claim, rType);

        /*
        //if the arg is >50 then its useful so add it (otherwise its a false -differs from OPPOSSES- argument so we dont include it for working out claims final score)
        Double argProb = new Double(argGroup.getProperty("probability").toString()).doubleValue();
        if (argProb < 50)
            return;
        
        //get the claim property as it is right now before argGroup added
        Double claimProb = new Double(claim.getProperty("probability").toString()).doubleValue();
        
        //want to get probability it didnt happen
        //now get teh probabiltiy both did not happen together
        */

        Double attachedArgProb = new Double(argGroup.getProperty("probability").toString()).doubleValue();
        Double claimOriginalProb = new Double(claim.getProperty("probability").toString()).doubleValue();

        Double newClaimProb;
        if (attachedArgProb > .5) {
            newClaimProb = 1.0;
        } else {
            newClaimProb = 0.0;
        }

        claim.setProperty("probability", newClaimProb);

        if (claimOriginalProb != newClaimProb) {

            //now arg Group added, and claim has changed to reflect, get all arg groups and update them
            UpdateArgState(claim);
            log.info("updating");
        }

        //return a claim object with attached arg groups - this is a query
        Result result;
        Transaction tx = db.beginTx();
        try {
            result = db.execute(GetClaimWithArgs(claimId));

            tx.success();
        } finally {
            tx.close();
        }

        // String stringResult = result.resultAsString();
        // return Stream.of(new SearchHit(stringResult));

        return result.stream().map(ClaimResult::new);
    }

    public static class SearchHit {
        // This records contain a single field named 'nodeId'
        public String res;

        public SearchHit(String res) {
            this.res = res;
        }
    }


    public class ClaimResult {
        public Object mainClaim;

        public ClaimResult(Object mainClaim) {
            this.mainClaim = mainClaim;
        }

        public ClaimResult(Map<String, Object> row) {

            this((Object) row.get("claim"));
        }
    }


    // public class ClaimResult {
    //     public Node mainClaim;
    //     public Node argGroup;

    //     public ClaimResult(Node mainClaim, Node argGroup) {
    //         this.mainClaim = mainClaim;
    //         this.argGroup = argGroup;
    //     }

    //     public ClaimResult(Map<String, Object> row) {

    //         this((Node) row.get("claim"), (Node) row.get("argGroup"));
    //     }
    // }

    // public class ArgResult {
    //     public Node argNode;
    //     public List<Node> premises;

    //     public ArgResult(Node argNode, List<Node> premises) {
    //         this.argNode = argNode;
    //         this.premises = premises;
    //     }

    //     // public HelpResult(Map<String, Object> row) {
    //     //     this((String) row.get("type"), (String) row.get("name"), (String) row.get("description"),
    //     //             (String) row.get("signature"), null, (Boolean) row.get("writes"));
    //     // }
    // }





    // String filter = " WHERE name starts with 'apoc.' "
    //         + " AND ({name} IS NULL  OR toLower(name) CONTAINS toLower({name}) "
    //         + " OR ({desc} IS NOT NULL AND toLower(description) CONTAINS toLower({desc}))) "
    //         + "RETURN type, name, description, signature ";

    // String query = "WITH 'procedure' as type CALL dbms.procedures() yield name, description, signature " + filter +
    //             " UNION ALL " +
    //             "WITH 'function' as type CALL dbms.functions() yield name, description, signature " + filter;
                
    //             return db.execute(query, map("name", name, "desc", searchText ? name : null))
    //             .stream().map(HelpResult::new);














    private String GetClaimWithArgs(long claimID) {
        // return "MATCH (claim) " 
        // + "WHERE (claim:Claim OR claim:Axiom) AND (ID(claim) =" + claimID + ") "
        //         + "RETURN claim";
        
        return
        
        "MATCH (claim) " + "WHERE (claim:Claim OR claim:Axiom) AND (ID(claim) ="
        + claimID + ") "
                + "OPTIONAL MATCH (argument:ArgGroup)-[argLink]->(claim) "
                + "OPTIONAL MATCH (premis:Claim)-[premisLink]->(argument) " + "WITH claim, argument, argLink,  "
                + "CASE WHEN ID(premis) IS NULL THEN null ELSE {id: ID(premis), text: premis.text, labels: LABELS(premis), probability: premis.probability} END AS premises "
                + "WITH claim,  "
                + "CASE WHEN ID(argument) IS NULL THEN null ELSE {id: ID(argument), type:TYPE(argLink), probability: argument.probability, premises: COLLECT(premises)} END AS arguments  "
                + "WITH {id: id(claim), text: claim.text, labels: LABELS(claim), probability: claim.probability, arguments: COLLECT(arguments)} AS claim "
                + "RETURN claim " + "LIMIT 100";
    }

    /**
     * Takes the claim that has changed and gets all the argGroups its involved with and sees if they need updated
     */
    private void UpdateArgState(Node startClaim) {
        Transaction transaction = db.beginTx();

        //get all the claims realtionships where it was used in the argGroup
        Iterable<org.neo4j.graphdb.Relationship> usedInRelations = startClaim
                .getRelationships(MyRelationshipTypes.USED_IN, Direction.OUTGOING);

        double newArgProbability = 0;
        for (org.neo4j.graphdb.Relationship entry : usedInRelations) {
            try {
                Node argNode = entry.getEndNode();
                Double argNodeOriginalValue = new Double(argNode.getProperty("probability").toString()).doubleValue();
                // double argBeforeChange = (Double) argNode.getProperty("probability");
                // // divide to get rid of old score then times to udpate with new
                // newArgProbability = (argBeforeChange / claimBeforeChange) * claimAfterChange;
                // argNode.setProperty("probability", newArgProbability);

                //get all its nodes and if they are all correct now, change probability
                Iterable<org.neo4j.graphdb.Relationship> nodesInArgGroup = argNode
                        .getRelationships(MyRelationshipTypes.USED_IN, Direction.INCOMING);
                boolean state = true;
                for (org.neo4j.graphdb.Relationship node : nodesInArgGroup) {
                    if (new Double(node.getProperty("probability").toString()).doubleValue() < .5) {
                        state = false;
                        break;
                    }
                }

                //check if there has been a change from cogent to uncogent or vice versa which means we have to update
                if (argNodeOriginalValue > 0.0 && state == false) {
                    argNode.setProperty("probability", 1.0);
                    UpdateClaimState(argNode);
                } else if (argNodeOriginalValue < 1.0 && state == true) {
                    argNode.setProperty("probability", 0.0);

                    UpdateClaimState(argNode);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        transaction.success();
        transaction.close();
    }

    private void UpdateClaimState(Node originalArgNode) {
        Transaction transaction = db.beginTx();

        //get all the claims realtionships where it was used in the argGroup
        Iterable<org.neo4j.graphdb.Relationship> usedForRelations = originalArgNode
                .getRelationships(MyRelationshipTypes.SUPPORTS, Direction.OUTGOING);

        double newClaimProbability = 0;
        for (org.neo4j.graphdb.Relationship entry : usedForRelations) {
            try {

                Node claimNode = entry.getEndNode();

                //the arg node getting its usedForRelations probably only gets back othe one node but for future proof
                //we check that the node does not have other arg goups
                Iterable<org.neo4j.graphdb.Relationship> supportingArgGroups = claimNode
                        .getRelationships(MyRelationshipTypes.SUPPORTS, Direction.INCOMING);
                boolean state = false;
                for (org.neo4j.graphdb.Relationship argGroup : supportingArgGroups) {
                    Node argNode = argGroup.getEndNode();

                    Double argValue = new Double(argNode.getProperty("probability").toString()).doubleValue();
                    if (argValue > 0.0) {
                        state = true;
                        break;
                    }
                }

                //if it is false, there is not even one supporting arg group for the claim so it needs to udpate
                if (state = false)
                    claimNode.setProperty("probability", 0.0);

                UpdateArgState(claimNode);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        transaction.success();
        transaction.close();
    }

    enum MyRelationshipTypes implements RelationshipType {
        USED_IN, SUPPORTS, OPPOSSES
    }

    private static class RelationshipTypeImpl implements RelationshipType {

        private final String _name;

        RelationshipTypeImpl(String name) {
            _name = name;
        }

        @Override
        public String name() {
            return _name;
        }
    }
}

// private void UpdateArgProbability(Node startClaim, Double claimBeforeChange, Double claimAfterChange) {
//     Transaction transaction = db.beginTx();

//     //get all the claims realtionships where it was used in the argGroup
//     Iterable<org.neo4j.graphdb.Relationship> usedInRelations = startClaim
//             .getRelationships(MyRelationshipTypes.USED_IN, Direction.OUTGOING);

//     double newArgProbability = 0;
//     for (org.neo4j.graphdb.Relationship entry : usedInRelations) {
//         try {
//             Node argNode = entry.getEndNode();
//             double argBeforeChange = (Double) argNode.getProperty("probability");
//             // divide to get rid of old score then times to udpate with new
//             newArgProbability = (argBeforeChange / claimBeforeChange) * claimAfterChange;
//             argNode.setProperty("probability", newArgProbability);

//             UpdateClaimProbability(argNode, argBeforeChange, newArgProbability);

//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }

//     transaction.success();
//     transaction.close();
// }

// private void UpdateClaimProbability(Node argNode, Double argBeforeChange, Double newArgProbability) {
//     Transaction transaction = db.beginTx();

//     //get all the claims realtionships where it was used in the argGroup
//     Iterable<org.neo4j.graphdb.Relationship> usedForRelations = argNode
//             .getRelationships(MyRelationshipTypes.SUPPORTS, Direction.OUTGOING);

//     double newClaimProbability = 0;
//     for (org.neo4j.graphdb.Relationship entry : usedForRelations) {
//         try {
//             Node claimNode = entry.getEndNode();
//             double claimBeforeChange = (Double) claimNode.getProperty("probability");
//             // divide to get rid of old score then times to udpate with new
//             newClaimProbability = (claimBeforeChange / argBeforeChange) * newArgProbability;
//             claimNode.setProperty("probability", newClaimProbability);

//             UpdateClaimProbability(claimNode, claimBeforeChange, newClaimProbability);

//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }

//     transaction.success();
//     transaction.close();
// }












// public Stream<GraphResult> graph()
//     {
//         Result execute = db.execute( "CALL dbms.cluster.overview()" );
//         List<Node> servers = new LinkedList<>();
//         List<Relationship> relationships = new LinkedList<>();

//         while ( execute.hasNext() )
//         {
//             Map<String,Object> next = execute.next();
//             String role = (String) next.get( "role" );
//             String id = (String) next.get( "id" );
//             Label roleLabel = Label.label( role );
//             String[] addresses = Arrays.stream( (Object[]) next.get( "addresses" ) ).toArray( String[]::new );
//             Map<String,Object> properties = new HashMap<>();
//             properties.put( "name", shortName.get( role ) );
//             properties.put( "title", role );
//             properties.put( boltAddressKey, addresses[0] );
//             properties.put( "http_address", addresses[1] );
//             properties.put( "cluster_id", id );
//             Node server = new VirtualNode( new Label[]{roleLabel}, properties, db );
//             servers.add( server );
//         }

//         Optional<Node> leaderNode = getLeaderNode( servers );
//         if ( leaderNode.isPresent() )
//         {
//             for ( Node server : servers )
//             {
//                 if ( server.hasLabel( Label.label( "FOLLOWER" ) ) )
//                 {
//                     VirtualRelationship follows =
//                             new VirtualRelationship( server, leaderNode.get(), RelationshipType.withName( "FOLLOWS" ) );
//                     relationships.add( follows );
//                 }
//             }
//         }

//         VirtualNode client =
//                 new VirtualNode( new Label[]{Label.label( "CLIENT" )}, singletonMap( "name", "Client" ), db );
//         Optional<Relationship> clientConnection = determineClientConnection( servers, client );
//         if ( clientConnection.isPresent() )
//         {
//             servers.add( client );
//             relationships.add( clientConnection.get() );
//         }

//         GraphResult graphResult = new GraphResult( servers, relationships );
//         return Stream.of( graphResult );
//     }
// public class GraphResult {
//     public final List<Node> nodes;
//     public final List<Relationship> relationships;

//     public GraphResult(List<Node> nodes, List<Relationship> relationships) {
//         this.nodes = nodes;
//         this.relationships = relationships;
//     }
// }