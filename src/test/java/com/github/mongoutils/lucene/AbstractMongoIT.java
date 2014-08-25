package com.github.mongoutils.lucene;

import com.mongodb.*;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractMongoIT {

    protected MongoClient mongo;
    protected DB db;
    protected DBCollection dbCollection;

    @Before
    public void createMongo() throws Exception {
        mongo = new MongoClient("localhost", 27017);
       mongo.dropDatabase("testdb");
        db = mongo.getDB("testdb");
        dbCollection = db.getCollection("testcollection");
    }

    @After
    public void closeMongo() {
        db.dropDatabase();
        mongo.close();
    }

}
