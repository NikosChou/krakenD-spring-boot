# Simple spring project running behind a KrakenD gateway.

Having a spring boot controller doing a redirect can produce different 
results depends on where its running.

# Start only spring boot
Running the spring boot app local under port 8080, we get a response with status 302 

``
$ http :8080/redirect-to-index
``
```
HTTP/1.1 302
Connection: keep-alive
Content-Length: 0
Date: Thu, 17 Mar 2022 13:54:33 GMT
Keep-Alive: timeout=60
Location: http://localhost:8080/index
```

# Run the KrakenD and the spring boot app's
```
cd krakenD/
docker-compose up

# It start's 3 spring-boot applications with the corresponding profile (local, k8s & k8s-error) 
# And a KrakenD running on port 9000
```
We are expecting the same behavior when the application is running behind a krakenD
with the only difference that the KrakenD will follow the Location response header.

##1) Running behind KrakenD
Setting the Host header so its more obvious where the request is coming
####request:
``
$ http :9000/local/redirect-to-index Host:server.prod
``
####response:
```
HTTP/1.1 200 OK
Content-Length: 16
Content-Type: text/plain;charset=UTF-8
Date: Thu, 17 Mar 2022 14:03:35 GMT
X-Krakend: Version 2.0.0
X-Krakend-Completed: false

Welcome to index
```
####Logs:
```
spring-boot-local        | 2022-03-17 14:22:22.070 TRACE 1 --- [nio-8080-exec-3] c.e.forwardheaders.SimpleController      : Enter redirect, headers:
spring-boot-local        | ['host': 'spring-boot-local:8080']
spring-boot-local        | ['user-agent': 'KrakenD Version 2.0.0']
spring-boot-local        | ['x-forwarded-for': '172.22.0.1']
spring-boot-local        | ['x-forwarded-host': 'server.prod']
spring-boot-local        | ['accept-encoding': 'gzip']
spring-boot-local        | 2022-03-17 14:22:22.070  INFO 1 --- [nio-8080-exec-3] c.e.forwardheaders.SimpleController      : Location header: 'http://spring-boot-local:8080/index'
spring-boot-local        | 2022-03-17 14:22:22.071 TRACE 1 --- [nio-8080-exec-3] c.e.forwardheaders.SimpleController      : Exit with(redirect)
spring-boot-local        | 2022-03-17 14:22:22.073 TRACE 1 --- [nio-8080-exec-4] c.e.forwardheaders.SimpleController      : Enter index
spring-boot-local        | 2022-03-17 14:22:22.073 TRACE 1 --- [nio-8080-exec-4] c.e.forwardheaders.SimpleController      : Exit with(index)
krakend                  | [GIN] 2022/03/17 - 14:22:22 | 200 |      5.8972ms |      172.22.0.1 | GET      "/local/redirect-to-index"
```
The X-Forwarded-host is identifying the original host `['x-forwarded-host': 'server.prod']`.

KrakenD follows the location response header `http://spring-boot-local:8080/index` and return a 200 response.


##2) Running behind KrakenD and make spring boot believe that is running on K8s
Setting the property `spring.main.cloud-platform=kubernetes` 

``
$ http :9000/k8s-error/redirect-to-index Host:server.prod
``
```
HTTP/1.1 500 Internal Server Error 
Content-Length: 0
Date: Thu, 17 Mar 2022 14:31:40 GMT
X-Krakend: Version 2.0.0
X-Krakend-Completed: false
```
```
spring-boot-k8s-error    | 2022-03-17 14:31:40.624 TRACE 1 --- [nio-8080-exec-2] c.e.forwardheaders.SimpleController      : Enter redirect, headers:
spring-boot-k8s-error    | ['host': 'spring-boot-k8s-error:8080']
spring-boot-k8s-error    | ['user-agent': 'KrakenD Version 2.0.0']
spring-boot-k8s-error    | ['x-forwarded-host': 'server.prod']
spring-boot-k8s-error    | ['accept-encoding': 'gzip']
spring-boot-k8s-error    | 2022-03-17 14:31:40.624  INFO 1 --- [nio-8080-exec-2] c.e.forwardheaders.SimpleController      : Location header: 'http://server.prod:8080/index'
spring-boot-k8s-error    | 2022-03-17 14:31:40.625 TRACE 1 --- [nio-8080-exec-2] c.e.forwardheaders.SimpleController      : Exit with(redirect)
krakend                  | [KRAKEND] 2022/03/17 - 14:31:40.846 ▶ ERROR [ENDPOINT: /k8s-error/redirect-to-index] Get "http://server.prod:8080/index": dial tcp: lookup server.prod on 127.0.0.11:53: no such host
krakend                  | [GIN] 2022/03/17 - 14:31:40 | 500 |    224.5853ms |      172.22.0.1 | GET      "/k8s-error/redirect-to-index"
```
The response is a 500 but in logs we can see the request went inside the redirect method in SimpleController,
setting the redirect to `http://server.prod:8080/index` and KrakenD fails to follow, so it returns a 500.

When is running in K8s spring use the X-Forwarded-host to redirect, this is the default behavior. See (https://spring.getdocs.org/en-US/spring-boot-docs/howto/howto-embedded-web-servers.html#howto-use-tomcat-behind-a-proxy-server)

##3) Running behind KrakenD & change forward header strategy
Setting the properties `spring.main.cloud-platform=kubernetes` `server.forward-headers-strategy=none`

``
$ http :9000/k8s/redirect-to-index Host:server.prod
``
```
HTTP/1.1 200 OK
Content-Length: 16
Content-Type: text/plain;charset=UTF-8
Date: Thu, 17 Mar 2022 14:39:40 GMT   
X-Krakend: Version 2.0.0
X-Krakend-Completed: false

Welcome to index
```
```
spring-boot-k8s          | 2022-03-17 14:39:40.047 TRACE 1 --- [nio-8080-exec-1] c.e.forwardheaders.SimpleController      : Enter redirect, headers:
spring-boot-k8s          | ['host': 'spring-boot-k8s:8080']
spring-boot-k8s          | ['user-agent': 'KrakenD Version 2.0.0']
spring-boot-k8s          | ['x-forwarded-for': '172.22.0.1']
spring-boot-k8s          | ['x-forwarded-host': 'server.prod']
spring-boot-k8s          | ['accept-encoding': 'gzip']
spring-boot-k8s          | 2022-03-17 14:39:40.050  INFO 1 --- [nio-8080-exec-1] c.e.forwardheaders.SimpleController      : Location header: 'http://spring-boot-k8s:8080/index'
spring-boot-k8s          | 2022-03-17 14:39:40.051 TRACE 1 --- [nio-8080-exec-1] c.e.forwardheaders.SimpleController      : Exit with(redirect)
spring-boot-k8s          | 2022-03-17 14:39:40.058 TRACE 1 --- [nio-8080-exec-1] c.e.forwardheaders.SimpleController      : Enter index
spring-boot-k8s          | 2022-03-17 14:39:40.058 TRACE 1 --- [nio-8080-exec-1] c.e.forwardheaders.SimpleController      : Exit with(index)
krakend                  | [GIN] 2022/03/17 - 14:39:40 | 200 |    167.7496ms |      172.22.0.1 | GET      "/k8s/redirect-to-index"
```

Location `http://spring-boot-k8s:8080/index` is pointing to spring application, so KrakenD follows the link and returns a 200
