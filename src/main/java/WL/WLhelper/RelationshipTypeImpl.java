package WL.WLhelper;

import org.neo4j.graphdb.RelationshipType;

public class RelationshipTypeImpl implements RelationshipType {

    private final String _name;

    public RelationshipTypeImpl(String name) {
        _name = name;
    }

    @Override
    public String name() {
        return _name;
    }
}