# greenyet

One status dashboard to rule them all, those lousy microservices. Are they green yet?

## <s>Problem</s> Solution statement

Show the applications statuses across the IT floor, to

1. Encourage good status pages,
2. Share application health with all stakeholders,
3. Don't make humans check a list of machines manually.

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

## Master greenyet

### Monitor your whole lot together

Put in your reverse proxy/load balancer together with your nodes. Most of the times your machines will be green, so only the first entry of the config will be shown per environment (e.g. the reverse proxy, your call). The list unfolds once one machine changes color.

### Green is green

Make sure that the main machine changes color when a component gets unhealthy. Greenyet does not interpret component colors, but only looks at the overall one.

### Visualise the flow to production

Get your environments sorted in the order from dev to prod. Make use of the [environment names](resources/environment_names.yaml) that greenyet knows about to get the right ordering.

### Start with little

You can monitor just based on a HTTP 200 response, if you don't have an elaborate JSON available yet.

## Development

    # Back-end unit tests
    $ ./lein test
    # UI unit tests, later `open http://localhost:3000/styleguide.html` 
    $ CONFIG_DIR=example/simple lein ring server-headless 

## More

Reach out to [@cburgmer](https://twitter.com/cburgmer) for feedback and ideas.
