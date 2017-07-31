package WL.WLhelper;

import org.neo4j.graphdb.RelationshipType;

import java.util.*;

public class Enums {
    public static enum MyRelationshipTypes implements RelationshipType {
        USED_IN, SUPPORTS, OPPOSSES
    }
}