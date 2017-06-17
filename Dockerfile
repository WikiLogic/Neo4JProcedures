#The dockerfile for Wikilogic's database server

# Pull base image - we're running it on Ubuntu!
FROM neo4j:3.0

# Install the db procedures
COPY ./plugins ./plugins

# Expose ports.
EXPOSE 7474
EXPOSE 7687