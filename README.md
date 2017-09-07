# schema-registry-examples

## Getting Started

### Setting up the Schema Registry

First, download the latest release of the schema registry:

```sh
$ wget https://github.com/hortonworks/registry/releases/download/v0.2.1/hortonworks-registry-0.2.1.tar.gz
$ tar xzvf hortonworks-registry-0.2.1.tar.gz
$ cd hortonworks-registry-0.2.1
$ cp conf/registry.yaml
```

Edit ./conf/registry.yaml, set the applicationConnectors port to 9999 (the NiFi template in ./src/main/resources/Twitter_Feed_Example.xml assumes this port), and then:

```sh
$ sudo ./bin/registry-server-start.sh conf/registry.yaml
```

Go to http://localhost:9999 and upload ./src/main/resources/Tweet.avsc or POST the schema to the registry REST API:

```sh
$ curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
  "type": "avro",
  "schemaGroup": "Kafka",
  "name": "twitter.Tweet",
  "description": "A Twitter status message",
  "compatibility": "BOTH",
  "evolve": true
 }' http://localhost:9999/api/v1/schemaregistry/schemas
 $ curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "description": "v1",
   "schemaText": "{\"namespace\":\"twitter\",\"type\":\"record\",\"name\":\"Tweet\",\"fields\":[{\"name\":\"id\",\"type\":\"long\"},{\"name\":\"id_str\",\"type\":\"string\"},{\"name\":\"text\",\"type\":\"string\"},{\"name\":\"lang\",\"type\":\"string\"},{\"name\":\"favorite_count\",\"type\":\"long\"},{\"name\":\"created_at\",\"type\":\"string\"},{\"name\":\"timestamp_ms\",\"type\":\"string\"},{\"name\":\"user\",\"type\":{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"id\",\"type\":\"long\"},{\"name\":\"id_str\",\"type\":\"string\"},{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"screen_name\",\"type\":\"string\"},{\"name\":\"location\",\"type\":[\"null\",\"string\"]},{\"name\":\"url\",\"type\":[\"null\",\"string\"]},{\"name\":\"description\",\"type\":[\"null\",\"string\"]},{\"name\":\"followers_count\",\"type\":\"long\"},{\"name\":\"friends_count\",\"type\":\"long\"}]}},{\"name\":\"entities\",\"type\":{\"type\":\"record\",\"name\":\"Entities\",\"fields\":[{\"name\":\"hashtags\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"Hashtag\",\"fields\":[{\"name\":\"text\",\"type\":\"string\"}]}}},{\"name\":\"user_mentions\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"UserMention\",\"fields\":[{\"name\":\"id\",\"type\":\"long\"},{\"name\":\"id_str\",\"type\":\"string\"},{\"name\":\"screen_name\",\"type\":\"string\"},{\"name\":\"name\",\"type\":\"string\"}]}}}]}}]}"
 }' 'http://localhost:9999/api/v1/schemaregistry/schemas/twitter.Tweet/versions'
```

### Running the Example

You can run the examples from the SBT console as follows:

```sh
$ ./sbt
> compile
> run --bootstrap.servers localhost:9092 --zookeeper.connect --group.id test --schema NONE --topic test
```

The job can be cancelled with CTRL-C when you are finished.
