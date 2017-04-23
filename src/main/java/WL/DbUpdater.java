package WL;

import java.util.List;
import java.util.Iterator;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.procedure.UserFunction;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.procedure.*;
import org.neo4j.logging.Log;
import org.neo4j.graphdb.GraphDatabaseService;

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

    @Procedure(value = "WL.CreateArgumentGroup", mode = Mode.SCHEMA)
    @Description("Takes node id's and create a new arg group with those nodes as premises")
    public void CreateArgumentGroup(@Name("nodeIds") List<Long> nodeIds) {

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
            //argGroupProb += new Double(node.getProperty("probability").toString()).doubleValue();
        }

        argGroup.setProperty("probability", lastValue);

        log.info("HELLO THERE!");
    }

    @Procedure(value = "WL.AttachArgumentGroup", mode = Mode.SCHEMA)
    @Description("Link the argGroupId to the claimId with a connection of the type passed in")
    public void AttachArgumentGroup(@Name("claimId") Long claimId, @Name("argGroupId") Long argGroupId, 
            @Name("type") String connectionType) {

        Node argGroup = db.getNodeById(argGroupId);
        Node claim = db.getNodeById(claimId);

        RelationshipType rType = new RelationshipTypeImpl(connectionType);
        argGroup.createRelationshipTo(claim, rType);

        log.info("HELLO THERE!");
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