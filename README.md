# schema-registry-examples

## Getting Started

### Setting up the Confluent Schema Registry

#### Building from Source

```sh
$ mkdir confluent && cd confluent
$ git clone https://github.com/confluentinc/common.git
$ cd common
$ git checkout tags/v3.3.0
$ mvn -T 2.0C clean install -DskipTests
$ cd ../
$ git clone https://github.com/confluentinc/rest-utils.git
$ cd rest-utils
$ git checkout tags/v3.3.0
$ mvn -T 2.0C clean install -DskipTests
$ cd ../
$ git clone https://github.com/confluentinc/schema-registry.git
$ cd schema-registry
$ git checkout tags/v3.3.0
$ mvn -T 2.0C clean package -DskipTests
$ ./bin/schema-registry-start config/schema-registry.properties
```

#### Running with Docker for Mac

```sh
$ docker pull confluentinc/cp-schema-registry
$ docker run -d \
  --name=confluent-schema-registry \
  -p 8081:8081 \
  -e SCHEMA_REGISTRY_KAFKASTORE_CONNECTION_URL=docker.for.mac.localhost:2181 \
  -e SCHEMA_REGISTRY_HOST_NAME=localhost \
  -e SCHEMA_REGISTRY_LISTENERS=http://0.0.0.0:8081 \
  confluentinc/cp-schema-registry
```

#### Trying it Out

Now if you curl the /subjects endpoint you should get an empty list:

```sh
$ curl http://localhost:8081/subjects
[]
```

The Confluent Schema Registry provides a console producer that can encode and publish messages using an appropriate serializer/wire format:

```sh
$ ./bin/kafka-avro-console-producer \
  --broker-list localhost:9092 \
  --topic tweets.avro.confluent \
  --property value.schema='{"namespace":"twitter","type":"record","name":"Tweet","fields":[{"name":"id","type":"long"},{"name":"id_str","type":"string"},{"name":"text","type":"string"},{"name":"lang","type":"string"},{"name":"favorite_count","type":"long"},{"name":"created_at","type":"string"},{"name":"timestamp_ms","type":"string"},{"name":"user","type":{"type":"record","name":"User","fields":[{"name":"id","type":"long"},{"name":"id_str","type":"string"},{"name":"name","type":"string"},{"name":"screen_name","type":"string"},{"name":"location","type":["null","string"]},{"name":"url","type":["null","string"]},{"name":"description","type":["null","string"]},{"name":"statuses_count","type":"long"},{"name":"followers_count","type":"long"},{"name":"friends_count","type":"long"}]}},{"name":"entities","type":{"type":"record","name":"Entities","fields":[{"name":"hashtags","type":{"type":"array","items":{"type":"record","name":"Hashtag","fields":[{"name":"text","type":"string"}]}}},{"name":"user_mentions","type":{"type":"array","items":{"type":"record","name":"UserMention","fields":[{"name":"id","type":"long"},{"name":"id_str","type":"string"},{"name":"screen_name","type":"string"},{"name":"name","type":"string"}]}}}]}}]}'
```

### Setting up the Hortonworks Schema Registry

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
  "compatibility": "FORWARD",
  "evolve": true
 }' http://localhost:9999/api/v1/schemaregistry/schemas
 ```

 ```sh
 $ curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "description": "v1",
   "schemaText": "{\"namespace\":\"twitter\",\"type\":\"record\",\"name\":\"Tweet\",\"fields\":[{\"name\":\"id\",\"type\":\"long\"},{\"name\":\"id_str\",\"type\":\"string\"},{\"name\":\"text\",\"type\":\"string\"},{\"name\":\"lang\",\"type\":\"string\"},{\"name\":\"favorite_count\",\"type\":\"long\"},{\"name\":\"created_at\",\"type\":\"string\"},{\"name\":\"timestamp_ms\",\"type\":\"string\"},{\"name\":\"user\",\"type\":{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"id\",\"type\":\"long\"},{\"name\":\"id_str\",\"type\":\"string\"},{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"screen_name\",\"type\":\"string\"},{\"name\":\"location\",\"type\":[\"null\",\"string\"]},{\"name\":\"url\",\"type\":[\"null\",\"string\"]},{\"name\":\"description\",\"type\":[\"null\",\"string\"]},{\"name\":\"statuses_count\", \"type\":\"long\"},{\"name\":\"followers_count\",\"type\":\"long\"},{\"name\":\"friends_count\",\"type\":\"long\"}]}},{\"name\":\"entities\",\"type\":{\"type\":\"record\",\"name\":\"Entities\",\"fields\":[{\"name\":\"hashtags\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"Hashtag\",\"fields\":[{\"name\":\"text\",\"type\":\"string\"}]}}},{\"name\":\"user_mentions\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"UserMention\",\"fields\":[{\"name\":\"id\",\"type\":\"long\"},{\"name\":\"id_str\",\"type\":\"string\"},{\"name\":\"screen_name\",\"type\":\"string\"},{\"name\":\"name\",\"type\":\"string\"}]}}}]}}]}"
 }' 'http://localhost:9999/api/v1/schemaregistry/schemas/twitter.Tweet/versions'
```

### Running the Examples

You can run the examples from the SBT console as follows. The job can be cancelled with CTRL-C when you are finished.

First build the example app:

```sh
$ ./sbt
> compile
```

#### Raw Data

```sh
> run --bootstrap.servers localhost:9092 --zookeeper.connect --group.id test --schema NONE --topic tweets
```

#### Embedded Schema

```sh
> run --bootstrap.servers localhost:9092 --zookeeper.connect --group.id test --schema NONE --topic tweets.avro.embedded
```

#### Hortonworks Schema Registry

```sh
> run --bootstrap.servers localhost:9092 --zookeeper.connect --group.id test --schema HORTONWORKS --topic tweets.avro.hortonworks
```
