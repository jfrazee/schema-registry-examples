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

import java.io.ByteArrayInputStream

import org.apache.avro.file.DataFileStream
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DatumReader
import org.apache.avro.reflect.ReflectDatumReader
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.specific.SpecificRecordBase
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.typeutils.TypeExtractor
import org.apache.flink.streaming.util.serialization.DeserializationSchema

@SerialVersionUID(1L)
class EmbeddedAvroDeserializationSchema[T](avroType: Class[T]) extends DeserializationSchema[T] {

  @transient
  private[this] lazy val typeInfo = TypeExtractor.getForClass(avroType)

  @transient
  private[this] lazy val reader: DatumReader[T] =
    if (avroType == classOf[GenericRecord])
      new GenericDatumReader[T]()
    else if (classOf[SpecificRecordBase].isAssignableFrom(avroType))
      new SpecificDatumReader[T](avroType)
    else
      new ReflectDatumReader[T](avroType)

  override def deserialize(message: Array[Byte]): T = {
    try {
      val content = new ByteArrayInputStream(message)
      val stream = new DataFileStream[T](content, reader)
      stream.next
    } catch {
      case e: Exception =>
        throw new RuntimeException(e)
    }
  }

  override def isEndOfStream(nextElement: T): Boolean = false

  override def getProducedType(): TypeInformation[T] = typeInfo

}
