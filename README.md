
# enrolments-orchestrator

## Summary

Agents and Traders require the capability to have their accounts and associated access suspended and terminated, the service is to fulfil this functionality.

The enrolments-orchestrator service exposes a single API call:

### DELETE /enrolments-orchestrator/agents/:ARN?terminationDate={termination Long}

 - terminationDate is optional, in milliseconds. Defaults to the current time

Responds with:

| Status        | Message       |
|:-------------:|---------------|
| 204      | Done |
| 400      | Payload incorrect or insufficient for processing.|
| 401      | Unauthorised - the provided bearer token is either expired or not valid|
| 500      | Service error |


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
