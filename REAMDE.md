This module provides common services for the Event Engine subsystem. 

Primarily it provides the `EventEnginePicker` declared in the `discovery` package. The
`EventEnginePicker` or more generally the discovery services provides a configurable means
to determine Kapacitor instances for routing and addressing using a consistent hashing of
the measurement and event task attributes, such as tenant and resource ID.

## Example Kubernetes deployment

For testing of the `KubernetesServiceEndpointPicker`, an example Kubernetes manifest file is
provided at `examples/kube-deployment/kapacitor.yml`. It is expected to use this manifest with
a local cluster running in Docker for Desktop. 

The kapacitor statefulset and service can be deployed using:

```
kubectl apply -f kapacitor.yml
```