# mongo-lucene

MongoDB-backed lucene directory for a scalable real-time search.

## License

Apache 2.0 License (http://www.apache.org/licenses/LICENSE-2.0)

## Requirements / Dependencies

* Java 1.7+ (http://www.java.com/de/download/)
* Apache Lucene 4.9.0+ (http://lucene.apache.org)
* MongoDB Java-Driver 2.12.2+ (https://github.com/mongodb/)

## How to get it

The maven dependecy:

```xml
<dependency>
    <groupId>com.github.mongoutils</groupId>
    <artifactId>mongo-lucene</artifactId>
    <version>1.2-SNAPSHOT</version>
</dependency>
```

## How to use it

```java
// Mongo connection
Mongo mongo = new Mongo("localhost", options);
DB db = mongo.getDB("testdb");
DBCollection dbCollection = db.getCollection("testcollection");

// serializers + map-store
DBObjectSerializer<String> keySerializer = new SimpleFieldDBObjectSerializer<String>("key");
DBObjectSerializer<MapDirectoryEntry> valueSerializer = new MapDirectoryEntrySerializer("value");
ConcurrentMap<String, MapDirectoryEntry> store = new MongoConcurrentMap<String, MapDirectoryEntry>(dbCollection, keySerializer, valueSerializer);

// lucene directory
Directory dir = new MapDirectory(store);

// index files
StandardAnalyzer analyser = new StandardAnalyzer();
IndexWriterConfig config = new IndexWriterConfig(analyser);
IndexWriter writer = new IndexWriter(dir, config);
Document doc = new Document();
doc.add(new TextField("title", "My file's content ...", Field.Store.YES));
writer.addDocument(doc);
writer.close();

...

// search index
Query q = new QueryParser("title", analyser).parse("My*content");
IndexReader reader = IndexReader.open(dir);
IndexSearcher searcher = new IndexSearcher(reader);
TopScoreDocCollector collector = TopScoreDocCollector.create(10, true);
```

## Test it using mongodb-vm

The project comes with a fully functional VM with an mongodb installation for testing purpose.
You need to have VirtualBox (https://www.virtualbox.org/) and Vagrant (http://vagrantup.com/) installed to run the VM.
All necessary ports are forwarded to the VM so you can connect to mongodb as it were installed on your system directly.

Check the project out, open a console in that directory and type:

```text
cd mongovm
vagrant up
```

Integration tests are done with https://github.com/joelittlejohn/embedmongo-maven-plugin.


[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/rstiller/mongo-lucene/trend.png)](https://bitdeli.com/free "Bitdeli Badge")

