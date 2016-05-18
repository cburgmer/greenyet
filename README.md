# greenyet

One status dashboard to rule them all, those lousy microservices. Are they green yet?

## <s>Problem</s> Solution statement

Show the applications statuses across the IT floor, to

1. Encourage good status pages,
2. Share application health with all stakeholders,
3. Provide a handy view on environment uptime.

Greenyet polls all your services for status information and gives a traffic light overview on the application health.

## Usage

    $ CONFIG_DIR=example lein ring server
    $ ./example/run_mock_service.sh

### Configuration

Config as YAML (remember JSON is a subset):

1. Status URL config `status_url.yaml`

    ``` yaml
    ---
    - system: MySystem
      url: http://%host%:8080/status
    ```

2. Host list `hosts.yaml`

    ``` yaml
    ---
    - hostname: 192.168.0.10
      environment: DEV
      system: MySystem
    - hostname: 192.168.10.42
      environment: PROD
      system: MySystem
    ```

## More

Reach out to [@cburgmer](https://twitter.com/cburgmer) for feedback and ideas.
