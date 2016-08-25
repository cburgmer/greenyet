# greenyet

One status dashboard to rule them all, those lousy microservices. Are they green yet?

## Why?

1. Microservices mean smaller and more services, machines become "cattle not pets". Keeping track becomes challenging.
2. Smaller machines allows scaling teams, increasing dependencies. Visibility becomes more important.

Greenyet polls all your services for status information and gives a traffic light overview on the application health.

## Usage

greenyet is written in Clojure. Give it a try if you haven't yet.

    $ CONFIG_DIR=example/simple ./lein ring server
    $ ./example/simple/run_mock_service.sh

Don't want to go that far? We got you covered! Try the production release [standalone JAR](https://github.com/cburgmer/greenyet/releases) (run via `java -jar greenyet-*-standalone.jar`).

![Screenshot](https://github.com/cburgmer/greenyet/raw/master/example/screenshot.png)

### Configuration

Config as YAML (remember JSON is a subset):

1. Status URL config `status_url.yaml`

    ``` yaml
    ---
    - system: SimpleSystem
      url: http://%hostname%:8080/
    - system: SystemWithStatusJson
      url: http://%hostname%:8080/status.json
      color: "status"
    - system: SystemWithComplexStatusJson
      url: http://%hostname%:8080/complex_status.json
      package-version: "packageNameWithVersion"
      message: "readableStatus"
      color:
        json-path: $.statuses[0].color # query as implemented by https://github.com/gga/json-path
        green-value: "healthy"
        yellow-value: "warning"
    - system: SystemWithComponents
      url: http://%hostname%:8080/status_with_components.json
      color: "status"
      components:
        json-path: $.subSystems
        color: "status"
        name: "name"
        message: "description"
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
    
But read the [full description here](https://github.com/cburgmer/greenyet/wiki/Howto).

## Development

    # Back-end unit tests
    $ ./lein test
    # UI unit tests, later `open http://localhost:3000/styleguide.html`
    $ CONFIG_DIR=example/simple lein ring server-headless

## Contributors

* [Sandeep Rakhra](https://github.com/rakhra)
* [Christoph Burgmer](https://github.com/cburgmer)

## More

* [Howto](https://github.com/cburgmer/greenyet/wiki/Howto)
* [Master greenyet](https://github.com/cburgmer/greenyet/wiki/Master-greenyet)
