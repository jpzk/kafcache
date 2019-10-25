# 1.2

* Exposed recover parameter setting, default set to true. If set to false, it will not write to store on recovery.
* Will not retry on exception e.g. connection loss, but will throw exception right away.

# 1.1

* Exposed StateStore persistent parameter setting

# 1.0

* Suppport for Memcached
* Apache Kafka 2.3.0