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

import com.hortonworks.registries.schemaregistry.SchemaVersionKey
import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient
import com.hortonworks.registries.schemaregistry.errors.SchemaNotFoundException

@SerialVersionUID(1L)
class HortonworksRegistryDeserializationSchema[T](avroType: Class[T], url: String = "http://localhost:9999/api/v1") extends DeserializationSchema[T] {

  import scala.collection.JavaConverters._

  @transient
  private[this] lazy val typeInfo = TypeExtractor.getForClass(avroType)

  @transient
  private[this] lazy val registryConfig = Map(
    SchemaRegistryClient.Configuration.SCHEMA_REGISTRY_URL.name -> url)

  @transient
  private[this] lazy val registryClient =
    new SchemaRegistryClient(registryConfig.asJava)

  @transient
  private[this] var decoder: BinaryDecoder = null

  private[this] def getSchemaInfo(message: Array[Byte]): (Long, Int) = {
    if (message.length < 13)
      throw new IllegalArgumentException(s"Message is too short for schema encoding reference: ${message.mkString(" ")}")

    val buffer = ByteBuffer.wrap(message)

    val encodingVersion = buffer.get
    if (encodingVersion > 1)
      throw new IllegalArgumentException(s"Unrecognized schema encoding version: ${encodingVersion}")

    val schemaId = buffer.getLong
    val schemaVersion = buffer.getInt

    (schemaId, schemaVersion)
  }

  private[this] def getSchema(schemaId: Long, schemaVersion: Int): Option[Schema] = {
    for {
      schemaMetadataInfo <- Option(registryClient.getSchemaMetadataInfo(schemaId))
      schemaMetadata <- Option(schemaMetadataInfo.getSchemaMetadata())
      schemaName <- Option(schemaMetadata.getName())
      schemaVersionKey = new SchemaVersionKey(schemaName, schemaVersion)
      schemaVersionInfo <- Option(registryClient.getSchemaVersionInfo(schemaVersionKey))
      schemaText <- Option(schemaVersionInfo.getSchemaText())
    } yield new Schema.Parser().parse(schemaText)
  }

  override def deserialize(message: Array[Byte]): T = {
    val (schemaId, schemaVersion) = getSchemaInfo(message)
    getSchema(schemaId, schemaVersion) match {
      case Some(schema) => {
        val reader: DatumReader[T] =
          if (avroType == classOf[GenericRecord])
            new GenericDatumReader[T](schema)
          else if (classOf[SpecificRecordBase].isAssignableFrom(avroType))
            new SpecificDatumReader[T](schema)
          else
            new ReflectDatumReader[T](schema)

        try {
          this.decoder = DecoderFactory.get().binaryDecoder(message, decoder)
          reader.read(null.asInstanceOf[T], decoder)
        } catch {
          case e: Exception =>
            throw new RuntimeException(e)
        }
      }
      case _ =>
        throw new SchemaNotFoundException(s"No schema found matching id and version: ${schemaId}v${schemaVersion}")
    }
  }

  override def isEndOfStream(nextElement: T): Boolean = false

  override def getProducedType(): TypeInformation[T] = typeInfo

}
