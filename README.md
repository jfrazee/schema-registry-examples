# schema-registry-examples

## Getting Started

### Running the Example

You can run the examples from the SBT console as follows:

```sh
$ ./sbt
> compile
> run --bootstrap.servers localhost:9092 --zookeeper.connect --group.id test --schema NONE --topic test
```

The job can be cancelled with CTRL-C when you are finished.
