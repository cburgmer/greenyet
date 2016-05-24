# greenyet

One status dashboard to rule them all, those lousy microservices. Are they green yet?

## <s>Problem</s> Solution statement

Show the applications statuses across the IT floor, to

1. Encourage good status pages,
2. Share application health with all stakeholders,
3. Provide a handy view on environment uptime.

Greenyet polls all your services for status information and gives a traffic light overview on the application health.

## Usage

greenyet is written in Clojure. Give it a try if you haven't yet.

    $ CONFIG_DIR=example/simple ./lein ring server
    $ ./example/simple/run_mock_service.sh

Don't want to go that far? We got you covered! Try the production release [standalone JAR](https://github.com/cburgmer/greenyet/releases) (run via `java -jar greenyet-*-standalone.jar`).

### Configuration

Config as YAML (remember JSON is a subset):

1. Status URL config `status_url.yaml`

    ``` yaml
    ---
    - system: SimpleSystem
      url: http://%host%:8080/
    - system: SystemWithStatusJson
      url: http://%hostname%:8080/status.json
      color: "status"
      message: "readableStatus"
      package-version: "packageWithVersion"
    - system: EvenMoreComplexSystem
      url: http://%hostname%:8080/complex.json
      package-version: "packageWithVersion"
      color:
        json-path: $.complex[1].color # query as implemented by https://github.com/gga/json-path
        green-value: "healthy"
        yellow-value: "warning"
    - system: SystemWithComponents
      url: http://%hostname%:8080/with_components.json
      color: "color"
      package-version: "version"
      components:
        json-path: $.subSystems
        color: "status"
        message: "description"
        name: "name"
    ```

2. Host list `hosts.yaml`

    ``` yaml
    ---
    - hostname: 192.168.0.10
      environment: DEV
      system: SimpleSystem
    - hostname: 192.168.10.42
      environment: PROD
      system: SimpleSystem
    ```

## More

Reach out to [@cburgmer](https://twitter.com/cburgmer) for feedback and ideas.
