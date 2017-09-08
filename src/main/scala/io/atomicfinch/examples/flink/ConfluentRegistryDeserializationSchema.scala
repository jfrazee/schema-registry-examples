/*
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

import java.nio.ByteBuffer

import scala.util.Try

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.rest.exceptions.Errors

import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.BinaryDecoder
import org.apache.avro.io.DatumReader
import org.apache.avro.io.DecoderFactory
import org.apache.avro.reflect.ReflectDatumReader
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.specific.SpecificRecordBase

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.typeutils.TypeExtractor
import org.apache.flink.streaming.util.serialization.DeserializationSchema

@SerialVersionUID(1L)
class ConfluentRegistryDeserializationSchema[T](avroType: Class[T], url: String = "http://localhost:8081/") extends DeserializationSchema[T] {

  @transient
  private[this] lazy val typeInfo = TypeExtractor.getForClass(avroType)

  @transient
  private[this] lazy val registryClient =
    new CachedSchemaRegistryClient(url, 1000)

  @transient
  private[this] var decoder: BinaryDecoder = null

  private[this] def getSchemaId(message: Array[Byte]): Int = {
    if (message.length < 5)
      throw new IllegalArgumentException(s"Message is too short for schema encoding reference: ${message.mkString(" ")}")

    val buffer = ByteBuffer.wrap(message)

    val magicByte = buffer.get
    if (magicByte > 0)
      throw new IllegalArgumentException(s"Unrecognized magic byte: ${magicByte}")

    val schemaId = buffer.getInt

    schemaId
  }

  private[this] def getContentsWithSchemaId(message: Array[Byte]): (Array[Byte], Int) = {
    val schemaId = getSchemaId(message)
    val contents = message.drop(5)

    if (contents.isEmpty)
      throw new IllegalArgumentException(s"Message is 0 bytes")

    (contents, schemaId)
  }

  private[this] def getSchema(schemaId: Int): Option[Schema] = {
    for {
      result <- Try(registryClient.getById(schemaId)).toOption
      schema <- Option(result)
    } yield schema
  }

  override def deserialize(message: Array[Byte]): T = {
    val (contents, schemaId) = getContentsWithSchemaId(message)

    getSchema(schemaId) match {
      case Some(schema) => {
        val reader: DatumReader[T] =
          if (avroType == classOf[GenericRecord])
            new GenericDatumReader[T](schema)
          else if (classOf[SpecificRecordBase].isAssignableFrom(avroType))
            new SpecificDatumReader[T](schema)
          else
            new ReflectDatumReader[T](schema)

        try {
          this.decoder = DecoderFactory.get().binaryDecoder(contents, decoder)
          reader.read(null.asInstanceOf[T], decoder)
        } catch {
          case e: Exception =>
            throw new RuntimeException(e)
        }
      }
      case _ =>
        throw Errors.schemaNotFoundException()
    }
  }

  override def isEndOfStream(nextElement: T): Boolean = false

  override def getProducedType(): TypeInformation[T] = typeInfo

}
