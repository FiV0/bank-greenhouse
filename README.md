# Bank API

Starting the server can be done via
```
clj -M -m server :port 8080
```

you can then get/post stuff to the endpoints. Either via terminal
```bash
curl -X POST -H "Content-Type: application/json" -d '{"name":"Mr. foo"}' http://localhost:8080/account
```
```bash
curl -X GET http://localhost:8080/account/0
```

or via the repl, see the [client](src/client.clj) namespace.
```clojure
(require '[client])
(client/http-post "/account" {:name "Mr. foo"})
(client/http-get "/account/0")
```

## License
