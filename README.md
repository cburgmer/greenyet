# greenyet

One dashboard to rule them all, those lousy machines. Are they green yet?

## Usage

    $ CONFIG_DIR=example lein ring server
    $ ./example/run_mock_service.sh

### Configuration

Two files are required (YAML/JSON):

1. status_url.yaml which configures the status URL for each service, e.g.

``` yaml
---
- system: MySystem
  url: http://%host%:8080/status
```

2. hosts.yaml which includes all the hosts monitored

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
