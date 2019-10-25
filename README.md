# kafcache

In-memory Kafka Streams state store backends for low latency state store lookups. In the current version only memcached is available. 

## Dependency

```
libraryDependencies += "com.madewithtea" %% "kafcache" % "1.2.0" 
```

## Use Memcached 

Memcached does not support binary keys, therefore the byte arraywill be serialized as hex string. For more information on Memcached have a look at its documentation on https://memcached.org/.

```
  import com.madewithtea.kafcache.MemcachedStoreSupplier

  val store = Stores
    .keyValueStoreBuilder(
      new MemcachedStoreSupplier("state-store-name", "localhost:11211"),
      Serdes.ByteArray(),
      Serdes.ByteArray()
    )
  .withLoggingEnabled(new java.util.HashMap[String, String]())
```

