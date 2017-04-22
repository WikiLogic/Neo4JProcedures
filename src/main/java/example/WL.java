package example;

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
public class WL
{ 
    // This field declares that we need a GraphDatabaseService
                  // as context when any procedure in this class is invoked
    @Context
    public GraphDatabaseService db;

    // This gives us a log instance that outputs messages to the
    // standard log, normally found under `data/log/console.log`
    @Context
    public Log log;

    @UserFunction
    @Description("WHo to mention?")
    public String HelloX(@Name("name") String name) 
    {
        return "Hello " + name;
    }

    @Procedure(value = "example.AddArgumentGroup", mode = Mode.SCHEMA)
    @Description("For the node with the given node-id, add properties for the provided keys to hello per label")
    public void AddArgumentGroup(@Name("nodeIds") List<Long> nodeIds) {

        Label[] labels = { Label.label("ArgGroup") };
        Node argGroup = db.createNode(labels);

        int argGroupProb = 0;
        for (Iterator<Long> i = nodeIds.iterator(); i.hasNext();) {
            Long item = i.next();
            Node node = db.getNodeById(item);
            RelationshipType rType = new RelationshipTypeImpl("USED_IN");
            node.createRelationshipTo(argGroup, rType);

            argGroupProb += (int) node.getProperty("probability");
        }

        argGroup.setProperty("probability", argGroupProb);

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