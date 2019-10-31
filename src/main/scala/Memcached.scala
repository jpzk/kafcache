/**
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package com.madewithtea.kafcache

import org.apache.kafka.streams.state.{KeyValueStore, KeyValueIterator, KeyValueBytesStoreSupplier}
import org.apache.kafka.streams.processor.{StateRestoreCallback, ProcessorContext, StateStore}
import org.apache.kafka.common.utils.Bytes

import java.{util => ju}
import net.spy.memcached._
import scala.util.{Try, Failure, Success}
import org.apache.commons.codec.binary.Hex

import scalacache._
import scalacache.memcached._
import scalacache.modes.try_._
import scalacache.serialization.Codec

class MemcachedStore(
    name: String,
    endpoint: String,
    persistent: Boolean = true,
    recover: Boolean = true
) extends KeyValueStore[Bytes, Array[Byte]] {

  var opened = false;
  var cache: Cache[Array[Byte]] = null
 
  implicit val codec = new Codec[Array[Byte]] {
    def encode(value: Array[Byte]): Array[Byte] = value
    def decode(bytes: Array[Byte]): Codec.DecodingResult[Array[Byte]] =
      Right(bytes)
  }

  def convertKey(bytes: Bytes) = Hex.encodeHex(bytes.get).mkString

  def name(): String = name
  def persistent(): Boolean = this.persistent

  def connect(): Unit = {
    val cf = new ConnectionFactoryBuilder()
      .setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
      .setFailureMode(FailureMode.Cancel)
      .build()
    val memcachedClient = new MemcachedClient(
      cf,
      AddrUtil.getAddresses(endpoint)
    )
    cache = MemcachedCache(memcachedClient)
  }

  def init(context: ProcessorContext, root: StateStore): Unit = {
    connect()
    val recoverCallback = if (recover) new StateRestoreCallback {
      def restore(key: Array[Byte], value: Array[Byte]) = {
        put(Bytes.wrap(key), value)
      }
    } else
      new StateRestoreCallback {
        def restore(key: Array[Byte], value: Array[Byte]) = {}
      }

    context.register(root, recoverCallback)
    opened = true
  }
  def isOpen(): Boolean = opened
  def approximateNumEntries(): Long = Long.MaxValue
  def range(
      from: Bytes,
      to: Bytes
  ) = throw new NotImplementedError("Range not implemented")
  def close() = {
    cache.close()
    opened = false
    ()
  }
  def flush(): Unit = {}

  def get(key: Bytes): Array[Byte] = {
    val t = cache.get(convertKey(key)).map { opt =>
      opt match {
        case Some(v) => v
        case None    => null
      }
    }
    t.get // to make it interface compliant
  }

  def delete(key: Bytes): Array[Byte] = {
    val t = for {
      old <- cache.get(convertKey(key)).map {
        case Some(v) => v
        case None    => null
      }
      _ <- cache.remove(convertKey(key))
    } yield old
    t.get // to make it interface compliant
  }

  def put(k: Bytes, v: Array[Byte]): Unit = {
    cache.put(convertKey(k))(v).get // to make it interface compliant
  }

  def putIfAbsent(key: Bytes, value: Array[Byte]): Array[Byte] = {
    val t = cache.get(convertKey(key)).map { opt =>
      opt match {
        case Some(v) => v
        case None =>
          cache.put(key)(value)
          null
      }
    }
    t.get // to make it interface compliant
  }
  def all(): KeyValueIterator[Bytes, Array[Byte]] =
    throw new NotImplementedError("all() not implemented")

  def putAll(
      entries: ju.List[
        org.apache.kafka.streams.KeyValue[Bytes, Array[Byte]]
      ]
  ): Unit = {
    entries.forEach { kv =>
      put(kv.key, kv.value)
    }
  }
}

class MemcachedStoreSupplier(
    name: String,
    endpoint: String,
    persistent: Boolean = true,
    recover: Boolean = true
) extends KeyValueBytesStoreSupplier {
  def get() = new MemcachedStore(name, endpoint, persistent, recover)
  def metricsScope() = s"memcached-$name"
  def name() = name
}
