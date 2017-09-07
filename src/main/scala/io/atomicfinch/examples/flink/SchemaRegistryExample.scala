/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomicfinch.examples.flink

import org.apache.flink.streaming.api.scala._

import org.apache.avro.generic.GenericRecord
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010
import org.apache.flink.streaming.util.serialization.SimpleStringSchema

// run --bootstrap.servers localhost:9092 --zookeeper.connect --group.id test --schema NONE --topic test
object SchemaRegistryExample {

  def main(args: Array[String]): Unit = {
    val params = ParameterTool.fromArgs(args)

    if (params.getNumberOfParameters < 4) {
      println("Missing parameters!\n"
        + "Usage: SchemaRegistryExample "
        + "--topic <topic> "
        + "--bootstrap.servers <kafka brokers> "
        + "--zookeeper.connect <zk quorum> "
        + "--group.id <some id> "
        + "[--schema <type>] "
        + "[--output <path>]")
      return
    }

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.getConfig.setGlobalJobParameters(params)

    val stream =
      params.get("schema", "NONE").toUpperCase match {
        case "NONE" => {
          val consumer =
            new FlinkKafkaConsumer010(
              params.getRequired("topic"),
              new SimpleStringSchema,
              params.getProperties)
          consumer.setStartFromLatest()
          env.addSource(consumer)
        }
        case "EMBEDDED" => {
          val consumer =
            new FlinkKafkaConsumer010(
              params.getRequired("topic"),
              new EmbeddedAvroDeserializationSchema[GenericRecord](classOf[GenericRecord]),
              params.getProperties)
          consumer.setStartFromLatest()
          env.addSource(consumer).map(_.toString)
        }
        case s => {
          println(s"Unknown schema type: ${s}")
          return
        }
      }

    if (params.has("output")) {
      stream.writeAsText(params.get("output"))
    } else {
      println("Printing result to stdout. Use --output to specify output path.")
      stream.print()
    }

    env.execute("SchemaRegistryExample")

    ()
  }

}
