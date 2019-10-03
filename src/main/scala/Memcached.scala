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
 
import org.apache.kafka.streams.processor.StateRestoreCallback;
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.KeyValueIterator
import java.{util => ju}
import org.apache.kafka.streams.processor.{ProcessorContext, StateStore}
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier
import org.apache.kafka.common.utils.Bytes
import net.spy.memcached._

import org.apache.commons.codec.binary.Hex

class MemcachedStore(name: String, endpoint: String)
    extends KeyValueStore[Bytes, Array[Byte]]
    {
  import scalacache._
  import scalacache.memcached._
  import scalacache.modes.sync._

  def bytes2hex(bytes: Array[Byte]) = Hex.encodeHex(bytes).mkString

  var opened = false;
  var cache: Cache[Array[Byte]] = null
  import scalacache.serialization.Codec

  implicit val codec = new Codec[Array[Byte]] {
    def encode(value: Array[Byte]): Array[Byte] = value
    def decode(bytes: Array[Byte]): Codec.DecodingResult[Array[Byte]] =
      Right(bytes)
  }
  def convertKey(bytes: Bytes) = bytes2hex(bytes.get())

  def name(): String = name
  def persistent(): Boolean = false
  def init(context: ProcessorContext, root: StateStore): Unit = {

    val memcachedClient = new MemcachedClient(
      new BinaryConnectionFactory(),
      AddrUtil.getAddresses(endpoint)
    )

    cache = MemcachedCache(memcachedClient)
    context.register(root, new StateRestoreCallback {
      def restore(key: Array[Byte], value: Array[Byte]) = {
        put(Bytes.wrap(key), value)
      }
    })
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
    cache.get(convertKey(key)) match {
      case Some(v) => v
      case None    => null
    }
  }
  def delete(key: Bytes): Array[Byte] = {
    val old: Array[Byte] = cache.get(convertKey(key)).getOrElse(null)
    cache.remove(convertKey(key))
    old
  }
  def put(k: Bytes, v: Array[Byte]) = {
    cache.put(convertKey(k))(v);
    ()
  }
  def putIfAbsent(key: Bytes, value: Array[Byte]): Array[Byte] = {
    cache.get(convertKey(key)) match {
      case Some(v) => v
      case None =>
        cache.put(key)(value)
        null
    }
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

class MemcachedStoreSupplier(name: String, endpoint: String) extends KeyValueBytesStoreSupplier {
  def get() = new MemcachedStore(name, endpoint)
  def metricsScope() = s"memcached-$name"
  def name() = name
}
