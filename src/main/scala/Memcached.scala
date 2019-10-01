package com.madewithtea.kafkache
 
import org.apache.kafka.streams.processor.StateRestoreCallback;
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.KeyValueIterator
import java.{util => ju}
import org.apache.kafka.streams.processor.{ProcessorContext, StateStore}
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier
import org.apache.kafka.common.utils.Bytes
import net.spy.memcached._

import org.apache.commons.codec.binary.Hex

/**
  * https://kafka.apache.org/23/javadoc/org/apache/kafka/streams/state/KeyValueStore.html
  */
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
