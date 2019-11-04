# kafcache

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/71a5940ef86a4cc88f07d792c93d89c1)](https://www.codacy.com/manual/jpzk/kafcache?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=jpzk/kafcache&amp;utm_campaign=Badge_Grade)
[![Maven Central](https://img.shields.io/maven-central/v/com.madewithtea/kafcache_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Ckafcache)

In-memory Kafka Streams state store backends for low latency state store lookups. In the current version only memcached is available. 

## Dependency

```scala
libraryDependencies += "com.madewithtea" %% "kafcache" % "1.3.0" 
```

## Use Memcached 

Memcached does not support binary keys, therefore the byte arraywill be serialized as hex string. For more information on Memcached have a look at its documentation on [https://memcached.org/](https://memcached.org/).

```scala
  import com.madewithtea.kafcache.MemcachedStoreSupplier

  val store = Stores
    .keyValueStoreBuilder(
      new MemcachedStoreSupplier("state-store-name", "localhost:11211"),
      Serdes.ByteArray(),
      Serdes.ByteArray()
    )
  .withLoggingEnabled(new java.util.HashMap[String, String]())
```

## Skip recovery

The following code will initialize a state store but it will not write to it in the recovery process. Hence, the recovery is much faster than doing actual writes.  

```scala
  import com.madewithtea.kafcache.MemcachedStoreSupplier

  val store = Stores
    .keyValueStoreBuilder(
      new MemcachedStoreSupplier("state-store-name", "localhost:11211", recover = false),
      Serdes.ByteArray(),
      Serdes.ByteArray()
    )
  .withLoggingEnabled(new java.util.HashMap[String, String]())
```

